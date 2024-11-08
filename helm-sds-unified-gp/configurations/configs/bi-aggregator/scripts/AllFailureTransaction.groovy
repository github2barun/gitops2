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

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class AllFailureTransaction extends AbstractAggregator {
    static final def TABLE = "all_failure_transactions"
    @Autowired
    RestHighLevelClient client
    @Autowired
    protected JdbcTemplate jdbcTemplate
    @Value('${AllFailureTransaction.bulkInsertionMode:false}')
    boolean bulkInsertionMode
    @Value('${AllFailureTransaction.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString
    @Value('${AllFailureTransaction.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString
    @Value('${AllFailureTransaction.eventName:RAISE_ORDER}')
    String eventName
    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset
    @Value('${AllFailureTransaction.scrollSize:1000}')
    int scrollSize;


    @Transactional
    @Scheduled(cron = '${AllFailureTransaction.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** AllFailureTransaction Aggregator started at " + new Date())
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString)
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString)
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
            //need to change
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TransactionFailureModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(transactionSummaryModels)
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
                List<TransactionFailureModel> transactionFailureModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate())
                insertAggregation(transactionFailureModels)
            }
        }
        log.info("********** AllFailureTransaction  ended at " + new Date())
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName)).mustNot(QueryBuilders.termsQuery("oms.resultCode", 0))
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize)
        return searchSourceBuilder
    }

    private List<TransactionFailureModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index)
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate)
        searchRequest.source(searchSourceBuilder)
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<TransactionFailureModel> transactionSummaryModels = generateResponse(searchResponse);
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

    private List<TransactionFailureModel> generateResponse(SearchResponse searchResponse) {
        List<TransactionFailureModel> transactionFailureModelList = new ArrayList<>()

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status()
            log.debug("response status -------------" + status)

            HashMap<String, TransactionFailureModel> transactionFailureModelMap = new HashMap<>()
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap()
                    int internalResultCode = searchHitMap.getOrDefault("oms.resultCode", 0) as int
                    if(internalResultCode!=0) {
                        Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp") as String)
                        String orderId = searchHitMap.getOrDefault("oms.orderId", "N/A") as String
                        String description = searchHitMap.getOrDefault("clientComment", "N/A") as String
                        String senderMsisdn = searchHitMap.getOrDefault("oms.sender.msisdn", "N/A") as String
                        List<HashMap<String, String>> receivers = searchHitMap.getOrDefault("oms.receivers", null)
                        String buyerDealerMsisdn = (receivers==null?"N/A":receivers[0].getOrDefault("msisdn", "N/A")) as String
                        String receiverId = (receivers==null?"N/A":receivers[0].getOrDefault("id", "N/A")) as String;
                        Map<String, Object> senderAddlField = searchHitMap.getOrDefault("oms.sender.additionalFields", null)
                        String dealerCode = senderAddlField == null ? "N/A" : senderAddlField.getOrDefault("dealer_code", "N/A") as String
                        String sellerTerminalId = senderAddlField == null ? "N/A" : senderAddlField.getOrDefault("epos_terminal_id", "N/A")
                        // String buyerDealerMsisdn = searchHitMap.getOrDefault("oms.buyer.buyerDealerMsisdn", "N/A") as String;
                        String failureCause = searchHitMap.getOrDefault("oms.resultMessage", "N/A") as String

                        String userName = "N/A"
                        String authCode = "N/A"
                        Map<String, Object> additionalField = searchHitMap.getOrDefault("oms.additionalFields", null)
                        if (additionalField != null) {
                            authCode = additionalField.getOrDefault("auth_code", "N/A") as String
                            userName = additionalField.getOrDefault("user_name", "N/A") as String
                        }

                        if (searchHitMap.getOrDefault("oms.items", null) != null) {
                            List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items")

                            for (int i = 0; i < omsItems.size(); i++) {
                                HashMap<String, String> omsItem = omsItems.get(i)
                                String productSku = omsItem.getOrDefault("productSku", "N/A") as String
                                String channel = searchHitMap.getOrDefault("channel", "N/A") as String

                                if (omsItem.getOrDefault("data", null) != null) {
                                    HashMap<String, String> data = omsItem.get("data")
                                    String subscriberMSISDN = data == null ? "N/A" : data.getOrDefault("SUBSCRIBERMSISDN", "N/A") as String
                                    if (receiverId.equals("SUBSCRIBER_ENTITY")) {
                                        buyerDealerMsisdn = subscriberMSISDN
                                    }
                                    String transactionReference = data.getOrDefault("transactionReference", "N/A") as String
                                    def amount = 0.0
                                    if (!data.getOrDefault("amount", "0").equals("N/A")) {
                                        amount = Double.parseDouble(data.getOrDefault("amount", "0"))
                                    }
                                    String commissionRate = data.getOrDefault("commissionPercentage", "N/A") as String
                                    String commissionAmount = data.getOrDefault("commissionAmount", "N/A") as String
                                    String commissionValueType = data.getOrDefault("commissionValueType", "N/A") as String

                                    if (commissionRate != "N/A" && !commissionRate.contains("<#if operationType")) {
                                        def commission = Double.parseDouble(commissionRate)
                                        commission /= 100
                                        commissionRate = commission as String
                                        if (commissionValueType.equalsIgnoreCase("Percentage")) {
                                            commissionRate = commissionRate + "%"
                                        }
                                    }

                                    String id = GenerateHash.createHashString(
                                            dateTimeDay.toString(),
                                            transactionReference,
                                            productSku
                                    )
                                    TransactionFailureModel transactionFailureModel = new TransactionFailureModel(id,
                                            transactionReference, orderId, internalResultCode+"", failureCause, channel, userName, description, dateTimeDay, senderMsisdn,
                                            dealerCode, sellerTerminalId, buyerDealerMsisdn, productSku, amount, commissionRate, commissionAmount, authCode)
                                    transactionFailureModelMap.put(id, transactionFailureModel)
                                }
                            }
                        }
                    }
                }
            }
            transactionFailureModelMap.each {
                entry -> transactionFailureModelList.add(entry.value)
            }
        }
        return transactionFailureModelList
    }
    private def insertAggregation(List<TransactionFailureModel> allTransactionDetailsReportModelList) {
        log.info("TransactionFailure  into ${allTransactionDetailsReportModelList.size()} rows.")
        if (allTransactionDetailsReportModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,order_id,transactionReference,resultCode,resultMesage,channel,user_name,clientComment,transaction_date,sender_msisdn,dealer_code,seller_terminal_id,buyer_msisdn,productsku,amount,commission_rate,commission_amount,auth_code) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE channel = VALUES(channel)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allTransactionDetailsReportModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.orderId)
                        ps.setString(++index, row.transactionReference)
                        ps.setString(++index, row.resultCode)
                        ps.setString(++index, row.resultMessage)
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.userName)
                        ps.setString(++index, row.description)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.sellerMsisdn)
                        ps.setString(++index, row.dealerCode)
                        ps.setString(++index, row.sellerTerminalId)
                        ps.setString(++index, row.buyerMsisdn)
                        ps.setString(++index, row.productSku)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.commissionRate)
                        ps.setString(++index, row.commissionAmount)
                        ps.setString(++index, row.authCode)
                    },
                    getBatchSize: { allTransactionDetailsReportModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class TransactionFailureModel {
    private String id
    private String transactionReference
    private String orderId
    private String resultCode
    private String resultMessage
    private String channel
    private String userName
    private String description
    private Date transactionDate
    private String sellerMsisdn
    private String dealerCode
    private String sellerTerminalId
    private String buyerMsisdn
    private String productSku
    private double amount
    private String commissionRate
    private String commissionAmount
    private String authCode

    TransactionFailureModel(String id,String transactionReference,  String orderId, String resultCode,String resultMessage,
                            String channel, String userName,String description,Date transactionDate,String sellerMsisdn,String dealerCode,
                            String sellerTerminalId,String buyerMsisdn,String productSku,double amount,String commissionRate, String commissionAmount,String authCode)
    {
        this.id = id
        this.transactionDate = transactionDate
        this.channel = channel
        this.sellerTerminalId=sellerTerminalId
        this.description=description
        this.amount=amount
        this.sellerMsisdn=sellerMsisdn
        this.orderId=orderId
        this.transactionReference=transactionReference
        this.userName=userName
        this.productSku=productSku
        this.dealerCode=dealerCode
        this.authCode=authCode
        this.commissionRate=commissionRate
        this.commissionAmount=commissionAmount
        this.resultCode=resultCode
        this.resultMessage=resultMessage
        this.buyerMsisdn=buyerMsisdn
    }
    String getResultCode() {
        return resultCode
    }

    void setResultCode(String resultCode) {
        this.resultCode = resultCode
    }

    String getResultMessage() {
        return resultMessage
    }

    void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage
    }

    String getBuyerMsisdn() {
        return buyerMsisdn
    }

    void setBuyerMsisdn(String buyerMsisdn) {
        this.buyerMsisdn = buyerMsisdn
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

    String getChannel() {
        return channel
    }

    void setChannel(String channel) {
        this.channel = channel
    }


    String getSellerTerminalId() {
        return sellerTerminalId
    }

    void setSellerTerminalId(String sellerTerminalId) {
        this.sellerTerminalId = sellerTerminalId
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    double getAmount() {
        return amount
    }

    void setAmount(double amount) {
        this.amount = amount
    }

    String getSellerMsisdn() {
        return sellerMsisdn
    }

    void setSellerMsisdn(String sellerMsisdn) {
        this.sellerMsisdn = sellerMsisdn
    }

    String getOrderId() {
        return orderId
    }

    void setOrderId(String orderId) {
        this.orderId = orderId
    }

    String getTransactionReference() {
        return transactionReference
    }

    void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference
    }

    String getUserName() {
        return userName
    }

    void setUserName(String userName) {
        this.userName = userName
    }

    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }

    String getDealerCode() {
        return dealerCode
    }

    void setDealerCode(String dealerCode) {
        this.dealerCode = dealerCode
    }

    String getAuthCode() {
        return authCode
    }

    void setAuthCode(String authCode) {
        this.authCode = authCode
    }

    String getCommissionGroup() {
        return commissionGroup
    }

    void setCommissionGroup(String commissionGroup) {
        this.commissionGroup = commissionGroup
    }

    String getCommissionRate() {
        return commissionRate
    }

    void setCommissionRate(String commissionRate) {
        this.commissionRate = commissionRate
    }

    String getCommissionAmount() {
        return commissionAmount
    }

    void setCommissionAmount(String commissionAmount) {
        this.commissionAmount = commissionAmount
    }
}