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
//@DynamicMixin
public class TransactionFailureAggregator extends AbstractAggregator {
    static final def TABLE = "transaction_failure_aggregator"
    @Autowired
    RestHighLevelClient client;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Value('${TransactionFailureAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;
    @Value('${TransactionFailureAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;
    @Value('${TransactionFailureAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;
    @Value('${TransactionFailureAggregator.eventName:RAISE_ORDER}')
    String eventName
    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${TransactionFailureAggregator.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TransactionFailureAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** TransactionFailureAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change
            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TransactionFailureAggregatorModel> transactionSummaryModels = aggregateDataES(index,
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
                List<TransactionFailureAggregatorModel> transactionFailureModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionFailureModels);
            }
        }
        log.info("********** TransactionFailureAggregator Aggregator ended at " + new Date());
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName));
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<TransactionFailureAggregatorModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<TransactionFailureAggregatorModel> transactionSummaryModels = generateResponse(searchResponse);
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
    private List<TransactionFailureAggregatorModel> generateResponse(SearchResponse searchResponse){
        List<TransactionFailureAggregatorModel> transactionFailureAggregatorModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, TransactionFailureAggregatorModel> transactionFailureAggregatorModelMap = new HashMap<>();
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp") as String);

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        int parentResultCode = searchHitMap.get("resultCode") as int;
                        String parentFailureMsg = searchHitMap.get("resultMessage").toString();

                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        for (int i = 0; i < omsItems.size(); i++) {
                            String channel = searchHitMap.get("channel").toString();

                            HashMap<String, String> omsItem = omsItems.get(i);
                            if (omsItem.getOrDefault("data", null) != null) {
                                HashMap<String, String> data = omsItem.get("data");
                                String failureCause = "N/A";
                                String id = "N/A";
                                String transactionProfile = data.getOrDefault("operationType", "N/A");

                                if (data.getOrDefault("resultCode", null) != null) {
                                    int internalResultCode = data.get("resultCode") as int;
                                    if (internalResultCode != 0) {
                                        failureCause = data.getOrDefault("internalResultCodeName", "N/A")
                                        id = GenerateHash.createHashString(
                                                dateTimeDay.toString(),
                                                channel,
                                                transactionProfile,
                                                failureCause
                                        )
                                    }

                                } else {
                                    if (parentResultCode != 0) {
                                        failureCause = parentFailureMsg
                                        id = GenerateHash.createHashString(
                                                dateTimeDay.toString(),
                                                channel,
                                                transactionProfile,
                                                failureCause
                                        )
                                    }
                                }

                                if (id != "N/A") {
                                    if (transactionFailureAggregatorModelMap.containsKey(id)) {
                                        TransactionFailureAggregatorModel transactionFailureAggregatorModel = transactionFailureAggregatorModelMap.get(id);
                                        transactionFailureAggregatorModel.setTransactionCount(transactionFailureAggregatorModel.getTransactionCount() + 1);
                                        transactionFailureAggregatorModelMap.put(id, transactionFailureAggregatorModel);
                                    } else {
                                        TransactionFailureAggregatorModel transactionFailureAggregatorModel = new TransactionFailureAggregatorModel(id, dateTimeDay, channel,
                                                transactionProfile, failureCause, 1);
                                        transactionFailureAggregatorModelMap.put(id, transactionFailureAggregatorModel);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            transactionFailureAggregatorModelMap.each {
                entry -> transactionFailureAggregatorModelList.add(entry.value)
            }
        }
        return transactionFailureAggregatorModelList;
    }

    private def insertAggregation(List<TransactionFailureAggregatorModel> transactionFailureAggregatorModelList) {
        log.info("TransactionFailureAggregator Aggregated into ${transactionFailureAggregatorModelList.size()} rows.")
        if (transactionFailureAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,channel,transaction_profile,failure_cause,transaction_count" +
                    ") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE transaction_count = VALUES(transaction_count)";

            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = transactionFailureAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.transactionProfile)
                        ps.setString(++index, row.failureCause)
                        ps.setLong(++index, row.transactionCount)
                    },
                    getBatchSize: { transactionFailureAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class TransactionFailureAggregatorModel {
    private String id;
    private Date transactionDate;
    private String channel;
    private String transactionProfile;
    private String failureCause;
    private long transactionCount;

    TransactionFailureAggregatorModel(String id, Date transactionDate, String channel, String transactionProfile, String failureCause, long transactionCount) {
        this.id = id
        this.transactionDate = transactionDate
        this.channel = channel
        this.transactionProfile = transactionProfile
        this.failureCause = failureCause
        this.transactionCount = transactionCount
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getTransactionDate() {
        return transactionDate
    }

    void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
    }

    String getChannel() {
        return channel
    }

    void setChannel(String channel) {
        this.channel = channel
    }

    String getTransactionProfile() {
        return transactionProfile
    }

    void setTransactionProfile(String transactionProfile) {
        this.transactionProfile = transactionProfile
    }

    String getFailureCause() {
        return failureCause
    }

    void setFailureCause(String failureCause) {
        this.failureCause = failureCause
    }

    long getTransactionCount() {
        return transactionCount
    }

    void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount
    }
}

