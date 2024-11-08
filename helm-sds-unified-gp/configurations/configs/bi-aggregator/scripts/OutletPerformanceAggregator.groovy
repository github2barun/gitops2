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
import java.time.format.DateTimeFormatter

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class OutletPerformanceAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "outlet_performance_report"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${OutletPerformanceAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${OutletPerformanceAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${OutletPerformanceAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${OutletPerformanceAggregator.eventName:OutletPerformanceDataDaily}')
    String eventName

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${OutletPerformanceAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** OutletPerformanceAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<OutletPerformanceModel> OutletPerformanceModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(OutletPerformanceModelList);
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
                List<OutletPerformanceModel> OutletPerformanceModelList = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate());
                insertAggregation(OutletPerformanceModelList);
            }
        }

        log.info("********** OutletPerformance Aggregator ended at " + new Date());
    }

    private List<OutletPerformanceModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<OutletPerformanceModel> outletPerformanceModelList = new ArrayList<>()
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            outletPerformanceModelList = generateResponse(searchResponse);
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    outletPerformanceModelList.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return outletPerformanceModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName.split(",", -1)))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<OutletPerformanceModel> generateResponse(SearchResponse searchResponse) {
        List<OutletPerformanceModel> OutletPerformanceModelList = new ArrayList<>();
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    //String transactionDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd");
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())
                    int month = transactionDate.toLocalDateTime().format(DateTimeFormatter.ofPattern("MM")) as int;
                    int year = transactionDate.toLocalDateTime().format(DateTimeFormatter.ofPattern("YYYY")) as int;

                    String resellerId = searchHitMap.getOrDefault("resellerId", null)
                    Double avgMpesaFloat = searchHitMap.getOrDefault("avgMpesaFloat", 0.0D) as Double
                    Double avgTransactionVolume = searchHitMap.getOrDefault("avgTransactionVolume", 0.0D) as Double
                    Double avgTransactionValue = searchHitMap.getOrDefault("avgTransactionValue", 0.0D) as Double
                    Double mpesaFloatLevel = searchHitMap.getOrDefault("mpesaFloatLevel", 0.0D) as Double
                    Double avgStockVolume = searchHitMap.getOrDefault("avgStockVolume", 0.0D) as Double
                    Double avgStockValue = searchHitMap.getOrDefault("avgStockValue", 0.0D) as Double
                    Long devicesSold = searchHitMap.getOrDefault("devicesSold", 0L) as Long
                    Long devicesAttached = searchHitMap.getOrDefault("devicesAttached", 0L) as Long
                    Long linesOrdered = searchHitMap.getOrDefault("linesOrdered", 0L) as Long
                    Long linesAttached = searchHitMap.getOrDefault("linesAttached", 0L) as Long
                    Long openIssues = searchHitMap.getOrDefault("openIssues", 0L) as Long
                    Long closedIssues = searchHitMap.getOrDefault("closedIssues", 0L) as Long
                    String lastVisitComments = searchHitMap.getOrDefault("lastVisitComments", "N/A")

                    String id = GenerateHash.createHashString(transactionDate as String, resellerId)

                    OutletPerformanceModel OutletPerformanceModel = new OutletPerformanceModel(id, resellerId, month, year, avgMpesaFloat, avgTransactionVolume, avgTransactionValue, mpesaFloatLevel, avgStockVolume, avgStockValue, devicesSold, devicesAttached, linesOrdered, linesAttached, openIssues, closedIssues, Validations.stringValidation(lastVisitComments));

                    OutletPerformanceModelList.add(OutletPerformanceModel)
                }
            }
            return OutletPerformanceModelList;
        }
    }

    private def insertAggregation(List OutletPerformanceModelList) {

        log.info("OutletPerformanceAggregator Aggregated into ${OutletPerformanceModelList.size()} rows.")
        if (OutletPerformanceModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,reseller_id,month,year,avg_mpesa_float,avg_transaction_volume,avg_transaction_value,mpesa_float_level," +
                    "avg_stock_volume,avg_stock_value,devices_sold,devices_attachment,lines_ordered,lines_attached,open_issues,closed_issues,last_visit_comments) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = OutletPerformanceModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.resellerId)
                        ps.setLong(++index, row.month)
                        ps.setLong(++index, row.year)
                        ps.setDouble(++index, row.avgMpesaFloat)
                        ps.setDouble(++index, row.avgTransactionVolume)
                        ps.setDouble(++index, row.avgTransactionValue)
                        ps.setDouble(++index, row.mpesaFloatLevel)
                        ps.setDouble(++index, row.avgStockVolume)
                        ps.setDouble(++index, row.avgStockValue)
                        ps.setLong(++index, row.devicesSold)
                        ps.setLong(++index, row.devicesAttachment)
                        ps.setLong(++index, row.linesOrdered)
                        ps.setLong(++index, row.linesAttached)
                        ps.setLong(++index, row.openIssues)
                        ps.setLong(++index, row.closedIssues)
                        ps.setString(++index, row.lastVisitComments)
                    },
                    getBatchSize: { OutletPerformanceModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class OutletPerformanceModel {
    private String id;
    private String resellerId;
    private int month;
    private int year;
    private Double avgMpesaFloat;
    private Double avgTransactionVolume;
    private Double avgTransactionValue;
    private Double mpesaFloatLevel;
    private Double avgStockVolume;
    private Double avgStockValue;
    private Long devicesSold;
    private Long devicesAttachment;
    private Long linesOrdered;
    private Long linesAttached;
    private Long openIssues;
    private Long closedIssues;
    private String lastVisitComments;

    OutletPerformanceModel(String id, String resellerId, int month, int year, Double avgMpesaFloat, Double avgTransactionVolume, Double avgTransactionValue, Double mpesaFloatLevel, Double avgStockVolume, Double avgStockValue, Long devicesSold, Long devicesAttachment, Long linesOrdered, Long linesAttached, Long openIssues, Long closedIssues, String lastVisitComments) {
        this.id = id
        this.resellerId = resellerId
        this.month = month
        this.year = year
        this.avgMpesaFloat = avgMpesaFloat
        this.avgTransactionVolume = avgTransactionVolume
        this.avgTransactionValue = avgTransactionValue
        this.mpesaFloatLevel = mpesaFloatLevel
        this.avgStockVolume = avgStockVolume
        this.avgStockValue = avgStockValue
        this.devicesSold = devicesSold
        this.devicesAttachment = devicesAttachment
        this.linesOrdered = linesOrdered
        this.linesAttached = linesAttached
        this.openIssues = openIssues
        this.closedIssues = closedIssues
        this.lastVisitComments = lastVisitComments
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    int getMonth() {
        return month
    }

    void setMonth(int month) {
        this.month = month
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }

    Double getAvgMpesaFloat() {
        return avgMpesaFloat
    }

    void setAvgMpesaFloat(Double avgMpesaFloat) {
        this.avgMpesaFloat = avgMpesaFloat
    }

    Double getAvgTransactionVolume() {
        return avgTransactionVolume
    }

    void setAvgTransactionVolume(Double avgTransactionVolume) {
        this.avgTransactionVolume = avgTransactionVolume
    }

    Double getAvgTransactionValue() {
        return avgTransactionValue
    }

    void setAvgTransactionValue(Double avgTransactionValue) {
        this.avgTransactionValue = avgTransactionValue
    }

    Double getMpesaFloatLevel() {
        return mpesaFloatLevel
    }

    void setMpesaFloatLevel(Double mpesaFloatLevel) {
        this.mpesaFloatLevel = mpesaFloatLevel
    }

    Double getAvgStockVolume() {
        return avgStockVolume
    }

    void setAvgStockVolume(Double avgStockVolume) {
        this.avgStockVolume = avgStockVolume
    }

    Double getAvgStockValue() {
        return avgStockValue
    }

    void setAvgStockValue(Double avgStockValue) {
        this.avgStockValue = avgStockValue
    }

    Long getDevicesSold() {
        return devicesSold
    }

    void setDevicesSold(Long devicesSold) {
        this.devicesSold = devicesSold
    }

    Long getDevicesAttachment() {
        return devicesAttachment
    }

    void setDevicesAttachment(Long devicesAttachment) {
        this.devicesAttachment = devicesAttachment
    }

    Long getLinesOrdered() {
        return linesOrdered
    }

    void setLinesOrdered(Long linesOrdered) {
        this.linesOrdered = linesOrdered
    }

    Long getLinesAttached() {
        return linesAttached
    }

    void setLinesAttached(Long linesAttached) {
        this.linesAttached = linesAttached
    }

    Long getOpenIssues() {
        return openIssues
    }

    void setOpenIssues(Long openIssues) {
        this.openIssues = openIssues
    }

    Long getClosedIssues() {
        return closedIssues
    }

    void setClosedIssues(Long closedIssues) {
        this.closedIssues = closedIssues
    }

    String getLastVisitComments() {
        return lastVisitComments
    }

    void setLastVisitComments(String lastVisitComments) {
        this.lastVisitComments = lastVisitComments
    }
}