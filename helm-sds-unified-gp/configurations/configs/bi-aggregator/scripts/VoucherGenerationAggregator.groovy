
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
public class VoucherGenerationAggregator extends AbstractAggregator {
    static final def TABLE = "voucher_generation_denom_day_wise"

    static class VoucherGenerationAggregatorModel {
		private String id;
        private String denomination;
        private String resellerId;
        private long quantity;
        private Date createdDate;
        private String userId;

        public VoucherGenerationAggregatorModel(String denomination, String resellerId, long quantity, Date createdDate, String userId) {
            this.id = GenerateHash.createHashString(createdDate.toString(), denomination, resellerId, userId);
            this.denomination = denomination;
            this.resellerId = resellerId;
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

        public String getResellerId() {
            return resellerId;
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
            return "VoucherGenerationaggregatorModel{" +
                    "id='" + id + '\'' +
                    ", denomination=" + denomination + '\'' +
                    ", resellerId='" + resellerId + '\'' +
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
    @Value('${VoucherGenerationAggregator.hourwisedata:true}')
    boolean hourwise;
    @Value('${VoucherGenerationAggregator.hour:10}')
    int hours;

    @Value('${VoucherGenerationAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${VoucherGenerationAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${VoucherGenerationAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${VoucherGenerationAggregator.profileId:VOS_PURCHASE,BULKVOUCHER_PURCHASE}')
    String profileId;

	@Value('${VoucherGenerationAggregator.transactionType:VOD,VOUCHER_PURCHASE}')
	String transactionType;

    @Transactional
    @Scheduled(cron = '${VoucherGenerationAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("VoucherGenerationAggregator Aggregator started***************************************************************************" + new Date());
		
		log.info("profileId: " + profileId);
		log.info("transactionType: " + transactionType);
		
        def profileIdList = profileId.split(",")
		def transactionTypeList = transactionType.split(",")
		
        if (bulkInsertionMode) {
			log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
			log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
				log.info("index: " + index.toString())
				
                //fetch data from ES
                try {
                    List<VoucherGenerationAggregatorModel> VoucherGenerationAggregatorModelES = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList, transactionTypeList)
					
					long startTime = System.currentTimeMillis();
                    insertAggregation(VoucherGenerationAggregatorModelES);
					log.info("insertAggregation() for index " + index + " executed in " + (System.currentTimeMillis() - startTime) + " ms");
                }
                catch (Exception e){
                    log.error(e.getMessage())
                }

            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex();

            for (ReportIndex index : indices) {
                log.info("index: " + index.toString())
				
                //fetch data from ES
				try {
					List<VoucherGenerationAggregatorModel> VoucherGenerationAggregatorModelES = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList, transactionTypeList)
					
					long startTime = System.currentTimeMillis();
					insertAggregation(VoucherGenerationAggregatorModelES);
					log.info("insertAggregation() for index " + index.getIndexName() + " executed in " + (System.currentTimeMillis() - startTime) + " ms");
				}
                catch (Exception e){
                    log.error(e.getMessage())
                }
            }
        }

        log.info("VoucherGenerationAggregator Aggregator ended**************************************************************************");
    }


    private List<VoucherGenerationAggregatorModel> aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList, String[] transactionTypeList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList, transactionTypeList);
        searchRequest.source(searchSourceBuilder);
        List<VoucherGenerationAggregatorModel> VoucherGenerationAggregatorModels = generateResponse(searchRequest);
        return VoucherGenerationAggregatorModels;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileID, String[] transactionTypeList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
                .field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("Denomination").field("Denomination").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerId").field("OwnerId").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("UserId").field("SenderUserId").missingBucket(true));
        
        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("VoucherGenerationAggregator",
                sources).size(200);

        compositeBuilder.subAggregation(AggregationBuilders.sum("Count").field("Count"))

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("ChainState", "Completed"))
                    .filter(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))                    
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("ChainState", "Completed"))
                    .filter(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))                   
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<VoucherGenerationAggregatorModel> generateResponse(SearchRequest searchRequest) {
		log.info("*******Request:::: " + searchRequest.toString())
		
        List<VoucherGenerationAggregatorModel> VoucherGenerationAggregatorModels = new ArrayList<>();
        SearchResponse searchResponse = null;
		RestStatus status = null;
        try {
			long startTime = System.currentTimeMillis();
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
			log.info("client.search() executed in " + (System.currentTimeMillis() - startTime) + " ms");
			
			status = searchResponse.status();
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }
        
        log.info("response status -------------" + status);

        if (status != null && status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
			if (aggregations != null) {
	            ParsedComposite parsedComposite = aggregations.asMap().get("VoucherGenerationAggregator");
	
	            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
	                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
	                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
	                Aggregation totalQuantityAggregration = aggregationMap.get("Count");
	                ParsedSum p = (ParsedSum) totalQuantityAggregration;
	                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));
	            
					String resellerId = emptyToNull(keyValuesMap.get("ResellerId"));
					String userId = emptyToNull(keyValuesMap.get("UserId"));
					
	                VoucherGenerationAggregatorModel VoucherGenerationAggregatorModel = new VoucherGenerationAggregatorModel(keyValuesMap.get("Denomination"), resellerId, (long)p.value(), dateTimeDay, userId);
	
	                VoucherGenerationAggregatorModels.add(VoucherGenerationAggregatorModel);
	            }
			} else {
				log.info("aggregations == null");
			}
        }

        return VoucherGenerationAggregatorModels;

    }

	private String emptyToNull(String s) {
		return ("".equals(s)) ? null : s;
	}
	
    private def insertAggregation(List VoucherGenerationAggregatorModels) {

        log.info("VoucherGenerationAggregator Aggregated into ${VoucherGenerationAggregatorModels.size()} rows.")
        if (VoucherGenerationAggregatorModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,denomination,resellerId,quantity,createdDate,userId) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = VoucherGenerationAggregatorModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.denomination)
                        ps.setString(++index, row.resellerId)
                        ps.setLong(++index, row.quantity)
                        ps.setDate(++index, new java.sql.Date(row.createdDate.getTime()))
                        ps.setString(++index, row.userId)                      

                    },
                    getBatchSize: { VoucherGenerationAggregatorModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}