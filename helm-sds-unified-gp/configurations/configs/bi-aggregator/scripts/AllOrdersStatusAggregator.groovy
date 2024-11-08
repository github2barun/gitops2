package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.Timestamp

/**
 *
 *
 *
 *
 */
@Slf4j
class AllOrdersStatusAggregator extends ScrollableAbstractAggregator {

    static final def TABLE = "all_orders_status_aggregator"

    @Autowired
    RestHighLevelClient client

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${AllOrdersStatusAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode

    @Value('${AllOrdersStatusAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString

    @Value('${AllOrdersStatusAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString

    @Value('${AllOrdersStatusAggregator.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset

    @Value('${AllOrdersStatusAggregator.excludeOrderStatus:RETURN_DELIVER}')
    String excludeOrderStatus

    @Value('${AllOrdersStatusAggregator.scrollSize:7000}')
    int scrollSize

    @Transactional
    @Scheduled(cron = '${AllOrdersStatusAggregator.cron:*/3 * * * * ?}')
    void aggregate() {

        log.info("********** AllOrdersStatusAggregator Aggregator started at " + new Date())

        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString)
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString)

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<AllOrdersStatusSummaryModel> allOrdersStatusSummaryModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(allOrdersStatusSummaryModelList)
                    Thread.sleep(50)
                } catch (InterruptedException e) {
                    log.error(e.getMessage())
                }
                catch (Exception e) {
                    log.error(e.getMessage())
                }
            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex()

            for (ReportIndex index : indices) {

                log.info(index.toString())
                //fetch data from ES
                List<AllOrdersStatusSummaryModel> allOrdersStatusSummaryModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate())
                insertAggregation(allOrdersStatusSummaryModelList)
            }
        }

        log.info("********** AllOrdersStatusAggregator ended at " + new Date())
    }

    private List<AllOrdersStatusSummaryModel> aggregateDataES(String index, String fromDate, String toDate) {

        List<AllOrdersStatusSummaryModel> allOrdersStatusSummaryModelList = new ArrayList<>()

        SearchRequest searchRequest = new SearchRequest(index)
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, excludeOrderStatus)
        searchRequest.source(searchSourceBuilder)
        searchRequest.scroll(TimeValue.timeValueMinutes(5))
        log.info("*******Request:::: " + searchRequest.toString())
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client)

        if (searchResponse != null) {
            allOrdersStatusSummaryModelList = generateResponse(searchResponse)
            String scrollId = searchResponse.getScrollId()
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId)
                scrollRequest.scroll(TimeValue.timeValueMinutes(5))
                searchResponse = generateScrollSearchResponse(scrollRequest, client)
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    allOrdersStatusSummaryModelList.addAll(generateResponse(searchResponse))
                    scrollId = searchResponse.getScrollId()
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return allOrdersStatusSummaryModelList
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String excludeOrderStatus) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        String[] orderStatusList = excludeOrderStatus.split(",")
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("oms.eventName.keyword", eventName.split(",", -1)))
                .mustNot(QueryBuilders.termsQuery("oms.orderStatus.keyword", orderStatusList))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }

        searchSourceBuilder.query(queryBuilder).size(scrollSize)
        return searchSourceBuilder
    }

    private List<AllOrdersStatusSummaryModel> generateResponse(SearchResponse searchResponse) {
        List<AllOrdersStatusSummaryModel> allOrdersStatusSummaryModelArrayList = new ArrayList<>()
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status()
            log.debug("Response status -------------" + status)
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap()

                    //String transactionDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd")
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"))
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                    // user.userId is the initiator
                    String resellerId = searchHitMap.getOrDefault("user.userId", "N/A")
                    String orderStatus = searchHitMap.getOrDefault("oms.orderStatus", "N/A")
                    String orderType = searchHitMap.getOrDefault("oms.orderType", "N/A")
                    String resellerType = searchHitMap.getOrDefault("user.resellerType", "N/A")
                    String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A")
                    String quarterValue = searchHitMap.getOrDefault("Quarter", "N/A")
                    def month = searchHitMap.getOrDefault("month", "0")
                    def monthValue = !month.equals("N/A") ? month : 0

                    String buyerId = searchHitMap.getOrDefault("oms.buyer.id", "N/A")
                    String sellerId = searchHitMap.getOrDefault("oms.seller.id", "N/A")
                    String senderId = searchHitMap.getOrDefault("oms.sender.id", "N/A")
                    List<HashMap<String, String>> receivers = searchHitMap.getOrDefault("oms.receivers", null)
                    String receiverId = (receivers != null && !receivers.isEmpty()) ? (receivers[receivers.size() - 1].getOrDefault("id", "N/A")) : "N/A"
                    List<HashMap<String, String>> deliveryPath = searchHitMap.getOrDefault("oms.deliveryPaths", null)
                    String dropId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.drop.id", "N/A")) : "N/A"
                    String pickupId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.pickup.id", "N/A")) : "N/A"

                    String orderId = searchHitMap.getOrDefault("oms.orderId", "N/A")

                    String id = GenerateHash.createHashString(transactionNumber)

                    AllOrdersStatusSummaryModel allOrdersStatusSummaryModel = new AllOrdersStatusSummaryModel(id, orderId,
                            transactionDate, transactionNumber, monthValue as long, quarterValue, orderStatus, orderType,
                            resellerType, resellerId, buyerId, senderId, sellerId, receiverId, dropId, pickupId)

                    allOrdersStatusSummaryModelArrayList.add(allOrdersStatusSummaryModel)

                }
                return allOrdersStatusSummaryModelArrayList
            }
        }
    }

    private def insertAggregation(List<AllOrdersStatusSummaryModel> allOrdersStatusSummaryModelList) {

        log.info("AllOrdersStatusAggregator Aggregated into ${allOrdersStatusSummaryModelList.size()} rows.")
        if (allOrdersStatusSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,transaction_number,month_value,quarter,reseller_id,buyer_id," +
                    "seller_id,receiver_id,order_status,order_id,order_type,reseller_type,drop_location_id,pickup_location_id,sender_id)" +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allOrdersStatusSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionNumber)
                        ps.setLong(++index, row.monthValue)
                        ps.setString(++index, row.quarter)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.receiverId)
                        ps.setString(++index, row.orderStatus)
                        ps.setString(++index, row.orderId)
                        ps.setString(++index, row.orderType)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.dropId)
                        ps.setString(++index, row.pickupId)
                        ps.setString(++index, row.senderId)
                    },
                    getBatchSize: { allOrdersStatusSummaryModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class AllOrdersStatusSummaryModel {
    private String id
    private String orderId
    private Timestamp transactionDate
    private String transactionNumber
    private long monthValue
    private String quarter
    private String orderStatus
    private String orderType
    private String resellerType
    private String resellerId
    private String buyerId
    private String senderId
    private String sellerId
    private String receiverId
    private String dropId
    private String pickupId

    AllOrdersStatusSummaryModel(String id, String orderId, Timestamp transactionDate, String transactionNumber, long monthValue, String quarter, String orderStatus, String orderType, String resellerType, String resellerId, String buyerId, String senderId, String sellerId, String receiverId, String dropId, String pickupId) {
        this.id = id
        this.orderId = orderId
        this.transactionDate = transactionDate
        this.transactionNumber = transactionNumber
        this.monthValue = monthValue
        this.quarter = quarter
        this.orderStatus = orderStatus
        this.orderType = orderType
        this.resellerType = resellerType
        this.resellerId = resellerId
        this.buyerId = buyerId
        this.senderId = senderId
        this.sellerId = sellerId
        this.receiverId = receiverId
        this.dropId = dropId
        this.pickupId = pickupId
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Timestamp getTransactionDate() {
        return transactionDate
    }

    void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate
    }

    String getTransactionNumber() {
        return transactionNumber
    }

    void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber
    }

    long getMonthValue() {
        return monthValue
    }

    void setMonthValue(long monthValue) {
        this.monthValue = monthValue
    }

    String getQuarter() {
        return quarter
    }

    void setQuarter(String quarter) {
        this.quarter = quarter
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getBuyerId() {
        return buyerId
    }

    void setBuyerId(String buyerId) {
        this.buyerId = buyerId
    }

    String getSellerId() {
        return sellerId
    }

    void setSellerId(String sellerId) {
        this.sellerId = sellerId
    }

    String getReceiverId() {
        return receiverId
    }

    void setReceiverId(String receiverId) {
        this.receiverId = receiverId
    }

    String getOrderId() {
        return orderId
    }

    void setOrderId(String orderId) {
        this.orderId = orderId
    }

    String getOrderStatus() {
        return orderStatus
    }

    void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus
    }

    String getOrderType() {
        return orderType
    }

    void setOrderType(String orderType) {
        this.orderType = orderType
    }

    String getResellerType() {
        return resellerType
    }

    void setResellerType(String resellerType) {
        this.resellerType = resellerType
    }

    String getDropId() {
        return dropId
    }

    void setDropId(String dropId) {
        this.dropId = dropId
    }

    String getPickupId() {
        return pickupId
    }

    void setPickupId(String pickupId) {
        this.pickupId = pickupId
    }

    String getSenderId() {
        return senderId
    }

    void setSenderId(String senderId) {
        this.senderId = senderId
    }
}
