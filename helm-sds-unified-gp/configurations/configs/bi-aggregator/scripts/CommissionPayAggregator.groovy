package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import java.util.Date;
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.time.TimeCategory
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
import org.elasticsearch.search.aggregations.bucket.composite.*
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.metrics.ParsedSum
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
public class CommissionPayAggregator extends AbstractAggregator {
	static final def TABLE = "bi.commission_pay_out_aggregator"

	static final def index = "bank_payments"

	static final def FETCH_MAX_DATE_RECORDS = "select c1.* from commission_pay_out_aggregator c1 inner join (select reseller_id, max(current_banked_date) as latest_date from commission_pay_out_aggregator where  `reseller_id` in (:resellerId)  group by reseller_id) c2 on c1.reseller_id = c2.reseller_id and c1.current_banked_date = c2.latest_date";
	static final def FETCH_COMMISSION_BETWEEN_DATE_RECORDS = "select sum(transactionAmount) as sales_amount, sum(`reseller_commission`) as accumulated_commission from std_sales_trend_aggregation where transaction_type not in ('CREDIT_TRANSFER', 'TRANSFER') and `aggregationDate` between ? and ? and  resellerId = ?";
	@Autowired
	RestHighLevelClient client;
	//private static final string OPERATORNAME = "operator";
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	@Qualifier("namedParameterJdbcTemplate")
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Value('${CommissionPayAggregator.bulkInsertionMode:false}')
	boolean bulkInsertionMode;

	@Transactional
	@Scheduled(cron = '${CommissionPayAggregator.cron:*/3 * * * * ?}')
	public void aggregate() {

		log.info("CommissionPayAggregator Aggregator started******************************************************************" + new Date());

		//def profileIdList = profileId.split(",")

		///log.info(index.toString())
		if(bulkInsertionMode){
			for(int i=9; i>=1;i--) {
				log.info("In Loop " + i);
				aggregateDataES(index, i);
			}
		} else{
			aggregateDataES(index, 1);
		}


		log.info("CommissionPayAggregator Aggregator ended********************************************************************************************");
	}


	private void aggregateDataES(String index, int i) {
		//List<CommissionPayModel> searchResponse = new ArrayList<>();
		Map<String, Object> afterKey = null;
		SearchRequest searchRequest = new SearchRequest(index);
		List<CommissionPayModel> list = null;
		SearchSourceBuilder searchSourceBuilder = buildESQuery(i, afterKey);
		searchRequest.source(searchSourceBuilder);
		afterKey= generateResponse(searchRequest);
		if(afterKey!=null) {
			log.info("##############" +afterKey.size())
			while(afterKey!=null) {
				searchSourceBuilder = buildESQuery(i,afterKey);
				searchRequest.source(searchSourceBuilder);
				afterKey = generateResponse(searchRequest);
			}
		}
	}

	private SearchSourceBuilder buildESQuery(int i, Map<String, Object> afterKey) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
				.field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601");

		List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
		sources.add(dateHistoByDay);
		sources.add(new TermsValuesSourceBuilder("AgentId").field("AgentId").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("AgentName").field("AgentName").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("MerchantId").field("MerchantId").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("MerchantName").field("MerchantName").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("AgentBankCode").field("AgentBankCode").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("BankedAmountCurrency").field("BankedAmountCurrency").missingBucket(true));
		sources.add(new TermsValuesSourceBuilder("AgentMsisdn").field("agentMSISDN").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("PendingAmount").field("PendingAmount").missingBucket(true));


		CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("CommissionPayAggregator",sources).size(10000);

		compositeBuilder
				.subAggregation(AggregationBuilders.sum("BankedAmountValue").field("BankedAmountValue"))
				.subAggregation(AggregationBuilders.sum("AgentCommission").field("agentCommission"))
				.subAggregation(AggregationBuilders.sum("accumulatedCommission").field("AccumulatedCommission"))

		String strGte = "now-"+i+"d/d";
		String strLt = "now-"+(i-1)+"d/d";
		if(i-1==0){
			strLt = "now/d";
		}
		log.info("strGte" + strGte);
		log.info("strLt" + strLt);
		BoolQueryBuilder  queryBuilder = QueryBuilders.boolQuery()
				.filter(QueryBuilders.rangeQuery("EndTime").gte(strGte).lt(strLt))
				.filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
				.filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("AgentId", "")))
		searchSourceBuilder.query(queryBuilder);
		if(afterKey!=null){
			compositeBuilder.aggregateAfter(afterKey)
		}
		searchSourceBuilder.aggregation(compositeBuilder).size(0);

		return searchSourceBuilder;
	}

	private Map<String, Object> generateResponse(SearchRequest searchRequest) {
		List<CommissionPayModel> payModelList = new ArrayList<>();
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, COMMON_OPTIONS);
		} catch (Exception e) {
			log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
		}

		log.debug("*******Request:::: " + searchRequest.toString())
		RestStatus status = null;
		if(searchResponse!=null){
			status = searchResponse.status();
		}
		log.debug("response status -------------" + status);

		if (status!=null && status == RestStatus.OK) {
			Aggregations aggregations = searchResponse.getAggregations();
			ParsedComposite parsedComposite = aggregations.asMap().get("CommissionPayAggregator");

			for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
				LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
				Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

				Aggregation bankedAmountValue = aggregationMap.get("BankedAmountValue");
				Aggregation agentCommissionValue = aggregationMap.get("AgentCommission");
				Aggregation accumulatedCommissionValue = aggregationMap.get("accumulatedCommission");

				ParsedSum bankedAmount = (ParsedSum) bankedAmountValue;
				ParsedSum agentCommission = (ParsedSum) agentCommissionValue;
				ParsedSum accumulatedCommission = (ParsedSum) accumulatedCommissionValue;

				Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));

				String id = GenerateHash.createHashString(keyValuesMap.get("AgentId"), keyValuesMap.get("endTimeDay"));
				CommissionPayModel model = new CommissionPayModel(id,keyValuesMap.get("AgentId"),keyValuesMap.get("AgentName"),
						keyValuesMap.get("MerchantId"), keyValuesMap.get("MerchantName"),null,
						DateFormatter.formatDate(keyValuesMap.get("endTimeDay")),null,
						Double.valueOf(bankedAmount.getValue()),Double.valueOf(keyValuesMap.get("PendingAmount")),
						Double.valueOf(accumulatedCommission.getValue()),Double.valueOf(agentCommission.getValue()),
						null,keyValuesMap.get("AgentBankCode"), keyValuesMap.get("AgentMsisdn"));

				payModelList.add(model);
			}
			Map<String, Object> afterKey = parsedComposite.afterKey();

			if(payModelList!=null && payModelList.size!=0) {
				///Find records already present in mysql tables
				Map<String, Object> existingRecords = fetchMaxDateRecords(payModelList);

				///if reseller present then update previousdate and current date
				updateDatesPayModelList(payModelList, existingRecords);

				///now with previous and new date find the std_sales_aggregation
				updateAmountCommission(payModelList);

				insertAggregation(payModelList);
				log.info("inserted First time in table");
				return afterKey;
			}
		}
		return null;
	}

	private def insertAggregation(List resellers) {


		if (resellers.size() != 0) {
			log.info("********************reseller exits***********************")
			def sql = """
		 	INSERT INTO ${TABLE} 
		 	(id,reseller_name,partner_name,previous_banked_date,current_banked_date,sales_amount,banked_amount,pending_amount,accumulated_commission,receivable_commission,withheld_commission,banking_code, reseller_id, partner_id, reseller_msisdn)
		 	VALUES (?,?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?,?) 
		 	ON DUPLICATE KEY UPDATE
		 	accumulated_commission=VALUES(accumulated_commission),
		 	withheld_commission=VALUES(withheld_commission),
		 	receivable_commission=VALUES(receivable_commission),
		 	sales_amount=VALUES(sales_amount),
		 	banked_amount=VALUES(banked_amount),
		 	pending_amount=VALUES(pending_amount),
		 	banking_code=VALUES(banking_code),
		 	reseller_msisdn=VALUES(reseller_msisdn)
			"""
			log.debug("Reseller sql : ${sql}")

			jdbcTemplate.batchUpdate(sql,[
					setValues: { ps, i ->
						def row = resellers[i]
						//def index = 0
						ps.setString(1,row.id)
						ps.setString(2,row.resellerName)
						ps.setString(3,row.partnerName)
						ps.setDate(4,row.previousBankedDate!=null?new java.sql.Date(row.previousBankedDate.getTime()):null)
						ps.setDate(5,row.currentBankedDate!=null?new java.sql.Date(row.currentBankedDate.getTime()):null)
						ps.setDouble(6,row.salesAmount!=null?row.salesAmount:0.0d)
						ps.setDouble(7,row.bankedAmount!=null?row.bankedAmount:0.0d)
						ps.setDouble(8,row.pendingAmount!=null?row.pendingAmount:0.0d)
						ps.setDouble(9,row.accumulatedCommission!=null?row.accumulatedCommission:0.0d)
						ps.setDouble(10,row.receivableCommission!=null?row.receivableCommission:0.0d)
						ps.setDouble(11,row.withheldCommission!=null?row.withheldCommission:0.0d)
						ps.setString(12,row.bankingCode)
						ps.setString(13,row.resellerId)
						ps.setString(14,row.partnerId)
						ps.setString(15,row.resellerMsisdn)
					},
					getBatchSize: { resellers.size() }
			] as BatchPreparedStatementSetter)
			log.debug("Data inserted in ResellerBalance")
		}
	}


	private Map<String, Object> fetchMaxDateRecords(List<CommissionPayModel> payModelList){
		//find distinct agentName through stream

		Set<String> listResellerId = new HashSet<>();

		ListIterator payIterattor = payModelList.listIterator();
		while(payIterattor.hasNext()){
			CommissionPayModel commissionPayModel = (CommissionPayModel) payIterattor.next();
			listResellerId.add(commissionPayModel.getResellerId());
		}

		def maxDateRecords;

		Map<String, Object> maxDateRecordsByResselerName = new HashMap<>();
		if(listResellerId!=null && listResellerId.size()!=0) {
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("resellerId", listResellerId);

			maxDateRecords = namedParameterJdbcTemplate.queryForList(FETCH_MAX_DATE_RECORDS, parameters);

			ListIterator maxDateRecordIterattor = maxDateRecords.listIterator();
			while (maxDateRecordIterattor.hasNext()) {
				Map<String, Object> maxDateRecord = (Map<String, Object>) maxDateRecordIterattor.next();
				maxDateRecordsByResselerName.put(maxDateRecord.get("reseller_id"), maxDateRecord);
			}
		}
		return maxDateRecordsByResselerName;
	}

	private void updateDatesPayModelList(List payModelList, Map<String, Object> existingRecords){
		ListIterator payModelListIterator = payModelList.listIterator();
		while(payModelListIterator.hasNext()){
			CommissionPayModel commissionPayModel = (CommissionPayModel) payModelListIterator.next();
			// LinkedCaseInsensitiveMap<String, Object> resellerData =  existingRecords.get(commissionPayModel.getResellerId());
			def resellerData =  existingRecords.get(commissionPayModel.getResellerId());
			if(resellerData != null && resellerData.size()!=0) {
				commissionPayModel.setPreviousBankedDate(resellerData.get("current_banked_date"));
			}
		}
	}

	private void updateAmountCommission(List payModelList) {

		ListIterator payModelListIterator = payModelList.listIterator();
		while(payModelListIterator.hasNext()) {
			CommissionPayModel commissionPayModel = (CommissionPayModel) payModelListIterator.next();

			def commissionBetweenDateRecords;
			commissionBetweenDateRecords = jdbcTemplate.query(FETCH_COMMISSION_BETWEEN_DATE_RECORDS, [setValues: { ps ->
				use(TimeCategory) {
					ps.setDate(1, commissionPayModel.getPreviousBankedDate()!=null?new java.sql.Date(commissionPayModel.getPreviousBankedDate().getTime()):new java.sql.Date(0))
					ps.setDate(2, new java.sql.Date(commissionPayModel.getCurrentBankedDate().getTime()))
					ps.setString(3, commissionPayModel.getResellerId())
				}
			}] as PreparedStatementSetter, new ColumnMapRowMapper())

			if(commissionBetweenDateRecords!=null && commissionBetweenDateRecords.size()!=0){
				commissionPayModel.setSalesAmount(commissionBetweenDateRecords.get(0).get("sales_amount")!=null?commissionBetweenDateRecords.get(0).get("sales_amount"):0.0d);
				commissionPayModel.setPendingAmount(commissionPayModel.getPendingAmount()-commissionPayModel.getBankedAmount());
			}

			//if(commissionBetweenDateRecords!=null && commissionBetweenDateRecords.size()!=0 && commissionBetweenDateRecords.get(0).get("accumulated_commission")!=null){
				//commissionPayModel.setAccumulatedCommission(commissionBetweenDateRecords.get(0).get("accumulated_commission")!=null?commissionBetweenDateRecords.get(0).get("accumulated_commission"):0.0d);
				commissionPayModel.setWithheldCommission(commissionPayModel.getAccumulatedCommission()-commissionPayModel.getReceivableCommission());
		//	}
		}
	}
}

class CommissionPayModel {
	private String id;
	private String resellerId;
	private String resellerName;
	private String partnerId;
	private String partnerName;
	private Date previousBankedDate;
	private Date currentBankedDate;
	private Double salesAmount;
	private Double bankedAmount;
	private Double pendingAmount;
	private Double accumulatedCommission;
	private Double receivableCommission;
	private Double withheldCommission;
	private String bankingCode;
	private String resellerMsisdn;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getResellerId() {
		return resellerId;
	}

	public void setResellerId(String resellerId) {
		this.resellerId = resellerId;
	}

	public String getResellerName() {
		return resellerName;
	}

	public void setResellerName(String resellerName) {
		this.resellerName = resellerName;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public String getPartnerName() {
		return partnerName;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	public Date getPreviousBankedDate() {
		return previousBankedDate;
	}

	public void setPreviousBankedDate(Date previousBankedDate) {
		this.previousBankedDate = previousBankedDate;
	}

	public Date getCurrentBankedDate() {
		return currentBankedDate;
	}

	public void setCurrentBankedDate(Date currentBankedDate) {
		this.currentBankedDate = currentBankedDate;
	}

	public Double getSalesAmount() {
		return salesAmount;
	}

	public void setSalesAmount(Double salesAmount) {
		this.salesAmount = salesAmount;
	}

	public Double getBankedAmount() {
		return bankedAmount;
	}

	public void setBankedAmount(Double bankedAmount) {
		this.bankedAmount = bankedAmount;
	}

	public Double getPendingAmount() {
		return pendingAmount;
	}

	public void setPendingAmount(Double pendingAmount) {
		this.pendingAmount = pendingAmount;
	}

	public Double getAccumulatedCommission() {
		return accumulatedCommission;
	}

	public void setAccumulatedCommission(Double accumulatedCommission) {
		this.accumulatedCommission = accumulatedCommission;
	}

	public Double getReceivableCommission() {
		return receivableCommission;
	}

	public void setReceivableCommission(Double receivableCommission) {
		this.receivableCommission = receivableCommission;
	}

	public Double getWithheldCommission() {
		return withheldCommission;
	}

	public void setWithheldCommission(Double withheldCommission) {
		this.withheldCommission = withheldCommission;
	}

	public String getBankingCode() {
		return bankingCode;
	}

	public void setBankingCode(String bankingCode) {
		this.bankingCode = bankingCode;
	}

	public String getResellerMsisdn() {
		return resellerMsisdn;
	}

	public void setResellerMsisdn(String resellerMsisdn) {
		this.resellerMsisdn = resellerMsisdn;
	}

	public CommissionPayModel(String id, String resellerId, String resellerName, String partnerId, String partnerName, Date previousBankedDate, Date currentBankedDate, Double salesAmount, Double bankedAmount, Double pendingAmount, Double accumulatedCommission, Double receivableCommission, Double withheldCommission, String bankingCode, String resellerMsisdn) {
		this.id = id;
		this.resellerId = resellerId;
		this.resellerName = resellerName;
		this.partnerId = partnerId;
		this.partnerName = partnerName;
		this.previousBankedDate = previousBankedDate;
		this.currentBankedDate = currentBankedDate;
		this.salesAmount = salesAmount;
		this.bankedAmount = bankedAmount;
		this.pendingAmount = pendingAmount;
		this.accumulatedCommission = accumulatedCommission;
		this.receivableCommission = receivableCommission;
		this.withheldCommission = withheldCommission;
		this.bankingCode = bankingCode;
		this.resellerMsisdn = resellerMsisdn;
	}
}

