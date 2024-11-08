
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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate
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
public class VouchersRedemptionAggregator extends AbstractAggregator {
	static final def TABLE = "voucher_redemption_denom_day_wise"
	static final def AGG_NAME = "VouchersRedemptionAggregator";
	
	static class ReportAggregation {
		private String id;
		private String denomination;
		private long quantity;
		private Date redemptionDate;

		public ReportAggregation(String denomination, long quantity, Date redemptionDate) {
			this.id = GenerateHash.createHashString(redemptionDate.toString(), denomination);
			this.denomination = denomination;
			this.quantity = quantity;
			this.redemptionDate = redemptionDate;
		}
		
		public String getId() {
			return id;
		}

		public String getDenomination() {
			return denomination;
		}

		public long getQuantity() {
			return quantity;
		}

		public Date getRedemptionDate() {
			return redemptionDate;
		}

		@Override
		public String toString() {
			return "ReportAggregation{" +
					"id='" + id + '\'' +
					", denomination=" + denomination + '\'' +
					", quantity='" + quantity + '\'' +
					", redemptionDate='" + redemptionDate + '\'' +
					'}';
		}
	}

	@Autowired
	RestHighLevelClient client;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Value('${VouchersRedemptionAggregator.bulkInsertionMode:false}')
	boolean bulkInsertionMode;

	@Value('${VouchersRedemptionAggregator.bulkInsertionModeFromDate:2020-08-03}')
	String bulkInsertionModeFromDate;

	@Value('${VouchersRedemptionAggregator.bulkInsertionModeToDate:2020-08-09}')
	String bulkInsertionModeToDate;

	@Value('${VouchersRedemptionAggregator.profileId:VOUCHER_REDEEM}')
	String profileId;

	@Value('${VouchersRedemptionAggregator.transactionType:TOPUP}')
	String transactionType;
	
	@Transactional
	@Scheduled(cron = '${VouchersRedemptionAggregator.cron:*/3 * * * * ?}')
	
	public void aggregate() {

		log.info(AGG_NAME + " Aggregator started***************************************************************************" + new Date());
		
		log.info("profileId: " + profileId);
		log.info("transactionType: " + transactionType);
		
		def profileIdList = profileId.split(",")
		def transactionTypeList = transactionType.split(",")
		
		if (bulkInsertionMode) {
			log.info("bulkInsertionModeFromDate: " + bulkInsertionModeFromDate);
			log.info("bulkInsertionModeToDate: " + bulkInsertionModeToDate);
			
			List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDate, bulkInsertionModeToDate);

			for (String index : indices) {
				log.info("index to be aggregated: " + index)
				
				try {
					List<ReportAggregation> aggList = aggregateDataES(index, bulkInsertionModeFromDate, bulkInsertionModeToDate, profileIdList, transactionTypeList)
					
					long startTime = System.currentTimeMillis();
					insertAggregation(aggList);
					log.info("insertAggregation() for index " + index + " executed in " + (System.currentTimeMillis() - startTime) + " ms");
				}
				catch (Exception e){
					log.error(e.getMessage())
				}
			}
		} else {
			List<ReportIndex> indices = DateUtil.getIndex();

			for (ReportIndex index : indices) {
				log.info("index to be aggregated: " + index.toString())
				
				try {
					List<ReportAggregation> aggList = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList, transactionTypeList)
					
					long startTime = System.currentTimeMillis();
					insertAggregation(aggList);
					log.info("insertAggregation() for index " + index.getIndexName() + " executed in " + (System.currentTimeMillis() - startTime) + " ms");
				}
				catch (Exception e){
					log.error(e.getMessage())
				}
			}
		}

		log.info(AGG_NAME + " Aggregator ended**************************************************************************" + new Date());
	}


	private List<ReportAggregation> aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList, String[] transactionTypeList) {
		SearchRequest searchRequest = new SearchRequest(index);
		SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList, transactionTypeList);
		searchRequest.source(searchSourceBuilder);
		List<ReportAggregation> aggList = generateResponse(searchRequest);
		return aggList;
	}

	private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileID, String[] transactionTypeList) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
				.field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);

		List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
		sources.add(dateHistoByDay);
		sources.add(new TermsValuesSourceBuilder("TransactionAmount").field("TransactionAmount").missingBucket(true));
		
		CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder(AGG_NAME, sources).size(200);

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

	private List<ReportAggregation> generateResponse(SearchRequest searchRequest) {
		log.info("searchRequest: " + searchRequest.toString())
		
		List<ReportAggregation> aggList = new ArrayList<>();
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
				ParsedComposite parsedComposite = aggregations.asMap().get(AGG_NAME);
	
				for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
					LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
					Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
					Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));
				
					String transactionAmount = keyValuesMap.get("TransactionAmount").toString();
					if (transactionAmount.endsWith(".0")) {
						transactionAmount = transactionAmount.substring(0, transactionAmount.length() - 2); // cut float 0 decimal (if any)
					}
					
					ReportAggregation aggregation = new ReportAggregation(transactionAmount, bucket.getDocCount(), dateTimeDay);
					log.info("reportAggregation: " + aggregation);
					
					aggList.add(aggregation);
				}
			} else {
				log.info("aggregations == null");
			}
		}

		return aggList;

	}

	private def insertAggregation(List<ReportAggregation> aggList) {

		log.info("${AGG_NAME} Aggregated into ${aggList.size()} rows.")

		if (aggList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,denomination,quantity,redemptionDate) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = aggList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setString(++index, row.denomination)
						ps.setLong(++index, row.quantity)
						ps.setDate(++index, new java.sql.Date(row.redemptionDate.getTime()))

					},
					getBatchSize: { aggList.size() }
			] as BatchPreparedStatementSetter)
		}
	}
}
