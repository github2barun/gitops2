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
//@DynamicMixin
public class ImsDataAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "ims_data_aggregator"

    @Autowired
    RestHighLevelClient client

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${ImsDataAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode

    @Value('${ImsDataAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString

    @Value('${ImsDataAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString

    @Value('${ImsDataAggregator.eventName:TRANSFER_INVENTORY}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${ImsDataAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** ImsDataAggregator Aggregator started at " + new Date())
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString)
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString)

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<ImsDataModel> imsDataModelList = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(imsDataModelList)
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
                List<ImsDataModel> imsDataModelList = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate())
                insertAggregation(imsDataModelList)
            }
        }

        log.info("********** ImsDataAggregator Aggregator ended at " + new Date())
    }


    private List<ImsDataModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<ImsDataModel> imsDataModelList = new ArrayList<>()

        SearchRequest searchRequest = new SearchRequest(index)
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate)
        searchRequest.source(searchSourceBuilder)
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            imsDataModelList = generateResponse(searchResponse)
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    imsDataModelList.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return imsDataModelList
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("ims.eventName.keyword", eventName.split(",", -1)))
                .filter(QueryBuilders.termsQuery("resultCode", 0))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize)
        return searchSourceBuilder
    }

    private List<ImsDataModel> generateResponse(SearchResponse searchResponse) {
        List<ImsDataModel> imsDataModelList = new ArrayList<>()
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status()
            log.debug("response status -------------" + status)

            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap()


                    //List<HashMap<String, String>> imsItems = searchHitMap.getOrDefault("ims.items", null)
                    List<HashMap<String, String>> imsItems = new ArrayList<>();
                    def itemsObject = searchHitMap.getOrDefault("ims.items", null)
                    if (itemsObject instanceof List) {
                        imsItems = itemsObject;
                    } else {
                        imsItems.add(itemsObject);
                    }

                    if (imsItems != null && (!imsItems.isEmpty())) {

                        Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                        Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                        String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A")
                        String resellerId = searchHitMap.getOrDefault("user.userId", "N/A") as String
                        String resellerType = searchHitMap.getOrDefault("user.resellerType", "N/A") as String
                        String sellerId = searchHitMap.getOrDefault("ims.seller.id", "N/A") as String
                        String buyerId = searchHitMap.getOrDefault("ims.buyer.id", "N/A") as String

                        String regexDigit = "(?<=^| )\\d+(\\.\\d+)?(?=\$| )|(?<=^| )\\.\\d+(?=\$| )"

                        int imsValue = 1
                        for (HashMap<String, String> imsItem : imsItems) {
                            String quantity = imsItem.getOrDefault("quantity", "N/A") as String
                            if (quantity.replaceAll("[,]*", "").matches(regexDigit)) {

                                String productCode = imsItem.getOrDefault("productCode", "N/A") as String
                                String productSku = imsItem.getOrDefault("productSku", "N/A") as String
                                List<Map<String, String>> updateAttributes = imsItem.getOrDefault("updateAttributes", null)
                                String tripID = "N/A", routeInformation = "N/A", operationType = "N/A"

                                if(updateAttributes != null) {
                                    for (Map<String, String> fields : updateAttributes) {
                                        if (fields.values().contains("tripId")) {
                                            tripID = fields.get("value");
                                        }
                                        if (fields.values().contains("routeInfo")) {
                                            routeInformation = fields.get("value");
                                        }
                                        if (fields.values().contains("imsOperation")) {
                                            operationType = fields.get("value");
                                        }
                                    }
                                }
                                String id = GenerateHash.createHashString(transactionNumber, imsValue as String)

                                ImsDataModel imsDataModel = new ImsDataModel(id, transactionDate, transactionNumber, quantity as Long,
                                        Validations.stringValidation(productCode), Validations.stringValidation(productSku), Validations.stringValidation(sellerId),
                                        Validations.stringValidation(buyerId), Validations.stringValidation(resellerId), Validations.stringValidation(resellerType),
                                        Validations.stringValidation(tripID), Validations.stringValidation(routeInformation), Validations.stringValidation(operationType))

                                imsDataModelList.add(imsDataModel)
                            } else {
                                log.error("***** Skipping this transaction | Unsupported quantity Value" + quantity + " for transactionNumber :" + transactionNumber)
                            }
                            imsValue++
                        }
                    }
                }
                return imsDataModelList
            }
        }
    }


    private def insertAggregation(List imsDataModelList) {
        log.info("ImsDataAggregator Aggregated into ${imsDataModelList.size()} rows.")
        if (imsDataModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,transaction_number,quantity,product_code,product_sku,seller_id,buyer_id,reseller_id,reseller_type, trip_id, route_information, operation_type) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)"
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = imsDataModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionNumber)
                        ps.setLong(++index, row.quantity)
                        ps.setString(++index, row.productCode)
                        ps.setString(++index, row.productSku)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.tripID)
                        ps.setString(++index, row.routeInformation)
                        ps.setString(++index, row.operationType)
                    },
                    getBatchSize: { imsDataModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class ImsDataModel {
    private String id
    private Timestamp transactionDate
    private String transactionNumber
    private long quantity
    private String productCode
    private String productSku
    private String sellerId
    private String buyerId
    private String resellerId
    private String resellerType
    private String tripID
    private String routeInformation
    private String operationType

    ImsDataModel(String id, Timestamp transactionDate, String transactionNumber, long quantity, String productCode, String productSku, String sellerId, String buyerId, String resellerId, String resellerType, String tripID, String routeInformation, String operationType) {
        this.id = id
        this.transactionDate = transactionDate
        this.transactionNumber = transactionNumber
        this.quantity = quantity
        this.productCode = productCode
        this.productSku = productSku
        this.sellerId = sellerId
        this.buyerId = buyerId
        this.resellerId = resellerId
        this.resellerType = resellerType
        this.tripID = tripID
        this.routeInformation = routeInformation
        this.operationType = operationType
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

    long getQuantity() {
        return quantity
    }

    void setQuantity(long quantity) {
        this.quantity = quantity
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

    String getSellerId() {
        return sellerId
    }

    void setSellerId(String sellerId) {
        this.sellerId = sellerId
    }

    String getBuyerId() {
        return buyerId
    }

    void setBuyerId(String buyerId) {
        this.buyerId = buyerId
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getResellerType() {
        return resellerType
    }

    void setResellerType(String resellerType) {
        this.resellerType = resellerType
    }

    String getTripID() {
        return tripID
    }

    void setTripID(String tripID) {
        this.tripID = tripID
    }

    String getRouteInformation() {
        return routeInformation
    }

    void setRouteInformation(String routeInformation) {
        this.routeInformation = routeInformation
    }

    String getOperationType() {
        return operationType
    }

    void setOperationType(String operationType) {
        this.operationType = operationType
    }
}