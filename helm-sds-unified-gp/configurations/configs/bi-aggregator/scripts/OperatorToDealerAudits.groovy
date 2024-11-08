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

import java.sql.Timestamp

@Slf4j
public class OperatorToDealerAudits extends AbstractAggregator {
    static final def TABLE = "bi.operator_dealer_audits_logs";

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${OperatorToDealerAudits.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${OperatorToDealerAudits.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${OperatorToDealerAudits.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${OperatorToDealerAudits.allowedEvent:addReseller,updateReseller,resellerChangeState}')
    String allowedEvents

    @Value('${OperatorToDealerAudits.changeState:resellerChangeState}')
    String changeState

    @Value('${OperatorToDealerAudits.deleteDataAfterDays:60}')
    Integer deleteDataAfterDays;

    @Value('${OperatorToDealerAudits.indexPrefix:audit_}')
    String indexPrefix

    @Transactional
    @Scheduled(cron = '${OperatorToDealerAudits.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("*************************************************** OperatorToDealerAudits Aggregator started ********************************************" + new Date());
        def eventsList = allowedEvents.split(",")

        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList( bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                index = indexPrefix + index;
                log.info(index.toString() + "for bulk insertion")
                //fetch data from ES
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventsList);

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
                index.setIndexName(indexPrefix+index.getIndexName())
                log.info(index.toString())
                //fetch data from ES
                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), eventsList);
            }
        }

        log.info("*************************************** OperatorToDealerAudits Aggregator ended ******************************************");
    }

    private void aggregateDataES(String index, String fromDate, String toDate, String[] eventsList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventsList);
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


    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] eventsList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventsList))
            searchSourceBuilder.size(1000).sort("timestamp", SortOrder.ASC).query(queryBuilder);
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventsList))
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
        log.info("..... preparing to insert Operator Dealer Audits ......")
        List<AuditsModel> auditModelArrayList = new ArrayList<>();
        AuditsModel auditModel;
        for (SearchHit searchHit : searchHits.getHits())
        {
            Map<String, String> searchHitMap = searchHit.getSourceAsMap();

            try{
                List dealerInfo = searchHitMap.get("DMS") as List

                for(index in 0..< dealerInfo.size()){

                    String resultCode = searchHitMap.get("resultCode")
                    log.info("resultCode = " + resultCode)

                    String receiverResellerID = "";
                    String status = "";
                    if ("0".equals(resultCode))
                    {
                        receiverResellerID = dealerInfo.get(index).get("resellerInfo").get("reseller").get("resellerId")
                        log.debug("receiverResellerID = " + receiverResellerID)

                        status = dealerInfo.get(index).get("resellerInfo").get("reseller").get("status")
                        log.debug("ResellerStatus = " + status)
                    }

                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    log.debug("transactionDate = " + dateTimeDay)

                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime());
                    String transactionReference = searchHitMap.get("transactionNumber")
                    log.info("transactionReference = " + transactionReference)

                    String senderResellerID = searchHitMap.get("user.userId")
                    log.debug("senderResellerID = " + senderResellerID)

                    String transactionType = searchHitMap.get("eventName")
                    log.debug("EventName = " + transactionType)

                    String resultDescription = (searchHitMap.get("resultMessage") == null || "null".equalsIgnoreCase(searchHitMap.get("resultMessage"))) ? "" : searchHitMap.get("resultMessage")
                    log.debug("resultDescription = " + resultDescription)

                    String channel = searchHitMap.get("channel")
                    log.debug("channel = " + channel)

                    Timestamp transactionEndDate = transactionDate

                    String clientComment = searchHitMap.get("clientComments")
                    log.debug("clientComment = " + clientComment)

                    auditModel = new AuditsModel(transactionDate, transactionReference, senderResellerID, receiverResellerID
                            , transactionType, channel, transactionEndDate, clientComment
                            , resultCode, status, resultDescription);
                    auditModelArrayList.add(auditModel);
                }
            }
            catch (Exception e) {
                log.error("Skipped record with transaction number:  " + searchHitMap.get("transactionNumber") + " due to " +
                        "error: " + e.getMessage());
            }

        }
        log.info("auditModelArrayList size = " + auditModelArrayList.size())
        insertAggregation(auditModelArrayList);
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

    private def insertAggregation(List auditModelArrayList) {
        if (auditModelArrayList.size() != 0)
        {
            log.info("deleting data for " + deleteDataAfterDays + " days old.")

            def date = new Date()
            use(TimeCategory)
                    {
                        date = toSqlTimestamp(date - deleteDataAfterDays.days)
                    }

            // first remove old data
            def delete = "delete from ${TABLE} where transactionStartDate < ?"
            jdbcTemplate.batchUpdate(delete, [
                    setValues   : { ps, i ->
                        ps.setTimestamp(1, toSqlTimestamp(date))
                    },
                    getBatchSize: { auditModelArrayList.size() }
            ] as BatchPreparedStatementSetter)

            // then insert new data
            def sql = "REPLACE INTO ${TABLE} (transactionReference,transactionStartDate,transactionEndDate" +
                    ",senderResellerID,receiverResellerID,resellerStatus,comments,transactionType" +
                    ",channel,resultStatus,resultDescription) " +
                    "VALUES (?,?,?,?,?, ?,?,?,?,?, ?)"

            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = auditModelArrayList[i]
                        def index = 0
                        ps.setString(++index, row.transactionReference)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setTimestamp(++index, row.transactionEndDate)
                        ps.setString(++index, row.senderResellerID)
                        ps.setString(++index, row.receiverResellerID)
                        ps.setString(++index, row.status)
                        ps.setString(++index, row.clientComment)
                        ps.setString(++index, row.transactionType)
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.resultCode)
                        ps.setString(++index, row.resultDescription)
                    },
                    getBatchSize:
                            { auditModelArrayList.size() }
            ] as BatchPreparedStatementSetter)
        }
        log.info("Audits Aggregated into ${auditModelArrayList.size()} rows.")

    }

    class AuditsModel
    {
        private Timestamp transactionDate
        private String transactionReference
        private String senderResellerID
        private String receiverResellerID
        private String transactionType
        private String channel
        private Timestamp transactionEndDate
        private String clientComment
        private String resultCode
        private String resultDescription
        private String status

        AuditsModel(Timestamp transactionDate, String transactionReference, String senderResellerID, String receiverResellerID
                    , String transactionType, String channel, Timestamp transactionEndDate, String clientComment
                    , String resultCode, String status, String resultDescription) {
            this.transactionDate = transactionDate
            this.transactionReference = transactionReference
            this.senderResellerID = senderResellerID
            this.receiverResellerID = receiverResellerID
            this.transactionType = transactionType
            this.channel = channel
            this.transactionEndDate = transactionEndDate
            this.clientComment = clientComment
            this.resultCode = resultCode
            this.status = status
            this.resultDescription = resultDescription
        }

        Timestamp getTransactionDate() {
            return transactionDate
        }

        void setTransactionDate(Timestamp transactionDate) {
            this.transactionDate = transactionDate
        }

        String getStatus() {
            return this.status
        }

        void setStatus(String status) {
            this.status = status
        }

        String getTransactionReference() {
            return transactionReference
        }

        void setTransactionReference(String transactionReference) {
            this.transactionReference = transactionReference
        }

        String getSenderResellerID() {
            return senderResellerID
        }

        void setSenderResellerID(String senderResellerID) {
            this.senderResellerID = senderResellerID
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

        String getChannel() {
            return channel
        }

        void setChannel(String channel) {
            this.channel = channel
        }

        String getResultCode() {
            return resultCode
        }

        void setReselCode(String resultCode){
            this.reselCode = resultCode
        }

        String getClientComment() {
            return clientComment
        }

        void setClientComment(String clientComment) {
            this.clientComment = clientComment;
        }

        Timestamp getTransactionEndDate() {
            return transactionEndDate
        }

        void setTransactionEndDate(Timestamp transactionEndDate) {
            this.transactionEndDate = transactionEndDate
        }

    }
}