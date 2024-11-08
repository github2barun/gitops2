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
public class TotalRejectActivationAggregator extends AbstractAggregator {

    static final def TABLE = "distributor_wise_weekly_sales_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TotalRejectActivationAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TotalRejectActivationAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TotalRejectActivationAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${TotalRejectActivationAggregator.eventName:REJECT_KYC}')
    String eventName

    @Value('${indexPattern:data_lake_}')
    String indexPattern

    @Transactional
    @Scheduled(cron = '${TotalRejectActivationAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TotalRejectActivationAggregator Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    index = index.replace("*","").trim();
                    List<TotalRejectActivationAggregatorModel> totalRejectActivationAggregatorModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventName)

                    insertAggregation(totalRejectActivationAggregatorModelList);
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
                List<TotalRejectActivationAggregatorModel> totalRejectActivationAggregatorModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventName);
                insertAggregation(totalRejectActivationAggregatorModelList);
            }
        }
        log.info("TotalRejectActivationAggregator Aggregator ended**************************************************************************");
    }

    private List<TotalRejectActivationAggregatorModel> aggregateDataES(String index, String fromDate, String toDate,  String eventName) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventName);
        searchRequest.source(searchSourceBuilder);
        List<TotalRejectActivationAggregatorModel> totalRejectActivationAggregatorModelList = generateResponse(searchRequest, index);
        return totalRejectActivationAggregatorModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String eventName) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(new TermsValuesSourceBuilder("distributor").field("kyc.parentId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("brand").field("kyc.brandName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("region").field("kyc.dms.reseller.region.name.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalRejectActivationAggregator",
                sources).size(10000);

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        }
        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<TotalRejectActivationAggregatorModel> generateResponse(SearchRequest searchRequest, String index) {
        List<TotalRejectActivationAggregatorModel> totalRejectActivationAggregatorModelList = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalRejectActivationAggregator");

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
                TotalRejectActivationAggregatorModel totalRejectActivationAggregatorModel = new TotalRejectActivationAggregatorModel(
                        id, keyValuesMap.get("PosId"), Integer.parseInt(yearAndWeek[0]), Integer.parseInt(yearAndWeek[1]), keyValuesMap.get("distributor"),
                        keyValuesMap.get("region"), keyValuesMap.get("brand"), bucket.getDocCount());
                totalRejectActivationAggregatorModelList.add(totalRejectActivationAggregatorModel);
            }
        }
        return totalRejectActivationAggregatorModelList;
    }

    private def insertAggregation(List totalRejectActivationAggregatorModelList) {

        log.info("TotalRejectActivationAggregator Aggregated into ${totalRejectActivationAggregatorModelList.size()} rows.")
        if (totalRejectActivationAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,year,weekNumber,dist_id,region,brand,activation_rejection_count)" +
                    " VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE activation_rejection_count = VALUES(activation_rejection_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalRejectActivationAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setInt(++index, row.year)
                        ps.setInt(++index, row.weekNumber)
                        ps.setString(++index, row.distributor)
                        ps.setString(++index,row.region)
                        ps.setString(++index, row.brand)
                        ps.setLong(++index, row.ttlRejectActivationCount)
                    },
                    getBatchSize: { totalRejectActivationAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }

}

class TotalRejectActivationAggregatorModel {
    private String id;
    private int year;
    private int weekNumber;
    private String distributor;
    private String region;
    private String brand;
    private long ttlRejectActivationCount;
    private String posId;

    TotalRejectActivationAggregatorModel(String id, String posId, int year, int weekNumber, String distributor,
                                          String region, String brand, long ttlRejectActivationCount) {
        this.id = id
        this.year = year
        this.posId=posId;
        this.weekNumber = weekNumber
        this.distributor = distributor
        this.region = region
        this.brand = brand
        this.ttlRejectActivationCount = ttlRejectActivationCount
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

    long getTtlRejectActivationCount() {
        return ttlRejectActivationCount
    }

    void setTtlRejectActivationCount(long ttlRejectActivationCount) {
        this.ttlRejectActivationCount = ttlRejectActivationCount
    }

    void setPosId(String posId) {
        this.posId = posId;
    }

    String getPosId() {
        return posId;
    }
}