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

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class TransactionSummaryReport extends AbstractAggregator {
    static final def TABLE = "total_monetary_transaction_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TransactionSummaryReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TransactionSummaryReport.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${TransactionSummaryReport.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${TransactionSummaryReport.eventName:RAISE_ORDER}')
    String eventName

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${TransactionSummaryReport.operationType:ADD,ADD_BANK,ADD_BANK_KHAAN,ADD_BANK_SAVING,ADD_MIG,ADD_SELF,DEALER_RECHARGE,SUB,SUB_MIG,SUB_SELF,SUB_SELLER,BIG_DEALER_RECHARGE}')
    String operationType;

    @Value('${TransactionSummaryReport.scrollSize:1000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${TransactionSummaryReport.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** TransactionSummaryReport Aggregator started at " + new Date());
        if (bulkInsertionMode) {
            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);
            //need to change

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
                catch (Exception e) {
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

        log.info("********** TransactionSummaryReport Aggregator ended at " + new Date());
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
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.termsQuery("eventName.keyword", eventName));
        if (!bulkInsertionMode) {
            queryBuilder = queryBuilder.filter(QueryBuilders.rangeQuery("timestamp").gte("now" + timeOffset + "-3h/d").lt("now" + timeOffset + "+1h/d")
                    .includeLower(true).includeUpper(true))
        }
        searchSourceBuilder.query(queryBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<TotalTransactionSummaryModel> generateResponse(SearchResponse searchResponse) {
        List<TotalTransactionSummaryModel> transactionSummaryModelList = new ArrayList<>();

        if (searchResponse == null) {
            log.info("******* Null response received ")
        } else {

            RestStatus status = searchResponse.status();
            log.debug("response status -------------" + status);

            HashMap<String, TotalTransactionSummaryModel> transactionSummaryMap = new HashMap<>();
            if (status == RestStatus.OK) {
                String[] operationTypes = operationType.split(",");

                SearchHits searchHits = searchResponse.getHits();
                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
                    if (searchHitMap.getOrDefault("oms.items", null) != null) {
                        String dealerMsisdn = searchHitMap.getOrDefault("oms.sender.msisdn", "N/A") as String;
                        String transactionDate = DateFormatter.formatDate(searchHitMap.get("timestamp") as String).format("yyyy-MM-dd");

                        String area = "N/A"
                        String section = "N/A"
                        String city_province = "N/A"
                        String district = "N/A"
                        String dealerId = "N/A"
                        String dealerEposTerminalId = "N/A"
                        Map<String, Object> senderAddlField = searchHitMap.getOrDefault("oms.sender.additionalFields", null);
                        if (senderAddlField != null) {
                            area = (senderAddlField.getOrDefault("area", "N/A") as String);
                            section = (senderAddlField.getOrDefault("section", "N/A") as String);
                            city_province = (senderAddlField.getOrDefault("city_province", "N/A") as String);
                            district = (senderAddlField.getOrDefault("district_sum", "N/A") as String);
                            dealerId = (senderAddlField.getOrDefault("dealer_code", "N/A") as String);
                            dealerEposTerminalId = (senderAddlField.getOrDefault("epos_terminal_id", "N/A") as String);
                        }
                        List<HashMap<String, String>> omsItems = searchHitMap.get("oms.items");
                        if (!omsItems.isEmpty()) {
                            for (HashMap<String, String> omsItem : omsItems) {
                                String productSku = omsItem.getOrDefault("productSku", "N/A") as String;

                                if (omsItem.getOrDefault("data", null) != null) {

                                    HashMap<String, String> data = omsItem.get("data");
                                    String dataOperationType = data.getOrDefault("operationType", "N/A");
                                    if (operationTypes.contains(dataOperationType) && data.getOrDefault("resultCode", null) != null && data.get("resultCode") == "0") {
                                        def amount = 0D
                                        if (!data.getOrDefault("amount", "0").equals("N/A")) {
                                            amount = Double.parseDouble(data.getOrDefault("amount", "0"))
                                        }

                                        String dealerType = data.getOrDefault("senderResellerType", "N/A") as String;
                                        String id = GenerateHash.createHashString(
                                                dataOperationType,
                                                dealerType,
                                                dealerMsisdn,
                                                dealerId,
                                                transactionDate
                                        );

                                        if (transactionSummaryMap.containsKey(id)) {
                                            TotalTransactionSummaryModel transactionSummaryModel = transactionSummaryMap.get(id);
                                            transactionSummaryModel.setSum(amount + transactionSummaryModel.getSum());
                                            transactionSummaryModel.setTransactionCount(transactionSummaryModel.getTransactionCount() + 1);
                                            transactionSummaryModel.setProductSku(productSku)
                                            transactionSummaryMap.put(id, transactionSummaryModel);
                                        } else {
                                            def dateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(transactionDate);
                                            TotalTransactionSummaryModel transactionSummaryModel = new TotalTransactionSummaryModel(id, dateObj, dealerType, dealerId, dealerMsisdn, 1, district, area, section, city_province, amount, dataOperationType, dealerEposTerminalId,productSku);
                                            transactionSummaryMap.put(id, transactionSummaryModel);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                transactionSummaryMap.each {
                    entry -> transactionSummaryModelList.add(entry.value)
                }
            }
        }
        return transactionSummaryModelList;
    }

    private def insertAggregation(List transactionSummaryModelList) {

        log.info("TransactionSummaryReport Aggregated into ${transactionSummaryModelList.size()} rows.")
        if (transactionSummaryModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,transaction_date,dealer_type,dealer_id,dealer_msisdn,dealer_epos_terminal_id,transaction_type,district,area,section," +
                    "city,transaction_count,sum,productsku) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE sum = VALUES(sum), transaction_count = VALUES(transaction_count)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = transactionSummaryModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, new java.sql.Date(row.transactionDate.getTime()))
                        ps.setString(++index, row.dealerType)
                        ps.setString(++index, row.dealerId)
                        ps.setString(++index, row.dealerMSISDN)
                        ps.setString(++index, row.dealerEposterminalId)
                        ps.setString(++index, row.transactionType)
                        ps.setString(++index, row.district)
                        ps.setString(++index, row.area)
                        ps.setString(++index, row.section)
                        ps.setString(++index, row.city)
                        ps.setLong(++index, row.transactionCount)
                        ps.setBigDecimal(++index, row.sum)
                        ps.setString(++index, row.productSku)
                    },
                    getBatchSize: { transactionSummaryModelList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

}

class TotalTransactionSummaryModel {
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
    private double sum;
    private String dealerEposterminalId;
    private String productSku


    public TotalTransactionSummaryModel(String id, Date transactionDate, String dealerType, String dealerId, String dealerMSISDN, long transactionCount,
                                        String district, String area, String section, String city, double sum, String transactionType, String dealerEposterminalId,String productSku) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.dealerType = dealerType;
        this.dealerId = dealerId;
        this.dealerMSISDN = dealerMSISDN;
        this.area = area;
        this.sum = sum;
        this.transactionCount = transactionCount;
        this.district = district;
        this.section = section;
        this.city = city;
        this.transactionType = transactionType;
        this.dealerEposterminalId = dealerEposterminalId
        this.productSku=productSku
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getTransactionDate() {
        return transactionDate
    }

    void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
    }

    String getTransactionType() {
        return transactionType
    }

    void setTransactionType(String transactionType) {
        this.transactionType = transactionType
    }

    String getDealerType() {
        return dealerType
    }

    void setDealerType(String dealerType) {
        this.dealerType = dealerType
    }


    String getDealerEposTerminalId() {
        return dealerEposterminalId
    }

    void setDealerEposTerminalId(String dealerEposterminalId) {
        this.dealerEposterminalId = dealerEposterminalId
    }


    String getDealerId() {
        return dealerId
    }

    void setDealerId(String dealerId) {
        this.dealerId = dealerId
    }

    String getDealerMSISDN() {
        return dealerMSISDN
    }

    void setDealerMSISDN(String dealerMSISDN) {
        this.dealerMSISDN = dealerMSISDN
    }

    long getTransactionCount() {
        return transactionCount
    }

    void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount
    }

    String getDistrict() {
        return district
    }

    void setDistrict(String district) {
        this.district = district
    }

    String getArea() {
        return area
    }

    void setArea(String area) {
        this.area = area
    }

    String getSection() {
        return section
    }

    void setSection(String section) {
        this.section = section
    }

    String getCity() {
        return city
    }

    void setCity(String city) {
        this.city = city
    }

    double getSum() {
        return sum
    }

    void setSum(double sum) {
        this.sum = sum
    }
    String getProductSku() {
        return productSku
    }

    void setProductSku(String productSku) {
        this.productSku = productSku
    }
}