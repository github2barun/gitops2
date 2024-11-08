import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.metrics.ParsedSum
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.Timestamp

@Slf4j
public class StdMobileAppResellerTransactionSalesAggregator extends AbstractAggregator {
    static final def TABLE = "std_mobile_reseller_tran_stats_sales_aggregation"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${StdMobileAppResellerTransactionSalesAggregator.allowed_profiles:VOUCHER_PURCHASE,VOS_PURCHASE,VOT_PURCHASE,PURCHASE,TOPUP,TRANSFER,CREDIT_TRANSFER,VAS_BUNDLE,PRODUCT_RECHARGE,DATA_BUNDLE,COMBO_BUNDLE,SMS_BUNDLE,IDD_BUNDLE,FIBER_BUNDLE,MM2ERS}')
    String profileId

    @Value('${StdMobileAppResellerTransactionSalesAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${StdMobileAppResellerTransactionSalesAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${StdMobileAppResellerTransactionSalesAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${StdMobileAppResellerTransactionSalesAggregator.deleteDataAfterDays:60}')
    Integer deleteDataAfterDays;

    @Transactional
    @Scheduled(cron = '${StdMobileAppResellerTransactionSalesAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("*************************************************** StdMobileAppResellerTransactionSalesAggregator Aggregator started ********************************************" + new Date());
        def profileIdList = profileId.split(",")

        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList( bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList);

                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                catch (Exception e){
                    log.error(e.getMessage())
                }

            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex();

            for (ReportIndex index : indices) {

                log.info(index.toString())
                //fetch data from ES
                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList);

            }
        }

        log.info("***************************************StdMobileAppResellerTransactionSalesAggregator Aggregator ended ******************************************");
    }

    private void aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = createSourceBuilder(fromDate, toDate, profileIdList, null);
        searchRequest.source(searchSourceBuilder);
        log.info("*******Search Request:::: " + searchRequest.toString())
        SearchResponse response = null
        response = generateResponse(searchRequest, response);

        if (response.status() == RestStatus.OK) {
            Aggregations aggregations = response.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("DayWiseSalesReport");
            while(parsedComposite.getBuckets().size() != 0) {
                searchRequest = new SearchRequest(index);
                searchSourceBuilder = createSourceBuilder(fromDate, toDate, profileIdList, parsedComposite.afterKey());
                searchRequest.source(searchSourceBuilder);
                log.info("*******Search Request:::: " + searchRequest.toString())
                response = generateResponse(searchRequest, response);
                parsedComposite = response.getAggregations().asMap().get("DayWiseSalesReport")
            }
        }
        cleanData()
    }

    private SearchSourceBuilder createSourceBuilder(String fromDate, String toDate,String[] profileID, Map<String, Object> afterKey) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("SenderMSISDN").field("senderMSISDN.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("TransactionProfile").field("transactionProfile.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("SenderResellerId").field("senderResellerId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("SenderAccountType").field("senderAccountType.keyword").missingBucket(true));


        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("DayWiseSalesReport",
                sources).size(1000);

        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalTransactionAmount").field("transactionAmount"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalResellerCommissionAmount").field("resellerCommissionAmount"))
        if (afterKey != null)
        {
            compositeBuilder.aggregateAfter(afterKey)
        }

        if(bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
            searchSourceBuilder.sort("endTime", SortOrder.ASC).query(queryBuilder)
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
                    .filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.sort("endTime", SortOrder.ASC).query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private SearchResponse generateResponse(SearchRequest searchRequest, SearchResponse searchResponse) {
        List<MobileSalesTransactionModel> mobileSalesTransactionModelArrayList = new ArrayList<>();
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.debug("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);
        Map<String, Object> afterKey = null
        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("DayWiseSalesReport");
            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                Aggregation totalAmountAggregation = aggregationMap.get("TotalTransactionAmount");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregation;
                log.info("TotalTransactionAmount = " + totalAmount.getValue())
                Aggregation totalCommissionAmountAggregation = aggregationMap.get("TotalResellerCommissionAmount");
                ParsedSum totalCommissionAmount = (ParsedSum) totalCommissionAmountAggregation;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));
                Timestamp transactionDate = new Timestamp(dateTimeDay.getTime());
                log.info("transactionDate = " + transactionDate)
                String msisdn = keyValuesMap.get("SenderMSISDN")
                log.info("msisdn = " + msisdn)
                String profile = keyValuesMap.get("TransactionProfile")
                log.info("profile = " + profile)
                String resellerId = keyValuesMap.get("SenderResellerId")
                if (resellerId == null) {
                    resellerId = keyValuesMap.get("SenderMSISDN")
                }
                log.info("resellerId = " + resellerId)
                String resellerAccountTypeId = keyValuesMap.get("SenderAccountType")
                log.info("resellerAccountTypeId = " + resellerAccountTypeId)
                Double transactionBonus = BigDecimal.ZERO

                MobileSalesTransactionModel mobileSalesTransactionModel = new MobileSalesTransactionModel(transactionDate.clearTime(), msisdn, profile, resellerId, totalAmount.getValue(), totalCommissionAmount.getValue(), transactionBonus, bucket.getDocCount(), resellerAccountTypeId);
                mobileSalesTransactionModelArrayList.add(mobileSalesTransactionModel);
            }
        }
        insertAggregation(mobileSalesTransactionModelArrayList)
        return searchResponse;
    }

    private def insertAggregation(List mobileSalesTransactionModelList) {

        log.info("StdMobileAppResellerTransactionSalesAggregator Aggregated into ${mobileSalesTransactionModelList.size()} rows.")
        if (mobileSalesTransactionModelList.size() != 0) {
            def sql = """INSERT INTO ${TABLE} (transaction_date, msisdn, profile, reseller_id, transaction_amount, transaction_commission, transaction_bonus, transaction_count, reseller_account_type_id) VALUES (?,?,?,?,?,?,?,?,?)
						ON DUPLICATE KEY UPDATE 
						transaction_amount = VALUES(transaction_amount),
						transaction_commission = VALUES(transaction_commission),
						transaction_bonus = VALUES(transaction_bonus),
						transaction_count= VALUES(transaction_count)"""

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = mobileSalesTransactionModelList[i]
                        def index = 0
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.msisdn)
                        ps.setString(++index, row.profile)
                        ps.setString(++index, row.resellerId)
                        ps.setBigDecimal(++index, row.transactionAmount)
                        ps.setBigDecimal(++index, row.transactionCommission)
                        ps.setBigDecimal(++index, row.transactionBonus)
                        ps.setBigDecimal(++index, row.transactionCount)
                        ps.setString(++index, row.resellerAccountTypeId)

                    },
                    getBatchSize: { mobileSalesTransactionModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }



    class MobileSalesTransactionModel {
        private BigDecimal id
        private Timestamp transactionDate
        private String msisdn
        private String profile
        private String resellerId
        private Double transactionAmount
        private Double transactionCommission
        private Double transactionBonus
        private Long transactionCount
        private String resellerAccountTypeId

        MobileSalesTransactionModel(Timestamp transactionDate, String msisdn, String profile, String resellerId, Double transactionAmount, Double transactionCommission, Double transactionBonus, Long transactionCount, String resellerAccountTypeId) {
            this.transactionDate = transactionDate
            this.msisdn = msisdn
            this.profile = profile
            this.resellerId = resellerId
            this.transactionAmount = transactionAmount
            this.transactionCommission = transactionCommission
            this.transactionBonus = transactionBonus
            this.transactionCount = transactionCount
            this.resellerAccountTypeId = resellerAccountTypeId
        }

        BigDecimal getId() {
            return id
        }

        void setId(BigDecimal id) {
            this.id = id
        }

        Timestamp getTransactionDate() {
            return transactionDate
        }

        void setTransactionDate(Timestamp transactionDate) {
            this.transactionDate = transactionDate
        }

        String getMsisdn() {
            return msisdn
        }

        void setMsisdn(String msisdn) {
            this.msisdn = msisdn
        }

        String getProfile() {
            return profile
        }

        void setProfile(String profile) {
            this.profile = profile
        }

        String getResellerId() {
            return resellerId
        }

        void setResellerId(String resellerId) {
            this.resellerId = resellerId
        }

        Double getTransactionAmount() {
            return transactionAmount
        }

        void setTransactionAmount(Double transactionAmount) {
            this.transactionAmount = transactionAmount
        }

        Double getTransactionCommission() {
            return transactionCommission
        }

        void setTransactionCommission(Double transactionCommission) {
            this.transactionCommission = transactionCommission
        }

        Double getTransactionBonus() {
            return transactionBonus
        }

        void setTransactionBonus(Double transactionBonus) {
            this.transactionBonus = transactionBonus
        }

        Long getTransactionCount() {
            return transactionCount
        }

        void setTransactionCount(Long transactionCount) {
            this.transactionCount = transactionCount
        }

        String getResellerAccountTypeId() {
            return resellerAccountTypeId
        }

        void setResellerAccountTypeId(String resellerAccountTypeId) {
            this.resellerAccountTypeId = resellerAccountTypeId
        }
    }

    private def cleanData() {
        log.info("StdMobileAppResellerTransactionSalesAggregator --> cleaning data from before " + deleteDataAfterDays + " days.")
        def query = "DELETE FROM ${TABLE} WHERE transaction_date <= DATE_SUB(NOW() , INTERVAL ${deleteDataAfterDays} DAY)"
        jdbcTemplate.update(query)
    }

}