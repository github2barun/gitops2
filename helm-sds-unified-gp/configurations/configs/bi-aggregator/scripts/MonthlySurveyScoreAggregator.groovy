package com.seamless.customer.bi.aggregator.aggregate


import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.*
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.metrics.ParsedAvg
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class MonthlySurveyScoreAggregator extends AbstractAggregator {
    static final def TABLE = "survey_pos_monthly_average"
    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${MonthlySurveyScoreAggregator.indexPattern:evaluated_survey_}')
    String indexPattern;

    @Value('${MonthlySurveyScoreAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${MonthlySurveyScoreAggregator.aggregationSize:2000}')
    int aggregationSize;

    @Value('${MonthlySurveyScoreAggregator.bulkInsertionModeFromMonthYear:2020-08}')
    String bulkInsertionModeFromMonthYear;

    @Value('${MonthlySurveyScoreAggregator.bulkInsertionModeToMonthYear:2020-08}')
    String bulkInsertionModeToMonthYear;

    @Transactional
    @Scheduled(cron = '${MonthlySurveyScoreAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info(" ****************** MonthlySurveyScoreAggregator started ******************");

        if (bulkInsertionMode) {
            log.info("Bulk insertion mode: true")
            String bulkFromDateString = bulkInsertionModeFromMonthYear+"-01";
            String bulkToDateString = bulkInsertionModeToMonthYear+ "-01";

            LocalDate bulkFromDate = LocalDate.parse(bulkFromDateString);
            LocalDate convertedToDate = LocalDate.parse(bulkToDateString);
            LocalDate bulkToDate = convertedToDate.with(TemporalAdjusters.lastDayOfMonth());
            log.info("bulkInsertionModeFromDate: " + bulkFromDate.toString());
            log.info("bulkInsertionModeToDate: " + bulkToDate.toString());

            String[] indexList = DateUtil.getSurveyIndices(bulkFromDate,bulkToDate,indexPattern);
            if(indexList.length > 54){
                log.info("index list size too large.. making index as "+indexPattern+"*");
                indexList = [indexPattern+"*"];
            }
            log.info("Index list based on FromDate and ToDate: "+ indexList);
            try {
                aggregateDataES(indexList,bulkFromDate.toString(),bulkToDate.toString())
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (Exception e){
                log.error("Exception occurred! could not aggregate! "+e.getMessage())}

        } else {
            log.info("Bulk insertion mode: false")
            String[] indexList = DateUtil.getSurveyIndices(LocalDate.now().minusWeeks(5),LocalDate.now(),indexPattern);
            log.info("Index list: "+ indexList);
            aggregateDataES(indexList,null,null)
        }

        log.info(" **************** MonthlySurveyScoreAggregator ended ***************");
    }


    private void aggregateDataES(String[] indices, String bulkFromDate, String bulkToDate) {
        SearchRequest searchRequest = new SearchRequest(indices);

        Map<String, Object> afterKey = null;
        SearchSourceBuilder searchSourceBuilder = fetchInput(afterKey, bulkFromDate,bulkToDate );
        searchRequest.source(searchSourceBuilder);
        log.debug("Request: "+searchRequest.toString());
        afterKey= generateResponse(searchRequest);
        //scrolling through all data
        if(afterKey!=null) {
            while(afterKey!=null) {
                searchSourceBuilder = fetchInput(afterKey, bulkFromDate,bulkToDate);
                searchRequest.source(searchSourceBuilder);
                log.debug("Search After Request: "+searchRequest.toString());
                afterKey = generateResponse(searchRequest);
            }
        }
    }

    private SearchSourceBuilder fetchInput(Map<String, Object> afterKey, String bulkFromDate, String bulkToDate) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(false);

        DateHistogramValuesSourceBuilder dateHistoByMonth = new DateHistogramValuesSourceBuilder("submittedOn")
                .field("submittedOn").calendarInterval(DateHistogramInterval.MONTH).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByMonth);
        sources.add(new TermsValuesSourceBuilder("surveyeeId").field("surveyee.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorId").field("surveyor.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("tdrResellerPath").field("surveyorResellerPath.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyeeChannel").field("channel.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorRoute").field("route.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyeeSalesArea").field("area.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyeeRegion").field("region.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorDirector").field("director.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorRHOD").field("hod.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorASMId").field("asm.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyorRSMId").field("rsm.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("surveyeeName").field("name.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("hqId").field("hq.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("branchId").field("branch.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("tdrDesignation").field("tdrDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("asmDesignation").field("asmDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("rsmDesignation").field("rsmDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("hodDesignation").field("hodDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("directorDesignation").field("directorDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("branchDesignation").field("branchDesignation.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("hqDesignation").field("hqDesignation.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("MonthlySurveyScore", sources).size(aggregationSize);

        compositeBuilder.subAggregation(AggregationBuilders.avg("VisibilityAverage").field("categoryPercentage.Visibility").missing(0));
        compositeBuilder.subAggregation(AggregationBuilders.avg("AdvocacyAverage").field("categoryPercentage.Advocacy").missing(0));
        compositeBuilder.subAggregation(AggregationBuilders.avg("AvailabilityAverage").field("categoryPercentage.Availability").missing(0));


        if (bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("status.keyword", "COMPLETED"))
                    .filter(QueryBuilders.termsQuery("surveyType.keyword", "RED"))
                    .filter(QueryBuilders.termsQuery("surveyorType.keyword","tdr"))
                    .filter(QueryBuilders.rangeQuery("submittedOn").gte(bulkFromDate).lte(bulkToDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("status.keyword", "COMPLETED"))
                    .filter(QueryBuilders.termsQuery("surveyType.keyword", "RED"))
                    .filter(QueryBuilders.termsQuery("surveyorType.keyword","tdr"))
                    .filter(QueryBuilders.rangeQuery("submittedOn").gte("now/M").lte("now/M"))
            searchSourceBuilder.query(queryBuilder);
        }
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private  Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<MonthlySurveyScoreModel> monthlySurveyScoreModelList = new ArrayList<>();
        SearchResponse searchResponse = null;

        try {
            log.info("Calling Elasticsearch..");
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Exception occurred while calling Elasticsearch! " + e.getMessage());
        }

        if (searchResponse.status()!=null && searchResponse.status() == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            if(aggregations!=null){
                ParsedComposite parsedComposite = aggregations.asMap().get("MonthlySurveyScore");
                log.info("Buckets size : "+ parsedComposite.getBuckets().size());
                for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                    LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                    Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                    Aggregation availabilityAverage = aggregationMap.get("AvailabilityAverage");
                    ParsedAvg availabilityParsedAvg = (ParsedAvg) availabilityAverage;

                    Aggregation visibilityAverage = aggregationMap.get("VisibilityAverage");
                    ParsedAvg visibilityParsedAvg = (ParsedAvg) visibilityAverage;

                    Aggregation advocacyAverage = aggregationMap.get("AdvocacyAverage");
                    ParsedAvg advocacyParsedAvg = (ParsedAvg) advocacyAverage;

                    Double averageScore = (availabilityParsedAvg.getValue() + visibilityParsedAvg.getValue() + advocacyParsedAvg.getValue())/3;

                    LocalDate localDate = LocalDate.parse(keyValuesMap.get("submittedOn"),java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                    int month=localDate.getMonthValue();
                    int year=localDate.getYear();
                    int quarter=localDate.get(IsoFields.QUARTER_OF_YEAR);

                    String id = GenerateHash.createHashString(
                            keyValuesMap.get("surveyeeId"),
                            keyValuesMap.get("surveyeeName"),
                            keyValuesMap.get("surveyeeChannel"),
                            month.toString(),
                            quarter.toString(),
                            year.toString(),
                            keyValuesMap.get("surveyorRoute"),
                            keyValuesMap.get("surveyeeSalesArea"),
                            keyValuesMap.get("surveyeeRegion"),
                            keyValuesMap.get("surveyorASMId"),
                            keyValuesMap.get("surveyorRSMId"),
                            keyValuesMap.get("surveyorId"),
                            keyValuesMap.get("tdrResellerPath"),
                            keyValuesMap.get("hqId"),
                            keyValuesMap.get("branchId"),
                            keyValuesMap.get("surveyorRHOD"),
                            keyValuesMap.get("surveyorDirector"));


                    MonthlySurveyScoreModel monthlySurveyScoreModel = new MonthlySurveyScoreModel(id,
                            keyValuesMap.get("surveyeeId"),
                            keyValuesMap.get("surveyeeName"),
                            keyValuesMap.get("surveyeeChannel"),
                            month,
                            quarter,
                            year,
                            visibilityParsedAvg.getValue().round(2),
                            availabilityParsedAvg.getValue().round(2),
                            advocacyParsedAvg.getValue().round(2),
                            bucket.getDocCount(),
                            averageScore,
                            keyValuesMap.get("surveyorRoute"),
                            keyValuesMap.get("surveyeeSalesArea"),
                            keyValuesMap.get("surveyeeRegion"),
                            keyValuesMap.get("surveyorASMId"),
                            keyValuesMap.get("surveyorRSMId"),
                            keyValuesMap.get("surveyorId"),
                            keyValuesMap.get("tdrResellerPath"),
                            keyValuesMap.get("hqId"),
                            keyValuesMap.get("branchId"),
                            keyValuesMap.get("surveyorRHOD"),
                            keyValuesMap.get("surveyorDirector"),
                            keyValuesMap.get("branchDesignation"),
                            keyValuesMap.get("hqDesignation"),
                            keyValuesMap.get("tdrDesignation"),
                            keyValuesMap.get("rsmDesignation"),
                            keyValuesMap.get("asmDesignation"),
                            keyValuesMap.get("directorDesignation"),
                            keyValuesMap.get("hodDesignation"));

                    monthlySurveyScoreModelList.add(monthlySurveyScoreModel);
                }
                insertAggregation(monthlySurveyScoreModelList);
                return parsedComposite.afterKey();
            }
            else {
                log.info("Elasticsearch returned 0 aggregations. Required data not present in any of the requested indices")
                return null;
            }

        }
        else {
            log.error("Error occurred. Response from Elasticsearch not 'OK'. Could not aggregate")
            return null;
        }
    }

    private def insertAggregation(List monthlySurveyScoreModelList) {
        if (monthlySurveyScoreModelList.size() != 0) {
            log.info("Inserting ${monthlySurveyScoreModelList.size()} aggregated rows into table "+ TABLE);
            def sql = "INSERT INTO ${TABLE}  (`id`, `posId`,`posName`,`channel`, `month`, `quarter`, `year`, `visibility`, `availability`, `advocacy`,`count`,`averageScore`, `route`, `area`, `region`, `asmId`, `rsmId`, " +
                    "`tdrId`,`tdrResellerPath`, `hq`, `branch`, `hod`, `director`,`branchDesignation`, `hqDesignation`, `tdrDesignation`, " +
                    "`rsmDesignation`, `asmDesignation`, `directorDesignation`, `hodDesignation`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
                    "?,?,?,?,?,?,?,?,?,?) ON" +
                    " DUPLICATE KEY UPDATE visibility = VALUES(visibility), availability = VALUES(availability), " +
                    "advocacy = VALUES(advocacy), count = VALUES(count), averageScore = VALUES(averageScore)";
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = monthlySurveyScoreModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.posId)
                        ps.setString(++index, row.posName)
                        ps.setString(++index, row.channel)
                        ps.setInt(++index, row.month)
                        ps.setInt(++index, row.quarter)
                        ps.setInt(++index, row.year)
                        ps.setDouble(++index, row.visibility)
                        ps.setDouble(++index, row.availability)
                        ps.setDouble(++index, row.advocacy)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index,row.averageScore)
                        ps.setString(++index, row.route)
                        ps.setString(++index, row.salesArea)
                        ps.setString(++index, row.region)
                        ps.setString(++index, row.asmId)
                        ps.setString(++index, row.rsmId)
                        ps.setString(++index, row.tdrId)
                        ps.setString(++index, row.tdrResellerPath)
                        ps.setString(++index, row.hqId)
                        ps.setString(++index, row.branchId)
                        ps.setString(++index, row.hodId)
                        ps.setString(++index, row.directorId)
                        ps.setString(++index, row.branchDesignation)
                        ps.setString(++index, row.hqDesignation)
                        ps.setString(++index, row.tdrDesignation)
                        ps.setString(++index, row.rsmDesignation)
                        ps.setString(++index, row.asmDesignation)
                        ps.setString(++index, row.directorDesignation)
                        ps.setString(++index, row.hodDesignation)
                    },
                    getBatchSize: { monthlySurveyScoreModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
        else {
            log.info("List size 0. Skipping inserting into table")
        }
    }

}
class MonthlySurveyScoreModel {
    private String id;
    private String posId;
    private String posName;
    private String channel;
    private int month;
    private int quarter;
    private int year;
    private double visibility;
    private double availability;
    private double advocacy;
    private long count;
    private double averageScore;
    private String route;
    private String salesArea;
    private String region;
    private String asmId;
    private String rsmId;
    private String tdrId;
    private String tdrResellerPath;
    private String hqId;
    private String branchId;
    private String hodId;
    private String directorId;
    private String branchDesignation;
    private String hqDesignation;
    private String tdrDesignation;
    private String rsmDesignation;
    private String asmDesignation;
    private String directorDesignation;
    private String hodDesignation;

    MonthlySurveyScoreModel(String id, String posId, String posName, String channel, int month, int quarter, int year, double visibility, double availability, double advocacy, long count, double averageScore, String route, String salesArea, String region, String asmId, String rsmId, String tdrId, String tdrResellerPath, String hqId, String branchId, String hodId, String directorId, String branchDesignation, String hqDesignation, String tdrDesignation, String rsmDesignation, String asmDesignation, String directorDesignation, String hodDesignation) {
        this.id = id
        this.posId = posId
        this.posName = posName
        this.channel = channel
        this.month = month
        this.quarter = quarter
        this.year = year
        this.visibility = visibility
        this.availability = availability
        this.advocacy = advocacy
        this.count = count
        this.averageScore = averageScore
        this.route = route
        this.salesArea = salesArea
        this.region = region
        this.asmId = asmId
        this.rsmId = rsmId
        this.tdrId = tdrId
        this.tdrResellerPath = tdrResellerPath
        this.hqId = hqId
        this.branchId = branchId
        this.hodId = hodId
        this.directorId = directorId
        this.branchDesignation = branchDesignation
        this.hqDesignation = hqDesignation
        this.tdrDesignation = tdrDesignation
        this.rsmDesignation = rsmDesignation
        this.asmDesignation = asmDesignation
        this.directorDesignation = directorDesignation
        this.hodDesignation = hodDesignation
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getPosId() {
        return posId
    }

    void setPosId(String posId) {
        this.posId = posId
    }

    String getPosName() {
        return posName
    }

    void setPosName(String posName) {
        this.posName = posName
    }
    int getCount() {
        return count
    }

    void setCount(long count) {
        this.count = count
    }

    double getAverageScore() {
        return averageScore
    }

    void setAverageScore(double averageScore) {
        this.averageScore = averageScore
    }

    String getChannel() {
        return channel
    }

    void setChannel(String channel) {
        this.channel = channel
    }

    int getMonth() {
        return month
    }

    void setMonth(int month) {
        this.month = month
    }

    int getQuarter() {
        return quarter
    }

    void setQuarter(int quarter) {
        this.quarter = quarter
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }

    int getVisibility() {
        return visibility
    }

    void setVisibility(double visibility) {
        this.visibility = visibility
    }

    int getAvailability() {
        return availability
    }

    void setAvailability(double availability) {
        this.availability = availability
    }

    int getAdvocacy() {
        return advocacy
    }

    void setAdvocacy(double advocacy) {
        this.advocacy = advocacy
    }

    String getRoute() {
        return route
    }

    void setRoute(String route) {
        this.route = route
    }

    String getSalesArea() {
        return salesArea
    }

    void setSalesArea(String salesArea) {
        this.salesArea = salesArea
    }

    String getRegion() {
        return region
    }

    void setRegion(String region) {
        this.region = region
    }

    String getAsmId() {
        return asmId
    }

    void setAsmId(String asmId) {
        this.asmId = asmId
    }

    String getRsmId() {
        return rsmId
    }

    void setRsmId(String rsmId) {
        this.rsmId = rsmId
    }

    String getTdrId() {
        return tdrId
    }

    void setTdrId(String tdrId) {
        this.tdrId = tdrId
    }

    String getTdrResellerPath() {
        return tdrResellerPath
    }

    void setTdrResellerPath(String tdrResellerPath) {
        this.tdrResellerPath = tdrResellerPath
    }

    String getHqId() {
        return hqId
    }

    void setHqId(String hqId) {
        this.hqId = hqId
    }

    String getBranchId() {
        return branchId
    }

    void setBranchId(String branchId) {
        this.branchId = branchId
    }

    String getHodId() {
        return hodId
    }

    void setHodId(String hodId) {
        this.hodId = hodId
    }

    String getDirectorId() {
        return directorId
    }

    void setDirectorId(String directorId) {
        this.directorId = directorId
    }

    String getBranchDesignation() {
        return branchDesignation
    }

    void setBranchDesignation(String branchDesignation) {
        this.branchDesignation = branchDesignation
    }

    String getHqDesignation() {
        return hqDesignation
    }

    void setHqDesignation(String hqDesignation) {
        this.hqDesignation = hqDesignation
    }

    String getTdrDesignation() {
        return tdrDesignation
    }

    void setTdrDesignation(String tdrDesignation) {
        this.tdrDesignation = tdrDesignation
    }

    String getRsmDesignation() {
        return rsmDesignation
    }

    void setRsmDesignation(String rsmDesignation) {
        this.rsmDesignation = rsmDesignation
    }

    String getAsmDesignation() {
        return asmDesignation
    }

    void setAsmDesignation(String asmDesignation) {
        this.asmDesignation = asmDesignation
    }

    String getDirectorDesignation() {
        return directorDesignation
    }

    void setDirectorDesignation(String directorDesignation) {
        this.directorDesignation = directorDesignation
    }

    String getHodDesignation() {
        return hodDesignation
    }

    void setHodDesignation(String hodDesignation) {
        this.hodDesignation = hodDesignation
    }
}