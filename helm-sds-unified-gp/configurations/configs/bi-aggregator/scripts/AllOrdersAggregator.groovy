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
public class AllOrdersAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "all_orders_aggregator"
    static final def ZERO = "0"
    static final def DELIMITER = ","

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${AllOrdersAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${AllOrdersAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${AllOrdersAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${AllOrdersAggregator.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${AllOrdersAggregator.excludeOrderStatus:TRANSFERRED,STOCK_DELIVERED,RETURN_DELIVER}')
    String excludeOrderStatus;

    @Value('${AllOrdersAggregator.commissionType:COMMISSION}')
    String commissionType;

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${AllOrdersAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** AllOrdersAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

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

        log.info("********** AllOrdersAggregator Aggregator ended at " + new Date());
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

                    //String transactionDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd");
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                    // user.userId is the initiator
                    String resellerId = searchHitMap.getOrDefault("user.userId", "N/A");
                    String orderStatus = searchHitMap.getOrDefault("oms.orderStatus", "N/A");
                    String orderType = searchHitMap.getOrDefault("oms.orderType", "N/A");
                    String resellerType = searchHitMap.getOrDefault("user.resellerType", "N/A");
                    String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A");
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

                    List<HashMap<String, String>> invoiceItems = searchHitMap.getOrDefault("oms.invoices", null)
                    List<HashMap<String, String>> orderItems = searchHitMap.getOrDefault("oms.items", null)

                    if (invoiceItems != null && (!invoiceItems.isEmpty())) {

                        log.debug("InvoiceItems size {} for order {}", invoiceItems.size(), orderId);

                        String regexDigit = "(?<=^| )\\d+(\\.\\d+)?(?=\$| )|(?<=^| )\\.\\d+(?=\$| )"

                        int invoiceValue = 1
                        for (HashMap<String, String> invoices : invoiceItems) {
                            String invoiceId = invoices.getOrDefault("invoice.id", "N/A") as String
                            String invoiceStatus = invoices.getOrDefault("invoice.status", "N/A") as String

                            List<HashMap<String, String>> invoiceEntryList = invoices.get("invoice.invoiceEntries")
                            int invoiceProductEntryValue = 1
                            for (HashMap<String, String> invoiceEntry : invoiceEntryList) {
                                String[] strCategoryPath = invoiceEntry.getOrDefault("categoryPath", "Not Available").split("/")
                                String categoryPath = strCategoryPath[0]
                                String productCode = invoiceEntry.getOrDefault("productCode", "N/A")
                                String productId = invoiceEntry.getOrDefault("productId", "N/A")
                                String productName = invoiceEntry.getOrDefault("productName", "N/A")
                                String productDescription = invoiceEntry.getOrDefault("productDescription", "N/A")
                                String productSku = invoiceEntry.getOrDefault("productSKU", "N/A")
                                String productType = invoiceEntry.getOrDefault("productType", "N/A")
                                String productQuantity = invoiceEntry.getOrDefault("quantity", ZERO)
                                String quantity = productQuantity.equalsIgnoreCase("N/A") ? ZERO : productQuantity.replaceAll(DELIMITER, "")
                                Map<String, Object> totalDiscount = invoiceEntry.getOrDefault("totalDiscount", null)
                                String discount = totalDiscount != null
                                        ? totalDiscount.getOrDefault("amount", ZERO).toString().replaceAll(DELIMITER, "")
                                        : ZERO

                                Map<String, Object> totalUnitPrice = invoiceEntry.getOrDefault("totalUnitPrice", null)
                                String unitPrice = totalUnitPrice != null
                                        ? totalUnitPrice.getOrDefault("amount", ZERO).toString().replaceAll(DELIMITER, "")
                                        : ZERO

                                if (unitPrice.replaceAll("[,]*", "").matches(regexDigit) && quantity.replaceAll("[,]*", "").matches(regexDigit)) {

                                    String id = GenerateHash.createHashString(orderId, invoiceValue as String, invoiceProductEntryValue as String)

                                    AllOrdersSummaryModel allOrdersSummaryModel = new AllOrdersSummaryModel(id, transactionDate,
                                            transactionNumber, invoiceValue, invoiceProductEntryValue, monthValue as long, quarterValue, resellerId, buyerId, sellerId,
                                            receiverId, invoiceId, invoiceStatus, orderId, orderStatus, productCode, productId, productName, productDescription, productSku,
                                            productType, categoryPath, commissionType, (discount != "" ? Math.abs(discount as double) : "N/A") as double, unitPrice as double, quantity as Long, orderType, resellerType, dropId,
                                            pickupId, senderId)

                                    allOrdersSummaryModelList.add(allOrdersSummaryModel)
                                    invoiceProductEntryValue++
                                } else {
                                    log.error("***** Skipping this Product : " + productId + " | Unsupported  Value" + " for transactionNumber :" + transactionNumber);
                                }
                            }

                            invoiceValue++
                        }

                    } else if (orderItems != null && (!orderItems.isEmpty())) {

                        log.debug("OrderItems size {} for order {}", orderItems.size(), orderId);

                        for (HashMap<String, String> item : orderItems) {

                            String[] strCategoryPath = item.getOrDefault("categoryPath", "Not Available").split("/")
                            String categoryPath = strCategoryPath[0]
                            String productCode = item.getOrDefault("productCode", "N/A")
                            String productId = item.getOrDefault("productId", "N/A")
                            String productName = item.getOrDefault("productName", "N/A")
                            String productDescription = item.getOrDefault("productDescription", "N/A")
                            String productSku = item.getOrDefault("productSku", "N/A")
                            String productType = item.getOrDefault("productType", "N/A")
                            String productQuantity = item.getOrDefault("quantity", "0")
                            String quantity = productQuantity.equalsIgnoreCase("N/A") ? "0" : productQuantity.replaceAll(DELIMITER, "")

                            String id = GenerateHash.createHashString(orderId)

                            AllOrdersSummaryModel allOrdersSummaryModel = new AllOrdersSummaryModel(id, transactionDate,
                                    transactionNumber, 0, 0, monthValue as long, quarterValue, resellerId, buyerId, sellerId,
                                    receiverId, StringUtils.EMPTY, StringUtils.EMPTY, orderId, orderStatus, productCode, productId, productName, productDescription, productSku,
                                    productType, categoryPath, StringUtils.EMPTY, 0, 0, quantity as Long, orderType, resellerType, dropId,
                                    pickupId, senderId)

                            allOrdersSummaryModelList.add(allOrdersSummaryModel)
                        }
                    }
                }
                return allOrdersSummaryModelList
            }
        }
    }

    private def insertAggregation(List allOrdersSummaryModelList) {

        log.info("AllOrdersAggregator Aggregated into ${allOrdersSummaryModelList.size()} rows.")
        if (allOrdersSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,transaction_number,invoice_value,invoice_product_entry_value,month_value,quarter,reseller_id," +
                    "buyer_id,seller_id,receiver_id,order_status,invoice_id,invoice_status,order_id,product_code,product_id,product_name,product_description,product_sku," +
                    "product_type,category_path,commission_type,total_discount,total_unit_price,uom_quantity,order_type,reseller_type,drop_location_id,pickup_location_id,sender_id) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)," +
                    "transaction_number = VALUES(transaction_number), " +
                    "invoice_status = VALUES(invoice_status), " +
                    "order_status = VALUES(order_status), " +
                    "transaction_date = VALUES(transaction_date) ";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allOrdersSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionNumber)
                        ps.setLong(++index, row.invoiceValue)
                        ps.setLong(++index, row.invoiceProductEntryValue)
                        ps.setLong(++index, row.monthValue)
                        ps.setString(++index, row.quarter)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.receiverId)
                        ps.setString(++index, row.orderStatus)
                        ps.setString(++index, row.invoiceId)
                        ps.setString(++index, row.invoiceStats)
                        ps.setString(++index, row.orderId)
                        ps.setString(++index, row.productCode)
                        ps.setString(++index, row.productId)
                        ps.setString(++index, row.productName)
                        ps.setString(++index, row.productDescription)
                        ps.setString(++index, row.productSku)
                        ps.setString(++index, row.productType)
                        ps.setString(++index, row.categoryPath)
                        ps.setString(++index, row.commissionType)
                        ps.setDouble(++index, row.totalDiscount)
                        ps.setDouble(++index, row.totalUnitPrice)
                        ps.setLong(++index, row.quantity)
                        ps.setString(++index, row.orderType)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.dropId)
                        ps.setString(++index, row.pickupId)
                        ps.setString(++index, row.senderId)
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
    private long invoiceValue;
    private long invoiceProductEntryValue;
    private long monthValue;
    private String quarter;
    private String resellerId;
    private String buyerId
    private String sellerId
    private String receiverId
    private String invoiceId
    private String invoiceStats
    private String orderId
    private String orderStatus;
    private String productCode
    private String productId
    private String productName;
    private String productDescription
    private String productSku;
    private String productType;
    private String categoryPath;
    private String commissionType;
    private double totalDiscount
    private double totalUnitPrice
    private long quantity
    private String orderType
    private String resellerType
    private String dropId
    private String pickupId
    private String senderId

    AllOrdersSummaryModel(String id, Timestamp transactionDate, String transactionNumber, long invoiceValue, long invoiceProductEntryValue, long monthValue, String quarter, String resellerId, String buyerId, String sellerId, String receiverId, String invoiceId, String invoiceStats, String orderId, String orderStatus, String productCode, String productId, String productName, String productDescription, String productSku, String productType, String categoryPath, String commissionType, double totalDiscount, double totalUnitPrice, long quantity, String orderType, String resellerType, String dropId, String pickupId, String senderId) {
        this.id = id
        this.transactionDate = transactionDate
        this.transactionNumber = transactionNumber
        this.invoiceValue = invoiceValue
        this.invoiceProductEntryValue = invoiceProductEntryValue
        this.monthValue = monthValue
        this.quarter = quarter
        this.resellerId = resellerId
        this.buyerId = buyerId
        this.sellerId = sellerId
        this.receiverId = receiverId
        this.invoiceId = invoiceId
        this.invoiceStats = invoiceStats
        this.orderId = orderId
        this.orderStatus = orderStatus
        this.productCode = productCode
        this.productId = productId
        this.productName = productName
        this.productDescription = productDescription
        this.productSku = productSku
        this.productType = productType
        this.categoryPath = categoryPath
        this.commissionType = commissionType
        this.totalDiscount = totalDiscount
        this.totalUnitPrice = totalUnitPrice
        this.quantity = quantity
        this.orderType = orderType
        this.resellerType = resellerType
        this.dropId = dropId
        this.pickupId = pickupId
        this.senderId = senderId
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

    long getInvoiceValue() {
        return invoiceValue
    }

    void setInvoiceValue(long invoiceValue) {
        this.invoiceValue = invoiceValue
    }

    long getInvoiceProductEntryValue() {
        return invoiceProductEntryValue
    }

    void setInvoiceProductEntryValue(long invoiceProductEntryValue) {
        this.invoiceProductEntryValue = invoiceProductEntryValue
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

    String getInvoiceId() {
        return invoiceId
    }

    void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId
    }

    String getInvoiceStats() {
        return invoiceStats
    }

    void setInvoiceStats(String invoiceStats) {
        this.invoiceStats = invoiceStats
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

    String getProductCode() {
        return productCode
    }

    void setProductCode(String productCode) {
        this.productCode = productCode
    }

    String getProductId() {
        return productId
    }

    void setProductId(String productId) {
        this.productId = productId
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

    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }

    String getProductType() {
        return productType
    }

    void setProductType(String productType) {
        this.productType = productType
    }

    String getCategoryPath() {
        return categoryPath
    }

    void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath
    }

    String getCommissionType() {
        return commissionType
    }

    void setCommissionType(String commissionType) {
        this.commissionType = commissionType
    }

    double getTotalDiscount() {
        return totalDiscount
    }

    void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount
    }

    double getTotalUnitPrice() {
        return totalUnitPrice
    }

    void setTotalUnitPrice(double totalUnitPrice) {
        this.totalUnitPrice = totalUnitPrice
    }

    long getQuantity() {
        return quantity
    }

    void setQuantity(long quantity) {
        this.quantity = quantity
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