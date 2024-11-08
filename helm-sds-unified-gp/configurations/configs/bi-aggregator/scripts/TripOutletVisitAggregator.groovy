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

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
class TripOutletVisitAggregator extends ScrollableAbstractAggregator {
    static final def TABLE = "trip_outlet_visit"

    @Autowired
    RestHighLevelClient client

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${TripOutletVisitAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode

    @Value('${TripOutletVisitAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString

    @Value('${TripOutletVisitAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString

    @Value('${TripOutletVisitAggregator.eventName:END_TRIP}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset

    @Value('${AllOrdersAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TripOutletVisitAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("********** TripOutletVisitAggregator Aggregator started at " + new Date())
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString)
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString)

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TripOutletVisitModel> tripOutletVisitModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(tripOutletVisitModelList)
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
                List<TripOutletVisitModel> tripOutletVisitModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate())
                insertAggregation(tripOutletVisitModelList)
            }
        }

        log.info("********** TripOutletVisitAggregator Aggregator ended at " + new Date())
    }


    private List<TripOutletVisitModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<TripOutletVisitModel> tripOutletVisitModel = new ArrayList<>()

        SearchRequest searchRequest = new SearchRequest(index)
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate)
        searchRequest.source(searchSourceBuilder)
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            tripOutletVisitModel = generateResponse(searchResponse)
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    tripOutletVisitModel.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return tripOutletVisitModel
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("tms.eventName.keyword", eventName.split(",", -1)))
                .filter(QueryBuilders.termsQuery("resultCode", 0))

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize)
        return searchSourceBuilder
    }

    private List<TripOutletVisitModel> generateResponse(SearchResponse searchResponse) {
        List<TripOutletVisitModel> tripOutletVisitModelList = new ArrayList<>()
        if (searchResponse != null) {
            RestStatus status = searchResponse.status()
            log.debug("response status -------------" + status)

            HashMap<String, TripOutletVisitModel> tripOutletVisitModelMap = new HashMap<>()
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits()
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap()

                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"))
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())

                    DateFormat df = new SimpleDateFormat("ddMMyyyy");
                    String dateStr = df.format(dateTimeDay)

                    HashMap<String, String> agentDetails = searchHitMap.getOrDefault("tms.agentDetail", null)
                    String resellerId = agentDetails != null ? agentDetails.getOrDefault("tms.agentId", "N/A") : "N/A"

                    List<HashMap<String, String>> posList = searchHitMap.getOrDefault("tms.posList", null)
                    if (posList != null && !posList.isEmpty()) {
                        for (HashMap<String, String> pos : posList) {
                            Long completedVisit = 0L
                            Long pendingVisit = 0L
                            Double dsaStrikeRate; //Logic: ('Completed Visit'/'Total Outlets Visit') * 100
                            String posStatus = pos.getOrDefault("tms.status", "N/A")
                            if (posStatus.equalsIgnoreCase("COMPLETED")) completedVisit = 1L
                            else if (posStatus.equalsIgnoreCase("SKIPPED")) pendingVisit = 1L

                            String id = GenerateHash.createHashString(dateStr, resellerId)

                            if (tripOutletVisitModelMap.containsKey(id)) {
                                TripOutletVisitModel tripOutletVisitModel = tripOutletVisitModelMap.get(id)
                                tripOutletVisitModel.setCompletedVisit(tripOutletVisitModel.getCompletedVisit() + completedVisit)
                                tripOutletVisitModel.setPendingVisit(tripOutletVisitModel.getPendingVisit() + pendingVisit)
                                tripOutletVisitModel.setTotalOutlets(tripOutletVisitModel.getTotalOutlets() + completedVisit + pendingVisit)
                                dsaStrikeRate = (Double) ((tripOutletVisitModel.getCompletedVisit() / tripOutletVisitModel.getTotalOutlets()) * 100)
                                tripOutletVisitModel.setDsaStrikeRate(dsaStrikeRate)
                                tripOutletVisitModelMap.put(id, tripOutletVisitModel)
                            } else {
                                dsaStrikeRate = (Double) ((completedVisit / 1) * 100)
                                TripOutletVisitModel tripOutletVisitModel = new TripOutletVisitModel(id, transactionDate, resellerId, 1, completedVisit, pendingVisit, dsaStrikeRate)
                                tripOutletVisitModelMap.put(id, tripOutletVisitModel)
                            }
                        }
                    }
                }

                tripOutletVisitModelMap.each {
                    entry -> tripOutletVisitModelList.add(entry.value)
                }
                return tripOutletVisitModelList
            }
        }
    }

    private def insertAggregation(List tripOutletVisitModelList) {

        log.info("TripOutletVisitAggregator Aggregated into ${tripOutletVisitModelList.size()} rows.")
        if (tripOutletVisitModelList.size() != 0) {
            def sql = """INSERT INTO ${TABLE} (id,trip_date,reseller_id,total_outlets,completed_visit,pending_visit,dsa_strike_rate)
                    VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE total_outlets = VALUES(total_outlets), completed_visit = VALUES(completed_visit),
                    pending_visit = VALUES(pending_visit), dsa_strike_rate = VALUES(dsa_strike_rate)"""
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = tripOutletVisitModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.transactionDate)
                        ps.setString(++index, row.resellerId)
                        ps.setLong(++index, row.totalOutlets)
                        ps.setLong(++index, row.completedVisit)
                        ps.setLong(++index, row.pendingVisit)
                        ps.setDouble(++index, row.dsaStrikeRate)
                    },
                    getBatchSize: { tripOutletVisitModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class TripOutletVisitModel {
    private String id
    private Timestamp transactionDate
    private String resellerId
    private Long totalOutlets
    private Long completedVisit
    private Long pendingVisit
    private Double dsaStrikeRate

    TripOutletVisitModel(String id, Timestamp transactionDate, String resellerId, Long totalOutlets, Long completedVisit, Long pendingVisit, Double dsaStrikeRate) {
        this.id = id
        this.transactionDate = transactionDate
        this.resellerId = resellerId
        this.totalOutlets = totalOutlets
        this.completedVisit = completedVisit
        this.pendingVisit = pendingVisit
        this.dsaStrikeRate = dsaStrikeRate
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

    Long getTotalOutlets() {
        return totalOutlets
    }

    void setTotalOutlets(Long totalOutlets) {
        this.totalOutlets = totalOutlets
    }

    Long getCompletedVisit() {
        return completedVisit
    }

    void setCompletedVisit(Long completedVisit) {
        this.completedVisit = completedVisit
    }

    Long getPendingVisit() {
        return pendingVisit
    }

    void setPendingVisit(Long pendingVisit) {
        this.pendingVisit = pendingVisit
    }

    Double getDsaStrikeRate() {
        return dsaStrikeRate
    }

    void setDsaStrikeRate(Double dsaStrikeRate) {
        this.dsaStrikeRate = dsaStrikeRate
    }
}