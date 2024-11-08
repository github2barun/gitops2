package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import java.util.Date;
import com.seamless.customer.bi.aggregator.model.ReportIndex
//import com.seamless.customer.bi.aggregator.model.ResellerWiseTransactionSummaryModel
//import com.seamless.customer.bi.aggregator.model.USSDSalesAggregatorModel
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
public class USSDSalesAggregator extends AbstractAggregator {
    static final def TABLE = "std_ussd_sales_aggregation"

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${USSDSalesAggregator.hourwisedata:true}')
    boolean hourwise;
    @Value('${USSDSalesAggregator.hour:10}')
    int hours;

    @Value('${USSDSalesAggregator.profileId:CREDIT_TRANSFER}')
    String profileId;

    @Value('${USSDSalesAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${USSDSalesAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${USSDSalesAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Transactional
    @Scheduled(cron = '${USSDSalesAggregator.cron:*/3 * * * * ?}')


    public void aggregate() {

        log.info("Aggregator started********************************************************************************************" + new Date());
        log.info(hours);
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
                (aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(),profileIdList));
            }
        }

        log.info("Aggregator ended********************************************************************************************");
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


        //  List<TotalTransaction> totalTransactions = generateResponse(searchRequest);

    }

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate, Map<String, Object> afterKey,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("aggregationDate")
                .field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);
        // DateHistogramValuesSourceBuilder dateHistoByHour = new DateHistogramValuesSourceBuilder("endTimeHour")
        //         .field("endTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601");

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        //sources.add(dateHistoByHour);
        sources.add(new TermsValuesSourceBuilder("profileId").field("TransactionProfile").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("senderMSISDN").field("SenderMSISDN").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("receiverMSISDN").field("ReceiverMSISDN").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("currency").field("Currency").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("USSDSalesAggregator",
                sources).size(10000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("amount").field("TransactionAmount")).subAggregation(AggregationBuilders.sum("ReceiverBonusAmount").field("ReceiverBonusAmount"))


        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionType",profileID))
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionType",profileID))
            //.filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<USSDSalesAggregatorModel> totalTransactions = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.debug("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("USSDSalesAggregator");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

                Aggregation totalAmountAggregration = aggregationMap.get("amount");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregration;

                Aggregation receiverBonusAggregation = aggregationMap.get("ReceiverBonusAmount");
                ParsedSum receiverBonusAmount = (ParsedSum) receiverBonusAggregation;

                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("aggregationDate"));
                // Date dateTimeHour = DateFormatter.formatDate(keyValuesMap.get("endTimeHour"));
                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("profileId"), keyValuesMap.get("senderMSISDN"),keyValuesMap.get("receiverMSISDN"), keyValuesMap.get("currency"));
                USSDSalesAggregatorModel totalTransaction = new USSDSalesAggregatorModel(id, dateTimeDay, keyValuesMap.get("profileId"), keyValuesMap.get("senderMSISDN"),keyValuesMap.get("receiverMSISDN"), bucket.getDocCount(), totalAmount.value(),receiverBonusAmount.value(),keyValuesMap.get("currency"));
                totalTransactions.add(totalTransaction);
            }
            log.info("Aggregated into ${totalTransactions.size()} rows.")
            insertAggregation( totalTransactions);
            return parsedComposite.afterKey();

        }
        return null;


    }

    private def insertAggregation(List totalTransactions) {


        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,aggregationDate,profileId,senderMSISDN,receiverMSISDN,quantity,amount,bonus_amount,currency) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), amount = VALUES(amount),bonus_amount=VALUES(bonus_amount)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.aggregationDate.getTime()))
                        ps.setString(++index, row.profileId)
                        ps.setString(++index, row.senderMSISDN)
                        ps.setString(++index, row.receiverMSISDN)
                        ps.setLong(++index, row.quantity)
                        ps.setDouble(++index, row.amount)
                        ps.setDouble(++index, row.bonus_amount)
                        ps.setString(++index, row.currency)

                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}
class USSDSalesAggregatorModel {


    private String id;
    private Date aggregationDate;
    private String profileId;
    private String senderMSISDN;
    private String receiverMSISDN;
    private long quantity;
    private Double amount;
    private Double bonus_amount;
    private String currency;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getAggregationDate() {
        return aggregationDate;
    }

    public void setAggregationDate(Date aggregationDate) {
        this.aggregationDate = aggregationDate;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getSenderMSISDN() {
        return senderMSISDN;
    }

    public void setSenderMSISDN(String senderMSISDN) {
        this.senderMSISDN = senderMSISDN;
    }

    public String getReceiverMSISDN() {
        return receiverMSISDN;
    }

    public void setReceiverMSISDN(String receiverMSISDN) {
        this.receiverMSISDN = receiverMSISDN;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getBonus_amount() {
        return bonus_amount;
    }

    public void setBonus_amount(Double bonus_amount) {
        this.bonus_amount = bonus_amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public USSDSalesAggregatorModel(String id, Date aggregationDate, String profileId, String senderMSISDN, String receiverMSISDN, long quantity, Double amount, Double bonus_amount, String currency) {
        this.id = id;
        this.aggregationDate = aggregationDate;
        this.profileId = profileId;
        this.senderMSISDN = senderMSISDN;
        this.receiverMSISDN = receiverMSISDN;
        this.quantity = quantity;
        this.amount = amount;
        this.bonus_amount = bonus_amount;
        this.currency = currency;
    }
    @Override
    public String toString() {
        return "USSDSalesAggregatorModel{" +
                "id='" + id + '\'' +
                ", aggregationDate=" + aggregationDate +
                ", profileId='" + profileId + '\'' +
                ", senderMSISDN='" + senderMSISDN + '\'' +
                ", receiverMSISDN='" + receiverMSISDN + '\'' +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", bonus_amount=" + bonus_amount +
                ", currency='" + currency + '\'' +
                '}';
    }

}



/*
    private Date aggregationDate;
    private String profileId;
    private String senderMSISDN;
    private String receiverMSISDN;
    private long quantity;
    private Double amount;
    private Double bonus_amount;
    private String currency;

CREATE TABLE `std_ussd_sales_aggregation` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `aggregationDate` date NOT NULL,
  `profileId` varchar(50) DEFAULT NULL,
  `senderMSISDN` varchar(50) DEFAULT NULL,
  `receiverMSISDN` varchar(50) DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `amount` decimal(65,5) DEFAULT NULL,
  `bonus_amount` decimal(65,5) DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `date_senderMSISDN` (`aggregationDate`,`senderMSISDN`),
  KEY `date_receiverMSISDN` (`aggregationDate`,`receiverMSISDN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 */