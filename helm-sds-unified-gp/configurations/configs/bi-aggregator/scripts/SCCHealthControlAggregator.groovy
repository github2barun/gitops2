package com.seamless.customer.bi.aggregator.aggregate

import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder
import org.elasticsearch.search.aggregations.metrics.ParsedSum
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram

import groovy.util.logging.Slf4j
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
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

import java.text.SimpleDateFormat
import java.time.LocalDate


/**
 *
 *
 *
 *
 */
@Slf4j
class SCCHealthControlAggregator extends ScrollableAbstractAggregator {
    static final def DATABASE = "bi"
    static final def TABLE = "health_control_info"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value('${SCCHealthControlAggregator.scrollSize:7000}')
    int scrollSize;

    @Value('${SCCHealthControlAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${SCCHealthControlAggregator.bulkInsertionModeFromDateString:2022-08-01}')
    String bulkInsertionModeFromDateString;

    @Value('${SCCHealthControlAggregator.bulkInsertionModeToDateString:2022-08-30}')
    String bulkInsertionModeToDateString;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")

    SearchResponse searchResponse;
    @Transactional
    @Scheduled(cron = '${SCCHealthControlAggregator.cron:*/2 * * * * ?}')
    void aggregate() {
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            log.info("SCCHealthControlAggregator Aggregator started**************************************************************************");
            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change
            for (String index : indices) {
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
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
                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate());
            }
        }
        // aggregateDataES(index)
        log.info("SCCHealthControlAggregator Aggregator ended**************************************************************************");
    }


    void aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateResponse(searchRequest);
        if (searchResponse != null) {
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size());
            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(1));
                try {
                    searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                } catch (Exception e) {
                    log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
                }
                scrollId = generateScrollResponse(searchResponse);
            }
        }
    }

    SearchSourceBuilder buildESQuery(String fromDate, String toDate) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(scrollSize);
        CompositeAggregationBuilder compositeBuilder = null;
         DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("DayWiseAggs")
                        .field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);
        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("commission.campaignName").field("commission.campaignName.keyword").missingBucket(false));
        compositeBuilder = new CompositeAggregationBuilder("HealthControl",sources).size(10000);
        compositeBuilder.subAggregation(AggregationBuilders.sum("CommissionAmount").field("commission.commissionAmount").missing(0));
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalCount").field("sccAggregator.totalRawTransactions").missing(0));
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalAmount").field("sccAggregator.totalRawTransactionAmount").missing(0));
        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                                searchSourceBuilder.query(queryBuilder);
        } else {
           BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                               .filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
                                           searchSourceBuilder.query(queryBuilder);
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    SearchResponse generateResponse(SearchRequest searchRequest) {
        List<HealthControl> data = new ArrayList<>();
        searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Could not perform search on elasticsearch. Error message: " + e);
        }
        log.info("*******Search Request*******" + searchRequest.toString())
        RestStatus status = searchResponse.status();

        if (searchResponse != null) {
            log.info("response status -------------" + status);

            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    List<HealthControl> healthControlList = fetchData(searchHit);
                    if (healthControlList.size() > 0) data.addAll(healthControlList)
                }
                log.info("loop finish******************");
            }
            insertAggregation(data);
            log.info("inserted first time in table");

        } else {
            log.info("No response found")
        }
        return searchResponse;

    }
    protected List<Aggregations> getAggregations(SearchResponse searchResponses)
    {
        List<Aggregations> aggregationsList = new ArrayList<>();
        // Getting non-null aggregations from SearchResponse

        Aggregations aggregations = searchResponse.getAggregations();
        if (Objects.nonNull(aggregations)) {
            aggregationsList.add(searchResponse.getAggregations());
        }
        return aggregationsList;
    }


    List<HealthControl> fetchData(SearchHit searchHit) {
        List<HealthControl> HealthControlList = new ArrayList<>();
        Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
        try {
            List<Aggregations> aggregations = getAggregations(searchResponse);
            ParsedComposite parsedComposite= (ParsedComposite) aggregations.get(0).asMap().get("HealthControl");
            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> bucketKeyValueMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                        Aggregation commissionAmountAggregation = aggregationMap.get("CommissionAmount");
                        ParsedSum commissionAmountSum = (ParsedSum) commissionAmountAggregation;

                        Aggregation totalCountAggregation = aggregationMap.get("TotalCount");
                        ParsedSum totalCountSum = (ParsedSum) totalCountAggregation;

                        Aggregation totalAmountAggregation = aggregationMap.get("TotalAmount");
                        ParsedSum totalAmountSum = (ParsedSum) totalAmountAggregation;


                        LocalDate localDate = LocalDate.parse(bucketKeyValueMap.get("DayWiseAggs"),java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));


                         String id = GenerateHash.createHashString(
                              bucketKeyValueMap.get("commission.campaignName"),
                              localDate.toString());

                        HealthControl data = new HealthControl(id,
                                bucketKeyValueMap.get("commission.campaignName"),
                                localDate.toString(),
                                bucket.getDocCount().toString(),
                                commissionAmountSum.value().toString(),
                                totalCountSum.value().toString(),
                                totalAmountSum.value().toString()
                        );

                        HealthControlList.add(data);
            }
        }
        catch (Exception e) {
            log.error("Exception " + e);
            return null;
        }
        return HealthControlList;
    }

    String generateScrollResponse(SearchResponse searchScrollResponse) {
        List<HealthControl> data = new ArrayList<>();
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchScrollResponse.getHits();
            log.info("no of hits after 1st request: " + searchHits.size());
            for (SearchHit searchHit : searchHits.getHits()) {
                List<HealthControl> healthControlList = fetchData(searchHit);
                if (healthControlList.size() > 0) data.addAll(healthControlList)
            }
        }

        insertAggregation(data);
        log.info("inserting records subsequent time in table, if any");


        return searchScrollResponse.getScrollId();
    }



    def insertAggregation(List data) {
        if (data.size() != 0) {

            def sql = """INSERT INTO ${DATABASE}.${TABLE}
            (id,commission_name,date_inserted,commission_count,commission_amount,total_count,total_amount)
                VALUES (?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
            commission_count=VALUES(commission_count), commission_amount=VALUES(commission_amount),
            total_count=VALUES(total_count), total_amount=VALUES(total_amount)"""

            jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = data[i]
                        //def index = 0
                        ps.setString(1, row.id)
                        ps.setString(2, row.commissionName)
                        ps.setString(3, row.dateInserted)
                        ps.setString(4, row.commissionCount)
                        ps.setString(5, row.commissionAmount)
                        ps.setString(6, row.totalCount)
                        ps.setString(7, row.totalAmount)
                    },
                    getBatchSize: { data.size() }
            ] as BatchPreparedStatementSetter)
            log.info("Data inserted in health_control_Info table: " + data.size());
        } else {
            log.info("List size empty. Could not insert any rows in table");
        }
    }

}

class HealthControl {
    String id;
    String commissionName;
    String dateInserted;
    String commissionCount;
    String commissionAmount;
    String totalCount;
    String totalAmount;

    HealthControl(String id, String commissionName, String dateInserted, String commissionCount, String commissionAmount, String totalCount, String totalAmount) {
        this.id = id
        this.commissionName = commissionName
        this.dateInserted = dateInserted
        this.commissionCount = commissionCount
        this.commissionAmount = commissionAmount
        this.totalCount = totalCount
        this.totalAmount = totalAmount
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getCommissionName() {
        return commissionName
    }

    void setCommissionName(String commissionName) {
        this.commissionName = commissionName
    }

    String getDateInserted() {
        return dateInserted
    }

    void setDateInserted(String dateInserted) {
        this.dateInserted = dateInserted
    }

    String getCommissionCount() {
        return commissionCount
    }

    void setCommissionCount(String commissionCount) {
        this.commissionCount = commissionCount
    }

    String getCommissionAmount() {
        return commissionAmount
    }

    void setCommissionAmount(String commissionAmount) {
        this.commissionAmount = commissionAmount
    }

    String getTotalCount() {
        return totalCount
    }

    void setTotalCount(String totalCount) {
        this.totalCount = totalCount
    }

    String getTotalAmount() {
        return totalAmount
    }

    void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount
    }
}

