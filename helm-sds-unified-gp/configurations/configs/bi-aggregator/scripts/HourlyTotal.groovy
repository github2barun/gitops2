package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
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
public class HourlyTotal extends AbstractAggregator {
    static final def TABLE = "hourly_total_transactions"

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${HourlyTotal.hourwisedata:true}')
    boolean hourwise;
    @Value('${HourlyTotal.hour:10}')
    int hours;

    @Value('${HourlyTotal.profileId:CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP}')
    String profileId;

    @Value('${HourlyTotal.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${HourlyTotal.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${HourlyTotal.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;


    @Transactional
    @Scheduled(cron = '${HourlyTotal.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("HourlyTotal Aggregator started********************************************************************************************" + new Date());
        def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TotalTransaction> totalTransactionsES = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList)

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
                //fetch data from ES
                List<TotalTransaction> totalTransactionsES = aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(),profileIdList)

            }
        }

        log.info("HourlyTotal Aggregator ended********************************************************************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate,String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        Map<String, Object> afterKey = null;
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate,afterKey,profileIdList);
        searchRequest.source(searchSourceBuilder);
        afterKey= generateResponse(searchRequest);

        if(afterKey!=null) {
            log.info("##############" +afterKey.size())
            while(afterKey!=null) {
                searchSourceBuilder = fetchInput(fromDate, toDate,afterKey,profileIdList);
                searchRequest.source(searchSourceBuilder);
                afterKey = generateResponse(searchRequest);
            }
        }

    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate,Map<String, Object> afterKey,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);
        DateHistogramValuesSourceBuilder dateHistoByHour = new DateHistogramValuesSourceBuilder("endTimeHour")
                .field("endTime").fixedInterval(DateHistogramInterval.hours(1)).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(dateHistoByHour);
        sources.add(new TermsValuesSourceBuilder("SenderRegion").field("senderRegion.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("SenderResellerType").field("senderResellerType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("TransactionType").field("transactionProfile.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Channel").field("channel.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ResultStatus").field("resultMessage.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Currency").field("currency.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("ErsReference").field("ersReference.keyword").missingBucket(true))

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("Hourlytotal",
                sources).size(10000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("RequestAmountValue").field("transactionAmount"))

        if (!bulkInsertionMode) {
            QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileID))
                    .filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
            searchSourceBuilder.query(queryBuilder);
        }
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);


        return searchSourceBuilder;
    }

    private  Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<TotalTransaction> totalTransactions = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("Hourlytotal");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                Aggregation totalAmountAggregration = aggregationMap.get("RequestAmountValue");
                ParsedSum p = (ParsedSum) totalAmountAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));
                Date dateTimeHour = DateFormatter.formatDate(keyValuesMap.get("endTimeHour"));
                String id = GenerateHash.createHashString( keyValuesMap.get("ErsReference"), keyValuesMap.get("SenderRegion"),keyValuesMap.get("SenderResellerType"), keyValuesMap.get("TransactionType"), keyValuesMap.get("Channel"), keyValuesMap.get("ResultStatus"),keyValuesMap.get("Currency"));
                TotalTransaction totalTransaction = new TotalTransaction(id, dateTimeDay, dateTimeHour, keyValuesMap.get("SenderRegion"),keyValuesMap.get("SenderResellerType"), keyValuesMap.get("TransactionType"), keyValuesMap.get("Channel"), keyValuesMap.get("ResultStatus"), bucket.getDocCount(), p.value(),keyValuesMap.get("Currency"));
                totalTransactions.add(totalTransaction);
            }
            insertAggregation( totalTransactions);
            return parsedComposite.afterKey();
        }

        return null;

    }

    private def insertAggregation(List totalTransactions) {

        log.info("HourlyTotal Aggregated into ${totalTransactions.size()} rows.")
        if (totalTransactions.size() != 0) {
            def sql = "INSERT IGNORE INTO ${TABLE} (id,end_time_day,end_time_hour,sender_region,sender_reseller_type,transaction_type,channel,result_status,count,amount,currency) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count = VALUES(count), amount = VALUES(amount)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        ps.setTimestamp(++index, new java.sql.Timestamp(row.endTimeHour.getTime()))
                        ps.setString(++index, row.senderRegion)
                        ps.setString(++index, row.senderResellerType)
                        ps.setString(++index, row.transactionType)
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.resultStatus)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class TotalTransaction {
    private String id;
    //private String year;
    //private String month;
    // private String day;
    //private String hour;
    private Date endTimeDay;
    private Date endTimeHour;
    private String senderRegion;
    private String senderResellerType;


    private String transactionType;



    private String channel;
    private String resultStatus;
    private long count;
    private double amount;
    private String currency;
    //private Date actualDateTime;


    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
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

    public Date getEndTimeHour() {
        return endTimeHour;
    }

    public void setEndTimeHour(Date endTimeHour) {
        this.endTimeHour = endTimeHour;
    }

    public String getSenderRegion() {
        return senderRegion;
    }

    public void setSenderRegion(String senderRegion) {
        this.senderRegion = senderRegion;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
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
    public String getSenderResellerType() {
        return senderResellerType;
    }

    public void setSenderResellerType(String senderResellerType) {
        this.senderResellerType = senderResellerType;
    }

    public TotalTransaction(String id, Date endTimeDay, Date endTimeHour, String senderRegion, String senderResellerType, String transactionType, String channel, String resultStatus, long count, double amount, String currency) {
        this.id = id;
        this.endTimeDay = endTimeDay;
        this.endTimeHour = endTimeHour;
        this.senderRegion = senderRegion;
        this.senderResellerType = senderResellerType;
        this.transactionType = transactionType;
        this.channel = channel;
        this.resultStatus = resultStatus;
        this.count = count;
        this.amount = amount;
        this.currency=currency;
    }

}
