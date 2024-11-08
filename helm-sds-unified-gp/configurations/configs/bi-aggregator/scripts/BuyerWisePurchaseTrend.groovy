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
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
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
public class BuyerWisePurchaseTrend extends AbstractAggregator {
    static final def TABLE = "buyer_wise_purchase_summary";

    @Autowired
    RestHighLevelClient client;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${BuyerWisePurchaseTrend.bulkInsertionMode:false}')
    boolean bulkInsertionMode;
    @Value('${BuyerWisePurchaseTrend.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;
    @Value('${BuyerWisePurchaseTrend.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;
    @Value('${BuyerWisePurchaseTrend.eventName:RAISE_ORDER}')
    String eventName
    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;
    @Value('${BuyerWisePurchaseTrend.operationType:GER_PAYMENT,PPS_RECHARGE}')
    String operationType;
    @Value('${BuyerWisePurchaseTrend.SUSCRIBERMSISDNoperationType:ADD_BANK}')
    String SUSCRIBERMSISDNoperationType;
    @Value('${BuyerWisePurchaseTrend.SUSCRIBERIDoperationType:SUB_MIG}')
    String SUSCRIBERIDoperationType;
    @Value('${BuyerWisePurchaseTrend.NATIONALIDoperationType:BIG_DEALER_RECHARGE}')
    String NATIONALIDoperationType;

    @Value('${BuyerWisePurchaseTrend.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${BuyerWisePurchaseTrend.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** BuyerWisePurchaseTrend Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<BuyerWisePurchaseTrendModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(transactionSummaryModels);
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
                List<BuyerWisePurchaseTrendModel> transactionFailureModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionFailureModels);
            }
        }
        log.info("********** BuyerWisePurchaseTrend Aggregator ended at " + new Date());
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
                .filter(QueryBuilders.termsQuery("oms.resultCode", 0));
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.sort("timestamp", SortOrder.ASC).query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<BuyerWisePurchaseTrendModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<BuyerWisePurchaseTrendModel> transactionSummaryModels = generateResponse(searchResponse);
        String scrollId =  searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::"+searchResponse.getHits().size())
        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(5));
            searchResponse = generateScrollSearchResponse(scrollRequest);
            log.info("_________________hits size inside loop _____________________"+searchResponse.getHits().size())
            transactionSummaryModels.addAll(generateResponse(searchResponse));
            scrollId = searchResponse.getScrollId();
        }
        return transactionSummaryModels;
    }

    private SearchResponse generateSearchResponse(SearchRequest searchRequest) {
        SearchResponse searchResponse = null;
        log.info("*******Request:::: " + searchRequest.toString());
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }
        return searchResponse;
    }

    private SearchResponse generateScrollSearchResponse(SearchScrollRequest scrollRequest) {
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + scrollRequest + "\nError message : " + e);
        }
        return searchResponse;
    }
    private List<BuyerWisePurchaseTrendModel> generateResponse(SearchResponse searchResponse) {
        List<BuyerWisePurchaseTrendModel> buyerWisePurchaseTrendModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, BuyerWisePurchaseTrendModel> buyerWisePurchaseTrendModelMap = new HashMap<>();
            if (status == RestStatus.OK) {
                String[] operationTypes = operationType.split(",");

                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
                    Date aggregationDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String);
                    String sellerResellerType = searchHitMap.getOrDefault("oms.sender.senderType", "N/A");

                    String receiverId = "N/A";
                    String dealerId = "N/A";
                    String dealerName = "N/A";
                    String buyerDealerMSISDN = "N/A";
                    String dealerCode = "N/A";

                    List<HashMap<String, String>> receivers = searchHitMap.get("oms.receivers");
                    if (receivers != null && receivers.size() > 0) {
                        def receiver = receivers[0];
                        receiverId = receiver.getOrDefault("id", "N/A") as String;
                        dealerId = receiver.getOrDefault("id", "N/A");
                        dealerName = receiver.getOrDefault("name", "N/A");
                        buyerDealerMSISDN = receiver.getOrDefault("msisdn", "N/A") as String;
                        Map<String, Object> buyerAddlField = receiver.getOrDefault("additionalFields", null);
                        if (buyerAddlField != null) {
                            dealerCode = (buyerAddlField.getOrDefault("dealer_code", "N/A") as String);
                        }
                    }

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        for (int i = 0; i < omsItems.size(); i++) {
                            HashMap<String, String> omsItem = omsItems.get(i);

                            if (omsItem.getOrDefault("data", null) != null) {
                                HashMap<String, String> data = omsItem.get("data");

                                String dataOperationType = data.getOrDefault("operationType", "N/A");
                                if (operationTypes.contains(dataOperationType)) {
                                    String regexDigit = "(?<=^| )\\d+(\\.\\d+)?(?=\$| )|(?<=^| )\\.\\d+(?=\$| )"
                                    if (omsItem.get("quantity") != null && omsItem.get("quantity").replaceAll("[,]*", "").matches(regexDigit)) {
                                        long quantity = omsItem.get("quantity").replaceAll("[,]*", "") as long;

                                        String subscriberMSISDN = data.getOrDefault("SUBSCRIBERMSISDN", "N/A") as String;
                                        String subscriberID = data.getOrDefault("SUBSCRIBERID", "N/A") as String;
                                        String nationalID = data.getOrDefault("NATIONALID", "N/A") as String;
                                        String dealerMsisdn = "N/A";
                                        if (dealerId.equals("SUBSCRIBER_ENTITY") || receiverId.equals("SUBSCRIBER_ENTITY")) {
                                            if (SUSCRIBERMSISDNoperationType.contains(dataOperationType))
                                                dealerMsisdn = subscriberMSISDN
                                            else if (SUSCRIBERIDoperationType.contains(dataOperationType))
                                                dealerMsisdn = subscriberID
                                            else if (NATIONALIDoperationType.contains(dataOperationType))
                                                dealerMsisdn = nationalID
                                        } else {
                                            dealerMsisdn = buyerDealerMSISDN
                                        }

                                        String id = GenerateHash.createHashString(
                                                aggregationDate.toString(),
                                                dealerId,
                                                dealerName,
                                                dealerMsisdn,
                                                sellerResellerType,
                                                dataOperationType
                                        );

                                        if (buyerWisePurchaseTrendModelMap.containsKey(id)) {
                                            BuyerWisePurchaseTrendModel buyerWisePurchaseTrendModel = buyerWisePurchaseTrendModelMap.get(id);
                                            buyerWisePurchaseTrendModel.setQuantity(buyerWisePurchaseTrendModel.getQuantity() + quantity);
                                            buyerWisePurchaseTrendModelMap.put(id, buyerWisePurchaseTrendModel);
                                        } else {
                                            BuyerWisePurchaseTrendModel buyerWisePurchaseTrendModel = new BuyerWisePurchaseTrendModel(id, aggregationDate, dealerId, dealerMsisdn, dealerName, sellerResellerType, dataOperationType, quantity, dealerCode);
                                            buyerWisePurchaseTrendModelMap.put(id, buyerWisePurchaseTrendModel);
                                        }
                                    } else {
                                        log.error("***** Skipping transaction : " + omsItem.get("transactionReference") + " | Unsupported Quantity Value: " + omsItem.get("quantity"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            buyerWisePurchaseTrendModelMap.each {
                entry -> buyerWisePurchaseTrendModelList.add(entry.value)
            }
        }
        return buyerWisePurchaseTrendModelList;
    }

    private def insertAggregation(List<BuyerWisePurchaseTrendModel> buyerWisePurchaseTrendModelList) {
        log.info("BuyerWisePurchaseTrend Aggregated into ${buyerWisePurchaseTrendModelList.size()} rows.")
        if (buyerWisePurchaseTrendModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregation_date,dealer_id,dealer_msisdn,dealer_name,seller_reseller_type,transaction_type,quantity,dealer_code) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = buyerWisePurchaseTrendModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.aggregationDate.getTime()))
                        ps.setString(++index, row.dealerId)
                        ps.setString(++index, row.dealerMsisdn)
                        ps.setString(++index, row.dealerName)
                        ps.setString(++index, row.sellerResellerType)
                        ps.setString(++index, row.dataOperationType)
                        ps.setLong(++index, row.quantity)
                        ps.setString(++index, row.dealerCode)
                    },
                    getBatchSize: { buyerWisePurchaseTrendModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class BuyerWisePurchaseTrendModel {
    private String id;
    private Date aggregationDate;
    private String dealerId;
    private String dealerMsisdn;
    private String dealerName;
    private String sellerResellerType;
    private String dataOperationType;
    private long quantity;
    private String dealerCode;

    BuyerWisePurchaseTrendModel(String id, Date aggregationDate, String dealerId, String dealerMsisdn, String dealerName, String sellerResellerType, String dataOperationType, long quantity, String dealerCode) {
        this.id = id
        this.aggregationDate = aggregationDate
        this.dealerId = dealerId
        this.dealerMsisdn = dealerMsisdn
        this.dealerName = dealerName
        this.sellerResellerType = sellerResellerType
        this.dataOperationType = dataOperationType
        this.quantity = quantity
        this.dealerCode = dealerCode
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getAggregationDate() {
        return aggregationDate
    }

    void setAggregationDate(Date aggregationDate) {
        this.aggregationDate = aggregationDate
    }

    String getDealerId() {
        return dealerId
    }

    void setDealerId(String dealerId) {
        this.dealerId = dealerId
    }

    String getDealerCode() {
        return dealerCode
    }

    void setDealerCode(String dealerCode) {
        this.dealerCode = dealerCode
    }

    String getDealerMsisdn() {
        return dealerMsisdn
    }

    void setDealerMsisdn(String dealerMsisdn) {
        this.dealerMsisdn = dealerMsisdn
    }

    String getDealerName() {
        return dealerName
    }

    void setDealerName(String dealerName) {
        this.dealerName = dealerName
    }

    String getSellerResellerType() {
        return sellerResellerType
    }

    void setSellerResellerType(String sellerResellerType) {
        this.sellerResellerType = sellerResellerType
    }

    String getDataOperationType() {
        return dataOperationType
    }

    void setDataOperationType(String dataOperationType) {
        this.dataOperationType = dataOperationType
    }

    long getQuantity() {
        return quantity
    }

    void setQuantity(long quantity) {
        this.quantity = quantity
    }
}
