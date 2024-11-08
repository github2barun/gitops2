package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
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
public class BulkTopupAggregator extends AbstractAggregator {
	static final def TABLE = "bi.bulk_topup_aggregator"

	static final def index = "bulk_topup_tdr"

	@Autowired
	RestHighLevelClient client;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	@Qualifier("namedParameterJdbcTemplate")
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Value('${BulkTopupAggregator.bulkInsertionMode:false}')
	boolean bulkInsertionMode;

	@Transactional
	@Scheduled(cron = '${BulkTopupAggregator.cron:*/3 * * * * ?}')
	public void aggregate() {

		log.info("BulkTopupAggregator Aggregator started******************************************************************" + new Date());

		if(bulkInsertionMode){
			for(int i=9; i>=1;i--) {
				log.info("In Loop " + i);
				aggregateDataES(index, i);
			}
		} else{
			aggregateDataES(index, 1);
		}

		log.info("BulkTopupAggregator Aggregator ended********************************************************************************************");
	}

	private void aggregateDataES(String index, int i) {

		Map<String, Object> afterKey = null;
		SearchRequest searchRequest = new SearchRequest(index);

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

		String strGte = "now-"+i+"d/d";
		String strLt = "now-"+(i-1)+"d/d";

		if(i-1==0){
			strLt = "now/d";
		}
		log.info("strGte" + strGte);
		log.info("strLt" + strLt);

		BoolQueryBuilder  queryBuilder = QueryBuilders.boolQuery()
				.filter(QueryBuilders.rangeQuery("StartTime").gte(strGte).lt(strLt))
				.filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("ErsReference", "")))

		searchSourceBuilder.query(queryBuilder);
		searchSourceBuilder.size(getElasticSearchSize(queryBuilder));

		return searchSourceBuilder;
	}

	private Map<String, Object> generateResponse(SearchRequest searchRequest) {

		List<BulkTopupModel> payModelList = new ArrayList<>();
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

			SearchHits hits = searchResponse.getHits();
			SearchHit[] searchHits = hits.getHits();

			for (SearchHit searchHit : searchHits) {

				Map<String, String> sourceAsMap = searchHit.getSourceAsMap()

				String id = GenerateHash.createHashString(sourceAsMap.get("ErsReference"), sourceAsMap.get("EndTime"));

				BulkTopupModel model = new BulkTopupModel(
						id,
						sourceAsMap.get("BatchId"),
						sourceAsMap.get("ReceiverMSISDN"),
						sourceAsMap.get("SenderResellerName"),
						sourceAsMap.get("FileDate"),
						sourceAsMap.get("UploadedFileName"),
						Double.valueOf(sourceAsMap.get("RequestAmountValue")),
						Double.valueOf(sourceAsMap.get("SenderBalanceValueBefore")),
						Double.valueOf(sourceAsMap.get("SenderBalanceValueAfter")),
						sourceAsMap.get("TransactionType"),
						sourceAsMap.get("ResultStatus"),
						sourceAsMap.get("ErsReference"),
						sourceAsMap.get("DataVolume"));

				payModelList.add(model);
			}

			if(payModelList!=null && payModelList.size!=0) {

				insertAggregation(payModelList);
				log.info("inserted First time in table");
			}
		}
		return null;
	}

	private def insertAggregation(List transactions) {

		if (transactions.size() != 0) {
			log.info("********************reseller exits***********************")
			def sql = """
		 	INSERT INTO ${TABLE} 
		 	(id,batch_id,reseller_msisdn,reseller_name,file_load_time,file_name,requested_amount,balance_before,balance_after,transaction_type,result_status,ers_reference,data_volume)
		 	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) 
		 	ON DUPLICATE KEY UPDATE
		 	batch_id=VALUES(batch_id),
		 	reseller_msisdn=VALUES(reseller_msisdn),
		 	reseller_name=VALUES(reseller_name),
		 	file_load_time=VALUES(file_load_time),
		 	file_name=VALUES(file_name),
		 	requested_amount=VALUES(requested_amount),
		 	balance_before=VALUES(balance_before),
		 	balance_after=VALUES(balance_after),
		 	transaction_type=VALUES(transaction_type),
		 	result_status=VALUES(result_status),
		 	ers_reference=VALUES(ers_reference),
		 	data_volume=VALUES(data_volume)
			"""
			log.debug("Reseller sql : ${sql}")

			jdbcTemplate.batchUpdate(sql,[
					setValues: { ps, i ->
						def row = transactions[i]
						//def index = 0
						ps.setString(1,row.id)
						ps.setString(2,row.batchId)
						ps.setString(3,row.resellerMsisdn)
						ps.setString(4,row.resellerName)
						ps.setString(5,row.fileLoadTime)
						ps.setString(6,row.fileName)
						ps.setDouble(7,row.requestedAmount!=null?row.requestedAmount:0.0d)
						ps.setDouble(8,row.balanceBefore!=null?row.balanceBefore:0.0d)
						ps.setDouble(9,row.balanceAfter!=null?row.balanceAfter:0.0d)
						ps.setString(10,row.transactionType)
						ps.setString(11,row.resultStatus)
						ps.setString(12,row.ersReference)
						ps.setString(13,row.dataVolume)
					},
					getBatchSize: { transactions.size() }
			] as BatchPreparedStatementSetter)
			log.debug("Data inserted in ResellerBalance")
		}
	}

	private int getElasticSearchSize(BoolQueryBuilder  queryBuilder) {

		long size = 0;
		SearchResponse searchResponse = null;
		SearchRequest searchRequest = new SearchRequest(index);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		searchSourceBuilder.query(queryBuilder);
		searchRequest.source(searchSourceBuilder);

		try {
			searchResponse = client.search(searchRequest, COMMON_OPTIONS);
		} catch (Exception e) {
			log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
		}

		RestStatus status = null;
		if(searchResponse!=null){
			status = searchResponse.status();
		}
		log.debug("response status -------------" + status);

		if (status!=null && status == RestStatus.OK) {

			SearchHits hits = searchResponse.getHits();
			size = searchResponse.getHits().getTotalHits().value;
		}

		return size.intValue();
	}
}

class BulkTopupModel {

	private String id;
	private String batchId;
	private String resellerMsisdn;
	private String resellerName;
	private String fileLoadTime;
	private String fileName;
	private Double requestedAmount;
	private Double balanceBefore;
	private Double balanceAfter;
	private String transactionType;
	private String resultStatus;
	private String ersReference;
	private String dataVolume;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public String getResellerMsisdn() {
		return resellerMsisdn;
	}

	public void setResellerMsisdn(String resellerMsisdn) {
		this.resellerMsisdn = resellerMsisdn;
	}

	public String getResellerName() {
		return resellerName;
	}

	public void setResellerName(String resellerName) {
		this.resellerName = resellerName;
	}

	public String getFileLoadTime() {
		return fileLoadTime;
	}

	public void setFileLoadTime(String fileLoadTime) {
		this.fileLoadTime = fileLoadTime;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Double getRequestedAmount() {
		return requestedAmount;
	}

	public void setRequestedAmount(Double requestedAmount) {
		this.requestedAmount = requestedAmount;
	}

	public Double getBalanceBefore() {
		return balanceBefore;
	}

	public void setBalanceBefore(Double balanceBefore) {
		this.balanceBefore = balanceBefore;
	}

	public Double getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(Double balanceAfter) {
		this.balanceAfter = balanceAfter;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getResultStatus() {
		return resultStatus;
	}

	public void setResultStatus(String resultStatus) {
		this.resultStatus = resultStatus;
	}

	public String getErsReference() {
		return ersReference;
	}

	public void setErsReference(String ersReference) {
		this.ersReference = ersReference;
	}

	public String getDataVolume() {
		return dataVolume;
	}

	public void setDataVolume(String dataVolume) {
		this.dataVolume = dataVolume;
	}

	public BulkTopupModel(String id, String batchId, String resellerMsisdn, String resellerName, String fileLoadTime, String fileName, Double requestedAmount, Double balanceBefore, Double balanceAfter, String transactionType, String resultStatus, String ersReference, String dataVolume) {
		this.id = id;
		this.batchId = batchId;
		this.resellerMsisdn = resellerMsisdn;
		this.resellerName = resellerName;
		this.fileLoadTime = fileLoadTime;
		this.fileName = fileName;
		this.requestedAmount = requestedAmount;
		this.balanceBefore = balanceBefore;
		this.balanceAfter = balanceAfter;
		this.transactionType = transactionType;
		this.resultStatus = resultStatus;
		this.ersReference = ersReference;
		this.dataVolume = dataVolume;
	}
}