
package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
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
//@DynamicMixin
public class FailedVoucherGenerationAggregator extends AbstractAggregator {
    static final def TABLE = "failed_voucher_generation_denom_day_wise"

    static class FailedVoucherGenerationAggregatorModel {
		private String id;
        private String denomination;
        private String status;
        private long quantity;
        private Date createdDate;
        private String userId;

        public FailedVoucherGenerationAggregatorModel(String denomination, String status, long quantity, Date createdDate, String userId) {
            this.id = GenerateHash.createHashString(createdDate.toString(), denomination, status, userId);
            this.denomination = denomination;
            this.status = status;
            this.quantity = quantity;
            this.createdDate = createdDate;
            this.userId = userId;
        }
		
		public String getId() {
            return id;
        }

        public String getDenomination() {
            return denomination;
        }

        public String getStatus() {
            return status;
        }

        public long getQuantity() {
            return quantity;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public String toString() {
            return "FailedVoucherGenerationAggregatorModel{" +
                    "id='" + id + '\'' +
                    ", denomination=" + denomination + '\'' +
                    ", status='" + status + '\'' +
                    ", quantity='" + quantity + '\'' +
                    ", createdDate='" + createdDate + '\'' +
                    ", userId=" + userId + '\'' +
                    '}';
        }		
	}

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${FailedVoucherGenerationAggregator.hourwisedata:true}')
    boolean hourwise;
    @Value('${FailedVoucherGenerationAggregator.hour:10}')
    int hours;

    @Value('${FailedVoucherGenerationAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${FailedVoucherGenerationAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${FailedVoucherGenerationAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${FailedVoucherGenerationAggregator.profileId:VOS_PURCHASE,BULKVOUCHER_PURCHASE}')
    String profileId;

	@Value('${FailedVoucherGenerationAggregator.transactionType:VOD,VOUCHER_PURCHASE}')
	String transactionType;

    @Transactional
    @Scheduled(cron = '${FailedVoucherGenerationAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("FailedVoucherGenerationAggregator Aggregator started***************************************************************************" + new Date());
		
		log.info("profileId: " + profileId);
		log.info("transactionType: " + transactionType);

        def profileIdList = profileId.split(",")
		def transactionTypeList = transactionType.split(",")
		
        if (bulkInsertionMode) {
			log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
			log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<FailedVoucherGenerationAggregatorModel> FailedVoucherGenerationAggregatorModelES = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList, transactionTypeList)
                    insertAggregation(FailedVoucherGenerationAggregatorModelES);
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
                List<FailedVoucherGenerationAggregatorModel> FailedVoucherGenerationAggregatorModelES = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList, transactionTypeList)
                insertAggregation(FailedVoucherGenerationAggregatorModelES);
            }
        }

        log.info("FailedVoucherGenerationAggregator Aggregator ended**************************************************************************");
    }


    private List<FailedVoucherGenerationAggregatorModel> aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList, String[] transactionTypeList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList, transactionTypeList);
        searchRequest.source(searchSourceBuilder);
        List<FailedVoucherGenerationAggregatorModel> FailedVoucherGenerationAggregatorModels = generateResponse(searchRequest);
        return FailedVoucherGenerationAggregatorModels;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileID, String[] transactionTypeList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
                .field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("Denomination").field("Denomination").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResultStatus").field("ResultStatus").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("UserId").field("SenderUserId").missingBucket(true));
        
        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("VoucherGenerationAggregator",
                sources).size(200);

        compositeBuilder.subAggregation(AggregationBuilders.sum("Count").field("Count"))

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .mustNot(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))       
                    .must(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .must(QueryBuilders.termsQuery("TransactionProfile", profileID))                    
                    .must(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))       
            searchSourceBuilder.query(queryBuilder);
        }
        else{
           BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .mustNot(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .must(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .must(QueryBuilders.termsQuery("TransactionProfile", profileID))                          
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<FailedVoucherGenerationAggregatorModel> generateResponse(SearchRequest searchRequest) {
        List<FailedVoucherGenerationAggregatorModel> FailedVoucherGenerationAggregatorModels = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.info("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.info("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("VoucherGenerationAggregator");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                Aggregation totalQuantityAggregration = aggregationMap.get("Count");
                ParsedSum p = (ParsedSum) totalQuantityAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));
            
                FailedVoucherGenerationAggregatorModel FailedVoucherGenerationAggregatorModel = new FailedVoucherGenerationAggregatorModel(keyValuesMap.get("Denomination"), "FAILED", (long)p.value(), dateTimeDay,keyValuesMap.get("UserId"));

                FailedVoucherGenerationAggregatorModels.add(FailedVoucherGenerationAggregatorModel);
            }
        }

        return FailedVoucherGenerationAggregatorModels;

    }

    private def insertAggregation(List FailedVoucherGenerationAggregatorModels) {

        log.info("FailedVoucherGenerationAggregator Aggregated into ${FailedVoucherGenerationAggregatorModels.size()} rows.")
        if (FailedVoucherGenerationAggregatorModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,denomination,status,quantity,createdDate,userId) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = FailedVoucherGenerationAggregatorModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.denomination)
                        ps.setString(++index, row.status)
                        ps.setLong(++index, row.quantity)
                        ps.setDate(++index, new java.sql.Date(row.createdDate.getTime()))
                        ps.setString(++index, row.userId)                      

                    },
                    getBatchSize: { FailedVoucherGenerationAggregatorModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}