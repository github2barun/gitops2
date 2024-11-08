package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import java.util.Date;
//import com.seamless.customer.bi.aggregator.model.ReceiverWiseCreditTransferSummaryModel
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.FieldNames
import com.seamless.customer.bi.aggregator.util.GenerateHash
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import com.seamless.customer.bi.aggregator.util.DateUtil

/**
 *
 *
 *
 *
 */
@Slf4j
public class ReceiverWiseCreditTransferSummary extends AbstractAggregator {
    static final def TABLE = "receiver_wise_credit_transfer_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${ReceiverWiseCreditTransferSummary.hourwisedata:true}')
    boolean hourwise;
    @Value('${ReceiverWiseCreditTransferSummary.hour:10}')
    int hours;

    @Value('${ReceiverWiseCreditTransferSummary.profileId:CREDIT_TRANSFER}')
    String profileId;

    @Value('${ReceiverWiseCreditTransferSummary.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ReceiverWiseCreditTransferSummary.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ReceiverWiseCreditTransferSummary.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;


    @Transactional
    @Scheduled(cron = '${ReceiverWiseCreditTransferSummary.cron:*/3 * * * * ?}')


    public void aggregate() {

        log.info("ReceiverWiseCreditTransferSummary Aggregator started********************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
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
                (aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate()));
            }
        }

        log.info("ReceiverWiseCreditTransferSummary Aggregator ended************************************************************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        Map<String, Object> afterKey = null;
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate,afterKey);
        searchRequest.source(searchSourceBuilder);
        afterKey= generateResponse(searchRequest);

        if(afterKey!=null) {
            log.info("##############" +afterKey.size())
            while(afterKey!=null) {
                searchSourceBuilder = buildESQuery(fromDate, toDate,afterKey);
                searchRequest.source(searchSourceBuilder);
                afterKey = generateResponse(searchRequest);
            }
        }


    }

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate, Map<String, Object> afterKey) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);

        sources.add(new TermsValuesSourceBuilder("ResellerId").field("receiverResellerId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerMSISDN").field("receiverMSISDN.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerType").field("receiverResellerType.keyword").missingBucket(true));
        // sources.add(new TermsValuesSourceBuilder("ResellerPath").field("ReceiverResellerPath").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("transactionType").field("transactionProfile.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Region").field("receiverRegionId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("currency").field("currency.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerName").field("receiverResellerName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ReceiverAccountType").field("receiverAccountType.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("ReceiverWiseCreditTransferSummary",
                sources).size(10000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("requestAmountValue").field("transactionAmount")).subAggregation(AggregationBuilders.cardinality("uniqueReceiverCount").field("receiverMSISDN.keyword"))


        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileId))
                    .filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileId))

            searchSourceBuilder.query(queryBuilder);
        }
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<ReceiverWiseCreditTransferSummaryModel> totalTransactions = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.debug("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("ReceiverWiseCreditTransferSummary");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

                Aggregation totalAmountAggregration = aggregationMap.get("requestAmountValue");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregration;

                totalAmountAggregration = aggregationMap.get("uniqueReceiverCount");
                ParsedCardinality uniqueReceiverCount = (ParsedCardinality) totalAmountAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));
                // Date dateTimeHour = DateFormatter.formatDate(keyValuesMap.get("endTimeHour"));
                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("ResellerId"), keyValuesMap.get("ResellerMSISDN"),keyValuesMap.get("ResellerName"), keyValuesMap.get("ResellerType"),keyValuesMap.get("Region"),keyValuesMap.get("transactionType"),keyValuesMap.get("ReceiverAccountType"));
                ReceiverWiseCreditTransferSummaryModel totalTransaction = new ReceiverWiseCreditTransferSummaryModel(id, dateTimeDay, keyValuesMap.get("ResellerId"), keyValuesMap.get("ResellerMSISDN"),keyValuesMap.get("ResellerName"), keyValuesMap.get("ResellerType"),keyValuesMap.get("Region"), keyValuesMap.get("transactionType"),uniqueReceiverCount.value(), bucket.getDocCount(), totalAmount.value(),keyValuesMap.get("currency"),keyValuesMap.get("ReceiverAccountType"));
                totalTransactions.add(totalTransaction);
            }
            log.info("ReceiverWiseCreditTransferSummary Aggregated into ${totalTransactions.size()} rows.")
            insertAggregation( totalTransactions);
            return parsedComposite.afterKey();

        }
        return null;


    }

    private def insertAggregation(List totalTransactions) {


        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregation_date,reseller_id,reseller_msisdn,reseller_name,reseller_type_id,region,transaction_type,unique_receiver_count,count,amount,currency,receiver_account_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count = VALUES(count), amount = VALUES(amount)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        ps.setString(++index, row.ResellerId)
                        ps.setString(++index, row.ResellerMSISDN)
                        ps.setString(++index, row.ResellerName)
                        ps.setString(++index, row.ResellerType)
                        //ps.setString(++index, row.ResellerPath)
                        ps.setString(++index, row.Region)
                        ps.setString(++index, row.transactionType)
                        ps.setDouble(++index,row.uniqueReceiverCount)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.currency)
                        ps.setString(++index, row.receiverAccountType)
                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class ReceiverWiseCreditTransferSummaryModel {
    private String id;
    private Date endTimeDay;
    private String ResellerId;
    private String ResellerMSISDN;
    private String ResellerName;
    private String ResellerType;
    private String Region;
    private String transactionType;
    private double uniqueReceiverCount;
    private long count;
    private double amount;
    private String currency;
    private String receiverAccountType;




    public double getUniqueReceiverCount() {
        return uniqueReceiverCount;
    }

    public void setUniqueReceiverCount(double uniqueReceiverCount) {
        this.uniqueReceiverCount = uniqueReceiverCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String operator) {
        this.currency = currency;
    }

    public Date getEndTimeDay() {
        return endTimeDay;
    }

    public void setEndTimeDay(Date endTimeDay) {
        this.endTimeDay = endTimeDay;
    }

    public String getResellerId() {
        return ResellerId;
    }

    public void setResellerId(String ResellerId) {
        this.ResellerId = ResellerId;
    }

    public String getResellerMSISDN() {
        return ResellerMSISDN;
    }

    public void setResellerMSISDN(String ResellerMSISDN) {
        this.ResellerMSISDN = ResellerMSISDN;
    }

    public String getResellerType() {
        return ResellerType;
    }

    public void setResellerType(String ResellerType) {
        this.ResellerType = ResellerType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    //private Date actualDateTime;

    public String getRegion() {
        return Region;
    }

    public void setRegion(String Region) {
        this.Region = Region;
    }

    public String getResellerName() {
        return ResellerName;
    }

    public void setResellerName(String ResellerName) {
        this.ResellerName = ResellerName;
    }

    public String getReceiverAccountType() {
        return receiverAccountType;
    }

    public void setReceiverAccountType(String receiverAccountType) {
        this.receiverAccountType = receiverAccountType;
    }

    public ReceiverWiseCreditTransferSummaryModel(String id, Date endTimeDay, String ResellerId, String ResellerMSISDN, String ResellerName, String ResellerType, String Region, String transactionType, double uniqueReceiverCount, long count, double amount, String currency,String receiverAccountType) {
        this.id = id;
        this.currency = currency;
        this.endTimeDay = endTimeDay;
        this.ResellerId = ResellerId;
        this.ResellerMSISDN = ResellerMSISDN;
        this.ResellerName = ResellerName;
        this.ResellerType = ResellerType;
        this.Region = Region;
        this.transactionType = transactionType;
        this.uniqueReceiverCount = uniqueReceiverCount;
        this.count = count;
        this.amount = amount;
        this.receiverAccountType = receiverAccountType;
    }

    @Override
    public String toString() {
        return "ResellerWiseTransactionSummaryModel{" +
                "id='" + id + '\'' +
                ", currency='" + currency + '\'' +
                ", endTimeDay=" + endTimeDay +
                ", ResellerId='" + ResellerId + '\'' +
                ", ResellerMSISDN='" + ResellerMSISDN + '\'' +
                ", ResellerName='" + ResellerName + '\'' +
                ", ResellerType='" + ResellerType + '\'' +
                ", Region='" + Region + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", uniqueReceiverCount=" + uniqueReceiverCount +
                ", count=" + count +
                ", amount=" + amount +
                '}';
    }
}


