package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class DetailBalanceReport extends AbstractAggregator {
	static final def TABLE = "bi.detail_balance_report_aggregation"

	static final def QUERY_TRANSF_IN = "SELECT reseller_id as 'reseller_id',sum(count) as 'count_transfer_in',sum(amount) as 'balance_transfer_in',region,receiver_account_type as 'account_type' FROM bi.receiver_wise_credit_transfer_summary WHERE transaction_type IN ('CREDIT_TRANSFER')  GROUP By reseller_id,receiver_account_type "
	// static final def QUERY_TRANSF_OUT = "SELECT senderResellerID as 'resellerId',count(*) as 'countTransferOut',sum(amount) as 'balanceTransferOut' FROM dataaggregator.std_daily_transaction_summary_aggregation WHERE transactionType IN ('TOPUP','CREDIT_TRANSFER','PURCHASE') AND resultStatus='Success' AND DATE_FORMAT(transactionDate,'%Y-%m-%d') =DATE_FORMAT(DATE_SUB(NOW(),INTERVAL 1 DAY),'%Y-%m-%d') GROUP By senderResellerID "
	static final def QUERY_TRANSF_OUT = "SELECT resellerId as 'reseller_id',sum(count) as 'count_transfer_out',sum(transactionAmount) as 'balance_transfer_out',region,account_type as 'account_type', max(aggregationDate) as 'aggregation_date'  FROM bi.std_sales_trend_aggregation WHERE transaction_type IN ('TOPUP','CREDIT_TRANSFER','PURCHASE')  GROUP By resellerId,account_type "
	static final def QUERY_CLOSING_BALANCE = "SELECT distinct accountId,balance FROM accountmanagement.accounts"
	static final def QUERY_OPENING_BALANCE = "SELECT distinct account_Id,closing_Balance FROM ${TABLE} where account_Id IS NOT NULL AND closing_Balance IS NOT NULL"

	@Autowired
	RestHighLevelClient client;
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	//fetch config setting for type of data
	@Value('${DetailBalanceReport.hourwisedata:true}')
	boolean hourwise;
	@Value('${DetailBalanceReport.hour:10}')
	int hours;

	@Value('${DetailBalanceReport.timeOffset:+0h}')
	String timeOffset;

	@Value('${DetailBalanceReport.profileId:CREDIT_TRANSFER}')
	String profileId;

	@Value('${DetailBalanceReport.bulkInsertionMode:false}')
	boolean bulkInsertionMode;

	@Value('${HourlyTotal.bulkInsertionModeFromDateString:2020-08-03}')
	String bulkInsertionModeFromDateString;

	@Value('${HourlyTotal.bulkInsertionModeToDateString:2020-08-09}')
	String bulkInsertionModeToDateString;

	@Transactional
	@Scheduled(cron = '${DetailBalanceReport.cron:*/3 * * * * ?}')


	public void aggregate() {


		log.info("DetailBalanceReport Aggregator started******************************************************************" + new Date());

		def profileIdList = profileId.split(",")

		if (bulkInsertionMode) {

			log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
			log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

			List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

			for (String index : indices) {
				//fetch data from ES
				try {
					aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList)
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
				aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(),profileIdList)

			}
		}

		log.info("DetailBalanceReport Aggregator ended********************************************************************************************");
	}


	private void aggregateDataES(String index, String fromDate, String toDate,String[] profileIdList)
	{
		SearchRequest searchRequest = new SearchRequest(index);
		Map<String, Object> afterKey = null;
		SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate,afterKey,profileIdList);
		searchRequest.source(searchSourceBuilder);
		afterKey = generateResponse(searchRequest);

		if(afterKey!=null)
		{
			log.info("##############" +afterKey.size())
			while(afterKey!=null)
			{
				searchSourceBuilder = fetchInput(fromDate, toDate,afterKey,profileIdList);
				searchRequest.source(searchSourceBuilder);
				afterKey = generateResponse(searchRequest);
			}
		}

		def reseller_opening_balance = jdbcTemplate.queryForList(QUERY_OPENING_BALANCE)
		log.info("Opening Balance = ${reseller_opening_balance}")
		updateResellerOpeningBalance(reseller_opening_balance)

		def reseller_transf_in = jdbcTemplate.queryForList(QUERY_TRANSF_IN)
		log.debug("Transfer In = ${reseller_transf_in}")
		updateResellerTransfIn(reseller_transf_in)


		def reseller_transf_out = jdbcTemplate.queryForList(QUERY_TRANSF_OUT)
		log.debug("Transfer Out = ${reseller_transf_out}")
		updateResellerTransfOut(reseller_transf_out)

		def reseller_closing_balance = jdbcTemplate.queryForList(QUERY_CLOSING_BALANCE)
		updateResellerClosingBalance(reseller_closing_balance)
		log.info("Closing Balance = ${reseller_closing_balance}")

	}

	private SearchSourceBuilder fetchInput(String fromDate, String toDate,Map<String, Object> afterKey,String[] profileID) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
				.field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);
		DateHistogramValuesSourceBuilder dateHistoByHour = new DateHistogramValuesSourceBuilder("endTimeHour")
				.field("timestamp").fixedInterval(DateHistogramInterval.hours(1)).format("iso8601").missingBucket(true);

		List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
		sources.add(dateHistoByDay);
		sources.add(dateHistoByHour);

		sources.add(new TermsValuesSourceBuilder("senderResellerId").field("senderResellerId.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderResellerName").field("senderResellerName.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderMSISDN").field("senderMSISDN.keyword").missingBucket(true));
		// Reseller Status is not available for now.
		sources.add(new TermsValuesSourceBuilder("senderResellerType").field("senderResellerType.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("openingBalance").field("senderBalanceValueBefore.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("currentBalance").field("senderBalanceValueAfter.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderAccountId").field("senderResellerId.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("resellerPath").field("senderResellerPath.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderAccountType").field("senderAccountType.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderRegion").field("senderRegion.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("currency").field("currency.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("channel").field("channel.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("resultStatus").field("resultMessage.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("transactionType").field("transactionProfile.keyword").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("senderRegionId").field("senderRegionId.keyword").missingBucket(true))

		CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("DetailBalance",
				sources).size(10000);

		compositeBuilder.subAggregation(AggregationBuilders.sum("RequestAmountValue").field("transactionAmount"))

		if (!bulkInsertionMode) {
			QueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("resultMessage.keyword", "SUCCESS"))
					.filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileID))
					.filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
			searchSourceBuilder.query(queryBuilder);
		}
		else {
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("resultMessage.keyword", "SUCCESS"))
					.filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
			searchSourceBuilder.query(queryBuilder);
		}
		if(afterKey!=null){
			compositeBuilder.aggregateAfter(afterKey)
		}
		searchSourceBuilder.aggregation(compositeBuilder).size(0);

		return searchSourceBuilder;
	}

	private Map<String, Object> generateResponse(SearchRequest searchRequest) {
		List<DetailBalanceReportModel> resellers = new ArrayList<>();
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, COMMON_OPTIONS);
		} catch (Exception e) {
			log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
		}

		log.debug("*******Request:::: " + searchRequest.toString())
		RestStatus status = searchResponse.status();
		log.debug("response status -------------" + status);


		if (status == RestStatus.OK && searchResponse.getAggregations() != null) {

			Aggregations aggregations = searchResponse.getAggregations();
			ParsedComposite parsedComposite = aggregations.asMap().get("DetailBalance");

			for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
				LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
				Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
				Aggregation totalAmountAggregration = aggregationMap.get("RequestAmountValue");
				ParsedSum p = (ParsedSum) totalAmountAggregration;
				Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));
				Date dateTimeHour = DateFormatter.formatDate(keyValuesMap.get("endTimeHour"));
				String id = GenerateHash.createHashString(keyValuesMap.get("senderResellerId"));
				String parentAccountId = null;
				String resellerStatus = null;
				Date transactionDate = null;

				String path = keyValuesMap.get("resellerPath");
				String parent = path;

				if(path.contains("/")){
					String[] pathArray = path.split("/");
					parent = pathArray[pathArray.length-2]
				}
				String bal = "0";
				if(!"".equals(keyValuesMap.get("currentBalance"))
						&& keyValuesMap.get("currentBalance") != null){
					bal = keyValuesMap.get("currentBalance")
				}
				DetailBalanceReportModel reseller = new DetailBalanceReportModel(id, keyValuesMap.get("senderResellerId"), keyValuesMap.get("senderResellerName")
						, keyValuesMap.get("senderMSISDN"), keyValuesMap.get("senderAccountId"), keyValuesMap.get("resellerPath")
						, parentAccountId, Double.valueOf(bal),resellerStatus, keyValuesMap.get("senderResellerType")
						, keyValuesMap.get("currency"), transactionDate, parent, keyValuesMap.get("senderAccountType"), keyValuesMap.get("senderRegionId"));

				resellers.add(reseller);
			}
			log.debug("loop finish******************");

			insertAggregation(resellers);
			return parsedComposite.afterKey();
		}
		return null;

	}

	private def insertAggregation(List resellers)
	{

		log.info("HourlyTotal Aggregated into ${resellers.size()} rows.")
		if (resellers.size() != 0) {
			log.info("********************reseller exits***********************")
			def sql = """
		 	INSERT INTO ${TABLE}
		 	(id,reseller_id,reseller_name,msisdn,reseller_parent,reseller_status,reseller_type,current_balance,account_id,reseller_path,currency,region,account_type_id)
		 	VALUES (?,?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?)
		 	ON DUPLICATE KEY UPDATE
		 	reseller_name=VALUES(reseller_name),
			msisdn=VALUES(msisdn),
		 	reseller_parent=VALUES(reseller_parent),
		 	reseller_status=VALUES(reseller_status),
            reseller_type=VALUES(reseller_type),
		 	current_balance=VALUES(current_balance),
            account_id=VALUES(account_id),
            reseller_path=VALUES(reseller_path)
		 	"""
			log.debug("Reseller sql : ${sql}")

			jdbcTemplate.batchUpdate(sql,[
					setValues: { ps, i ->
						def row = resellers[i]
						ps.setString(1,row.id)
						ps.setString(2,row.resellerId)
						ps.setString(3,row.resellerName)
						ps.setString(4,row.msisdn)
						ps.setString(5,row.parent)
						ps.setString(6,row.resellerStatus)
						ps.setString(7,row.resellerTypeName)
						ps.setDouble(8,row.resellerBalance)
						ps.setString(9,row.accountId)
						ps.setString(10,row.resellerPath)
						ps.setString(11,row.currency)
						ps.setString(12,row.region)
						ps.setString(13,row.accountTypeId)
					},
					getBatchSize: { resellers.size() }
			] as BatchPreparedStatementSetter)
			log.debug("Data inserted in ResellerBalance")
		}

	}

	private def updateResellerTransfIn(List aggregation)
	{
		log.debug("Start Updating Reseller Transfer In ")
		if(aggregation)
		{

			def sql="UPDATE ${TABLE} SET count_transfer_in=?,balance_transfer_in=?,region=? WHERE reseller_id =? and account_type_id=?"
			//aggregation.eachWithIndex { row, index ->

			jdbcTemplate.batchUpdate(sql, [setValues: { ps, i ->
				def row = aggregation[i]
				ps.setInt(1, row.count_transfer_in.toInteger())
				ps.setDouble(2, row.balance_transfer_in)
				ps.setString(3, row.region)
				ps.setString(4, row.reseller_id.toString())
				ps.setString(5, row.account_type)
			}, getBatchSize: { aggregation.size() }] as BatchPreparedStatementSetter)
			//}
		}
		log.debug("Finish Updating Reseller Transfer In ")
	}

	private def updateResellerOpeningBalance(List aggregation)
	{
		log.debug("Updating Opening Balance")
		if(aggregation)
		{
			def sql = "UPDATE ${TABLE} SET opening_balance=? WHERE reseller_id = ? "
			jdbcTemplate.batchUpdate(sql, [setValues: { ps, i ->
				def row = aggregation[i]
				ps.setDouble(1, row.closing_Balance)
				ps.setString(2, row.account_Id.toString())
			}, getBatchSize: { aggregation.size() }] as BatchPreparedStatementSetter)

			log.debug("Finish Updating Opening Balance")
		}
	}

	private def updateResellerTransfOut(List aggregation_trans_out)
	{
		if(aggregation_trans_out)
		{
			log.debug("Start Updating Balance Transfer Out")

			def sql_transf_out="UPDATE ${TABLE} SET count_transfer_out=?,balance_transfer_out=?,region=?,last_transaction_date=? WHERE reseller_id =? and account_type_id=?"

			jdbcTemplate.batchUpdate(sql_transf_out, [setValues: { prep_st, i ->
				def row = aggregation_trans_out[i]
				prep_st.setInt(1, row.count_transfer_out.toInteger())
				prep_st.setDouble(2, row.balance_transfer_out)
				prep_st.setString(3, row.region)
				prep_st.setTimestamp(4, new java.sql.Timestamp(row.aggregation_date.getTime()))
				prep_st.setString(5, row.reseller_id.toString())
				prep_st.setString(6, row.account_type)

			}, getBatchSize: { aggregation_trans_out.size() }] as BatchPreparedStatementSetter)

			log.debug("Finish Updating Balance Transfer Out")
		}


	}

	private def updateResellerClosingBalance(List aggregation)
	{
		log.debug("Updating Closing Balance")
		if(aggregation)
		{
			def sql = "UPDATE ${TABLE} SET closing_balance = ? WHERE reseller_id=? "
			jdbcTemplate.batchUpdate(sql, [setValues: { ps, i ->
				def row = aggregation[i]
				ps.setDouble(1, row.balance)
				ps.setString(2, row.accountId.toString())
			}, getBatchSize: { aggregation.size() }] as BatchPreparedStatementSetter)

			log.debug("Finish Updating Closing Balance")

		}
	}

}
class DetailBalanceReportModel
{
	private String id;
	private String resellerId;
	private String resellerName;
	private String msisdn;
	private String accountId;
	private String resellerPath;
	private String parentAccountId;
	private Double resellerBalance;
	private String resellerStatus;
	private String resellerTypeName;
	private String currency;
	private Date transactionDate;
	private String parent;
	private String accountTypeId;
	private String region

	public DetailBalanceReportModel(String id,String resellerId, String resellerName, String msisdn, String accountId,
									String resellerPath, String parentAccountId, Double resellerBalance,String resellerStatus
									,String resellerTypeName,String currency,Date transactionDate,String parent,String accountTypeId, String region)
	{
		super();
		this.id = id;
		this.resellerId = resellerId;
		this.resellerName = resellerName;
		this.msisdn = msisdn;
		this.accountId = accountId;
		this.resellerPath = resellerPath;
		this.parentAccountId = parentAccountId;
		this.resellerBalance = resellerBalance;
		this.resellerStatus = resellerStatus;
		this.resellerTypeName =resellerTypeName;
		this.currency=currency;
		this.transactionDate = transactionDate;
		this.parent = parent;
		this.accountTypeId = accountTypeId;
		this.region = region
	}

}
