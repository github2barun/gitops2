package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
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

import java.text.SimpleDateFormat
import java.time.LocalDate

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class FeedbackSurveyAllScores extends AbstractAggregator {
    static final def TABLE = "survey_feedback_all_scores"
    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${FeedbackSurveyAllScores.indexPattern:evaluated_survey_}')
    String indexPattern;

    @Value('${FeedbackSurveyAllScores.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${FeedbackSurveyAllScores.scrollSize:7000}')
    int scrollSize;

    @Value('${FeedbackSurveyAllScores.bulkInsertionModeFromDateString:2020-08}')
    String bulkInsertionModeFromDateString;

    @Value('${FeedbackSurveyAllScores.bulkInsertionModeToDateString:2020-08}')
    String bulkInsertionModeToDateString;

    @Value('${FeedbackSurveyAllScores.purgeOldData:false}')
    boolean purge;

    @Value('${FeedbackSurveyAllScores.purgeBeforeDays:180}')
    String purgeBeforeDays;

    @Transactional
    @Scheduled(cron = '${FeedbackSurveyAllScores.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info(" ****************** FeedbackSurveyScoreModel started ******************");

        if (bulkInsertionMode) {
            log.debug("Bulk insertion mode: true")
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            LocalDate bulkFromDate = LocalDate.parse(bulkInsertionModeFromDateString);
            LocalDate bulkToDate = LocalDate.parse(bulkInsertionModeToDateString);

            String[] indexList = DateUtil.getSurveyIndices(bulkFromDate,bulkToDate,indexPattern);

            log.info("Index list based on FromDate and ToDate: "+ indexList);
            for(int i=0; i< indexList.size(); i++){
                try {
                    aggregateDataES(indexList[i],bulkFromDate.toString(),bulkToDate.toString())
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                } catch (Exception e){
                    log.error("Exception occurred! could not aggregate! "+e.getMessage())}
            }
        } else {
            log.debug("Bulk insertion mode: false")
            String[] indexList = DateUtil.getSurveyIndices(LocalDate.now().minusDays(3),LocalDate.now(),indexPattern);
            log.info("Index list: "+ indexList);
            for(int i=0; i< indexList.size(); i++){
                try {
                    aggregateDataES(indexList[i],null,null)
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                } catch (Exception e){
                    log.error("Exception occurred! could not aggregate! "+e.getMessage())}
            }
        }

        if(purge){
            log.info("Purging old data set to: "+purge);
            purgeData();
        }

        log.info(" **************** FeedbackSurveyScoreModel ended ***************");
    }


    private void aggregateDataES(String index, String bulkFromDate, String bulkToDate) {
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder searchSourceBuilder = fetchInput(bulkFromDate, bulkToDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = null;
        try {
            log.info("Calling Elasticsearch.. index: "+ index);
            log.debug("::::::First Request:::: " + searchRequest.toString());
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Exception occurred while calling Elasticsearch! " + e.getMessage());
        }
        String scrollId= generateResponse(searchResponse);
        log.info("_____hits size outside loop first time____"+searchResponse.getHits().size())

        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueSeconds(30));
            log.debug("::::::Scroll Request:::: " + scrollRequest.toString());
            try {
                searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Exception occurred while calling Elasticsearch! " + e.getMessage());
            }
            log.info("_____hits size inside loop____"+searchResponse.getHits().size())
            scrollId = generateResponse(searchResponse);
        }
        if(scrollId!=null){
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        }
    }

    private SearchSourceBuilder fetchInput(String bulkFromDate, String bulkToDate) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(bulkInsertionMode) {
            QueryBuilder queryBuilder = QueryBuilders.termsQuery("status.keyword", "COMPLETED");
            searchSourceBuilder.size(scrollSize).query(queryBuilder);
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("status.keyword", "COMPLETED"))
                    .filter(QueryBuilders.rangeQuery("submittedOn").gte("now-12h/h").lt("now+1h/h"))
            searchSourceBuilder.size(scrollSize).query(queryBuilder);
        }

        return searchSourceBuilder;
    }

    private  String generateResponse(SearchResponse searchResponse) {
        List<FeedbackSurveyScoreModel> feedbackSurveyScoreModelList = new ArrayList<>();
        SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        if (searchResponse.status() != null && searchResponse.status() == RestStatus.OK) {

            SearchHits searchHits = searchResponse.getHits();

            for (SearchHit searchHit : searchHits.getHits()) {
                Map<String, String> searchHitMap = searchHit.getSourceAsMap();
                String id = searchHit.getId();
                Double score= 0
                Double scorePercent=0
                if(searchHitMap.get("surveyScore")!=null){
                    score = new Double(searchHitMap.get("surveyScore"));
                }
                if(searchHitMap.get("surveyScorePercent")!=null){
                    scorePercent = new Double(searchHitMap.get("surveyScorePercent"));
                }
                String taskType = searchHitMap.get("taskType");
                if(!taskType.equals("generic")){
                    taskType = searchHitMap.get("taskType").substring(9);
                }

                FeedbackSurveyScoreModel feedbackSurveyScoreModel = new FeedbackSurveyScoreModel(
                        id,
                        dayFormatter.parse(searchHitMap.get("submittedOn")),
                        searchHitMap.get("surveyor"),
                        searchHitMap.get("surveyee"),
                        taskType,
                        score,
                        scorePercent,
                        searchHitMap.get("surveyorPath"),
                        searchHitMap.get("surveyeePath"))

                feedbackSurveyScoreModelList.add(feedbackSurveyScoreModel);

            }
            insertAggregation(feedbackSurveyScoreModelList);
            return searchResponse.getScrollId();

        } else {
            log.error("Error occurred. Response from Elasticsearch not 'OK'. Could not aggregate")
            return null;
        }
    }


    private def insertAggregation(List feedbackSurveyScoreModelList) {
        if (feedbackSurveyScoreModelList.size() != 0) {
            log.info("Inserting ${feedbackSurveyScoreModelList.size()} rows into table "+ TABLE);
            def sql = "INSERT INTO ${TABLE}  (`id`, `surveyDate`,`resellerId`,`agentId`, `score`, `scorePercent`, `taskType`,`surveyorResellerPath`,`agentResellerPath`) " +
                    "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id=VALUES(id)";
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = feedbackSurveyScoreModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.surveyDate.getTime()))
                        ps.setString(++index, row.posId)
                        ps.setString(++index, row.agentId)
                        ps.setDouble(++index,row.score)
                        ps.setDouble(++index,row.scorePercent)
                        ps.setString(++index, row.taskType)
                        ps.setString(++index, row.posResellerPath)
                        ps.setString(++index, row.agentResellerPath)

                    },
                    getBatchSize: { feedbackSurveyScoreModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
        else {
            log.info("List size 0. Skipping inserting into table")
        }
    }


    private def purgeData() {
        String sql  = "DELETE FROM "+ TABLE +" WHERE surveyDate < DATE_SUB(CURRENT_DATE(), INTERVAL " + purgeBeforeDays+ " DAY)";
        log.info("Delete query: "+ sql);
        int rowsDeleted = jdbcTemplate.update(sql);
        log.info("Deleted "+rowsDeleted+" number of rows!");

    }
}
class FeedbackSurveyScoreModel {
    private String id;
    private Date surveyDate;
    private String posId;
    private String agentId;
    private String taskType;
    private double score;
    private double scorePercent;
    private String posResellerPath;
    private String agentResellerPath;

    public FeedbackSurveyScoreModel(String id, Date surveyDate, String posId, String agentId, String taskType, double score, double scorePercent,
                                    String posResellerPath, String agentResellerPath) {
        this.id = id;
        this.surveyDate = surveyDate;
        this.posId = posId;
        this.agentId = agentId;
        this.taskType = taskType;
        this.score = score;
        this.scorePercent = scorePercent;
        this.agentResellerPath = agentResellerPath;
        this.posResellerPath = posResellerPath;

    }


    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getSurveyDate() {
        return surveyDate
    }

    void setSurveyDate(Date surveyDate) {
        this.surveyDate = surveyDate
    }

    String getPosId() {
        return posId
    }

    void setPosId(String posId) {
        this.posId = posId
    }

    String getAgentId() {
        return agentId
    }

    void setAgentId(String agentId) {
        this.agentId = agentId
    }

    String getTaskType() {
        return taskType
    }

    void setTaskType(String taskType) {
        this.taskType = taskType
    }

    double getScore() {
        return score
    }

    void setScore(double score) {
        this.score = score
    }

    double getScorePercent() {
        return scorePercent
    }

    void setScorePercent(double scorePercent) {
        this.scorePercent = scorePercent
    }

    String getPosResellerPath() {
        return posResellerPath
    }

    void setPosResellerPath(String posResellerPath) {
        this.posResellerPath = posResellerPath
    }

    String getAgentResellerPath() {
        return agentResellerPath
    }

    void setAgentResellerPath(String agentResellerPath) {
        this.agentResellerPath = agentResellerPath
    }
}