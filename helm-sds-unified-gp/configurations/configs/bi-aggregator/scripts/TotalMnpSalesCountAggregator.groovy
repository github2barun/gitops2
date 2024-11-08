import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
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
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

@Slf4j
public class TotalMnpSalesCountAggregator extends AbstractAggregator {

    static final def TABLE = "distributor_wise_weekly_sales_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TotalMnpSalesCountAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TotalMnpSalesCountAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TotalMnpSalesCountAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;


    @Value('${TotalMnpSalesCountAggregator.simType:mnp}')
    String simType

    @Value('${TotalMnpSalesCountAggregator.eventName:ADD_KYC}')
    String eventName

    @Value('${indexPattern:data_lake_}')
    String indexPattern

    @Transactional
    @Scheduled(cron = '${TotalMnpSalesCountAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TotalMnpSalesCountAggregator Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    index = index.replace("*","").trim();
                    List<TotalMnpSalesCountAggregatorModel> totalMnpSalesCountAggregatorModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventName, simType)

                    insertAggregation(totalMnpSalesCountAggregatorModelList);
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
                List<TotalMnpSalesCountAggregatorModel> totalMnpSalesCountAggregatorModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventName, simType);
                insertAggregation(totalMnpSalesCountAggregatorModelList);
            }
        }
        log.info("TotalMnpSalesCountAggregator Aggregator ended**************************************************************************");
    }

    private List<TotalMnpSalesCountAggregatorModel> aggregateDataES(String index, String fromDate, String toDate, String eventName, String simType) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventName, simType);
        searchRequest.source(searchSourceBuilder);
        List<TotalMnpSalesCountAggregatorModel> totalMnpSalesCountAggregatorModelList = generateResponse(searchRequest, index);
        return totalMnpSalesCountAggregatorModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String eventName, simType) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(new TermsValuesSourceBuilder("distributor").field("kyc.parentId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("brand").field("kyc.brandName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("region").field("kyc.dms.reseller.region.name.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalMnpSalesCountAggregator",
                sources).size(10000);

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("kyc.simType.keyword",simType))
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("kyc.simType.keyword",simType))
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<TotalMnpSalesCountAggregatorModel> generateResponse(SearchRequest searchRequest, String index) {
        List<TotalMnpSalesCountAggregatorModel> totalMnpSalesCountAggregatorModelList = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalMnpSalesCountAggregator");

            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                if(keyValuesMap.get("region") == null) {
                    keyValuesMap.put("region", "N/A");
                }
                if(keyValuesMap.get("distributor") == null) {
                    keyValuesMap.put("distributor", "N/A");
                }
                String[] yearAndWeek = DateUtil.getIndexWorkWeekAndYear(index, indexPattern);
                String id = GenerateHash.createHashString(yearAndWeek[0], yearAndWeek[1], keyValuesMap.get("brand"), keyValuesMap.get("distributor"), keyValuesMap.get("region"));
                TotalMnpSalesCountAggregatorModel totalMnpSalesCountAggregatorModel = new TotalMnpSalesCountAggregatorModel(
                        id, keyValuesMap.get("PosId"), Integer.parseInt(yearAndWeek[0]), Integer.parseInt(yearAndWeek[1]), keyValuesMap.get("distributor"),
                        keyValuesMap.get("region"), keyValuesMap.get("brand"), bucket.getDocCount());
                totalMnpSalesCountAggregatorModelList.add(totalMnpSalesCountAggregatorModel);
            }
        }
        return totalMnpSalesCountAggregatorModelList;
    }

    private def insertAggregation(List totalMnpSalesCountAggregatorModelList) {

        log.info("TotalMnpSalesCountAggregator Aggregated into ${totalMnpSalesCountAggregatorModelList.size()} rows.")
        if (totalMnpSalesCountAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,year,weekNumber,dist_id,region,brand,mnp_sales_count)" +
                    " VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE mnp_sales_count = VALUES(mnp_sales_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalMnpSalesCountAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setInt(++index, row.year)
                        ps.setInt(++index, row.weekNumber)
                        ps.setString(++index, row.distributor)
                        ps.setString(++index,row.region)
                        ps.setString(++index, row.brand)
                        ps.setLong(++index, row.ttlMnpSalesCount)
                    },
                    getBatchSize: { totalMnpSalesCountAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }

}

class TotalMnpSalesCountAggregatorModel {
    private String id;
    private int year;
    private int weekNumber;
    private String distributor;
    private String region;
    private String brand;
    private long ttlMnpSalesCount;
    private String posId;

    TotalMnpSalesCountAggregatorModel(String id, String posId, int year, int weekNumber, String distributor,
                                          String region, String brand, long ttlMnpSalesCount) {
        this.id = id
        this.year = year
        this.posId=posId;
        this.weekNumber = weekNumber
        this.distributor = distributor
        this.region = region
        this.brand = brand
        this.ttlMnpSalesCount = ttlMnpSalesCount
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }

    int getWeekNumber() {
        return weekNumber
    }

    void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber
    }

    String getDistributor() {
        return distributor
    }

    void setDistributor(String distributor) {
        this.distributor = distributor
    }

    String getRegion() {
        return region
    }

    void setRegion(String region) {
        this.region = region
    }

    String getBrand() {
        return brand
    }

    void setBrand(String brand) {
        this.brand = brand
    }

    void setPosId(String posId) {
        this.posId = posId;
    }

    String getPosId() {
        return posId;
    }

    long getTtlMnpSalesCount() {
        return ttlMnpSalesCount
    }

    void setTtlMnpSalesCount(long ttlMnpSalesCount) {
        this.ttlMnpSalesCount = ttlMnpSalesCount
    }
}