package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
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
//@DynamicMixin
public class AllOrdersForLiveMapAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "all_orders_live_map_aggregator"
    static final def ZERO = "0"
    static final def DELIMITER = ","

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${AllOrdersForLiveMapAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${AllOrdersForLiveMapAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${AllOrdersForLiveMapAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${AllOrdersForLiveMapAggregator.eventName:ORDER_ACTION}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${AllOrdersForLiveMapAggregator.excludeOrderStatus:RETURN_DELIVER}')
    String excludeOrderStatus;

    @Value('${AllOrdersForLiveMapAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${AllOrdersForLiveMapAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** AllOrdersForLiveMapAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<AllOrdersSummaryModel> allOrdersSummaryModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(allOrdersSummaryModelList);
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
                List<AllOrdersSummaryModel> allOrdersSummaryModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(allOrdersSummaryModelList);
            }
        }

        log.info("********** AllOrdersForLiveMap Aggregator ended at " + new Date());
    }

    private List<AllOrdersSummaryModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<AllOrdersSummaryModel> allOrdersSummaryModelList = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, excludeOrderStatus);


        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            allOrdersSummaryModelList = generateResponse(searchResponse);
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    allOrdersSummaryModelList.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return allOrdersSummaryModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String excludeOrderStatus) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        String[] orderStatusList = excludeOrderStatus.split(",");
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("oms.eventName.keyword", eventName.split(",", -1)))
                .filter(QueryBuilders.termsQuery("resultCode", 0))
                .mustNot(QueryBuilders.termsQuery("oms.orderStatus.keyword", orderStatusList))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }

        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<AllOrdersSummaryModel> generateResponse(SearchResponse searchResponse) {
        List<AllOrdersSummaryModel> allOrdersSummaryModelList = new ArrayList<>();
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                log.info("Total search hits -------------" + searchHits.getTotalHits().value);

                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                    String resellerId = searchHitMap.getOrDefault("user.userId", "N/A");

                    resellerId = !resellerId.equalsIgnoreCase("N/A") ? resellerId : searchHitMap.getOrDefault("oms.sender.id", "N/A");
                    resellerId = !resellerId.equalsIgnoreCase("N/A") ? resellerId : searchHitMap.getOrDefault("ims.seller.id", "N/A");
                    String resellerType = searchHitMap.getOrDefault("user.resellerType", "N/A");
                    resellerType = !resellerType.equalsIgnoreCase("N/A") ? resellerType : searchHitMap.getOrDefault("oms.sender.senderType", "N/A");
                    resellerType = !resellerType.equalsIgnoreCase("N/A") ? resellerType : searchHitMap.getOrDefault("ims.seller.type", "N/A");
                    String imsMessageStatus = searchHitMap.getOrDefault("resultMessage", "N/A");
                    String omsMessageStatus = searchHitMap.getOrDefault("oms.resultMessage", "N/A");
                    if (imsMessageStatus.contains("REJECT") || omsMessageStatus.contains("REJECT")) {
                        imsMessageStatus = "TRANSFER_REJECTED";
                    } else {
                        imsMessageStatus = "TRANSFERRED";
                    }

                    String orderStatus = searchHitMap.getOrDefault("oms.orderStatus", "N/A");
                    orderStatus = !orderStatus.equalsIgnoreCase("N/A") ? orderStatus : imsMessageStatus;

                    log.info("imsMessageStatus -{}  orderStatus-{}", imsMessageStatus, orderStatus)
                    String orderType = searchHitMap.getOrDefault("oms.orderType", "N/A");

                    String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A");

                    String buyerId = searchHitMap.getOrDefault("oms.buyer.id", "N/A")
                    String sellerId = searchHitMap.getOrDefault("oms.seller.id", "N/A")
                    String senderId = searchHitMap.getOrDefault("oms.sender.id", "N/A")
                    senderId = !senderId.equalsIgnoreCase("N/A") ? senderId : searchHitMap.getOrDefault("ims.seller.id", "N/A")
                    List<HashMap<String, String>> receivers = searchHitMap.getOrDefault("oms.receivers", null)
                    String receiverId = (receivers != null && !receivers.isEmpty()) ? (receivers[receivers.size() - 1].getOrDefault("id", "N/A")) : "N/A"
                    receiverId = !receiverId.equalsIgnoreCase("N/A") ? receiverId : searchHitMap.getOrDefault("ims.buyer.id", "N/A")

                    List<HashMap<String, String>> deliveryPath = searchHitMap.getOrDefault("oms.deliveryPaths", null)
                    String dropId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.drop.id", "N/A")) : "N/A"
                    String pickupId = (deliveryPath != null && !deliveryPath.isEmpty()) ? (deliveryPath[deliveryPath.size() - 1].getOrDefault("oms.pickup.id", "N/A")) : "N/A"
                    Map<String, String> additionalFields = searchHitMap.getOrDefault("oms.additionalFields", null);
                    String latitude = null, longitude = null;

                    if (additionalFields != null) {
                        for (String field : additionalFields.keySet()) {
                            if (field.equalsIgnoreCase("latitude")) {
                                latitude = additionalFields.get(field)
                            }
                            if (field.equalsIgnoreCase("longitude")) {
                                longitude = additionalFields.get(field)
                            }
                        }
                    }

                    String orderId = searchHitMap.getOrDefault("oms.orderId", "N/A")
                    orderId = !orderId.equalsIgnoreCase("N/A") ? orderId : searchHitMap.getOrDefault("ims.orderId", "N/A")
                    List<HashMap<String, String>> orderItems = searchHitMap.getOrDefault("oms.items", null)
                    List<HashMap<String, String>> imsItems = searchHitMap.getOrDefault("ims.items", null)

                    if (imsItems != null && (!imsItems.isEmpty())) {

                        log.info("imsItems size {} for order {}", imsItems.size(), orderId);
                        String allbatchIds;
                        Long totalQuantity = 0;
                        Map<String, Long> products = new HashMap<String, Long>();
                        Map<String, List<String>> batchIdsforProduct = new HashMap<String, List<String>>();

                        for (HashMap<String, String> item : imsItems) {
                            String[] strCategoryPath = item.getOrDefault("categoryPath", "Not Available").split("/")
                            String categoryPath = strCategoryPath[0]
                            String productSku = item.getOrDefault("productSku", "N/A")
                            String productCode = item.getOrDefault("productCode", productSku)
                            String productName = item.getOrDefault("productName", productSku)
                            String productDescription = item.getOrDefault("productDescription", productSku)
                            String productType = item.getOrDefault("productType", "N/A")
                            String quantity = item.getOrDefault("quantity", "0")
                            List<String> batchIdList = item.getOrDefault("batchIds", null)
                            List<HashMap<String, String>> ranges = item.getOrDefault("ranges", "N/A")

                            BigInteger rangeQuantity = 0;
                            if (ranges.size() != 0) {
                                for (HashMap<String, String> range : ranges) {

                                    String startSerial = range.getOrDefault("startSerial", "N/A")
                                    String endSerial = range.getOrDefault("endSerial", "N/A")
                                    rangeQuantity = rangeQuantity + new BigInteger(endSerial).subtract(new BigInteger(startSerial)).add(BigInteger.ONE);

                                    String batchId = range.getOrDefault("batchId", "N/A")

                                    if (!batchIdList.contains(batchId)) {
                                        batchIdList.add(batchId)
                                    }
                                }
                            }

                            if (batchIdsforProduct.containsKey(productSku)) {
                                List<String> batchID = batchIdsforProduct.get(productSku)
                                for (String batch : batchIdList) {
                                    if (!batchID.contains(batch)) {
                                        batchID.add(batch)
                                    }
                                }
                                batchIdsforProduct.put(productSku, batchID)
                            } else {
                                batchIdsforProduct.put(productSku, batchIdList)
                            }

                            batchIdList = batchIdsforProduct.get(productSku)
                            allbatchIds = String.join(",", batchIdList);

                            quantity = Long.parseLong(quantity) == 0 ? rangeQuantity.longValue() : Long.parseLong(quantity)

                            if (products.containsKey(productSku)) {
                                Long itemQuantity = Long.parseLong(quantity) + products.get(productSku)
                                products.put(productSku, itemQuantity)
                            } else {
                                products.put(productSku, Long.parseLong(quantity))
                            }
                            Long totalItemQuantity = products.get(productSku)
                            String id = GenerateHash.createHashString(orderId, productSku)

                            log.info("Id===========>{}", id)
                            AllOrdersSummaryModel allOrdersSummaryModel = new AllOrdersSummaryModel(id, transactionDate,
                                    transactionNumber, orderId, orderStatus, orderType, resellerId, senderId, buyerId, sellerId,
                                    receiverId, dropId, pickupId, productCode, productSku, productName, productDescription,
                                    productType, allbatchIds, categoryPath, totalItemQuantity, resellerType, latitude, longitude)

                            allOrdersSummaryModelList.add(allOrdersSummaryModel)
                        }
                    }
                    else if (orderItems != null && (!orderItems.isEmpty())) {

                        log.info("OrderItems size {} for order {}", orderItems.size(), orderId);
                        String allbatchIds;
                        Long totalQuantity = 0;
                        Map<String, Long> products = new HashMap<String, Long>();
                        Map<String, List<String>> batchIdsforProduct = new HashMap<String, List<String>>();

                        for (HashMap<String, String> item : orderItems) {
                            String[] strCategoryPath = item.getOrDefault("categoryPath", "Not Available").split("/")
                            String categoryPath = strCategoryPath[0]
                            String productCode = item.getOrDefault("productCode", "N/A")
                            String productName = item.getOrDefault("productName", "N/A")
                            String productDescription = item.getOrDefault("productDescription", "N/A")
                            String productSku = item.getOrDefault("productSku", "N/A")
                            String productType = item.getOrDefault("productType", "N/A")
                            String quantity = item.getOrDefault("quantity", "0")
                            List<String> batchIdList = item.getOrDefault("batchIds", null)
                            List<HashMap<String, String>> ranges = item.getOrDefault("ranges", "N/A")

                            BigInteger rangeQuantity = 0;
                            if (ranges.size() != 0) {
                                for (HashMap<String, String> range : ranges) {

                                    String startSerial = range.getOrDefault("startSerial", "N/A")
                                    String endSerial = range.getOrDefault("endSerial", "N/A")
                                    rangeQuantity = rangeQuantity + new BigInteger(endSerial).subtract(new BigInteger(startSerial)).add(BigInteger.ONE);

                                    String batchId = range.getOrDefault("batchId", "N/A")

                                    if (!batchIdList.contains(batchId)) {
                                        batchIdList.add(batchId)
                                    }
                                }
                            }

                            if (batchIdsforProduct.containsKey(productSku)) {
                                List<String> batchID = batchIdsforProduct.get(productSku)
                                for (String batch : batchIdList) {
                                    if (!batchID.contains(batch)) {
                                        batchID.add(batch)
                                    }
                                }
                                batchIdsforProduct.put(productSku, batchID)
                            } else {
                                batchIdsforProduct.put(productSku, batchIdList)
                            }

                            batchIdList = batchIdsforProduct.get(productSku)
                            allbatchIds = String.join(",", batchIdList);

                            quantity = Long.parseLong(quantity) == 0 ? rangeQuantity.longValue() : Long.parseLong(quantity)

                            if (products.containsKey(productSku)) {
                                Long itemQuantity = Long.parseLong(quantity) + products.get(productSku)
                                products.put(productSku, itemQuantity)
                            } else {
                                products.put(productSku, Long.parseLong(quantity))
                            }
                            Long totalItemQuantity = products.get(productSku)
                            String id = GenerateHash.createHashString(orderId, productSku)

                            log.info("Id===========>{}", id)
                            AllOrdersSummaryModel allOrdersSummaryModel = new AllOrdersSummaryModel(id, transactionDate,
                                    transactionNumber, orderId, orderStatus, orderType, resellerId, senderId, buyerId, sellerId,
                                    receiverId, dropId, pickupId, productCode, productSku, productName, productDescription,
                                    productType, allbatchIds, categoryPath, totalItemQuantity, resellerType, latitude, longitude)

                            allOrdersSummaryModelList.add(allOrdersSummaryModel)
                        }
                    }
                    else {
                        String id = GenerateHash.createHashString(orderId)
                        AllOrdersSummaryModel allOrdersSummaryModel = new AllOrdersSummaryModel(id, transactionDate, transactionNumber, orderId, orderStatus, orderType, resellerId, senderId, buyerId, sellerId,
                                receiverId, dropId, pickupId, "", "", "", "", "", "", "", 0, resellerType, latitude, longitude)

                        allOrdersSummaryModelList.add(allOrdersSummaryModel)
                    }
                }
                return allOrdersSummaryModelList
            }
        }
    }

    private def insertAggregation(List allOrdersSummaryModelList) {

        log.info("AllOrdersForLiveMapAggregator Aggregated into ${allOrdersSummaryModelList.size()} rows.")
        if (allOrdersSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,transaction_number,order_id,order_status,order_type,reseller_id," +
                    "sender_id,buyer_id,seller_id,receiver_id,drop_location_id,pickup_location_id,product_code,product_sku,product_name,product_description," +
                    "product_type,batch_id,category_path,quantity,reseller_type,latitude,longitude) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)," +
                    "transaction_number = VALUES(transaction_number), " +
                    "order_status = VALUES(order_status), batch_id = VALUES(batch_id), quantity = VALUES(quantity)," +
                    "transaction_date = VALUES(transaction_date) ";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allOrdersSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionNumber)
                        ps.setString(++index, row.orderId)
                        ps.setString(++index, row.orderStatus)
                        ps.setString(++index, row.orderType)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.senderId)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.receiverId)
                        ps.setString(++index, row.dropId)
                        ps.setString(++index, row.pickupId)
                        ps.setString(++index, row.productCode)
                        ps.setString(++index, row.productSku)
                        ps.setString(++index, row.productName)
                        ps.setString(++index, row.productDescription)
                        ps.setString(++index, row.productType)
                        ps.setString(++index, row.batchIds)
                        ps.setString(++index, row.categoryPath)
                        ps.setLong(++index, row.quantity)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.latitude)
                        ps.setString(++index, row.longitude)


                    },
                    getBatchSize: { allOrdersSummaryModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class AllOrdersSummaryModel {
    private String id;
    private Timestamp transactionDate;
    private String transactionNumber;
    private String orderId
    private String orderStatus;
    private String orderType
    private String resellerId;
    private String senderId
    private String buyerId
    private String sellerId
    private String receiverId
    private String dropId
    private String pickupId
    private String productCode
    private String productSku;
    private String productName;
    private String productDescription
    private String productType;
    private String batchIds;
    private String categoryPath;
    private long quantity
    private String resellerType
    private String latitude
    private String longitude

    AllOrdersSummaryModel(String id, Timestamp transactionDate, String transactionNumber, String orderId, String orderStatus, String orderType, String resellerId, String senderId, String buyerId, String sellerId, String receiverId, String dropId, String pickupId, String productCode, String productSku, String productName, String productDescription, String productType, String batchIds, String categoryPath, long quantity, String resellerType, String latitude, String longitude) {
        this.id = id
        this.transactionDate = transactionDate
        this.transactionNumber = transactionNumber
        this.orderId = orderId
        this.orderStatus = orderStatus
        this.orderType = orderType
        this.resellerId = resellerId
        this.senderId = senderId
        this.buyerId = buyerId
        this.sellerId = sellerId
        this.receiverId = receiverId
        this.dropId = dropId
        this.pickupId = pickupId
        this.productCode = productCode
        this.productSku = productSku
        this.productName = productName
        this.productDescription = productDescription
        this.productType = productType
        this.batchIds = batchIds
        this.categoryPath = categoryPath
        this.quantity = quantity
        this.resellerType = resellerType
        this.latitude = latitude
        this.longitude = longitude
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

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getSenderId() {
        return senderId
    }

    void setSenderId(String senderId) {
        this.senderId = senderId
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

    String getProductName() {
        return productName
    }

    void setProductName(String productName) {
        this.productName = productName
    }

    String getProductDescription() {
        return productDescription
    }

    void setProductDescription(String productDescription) {
        this.productDescription = productDescription
    }

    String getProductType() {
        return productType
    }

    void setProductType(String productType) {
        this.productType = productType
    }

    String getBatchIds() {
        return batchIds
    }

    void setBatchIds(String batchIds) {
        this.batchIds = batchIds
    }

    String getCategoryPath() {
        return categoryPath
    }

    void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath
    }

    long getQuantity() {
        return quantity
    }

    void setQuantity(long quantity) {
        this.quantity = quantity
    }

    String getResellerType() {
        return resellerType
    }

    void setResellerType(String resellerType) {
        this.resellerType = resellerType
    }

    String getLatitude() {
        return latitude
    }

    void setLatitude(String latitude) {
        this.latitude = latitude
    }

    String getLongitude() {
        return longitude
    }

    void setLongitude(String longitude) {
        this.longitude = longitude
    }
}