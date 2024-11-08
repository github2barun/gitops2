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
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.core.TimeValue
import java.util.stream.Collectors


/**
 *
 *
 *
 *
 */
@Slf4j
public class ResellerCurrentStatus extends AbstractAggregator {
    static final def TABLE = "reseller_current_status"
    static final def ADDITIONAL_TABLE = "reseller_current_status_additional_info"
    @Autowired
    RestHighLevelClient client;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value('${ResellerCurrentStatus.scrollSize:200}')
    int scrollSize;
//    @Value('${ResellerCurrentStatus.index:reseller_creation}')
//    String index;

    @Value('${ResellerCurrentStatus.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ResellerCurrentStatus.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ResellerCurrentStatus.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${ResellerCurrentStatus.eventName:addReseller,updateReseller}')
    String eventName;

    @Value('${ResellerCurrentStatus.resellerCreationEventName:addReseller}')
    String resellerCreationEventName;

    @Value('${ResellerCurrentStatus.resellerChangeStateEventName:resellerChangeState}')
    String resellerChangeStateEventName;

    @Value('${ResellerCurrentStatus.updateResellerEventName:updateReseller}')
    String updateResellerEventName;

    @Value('${ResellerCurrentStatus.updateUserEventName:updateUser}')
    String updateUserEventName;

    @Value('${ResellerCurrentStatus.indexPrefix:dms_}')
    String indexPrefix

    @Transactional
    @Scheduled(cron = '${ResellerCurrentStatus.cron:*/3 * * * * ?}')

    public void aggregate() {

        log.info("ResellerCurrentStatus Aggregator started**************************************************************** at " + new Date());
        def eventNameList = eventName.split(",")
        //fetch data from ES and insert into table
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);  //need to change

            for (String index : indices) {
                index = indexPrefix + index;
                log.info(index.toString() + "for bulk insertion")
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventNameList)
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
                index.setIndexName(indexPrefix + index.getIndexName());
                log.info(index.toString())
                aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventNameList);
            }
        }
        // aggregateDataES(index)
        log.info("ResellerCurrentStatus Aggregator ended**************************************************************************");
    }

    private void aggregateDataES(String index, String fromDate, String toDate, String[] eventNameList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate, toDate, eventNameList);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(10));
        SearchResponse searchResponse = generateResponse(searchRequest);

        String scrollId = searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::" + searchResponse.getHits().size())
        while (searchResponse.getHits().size() != 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(1));
            try {
                searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
            }

            log.info("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
            scrollId = generateScrollResponse(searchResponse);
        }


    }

    private SearchSourceBuilder buildESQuery(String fromDate, String toDate, String[] eventNameList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(scrollSize);

        if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventNameList))
                    .filter(QueryBuilders.termsQuery("resultCode.keyword", 0))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
            searchSourceBuilder.sort("timestamp", SortOrder.ASC).query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword", eventNameList))
                    .filter(QueryBuilders.termsQuery("resultCode.keyword", 0))
            searchSourceBuilder.sort("timestamp", SortOrder.ASC).query(queryBuilder);
        }
        return searchSourceBuilder;
    }

    private SearchResponse generateResponse(SearchRequest searchRequest) {
        List<PosCurrentStatusModel> resellers = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Could not perform search on elasticsearch. Error message: " + e);
        }

        log.info("*******Search Request*******" + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.info("response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchResponse.getHits()
            for (SearchHit searchHit : searchHits.getHits()) {
                List<PosCurrentStatusModel> pos = fetchReseller(searchHit)
                if (pos != null && !pos.isEmpty()) {
                    resellers.addAll(pos)
                }
            }
            log.info("loop finish******************");
        }
        insertAggregation(resellers)
        // populateAdditionalInfo(resellers)
        insertAdditionalAggregation(resellers)
        log.info("inserted first time in table");
        return searchResponse;

    }

    private String generateScrollResponse(SearchResponse searchScrollResponse) {
        List<PosCurrentStatusModel> resellers = new ArrayList<>();
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchScrollResponse.getHits();
            log.info("no of hits after 1st request: " + searchHits.size());
            for (SearchHit searchHit : searchHits.getHits()) {
                List<PosCurrentStatusModel> pos = fetchReseller(searchHit)
                if (pos != null && !pos.isEmpty()) {
                    resellers.addAll(pos)
                }
            }

        }
        insertAggregation(resellers);
        // populateAdditionalInfo(resellers)
        insertAdditionalAggregation(resellers)
        log.info("inserting records subsequent time in table, if any");
        return searchScrollResponse.getScrollId();
    }

    private List<PosCurrentStatusModel> fetchReseller(SearchHit searchHit) {
        Map<String, Object> searchHitMap = searchHit.getSourceAsMap()
        List<PosCurrentStatusModel> resellerList = new ArrayList<>();
        List<PosCurrentStatusModel> resellerListOnChangeState = new ArrayList<>();

        try {
            if (searchHitMap.get("eventName").toString().equalsIgnoreCase(resellerCreationEventName)
                    || searchHitMap.get("eventName").toString().equalsIgnoreCase(updateResellerEventName) ||
                    searchHitMap.get("eventName").toString().equalsIgnoreCase(updateUserEventName) ||
                    searchHitMap.get("eventName").toString().equalsIgnoreCase(resellerChangeStateEventName)) {
                List dmsInfo = searchHitMap.get("DMS") as List

                for (index in 0..<dmsInfo.size()) {
                    if (searchHitMap.get("eventName").toString().equalsIgnoreCase(resellerChangeStateEventName)) {
                        HashMap<String, String> resellerParam
                        String id = GenerateHash.createHashString(dmsInfo.get(index).get("resellerId"));
                        PosCurrentStatusModel reseller = new PosCurrentStatusModel();
                        reseller.setId(id);
                        reseller.setResellerId(dmsInfo.get(index).get("resellerId"));
                        reseller.setResellerName(dmsInfo.get(index).get("resellerName"));
                        reseller.setResellerPath(dmsInfo.get(index).get("resellerPath"));
                        reseller.setResellerStatus(dmsInfo.get(index).get("resellerStatus"));
                        reseller.setResellerParentId(dmsInfo.get(index).get("resellerParentId"));
                        reseller.setResellerTypeId(dmsInfo.get(index).get("resellerType"));

                        resellerListOnChangeState.add(reseller);


                    } else {
                        HashMap<String, String> resellerInfo = dmsInfo.get(index).get("resellerInfo").get("reseller");
                        HashMap<String, String> address = dmsInfo.get(index).get("resellerInfo").get("reseller").get("address")
                        //def batchId = dmsInfo.get(index).get("batchId") as String

                        ArrayList<Map<String, String>> additionalFields = dmsInfo.get(index).get("resellerInfo").get("additionalFields");
                        String emailResponsible = "", salesArea = null, latitude = null, longitude = null, region = null, suburb = null, city = null;
                        for (Map<String, String> fields : additionalFields) {
                            if (fields.values().contains("email_responsible")) {
                                emailResponsible = fields.get("value");
                            }
                            if (fields.values().contains("salesArea")) {
                                salesArea = fields.get("value");
                            }
                            if (fields.values().contains("latitude")) {
                                latitude = fields.get("value")
                            }
                            if (fields.values().contains("longitude")) {
                                longitude = fields.get("value")
                            }
                            if (fields.values().contains("region")) {
                                region = fields.get("value")
                            }
                            if (fields.values().contains("suburb")) {
                                suburb = fields.get("value")
                            }
                            if (fields.values().contains("city")) {
                                city = fields.get("value")
                            }
                        }
                        String id = GenerateHash.createHashString(resellerInfo.get("resellerId"));
                        PosCurrentStatusModel reseller = new PosCurrentStatusModel(id,
                                resellerInfo.get("resellerId"),
                                resellerInfo.get("resellerName"),
                                resellerInfo.get("resellerPath"),
                                resellerInfo.get("status"),
                                resellerInfo.get("parentResellerId"),
                                resellerInfo.get("resellerTypeId"),
                                region,
                                address.get("street"),
                                suburb,
                                address.get("zip"),
                                city,
                                address.get("country"),
                                address.get("email"),
                                resellerInfo.get("resellerMSISDN"),
                                address.get("email"),
                                DateFormatter.formatDate(searchHitMap.get("timestamp")),
                                searchHitMap.get("user.userId").toString(), null, null);

                        reseller.setCnic(resellerInfo.getOrDefault("cnic", "N/A"))
                        reseller.setContactNo(resellerInfo.get("contactNo"))
                        reseller.setBalanceThreshold(resellerInfo.get("balanceThreshold"))
                        reseller.setSalesArea(salesArea)
                        reseller.setLatitude(latitude)
                        reseller.setLongitude(longitude)
                        reseller.setMsrId(resellerInfo.get("resellerId"))
                        reseller.setMsrName(resellerInfo.get("resellerName"))
                        reseller.setMsrMsisdn(resellerInfo.get("resellerMSISDN"))

                        resellerList.add(reseller)
                    }
                }
                UpdateOnChangeStateEvent(resellerListOnChangeState);
                return resellerList
            }
        }
        catch (Exception e) {
            log.error("Skipped record with transaction number:  " + searchHitMap.get("transactionNumber") + " due to " +
                    "error");
        }
    }

    private def insertAggregation(List resellers) {
        if (resellers.size() != 0) {
            //TODO: Add address,additionalFields and region support at 'ON DUPLICATE KEY UPDATE' when updateReseller event DMS TDR gets fixed
            def sql = """INSERT INTO ${TABLE}
            (id,reseller_id,reseller_name,reseller_path,reseller_status,reseller_parent,reseller_type_id,region,street,suburb,
            zip,city,country,email,MSISDN,email_responsible,created_on,created_by,status_changed_on,status_changed_by)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
            reseller_status=VALUES(reseller_status),status_changed_on=VALUES(status_changed_on),
            status_changed_by=VALUES(status_changed_by),reseller_name=VALUES(reseller_name),MSISDN=VALUES(MSISDN),
	   region=VALUES(region),street=VALUES(street),suburb=VALUES(suburb),zip=VALUES(zip),city=VALUES(city),country=VALUES(country),email=VALUES(email),reseller_path=VALUES(reseller_path)"""

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
                        ps.setString(9, row.street)
                        ps.setString(10, row.suburb)
                        ps.setString(11, row.zip)
                        ps.setString(12, row.city)
                        ps.setString(13, row.country)
                        ps.setString(14, row.email)
                        ps.setString(15, row.resellerMSISDN)
                        ps.setString(16, row.emailResponsible)
                        ps.setDate(17, (row.createdOn != null) ? new java.sql.Date(row.createdOn.getTime()) : null)
                        ps.setString(18, row.createdBy)
                        ps.setDate(19, (row.statusOn != null) ? new java.sql.Date(row.statusOn.getTime()) : null)
                        ps.setString(20, row.statusBy)
                    },
                    getBatchSize: { resellers.size() }
            ] as BatchPreparedStatementSetter)
            log.info("Data inserted in Reseller_current_status table: " + resellers.size());
        } else {
            log.info("List size empty. Could not insert any rows in table");
        }
    }

    private def insertAdditionalAggregation(List<PosCurrentStatusModel> resellers) {
        if (resellers.size() != 0) {
            def filteredResellers = resellers.stream().filter({ reseller -> reseller.getCnic() != null && !reseller.getCnic().trim().isEmpty() }).collect(Collectors.toList()) as List<PosCurrentStatusModel>
            //  if (!filteredResellers.isEmpty()) {
            def sql = """INSERT INTO ${ADDITIONAL_TABLE}
            (reseller_current_id, cnic, msr_id, msr_name, msr_msisdn, sfr_id, sfr_name, sfr_msisdn,
             rso_id, rso_name, rso_msisdn, birthday, postal_code, contact_no, batch_id, circle, max_daily_recharge_limit, balance_threshold, district,
		 sales_area, latitude, longitude)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                cnic=VALUES(cnic), msr_id=VALUES(msr_id), msr_name=VALUES(msr_name), msr_msisdn=VALUES(msr_msisdn), sfr_id=VALUES(sfr_id),
                sfr_name=VALUES(sfr_name), sfr_msisdn=VALUES(sfr_msisdn),rso_id=VALUES(rso_id), rso_name=VALUES(rso_name),
                rso_msisdn=VALUES(rso_msisdn), birthday=VALUES(birthday), postal_code=VALUES(postal_code), contact_no=VALUES(contact_no),
                circle=VALUES(circle), max_daily_recharge_limit=VALUES(max_daily_recharge_limit), balance_threshold=VALUES(balance_threshold),district=VALUES(district),
		sales_area=VALUES(sales_area),latitude=VALUES(latitude),longitude=VALUES(longitude)"""

            jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        //  def row = filteredResellers[i] as PosCurrentStatusModel
                        //def index = 0
                        def row = resellers[i]
                        ps.setString(1, row.resellerId)
                        ps.setString(2, row.cnic)
                        ps.setString(3, row.resellerId)
                        ps.setString(4, row.msrName)
                        ps.setString(5, row.resellerMSISDN)
                        ps.setString(6, row.sfrId)
                        ps.setString(7, row.sfrName)
                        ps.setString(8, row.sfrMsisdn)
                        ps.setString(9, row.rsoId)
                        ps.setString(10, row.rsoName)
                        ps.setString(11, row.rsoMsisdn)
                        ps.setString(12, row.birthday)
                        ps.setString(13, row.postalCode)
                        ps.setString(14, row.contactNo)
                        ps.setString(15, row.batchId)
                        ps.setString(16, row.circle)
                        ps.setString(17, row.maxDailyRechargeLimit)
                        ps.setString(18, row.balanceThreshold)
                        ps.setString(19, row.district)
                        ps.setString(20, row.suburb)
                        ps.setString(21, row.latitude)
                        ps.setString(22, row.longitude)
                    },
                    getBatchSize: { resellers.size() }
            ] as BatchPreparedStatementSetter)
            log.info("Data inserted in ${ADDITIONAL_TABLE} table: " + resellers.size());
            //} else {
            //   log.info("List size empty. Could not insert any rows in additional table");
            //}
        } else {
            log.info("List size empty. Could not insert any rows in table");
        }
    }

    private def UpdateOnChangeStateEvent(List resellers) {
        if (resellers.size() != 0) {
            //TODO: Add address,additionalFields and region support at 'ON DUPLICATE KEY UPDATE' when updateReseller event DMS TDR gets fixed
            def sql = """INSERT INTO ${TABLE}
            (id,reseller_id,reseller_name,reseller_path,reseller_status,reseller_parent,reseller_type_id,region,street,suburb,
            zip,city,country,email,MSISDN,email_responsible,created_on,created_by,status_changed_on,status_changed_by)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
            reseller_status=VALUES(reseller_status),status_changed_on=VALUES(status_changed_on),
            status_changed_by=VALUES(status_changed_by)"""

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
                        ps.setString(9, row.street)
                        ps.setString(10, row.suburb)
                        ps.setString(11, row.zip)
                        ps.setString(12, row.city)
                        ps.setString(13, row.country)
                        ps.setString(14, row.email)
                        ps.setString(15, row.resellerMSISDN)
                        ps.setString(16, row.emailResponsible)
                        ps.setDate(17, (row.createdOn != null) ? new java.sql.Date(row.createdOn.getTime()) : null)
                        ps.setString(18, row.createdBy)
                        ps.setDate(19, (row.statusOn != null) ? new java.sql.Date(row.statusOn.getTime()) : null)
                        ps.setString(20, row.statusBy)
                    },
                    getBatchSize: { resellers.size() }
            ] as BatchPreparedStatementSetter)
            log.info("On ChangeStateEvent status Updated in Reseller_current_status table: " + resellers.size());
        } else {
            log.info("List size empty. Could not insert any rows in table");
        }

    }

    private def populateAdditionalInfo(List resellers) {

        resellers.forEach({ reseller ->
            fetchHierarchicalReseller(reseller as com.seamless.customer.bi.aggregator.aggregate.PosCurrentStatusModel)
        })
        insertAdditionalAggregation(resellers)
    }

    private def fetchHierarchicalReseller(PosCurrentStatusModel posCurrentStatusModel) {
        def sql = """select reseller_id, reseller_name, MSISDN, reseller_type_id from ${TABLE} where reseller_id = :id or (reseller_id in (:ids) and reseller_type_id in (:types))"""
        def resellerIds = posCurrentStatusModel.resellerPath.split("/") as List<String>
        def rso = resellerIds[resellerIds.size() - 2]
        List<String> typeList = ["MSR", "Super_Franchise"] as String[]

        SqlParameterSource parameters = new MapSqlParameterSource("ids", resellerIds)
        parameters.addValue("types", typeList)
        parameters.addValue("id", rso)

        List<PosCurrentStatusModel> resellerList = namedParameterJdbcTemplate.query(sql, parameters, { rs, rowNum ->

            PosCurrentStatusModel resellerInfo = new PosCurrentStatusModel()
            resellerInfo.setResellerId(rs.getString("reseller_id"))
            resellerInfo.setResellerName(rs.getString("reseller_name"))
            resellerInfo.setResellerMSISDN(rs.getString("MSISDN"))
            resellerInfo.setResellerTypeId(rs.getString("reseller_type_id"))
            return resellerInfo
        } as RowMapper<PosCurrentStatusModel>
        ) as List<com.seamless.customer.bi.aggregator.aggregate.PosCurrentStatusModel>

        resellerList.forEach({ reseller ->
            if (reseller.getResellerTypeId().equalsIgnoreCase("MSR")) {
                posCurrentStatusModel.setMsrId(reseller.resellerId)
                posCurrentStatusModel.setMsrName(reseller.resellerName)
                posCurrentStatusModel.setMsrMsisdn(reseller.resellerMSISDN)
            } else if (reseller.getResellerTypeId().equalsIgnoreCase("Super_Franchise")) {
                posCurrentStatusModel.setSfrId(reseller.resellerId)
                posCurrentStatusModel.setSfrName(reseller.resellerName)
                posCurrentStatusModel.setSfrMsisdn(reseller.resellerMSISDN)
            }
            if (rso == reseller.getResellerId()) {
                posCurrentStatusModel.setRsoId(reseller.resellerId)
                posCurrentStatusModel.setRsoName(reseller.resellerName)
                posCurrentStatusModel.setRsoMsisdn(reseller.resellerMSISDN)
            }
        })
    }

}

class PosCurrentStatusModel {
    private String id;
    private String resellerId;
    private String resellerName;
    private String resellerPath;
    private String resellerStatus;
    private String resellerParentId;
    private String resellerTypeId;
    private String region;
    private String street;
    private String zip;
    private String suburb;
    private String city;
    private String country;
    private String email;
    private String resellerMSISDN;
    private String emailResponsible;
    private Date createdOn;
    private String createdBy;
    private Date statusOn;
    private String statusBy;

    //additional information
    private String cnic;
    private String msrId;
    private String msrName;
    private String msrMsisdn;
    private String sfrId;
    private String sfrName;
    private String sfrMsisdn;
    private String rsoId;
    private String rsoName;
    private String rsoMsisdn;
    private Date birthday;
    private String postalCode;
    private String contactNo;
    private String batchId
    private String circle
    private String district
    private String maxDailyRechargeLimit
    private String balanceThreshold
    private String salesArea
    private String latitude
    private String longitude

    PosCurrentStatusModel() {
    }

    public PosCurrentStatusModel(String id, String resellerId, String resellerName, String resellerPath,
                                 String resellerStatus, String resellerParentId, String resellerTypeId, String region, String street, String suburb,
                                 String zip, String city, String country, String email, String resellerMSISDN, String emailResponsible,
                                 Date createdOn, String createdBy, Date statusOn, String statusBy) {
        super();
        this.id = id;
        this.resellerId = resellerId;
        this.resellerName = resellerName;
        this.resellerPath = resellerPath;
        this.resellerStatus = resellerStatus;
        this.resellerParentId = resellerParentId;
        this.resellerTypeId = resellerTypeId;
        this.region = region;
        this.street = street;
        this.suburb = suburb;
        this.zip = zip;
        this.city = city;
        this.country = country;
        this.email = email;
        this.resellerMSISDN = resellerMSISDN;
        this.emailResponsible = emailResponsible;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.statusOn = statusOn;
        this.statusBy = statusBy;

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

    String getSuburb() {
        return suburb
    }

    void setSuburb(String suburb) {
        this.suburb = suburb
    }

    String getStreet() {
        return street
    }

    void setStreet(String street) {
        this.street = street
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

    String getEmailResponsible() {
        return emailResponsible
    }

    void setEmailResponsible(String emailResponsible) {
        this.emailResponsible = emailResponsible
    }


    Date getCreatedOn() {
        return createdOn
    }

    void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn
    }

    String getCreatedBy() {
        return createdBy
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy
    }

    Date getStatusOn() {
        return statusOn
    }

    void setStatusOn(Date statusOn) {
        this.statusOn = statusOn
    }

    String getStatusBy() {
        return statusBy
    }

    void setStatusBy(String statusBy) {
        this.statusBy = statusBy
    }

    String getCnic() {
        return cnic
    }

    void setCnic(String cnic) {
        this.cnic = cnic
    }

    String getMsrId() {
        return msrId
    }

    void setMsrId(String msrId) {
        this.msrId = msrId
    }

    String getMsrName() {
        return msrName
    }

    void setMsrName(String msrName) {
        this.msrName = msrName
    }

    String getMsrMsisdn() {
        return msrMsisdn
    }

    void setMsrMsisdn(String msrMsisdn) {
        this.msrMsisdn = msrMsisdn
    }

    String getSfrId() {
        return sfrId
    }

    void setSfrId(String sfrId) {
        this.sfrId = sfrId
    }

    String getSfrName() {
        return sfrName
    }

    void setSfrName(String sfrName) {
        this.sfrName = sfrName
    }

    String getSfrMsisdn() {
        return sfrMsisdn
    }

    void setSfrMsisdn(String sfrMsisdn) {
        this.sfrMsisdn = sfrMsisdn
    }

    String getRsoId() {
        return rsoId
    }

    void setRsoId(String rsoId) {
        this.rsoId = rsoId
    }

    String getRsoName() {
        return rsoName
    }

    void setRsoName(String rsoName) {
        this.rsoName = rsoName
    }

    String getRsoMsisdn() {
        return rsoMsisdn
    }

    void setRsoMsisdn(String rsoMsisdn) {
        this.rsoMsisdn = rsoMsisdn
    }

    Date getBirthday() {
        return birthday
    }

    void setBirthday(Date birthday) {
        this.birthday = birthday
    }

    String getPostalCode() {
        return postalCode
    }

    void setPostalCode(String postalCode) {
        this.postalCode = postalCode
    }

    String getContactNo() {
        return contactNo
    }

    void setContactNo(String contactNo) {
        this.contactNo = contactNo
    }

    String getBatchId() {
        return batchId
    }

    void setBatchId(String batchId) {
        this.batchId = batchId
    }

    String getCircle() {
        return circle
    }

    void setCircle(String circle) {
        this.circle = circle
    }

    String getDistrict() {
        return district
    }

    void setDistrict(String district) {
        this.district = district
    }

    String getMaxDailyRechargeLimit() {
        return maxDailyRechargeLimit
    }

    void setMaxDailyRechargeLimit(String maxDailyRechargeLimit) {
        this.maxDailyRechargeLimit = maxDailyRechargeLimit
    }

    String getBalanceThreshold() {
        return balanceThreshold
    }

    void setBalanceThreshold(String balanceThreshold) {
        this.balanceThreshold = balanceThreshold
    }

    String getSalesArea() {
        return salesArea
    }

    void setSalesArea(String salesArea) {
        this.salesArea = salesArea
    }

    String getLatitude() {
        return latitude
    }

    void setLatitude(String latitude) {
        this.latitude = latitude
    }

    String getLongitude() {
        return longitude
    }

    void setLongitude(String longitude) {
        this.longitude = longitude
    }
}
