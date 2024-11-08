package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import com.seamless.customer.bi.aggregator.util.DateUtil

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class ElectronicRecharge extends AbstractAggregator {
    static final def TABLE = "electronic_recharge"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${ElectronicRecharge.hourwisedata:true}')
    boolean hourwise;
    @Value('${ElectronicRecharge.hour:10}')
    int hours;

    @Value('${ElectronicRecharge.r2rProfileId:CREDIT_TRANSFER}')
    String r2rProfileId;

    @Value('${ElectronicRecharge.r2sProfileId:TOPUP}')
    String r2sProfileId;

    @Value('${ElectronicRecharge.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ElectronicRecharge.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ElectronicRecharge.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;


    @Transactional
    @Scheduled(cron = '${ElectronicRecharge.cron:*/3 * * * * ?}')


    public void aggregate() {

        log.info("ElectronicRecharge Aggregator started********************************************************************************************" + new Date());
        log.info(hours.toString());

        def r2rProfileIdList = r2rProfileId.split(",")
        def r2sProfileIdList = r2sProfileId.split(",")

        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES

                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString,r2rProfileIdList,r2sProfileIdList);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                catch (Exception e){
                    log.error(e.getMessage())
                }
            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex();

            for (ReportIndex index : indices) {

                log.info(index.toString())
                (aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(),r2rProfileIdList,r2sProfileIdList));
            }
        }

        log.info("ElectronicRecharge Aggregator ended****************************************************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate,String[] r2rProfileIdList,String[] r2sProfileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate, r2rProfileIdList);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.query()
        insertAggregation(generateResponse(searchRequest));

        searchSourceBuilder = buildESQuery(fromDate, toDate,r2sProfileIdList);
        searchRequest.source(searchSourceBuilder);
        insertR2SAggregation(generateResponse(searchRequest));

    }

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);

        sources.add(new TermsValuesSourceBuilder("channel").field("channel.keyword").missingBucket(true))
        sources.add(new TermsValuesSourceBuilder("currency").field("currency.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("ElectronicRecharge",
                sources).size(1000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("requestAmountValue").field("transactionAmount"))


        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileID))
                    .filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<ElectronicRechargeModel> generateResponse(SearchRequest searchRequest) {
        List<ElectronicRechargeModel> totalTransactions = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.debug("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("ElectronicRecharge");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

                Aggregation totalAmountAggregration = aggregationMap.get("requestAmountValue");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregration;

                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));

                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("channel"), keyValuesMap.get("currency"));
                ElectronicRechargeModel totalTransaction = new ElectronicRechargeModel(id, dateTimeDay, keyValuesMap.get("channel"), bucket.getDocCount(), totalAmount.value(),keyValuesMap.get("currency"));
                totalTransactions.add(totalTransaction);
            }
            log.info("ElectronicRecharge Aggregated into ${totalTransactions.size()} rows.")



        }
        return  totalTransactions;


    }

    private def insertAggregation(List totalTransactions) {


        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregation_date,channel,r2r_count,r2r_amount,currency) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE r2r_count = VALUES(r2r_count), r2r_amount = VALUES(r2r_amount)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        ps.setString(++index, row.channel)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

    private def insertR2SAggregation(List totalTransactions) {


        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregation_date,channel,r2s_count,r2s_amount,currency) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE r2s_count = VALUES(r2s_count), r2s_amount = VALUES(r2s_amount)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        ps.setString(++index, row.channel)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}
class ElectronicRechargeModel {


    private String id;
    private Date endTimeDay;
    private String channel;
    private long count;
    private double amount;
    private String currency;

    public ElectronicRechargeModel(String id, Date endTimeDay, String channel, long count, double amount, String currency) {
        this.id = id;
        this.endTimeDay = endTimeDay;
        this.channel = channel;
        this.count = count;
        this.amount = amount;
        this.currency = currency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getEndTimeDay() {
        return endTimeDay;
    }

    public void setEndTimeDay(Date endTimeDay) {
        this.endTimeDay = endTimeDay;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
