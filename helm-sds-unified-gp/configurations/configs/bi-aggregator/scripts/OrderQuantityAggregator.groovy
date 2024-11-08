package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import com.seamless.customer.bi.aggregator.util.Validations
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
public class OrderQuantityAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "orders_quantity_aggregator"

    @Autowired
    RestHighLevelClient client

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${OrderQuantityAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode

    @Value('${OrderQuantityAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString

    @Value('${OrderQuantityAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString

    @Value('${OrderQuantityAggregator.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${OrderQuantityAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** OrderQuantityAggregator Aggregator started at " + new Date())
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString)
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString)

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<OrderQuantityModel> orderQuantityModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(orderQuantityModels)
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
                List<OrderQuantityModel> orderQuantityModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate())
                insertAggregation(orderQuantityModels)
            }
        }
        log.info("********** OrderQuantityAggregator Aggregator ended at " + new Date())
    }


    private List<OrderQuantityModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<OrderQuantityModel> orderQuantityModelList = new ArrayList<>();

        SearchRequest searchRequest = new SearchRequest(index)
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate)
        searchRequest.source(searchSourceBuilder)
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            orderQuantityModelList = generateResponse(searchResponse)
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    orderQuantityModelList.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return orderQuantityModelList
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().
                filter(QueryBuilders.termsQuery("oms.eventName.keyword", eventName.split(",", -1)))
                .filter(QueryBuilders.termsQuery("resultCode", 0))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize)
        return searchSourceBuilder
    }

    private List<OrderQuantityModel> generateResponse(SearchResponse searchResponse) {
        List<OrderQuantityModel> orderQuantityModelList = new ArrayList<>()
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status()
            log.debug("response status -------------" + status)
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap()
                    List<HashMap<String, String>> omsItems = searchHitMap.getOrDefault("oms.items", null)
                    if (omsItems != null && (!omsItems.isEmpty())) {
                        Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"))
                        Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())
                        String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A") as String
                        String resellerId = searchHitMap.getOrDefault("user.userId", "N/A")
                        String buyerId = searchHitMap.getOrDefault("oms.buyer.id", "N/A")
                        String sellerId = searchHitMap.getOrDefault("oms.sender.id", "N/A")
                        Map<String, String> additionalFields = searchHitMap.getOrDefault("oms.additionalFields", "N/A")
                        String routeId = "N/A"
                        if (additionalFields != "N/A") {
                            routeId = additionalFields.getOrDefault("routeInfo", "N/A")
                        }
                        List<HashMap<String, String>> receivers = searchHitMap.getOrDefault("oms.receivers", null)
                        String receiverId = (receivers != null && !receivers.isEmpty()) ? (receivers[receivers.size() - 1].getOrDefault("id", "N/A")) : "N/A"

                        String senderId = searchHitMap.getOrDefault("oms.sender.id", "N/A")
                        List<HashMap<String, String>> deliveryPath = searchHitMap.getOrDefault("oms.deliveryPaths", null)
                        String dropId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.drop.id", "N/A")) : "N/A"
                        String pickupId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.pickup.id", "N/A")) : "N/A"

                        String orderType = searchHitMap.getOrDefault("oms.orderType", "N/A")
                        int item = 1
                        for (HashMap<String, String> omsItem : omsItems) {

                            String productCode = omsItem.getOrDefault("productCode", "N/A")
                            String productSku = omsItem.getOrDefault("productSku", "N/A")
                            String productDescription = omsItem.getOrDefault("productDescription", "N/A")
                            long quantity = 0
                            String reserveType = omsItem.getOrDefault("reserveType", "N/A")

                            if (reserveType != null && (!reserveType.equals(""))) {
                                if (reserveType.equals("SERIAL")) {
                                    String[] serial = omsItem.getOrDefault("serials", null)
                                    if (serial != null) {
                                        quantity = serial.length as long
                                    }
                                } else if (reserveType.equals("NON_SERIAL")) {
                                    quantity = ((omsItem.getOrDefault("quantity", "0") as String).replaceAll("[,]*", "")) as long
                                } else if (reserveType.equals("RANGE")) {
                                    List<HashMap<String, String>> ranges = omsItem.getOrDefault("ranges", null)
                                    if (ranges != null) {
                                        for (HashMap<String, String> range : ranges) {
                                            quantity += ((range.getOrDefault("endSerial", "0") as long) - (range.getOrDefault("startSerial", "0") as long)) + 1
                                        }
                                    }
                                }
                            }
                            String id = GenerateHash.createHashString(transactionNumber, item as String)


                            OrderQuantityModel orderQuantityModel
                            orderQuantityModel = new OrderQuantityModel(id, transactionDate, transactionNumber, quantity,
                                    Validations.stringValidation(productCode),
                                    Validations.stringValidation(productSku), Validations.stringValidation(productDescription), resellerId,
                                    Validations.stringValidation(orderType), Validations.stringValidation(buyerId), Validations.stringValidation(sellerId), Validations.stringValidation(receiverId), dropId, pickupId, senderId, Validations.stringValidation(routeId))

                            orderQuantityModelList.add(orderQuantityModel)
                            item++
                        }
                    }
                }
                return orderQuantityModelList
            }
        }
    }


    private def insertAggregation(List orderQuantityModelsList) {

        log.info("OrderQuantityAggregator Aggregated into ${orderQuantityModelsList.size()} rows.")
        if (orderQuantityModelsList.size() != 0) {
            def sql = """
                    INSERT INTO ${TABLE}
                     (id,transaction_date,transaction_number,quantity,product_code,product_sku,product_description,
                     reseller_id,order_type,buyer_id,seller_id,receiver_id,sender_id,drop_location_id,pickup_location_id, route_id)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)
                    """
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = orderQuantityModelsList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionNumber)
                        ps.setLong(++index, row.quantity)
                        ps.setString(++index, row.productCode)
                        ps.setString(++index, row.productSku)
                        ps.setString(++index, row.productDescription)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.orderType)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.receiverId)
                        ps.setString(++index, row.senderId)
                        ps.setString(++index, row.dropId)
                        ps.setString(++index, row.pickupId)
                        ps.setString(++index, row.routeId)
                    },
                    getBatchSize: { orderQuantityModelsList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class OrderQuantityModel {
    private String id
    private Timestamp transactionDate
    private String transactionNumber
    private long quantity
    private String productCode
    private String productSku
    private String productDescription
    private String resellerId
    private String orderType
    private String buyerId
    private String sellerId
    private String receiverId
    private String dropId
    private String pickupId
    private String senderId
    private String routeId;

    OrderQuantityModel(String id, Timestamp transactionDate, String transactionNumber, long quantity, String productCode, String productSku, String productDescription, String resellerId, String orderType, String buyerId, String sellerId, String receiverId, String dropId, String pickupId, String senderId, String routeId) {
        this.id = id
        this.transactionDate = transactionDate
        this.transactionNumber = transactionNumber
        this.quantity = quantity
        this.productCode = productCode
        this.productSku = productSku
        this.productDescription = productDescription
        this.resellerId = resellerId
        this.orderType = orderType
        this.buyerId = buyerId
        this.sellerId = sellerId
        this.receiverId = receiverId
        this.dropId = dropId
        this.pickupId = pickupId
        this.senderId = senderId
        this.routeId = routeId
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

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getProductCode() {
        return productCode
    }

    void setProductCode(String productCode) {
        this.productCode = productCode
    }

    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }

    long getQuantity() {
        return quantity
    }

    void setQuantity(long quantity) {
        this.quantity = quantity
    }

    String getProductDescription() {
        return productDescription
    }

    void setProductDescription(String productDescription) {
        this.productDescription = productDescription
    }

    String getOrderType() {
        return orderType
    }

    void setOrderType(String orderType) {
        this.orderType = orderType
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


    String getRouteId() {
        return routeId
    }

    void setRouteId(String routeId) {
        this.routeId = routeId
    }

}