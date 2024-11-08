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
import org.elasticsearch.script.Script
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.*
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
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
public class TotalTransactionSummaryReport extends AbstractAggregator {
    static final def TABLE = "total_transaction_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TotalTransactionSummaryReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TotalTransactionSummaryReport.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TotalTransactionSummaryReport.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${TotalTransactionSummaryReport.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${TotalTransactionSummaryReport.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TotalTransactionSummaryReport.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TotalTransactionSummaryReport Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);  //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TotalTransactionSummaryModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(transactionSummaryModels);
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
                List<TotalTransactionSummaryModel> transactionSummaryModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionSummaryModels);
            }
        }

        log.info("TotalTransactionSummaryReport Aggregator ended**************************************************************************");
    }


    private List<TotalTransactionSummaryModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<TotalTransactionSummaryModel> transactionSummaryModels = generateResponse(searchResponse);
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

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("timestamp")
                .field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("dealerMSISDN").field("oms.seller.MSISDN.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("dealerType").field("user.resellerPath.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("dealerId").script(new Script("if(doc['oms.orderType.keyword'].value == 'ISO_ST') {return doc['oms.sender.id.keyword'].value} else if(doc['oms.orderType.keyword'].value == 'ISO') {return doc['oms.seller.id.keyword'].value} return 'N/A'")).missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("area").field("dms.area.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("section").field("dms.section.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("city").field("dms.city.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("district").field("dms.district.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("productSku").field("oms.items.productSku.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalTransactionSummary",
                sources).size(scrollSize);

        compositeBuilder.subAggregation(AggregationBuilders.sum("sumOfTransactionAmounts").field("oms.transactionAmount"))

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("componentName.keyword","oms"))
                    .filter(QueryBuilders.termsQuery("oms.resultcode",0))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte("now"+timeOffset+"-3h/d").lt("now"+timeOffset+"+1h/d").includeLower(true).includeUpper(true))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("componentName.keyword","oms"))
                    .filter(QueryBuilders.termsQuery("oms.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<TotalTransactionSummaryModel> generateResponse(SearchResponse searchResponse){
        List<TotalTransactionSummaryModel> transactionSummaryModelList = new ArrayList<>();
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalTransactionSummary");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("timestamp"));
                Calendar calender = Calendar.getInstance();
                calender.setTime(dateTimeDay);

                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("productSku"), keyValuesMap.get("dealerType"),keyValuesMap.get("dealerId"),keyValuesMap.get("dealerMSISDN"),
                        keyValuesMap.get("district"), keyValuesMap.get("area"), keyValuesMap.get("section"), keyValuesMap.get("city"));
                TotalTransactionSummaryModel transactionSummaryModel = new TotalTransactionSummaryModel(id, dateTimeDay, keyValuesMap.get("dealerType"),keyValuesMap.get("dealerId"),keyValuesMap.get("dealerMSISDN"),bucket.getDocCount(),
                        keyValuesMap.get("district"), keyValuesMap.get("area"), keyValuesMap.get("section"), keyValuesMap.get("city"), bucket.getAggregations().get("sumOfTransactionAmounts").getAt("value") as Float, "");

                transactionSummaryModelList.add(transactionSummaryModel);
            }
        }

        return transactionSummaryModelList;

    }

    private def insertAggregation(List transactionSummaryModelList) {

        log.info("TransactionSummaryReport Aggregated into ${transactionSummaryModelList.size()} rows.")
        if (transactionSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,dealer_type,dealer_id,dealer_msisdn,transaction_type,district,area,section," +
                    "city,transaction_count,sum) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE sum = VALUES(sum), transaction_count = VALUES(transaction_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = transactionSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.dealerType)
                        ps.setString(++index,row.dealerId)
                        ps.setString(++index,row.dealerMSISDN)
                        ps.setString(++index, row.transactionType)
                        ps.setString(++index, row.district)
                        ps.setString(++index, row.area)
                        ps.setString(++index,row.section)
                        ps.setString(++index, row.city)
                        ps.setLong(++index, row.transactionCount)
                        ps.setBigDecimal(++index, row.sum)

                    },
                    getBatchSize: { transactionSummaryModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}
class TotalTransactionSummaryModel
{
    private String id;
    private Date transactionDate;
    private String transactionType;
    private String dealerType;
    private String dealerId;
    private String dealerMSISDN;
    private long transactionCount;
    private String district;
    private String area;
    private String section;
    private String city;
    private Float sum;


    public TotalTransactionSummaryModel(String id, Date transactionDate, String dealerType, String dealerId, String dealerMSISDN, long transactionCount,
                                        String district, String area, String section, String city, Float sum, String transactionType) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.dealerType=dealerType;
        this.dealerId=dealerId;
        this.dealerMSISDN=dealerMSISDN;
        this.area=area;
        this.sum = sum;
        this.transactionCount = transactionCount;
        this.district = district;
        this.section = section;
        this.city = city;
        this.transactionType = transactionType;
    }

    String getId()
    {
        return id
    }

    void setId(String id)
    {
        this.id = id
    }

    Date getTransactionDate()
    {
        return transactionDate
    }

    void setTransactionDate(Date transactionDate)
    {
        this.transactionDate = transactionDate
    }

    String getTransactionType()
    {
        return transactionType
    }

    void setTransactionType(String transactionType)
    {
        this.transactionType = transactionType
    }

    String getDealerType()
    {
        return dealerType
    }

    void setDealerType(String dealerType)
    {
        this.dealerType = dealerType
    }

    String getDealerId()
    {
        return dealerId
    }

    void setDealerId(String dealerId)
    {
        this.dealerId = dealerId
    }

    String getDealerMSISDN()
    {
        return dealerMSISDN
    }

    void setDealerMSISDN(String dealerMSISDN)
    {
        this.dealerMSISDN = dealerMSISDN
    }

    long getTransactionCount()
    {
        return transactionCount
    }

    void setTransactionCount(long transactionCount)
    {
        this.transactionCount = transactionCount
    }

    String getDistrict()
    {
        return district
    }

    void setDistrict(String district)
    {
        this.district = district
    }

    String getArea()
    {
        return area
    }

    void setArea(String area)
    {
        this.area = area
    }

    String getSection()
    {
        return section
    }

    void setSection(String section)
    {
        this.section = section
    }

    String getCity()
    {
        return city
    }

    void setCity(String city)
    {
        this.city = city
    }

    Float getSum()
    {
        return sum
    }

    void setSum(Float sum)
    {
        this.sum = sum
    }
}