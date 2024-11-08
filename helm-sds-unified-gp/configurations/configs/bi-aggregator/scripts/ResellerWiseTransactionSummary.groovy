package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import java.util.Date;
import com.seamless.customer.bi.aggregator.model.ReportIndex
//import com.seamless.customer.bi.aggregator.model.ResellerWiseTransactionSummaryModel
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.FieldNames
import com.seamless.customer.bi.aggregator.util.GenerateHash
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality
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
public class ResellerWiseTransactionSummary extends AbstractAggregator {
    static final def TABLE = "std_sales_trend_aggregation"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${ResellerWiseTransactionSummary.hourwisedata:true}')
    boolean hourwise;
    @Value('${ResellerWiseTransactionSummary.hour:10}')
    int hours;


    @Value('${ResellerWiseTransactionSummary.profileId:CREDIT_TRANSFER}')
    String profileId;

    @Value('${ResellerWiseTransactionSummary.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ResellerWiseTransactionSummary.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ResellerWiseTransactionSummary.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${ResellerWiseTransactionSummary.fromTimeString:now+0h+0m-1m/m}')
    String fromTimeString

    @Value('${ResellerWiseTransactionSummary.toTimeString:now+0h+0m}')
    String toTimeString

    @Transactional
    @Scheduled(cron = '${ResellerWiseTransactionSummary.cron:*/3 * * * * ?}')


    public void aggregate() {

        log.info("ResellerWiseTransactionSummary Aggregator started******************************************************************" + new Date());

        def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES

                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString,profileIdList);
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
                (aggregateDataES(index.getIndexName(), fromTimeString, toTimeString,profileIdList));
            }
        }

        log.info("ResellerWiseTransactionSummary Aggregator ended********************************************************************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate,String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        Map<String, Object> afterKey = null;
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate,afterKey,profileIdList);
        searchRequest.source(searchSourceBuilder);
        afterKey= generateResponse(searchRequest);

        if(afterKey!=null) {
            log.info("##############" +afterKey.size())
            while(afterKey!=null) {
                searchSourceBuilder = buildESQuery(fromDate, toDate,afterKey,profileIdList);
                searchRequest.source(searchSourceBuilder);
                afterKey = generateResponse(searchRequest);
            }
        }



    }

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate, Map<String, Object> afterKey,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
                .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601");


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("SenderAccountType").field("senderAccountType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderResellerId").field("senderResellerId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderMSISDN").field("senderMSISDN.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderResellerType").field("senderResellerType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderResellerPath").field("senderResellerPath.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("transactionType").field("transactionProfile.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderRegion").field("senderRegionId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("currency").field("currency.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderResellerName").field("senderResellerName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderCommission").field("senderCommission.keyword").missingBucket(true))

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("ResellerWiseTransactionSummary",
                sources).size(10000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("requestAmountValue").field("transactionAmount")).subAggregation(AggregationBuilders.cardinality("uniqueReceiverCount").field("receiverMSISDN.keyword"))


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
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<ResellerWiseTransactionSummaryModel> totalTransactions = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("ResellerWiseTransactionSummary");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

                Aggregation totalAmountAggregration = aggregationMap.get("requestAmountValue");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregration;

                totalAmountAggregration = aggregationMap.get("uniqueReceiverCount");
                ParsedCardinality uniqueReceiverCount = (ParsedCardinality) totalAmountAggregration;


                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));

                String id = GenerateHash.createHashString(keyValuesMap.get("senderResellerId"), keyValuesMap.get("transactionType"));

                ResellerWiseTransactionSummaryModel totalTransaction = new ResellerWiseTransactionSummaryModel(id, dateTimeDay, keyValuesMap.get("SenderAccountType"), keyValuesMap.get("senderResellerId"), keyValuesMap.get("senderMSISDN"), keyValuesMap.get("senderResellerName"), keyValuesMap.get("senderResellerType"), keyValuesMap.get("senderResellerPath"), keyValuesMap.get("senderRegion"), keyValuesMap.get("transactionType"), uniqueReceiverCount.value(), bucket.getDocCount(), totalAmount.value(), keyValuesMap.get("currency"));
                totalTransactions.add(totalTransaction);
            }

            insertAggregation( totalTransactions);
            log.info("ResellerWiseTransactionSummary Aggregated into ${totalTransactions.size()} rows.")
            return parsedComposite.afterKey();

        }
        return null;


    }

    private def insertAggregation(List totalTransactions) {


        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregationDate,account_type,resellerId,resellerMSISDN,resellerName,resellerTypeId,reseller_path,region,transaction_type,unique_receiver_count,count,transactionAmount,reseller_commission,currency) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE std_sales_trend_aggregation.count = std_sales_trend_aggregation.count+VALUES(count), std_sales_trend_aggregation.transactionAmount = std_sales_trend_aggregation.transactionAmount+VALUES(transactionAmount),std_sales_trend_aggregation.reseller_commission = std_sales_trend_aggregation.reseller_commission+VALUES(reseller_commission), aggregationDate = VALUES(aggregationDate)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        ps.setString(++index, row.senderAccountType)
                        ps.setString(++index, row.senderResellerId)
                        ps.setString(++index, row.senderMSISDN)
                        ps.setString(++index, row.senderResellerName)
                        ps.setString(++index, row.senderResellerType)
                        ps.setString(++index, row.senderResellerPath)
                        ps.setString(++index, row.senderRegion)
                        ps.setString(++index, row.transactionType)
                        ps.setDouble(++index,row.uniqueReceiverCount)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setDouble(++index, row.commission)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class ResellerWiseTransactionSummaryModel {
    private String id;
    private Date endTimeDay;
    private String senderAccountType;
    private String senderResellerId;
    private String senderMSISDN;
    private String senderResellerName;
    private String senderResellerType;
    private String senderResellerPath;
    private String senderRegion;
    private String transactionType;
    private double uniqueReceiverCount;
    private long count;
    private double amount;
    private double commission;
    private String currency;


    public double getUniqueReceiverCount() {
        return uniqueReceiverCount;
    }

    public void setUniqueReceiverCount(double uniqueReceiverCount) {
        this.uniqueReceiverCount = uniqueReceiverCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String operator) {
        this.currency = currency;
    }

    public Date getEndTimeDay() {
        return endTimeDay;
    }

    public void setEndTimeDay(Date endTimeDay) {
        this.endTimeDay = endTimeDay;
    }

    public String getSenderResellerId() {
        return senderResellerId;
    }

    public void setSenderResellerId(String senderResellerId) {
        this.senderResellerId = senderResellerId;
    }

    public String getSenderMSISDN() {
        return senderMSISDN;
    }

    public void setSenderMSISDN(String senderMSISDN) {
        this.senderMSISDN = senderMSISDN;
    }

    public String getSenderResellerType() {
        return senderResellerType;
    }

    public void setSenderResellerType(String senderResellerType) {
        this.senderResellerType = senderResellerType;
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

    public double getCommission() {
        return commission;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public String getSenderResellerPath() {
        return senderResellerPath;
    }

    public void setSenderResellerPath(String senderResellerPath) {
        this.senderResellerPath = senderResellerPath;
    }
    //private Date actualDateTime;


    public String getSenderRegion() {
        return senderRegion;
    }

    public void setSenderRegion(String senderRegion) {
        this.senderRegion = senderRegion;
    }

    public String getSenderResellerName() {
        return senderResellerName;
    }

    public void setSenderResellerName(String senderResellerName) {
        this.senderResellerName = senderResellerName;
    }

    public String getSenderAccountType() {
        return senderAccountType;
    }

    public void setSenderAccountType(String senderAccountType) {
        this.senderAccountType = senderAccountType;
    }

    public ResellerWiseTransactionSummaryModel(String id, Date endTimeDay, String senderAccountType, String senderResellerId, String senderMSISDN, String senderResellerName, String senderResellerType, String senderResellerPath, String senderRegion, String transactionType, double uniqueReceiverCount, long count, double amount, String currency) {
        this.id = id;
        this.currency = currency;
        this.endTimeDay = endTimeDay;
        this.senderAccountType=senderAccountType;
        this.senderResellerId = senderResellerId;
        this.senderMSISDN = senderMSISDN;
        this.senderResellerName = senderResellerName;
        this.senderResellerType = senderResellerType;
        this.senderResellerPath = senderResellerPath;
        this.senderRegion = senderRegion;
        this.transactionType = transactionType;
        this.uniqueReceiverCount = uniqueReceiverCount;
        this.count = count;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "ResellerWiseTransactionSummaryModel{" +
                "id='" + id + '\'' +
                ", currency='" + currency + '\'' +
                ", endTimeDay=" + endTimeDay +
                ", senderAccountType=" + senderAccountType +
                ", senderResellerId='" + senderResellerId + '\'' +
                ", senderMSISDN='" + senderMSISDN + '\'' +
                ", senderResellerName='" + senderResellerName + '\'' +
                ", senderResellerType='" + senderResellerType + '\'' +
                ", senderResellerPath='" + senderResellerPath + '\'' +
                ", senderRegion='" + senderRegion + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", uniqueReceiverCount=" + uniqueReceiverCount +
                ", count=" + count +
                ", amount=" + amount +
                ", commission=" + commission +
                '}';
    }
}



/*
CREATE TABLE `std_sales_trend_aggregation` (
  `id` varchar(255) NOT NULL,
  `aggregationDate` date DEFAULT NULL,
  `resellerId` varchar(25) DEFAULT NULL,
  `resellerMSISDN` varchar(25) DEFAULT NULL,
  `resellerName` varchar(25) DEFAULT NULL,
  `resellerTypeId` varchar(25) DEFAULT NULL,
  `reseller_path` varchar(150) DEFAULT NULL,
  `region` varchar(150) DEFAULT NULL,
  `transaction_type` varchar(25) DEFAULT NULL,
  `unique_receiver_count` double DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  `transactionAmount` double DEFAULT NULL,
  `currency` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 */