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
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

@Slf4j
//@DynamicMixin
public class HourlyCdrUsageReport extends AbstractAggregator {

    static final def TABLE = "hourly_cdr_usage_statistics_aggregation"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${HourlyCdrUsageReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${HourlyCdrUsageReport.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${HourlyCdrUsageReport.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${HourlyCdrUsageReport.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;
    @Value('${HourlyCdrUsageReport.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${HourlyCdrUsageReport.cron:*/3 * * * * ?}')

    public void aggregate() {
        log.info("********** HourlyCdrUsageReport Aggregator started at " + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<HourlyCdrUsageStatisticsAggregatorModel> hourlyCdrUsageStatisticsAggregatorModel = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(hourlyCdrUsageStatisticsAggregatorModel);
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
                List<HourlyCdrUsageStatisticsAggregatorModel> hourlyCdrUsageStatisticsAggregatorModel = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(hourlyCdrUsageStatisticsAggregatorModel);
            }
        }
        log.info("********** HourlyCdrUsageReport Aggregator ended at " + new Date());
    }

    private List<HourlyCdrUsageStatisticsAggregatorModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        SearchResponse searchResponse = generateSearchResponse(searchRequest);
        List<HourlyCdrUsageStatisticsAggregatorModel> hourlyCdrUsageStatisticsAggregatorModel = generateResponse(searchResponse);
        String scrollId =  searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::"+searchResponse.getHits().size())
        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(5));
            searchResponse = generateScrollSearchResponse(scrollRequest);
            log.info("_________________hits size inside loop _____________________"+searchResponse.getHits().size())
            hourlyCdrUsageStatisticsAggregatorModel.addAll(generateResponse(searchResponse));
            scrollId = searchResponse.getScrollId();
        }

        return hourlyCdrUsageStatisticsAggregatorModel;
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
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
                .filter(QueryBuilders.termsQuery("resultCode", 0));

        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }

        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<HourlyCdrUsageStatisticsAggregatorModel> generateResponse(SearchResponse searchResponse) {
        List<HourlyCdrUsageStatisticsAggregatorModel> hourlyCdrUsageList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {
            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, HourlyCdrUsageStatisticsAggregatorModel> hourlyCdrUsageMap = new HashMap<>();
            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        String dealerMsisdn = searchHitMap.getOrDefault("oms.sender.msisdn", "N/A");
                        String dateOnly = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd") as String;
                        String hourOnly = (DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("HH") as String) + ":00:00";

                        Map<String, Object> senderAddlField = searchHitMap.getOrDefault("oms.sender.additionalFields", null);
                        String section = "N/A";
                        String city_province = "N/A";
                        String district = "N/A";
                        String dealerId = "N/A";
                        String dealerEposTerminalId = "N/A"
                        if (senderAddlField != null) {
                            section = (senderAddlField.getOrDefault("section", "N/A") as String);
                            city_province = (senderAddlField.getOrDefault("city_province", "N/A") as String);
                            district = (senderAddlField.getOrDefault("district_sum", "N/A") as String);
                            dealerId = (senderAddlField.getOrDefault("dealer_code", "N/A") as String);
                            dealerEposTerminalId = senderAddlField.getOrDefault("epos_terminal_id", "N/A");
                        }

                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        if (!omsItems.isEmpty()) {
                            for (HashMap<String, String> omsItem : omsItems) {

                                HashMap<String, String> data = omsItem.getOrDefault("data", null);
                                if (data != null && data.getOrDefault("resultCode", null) != null) {

                                    Integer internalResultCode = Integer.valueOf(data.get("resultCode"));
                                    if (internalResultCode == 0) {
                                        String dealerType = data.getOrDefault("senderResellerType", "N/A");
                                        String id = GenerateHash.createHashString(
                                                dealerType,
                                                dealerMsisdn,
                                                dealerId,
                                                dateOnly,
                                                hourOnly,
                                                dealerEposTerminalId
                                        );

                                        if (hourlyCdrUsageMap.containsKey(id)) {
                                            HourlyCdrUsageStatisticsAggregatorModel hourlyCdrUsageStatisticsAggregatorModel = hourlyCdrUsageMap.get(id);
                                            hourlyCdrUsageStatisticsAggregatorModel.setCount(hourlyCdrUsageStatisticsAggregatorModel.getCount() + 1);
                                            hourlyCdrUsageMap.put(id, hourlyCdrUsageStatisticsAggregatorModel);
                                        } else {
                                            def dateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateOnly);
                                            HourlyCdrUsageStatisticsAggregatorModel hourlyCdrUsageStatisticsAggregatorModel = new HourlyCdrUsageStatisticsAggregatorModel(id, dateObj, hourOnly, dealerId.equals("") ? "N/A" : dealerId, dealerType.equals("") ? "N/A" : dealerType, dealerMsisdn.equals("") ? "N/A" : dealerMsisdn, city_province.equals("") ? "N/A" : city_province, district.equals("") ? "N/A" : district, section.equals("") ? "N/A" : section, 1, dealerEposTerminalId.equals("") ? "N/A" : dealerEposTerminalId);
                                            hourlyCdrUsageMap.put(id, hourlyCdrUsageStatisticsAggregatorModel);
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
            hourlyCdrUsageMap.each {
                entry -> hourlyCdrUsageList.add(entry.value)
            }
        }
        return hourlyCdrUsageList;
    }

    private def insertAggregation(List hourlyCdrUsageStatisticsAggregatorModelList) {
        log.info("HourlyCdrUsageReport Aggregated into ${hourlyCdrUsageStatisticsAggregatorModelList.size()} rows.")
        if (hourlyCdrUsageStatisticsAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,date,hour,dealer_id,dealer_type,dealer_msisdn,dealer_city,dealer_district,section,cdr_usage_count,dealer_epos_terminal_id) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE cdr_usage_count = VALUES(cdr_usage_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = hourlyCdrUsageStatisticsAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.date.getTime()))
                        ps.setString(++index, row.dateHour)
                        ps.setString(++index, row.dealerId)
                        ps.setString(++index, row.dealerType)
                        ps.setString(++index, row.dealerMsisdn)
                        ps.setString(++index, row.dealerCity)
                        ps.setString(++index, row.dealerDistrict)
                        ps.setString(++index, row.section)
                        ps.setLong(++index, row.count)
                        ps.setString(++index, row.dealerEposterminalId)
                    },
                    getBatchSize: { hourlyCdrUsageStatisticsAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class HourlyCdrUsageStatisticsAggregatorModel {
    private String id;
    private Date date;
    private String dateHour;
    private String dealerId;
    private String dealerType;
    private String dealerMsisdn;
    private String dealerCity;
    private String dealerDistrict;
    private String section;
    private long count;
    private String dealerEposterminalId;

    HourlyCdrUsageStatisticsAggregatorModel(String id, Date date, String dateHour, String dealerId, String dealerType, String dealerMsisdn, String dealerCity, String dealerDistrict, String section, long count, String dealerEposterminalId) {
        this.id = id
        this.date = date
        this.dateHour = dateHour
        this.dealerId = dealerId
        this.dealerType = dealerType
        this.dealerMsisdn = dealerMsisdn
        this.dealerCity = dealerCity
        this.dealerDistrict = dealerDistrict
        this.section = section
        this.count = count
        this.dealerEposterminalId = dealerEposterminalId
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getDate() {
        return date
    }

    void setDate(Date date) {
        this.date = date
    }

    String getDateHour() {
        return dateHour
    }

    void setDateHour(String dateHour) {
        this.dateHour = dateHour
    }

    String getDealerId() {
        return dealerId
    }

    void setDealerId(String dealerId) {
        this.dealerId = dealerId
    }

    String getDealerType() {
        return dealerType
    }

    void setDealerType(String dealerType) {
        this.dealerType = dealerType
    }

    String getDealerMsisdn() {
        return dealerMsisdn
    }

    void setDealerMsisdn(String dealerMsisdn) {
        this.dealerMsisdn = dealerMsisdn
    }

    String getDealerCity() {
        return dealerCity
    }

    void setDealerCity(String dealerCity) {
        this.dealerCity = dealerCity
    }

    String getDealerDistrict() {
        return dealerDistrict
    }

    void setDealerDistrict(String dealerDistrict) {
        this.dealerDistrict = dealerDistrict
    }

    String getSection() {
        return section
    }

    void setSection(String section) {
        this.section = section
    }

    long getCount() {
        return count
    }

    void setCount(long count) {
        this.count = count
    }

    String getDealerEposTerminalId() {
        return dealerEposterminalId
    }

    void setDealerEposTerminalId(String dealerEposterminalId) {
        this.dealerEposterminalId = dealerEposterminalId
    }
}