
package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import java.util.Date;
import java.util.List;
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
public class ChannelWiseDayWise extends AbstractAggregator {
    static final def TABLE = "channel_wise_day_wise"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${ChannelWiseDayWise.hourwisedata:true}')
    boolean hourwise;
    @Value('${ChannelWiseDayWise.hour:10}')
    int hours;

    @Value('${ChannelWiseDayWise.profileId:CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP}')
    String profileId;

    @Value('${ChannelWiseDayWise.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ChannelWiseDayWise.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ChannelWiseDayWise.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${ChannelWiseDayWise.fromTimeString:now+0h+0m-1m/m}')
    String fromTimeString

    @Value('${ChannelWiseDayWise.toTimeString:now+0h+0m}')
    String toTimeString


    @Transactional
    @Scheduled(cron = '${ChannelWiseDayWise.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("ChannelWiseDayWise Aggregator started***************************************************************************" + new Date());
        def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<ChannelWiseDayWiseModel> channelWiseDayWiseModelES = aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList)
                    insertAggregation(channelWiseDayWiseModelES);
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
                List<ChannelWiseDayWiseModel> channelWiseDayWiseModelES = aggregateDataES(index.getIndexName(), fromTimeString, toTimeString,profileIdList)
                insertAggregation(channelWiseDayWiseModelES);
            }
        }

        log.info("ChannelWiseDayWise Aggregator ended**************************************************************************");
    }


    private List<ChannelWiseDayWiseModel> aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList);
        searchRequest.source(searchSourceBuilder);
        List<ChannelWiseDayWiseModel> channelWiseDayWiseModels = generateResponse(searchRequest);
        return channelWiseDayWiseModels;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("EndTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("SenderRegion").field("senderRegion.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Channel").field("channel.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("SenderAccountType").field("senderAccountType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("SenderResellerType").field("senderResellerType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("TransactionType").field("transactionProfile.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Currency").field("currency.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("ChannelWiseDayWise",
                sources).size(10000);

        compositeBuilder.subAggregation(AggregationBuilders.sum("RequestAmountValue").field("transactionAmount"))

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
                    .filter(QueryBuilders.termsQuery("transactionProfile.keyword",profileID))
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<ChannelWiseDayWiseModel> generateResponse(SearchRequest searchRequest) {
        List<ChannelWiseDayWiseModel> channelWiseDayWiseModels = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("ChannelWiseDayWise");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                Aggregation totalAmountAggregration = aggregationMap.get("RequestAmountValue");
                ParsedSum p = (ParsedSum) totalAmountAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("EndTimeDay"));

                String id = GenerateHash.createHashString(keyValuesMap.get("Channel"),keyValuesMap.get("TransactionType"));
                ChannelWiseDayWiseModel channelWiseDayWiseModel = new ChannelWiseDayWiseModel(id, 'seamless', dateTimeDay,keyValuesMap.get("SenderRegion"),keyValuesMap.get("Channel"),keyValuesMap.get("SenderAccountType"),keyValuesMap.get("SenderResellerType"),keyValuesMap.get("TransactionType"), bucket.getDocCount(),p.value(),keyValuesMap.get("Currency"));

                channelWiseDayWiseModels.add(channelWiseDayWiseModel);
            }
        }

        return channelWiseDayWiseModels;

    }

    private def insertAggregation(List channelWiseDayWiseModels) {

        log.info("ChannelWiseDayWise Aggregated into ${channelWiseDayWiseModels.size()} rows.")
        if (channelWiseDayWiseModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,operator,end_time_day,sender_region,channel,account_type,reseller_type,transaction_type,count,amount,currency) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE channel_wise_day_wise.count = channel_wise_day_wise.count+VALUES(count), channel_wise_day_wise.amount = channel_wise_day_wise.amount+VALUES(amount), end_time_day = VALUES(end_time_day)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = channelWiseDayWiseModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.operator)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        //ps.setTimestamp(++index, new java.sql.Timestamp(row.endTimeHour.getTime()))
                        ps.setString(++index, row.senderRegion)
                        ps.setString(++index, row.channel)
                        ps.setString(++index, row.senderAccountType)
                        ps.setString(++index, row.resellerType)
                        ps.setString(++index, row.transactionType)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { channelWiseDayWiseModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}
class ChannelWiseDayWiseModel {
    private String id;
    private String operator;
    //private String year;
    //private String month;
    // private String day;
    //private String hour;
    private Date endTimeDay;
    //private Date endTimeHour;
    private String senderRegion;
    private String channel;
    private String senderAccountType;
    private String resellerType;
    private String transactionType;
    //private String resultStatus;
    private long count;
    private double amount;
    private String currency;


//private Date actualDateTime;




    public ChannelWiseDayWiseModel(String id, String operator, Date endTimeDay, String senderRegion, String channel, String senderAccountType, String resellerType, String transactionType, long count, double amount, String currency) {
        this.id = id;
        this.operator = operator;
        this.endTimeDay = endTimeDay;
        //this.endTimeHour=endTimeHour;
        this.senderRegion=senderRegion;
        this.channel = channel;
        this.senderAccountType=senderAccountType;
        this.resellerType=resellerType;
        this.transactionType=transactionType;
        this.count = count;
        this.amount = amount;
        this.currency = currency;
        // this.actualDateTime = actualDateTime;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getEndTimeDay() {
        return endTimeDay;
    }

    public void setEndTimeDay(Date endTimeDay) {
        this.endTimeDay = endTimeDay;
    }


    public String getResellerType() {
        return resellerType;
    }

    public void setResellerType(String resellerType) {
        this.resellerType = resellerType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSenderAccountType() {
        return senderAccountType;
    }

    public void setSenderAccountType(String senderAccountType) {
        this.senderAccountType = senderAccountType;
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
