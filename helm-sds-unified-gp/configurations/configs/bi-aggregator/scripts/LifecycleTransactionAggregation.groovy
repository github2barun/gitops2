package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.rest.RestStatus
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import com.seamless.customer.bi.aggregator.util.DateUtil


/**
 *
 *
 *
 *
 */
@Slf4j
public class LifecycleTransactionAggregation extends AbstractAggregator {
	static final def TABLE = "lifecycle_transaction_aggregator"

	@Autowired
	RestHighLevelClient client;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Value('${LifecycleTransactionAggregation.profileId:CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP}')
	String profileId;

	@Value('${LifecycleTransactionAggregation.bulkInsertionMode:false}')
	boolean bulkInsertionMode;

	@Value('${LifecycleTransactionAggregation.bulkInsertionModeFromDateString:2020-08-03}')
	String bulkInsertionModeFromDateString;

	@Value('${LifecycleTransactionAggregation.bulkInsertionModeToDateString:2020-08-09}')
	String bulkInsertionModeToDateString;

	@Transactional
	@Scheduled(cron = '${LifecycleTransactionAggregation.cron:*/3 * * * * ?}')
	public void aggregate() {

		log.info("LifecycleTransactionAggregation Aggregator started*******************************************************************************" + new Date());
		def profileIdList = profileId.split(",")
		if (bulkInsertionMode) {

			log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
			log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

			List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

			for (String index : indices) {
				//fetch data from ES
				try {
					aggregateBalanceDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList);
					log.info("Balance transactions aggregated.")
					/*aggregateInventoryDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList);
					log.info("Inventory transactions aggregated.")*/
					Thread.sleep(50);
				} catch (InterruptedException e) {
					log.error(e.getMessage());
				}
				catch (Exception e) {
					log.error(e.getMessage())
				}

			}

		} else {
			List<ReportIndex> indices = DateUtil.getIndex();

			for (ReportIndex index : indices) {

				log.info(index.toString())
				//fetch data from ES
				aggregateBalanceDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList);
				log.info("Balance transactions aggregated.")
				/*aggregateInventoryDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList);
				log.info("Inventory transactions aggregated.")*/

			}
		}

		log.info("LifecycleTransactionAggregation Aggregator ended**********************************************************************");
	}

	private void aggregateInventoryDataES(String index, String fromDate, String toDate, String[] profileIdList) {
		SearchRequest searchRequest = new SearchRequest(index);
		SearchSourceBuilder searchSourceBuilder = fetchInventoryInput(fromDate, toDate, profileIdList);
		searchRequest.source(searchSourceBuilder);
		searchRequest.scroll(TimeValue.timeValueMinutes(10));


		//Sender
		String scrollId = generateInventoryResponse(searchRequest);
		SearchResponse searchScrollResponse = client.search(searchRequest, COMMON_OPTIONS);
		log.info("_________________Inventory hits size outside loop for the first time_____________________" + searchScrollResponse.getHits().size())
		//fetchScrollInput(firstScrollId);
		while (searchScrollResponse.getHits().size() != 0) {
			SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
			scrollRequest.scroll(TimeValue.timeValueSeconds(30));
			log.info("*******Scroll Request:::: " + scrollRequest.toString());
			try {
				searchScrollResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
			} catch (Exception e) {
				log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
			}


			log.info("_________________Inventory  hits size inside loop _____________________" + searchScrollResponse.getHits().size())

			scrollId = generateInventoryScrollResponse(searchScrollResponse);
		}

	}


	private void aggregateBalanceDataES(String index, String fromDate, String toDate, String[] profileIdList) {
		SearchRequest searchRequest = new SearchRequest(index);
		SearchSourceBuilder searchSourceBuilder = fetchBalanceInput(fromDate, toDate, profileIdList);
		searchRequest.source(searchSourceBuilder);
		searchRequest.scroll(TimeValue.timeValueMinutes(10));

		log.info("Getting result for time value: " + TimeValue.timeValueMinutes(10));

		//Sender
		String scrollId = generateBalanceResponse(searchRequest);
		SearchResponse searchScrollResponse = client.search(searchRequest, COMMON_OPTIONS);
		log.info("_________________Balance hits size outside loop for the first time_____________________" + searchScrollResponse.getHits().size())
		//fetchScrollInput(firstScrollId);
		while (searchScrollResponse.getHits().size() != 0) {
			SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
			scrollRequest.scroll(TimeValue.timeValueSeconds(30));
			log.info("*******Scroll Request:::: " + scrollRequest.toString());
			try {
				searchScrollResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
			} catch (Exception e) {
				log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
			}


			log.info("_________________Balance hits size inside loop _____________________" + searchScrollResponse.getHits().size())

			scrollId = generateBalanceScrollResponse(searchScrollResponse);
		}

	}

	private SearchSourceBuilder fetchInventoryInput(String fromDate, String toDate, String[] profileID) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		if (bulkInsertionMode) {
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("rootComponentName", "oms"))
					.filter(QueryBuilders.termsQuery("TransactionType.keyword", profileID))
			searchSourceBuilder.size(5000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
		} else {
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("rootComponentName", "oms"))
					.filter(QueryBuilders.termsQuery("TransactionType.keyword", profileID))
					.filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
			searchSourceBuilder.size(5000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
		}

		return searchSourceBuilder;
	}

	private SearchSourceBuilder fetchBalanceInput(String fromDate, String toDate, String[] profileID) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		if (bulkInsertionMode) {
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("TransactionType.keyword", profileID))
			searchSourceBuilder.size(5000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
		} else {
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
					.filter(QueryBuilders.termsQuery("TransactionType.keyword", profileID))
					.filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
			searchSourceBuilder.size(5000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
		}

		return searchSourceBuilder;
	}

	private String generateInventoryScrollResponse(SearchResponse searchScrollResponse) {

		if(searchScrollResponse == null) {
			log.info("scroll response is null");
			return null;
		}

		List<SenderTransactionModel> senderTransactionModels = new ArrayList<>();
		List<ReceiverTransactionModel> receiverTransactionModels = new ArrayList<>();
		RestStatus status = searchScrollResponse.status();
		log.info("scroll response status -------------" + status);



		if (status == RestStatus.OK) {
			SearchHits searchHits = searchScrollResponse.getHits();
			log.info("no of hits after 1st request" + searchHits.size());
			for (SearchHit searchHit : searchHits.getHits()) {

				Map<String, String> searchHitMap = searchHit.getSourceAsMap();

				if(searchHitMap.get("timestamp") == null) {
					log.info("Time Stamp is null, skipping.");
					continue;
				}

				Date transactionTime = DateFormatter.formatDate(searchHitMap.get("timestamp"));

				if(searchHitMap.get("oms.seller.id") != null) {
					String senderResellerId = GenerateHash.createHashString(searchHitMap.get("oms.seller.id"));
					SenderTransactionModel senderTransactionModel = new SenderTransactionModel(senderResellerId, searchHitMap.get("oms.seller.id"), null, transactionTime);
					senderTransactionModels.add(senderTransactionModel);
				}

				if(searchHitMap.get("oms.buyer.id") != null) {
					String receiverResellerid = GenerateHash.createHashString(searchHitMap.get("oms.buyer.id"));
					ReceiverTransactionModel receiverTransactionModel = new ReceiverTransactionModel(receiverResellerid, searchHitMap.get("oms.buyer.id"), null, transactionTime);
					receiverTransactionModels.add(receiverTransactionModel);
				}
			}

		}
		insertSenderAggregation(senderTransactionModels);
		insertReceiverAggregation(receiverTransactionModels);
		return searchScrollResponse.getScrollId();
	}

	private String generateInventoryResponse(SearchRequest searchRequest) {
		List<SenderTransactionModel> senderTransactionModels = new ArrayList<>();
		List<ReceiverTransactionModel> receiverTransactionModels = new ArrayList<>();
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, COMMON_OPTIONS);
		} catch (Exception e) {
			log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
		}

		if(searchResponse == null) {
			log.info("search response is null");
			return null;
		}

		log.info("*******Request:::: " + searchRequest.toString())
		RestStatus status = searchResponse.status();
		log.info("response status -------------" + status);


		if (status == RestStatus.OK) {
			SearchHits searchHits = searchResponse.getHits();
			for (SearchHit searchHit : searchHits.getHits()) {

				Map<String, String> searchHitMap = searchHit.getSourceAsMap();
				if(searchHitMap.get("timestamp") == null) {
					log.info("Time Stamp is null, skipping.");
					continue;
				}

				Date transactionTime = DateFormatter.formatDate(searchHitMap.get("timestamp"));

				if(searchHitMap.get("oms.seller.id") != null) {
					String senderResellerId = GenerateHash.createHashString(searchHitMap.get("oms.seller.id"));
					SenderTransactionModel senderTransactionModel = new SenderTransactionModel(senderResellerId, searchHitMap.get("oms.seller.id"), null, transactionTime);
					senderTransactionModels.add(senderTransactionModel);
				}

				if(searchHitMap.get("oms.buyer.id") != null) {
					String receiverResellerid = GenerateHash.createHashString(searchHitMap.get("oms.buyer.id"));
					ReceiverTransactionModel receiverTransactionModel = new ReceiverTransactionModel(receiverResellerid, searchHitMap.get("oms.buyer.id"), null, transactionTime);
					receiverTransactionModels.add(receiverTransactionModel);
				}
			}

			insertSenderAggregation(senderTransactionModels);
			insertReceiverAggregation(receiverTransactionModels);
			return searchResponse.getScrollId();
		}
	}

	private String generateBalanceScrollResponse(SearchResponse searchScrollResponse) {

		if(searchScrollResponse == null) {
			log.info("scroll response is null");
			return null;
		}

		List<SenderTransactionModel> senderTransactionModels = new ArrayList<>();
		List<ReceiverTransactionModel> receiverTransactionModels = new ArrayList<>();
		RestStatus status = searchScrollResponse.status();
		log.info("scroll response status -------------" + status);



		if (status == RestStatus.OK) {
			SearchHits searchHits = searchScrollResponse.getHits();
			log.info("no of hits after 1st request" + searchHits.size());
			for (SearchHit searchHit : searchHits.getHits()) {

				Map<String, String> searchHitMap = searchHit.getSourceAsMap();

				if(searchHitMap.get("timestamp") == null) {
					log.info("Time Stamp is null, skipping.");
					continue;
				}

				Date transactionTime = DateFormatter.formatDate(searchHitMap.get("timestamp"));
				if(searchHitMap.get("SenderResellerId") != null) {
					String senderResellerId = GenerateHash.createHashString(searchHitMap.get("SenderResellerId"));
					SenderTransactionModel senderTransactionModel = new SenderTransactionModel(senderResellerId, searchHitMap.get("SenderResellerId"), transactionTime, null);
					senderTransactionModels.add(senderTransactionModel);
				}
				if(searchHitMap.get("ReceiverResellerId") != null) {
					String receiverResellerid = GenerateHash.createHashString(searchHitMap.get("ReceiverResellerId"));
					ReceiverTransactionModel receiverTransactionModel = new ReceiverTransactionModel(receiverResellerid, searchHitMap.get("ReceiverResellerId"), transactionTime, null);
					receiverTransactionModels.add(receiverTransactionModel);
				}
			}

		}
		insertSenderAggregation(senderTransactionModels);
		insertReceiverAggregation(receiverTransactionModels);
		return searchScrollResponse.getScrollId();
	}

	private String generateBalanceResponse(SearchRequest searchRequest) {
		List<SenderTransactionModel> senderTransactionModels = new ArrayList<>();
		List<ReceiverTransactionModel> receiverTransactionModels = new ArrayList<>();
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, COMMON_OPTIONS);
		} catch (Exception e) {
			log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
		}

		if(searchResponse == null) {
			log.info("search response is null");
			return null;
		}

		log.info("*******Request:::: " + searchRequest.toString())
		RestStatus status = searchResponse.status();
		log.info("response status -------------" + status);


		if (status == RestStatus.OK) {
			SearchHits searchHits = searchResponse.getHits();
			log.info("search Hits : " + searchHits);
			for (SearchHit searchHit : searchHits.getHits()) {

				Map<String, String> searchHitMap = searchHit.getSourceAsMap();

				if(searchHitMap.get("timestamp") == null) {
					log.info("Time Stamp is null, skipping.");
					continue;
				}

				Date transactionTime = DateFormatter.formatDate(searchHitMap.get("timestamp"));
				if(searchHitMap.get("SenderResellerId") != null) {
					String senderResellerId = GenerateHash.createHashString(searchHitMap.get("SenderResellerId"));
					SenderTransactionModel senderTransactionModel = new SenderTransactionModel(senderResellerId, searchHitMap.get("SenderResellerId"), transactionTime, null);
					senderTransactionModels.add(senderTransactionModel);
				}
				if(searchHitMap.get("ReceiverResellerId") != null) {
					String receiverResellerid = GenerateHash.createHashString(searchHitMap.get("ReceiverResellerId"));
					ReceiverTransactionModel receiverTransactionModel = new ReceiverTransactionModel(receiverResellerid, searchHitMap.get("ReceiverResellerId"), transactionTime, null);
					receiverTransactionModels.add(receiverTransactionModel);
				}
			}

			insertSenderAggregation(senderTransactionModels);
			insertReceiverAggregation(receiverTransactionModels);
			return searchResponse.getScrollId();
		}
	}

	private def insertSenderAggregation(List<SenderTransactionModel> senderTransactionModelList) {

		log.info("senderTransactionModelList Aggregated into ${senderTransactionModelList.size()} rows.")
		if (senderTransactionModelList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,reseller_id,last_sender_balance_transaction,last_sender_inventory_transaction) VALUES (?,?,?,?)" +
					" ON DUPLICATE KEY UPDATE last_sender_balance_transaction = VALUES(last_sender_balance_transaction), last_sender_inventory_transaction = VALUES(last_sender_inventory_transaction) "
			log.info(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = senderTransactionModelList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setString(++index, row.resellerId)
						ps.setDate(++index, (row.lastSenderBalanceTransactionDate == null) ? null : new java.sql.Date(row.lastSenderBalanceTransactionDate.getTime()))
						ps.setDate(++index, (row.lastSenderInventoryTransactionDate == null) ? null : new java.sql.Date(row.lastSenderInventoryTransactionDate.getTime()))
					},
					getBatchSize: { senderTransactionModelList.size() }
			] as BatchPreparedStatementSetter)
		}

	}

	private def insertReceiverAggregation(List<ReceiverTransactionModel> receiverTransactionModelList) {

		log.info("senderTransactionModelList Aggregated into ${receiverTransactionModelList.size()} rows.")
		if (receiverTransactionModelList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,reseller_id,last_receiver_balance_transaction,last_receiver_inventory_transaction) VALUES (?,?,?,?)" +
					" ON DUPLICATE KEY UPDATE last_receiver_balance_transaction = VALUES(last_receiver_balance_transaction), last_receiver_inventory_transaction = VALUES(last_receiver_inventory_transaction)"
			log.info(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = receiverTransactionModelList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setString(++index, row.resellerId)
						ps.setDate(++index, (row.lastReceiverBalanceTransactionDate == null) ? null : new java.sql.Date(row.lastReceiverBalanceTransactionDate.getTime()))
						ps.setDate(++index, (row.lastReceiverInventoryTransactionDate == null) ? null : new java.sql.Date(row.lastReceiverInventoryTransactionDate.getTime()))
					},
					getBatchSize: { receiverTransactionModelList.size() }
			] as BatchPreparedStatementSetter)
		}

	}
}

class SenderTransactionModel {
	private String id;
	private String resellerId;
	private Date lastSenderBalanceTransactionDate;
	private Date lastSenderInventoryTransactionDate;

	SenderTransactionModel(String id, String resellerId,Date lastSenderBalanceTransactionDate, Date
			lastSenderInventoryTransactionDate) {
		this.id = id
		this.resellerId = resellerId
		this.lastSenderBalanceTransactionDate = lastSenderBalanceTransactionDate
		this.lastSenderInventoryTransactionDate = lastSenderInventoryTransactionDate
	}

	String getId() {
		return id
	}

	void setId(String id) {
		this.id = id
	}

	String getResellerId() {
		return resellerId
	}

	void setResellerId(String resellerId) {
		this.resellerId = resellerId
	}

	Date getLastSenderBalanceTransactionDate() {
		return lastSenderBalanceTransactionDate
	}

	void setLastSenderBalanceTransactionDate(Date lastSenderBalanceTransactionDate) {
		this.lastSenderBalanceTransactionDate = lastSenderBalanceTransactionDate
	}

	Date getLastSenderInventoryTransactionDate() {
		return lastSenderInventoryTransactionDate
	}

	void setLastSenderInventoryTransactionDate(Date lastSenderInventoryTransactionDate) {
		this.lastSenderInventoryTransactionDate = lastSenderInventoryTransactionDate
	}
}


class ReceiverTransactionModel {
	private String id;
	private String resellerId;
	private Date lastReceiverBalanceTransactionDate;
	private Date lastReceiverInventoryTransactionDate;

	ReceiverTransactionModel(String id, String resellerId, Date lastReceiverBalanceTransactionDate, Date lastReceiverInventoryTransactionDate) {
		this.id = id
		this.resellerId = resellerId
		this.lastReceiverBalanceTransactionDate = lastReceiverBalanceTransactionDate
		this.lastReceiverInventoryTransactionDate = lastReceiverInventoryTransactionDate
	}

	String getId() {
		return id
	}

	void setId(String id) {
		this.id = id
	}

	String getResellerId() {
		return resellerId
	}

	void setResellerId(String resellerId) {
		this.resellerId = resellerId
	}

	Date getLastReceiverBalanceTransactionDate() {
		return lastReceiverBalanceTransactionDate
	}

	void setLastReceiverBalanceTransactionDate(Date lastReceiverBalanceTransactionDate) {
		this.lastReceiverBalanceTransactionDate = lastReceiverBalanceTransactionDate
	}

	Date getLastReceiverInventoryTransactionDate() {
		return lastReceiverInventoryTransactionDate
	}

	void setLastSenderReceiverTransactionDate(Date lastReceiverInventoryTransactionDate) {
		this.lastReceiverInventoryTransactionDate = lastReceiverInventoryTransactionDate
	}
}
