package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.core.TimeValue;
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

import java.sql.Timestamp
import java.util.regex.Pattern

@Slf4j
public class StdDailyTransactionSummaryAggregator extends AbstractAggregator {
    static final def TABLE = "std_daily_transaction_summary_aggregation";

    static final def SUPPORTING_TABLE = "std_daily_transaction_summary_aggregation_additional_details";

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${StdDailyTransactionSummaryAggregator.allowed_profiles:TOPUP,REVERSE_TOPUP,CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,PRODUCT_RECHARGE,VOUCHER_PURCHASE,VOS_PURCHASE,VOT_PURCHASE,PURCHASE,DATA_BUNDLE,COMBO_BUNDLE,SMS_BUNDLE,IDD_BUNDLE,FIBER_BUNDLE,MM2ERS}')
    String profileId

    @Value('${StdDailyTransactionSummaryAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${StdDailyTransactionSummaryAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${StdDailyTransactionSummaryAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${StdDailyTransactionSummaryAggregator.deleteDataAfterDays:60}')
    Integer deleteDataAfterDays;

    @Value('${StdDailyTransactionSummaryAggregator.static_region.enabled:true}')
    boolean staticRegionEnabled

    @Value('${StdDailyTransactionSummaryAggregator.static_region.table:Refill.commission_receivers}')
    String staticRegionTable

    @Value('${StdDailyTransactionSummaryAggregator.static_region.table.column:rgroup}')
    String staticRegionColumn

    @Transactional
    @Scheduled(cron = '${StdDailyTransactionSummaryAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("*************************************************** StdDailyTransactionSummaryAggregator Aggregator started ********************************************" + new Date());
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

        log.info("*************************************** StdDailyTransactionSummaryAggregator Aggregator ended ******************************************");
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
    }


    private SearchSourceBuilder fetchInput(String fromDate, String toDate,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
            searchSourceBuilder.size(1000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
            searchSourceBuilder.size(1000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
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

    private void prepareAndInsertTransactions(SearchHits searchHits)
    {
        log.info("..... preparing to insert transactions ......")
        List<DailyTransactionSummaryModel> dailyTransactionSummaryModelArrayList = new ArrayList<>();
        for (SearchHit searchHit : searchHits.getHits())
        {
            Map<String, String> searchHitMap = searchHit.getSourceAsMap();
            Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("startTime"));
            Timestamp transactionDate = new Timestamp(dateTimeDay.getTime());
            log.info("transactionDate = " + transactionDate)
            Integer transactionHour = transactionDate.getHours()
            log.info("transactionHour = " + transactionHour)
            String transactionReference = searchHitMap.get("ersReference")
            log.info("transactionReference = " + transactionReference)
            String senderMSISDN = searchHitMap.get("senderMSISDN")
            log.info("senderMSISDN = " + senderMSISDN)
            String senderResellerId = searchHitMap.get("senderResellerId")
            log.info("senderResellerId = " + senderResellerId)
            String receiverMSISDN = searchHitMap.get("receiverMSISDN")
            log.info("receiverMSISDN = " + receiverMSISDN)
            String tranProps = searchHitMap.get("transactionProps")

            String displayReceiverMSISDN = getValueByKeyFromTransactionProperties(tranProps, "DISPLAY_RECEIVER_MSISDN", "|", "=")
            log.info("displayReceiverMSISDN = " + displayReceiverMSISDN)

            if (displayReceiverMSISDN == "")
            {
                displayReceiverMSISDN = receiverMSISDN
            }

            String receiverResellerId = searchHitMap.get("receiverResellerId")
            if (isNullOrEmptyData(receiverResellerId))
            {
                receiverResellerId = receiverMSISDN
            }
            log.info("receiverResellerId = " + receiverResellerId)
            String transactionType = searchHitMap.get("transactionProfile")
            log.info("transactionType = " + transactionType)
            Double amount
            if (!isNullOrEmptyData(searchHitMap.get("transactionAmount")))
            {
                amount = Double.valueOf(searchHitMap.get("transactionAmount"))
            }
            else
            {
                amount = 0
            }
            log.info("amount = " + amount.toString())
            String channel = searchHitMap.get("channel")
            log.info("channel = " + channel)

            String transactionStatus = searchHitMap.get("resultMessage")
            String resultStatus = transactionStatus
            if ("SUCCESS".equalsIgnoreCase(transactionStatus))
            {
                resultStatus = "Success"
            }
            log.info("resultStatus = " + resultStatus)
            String externalId = searchHitMap.get("dealerSequenceNo")
            if (isNullOrEmptyData(externalId))
            {
                externalId = searchHitMap.get("clientReference")
            }
            log.info("externalId = " + externalId)

            Double senderBalanceBefore
            if (!isNullOrEmptyData(searchHitMap.get("senderBalanceValueBefore")))
            {
                senderBalanceBefore = Double.valueOf(searchHitMap.get("senderBalanceValueBefore"))
            }
            else
            {
                senderBalanceBefore = 0
            }
            log.info("senderBalanceBefore = " + senderBalanceBefore.toString())
            Double senderBalanceAfter
            if (!isNullOrEmptyData(searchHitMap.get("senderBalanceValueAfter")))
            {
                senderBalanceAfter = Double.valueOf(searchHitMap.get("senderBalanceValueAfter"))

            }
            else
            {
                senderBalanceAfter = 0
            }
            log.info("senderBalanceAfter = " + senderBalanceAfter.toString())

            String currency = searchHitMap.get("currency")
            log.info("currency = " + currency)
            String resultDescription  = searchHitMap.get("resultMessage")
            if (isNullOrEmptyData(resultDescription))
            {
                resultDescription = ""
            }
            log.info("resultDescription = " + resultDescription)
            Double receiverBalanceBefore
            if (!isNullOrEmptyData(searchHitMap.get("receiverBalanceValueBefore")))
            {
                receiverBalanceBefore = Double.valueOf(searchHitMap.get("receiverBalanceValueBefore"))
            }
            else
            {
                receiverBalanceBefore = 0
            }
            log.info("receiverBalanceBefore = " + receiverBalanceBefore.toString())
            Double receiverBalanceAfter
            if (!isNullOrEmptyData(searchHitMap.get("receiverBalanceValueAfter")))
            {
                receiverBalanceAfter = Double.valueOf(searchHitMap.get("receiverBalanceValueAfter"))
            }
            else
            {
                receiverBalanceAfter = 0
            }
            log.info("receiverBalanceAfter = " + receiverBalanceAfter.toString())

            // reseller commission not found in dump but sender commission
            Double resellerCommission
            if (!isNullOrEmptyData(searchHitMap.get("senderCommission")))
            {
                String[] resellerCommissionList = searchHitMap.get("senderCommission").split();
                resellerCommission = Double.valueOf(resellerCommissionList[0].replaceAll(",",""))
            }
            else
            {
                resellerCommission = 0
            }
            log.info("resellerCommission = " + resellerCommission.toString())

            // reseller bonus amount not found in dump
            Double resellerBonus
            if (!isNullOrEmptyData(searchHitMap.get("senderBonusAmount")))
            {
                String[] resellerBonusList = searchHitMap.get("senderBonusAmount").split()
                resellerBonus = Double.valueOf(resellerBonusList[0].replaceAll(",",""))
            }
            else
            {
                resellerBonus = 0
            }
            log.info("resellerBonus = " + resellerBonus.toString())
            Double receiverResellerBonus

            if(!isNullOrEmptyData(searchHitMap.get("receiverBonusAmount"))) {
                String[] receiverBonus = searchHitMap.get("receiverBonusAmount").split();
                receiverResellerBonus = Double.valueOf(receiverBonus[0].replaceAll(",",""))
            }
            else {
                receiverResellerBonus = 0
            }
            log.info("receiverResellerBonus = " + receiverResellerBonus.toString())

            Double receiverResellerCommission
            if(!isNullOrEmptyData(searchHitMap.get("receiverCommission"))) {
                String[] receiverCommission = searchHitMap.get("receiverCommission").split();
                receiverResellerCommission = Double.valueOf(receiverCommission[0].replaceAll(",",""))
            }
            else {
                receiverResellerCommission = 0
            }
            log.info("receiverResellerCommission = " + receiverResellerCommission.toString())

            String senderResellerName  = searchHitMap.get("senderResellerName")
            log.info("senderResellerName = " + senderResellerName)
            String receiverResellerName  = searchHitMap.get("receiverResellerName")
            log.info("receiverResellerName = " + receiverResellerName)

            // not found in dump
            String resellerParent  = searchHitMap.get("SenderResellerParent")
            log.info("resellerParent = " + resellerParent)
            String region  = searchHitMap.get("senderRegionId")
            if (isNullOrEmptyData(region))
            {
                region = "NO_REGION"
            }
            log.info("region = " + region)
            String batchId  = searchHitMap.get("bulkBatchId")
            log.info("batchId = " + batchId)
            String senderAccountTypeId  = searchHitMap.get("senderAccountType")
            log.info("senderAccountTypeId = " + senderAccountTypeId)
            String receiverAccountTypeId  = searchHitMap.get("receiverAccountType")
            log.info("receiverAccountTypeId = " + receiverAccountTypeId)

            Date transactionEndTime = DateFormatter.formatDate(searchHitMap.get("endTime"));
            Timestamp transactionEndDate = new Timestamp(transactionEndTime.getTime());
            log.info("transactionEndDate = " + transactionEndDate)

            String clientComment  = searchHitMap.get("clientComment")
            log.info("clientComment = " +  clientComment)

            String originalErsRef = searchHitMap.get("originalErsReference") == null ? "" : searchHitMap.get("originalErsReference");
            log.info("originalErsRef" + originalErsRef )

            Double resultCode = Double.valueOf(searchHitMap.get("resultCode"));
            log.info("resultCode" + resultCode)

            String transactionProfile = searchHitMap.get("transactionProfile");
            log.info("transactionProfile" + transactionProfile)

            String productName = searchHitMap.get("productName");
            log.info("productName" + productName)

            String resource = searchHitMap.get("productSKU");
            log.info("resource" + resource)


            DailyTransactionSummaryModel dailyTransactionSummaryModel =
                    new DailyTransactionSummaryModel(transactionDate, transactionHour, transactionReference, senderMSISDN, senderResellerId, receiverMSISDN, displayReceiverMSISDN
                            , receiverResellerId, transactionType, amount, channel, resultStatus, externalId, senderBalanceBefore, senderBalanceAfter, currency
                            , resultDescription, receiverBalanceBefore, receiverBalanceAfter, resellerCommission, resellerBonus, senderResellerName
                            , receiverResellerName, resellerParent, region, batchId, senderAccountTypeId, receiverAccountTypeId
                            , transactionEndDate, clientComment, originalErsRef, resultCode, transactionProfile, productName, resource, receiverResellerCommission, receiverResellerBonus);
            dailyTransactionSummaryModelArrayList.add(dailyTransactionSummaryModel);
        }
        log.info("dailyTransactionSummaryModelArrayList size = " + dailyTransactionSummaryModelArrayList.size())
        insertAggregation(dailyTransactionSummaryModelArrayList);
    }

    /**
     * This method is used to get the value against a key
     * that is present in transactionProperties string
     *
     * e.g. eanCode=1232|TAX=WHT|productType=SERVICES
     * it'll get WHT when key TAX is passed
     * with outerDelimiter as |
     * innerDelimiter as =
     *
     * @param testString
     * @param key
     * @param outerDelimiter
     * @param innerDelimiter
     * @return
     */
    private String getValueByKeyFromTransactionProperties(String testString, String key, String outerDelimiter, String innerDelimiter)
    {
        def outerParts = testString.split(Pattern.quote(outerDelimiter))
        for (def outerPart : outerParts) {
            def innerParts = outerPart.split(Pattern.quote(innerDelimiter))
            if (innerParts[0].equals(key)) {
                return innerParts[1]
            }
        }
        return ""
    }

    private static boolean isNullOrEmptyData(String data)
    {
        return null == data || data.trim().isEmpty();
    }

    private static boolean isNullOrEmptyData(Double data)
    {
        return null == data;
    }

    private String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceAll(replacement);
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

    def toSqlTimestamp =
            { Date date ->
                new java.sql.Timestamp(date.time)
            }

    private def insertAggregation(List dailyTransactionSummaryModelArrayList) {
        if (dailyTransactionSummaryModelArrayList.size() != 0)
        {
            log.info("deleting data for " + deleteDataAfterDays + " days old.")

            def date = new Date()
            use(TimeCategory)
                    {
                        date = toSqlTimestamp(date - deleteDataAfterDays.days)
                    }

            // first remove old data
            def delete = "delete from ${TABLE} where transactionDate < ?"
            jdbcTemplate.batchUpdate(delete, [
                    setValues   : { ps, i ->
                        ps.setTimestamp(1, toSqlTimestamp(date))
                    },
                    getBatchSize: { dailyTransactionSummaryModelArrayList.size() }
            ] as BatchPreparedStatementSetter)

            delete = "delete from ${SUPPORTING_TABLE} where transaction_date < ?"
            jdbcTemplate.batchUpdate(delete, [
                    setValues   : { ps, i ->
                        ps.setTimestamp(1, toSqlTimestamp(date))
                    },
                    getBatchSize: { dailyTransactionSummaryModelArrayList.size() }
            ] as BatchPreparedStatementSetter)

            log.info("StdDailyTransactionSummaryAggregator Aggregated into ${dailyTransactionSummaryModelArrayList.size()} rows.")
            def regionSql
            if (staticRegionEnabled) {
                regionSql = "( SELECT ${staticRegionColumn} FROM ${staticRegionTable} WHERE tag = ? )"
            }
            // then insert new data
            def sql = "REPLACE INTO ${TABLE} (transactionDate, transactionHour, transactionReference, senderMSISDN, senderResellerID, receiverMSISDN, displayReceiverMSISDN" +
                    ", receiverResellerID, transactionType, amount, channel, resultStatus, externalID, senderBalanceBefore, senderBalanceAfter, currency" +
                    ", resultDescription, receiverBalanceBefore, receiverBalanceAfter, resellerCommission, resellerBonus, senderResellerName, receiverResellerName" +
                    ", resellerParent, region, batchId, sender_reseller_account_type_id, receiver_reseller_account_type_id, receiverResellerCommission, receiverResellerBonus ) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, " + (staticRegionEnabled ? regionSql : "?") + ", ?, ?, ?,?,?)"

            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dailyTransactionSummaryModelArrayList[i]
                        def index = 0
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setInt(++index, row.transactionHour)
                        ps.setString(++index, row.transactionReference)
                        ps.setString(++index, row.senderMSISDN)
                        ps.setString(++index, row.senderResellerID)
                        ps.setString(++index, row.receiverMSISDN)
                        ps.setString(++index, row.displayReceiverMSISDN)
                        ps.setString(++index, row.receiverResellerID)
                        ps.setString(++index, row.transactionType)
                        ps.setBigDecimal(++index, row.amount)
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.resultStatus)
                        ps.setString(++index, row.externalID)
                        ps.setBigDecimal(++index, row.senderBalanceBefore)
                        ps.setBigDecimal(++index, row.senderBalanceAfter)
                        ps.setString(++index, row.currency)
                        ps.setString(++index, row.resultDescription)
                        ps.setBigDecimal(++index, row.receiverBalanceBefore)
                        ps.setBigDecimal(++index, row.receiverBalanceAfter)
                        ps.setBigDecimal(++index, row.resellerCommission)
                        ps.setBigDecimal(++index, row.resellerBonus)
                        ps.setString(++index, row.senderResellerName)
                        ps.setString(++index, row.receiverResellerName)
                        ps.setString(++index, row.resellerParent)
                        if (staticRegionEnabled) {
                            ps.setString(++index, row.senderResellerID)
                        } else {
                            ps.setString(++index, row.region)
                        }
                        ps.setString(++index, row.batchId)
                        ps.setString(++index, row.senderAccountTypeId)
                        ps.setString(++index, row.receiverAccountTypeId)
                        ps.setBigDecimal(++index, row.receiverResellerCommission)
                        ps.setBigDecimal(++index, row.receiverResellerBonus)
                    },
                    getBatchSize:
                            { dailyTransactionSummaryModelArrayList.size() }
            ] as BatchPreparedStatementSetter)

            // then insert new data to Supporting table
            def sqlSupporting = "REPLACE INTO ${SUPPORTING_TABLE} (transaction_end_date, transaction_date, transaction_reference, original_ers_reference" +
                    ", commission_class, client_comment, resource, product_name, transaction_profile, result_code) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)"

            def batchUpdateSupportingTable = jdbcTemplate.batchUpdate(sqlSupporting, [
                    setValues   : { ps, i ->
                        def row = dailyTransactionSummaryModelArrayList[i]
                        def index = 0
                        ps.setTimestamp(++index, row.transactionEndDate)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.transactionReference)
                        ps.setString(++index, row.originalErsRef)
                        ps.setString(++index, "NA")
                        ps.setString(++index, row.clientComment == null || "null".equals(row.clientComment) ? "" : row.clientComment)
                        ps.setString(++index, row.resource)
                        ps.setString(++index, row.productName)
                        ps.setString(++index, row.transactionProfile)
                        ps.setString(++index, row.resultCode.toString())
                    },
                    getBatchSize:
                            { dailyTransactionSummaryModelArrayList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }


    class DailyTransactionSummaryModel
    {
        private Timestamp transactionDate
        private Integer transactionHour
        private String transactionReference
        private String senderMSISDN
        private String senderResellerID
        private String receiverMSISDN
        private String displayReceiverMSISDN
        private String receiverResellerID
        private String transactionType
        private Double amount
        private String channel
        private String resultStatus
        private String externalID
        private Double senderBalanceBefore
        private Double senderBalanceAfter
        private String currency
        private String resultDescription
        private Double receiverBalanceBefore
        private Double receiverBalanceAfter
        private Double resellerCommission
        private Double resellerBonus
        private String senderResellerName
        private String receiverResellerName
        private String resellerParent
        private String region
        private String batchId
        private String senderAccountTypeId
        private String receiverAccountTypeId


        private Timestamp transactionEndDate
        private String clientComment
        private String commissionClass
        private String originalErsRef
        private Timestamp accountValidityUntil
        private Timestamp expiryBeforeRecharge
        private Timestamp expiryAfterRecharge
        private Double openingBalance
        private Double taxRate
        private Double tax
        private Double resultCode
        private String resource
        private String transactionProfile
        private String productName
        private Double receiverResellerCommission
        private Double receiverResellerBonus

        DailyTransactionSummaryModel(Timestamp transactionDate, Integer transactionHour, String transactionReference, String senderMSISDN, String senderResellerID
                                     , String receiverMSISDN, String displayReceiverMSISDN, String receiverResellerID, String transactionType, Double amount, String channel, String resultStatus, String externalID
                                     , Double senderBalanceBefore, Double senderBalanceAfter, String currency, String resultDescription, Double receiverBalanceBefore, Double receiverBalanceAfter
                                     , Double resellerCommission, Double resellerBonus, String senderResellerName, String receiverResellerName, String resellerParent, String region
                                     , String batchId, String senderAccountTypeId, String receiverAccountTypeId, Timestamp transactionEndDate,
                                     String clientComment, String originalErsRef, Double resultCode, String transactionProfile, String productName, String resource, Double receiverResellerCommission, Double receiverResellerBonus) {
            this.transactionDate = transactionDate
            this.transactionHour = transactionHour
            this.transactionReference = transactionReference
            this.senderMSISDN = senderMSISDN
            this.senderResellerID = senderResellerID
            this.receiverMSISDN = receiverMSISDN
            this.displayReceiverMSISDN = displayReceiverMSISDN
            this.receiverResellerID = receiverResellerID
            this.transactionType = transactionType
            this.amount = amount
            this.channel = channel
            this.resultStatus = resultStatus
            this.externalID = externalID
            this.senderBalanceBefore = senderBalanceBefore
            this.senderBalanceAfter = senderBalanceAfter
            this.currency = currency
            this.resultDescription = resultDescription
            this.receiverBalanceBefore = receiverBalanceBefore
            this.receiverBalanceAfter = receiverBalanceAfter
            this.resellerCommission = resellerCommission
            this.resellerBonus = resellerBonus
            this.senderResellerName = senderResellerName
            this.receiverResellerName = receiverResellerName
            this.resellerParent = resellerParent
            this.region = region
            this.batchId = batchId
            this.senderAccountTypeId = senderAccountTypeId
            this.receiverAccountTypeId = receiverAccountTypeId
            this.receiverResellerBonus = receiverResellerBonus
            this.receiverResellerCommission = receiverResellerCommission

            // Additional fields

            this.transactionEndDate = transactionEndDate
            this.clientComment = clientComment
            this.originalErsRef = originalErsRef
            this.resultCode = resultCode
            this.resource = resource
            this.transactionProfile = transactionProfile
            this.productName = productName
        }

        Timestamp getTransactionDate() {
            return transactionDate
        }

        void setTransactionDate(Timestamp transactionDate) {
            this.transactionDate = transactionDate
        }

        Integer getTransactionHour() {
            return transactionHour
        }

        void setTransactionHour(Integer transactionHour) {
            this.transactionHour = transactionHour
        }

        String getTransactionReference() {
            return transactionReference
        }

        void setTransactionReference(String transactionReference) {
            this.transactionReference = transactionReference
        }

        String getSenderMSISDN() {
            return senderMSISDN
        }

        void setSenderMSISDN(String senderMSISDN) {
            this.senderMSISDN = senderMSISDN
        }

        String getSenderResellerID() {
            return senderResellerID
        }

        void setSenderResellerID(String senderResellerID) {
            this.senderResellerID = senderResellerID
        }

        String getReceiverMSISDN() {
            return receiverMSISDN
        }

        String getDisplayReceiverMSISDN() {
            return displayReceiverMSISDN
        }

        void setReceiverMSISDN(String receiverMSISDN) {
            this.receiverMSISDN = receiverMSISDN
        }

        void setDisplayReceiverMSISDN(String displayReceiverMSISDN) {
            this.displayReceiverMSISDN = displayReceiverMSISDN
        }

        String getReceiverResellerID() {
            return receiverResellerID
        }

        void setReceiverResellerID(String receiverResellerID) {
            this.receiverResellerID = receiverResellerID
        }

        String getTransactionType() {
            return transactionType
        }

        void setTransactionType(String transactionType) {
            this.transactionType = transactionType
        }

        Double getAmount() {
            return amount
        }

        void setAmount(Double amount) {
            this.amount = amount
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

        String getExternalID() {
            return externalID
        }

        void setExternalID(String externalID) {
            this.externalID = externalID
        }

        Double getSenderBalanceBefore() {
            return senderBalanceBefore
        }

        void setSenderBalanceBefore(Double senderBalanceBefore) {
            this.senderBalanceBefore = senderBalanceBefore
        }

        Double getSenderBalanceAfter() {
            return senderBalanceAfter
        }

        void setSenderBalanceAfter(Double senderBalanceAfter) {
            this.senderBalanceAfter = senderBalanceAfter
        }

        String getCurrency() {
            return currency
        }

        void setCurrency(String currency) {
            this.currency = currency
        }

        String getResultDescription() {
            return resultDescription
        }

        void setResultDescription(String resultDescription) {
            this.resultDescription = resultDescription
        }

        Double getReceiverBalanceBefore() {
            return receiverBalanceBefore
        }

        void setReceiverBalanceBefore(Double receiverBalanceBefore) {
            this.receiverBalanceBefore = receiverBalanceBefore
        }

        Double getReceiverBalanceAfter() {
            return receiverBalanceAfter
        }

        void setReceiverBalanceAfter(Double receiverBalanceAfter) {
            this.receiverBalanceAfter = receiverBalanceAfter
        }

        Double getResellerCommission() {
            return resellerCommission
        }

        void setResellerCommission(Double resellerCommission) {
            this.resellerCommission = resellerCommission
        }

        Double getResellerBonus() {
            return resellerBonus
        }

        void setResellerBonus(Double resellerBonus) {
            this.resellerBonus = resellerBonus
        }

        String getSenderResellerName() {
            return senderResellerName
        }

        void setSenderResellerName(String senderResellerName) {
            this.senderResellerName = senderResellerName
        }

        String getReceiverResellerName() {
            return receiverResellerName
        }

        void setReceiverResellerName(String receiverResellerName) {
            this.receiverResellerName = receiverResellerName
        }

        String getResellerParent() {
            return resellerParent
        }

        void setResellerParent(String resellerParent) {
            this.resellerParent = resellerParent
        }

        String getRegion() {
            return region
        }

        void setRegion(String region) {
            this.region = region
        }

        String getBatchId() {
            return batchId
        }

        void setBatchId(String batchId) {
            this.batchId = batchId
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

        String getCommissionClass() {
            return commissionClass
        }

        String getOriginalErsRef() {
            return originalErsRef
        }

        Timestamp getAccountValidityUntil() {
            return accountValidityUntil
        }

        Timestamp getExpiryBeforeRecharge() {
            return expiryBeforeRecharge
        }

        Timestamp getExpiryAfterRecharge() {
            return expiryAfterRecharge
        }

        Double getOpeningBalance() {
            return openingBalance
        }

        Double getTaxRate() {
            return taxRate
        }

        Double getTax() {
            return tax
        }

        int getResultCode() {
            return resultCode
        }

        String getResource() {
            return resource
        }

        String getTransactionProfile() {
            return transactionProfile
        }

        String getProductName() {
            return productName
        }

        String getClientComment() {
            return clientComment
        }

        Timestamp getTransactionEndDate() {
            return transactionEndDate
        }

        void setReceiverAccountTypeId(String receiverAccountTypeId) {
            this.receiverAccountTypeId = receiverAccountTypeId
        }
        Double getReceiverResellerCommission() {
            return receiverResellerCommission
        }

        void setReceiverResellerCommission(Double receiverResellerCommission) {
            this.receiverResellerCommission = receiverResellerCommission
        }

        Double getReceiverResellerBonus() {
            return receiverResellerBonus
        }

        void setReceiverResellerBonus(Double receiverResellerBonus) {
            this.receiverResellerBonus = receiverResellerBonus
        }
    }
}