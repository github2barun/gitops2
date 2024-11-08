package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.*
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.search.aggregations.AggregationBuilders;

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class DealerStockMovementAggregator extends AbstractAggregator {
    static final def TABLE = "dealer_stock_movement_aggregation"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${DealerStockMovementAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DealerStockMovementAggregator.bulkInsertionModeFromDateString:2020-08-17}')
    String bulkInsertionModeFromDateString;

    @Value('${DealerStockMovementAggregator.bulkInsertionModeToDateString:2020-08-19}')
    String bulkInsertionModeToDateString;

    @Value('${DealerStockMovementAggregator.eventName:TRANSFER_INVENTORY}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Transactional
    @Scheduled(cron = '${DealerStockMovementAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("DealerStockMovementAggregator Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<DealerStockMovementModel> dealerStockMovementModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(dealerStockMovementModelList);
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
                List<DealerStockMovementModel> dealerStockMovementModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(dealerStockMovementModelList);
            }
        }

        log.info("DealerStockMovementAggregator Aggregator ended**************************************************************************");
    }


    private List<DealerStockMovementModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        List<DealerStockMovementModel> dealerStockMovementModelList = generateResponse(searchRequest);
        return dealerStockMovementModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {

        /* ELK Query for Nested mapping
        {"size":0,"query":{"bool":{"filter":[{"terms":{"eventName.keyword":["TRANSFER_INVENTORY"],"boost":1}},{"terms":{"resultCode":[0],"boost":1}}],"adjust_pure_negative":true,"boost":1}},"aggs":{"parameters":{"nested":{"path":"ims.items"},"aggs":{"DealerStockMovementAggregator":{"composite":{"size":1000,"sources":[{"timestamp":{"date_histogram":{"field":"ims.items.timestamp","missing_bucket":true,"value_type":"date","format":"iso8601","order":"asc","fixed_interval":"1d"}}},{"category":{"terms":{"field":"ims.items.category.keyword","missing_bucket":true}}},{"subCategory":{"terms":{"field":"ims.items.subCategory.keyword","missing_bucket":true}}},{"productType":{"terms":{"field":"ims.items.productType.keyword","missing_bucket":true}}},{"productSku":{"terms":{"field":"ims.items.productSku.keyword","missing_bucket":true}}},{"sellerId":{"terms":{"field":"ims.items.seller.id.keyword","missing_bucket":true,"order":"asc"}}},{"buyerid":{"terms":{"field":"ims.items.buyer.id.keyword","missing_bucket":true,"order":"asc"}}}]},"aggregations":{"numberOfStocks":{"sum":{"field":"ims.items.quantity"}}}}}}}}
         */
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("timestamp")
                .field("ims.items.timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("sellerId").field("ims.items.seller.id.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("buyerId").field("ims.items.buyer.id.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("category").field("ims.items.category.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("subCategory").field("ims.items.subCategory.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("productType").field("ims.items.productType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("productSku").field("ims.items.productSku.keyword").missingBucket(true));

        NestedAggregationBuilder nestedAggregationBuilder =
                AggregationBuilders.nested("DealerStockMovementAggregator", "ims.items").
                        subAggregation(AggregationBuilders.composite("NestedAggregator", sources).size(1000).
                                subAggregation(AggregationBuilders.sum("numberOfStocks").field("ims.items.quantity")))
        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
                    .filter(QueryBuilders.termsQuery("resultCode", 0))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d").includeLower(true).includeUpper(true))

            searchSourceBuilder.query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
                    .filter(QueryBuilders.termsQuery("resultCode", 0))
            searchSourceBuilder.query(queryBuilder);
        }
        searchSourceBuilder.aggregation(nestedAggregationBuilder).size(0)
        return searchSourceBuilder;
    }

    private List<DealerStockMovementModel> generateResponse(SearchRequest searchRequest) {
        List<DealerStockMovementModel> dealerStockMovementModelList = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.info("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();

            ParsedComposite parsedComposite = ((org.elasticsearch.search.aggregations.bucket.nested.ParsedNested)aggregations.asMap().get("DealerStockMovementAggregator"))
                    .getAggregations().getAsMap().get("NestedAggregator")

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("timestamp"));
                Calendar calender = Calendar.getInstance();
                calender.setTime(dateTimeDay);

                def totalQuantity = bucket.getAggregations().get("numberOfStocks").getAt("value")

                if (keyValuesMap.get("category") == null || (keyValuesMap.get("category").equals("") )) {
                    keyValuesMap.put("category", "N/A");
                }
                if (keyValuesMap.get("subCategory") == null || (keyValuesMap.get("subCategory").equals("") )) {
                    keyValuesMap.put("subCategory", "N/A");
                }
                if (keyValuesMap.get("productType") == null || (keyValuesMap.get("productType").equals("") )) {
                    keyValuesMap.put("productType", "N/A");
                }
                if (keyValuesMap.get("productSku") == null || (keyValuesMap.get("productSku").equals("") )) {
                    keyValuesMap.put("productSku", "N/A");
                }

                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("sellerId"), keyValuesMap.get("buyerId"), keyValuesMap.get("category"), keyValuesMap.get("subCategory"),
                        keyValuesMap.get("productType"), keyValuesMap.get("productSku"));
                DealerStockMovementModel dealerStockMovementModel = new DealerStockMovementModel(id, dateTimeDay, keyValuesMap.get("sellerId"), keyValuesMap.get("buyerId"), keyValuesMap.get("category"),
                        keyValuesMap.get("subCategory"), keyValuesMap.get("productType"), keyValuesMap.get("productSku"), (long) totalQuantity);

                dealerStockMovementModelList.add(dealerStockMovementModel);
            }
        }

        return dealerStockMovementModelList;

    }

    private def insertAggregation(List dealerStockMovementModelList) {

        log.info("DealerStockMovementAggregator Aggregated into ${dealerStockMovementModelList.size()} rows.")
        if (dealerStockMovementModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,seller_id,buyer_id,category,sub_category,product_type," +
                    "product_sku,total_stocks_count) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE total_stocks_count = VALUES(total_stocks_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dealerStockMovementModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.sellerId)
                        ps.setString(++index, row.buyerId)
                        ps.setString(++index, row.category)
                        ps.setString(++index, row.subCategory)
                        ps.setString(++index, row.productType)
                        ps.setString(++index, row.productSku)
                        ps.setLong(++index, row.numberOfStocks)

                    },
                    getBatchSize: { dealerStockMovementModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class DealerStockMovementModel {
    private String id;
    private Date transactionDate;
    private String sellerId;
    private String buyerId;
    private String category;
    private String subCategory;
    private String productType;
    private String productSku;
    private long numberOfStocks;

    public DealerStockMovementModel(String id, Date transactionDate, String sellerId, String buyerId, String category, String subCategory, String productType, String productSku, long numberOfStocks) {
        this.id = id
        this.transactionDate = transactionDate
        this.sellerId = sellerId
        this.buyerId = buyerId
        this.category = category
        this.subCategory = subCategory
        this.productType = productType
        this.productSku = productSku
        this.numberOfStocks = numberOfStocks
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

    String getSellerId() {
        return sellerId
    }

    void setSellerId(String sellerId) {
        this.sellerId = sellerId
    }

    String getBuyerId() {
        return buyerId
    }

    void setBuyerId(String buyerId) {
        this.buyerId = buyerId
    }

    String getCategory() {
        return category
    }

    void setCategory(String category) {
        this.category = category
    }

    String getSubCategory() {
        return subCategory
    }

    void setSubCategory(String subCategory) {
        this.subCategory = subCategory
    }

    String getProductType() {
        return productType
    }

    void setProductType(String productType) {
        this.productType = productType
    }

    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }

    long getNumberOfStocks() {
        return numberOfStocks
    }

    void setNumberOfStocks(long numberOfStocks) {
        this.numberOfStocks = numberOfStocks
    }
}