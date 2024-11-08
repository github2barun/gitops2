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
public class HourlyUsageStatisticsAggregator extends AbstractAggregator {
    static final def TABLE = "hourly_usage_statistics";
    @Autowired
    RestHighLevelClient client;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Value('${HourlyUsageStatisticsAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;
    @Value('${HourlyUsageStatisticsAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;
    @Value('${HourlyUsageStatisticsAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;
    @Value('${HourlyUsageStatisticsAggregator.eventName:RAISE_ORDER}')
    String eventName
    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;
    @Value('${HourlyUsageStatisticsAggregator.operationType:GER_PAYMENT,PPS_RECHARGE}')
    String operationType;
    @Value('${HourlyUsageStatisticsAggregator.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${HourlyUsageStatisticsAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** HourlyUsageStatisticsAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<HourlyUsageStatisticsAggregatorModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
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
                List<HourlyUsageStatisticsAggregatorModel> transactionFailureModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionFailureModels);
            }
        }
        log.info("********** HourlyUsageStatisticsAggregator Aggregator ended at " + new Date());
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.termsQuery("eventName.keyword", eventName));
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<HourlyUsageStatisticsAggregatorModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<HourlyUsageStatisticsAggregatorModel> transactionSummaryModels = generateResponse(searchResponse);
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
    private List<HourlyUsageStatisticsAggregatorModel> generateResponse(SearchResponse searchResponse) {
        List<HourlyUsageStatisticsAggregatorModel> hourlyUsageStatisticsAggregatorModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);
            HashMap<String, HourlyUsageStatisticsAggregatorModel> hourlyUsageStatisticsAggregatorModelMap = new HashMap<>();

            if (status == RestStatus.OK) {
                String[] operationTypes = operationType.split(",");
                SearchHits searchHits = searchResponse.getHits();

                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
                    String channel = searchHitMap.get("channel") as String;
                    String dateOnly = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd") as String;
                    String hourOnly = (DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("HH") as String) + ":00:00";

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");

                        if (!omsItems.isEmpty()) {
                            for (int i = 0; i < omsItems.size(); i++) {
                                HashMap<String, String> omsItem = omsItems.get(i);

                                if (omsItem.getOrDefault("data", null) != null) {
                                    HashMap<String, String> data = omsItem.get("data");

                                    String dataOperationType = data.getOrDefault("operationType", "N/A");
                                    if (operationTypes.contains(dataOperationType) && data.getOrDefault("resultCode", null) != null) {
                                        long successCount = 0, failureCount = 0;
                                        long resultCode = data.get("resultCode") as long;
                                        if (resultCode == 0) {
                                            successCount++;
                                        } else {
                                            failureCount++;
                                        }

                                        String id = GenerateHash.createHashString(dateOnly, hourOnly, channel);

                                        if (hourlyUsageStatisticsAggregatorModelMap.containsKey(id)) {
                                            HourlyUsageStatisticsAggregatorModel hourlyUsageStatisticsAggregatorModel = hourlyUsageStatisticsAggregatorModelMap.get(id);
                                            hourlyUsageStatisticsAggregatorModel.setSuccessCount(hourlyUsageStatisticsAggregatorModel.getSuccessCount() + successCount);
                                            hourlyUsageStatisticsAggregatorModel.setFailureCount(hourlyUsageStatisticsAggregatorModel.getFailureCount() + failureCount);
                                            hourlyUsageStatisticsAggregatorModel.setTotalCount(hourlyUsageStatisticsAggregatorModel.getTotalCount() + successCount + failureCount);
                                            hourlyUsageStatisticsAggregatorModelMap.put(id, hourlyUsageStatisticsAggregatorModel);
                                        } else {
                                            def dateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateOnly);
                                            def hour = new java.text.SimpleDateFormat("HH:mm:ss").parse(hourOnly);
                                            HourlyUsageStatisticsAggregatorModel hourlyUsageStatisticsAggregatorModel =
                                                    new HourlyUsageStatisticsAggregatorModel(id, dateObj, hour, channel, successCount, failureCount, successCount + failureCount);
                                            hourlyUsageStatisticsAggregatorModelMap.put(id, hourlyUsageStatisticsAggregatorModel);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            hourlyUsageStatisticsAggregatorModelMap.each {
                entry -> hourlyUsageStatisticsAggregatorModelList.add(entry.value)
            }
        }
        return hourlyUsageStatisticsAggregatorModelList;
    }

    private def insertAggregation(List<HourlyUsageStatisticsAggregatorModel> hourlyUsageStatisticsAggregatorModelList) {
        log.info("HourlyUsageStatisticsAggregator Aggregated into ${hourlyUsageStatisticsAggregatorModelList.size()} rows.")
        if (hourlyUsageStatisticsAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,date,hour,channel,successful_txn_count,failed_txn_count,ttl_txn_count) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE successful_txn_count = VALUES(successful_txn_count), failed_txn_count = VALUES(failed_txn_count), ttl_txn_count = VALUES(ttl_txn_count)";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = hourlyUsageStatisticsAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.date.getTime()))
                        ps.setTimestamp(++index, new java.sql.Timestamp(row.dateHour.getTime()))
                        ps.setString(++index, row.channel)
                        ps.setLong(++index, row.successCount)
                        ps.setLong(++index, row.failureCount)
                        ps.setLong(++index, row.totalCount)
                    },
                    getBatchSize: { hourlyUsageStatisticsAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class HourlyUsageStatisticsAggregatorModel {
    private String id;
    private Date date;
    private Date dateHour;
    private String channel;
    private long successCount;
    private long failureCount;
    private long totalCount;

    HourlyUsageStatisticsAggregatorModel(String id, Date date, Date dateHour, String channel, long successCount, long failureCount, long totalCount) {
        this.id = id
        this.date = date
        this.dateHour = dateHour
        this.channel = channel
        this.successCount = successCount
        this.failureCount = failureCount
        this.totalCount = totalCount
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getDate() {
        return date
    }

    void setDate(Date date) {
        this.date = date
    }

    Date getDateHour() {
        return dateHour
    }

    void setDateHour(Date dateHour) {
        this.dateHour = dateHour
    }

    String getChannel() {
        return channel
    }

    void setChannel(String channel) {
        this.channel = channel
    }

    long getSuccessCount() {
        return successCount
    }

    void setSuccessCount(long successCount) {
        this.successCount = successCount
    }

    long getFailureCount() {
        return failureCount
    }

    void setFailureCount(long failureCount) {
        this.failureCount = failureCount
    }

    long getTotalCount() {
        return totalCount
    }

    void setTotalCount(long totalCount) {
        this.totalCount = totalCount
    }
}
