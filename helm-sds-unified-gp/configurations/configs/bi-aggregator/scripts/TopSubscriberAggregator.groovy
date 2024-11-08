import groovy.util.logging.Slf4j

import java.util.Date;
import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.model.TopSubscribersModel
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

@Slf4j
public class TopSubscriberAggregator extends AbstractAggregator {
    static final def TABLE = "subscribers_aggregation"

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${TopSubscriberAggregator.hourwisedata:true}')
    boolean hourwise;
    @Value('${TopSubscriberAggregator.hour:10}')
    int hours;

    @Value('${TopSubscriberAggregator.profileId:TOPUP}')
    String profileId;

    @Value('${TopSubscriberAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TopSubscriberAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TopSubscriberAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Transactional
    @Scheduled(cron = '${TopSubscriberAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TopSubscriberAggregator Aggregator started********************************************************************************" + new Date());
        def profileIdList = profileId.split(",")
        log.info("profileIdList::"+profileIdList);
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

        log.info("TopSubscriberAggregator Aggregator ended************************************************************************************");
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

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate, Map<String, Object> afterKey,String[] profileIdList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("endTimeDay")
                .field("EndTime").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);

        sources.add(new TermsValuesSourceBuilder("ResellerMSISDN").field("ReceiverMSISDN").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("TransactionProfile").field("TransactionProfile").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("currency").field("Currency").missingBucket(true));


        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TopSubscriberSummary",
                sources).size(10000);

        compositeBuilder
                .subAggregation(AggregationBuilders.sum("requestAmountValue").field("TransactionAmount"))
                .subAggregation(AggregationBuilders.cardinality("uniqueReceiverCount").field("ReceiverMSISDN"))


        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionType",profileIdList))
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionType", profileIdList))

            searchSourceBuilder.query(queryBuilder);
        }
        if(afterKey!=null){
            compositeBuilder.aggregateAfter(afterKey)
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private Map<String, Object> generateResponse(SearchRequest searchRequest) {
        List<TopSubscribersModel> transactions = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("TopSubscriberSummary");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();

                Aggregation totalAmountAggregration = aggregationMap.get("requestAmountValue");
                ParsedSum totalAmount = (ParsedSum) totalAmountAggregration;

                totalAmountAggregration = aggregationMap.get("uniqueReceiverCount");
                ParsedCardinality uniqueReceiverCount = (ParsedCardinality) totalAmountAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("endTimeDay"));
                // Date dateTimeHour = DateFormatter.formatDate(keyValuesMap.get("endTimeHour"));
                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("ResellerMSISDN"));
                TopSubscribersModel totalTransaction = new TopSubscribersModel(id, dateTimeDay, keyValuesMap.get("ResellerMSISDN"),  bucket.getDocCount(),totalAmount.value(),keyValuesMap.get("currency"),keyValuesMap.get("TransactionProfile"));
                transactions.add(totalTransaction);
            }
            log.info("TopSubscriberAggregator Aggregated into ${transactions.size()} rows.")
            insertAggregation( transactions);
            return parsedComposite.afterKey();

        }
        return null;


    }

    private def insertAggregation(List totalTransactions) {
    /*
    	private String id;
    private Date date;
    private String subscriberMSISDN;
    private long count;
    private double amount;
    private String currency;
    private String transactionProfile;
     */

        if (totalTransactions.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,subscriber_msisdn,transaction_profile,count,transaction_amount,currency) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count = VALUES(count), transaction_amount = VALUES(transaction_amount)";
            log.info(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalTransactions[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.date.getTime()))
                        ps.setString(++index, row.subscriberMSISDN)
                        ps.setString(++index, row.transactionProfile)
                        ps.setLong(++index, row.count)
                        ps.setDouble(++index, row.amount)
                        ps.setString(++index,row.currency)
                    },
                    getBatchSize: { totalTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class TopSubscribersModel {
	private String id;
    private Date date;
    private String subscriberMSISDN;
    private long count;
    private double amount;
    private String currency;
    private String transactionProfile;
    
	public TopSubscribersModel(String id, Date date, String subscriberMSISDN, long count, double amount,
			String currency, String transactionProfile) {
		super();
		this.id = id;
		this.date = date;
		this.subscriberMSISDN = subscriberMSISDN;
		this.count = count;
		this.amount = amount;
		this.currency = currency;
		this.transactionProfile = transactionProfile;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getSubscriberMSISDN() {
		return subscriberMSISDN;
	}
	public void setSubscriberMSISDN(String subscriberMSISDN) {
		this.subscriberMSISDN = subscriberMSISDN;
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
	public String getTransactionProfile() {
		return transactionProfile;
	}
	public void setTransactionProfile(String transactionProfile) {
		this.transactionProfile = transactionProfile;
	}
    
    
    
}


/*
CREATE TABLE `subscribers_aggregation` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `subscriber_msisdn` varchar(50) DEFAULT NULL,
  `transaction_date` date DEFAULT NULL,
  `transaction_profile` varchar(50) DEFAULT NULL,
  `currency` varchar(50) DEFAULT NULL,
  `transaction_amount` decimal(20,5) DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 */