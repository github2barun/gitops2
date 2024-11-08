
package com.seamless.customer.bi.aggregator.aggregate

import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.PreparedStatementSetter
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
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
import org.springframework.beans.factory.annotation.Qualifier
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
public class ResellerEvoucherSalesSummaryReportAggregator extends AbstractAggregator {
    static final def TABLE = "reseller_evoucher_sales_day_wise"

    static class ResellerEvoucherSalesSummaryReportAggregatorModel {
		private String id;
        private String productId;
        private String resellerId;
        private String zone;
        private String group;
        private String subGroup;
        private Date date;
        private Long quantity;
        private Float amount;

        public ResellerEvoucherSalesSummaryReportAggregatorModel(String productId, String resellerId, String zone, String group, String subGroup, Date date, Long quantity, Float amount) {
            this.id = GenerateHash.createHashString(productId, resellerId, zone, group, subGroup, date.toString());
            this.productId = productId;
            this.resellerId = resellerId;
            this.zone = zone;
            this.group = group;
            this.subGroup = subGroup;
            this.date = date;
            this.quantity = quantity;
            this.amount = amount;
        }
		
		public String getId() {
            return id;
        }

        public String getProductId() {
            return productId;
        }

        public String getResellerId() {
            return resellerId;
        }

        public String getZone() {
            return zone;
        }

        public String getGroup() {
            return group;
        }

        public String getSubGroup() {
            return subGroup;
        }

        public Date getDate() {
            return date;
        }

        public long getQuantity() {
            return quantity;
        }

        public Float getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "ResellerEvoucherSalesSummaryReportAggregatorModel{" +
                    "id='" + id + '\'' +
                    ", productId=" + productId + '\'' +
                    ", resellerId='" + resellerId + '\'' +
                    ", zone='" + zone + '\'' +
                    ", group='" + group + '\'' +
                    ", subgroup='" + subgroup + '\'' +
                    ", date='" + date + '\'' +
                    ", quantity=" + quantity + '\'' +
                    ", amount=" + amount + '\'' +
                    '}';
        }		
	}

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("refill")
    protected JdbcTemplate refill;

    @Value('${ResellerEvoucherSalesSummaryReportAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ResellerEvoucherSalesSummaryReportAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ResellerEvoucherSalesSummaryReportAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${ResellerEvoucherSalesSummaryReportAggregator.profileId:VOS_PURCHASE,VOT_PURCHASE}')
    String profileId;

	@Value('${ResellerEvoucherSalesSummaryReportAggregator.transactionType:VOUCHER_PURCHASE}')
	String transactionType;

    @Value('${ResellerEvoucherSalesSummaryReportAggregator.product:VOS,VOT}')
	String product;

    @Transactional
    @Scheduled(cron = '${ResellerEvoucherSalesSummaryReportAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("ResellerEvoucherSalesSummaryReportAggregator Aggregator started***************************************************************************" + new Date());
		
		log.info("profileId: " + profileId);
		log.info("transactionType: " + transactionType);
		
        def profileIdList = profileId.split(",")
		def transactionTypeList = transactionType.split(",")
        def productList = product.split(",")
		
        if (bulkInsertionMode) {
			log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
			log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
				log.info("index: " + index.toString())
				
                //fetch data from ES
                try {
                    List<ResellerEvoucherSalesSummaryReportAggregatorModel> ResellerEvoucherSalesSummaryReportAggregatorModelES = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList, transactionTypeList, productList)
					
					long startTime = System.currentTimeMillis();
                    insertAggregation(ResellerEvoucherSalesSummaryReportAggregatorModelES);
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
					List<ResellerEvoucherSalesSummaryReportAggregatorModel> ResellerEvoucherSalesSummaryReportAggregatorModelES = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList, transactionTypeList, productList)
					
					long startTime = System.currentTimeMillis();
					insertAggregation(ResellerEvoucherSalesSummaryReportAggregatorModelES);
					log.info("insertAggregation() for index " + index.getIndexName() + " executed in " + (System.currentTimeMillis() - startTime) + " ms");
				}
                catch (Exception e){
                    log.error(e.getMessage())
                }
            }
        }

        log.info("ResellerEvoucherSalesSummaryReportAggregator Aggregator ended**************************************************************************");
    }


    private List<ResellerEvoucherSalesSummaryReportAggregatorModel> aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList, String[] transactionTypeList, String[] productList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList, transactionTypeList, productList);
        searchRequest.source(searchSourceBuilder);
        List<ResellerEvoucherSalesSummaryReportAggregatorModel> ResellerEvoucherSalesSummaryReportAggregatorModels = generateResponse(searchRequest);
        return ResellerEvoucherSalesSummaryReportAggregatorModels;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileID, String[] transactionTypeList, String[] productList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
                .field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("ProductId").field("ProductSKU").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerName").field("SenderResellerId").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResellerId").field("SenderMSISDN").missingBucket(true));
        
        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("ResellerEvoucherSalesSummaryReportAggregator",
                sources).size(200);

        compositeBuilder.subAggregation(AggregationBuilders.sum("Quantity").field("Count"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("Amount").field("TransactionAmount"))

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("ChainState", "Completed"))
                    .filter(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
                    .filter(QueryBuilders.termsQuery("ProductSKU", productList))                    
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("ChainState", "Completed"))
                    .filter(QueryBuilders.termsQuery("TransactionType", transactionTypeList))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
                    .filter(QueryBuilders.termsQuery("ProductSKU", productList))                  
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<ResellerEvoucherSalesSummaryReportAggregatorModel> generateResponse(SearchRequest searchRequest) {
		log.info("*******Request:::: " + searchRequest.toString())
		
        List<ResellerEvoucherSalesSummaryReportAggregatorModel> ResellerEvoucherSalesSummaryReportAggregatorModels = new ArrayList<>();
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
	            ParsedComposite parsedComposite = aggregations.asMap().get("ResellerEvoucherSalesSummaryReportAggregator");
	
	            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
	                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
	                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

	                Aggregation totalQuantityAggregration = aggregationMap.get("Quantity");
	                ParsedSum q = (ParsedSum) totalQuantityAggregration;

                    Aggregation totalAmountAggregration = aggregationMap.get("Amount");
	                ParsedSum a = (ParsedSum) totalAmountAggregration;

	                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));
	            
					String productId = emptyToNull(keyValuesMap.get("ProductId"));

					String resellerName = emptyToNull(keyValuesMap.get("ResellerName"));
                    String resellerId = emptyToNull(keyValuesMap.get("ResellerId"));

                    def RESELLER_INFO_SQL = "SELECT rgroup AS 'zone',subrgroup as 'group',subsubrgroup AS 'sub_group' FROM Refill.commission_receivers resellers where resellers.chain_store_id = ?";
                    def product_status_updates = refill.query(RESELLER_INFO_SQL,  [setValues: { pr ->
                        use(TimeCategory) {
                            pr.setString(1, resellerName)
                        }
                    }] as PreparedStatementSetter, new ColumnMapRowMapper())

                    String zone = emptyToNull(product_status_updates[0]?.zone?.toString());
                    String group = emptyToNull(product_status_updates[0]?.group?.toString());
                    String subGroup = emptyToNull(product_status_updates[0]?.sub_group?.toString());
					
	                ResellerEvoucherSalesSummaryReportAggregatorModel ResellerEvoucherSalesSummaryReportAggregatorModel = new ResellerEvoucherSalesSummaryReportAggregatorModel(productId, resellerId, zone, group, subGroup, dateTimeDay, (long)q.value(), (float)a.value());
	
	                ResellerEvoucherSalesSummaryReportAggregatorModels.add(ResellerEvoucherSalesSummaryReportAggregatorModel);
	            }
			} else {
				log.info("aggregations == null");
			}
        }

        return ResellerEvoucherSalesSummaryReportAggregatorModels;

    }

	private String emptyToNull(String s) {
		return ("".equals(s)) ? null : s;
	}
	
    private def insertAggregation(List ResellerEvoucherSalesSummaryReportAggregatorModels) {

        log.info("ResellerEvoucherSalesSummaryReportAggregator Aggregated into ${ResellerEvoucherSalesSummaryReportAggregatorModels.size()} rows.")
        if (ResellerEvoucherSalesSummaryReportAggregatorModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,productId,resellerId,zone,group1,subGroup,executionDate,quantity,amount) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), amount = VALUES(amount)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = ResellerEvoucherSalesSummaryReportAggregatorModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.productId)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.zone)
                        ps.setString(++index, row.group)
                        ps.setString(++index, row.subGroup)
                        ps.setDate(++index, new java.sql.Date(row.date.getTime()))
                        ps.setLong(++index, row.quantity)
                        ps.setBigDecimal(++index, row.amount)                      

                    },
                    getBatchSize: { ResellerEvoucherSalesSummaryReportAggregatorModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}