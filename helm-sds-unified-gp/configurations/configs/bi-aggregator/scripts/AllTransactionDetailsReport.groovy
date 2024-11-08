package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
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

@Slf4j
public class AllTransactionDetailsReport extends AbstractAggregator {

    static final def TABLE = "all_transaction_details";

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${AllTransactionDetailsReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${AllTransactionDetailsReport.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;


    @Value('${AllTransactionDetailsReport.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${AllTransactionDetailsReport.currency:BDT}')
    String currency

    @Value('${AllTransactionDetailsReport.operationType:CREDIT_TRANSFER,TOPUP}')
    String operationType;

    @Value('${AllTransactionDetailsReport.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${AllTransactionDetailsReport.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** AllTransactionDetailsReport Aggregator started at " + new Date());
        def transactionProfiles = operationType.split(",")
        if (bulkInsertionMode) {
            log.info("Bulk Insertion Mode is Active******************");
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<AllTransactionDetailsReportModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, transactionProfiles)
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
                List<AllTransactionDetailsReportModel> transactionSummaryModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), transactionProfiles);
                insertAggregation(transactionSummaryModels);
            }
        }
        log.info("********** AllTransactionDetailsReport Aggregator ended at " + new Date());
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileIds) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (!bulkInsertionMode) {

            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileIds))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileIds))
                    .filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        return searchSourceBuilder;
    }

    private List<AllTransactionDetailsReportModel> aggregateDataES(String index, String fromDate, String toDate, String[] profileIds) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIds);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<AllTransactionDetailsReportModel> transactionSummaryModels = generateResponse(searchResponse);
        String scrollId = searchResponse.getScrollId();
        log.info("_________________search hits size for the first time ____________ :" + searchResponse.getHits().size())
        while (searchResponse.getHits().size() != 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(5));
            searchResponse = generateScrollSearchResponse(scrollRequest);
            log.info("_________________search hits size inside loop ___________________ :" + searchResponse.getHits().size())
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

    private List<AllTransactionDetailsReportModel> generateResponse(SearchResponse searchResponse) {
        List<AllTransactionDetailsReportModel> allTransactionDetailsReportModelList = new ArrayList<>();
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);


            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {

                    Map<String, String> searchHitMap = searchHit.getSourceAsMap();
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("endTime"));
                    String id = GenerateHash.createHashString(searchHitMap.get("senderResellerId"), searchHitMap.get("ersReference"));
                    Double senderBalanceAfter = 0.0
                    if(!StringUtils.isEmpty(searchHitMap.get("senderBalanceValueAfter"))){

                        senderBalanceAfter = Double.valueOf(searchHitMap.get("senderBalanceValueAfter"))
                    }
                    Double transactionAmount = 0.0
                    if(null!=(searchHitMap.get("transactionAmount"))){

                        transactionAmount = searchHitMap.get("transactionAmount")
                    }
                    AllTransactionDetailsReportModel allTransactionDetailsReportModel = new AllTransactionDetailsReportModel(id, searchHitMap.get("senderMSISDN"),
                            dateTimeDay, searchHitMap.get("receiverMSISDN"), transactionAmount,
                            senderBalanceAfter, searchHitMap.get("ersReference"),
                            searchHitMap.get("transactionProfile"), searchHitMap.get("productSKU"),searchHitMap.get("transactionStatus"),searchHitMap.get("clientReference"),
                            searchHitMap.get("senderResellerId"),searchHitMap.get("senderResellerType"),searchHitMap.get("senderResellerPath"),searchHitMap.get("receiverResellerId"),searchHitMap.get("receiverResellerType"),searchHitMap.get("receiverResellerPath"))

                    allTransactionDetailsReportModelList.add(allTransactionDetailsReportModel)

                }
            }
        }
        return allTransactionDetailsReportModelList
    }


    private def insertAggregation(List<AllTransactionDetailsReportModel> allTransactionDetailsReportModelList) {
        log.info("AllTransactionDetailsReport Aggregated into ${allTransactionDetailsReportModelList.size()} rows.")
        if (allTransactionDetailsReportModelList.size() != 0) {
            def sql = "INSERT IGNORE INTO ${TABLE} (id,transaction_date,transaction_reference,client_reference," +
                    "seller_msisdn,seller_closing_balance,buyer_msisdn,amount,operation_type," +
                    "productsku,transaction_status, seller_id,seller_dealer_type,sender_reseller_path,buyer_id,buyer_reseller_type,receiver_reseller_path) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allTransactionDetailsReportModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.transactionReference)
                        ps.setString(++index, row.clientReference)
                        ps.setString(++index, row.sellerMsisdn)
                        ps.setDouble(++index, row.sellerClosingBalance)
                        ps.setString(++index, row.buyerMsisdn)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.operationType)
                        ps.setString(++index, row.productSku)
                        ps.setString(++index, row.transactionStatus)
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.sellerType)
                        ps.setString(++index, row.senderResellerPath)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.buyerType)
                        ps.setString(++index, row.buyerResellerPath)
                    },
                    getBatchSize: { allTransactionDetailsReportModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class AllTransactionDetailsReportModel {
    private String id;
    private String sellerMsisdn;
    private Date transactionDate;
    private String buyerMsisdn;
    private double amount;
    private double sellerClosingBalance;
    private String transactionReference;
    private String operationType;
    private String productSku
    private String transactionStatus;
    private String clientReference
    private String sellerId
    private String sellerType
    private String senderResellerPath
    private String buyerId
    private String buyerType
    private String buyerResellerPath

    AllTransactionDetailsReportModel(String id, String sellerMsisdn,Date transactionDate, String buyerMsisdn, double amount, double sellerClosingBalance, String transactionReference, String operationType, String productSku, String transactionStatus, String clientReference, String sellerId, String sellerType, String senderResellerPath, String buyerId, String buyerType, String buyerResellerPath) {
        this.id = id
        this.sellerMsisdn = sellerMsisdn
        this.transactionDate = transactionDate
        this.buyerMsisdn = buyerMsisdn
        this.amount = amount
        this.sellerClosingBalance = sellerClosingBalance
        this.transactionReference = transactionReference
        this.operationType = operationType
        this.productSku = productSku
        this.transactionStatus = transactionStatus
        this.clientReference = clientReference
        this.sellerId = sellerId
        this.sellerType = sellerType
        this.senderResellerPath = senderResellerPath
        this.buyerId = buyerId
        this.buyerType = buyerType
        this.buyerResellerPath = buyerResellerPath
    }

    String getSellerId() {
        return sellerId
    }

    void setSellerId(String sellerId) {
        this.sellerId = sellerId
    }

    String getSellerType() {
        return sellerType
    }

    void setSellerType(String sellerType) {
        this.sellerType = sellerType
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getSellerMsisdn() {
        return sellerMsisdn
    }

    void setSellerMsisdn(String sellerMsisdn) {
        this.sellerMsisdn = sellerMsisdn
    }

    Date getTransactionDate() {
        return transactionDate
    }

    void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
    }

    String getBuyerMsisdn() {
        return buyerMsisdn
    }

    void setBuyerMsisdn(String buyerMsisdn) {
        this.buyerMsisdn = buyerMsisdn
    }

    double getAmount() {
        return amount
    }

    void setAmount(double amount) {
        this.amount = amount
    }

    double getSellerClosingBalance() {
        return sellerClosingBalance
    }

    void setSellerClosingBalance(double sellerClosingBalance) {
        this.sellerClosingBalance = sellerClosingBalance
    }

    String getTransactionReference() {
        return transactionReference
    }

    void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference
    }

    String getOperationType() {
        return operationType
    }

    void setOperationType(String operationType) {
        this.operationType = operationType
    }

    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }

    String getTransactionStatus() {
        return transactionStatus
    }

    void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus
    }

    String getClientReference() {
        return clientReference
    }

    void setClientReference(String clientReference) {
        this.clientReference = clientReference
    }

    String getSenderResellerPath() {
        return senderResellerPath
    }

    void setSenderResellerPath(String senderResellerPath) {
        this.senderResellerPath = senderResellerPath
    }

    String getBuyerId() {
        return buyerId
    }

    void setBuyerId(String buyerId) {
        this.buyerId = buyerId
    }

    String getBuyerType() {
        return buyerType
    }

    void setBuyerType(String buyerType) {
        this.buyerType = buyerType
    }

    String getBuyerResellerPath() {
        return buyerResellerPath
    }

    void setBuyerResellerPath(String buyerResellerPath) {
        this.buyerResellerPath = buyerResellerPath
    }
}