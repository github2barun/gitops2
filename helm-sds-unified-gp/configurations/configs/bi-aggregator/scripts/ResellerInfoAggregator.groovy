package com.seamless.customer.bi.aggregator.aggregate
import groovy.util.logging.Slf4j
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
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
import org.elasticsearch.search.sort.SortOrder
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
class ResellerInfoAggregator extends ScrollableAbstractAggregator {
    static final def DATABASE = "scc_data_aggregator"
    static final def TABLE = "reseller_info"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value('${ResellerInfoAggregator.scrollSize:7000}')
    int scrollSize;

    @Value('${ResellerInfoAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ResellerInfoAggregator.bulkInsertionModeFromDateString:2022-08-01}')
    String bulkInsertionModeFromDateString;

    @Value('${ResellerInfoAggregator.bulkInsertionModeToDateString:2022-08-17}')
    String bulkInsertionModeToDateString;

    @Transactional
    @Scheduled(cron = '${ResellerInfoAggregator.cron:*/2 * * * * ?}')
    void aggregate() {
        log.info("ResellerInfoAggregator Aggregator started**************************************************************** at " + new Date());
        String index = "reseller_data_lake"
        aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
        log.info("ResellerInfoAggregator Aggregator ended**************************************************************************");
    }

    void aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));
        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateResponse(searchRequest);
        if (searchResponse != null) {
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size());
            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(1));
                try {
                    searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                } catch (Exception e) {
                    log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
                }
                scrollId = generateScrollResponse(searchResponse);
            }
        }
    }

    SearchSourceBuilder buildESQuery(String fromDate, String toDate) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(scrollSize);

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("resultCode.keyword", 0))
                    .filter(QueryBuilders.termsQuery("dms.resultCode", 0))
                    .filter(QueryBuilders.rangeQuery("@timestamp").gte("now-60m").lt("now"))
            searchSourceBuilder.sort("@timestamp", SortOrder.ASC).query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("resultCode.keyword", 0))
                    .filter(QueryBuilders.termsQuery("dms.resultCode", 0))
                    .filter(QueryBuilders.rangeQuery("@timestamp").gte(fromDate).lt(toDate))
            searchSourceBuilder.sort("@timestamp", SortOrder.ASC).query(queryBuilder);
        }
        return searchSourceBuilder;
    }

    SearchResponse generateResponse(SearchRequest searchRequest) {
        List<ResellerInfo> resellers = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Could not perform search on elasticsearch. Error message: " + e);
        }
        log.info("*******Search Request*******" + searchRequest.toString())
        RestStatus status = searchResponse.status();

        if (searchResponse != null) {
            log.info("response status -------------" + status);

            if (status == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    List<ResellerInfo> resellerList = fetchReseller(searchHit);
                    if (resellerList.size() > 0) resellers.addAll(resellerList)
                }
                log.info("loop finish******************");
            }
            insertAggregation(resellers);
            log.info("inserted first time in table");

        } else {
            log.info("No response found")
        }
        return searchResponse;

    }

    List<ResellerInfo> fetchReseller(SearchHit searchHit) {
        List<ResellerInfo> resellerInfoList = new ArrayList<>();
        Map<String, Object> searchHitMap = searchHit.getSourceAsMap();

        try{
            if(searchHitMap.get("dms.resellerInfo.reseller.resellerId")!=null){
                HashMap<String, String> address = searchHitMap.get("dms.resellerInfo.reseller.address");
                ArrayList<Map<String, String>> additionalFields = searchHitMap.get("dms.resellerInfo.additionalFields");
                String additionalFieldsData="";
                if(additionalFields != null){
                    if (additionalFields.size() > 0) {
                        additionalFieldsData = additionalFieldsData.concat("{ ");
                        int size = 1;
                        for (Map<String, String> field : additionalFields) {
                            additionalFieldsData = additionalFieldsData.concat("\"" + field.get("name") + "\" : \"" + field.get("value") + "\"");
                            if (size < additionalFields.size())
                                additionalFieldsData = additionalFieldsData.concat(",");
                            size++;
                        }
                        additionalFieldsData = additionalFieldsData.concat(",");
                        additionalFieldsData = additionalFieldsData.concat("\"" + "trackerStatus" + "\" : \"" + searchHitMap.get("dms.resellerInfo.reseller.status") + "\"");
                        additionalFieldsData = additionalFieldsData.concat(" }");
                    }
                }
                String id = GenerateHash.createHashString(searchHitMap.get("dms.resellerInfo.reseller.resellerId").toString());
                ResellerInfo reseller = new ResellerInfo(id,
                        searchHitMap.get("dms.resellerInfo.reseller.resellerId").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.resellerName").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.resellerPath").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.status").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.parentResellerName").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.resellerTypeId").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.region").toString(),
                        address.get("zip"),
                        address.get("city"),
                        address.get("country"),
                        address.get("email"),
                        searchHitMap.get("dms.resellerInfo.reseller.resellerMSISDN").toString(),
                        searchHitMap.get("dms.resellerInfo.reseller.resellerJuridicalName").toString(),
                        additionalFieldsData
                );
                resellerInfoList.add(reseller);
            }
        }
        catch (Exception e)
        {
            log.error("Exception "+e);
            return null;
        }
        return resellerInfoList;
    }

    String generateScrollResponse(SearchResponse searchScrollResponse) {
        List<ResellerInfo> resellers = new ArrayList<>();
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchScrollResponse.getHits();
            log.info("no of hits after 1st request: " + searchHits.size());
            for (SearchHit searchHit : searchHits.getHits()) {
                List<ResellerInfo> resellerList = fetchReseller(searchHit);
                if (resellerList.size() > 0) resellers.addAll(resellerList)
            }
        }
        insertAggregation(resellers);
        log.info("inserting records subsequent time in table, if any");


        return searchScrollResponse.getScrollId();
    }

    def insertAggregation(List resellers) {
        if (resellers.size() != 0) {

            def sql = """INSERT INTO ${DATABASE}.${TABLE}
            (id,reseller_id,reseller_name,reseller_path,reseller_status,reseller_parent,reseller_type_id,region,zip,city,country,email,MSISDN,reseller_juridical_name,extra_params)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
            reseller_name=VALUES(reseller_name), reseller_path=VALUES(reseller_path), reseller_status=VALUES(reseller_status),
            reseller_parent=VALUES(reseller_parent), reseller_type_id=VALUES(reseller_type_id), region=VALUES(region),
            zip=VALUES(zip), city=VALUES(city), country=VALUES(country), email=VALUES(email), MSISDN=VALUES(MSISDN), reseller_juridical_name=VALUES(reseller_juridical_name), extra_params=VALUES(extra_params)"""

            jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = resellers[i]
                        //def index = 0
                        ps.setString(1, row.id)
                        ps.setString(2, row.resellerId)
                        ps.setString(3, row.resellerName)
                        ps.setString(4, row.resellerPath)
                        ps.setString(5, row.resellerStatus)
                        ps.setString(6, row.resellerParentId)
                        ps.setString(7, row.resellerTypeId)
                        ps.setString(8, row.region)
                        ps.setString(9, row.zip)
                        ps.setString(10, row.city)
                        ps.setString(11, row.country)
                        ps.setString(12, row.email)
                        ps.setString(13, row.resellerMSISDN)
                        ps.setString(14, row.resellerJuridicalName)
                        ps.setString(15, row.extra_params)
                    },
                    getBatchSize: { resellers.size() }
            ] as BatchPreparedStatementSetter)
            log.info("Data inserted in reseller_info table: " + resellers.size());
        } else {
            log.info("List size empty. Could not insert any rows in table");
        }
    }
}

class ResellerInfo {
    String id;
    String resellerId;
    String resellerName;
    String resellerPath;
    String resellerStatus;
    String resellerParentId;
    String resellerTypeId;
    String region;
    String city;
    String zip;
    String country;
    String email;
    String resellerMSISDN;
    String resellerJuridicalName;
    String extra_params;

    ResellerInfo(String id, String resellerId, String resellerName, String resellerPath, String resellerStatus,
                 String resellerParentId, String resellerTypeId, String region, String zip, String city,
                 String country, String email, String resellerMSISDN, String resellerJuridicalName, String extra_params) {
        super();
        this.id = id;
        this.resellerId = resellerId;
        this.resellerName = resellerName;
        this.resellerPath = resellerPath;
        this.resellerStatus = resellerStatus;
        this.resellerParentId = resellerParentId;
        this.resellerTypeId = resellerTypeId;
        this.region = region;
        this.zip = zip;
        this.city = city;
        this.country = country;
        this.email = email;
        this.resellerMSISDN = resellerMSISDN;
        this.resellerJuridicalName = resellerJuridicalName;
        this.extra_params = extra_params;

    }
    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getResellerName() {
        return resellerName
    }

    void setResellerName(String resellerName) {
        this.resellerName = resellerName
    }

    String getResellerPath() {
        return resellerPath
    }

    void setResellerPath(String resellerPath) {
        this.resellerPath = resellerPath
    }

    String getResellerStatus() {
        return resellerStatus
    }

    void setResellerStatus(String resellerStatus) {
        this.resellerStatus = resellerStatus
    }

    String getResellerParentId() {
        return resellerParentId
    }

    void setResellerParentId(String resellerParentId) {
        this.resellerParentId = resellerParentId
    }

    String getResellerTypeId() {
        return resellerTypeId
    }

    void setResellerTypeId(String resellerTypeId) {
        this.resellerTypeId = resellerTypeId
    }

    String getRegion() {
        return region
    }

    void setRegion(String region) {
        this.region = region
    }

    String getZip() {
        return zip
    }

    void setZip(String zip) {
        this.zip = zip
    }

    String getCity() {
        return city
    }

    void setCity(String city) {
        this.city = city
    }

    String getCountry() {
        return country
    }

    void setCountry(String country) {
        this.country = country
    }

    String getEmail() {
        return email
    }

    void setEmail(String email) {
        this.email = email
    }

    String getResellerMSISDN() {
        return resellerMSISDN
    }

    void setResellerMSISDN(String resellerMSISDN) {
        this.resellerMSISDN = resellerMSISDN
    }

    String getResellerJuridicalName() {
        return resellerJuridicalName
    }

    void setResellerJuridicalName(String resellerJuridicalName) {
        this.resellerJuridicalName = resellerJuridicalName
    }

    String getExtra_params() {
        return extra_params
    }

    void setExtra_params(String extra_params) {
        this.extra_params = extra_params
    }

}

