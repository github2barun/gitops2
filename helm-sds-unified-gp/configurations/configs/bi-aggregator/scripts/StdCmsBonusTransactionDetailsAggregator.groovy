import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
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
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.Time
import java.sql.Timestamp

@Slf4j
public class StdCmsBonusTransactionDetailsAggregator extends AbstractAggregator {
    static final def TABLE = "std_transaction_details_aggregation";

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${StdCmsBonusTransactionDetailsAggregator.allowed_profiles:BONUS_TRANSFER}')
    String profileId

    @Value('${StdCmsBonusTransactionDetailsAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${StdCmsBonusTransactionDetailsAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${StdCmsBonusTransactionDetailsAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${StdCmsBonusTransactionDetailsAggregator.deleteDataAfterDays:60}')
    Integer deleteDataAfterDays;

    @Value('${StdCmsBonusTransactionDetailsAggregator.static_region.enabled:true}')
    boolean staticRegionEnabled

    @Value('${StdCmsBonusTransactionDetailsAggregator.static_region.table:Refill.commission_receivers}')
    String staticRegionTable

    @Value('${StdCmsBonusTransactionDetailsAggregator.static_region.table.column:rgroup}')
    String staticRegionColumn

    @Transactional
    @Scheduled(cron = '${StdCmsBonusTransactionDetailsAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("*************************************************** StdCmsBonusTransactionDetailsAggregator Aggregator started ********************************************" + new Date());
        def profileIdList = profileId.split(",")

        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList( bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList);

                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                catch (Exception e){
                    log.error(e.getMessage())
                }

            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex();

            for (ReportIndex index : indices) {

                log.info(index.toString())
                //fetch data from ES
                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList);

            }
        }

        log.info("*************************************** StdCmsBonusTransactionDetailsAggregator Aggregator ended ******************************************");
    }

    private void aggregateDataES(String index, String fromDate, String toDate,String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate,profileIdList);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(2));

        String scrollId= generateResponse(searchRequest);
        SearchResponse searchScrollResponse=client.search(searchRequest, COMMON_OPTIONS);
        log.info("_________________hits size outside loop for the first time_____________________"+searchScrollResponse.getHits().size())

        while(searchScrollResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueSeconds(30));
            log.info("******* Scroll Request:::: " + scrollRequest.toString());
            try {
                searchScrollResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
            }


            log.info("_________________hits size inside loop _____________________"+searchScrollResponse.getHits().size())

            scrollId = generateScrollResponse(searchScrollResponse);
        }
        cleanData()
    }


    private SearchSourceBuilder fetchInput(String fromDate, String toDate,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
            searchSourceBuilder.size(1000).sort("EndTime", SortOrder.ASC).query(queryBuilder);
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.size(1000).sort("EndTime", SortOrder.ASC).query(queryBuilder);
        }

        return searchSourceBuilder;
    }


    private String generateResponse(SearchRequest searchRequest) {
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.info("******* Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.info("response status -------------" + status);
        if (status == RestStatus.OK) {
            SearchHits searchHits = searchResponse.getHits();
            log.info("Total search hits founds = " + searchHits.getHits().size())
            prepareAndInsertTransactions(searchHits)
        }
        return searchResponse.getScrollId();
    }

    private String generateScrollResponse(SearchResponse searchScrollResponse)
    {
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchScrollResponse.getHits();
            log.info("no of hits after 1st request" + searchHits.size());
            prepareAndInsertTransactions(searchHits)
        }
        return searchScrollResponse.getScrollId();
    }

    private void prepareAndInsertTransactions(SearchHits searchHits)
    {
        log.info("..... preparing to insert transactions ......")
        List<CmsBonusTransactionDetailModel> cmsBonusTransactionDetailModelArrayList = new ArrayList<>();
        for (SearchHit searchHit : searchHits.getHits()) {
            Map<String, String> searchHitMap = searchHit.getSourceAsMap()

            Date transactionDate = DateFormatter.formatDate(searchHitMap.get("EndTime"))
            Timestamp endTime = new Timestamp(transactionDate.getTime())
            log.info("endTime = " + endTime)

            Timestamp date = endTime.clearTime()
            log.info("date = " + date)

            String ersReference = searchHitMap.get("ErsReference")
            log.info("ersReference = " + ersReference)

            String resellerId = searchHitMap.get("SenderResellerId")
            log.info("resellerId = " + resellerId)

            String resellerName = searchHitMap.get("SenderResellerName")
            log.info("resellerName = " + resellerName)

            String resellerTypeId = searchHitMap.get("SenderResellerType")
            log.info("resellerTypeId = " + resellerTypeId)

            String resellerMSISDN = searchHitMap.get("SenderMSISDN")
            log.info("resellerMSISDN = " + resellerMSISDN)

            String resellerParent = searchHitMap.get("SenderResellerParent")
            log.info("resellerParent = " + resellerParent)

            String resellerPath = searchHitMap.get("SenderResellerPath")
            log.info("resellerPath = " + resellerPath)

            String receiverMSISDN = searchHitMap.get("ReceiverMSISDN")
            log.info("receiverMSISDN = " + receiverMSISDN)

            String transactionType = searchHitMap.get("TransactionProfile")
            log.info("transactionType = " + transactionType)

            String transactionResult = searchHitMap.get("ResultCode")
            log.info("transactionResult = " + transactionResult)

            String channel = searchHitMap.get("Channel")
            log.info("channel = " + channel)

            String resultStatus = searchHitMap.get("ResultStatus")
            log.info("resultStatus = " + resultStatus)

            Double resellerOpeningBalance
            if (searchHitMap.get("SenderBalanceValueBefore") != null)
            {
                resellerOpeningBalance = Double.valueOf(searchHitMap.get("SenderBalanceValueBefore"))
            }
            else
            {
                resellerOpeningBalance = 0
            }
            log.info("resellerOpeningBalance = " + resellerOpeningBalance.toString())

            Double resellerClosingBalance
            if (searchHitMap.get("SenderBalanceValueAfter") != null)
            {
                resellerClosingBalance = Double.valueOf(searchHitMap.get("SenderBalanceValueAfter"))
            }
            else
            {
                resellerClosingBalance = 0
            }
            log.info("resellerClosingBalance = " + resellerClosingBalance.toString())

            Double receiverOpeningBalance
            if (searchHitMap.get("ReceiverBalanceValueBefore") != null)
            {
                receiverOpeningBalance = Double.valueOf(searchHitMap.get("ReceiverBalanceValueBefore"))
            }
            else
            {
                receiverOpeningBalance = 0
            }
            log.info("receiverOpeningBalance = " + receiverOpeningBalance.toString())

            Double receiverClosingBalance
            if (searchHitMap.get("ReceiverBalanceValueAfter") != null)
            {
                receiverClosingBalance = Double.valueOf(searchHitMap.get("ReceiverBalanceValueAfter"))
            }
            else
            {
                receiverClosingBalance = 0
            }
            log.info("receiverClosingBalance = " + receiverClosingBalance.toString())

            Double transactionAmount
            if (searchHitMap.get("TransactionAmount") != null)
            {
                transactionAmount = Double.valueOf(searchHitMap.get("TransactionAmount"))
            }
            else
            {
                transactionAmount = 0
            }
            log.info("transactionAmount = " + transactionAmount.toString())

            String region  = searchHitMap.get("SenderRegion")
            if (region == null || region == "")
            {
                region = "NO_REGION"
            }
            log.info("region = " + region)

            String senderAccountTypeId  = searchHitMap.get("SenderAccountType")
            log.info("senderAccountTypeId = " + senderAccountTypeId)

            String receiverAccountTypeId  = searchHitMap.get("ReceiverAccountType")
            log.info("receiverAccountTypeId = " + receiverAccountTypeId)

            CmsBonusTransactionDetailModel cmsBonusTransactionDetailModel = new CmsBonusTransactionDetailModel(ersReference, date, endTime, resellerId, resellerName, resellerTypeId, resellerMSISDN, resellerParent, resellerPath, receiverMSISDN, transactionType, transactionResult, channel, resultStatus, resellerOpeningBalance, transactionAmount, resellerClosingBalance, receiverClosingBalance, receiverOpeningBalance, region, senderAccountTypeId, receiverAccountTypeId)
            cmsBonusTransactionDetailModelArrayList.add(cmsBonusTransactionDetailModel)
        }
        log.info("dailyTransactionSummaryModelArrayList size = " + cmsBonusTransactionDetailModelArrayList.size())
        insertAggregation(cmsBonusTransactionDetailModelArrayList);
    }
    private def insertAggregation(List cmsBonusTransactionDetailModelArrayList)
    {
        if (cmsBonusTransactionDetailModelArrayList.size() != 0) {
            def regionSql
            if (staticRegionEnabled) {
                regionSql = "( SELECT ${staticRegionColumn} FROM ${staticRegionTable} WHERE tag = ? )"
            }
            def sql = "REPLACE INTO ${TABLE} (ers_reference, date, reseller_id, reseller_msisdn, reseller_parent, reseller_path, receiver_msisdn, transaction_type, reseller_opening_balance, transaction_amount, reseller_closing_balance,reseller_name,receiver_opening_balance,receiver_closing_balance,end_time,reseller_type_id,transaction_result, region, channel, status, sender_reseller_account_type_id, receiver_reseller_account_type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?, " + (staticRegionEnabled ? regionSql : "?") + " , ?, ?, ?, ?)"
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   :
                            { ps, i ->
                                def row = cmsBonusTransactionDetailModelArrayList[i]
                                int index = 0
                                ps.setString(++index, row.ersReference)
                                ps.setTimestamp(++index, row.date)
                                if (row.resellerId)
                                    ps.setString(++index, row.resellerId)
                                else
                                    ps.setString(++index, "")

                                if (row.resellerMSISDN)
                                    ps.setString(++index, row.resellerMSISDN)
                                else
                                    ps.setString(++index, "")

                                ps.setString(++index, row.resellerParent)
                                ps.setString(++index, row.resellerPath)
                                if (row.receiverMSISDN)
                                    ps.setString(++index, row.receiverMSISDN)
                                else
                                    ps.setString(++index, "")
                                ps.setString(++index, row.transactionType)
                                ps.setBigDecimal(++index, row.resellerOpeningBalance)
                                ps.setBigDecimal(++index, row.transactionAmount)
                                ps.setBigDecimal(++index, row.resellerClosingBalance)
                                ps.setString(++index, row.resellerName)
                                ps.setBigDecimal(++index, row.receiverOpeningBalance)
                                ps.setBigDecimal(++index, row.receiverClosingBalance)
                                ps.setTimestamp(++index, row.endTime)
                                ps.setString(++index, row.resellerTypeId)
                                ps.setString(++index, row.transactionResult)

                                if (staticRegionEnabled) {
                                    ps.setString(++index, row.resellerId)
                                } else {
                                    ps.setString(++index, row.region)
                                }

                                ps.setString(++index, row.channel)
                                ps.setString(++index, row.resultStatus)
                                ps.setString(++index, row.senderAccountTypeId)
                                ps.setString(++index, row.receiverAccountTypeId)

                            },
                    getBatchSize:
                            { cmsBonusTransactionDetailModelArrayList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }


    class CmsBonusTransactionDetailModel
    {
        private String ersReference
        private Timestamp date
        private Timestamp endTime
        private String resellerId
        private String resellerName
        private String resellerTypeId
        private String resellerMSISDN
        private String resellerParent
        private String resellerPath
        private String receiverMSISDN
        private String transactionType
        private String transactionResult
        private String channel
        private String resultStatus
        private Double resellerOpeningBalance
        private Double transactionAmount
        private Double resellerClosingBalance
        private Double receiverClosingBalance
        private Double receiverOpeningBalance
        private String region
        private String senderAccountTypeId
        private String receiverAccountTypeId

        CmsBonusTransactionDetailModel(String ersReference, Timestamp date, Timestamp endTime, String resellerId, String resellerName, String resellerTypeId, String resellerMSISDN, String resellerParent, String resellerPath, String receiverMSISDN, String transactionType, String transactionResult, String channel, String resultStatus, Double resellerOpeningBalance, Double transactionAmount, Double resellerClosingBalance, Double receiverClosingBalance, Double receiverOpeningBalance, String region, String senderAccountTypeId, String receiverAccountTypeId) {
            this.ersReference = ersReference
            this.date = date
            this.endTime = endTime
            this.resellerId = resellerId
            this.resellerName = resellerName
            this.resellerTypeId = resellerTypeId
            this.resellerMSISDN = resellerMSISDN
            this.resellerParent = resellerParent
            this.resellerPath = resellerPath
            this.receiverMSISDN = receiverMSISDN
            this.transactionType = transactionType
            this.transactionResult = transactionResult
            this.channel = channel
            this.resultStatus = resultStatus
            this.resellerOpeningBalance = resellerOpeningBalance
            this.transactionAmount = transactionAmount
            this.resellerClosingBalance = resellerClosingBalance
            this.receiverClosingBalance = receiverClosingBalance
            this.receiverOpeningBalance = receiverOpeningBalance
            this.region = region
            this.senderAccountTypeId = senderAccountTypeId
            this.receiverAccountTypeId = receiverAccountTypeId
        }

        String getErsReference() {
            return ersReference
        }

        void setErsReference(String ersReference) {
            this.ersReference = ersReference
        }

        Timestamp getDate() {
            return date
        }

        void setDate(Timestamp date) {
            this.date = date
        }

        Timestamp getEndTime() {
            return endTime
        }

        void setEndTime(Timestamp endTime) {
            this.endTime = endTime
        }

        String getResellerId() {
            return resellerId
        }

        void setResellerId(String resellerId) {
            this.resellerId = resellerId
        }

        String getResellerName() {
            return resellerName
        }

        void setResellerName(String resellerName) {
            this.resellerName = resellerName
        }

        String getResellerTypeId() {
            return resellerTypeId
        }

        void setResellerTypeId(String resellerTypeId) {
            this.resellerTypeId = resellerTypeId
        }

        String getResellerMSISDN() {
            return resellerMSISDN
        }

        void setResellerMSISDN(String resellerMSISDN) {
            this.resellerMSISDN = resellerMSISDN
        }

        String getResellerParent() {
            return resellerParent
        }

        void setResellerParent(String resellerParent) {
            this.resellerParent = resellerParent
        }

        String getResellerPath() {
            return resellerPath
        }

        void setResellerPath(String resellerPath) {
            this.resellerPath = resellerPath
        }

        String getReceiverMSISDN() {
            return receiverMSISDN
        }

        void setReceiverMSISDN(String receiverMSISDN) {
            this.receiverMSISDN = receiverMSISDN
        }

        String getTransactionType() {
            return transactionType
        }

        void setTransactionType(String transactionType) {
            this.transactionType = transactionType
        }

        String getTransactionResult() {
            return transactionResult
        }

        void setTransactionResult(String transactionResult) {
            this.transactionResult = transactionResult
        }

        String getChannel() {
            return channel
        }

        void setChannel(String channel) {
            this.channel = channel
        }

        String getResultStatus() {
            return resultStatus
        }

        void setResultStatus(String resultStatus) {
            this.resultStatus = resultStatus
        }

        Double getResellerOpeningBalance() {
            return resellerOpeningBalance
        }

        void setResellerOpeningBalance(Double resellerOpeningBalance) {
            this.resellerOpeningBalance = resellerOpeningBalance
        }

        Double getTransactionAmount() {
            return transactionAmount
        }

        void setTransactionAmount(Double transactionAmount) {
            this.transactionAmount = transactionAmount
        }

        Double getResellerClosingBalance() {
            return resellerClosingBalance
        }

        void setResellerClosingBalance(Double resellerClosingBalance) {
            this.resellerClosingBalance = resellerClosingBalance
        }

        Double getReceiverClosingBalance() {
            return receiverClosingBalance
        }

        void setReceiverClosingBalance(Double receiverClosingBalance) {
            this.receiverClosingBalance = receiverClosingBalance
        }

        Double getReceiverOpeningBalance() {
            return receiverOpeningBalance
        }

        void setReceiverOpeningBalance(Double receiverOpeningBalance) {
            this.receiverOpeningBalance = receiverOpeningBalance
        }

        String getRegion() {
            return region
        }

        void setRegion(String region) {
            this.region = region
        }

        String getSenderAccountTypeId() {
            return senderAccountTypeId
        }

        void setSenderAccountTypeId(String senderAccountTypeId) {
            this.senderAccountTypeId = senderAccountTypeId
        }

        String getReceiverAccountTypeId() {
            return receiverAccountTypeId
        }

        void setReceiverAccountTypeId(String receiverAccountTypeId) {
            this.receiverAccountTypeId = receiverAccountTypeId
        }
    }

    private def cleanData() {
        log.info("StdTransactionDetailsAggregator --> cleaning data from before " + deleteDataAfterDays + " days.")
        def query = "DELETE FROM ${TABLE} WHERE date <= DATE_SUB(NOW() , INTERVAL ${deleteDataAfterDays} DAY)"
        jdbcTemplate.update(query)
    }



}
