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
public class ExternalPosStockHolding extends ScrollableAbstractAggregator {
    static final def TABLE = "external_pos_stock_holding"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${ExternalPosStockHolding.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${ExternalPosStockHolding.bulkInsertionModeFromDateString:2022-01-01}')
    String bulkInsertionModeFromDateString;

    @Value('${ExternalPosStockHolding.bulkInsertionModeToDateString:2022-01-01}')
    String bulkInsertionModeToDateString;

    @Value('${ExternalPosStockHolding.eventName:ExternalPosStockHoldingDataDaily}')
    String eventName

    @Value('${ExternalPosStockHolding.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${ExternalPosStockHolding.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** ExternalPosStockHolding Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<ExternalPosStockHoldingModel> ExternalPosStockHoldingModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(ExternalPosStockHoldingModelList);
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
                List<ExternalPosStockHoldingModel> ExternalPosStockHoldingModelList = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate());
                insertAggregation(ExternalPosStockHoldingModelList);
            }
        }

        log.info("********** ExternalPosStockHolding Aggregator ended at " + new Date());
    }

    private List<ExternalPosStockHoldingModel> aggregateDataES(String index, String fromDate, String toDate) {
        List<ExternalPosStockHoldingModel> ExternalPosStockHoldingModelList = new ArrayList<>()
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            ExternalPosStockHoldingModelList = generateResponse(searchResponse);
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    ExternalPosStockHoldingModelList.addAll(generateResponse(searchResponse));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return ExternalPosStockHoldingModelList;
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

    private List<ExternalPosStockHolding> generateResponse(SearchResponse searchResponse) {
        List<ExternalPosStockHoldingModel> ExternalPosStockHoldingModelList = new ArrayList<>();
        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("timestamp"));
                    Timestamp transactionDate = new Timestamp(dateTimeDay.getTime())
                    String resellerId = searchHitMap.getOrDefault("resellerId", null);
                    String sku = searchHitMap.getOrDefault("sku", null);
                    String resellerType = searchHitMap.getOrDefault("dealerType", null);
                    Long stockQtySold = searchHitMap.getOrDefault("stock_qty_sold", null) as long;
                    Long stockQtyHand = searchHitMap.getOrDefault("stock_qty_on_hand", null) as long;



                    String id = GenerateHash.createHashString(transactionDate as String, resellerId, sku)
                    ExternalPosStockHoldingModel stockHoldingModel = new ExternalPosStockHoldingModel(id, resellerId, resellerType, transactionDate, sku, stockQtySold, stockQtyHand);

                    ExternalPosStockHoldingModelList.add(stockHoldingModel)
                }
            }
            return ExternalPosStockHoldingModelList;
        }
    }

    private def insertAggregation(List ExternalPosStockHoldingModelList) {

        log.info("ExternalPosStockHolding Aggregated into ${ExternalPosStockHoldingModelList.size()} rows.")
        if (ExternalPosStockHoldingModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,stock_date,reseller_id,reseller_type,product_sku,stock_qty_sold,stock_qty_hand) " +
                    "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = ExternalPosStockHoldingModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setTimestamp(++index, row.stockDate)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.sku)
                        ps.setLong(++index, row.stockQtySold)
                        ps.setLong(++index, row.stockQtyHand)
                    },
                    getBatchSize: { ExternalPosStockHoldingModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }
}

class ExternalPosStockHoldingModel {
    private String id;
    private String resellerId;
    private String resellerType;
    private Timestamp stockDate;
    private String sku;
    private Long stockQtySold;
    private Long stockQtyHand;

    ExternalPosStockHoldingModel(String id, String resellerId, String resellerType, Timestamp stockDate, String sku, Long stockQtySold, Long stockQtyHand) {
        this.id = id
        this.resellerId = resellerId
        this.resellerType = resellerType
        this.stockDate = stockDate
        this.sku = sku
        this.stockQtySold = stockQtySold
        this.stockQtyHand = stockQtyHand
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

    String getResellerType() {
        return resellerType
    }

    void setResellerType(String resellerType) {
        this.resellerType = resellerType
    }

    Timestamp getStockDate() {
        return stockDate
    }

    void setStockDate(Timestamp stockDate) {
        this.stockDate = stockDate
    }

    String getSku() {
        return sku
    }

    void setSku(String sku) {
        this.sku = sku
    }

    Long getStockQtySold() {
        return stockQtySold
    }

    void setStockQtySold(Long stockQtySold) {
        this.stockQtySold = stockQtySold
    }

    Long getStockQtyHand() {
        return stockQtyHand
    }

    void setStockQtyHand(Long stockQtyHand) {
        this.stockQtyHand = stockQtyHand
    }
}