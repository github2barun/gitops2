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
import org.elasticsearch.search.aggregations.Aggregation
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

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class TotalKycSales extends AbstractAggregator {
    static final def TABLE = "total_kyc_sales"

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";

    @Autowired
    protected JdbcTemplate jdbcTemplate;


    // @Value('${ChannelWiseDayWise.profileId:CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP}')
    // String profileId;

    @Value('${TotalKycSales.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TotalKycSales.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TotalKycSales.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${TotalKycSales.eventName:ADD_KYC}')
    String eventName

    @Value('${TotalKycSales.brands:TT,TARAJI}')
    String brands;

    @Value('${TotalKycSales.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TotalKycSales.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TotalKycSales Aggregator started***************************************************************************" + new Date());
        log.info("even name: " + eventName);
        // def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);  //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TotalKycSalesModel> totalKycSalesModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventName)
                    insertAggregation(totalKycSalesModelList);
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
                List<TotalKycSalesModel> totalKycSalesModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventName);
                insertAggregation(totalKycSalesModelList);
            }
        }

        log.info("TotalKycSales Aggregator ended**************************************************************************");
    }


    private List<TotalKycSalesModel> aggregateDataES(String index, String fromDate, String toDate, String eventName) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventName);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<TotalKycSalesModel> totalKycSalesModelList= generateResponse(searchResponse);
        String scrollId =  searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::"+searchResponse.getHits().size())
        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(5));
            searchResponse = generateScrollSearchResponse(scrollRequest);
            log.info("_________________hits size inside loop _____________________"+searchResponse.getHits().size())
            totalKycSalesModelList.addAll(generateResponse(searchResponse));
            scrollId = searchResponse.getScrollId();
        }

        return totalKycSalesModelList;
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
    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String eventName) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("timestamp")
                .field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("PosId").field("user.userId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("brand").field("kyc.brandName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("brandCode").field("kyc.brandCode.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("brandPrefix").field("kyc.brandPrefix.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("region").field("kyc.region.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalKycSales",
                sources).size(scrollSize);

        //compositeBuilder.subAggregation(AggregationBuilders.sum("RequestAmountValue").field("TransactionAmount"))

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<TotalKycSalesModel> generateResponse(SearchResponse searchResponse) {
        List<TotalKycSalesModel> totalKycSalesModelList = new ArrayList<>();
        Map<String, Set<String>> posIdBrandMap = new HashMap<>();

        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalKycSales");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                Set<String> brands = new ArrayList<>();
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                //Aggregation totalAmountAggregration = aggregationMap.get("RequestAmountValue");
                //ParsedSum p = (ParsedSum) totalAmountAggregration;
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("timestamp"));
                Calendar calender = Calendar.getInstance();
                calender.setTime(dateTimeDay);

                if (posIdBrandMap.containsKey(keyValuesMap.get("PosId")))
                    posIdBrandMap.get(keyValuesMap.get("PosId")).add(keyValuesMap.get("brand"));
                else {
                    brands.add(keyValuesMap.get("brand"));
                    posIdBrandMap.put(keyValuesMap.get("PosId"), brands);
                }
                String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("PosId"), keyValuesMap.get("brand"));
                TotalKycSalesModel totalKycSalesModel = new TotalKycSalesModel(id, dateTimeDay, keyValuesMap.get("PosId"), calender.get(calender.WEEK_OF_YEAR), calender.get(calender.YEAR), keyValuesMap.get("brand"), keyValuesMap.get("brandCode"), keyValuesMap.get("brandPrefix"), bucket.getDocCount());

                totalKycSalesModelList.add(totalKycSalesModel);
            }
            List<String> allBrandList = Arrays.asList(brands.split(','));
            List<TotalKycSalesModel> modelList = new ArrayList<>();
            for (TotalKycSalesModel totalKycSalesModel : totalKycSalesModelList) {
                String posId = totalKycSalesModel.getPosId()
                Set<String> brandSet = posIdBrandMap.get(posId);
                for (String brand : allBrandList) {
                    if (!(brandSet.contains(brand))) {
                        String id = GenerateHash.createHashString("", posId, brand);
                        TotalKycSalesModel model = new TotalKycSalesModel(id, totalKycSalesModel.getEndTimeDay(), posId, totalKycSalesModel.getWeekNumber(), totalKycSalesModel.getYear(), brand, totalKycSalesModel.getBrandCode(), totalKycSalesModel.getBrandPrefix(), 0);
                        modelList.add(model);
                    }
                }
            }
            totalKycSalesModelList.addAll(modelList);
        }

        return totalKycSalesModelList;

    }

    private def insertAggregation(List totalKycSalesModelList) {

        log.info("TotalKycSales Aggregated into ${totalKycSalesModelList.size()} rows.")
        if (totalKycSalesModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,end_time_day,posId,weekNumber,year,brand,brandCode,brandPrefix,count) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count = VALUES(count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalKycSalesModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.endTimeDay.getTime()))
                        //ps.setTimestamp(++index, new java.sql.Timestamp(row.endTimeHour.getTime()))
                        ps.setString(++index, row.posId)
                        ps.setInt(++index, row.weekNumber)
                        ps.setInt(++index, row.year)
                        ps.setString(++index, row.brand)
                        ps.setString(++index, row.brandCode)
                        ps.setString(++index, row.brandPrefix)
                        ps.setLong(++index, row.count)

                    },
                    getBatchSize: { totalKycSalesModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}
class TotalKycSalesModel {
    private String id;
    private Date endTimeDay;
    private String posId;
    private int weekNumber;
    private int year;
    private String brand;
    private String brandCode;
    private String brandPrefix;
    private long count;


    public TotalKycSalesModel(String id, Date endTimeDay, String posId, int weekNumber, int year, String brand, String brandCode, String brandPrefix, Long count) {
        this.id = id;
        this.endTimeDay = endTimeDay;
        this.posId=posId;
        this.brand=brand;
        this.count = count;
        this.weekNumber = weekNumber;
        this.year = year;
        this.brandCode = brandCode;
        this.brandPrefix = brandPrefix;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPosId() {
        return posId;
    }

    int getWeekNumber() {
        return weekNumber
    }

    void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }

    String getBrandCode() {
        return brandCode
    }

    void setBrandCode(String brandCode) {
        this.brandCode = brandCode
    }

    String getBrandPrefix() {
        return brandPrefix
    }

    void setBrandPrefix(String brandPrefix) {
        this.brandPrefix = brandPrefix
    }

    public void setPosId(String posId) {
        this.posId = posId;
    }

    public Date getEndTimeDay() {
        return endTimeDay;
    }

    public void setEndTimeDay(Date endTimeDay) {
        this.endTimeDay = endTimeDay;
    }


    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }


    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }


}