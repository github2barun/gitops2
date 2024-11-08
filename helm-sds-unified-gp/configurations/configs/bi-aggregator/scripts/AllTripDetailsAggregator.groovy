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
public class AllTripDetailsAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "all_trips_detail"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${AllTripDetailsAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${AllTripDetailsAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${AllTripDetailsAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${AllTripDetailsAggregator.eventName:END_TRIP}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${AllTripDetailsAggregator.taskList:PO,SO,RO,PISO,SURVEY,COLLECTION}')
    String taskTypeList;

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${AllTripDetailsAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** AllTripDetailsAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<AllTripDetailsModel> allTripDetailsModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(allTripDetailsModelList);
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
                List<AllTripDetailsModel> allTripDetailsModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(allTripDetailsModelList);
            }
        }

        log.info("********** AllTripDetailsAggregator Aggregator ended at " + new Date());
    }

    private List<AllTripDetailsModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<AllTripDetailsModel> allTripDetailsModel = new ArrayList<>()
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate)
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            allTripDetailsModel = generateResponse(searchResponse)
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    allTripDetailsModel.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return allTripDetailsModel
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("tms.eventName.keyword", eventName.split(",", -1)))
                .filter(QueryBuilders.termsQuery("resultCode", 0))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<AllTripDetailsModel> generateResponse(SearchResponse searchResponse) {
        List<AllTripDetailsModel> allTripDetailsModelList = new ArrayList<>();
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, AllTripDetailsModel> allTripDetailsModelMap = new HashMap<>();
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    String transactionNumber = searchHitMap.getOrDefault("transactionNumber", "N/A");
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                    HashMap<String, String> agentDetails = searchHitMap.getOrDefault("tms.agentDetail", null)
                    String resellerId = agentDetails != null ? agentDetails.getOrDefault("tms.agentId", "N/A") : "N/A"

                    List<HashMap<String, String>> posList = searchHitMap.getOrDefault("tms.posList", null)

                    if (posList != null && !posList.isEmpty()) {
                        int posCount = 1
                        for (HashMap<String, String> pos : posList) {
                            String posId = pos.getOrDefault("tms.posId", "N/A")
                            String posStatus = pos.getOrDefault("tms.status", "N/A")
                            List<HashMap<String, String>> taskList = pos.getOrDefault("tms.taskList", null)
                            if (taskList != null && !taskList.isEmpty()) {
                                int taskCount = 1
                                for (HashMap<String, String> task : taskList) {
                                    String taskType = task.getOrDefault("tms.taskType", "N/A")
                                    if (taskTypeList.contains(taskType)) {
                                        String id = GenerateHash.createHashString(transactionNumber, posCount as String, taskCount as String)
                                        AllTripDetailsModel allTripDetailsModel = new AllTripDetailsModel(id, transactionDate, Validations.stringValidation(resellerId),
                                                Validations.stringValidation(posId), Validations.stringValidation(taskType),
                                                Validations.stringValidation(posStatus), transactionNumber)
                                        allTripDetailsModelList.add(allTripDetailsModel);
                                    } else {
                                        log.error("Task " + taskType + " is not in the allowed task list");
                                    }
                                    taskCount++
                                }
                            }
                            posCount++
                        }
                    }
                }
                return allTripDetailsModelList;
            }
        }
    }

    private def insertAggregation(List allTripDetailsModelList) {

        log.info("AllTripDetailsAggregator Aggregated into ${allTripDetailsModelList.size()} rows.")
        if (allTripDetailsModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,trip_date,reseller_id,pos_id,transaction_number,task_type,pos_status) " +
                    "VALUES (?,?,?,?,?,?,?)ON DUPLICATE KEY UPDATE id = VALUES(id)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = allTripDetailsModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.posId)
                        ps.setString(++index, row.transactionNumber)
                        ps.setString(++index, row.taskType)
                        ps.setString(++index, row.posStatus)
                    },
                    getBatchSize: { allTripDetailsModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class AllTripDetailsModel {
    private String id;
    private Timestamp transactionDate;
    private String resellerId;
    private String posId;
    private String taskType;
    private String posStatus;
    private String transactionNumber;

    AllTripDetailsModel(String id, Timestamp transactionDate, String resellerId, String posId, String taskType, String posStatus, String transactionNumber) {
        this.id = id
        this.transactionDate = transactionDate
        this.resellerId = resellerId
        this.posId = posId
        this.taskType = taskType
        this.posStatus = posStatus
        this.transactionNumber = transactionNumber
    }

    String getTransactionNumber() {
        return transactionNumber
    }

    void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber
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

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getPosId() {
        return posId
    }

    void setPosId(String posId) {
        this.posId = posId
    }

    String getTaskType() {
        return taskType
    }

    void setTaskType(String taskType) {
        this.taskType = taskType
    }

    String getPosStatus() {
        return posStatus
    }

    void setPosStatus(String posStatus) {
        this.posStatus = posStatus
    }
}