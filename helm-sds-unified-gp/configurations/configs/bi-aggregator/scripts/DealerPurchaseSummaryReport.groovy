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
public class DealerPurchaseSummaryReport extends AbstractAggregator {
    static final def TABLE = "dealer_purchase_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${DealerPurchaseSummaryReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DealerPurchaseSummaryReport.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${DealerPurchaseSummaryReport.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${DealerPurchaseSummaryReport.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DealerPurchaseSummaryReport.operationType:ADD,ADD_BANK,ADD_BANK_KHAAN,ADD_BANK_SAVING,ADD_MIG,ADD_SELF,DEALER_RECHARGE,SUB,SUB_MIG,SUB_SELF,SUB_SELLER,BIG_DEALER_RECHARGE}')
    String operationType;

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${DealerPurchaseSummaryReport.SUSCRIBERMSISDNoperationType:ADD_BANK}')
    String SUSCRIBERMSISDNoperationType;

    @Value('${DealerPurchaseSummaryReport.SUSCRIBERIDoperationType:SUB_MIG}')
    String SUSCRIBERIDoperationType;

    @Value('${DealerPurchaseSummaryReport.NATIONALIDoperationType:BIG_DEALER_RECHARGE}')
    String NATIONALIDoperationType;

    @Value('${DealerPurchaseSummaryReport.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${DealerPurchaseSummaryReport.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** DealerPurchaseSummaryReport Aggregator started at " + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<DealerPurchaseSummaryModel> purchaseSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(purchaseSummaryModels);
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
                List<DealerPurchaseSummaryModel> purchaseSummaryModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(purchaseSummaryModels);
            }
        }

        log.info("********** DealerPurchaseSummaryReport Aggregator ended at " + new Date());
    }


    private List<DealerPurchaseSummaryModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);

        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<DealerPurchaseSummaryModel> purchaseSummaryModels = generateResponse(searchResponse);
        String scrollId =  searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::"+searchResponse.getHits().size())
        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(5));
            searchResponse = generateScrollSearchResponse(scrollRequest);
            log.info("_________________hits size inside loop _____________________"+searchResponse.getHits().size())
            purchaseSummaryModels.addAll(generateResponse(searchResponse));
            scrollId = searchResponse.getScrollId();
        }
        return purchaseSummaryModels;
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
    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName));

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.sort("timestamp",SortOrder.ASC).query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<DealerPurchaseSummaryModel> generateResponse(SearchResponse searchResponse)  {
        List<DealerPurchaseSummaryModel> purchaseSummaryModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, DealerPurchaseSummaryModel> purchaseSummaryMap = new HashMap<>();
            if (status == RestStatus.OK) {
                String[] operationTypes = operationType.split(",");
                SearchHits searchHits = searchResponse.getHits();

                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        String buyerDealerType = searchHitMap.getOrDefault("oms.buyer.resellerType", "N/A") as String;
                        String buyerDealerMSISDN = searchHitMap.getOrDefault("oms.buyer.buyerDealerMsisdn", "N/A") as String;
                        Map<String, Object> buyerAddlField = searchHitMap.getOrDefault("oms.buyer.additionalFields", null);
                        String buyerDealerCode = buyerAddlField == null ? "N/A" : (buyerAddlField.getOrDefault("dealer_code", "N/A") as String);
                        String buyerDealerID = searchHitMap.getOrDefault("oms.buyer.id", "N/A") as String;
                        String transactionDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd");
                        List<HashMap<String, String>> receivers = searchHitMap.get("oms.receivers");
                        String receiverId = receivers[0].getOrDefault("id", "N/A") as String;

                        Map<String, Object> senderAddlField = searchHitMap.getOrDefault("oms.sender.additionalFields", null);
                        String area = senderAddlField == null ? "N/A" : (senderAddlField.getOrDefault("area", "N/A") as String);
                        String section = senderAddlField == null ? "N/A" : (senderAddlField.getOrDefault("section", "N/A") as String);
                        String city_province = senderAddlField == null ? "N/A" : (senderAddlField.getOrDefault("city_province", "N/A") as String);
                        String district = senderAddlField == null ? "N/A" : (senderAddlField.getOrDefault("district_sum", "N/A") as String);

                        //Need to change field after discussion
                        // String buyerDealerStatus = searchHitMap.getOrDefault("oms.buyer.dealerStatus", "N/A") as String;
                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        // Already applied null validation for this
                        if (!omsItems.isEmpty()) {
                            for (HashMap<String, String> omsItem : omsItems) {
                                if (omsItem.getOrDefault("data", null) != null) {
                                    HashMap<String, String> data = omsItem.get("data");
                                    String dataOperationType = data.getOrDefault("operationType", "N/A");

                                    if (operationTypes.contains(dataOperationType) && data.getOrDefault("resultCode", null) != null && data.get("resultCode") == "0") {
                                        def amount = 0D
                                        if (!data.getOrDefault("amount", "0").equals("N/A")) {
                                            amount = Double.parseDouble(data.getOrDefault("amount", "0"))
                                        }
                                        String buyerDealerBalance = data.getOrDefault("receiverBalanceAfter", "N/A") as String;
                                        String dealerType = data.getOrDefault("senderResellerType", "N/A") as String;
                                        String buyerDealerStatus = data.getOrDefault("receiverBalanceStatus", "N/A") as String;
                                        String subscriberMSISDN = data.getOrDefault("SUBSCRIBERMSISDN", "N/A") as String;
                                        String subscriberID = data.getOrDefault("SUBSCRIBERID", "N/A") as String;
                                        String nationalID = data.getOrDefault("NATIONALID", "N/A") as String;
                                        String buyerMsisdn = "N/A";
                                        if (buyerDealerID.equals("SUBSCRIBER_ENTITY") || receiverId.equals("SUBSCRIBER_ENTITY")) {
                                            if (SUSCRIBERMSISDNoperationType.contains(dataOperationType))
                                                buyerMsisdn = subscriberMSISDN
                                            else if (SUSCRIBERIDoperationType.contains(dataOperationType))
                                                buyerMsisdn = subscriberID
                                            else if (NATIONALIDoperationType.contains(dataOperationType))
                                                buyerMsisdn = nationalID
                                        } else {
                                            buyerMsisdn = buyerDealerMSISDN
                                        }

                                        String id = GenerateHash.createHashString(
                                                dealerType,
                                                buyerMsisdn,
                                                buyerDealerID,
                                                buyerDealerType,
                                                buyerDealerStatus,
                                                transactionDate
                                        );

                                        if (purchaseSummaryMap.containsKey(id)) {
                                            DealerPurchaseSummaryModel dealerPurchaseSummaryModel = purchaseSummaryMap.get(id);
                                            dealerPurchaseSummaryModel.setAmount(amount + dealerPurchaseSummaryModel.getAmount());
                                            dealerPurchaseSummaryModel.setTransactionCount(dealerPurchaseSummaryModel.getTransactionCount() + 1);
                                            dealerPurchaseSummaryModel.setBuyerDealerBalance(buyerDealerBalance);
                                            purchaseSummaryMap.put(id, dealerPurchaseSummaryModel);
                                        } else {
                                            def dateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(transactionDate);
                                            DealerPurchaseSummaryModel dealerPurchaseSummaryModel = new DealerPurchaseSummaryModel(id, dateObj, dealerType, buyerMsisdn, 1, district, area, section, city_province, amount, buyerDealerID, buyerDealerType, buyerDealerBalance, buyerDealerStatus, buyerDealerCode);
                                            purchaseSummaryMap.put(id, dealerPurchaseSummaryModel);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                purchaseSummaryMap.each {
                    entry -> purchaseSummaryModelList.add(entry.value)
                }
            }
        }
        return purchaseSummaryModelList;
    }

    private def insertAggregation(List purchaseSummaryModelList) {

        log.info("DealerPurchaseSummaryReport Aggregated into ${purchaseSummaryModelList.size()} rows.")
        if (purchaseSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,buyer_dealer_type,buyer_dealer_id,buyer_dealer_msisdn,buyer_dealer_status,buyer_dealer_balance,district,area,section," +
                    "city,seller_dealer_type,purchase_frequency,purchase_amount,buyer_dealer_code) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE purchase_amount = VALUES(purchase_amount), purchase_frequency = VALUES(purchase_frequency), buyer_dealer_balance = VALUES(buyer_dealer_balance)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = purchaseSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.buyerDealerType)
                        ps.setString(++index, row.buyerDealerID)
                        ps.setString(++index, row.buyerDealerMSISDN)
                        ps.setString(++index, row.buyerDealerStatus)
                        ps.setString(++index, row.buyerDealerBalance)
                        ps.setString(++index, row.district)
                        ps.setString(++index, row.area)
                        ps.setString(++index, row.section)
                        ps.setString(++index, row.city)
                        ps.setString(++index, row.dealerType)
                        ps.setLong(++index, row.transactionCount)
                        ps.setBigDecimal(++index, row.amount)
                        ps.setString(++index, row.buyerDealerCode)
                    },
                    getBatchSize: { purchaseSummaryModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class DealerPurchaseSummaryModel {
    private String id;
    private Date transactionDate;
    private String dealerType;
    private String buyerDealerMSISDN;
    private String buyerDealerID;
    private long transactionCount;
    private String district;
    private String area;
    private String section;
    private String city;
    private double amount;
    private String buyerDealerType;
    private String buyerDealerBalance;
    private String buyerDealerStatus;
    private String buyerDealerCode;

    public DealerPurchaseSummaryModel(String id, Date transactionDate, String dealerType, String buyerDealerMSISDN, long transactionCount,
                                      String district, String area, String section, String city, double amount, String buyerDealerID, String buyerDealerType, String buyerDealerBalance, String buyerDealerStatus, String buyerDealerCode) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.dealerType = dealerType;
        this.buyerDealerMSISDN = buyerDealerMSISDN;
        this.area = area;
        this.amount = amount;
        this.transactionCount = transactionCount;
        this.district = district;
        this.section = section;
        this.city = city;
        this.buyerDealerID = buyerDealerID;
        this.buyerDealerType = buyerDealerType;
        this.buyerDealerBalance = buyerDealerBalance;
        this.buyerDealerStatus = buyerDealerStatus;
        this.buyerDealerCode = buyerDealerCode;
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getTransactionDate() {
        return transactionDate
    }

    void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
    }

    String getDealerType() {
        return dealerType
    }

    void setDealerType(String dealerType) {
        this.dealerType = dealerType
    }


    String getBuyerDealerID() {
        return buyerDealerID
    }

    void setBuyerDealerID(String buyerDealerID) {
        this.buyerDealerID = buyerDealerID
    }

    String getBuyerDealerCode() {
        return buyerDealerCode
    }

    void setBuyerDealerCode(String buyerDealerCode) {
        this.buyerDealerCode = buyerDealerCode
    }

    String getBuyerDealerType() {
        return buyerDealerType
    }

    void setBuyerDealerType(String buyerDealerType) {
        this.buyerDealerType = buyerDealerType
    }


    String getBuyerDealerBalance() {
        return buyerDealerBalance
    }

    void setBuyerDealerBalance(String buyerDealerBalance) {
        this.buyerDealerBalance = buyerDealerBalance
    }

    String getBuyerDealerStatus() {
        return buyerDealerStatus
    }

    void setBuyerDealerStatus(String buyerDealerStatus) {
        this.buyerDealerStatus = buyerDealerStatus
    }

    String getBuyerDealerMSISDN() {
        return buyerDealerMSISDN
    }

    void setBuyerDealerMSISDN(String buyerDealerMSISDN) {
        this.buyerDealerMSISDN = buyerDealerMSISDN
    }

    long getTransactionCount() {
        return transactionCount
    }

    void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount
    }

    String getDistrict() {
        return district
    }

    void setDistrict(String district) {
        this.district = district
    }

    String getArea() {
        return area
    }

    void setArea(String area) {
        this.area = area
    }

    String getSection() {
        return section
    }

    void setSection(String section) {
        this.section = section
    }

    String getCity() {
        return city
    }

    void setCity(String city) {
        this.city = city
    }

    double getAmount() {
        return amount
    }

    void setAmount(double amount) {
        this.amount = amount
    }
}