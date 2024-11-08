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
public class TransactionStatisticsAggregator extends AbstractAggregator {
    static final def TABLE = "transaction_statistics_aggregator"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TransactionStatisticsAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TransactionStatisticsAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TransactionStatisticsAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${TransactionStatisticsAggregator.eventName:RAISE_ORDER}')
    String eventName

    @Value('${TransactionStatisticsAggregator.currency:MNT}')
    String currency

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${TransactionStatisticsAggregator.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TransactionStatisticsAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("********** TransactionStatisticsAggregator Aggregator started at " + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TransactionStatisticsAggregatorModel> transactionSummaryModels = aggregateDataES(index,
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
                List<TransactionStatisticsAggregatorModel> transactionSummaryModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionSummaryModels);
            }
        }
        log.info("********** TransactionStatisticsAggregator Aggregator ended at " + new Date());
    }


    private List<TransactionStatisticsAggregatorModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<TransactionStatisticsAggregatorModel> transactionSummaryModels = generateResponse(searchResponse);
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
    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
                .filter(QueryBuilders.termsQuery("oms.resultCode", 0));
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<TransactionStatisticsAggregatorModel> generateResponse(SearchResponse searchResponse){
        List<TransactionStatisticsAggregatorModel> transactionStatisticsAggregatorModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);
            HashMap<String, TransactionStatisticsAggregatorModel> transactionStatisticsAggregatorModelMap = new HashMap<>();

            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();

                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp") as String);
                        String accountType = searchHitMap.getOrDefault("SenderAccountType", "RESELLER") as String;

                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        String channel = searchHitMap.getOrDefault("channel", "N/A");
                        for (int i = 0; i < omsItems.size(); i++) {
                            HashMap<String, String> omsItem = omsItems.get(i);

                            if (omsItem != null) {
                                HashMap<String, String> data = omsItem.getOrDefault("data", null)

                                if (data != null && data.getOrDefault("resultCode", null) != null) {
                                    Integer internalResultCode = Integer.valueOf(data.get("resultCode"));

                                    if (internalResultCode == 0) {
                                        String transactionType = data.getOrDefault("operationType", "N/A");

                                        if (data.containsKey("amount") && data.get("amount") != null && !data.get("amount").equals("N/A")) {
                                            Double transactionAmount = Double.valueOf(data.get("amount"));

                                            String id = GenerateHash.createHashString(
                                                    dateTimeDay.toString(),
                                                    channel,
                                                    accountType,
                                                    transactionType
                                            );
                                            if (transactionStatisticsAggregatorModelMap.containsKey(id)) {
                                                TransactionStatisticsAggregatorModel transactionStatisticsAggregatorModel = transactionStatisticsAggregatorModelMap.get(id);
                                                transactionStatisticsAggregatorModel.setTransactionCount(transactionStatisticsAggregatorModel.getTransactionCount() + 1)
                                                transactionStatisticsAggregatorModel.setTotalAmount(transactionStatisticsAggregatorModel.getTotalAmount() + transactionAmount);
                                                transactionStatisticsAggregatorModelMap.put(id, transactionStatisticsAggregatorModel);
                                            } else {
                                                TransactionStatisticsAggregatorModel transactionStatisticsAggregatorModel = new TransactionStatisticsAggregatorModel(id, dateTimeDay, channel,
                                                        accountType, transactionType, 1, transactionAmount, currency);
                                                transactionStatisticsAggregatorModelMap.put(id, transactionStatisticsAggregatorModel);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            transactionStatisticsAggregatorModelMap.each {
                entry -> transactionStatisticsAggregatorModelList.add(entry.value)
            }
        }
        return transactionStatisticsAggregatorModelList;
    }

    private def insertAggregation(List transactionStatisticsAggregatorModelList) {

        log.info("TransactionStatisticsAggregator Aggregated into ${transactionStatisticsAggregatorModelList.size()} rows.")
        if (transactionStatisticsAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,channel,account_type,transaction_type,transaction_count,sum,currency" +
                    ") VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE sum = VALUES(sum), transaction_count = VALUES(transaction_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = transactionStatisticsAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.accountType)
                        ps.setString(++index, row.transactionType)
                        ps.setLong(++index, row.transactionCount)
                        ps.setBigDecimal(++index, row.totalAmount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { transactionStatisticsAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}


class TransactionStatisticsAggregatorModel {
    private String id;
    private Date transactionDate;
    private String channel;
    private String accountType;
    private String transactionType;
    private long transactionCount;
    private Double totalAmount;
    private String currency;

    TransactionStatisticsAggregatorModel(String id, Date transactionDate, String channel, String accountType, String transactionType, long transactionCount, Double totalAmount, String currency) {
        this.id = id
        this.transactionDate = transactionDate
        this.channel = channel
        this.accountType = accountType
        this.transactionType = transactionType
        this.transactionCount = transactionCount
        this.totalAmount = totalAmount
        this.currency = currency
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

    String getAccountType() {
        return accountType
    }

    void setAccountType(String accountType) {
        this.accountType = accountType
    }

    String getTransactionType() {
        return transactionType
    }

    void setTransactionType(String transactionType) {
        this.transactionType = transactionType
    }

    long getTransactionCount() {
        return transactionCount
    }

    void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount
    }

    Double getTotalAmount() {
        return totalAmount
    }

    void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount
    }

    String getCurrency() {
        return currency
    }

    void setCurrency(String currency) {
        this.currency = currency
    }
}
