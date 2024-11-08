package com.seamless.customer.bi.aggregator.aggregate


import com.seamless.customer.bi.aggregator.model.ReportIndex
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
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder
import org.elasticsearch.search.aggregations.metrics.ParsedSum
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

@Slf4j
public class DealerPerformanceExternalDataAggregator extends ScrollableAbstractAggregator {

    static final def TABLE = "dealer_performance_external_weekly_sales_summary"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${DealerPerformanceExternalDataAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DealerPerformanceExternalDataAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${DealerPerformanceExternalDataAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${DealerPerformanceExternalDataAggregator.eventName:externalDevicesDataDaily}')
    String eventName

    @Value('${indexPattern:data_lake_}')
    String indexPattern

    @Value('${DealerPerformanceExternalDataAggregator.scrollSize:7000}')
    int scrollSize;

    @Transactional
    @Scheduled(cron = '${DealerPerformanceExternalDataAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("DealerPerformanceExternalDataAggregator Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    index = index.replace("*", "").trim();
                    List<DealerPerformanceExternalDataAggregatorModel> dealerPerformanceExternalDataAggregatorModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventName)

                    insertAggregation(dealerPerformanceExternalDataAggregatorModelList);
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
                List<DealerPerformanceExternalDataAggregatorModel> dealerPerformanceExternalDataAggregatorModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventName);
                insertAggregation(dealerPerformanceExternalDataAggregatorModelList);
            }
        }
        log.info("DealerPerformanceExternalDataAggregator Aggregator ended**************************************************************************");
    }

    private List<DealerPerformanceExternalDataAggregatorModel> aggregateDataES(String index, String fromDate, String toDate, String eventName) {
        List<DealerPerformanceExternalDataAggregatorModel> dealerPerformanceExternalDataAggregatorModelList = new ArrayList<>()
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventName);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(5));


        log.info("*******Request:::: " + searchRequest.toString());
        SearchResponse searchResponse = generateSearchResponse(searchRequest, client);

        if (searchResponse != null) {
            dealerPerformanceExternalDataAggregatorModelList = generateResponse(searchResponse, index);
            String scrollId = searchResponse.getScrollId();
            log.debug("hits size outside loop for the first time:::" + searchResponse.getHits().size())

            while (searchResponse.getHits().size() != 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = generateScrollSearchResponse(scrollRequest, client);
                if (searchResponse != null) {
                    log.debug("_________________hits size inside loop _____________________" + searchResponse.getHits().size())
                    dealerPerformanceExternalDataAggregatorModelList.addAll(generateResponse(searchResponse, index));
                    scrollId = searchResponse.getScrollId();
                }
            }
        } else {
            log.debug("****** No Search Response found ******")
        }

        return dealerPerformanceExternalDataAggregatorModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String eventName) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(new TermsValuesSourceBuilder("resellerId").field("resellerId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("month").field("month.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("Quarter").field("Quarter.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("year").field("year.keyword").missingBucket(true));
        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("DealerPerformanceExternalDataAggregator",
                sources).size(scrollSize);
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalNumberOfVans").field("numberOfVans"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalLinesConnected").field("linesConnected"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalNumberOfTills").field("numberOfTills"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalNumberOfMotorBikes").field("numberOfMotorBikes"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalNumberOfDevice").field("numberOfDevice"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalSold").field("sold"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalDeviceAttached").field("deviceAttached"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalAirtimeSold").field("totalAirtimeSold"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalMPESAFLOAT").field("MPESAFLOAT"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalTransactionVolume").field("transactionVolume"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalTransactionValue").field("transactionValue"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalMPESAFLOATLEVEL").field("MPESAFLOATLEVEL"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalStockVolume").field("stockVolume"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalStockValue").field("stockValue"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalDeviceSold").field("deviceSold"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalLinesOrdered").field("linesOrdered"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalLinesAttached").field("linesAttached"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalOpenIssues").field("openIssues"))
        compositeBuilder.subAggregation(AggregationBuilders.sum("TotalClosedIssues").field("closedIssues"))


        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword", eventName))
        searchSourceBuilder.query(queryBuilder);

        searchSourceBuilder.aggregation(compositeBuilder).size(scrollSize);
        return searchSourceBuilder;
    }

    private List<DealerPerformanceExternalDataAggregatorModel> generateResponse(SearchResponse searchResponse, String index) {
        List<DealerPerformanceExternalDataAggregatorModel> dealerPerformanceExternalDataAggregatorModelList = new ArrayList<>();
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);

        if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();

            if (aggregations != null) {
                ParsedComposite parsedComposite = aggregations.asMap().get("DealerPerformanceExternalDataAggregator");

                for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                    LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                    if (keyValuesMap.get("resellerId") == null) {
                        keyValuesMap.put("resellerId", "N/A");
                    }
                    if (keyValuesMap.get("month") == null) {
                        keyValuesMap.put("month", "N/A");
                    }
                    if (keyValuesMap.get("Quarter") == null) {
                        keyValuesMap.put("Quarter", "N/A");
                    }
                    if (keyValuesMap.get("year") == null) {
                        keyValuesMap.put("year", "N/A");
                    }

                    if (bucket.getAggregations() != null) {
                        Map<String, Aggregation> aggregationMap = bucket.getAggregations().asMap();
                        Aggregation numberOfMotorBikesAggregation = aggregationMap.get("TotalNumberOfMotorBikes");
                        ParsedSum numberOfMotorBikes = (ParsedSum) numberOfMotorBikesAggregation;
                        Aggregation numberOfVansAggregation = aggregationMap.get("TotalNumberOfVans");
                        ParsedSum numberOfVans = (ParsedSum) numberOfVansAggregation;
                        Aggregation numberOfTillsAggregation = aggregationMap.get("TotalNumberOfTills");
                        ParsedSum numberOfTills = (ParsedSum) numberOfTillsAggregation;
                        Aggregation linesConnectedAggregation = aggregationMap.get("TotalLinesConnected");
                        ParsedSum linesConnected = (ParsedSum) linesConnectedAggregation;
                        Aggregation numberOfDeviceAggregation = aggregationMap.get("TotalNumberOfDevice");
                        ParsedSum numberOfDevice = (ParsedSum) numberOfDeviceAggregation;
                        Aggregation soldAggregation = aggregationMap.get("TotalSold");
                        ParsedSum sold = (ParsedSum) soldAggregation;
                        Aggregation deviceAttachedAggregation = aggregationMap.get("TotalDeviceAttached");
                        ParsedSum deviceAttached = (ParsedSum) deviceAttachedAggregation;
                        Aggregation totalAirtimeSoldAggregation = aggregationMap.get("TotalAirtimeSold");
                        ParsedSum totalAirtimeSold = (ParsedSum) totalAirtimeSoldAggregation;
                        Aggregation MPESAFLOATAggregation = aggregationMap.get("TotalMPESAFLOAT");
                        ParsedSum MPESAFLOAT = (ParsedSum) MPESAFLOATAggregation;
                        Aggregation transactionVolumeAggregation = aggregationMap.get("TotalTransactionVolume");
                        ParsedSum transactionVolume = (ParsedSum) transactionVolumeAggregation;
                        Aggregation transactionValueAggregation = aggregationMap.get("TotalTransactionValue");
                        ParsedSum transactionValue = (ParsedSum) transactionValueAggregation;
                        Aggregation MPESAFLOATLEVELAggregation = aggregationMap.get("TotalMPESAFLOATLEVEL");
                        ParsedSum MPESAFLOATLEVEL = (ParsedSum) MPESAFLOATLEVELAggregation;
                        Aggregation stockVolumeAggregation = aggregationMap.get("TotalStockVolume");
                        ParsedSum stockVolume = (ParsedSum) stockVolumeAggregation;
                        Aggregation stockValueAggregation = aggregationMap.get("TotalStockValue");
                        ParsedSum stockValue = (ParsedSum) stockValueAggregation;
                        Aggregation deviceSoldAggregation = aggregationMap.get("TotalDeviceSold");
                        ParsedSum deviceSold = (ParsedSum) deviceSoldAggregation;
                        Aggregation linesOrderedAggregation = aggregationMap.get("TotalLinesOrdered");
                        ParsedSum linesOrdered = (ParsedSum) linesOrderedAggregation;
                        Aggregation linesAttachedAggregation = aggregationMap.get("TotalLinesAttached");
                        ParsedSum linesAttached = (ParsedSum) linesAttachedAggregation;
                        Aggregation openIssuesAggregation = aggregationMap.get("TotalOpenIssues");
                        ParsedSum openIssues = (ParsedSum) openIssuesAggregation;
                        Aggregation closedIssuesAggregation = aggregationMap.get("TotalClosedIssues");
                        ParsedSum closedIssues = (ParsedSum) closedIssuesAggregation;
                        String[] yearAndWeek = DateUtil.getIndexWorkWeekAndYear(index, indexPattern);
                        String id = GenerateHash.createHashString(yearAndWeek[0], yearAndWeek[1], keyValuesMap.get("year"), keyValuesMap.get("month"), keyValuesMap.get("Quarter"), keyValuesMap.get("resellerId"));
                        DealerPerformanceExternalDataAggregatorModel dealerPerformanceExternalDataAggregatorModel = new DealerPerformanceExternalDataAggregatorModel(
                                id, Integer.parseInt(keyValuesMap.get("year")), Integer.parseInt(yearAndWeek[1]), Integer.parseInt(keyValuesMap.get("month")),
                                keyValuesMap.get("Quarter"), keyValuesMap.get("resellerId"), numberOfMotorBikes.getValue(), numberOfVans.getValue(), numberOfTills.getValue(),
                                linesConnected.getValue(), numberOfDevice.getValue(), sold.getValue(), deviceAttached.getValue(), totalAirtimeSold.getValue(),
                                MPESAFLOAT.getValue(), transactionVolume.getValue(), transactionValue.getValue(), MPESAFLOATLEVEL.getValue(),
                                stockVolume.getValue(), stockValue.getValue(), deviceSold.getValue(), linesOrdered.getValue(), linesAttached.getValue(), openIssues.getValue(), closedIssues.getValue());


                        dealerPerformanceExternalDataAggregatorModelList.add(dealerPerformanceExternalDataAggregatorModel);
                    }
                }
            }
        }
        return dealerPerformanceExternalDataAggregatorModelList;
    }

    private def insertAggregation(List dealerPerformanceExternalDataAggregatorModelList) {

        log.info("DealerPerformanceExternalDataAggregator Aggregated into ${dealerPerformanceExternalDataAggregatorModelList.size()} rows.")
        if (dealerPerformanceExternalDataAggregatorModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,reseller_id,year,week,month,quarter,number_of_motorbikes,number_of_vans,number_of_tills,lines_connected," +
                    "number_of_device,sold,device_attached,total_airtime_sold,MPESAFLOAT,transaction_volume,transaction_value,MPESAFLOATLEVEL,stock_volume," +
                    "stock_value,device_sold,lines_ordered,lines_attached,open_issues,closed_issues)" +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE number_of_motorbikes = VALUES(number_of_motorbikes),number_of_vans = VALUES(number_of_vans)," +
                    "number_of_tills = VALUES(number_of_tills),lines_connected = VALUES(lines_connected),number_of_device = VALUES(number_of_device)," +
                    "sold = VALUES(sold),device_attached = VALUES(device_attached),total_airtime_sold = VALUES(total_airtime_sold),MPESAFLOAT = VALUES(MPESAFLOAT)," +
                    "transaction_volume = VALUES(transaction_volume),transaction_value = VALUES(transaction_value),MPESAFLOATLEVEL = VALUES(MPESAFLOATLEVEL)," +
                    "stock_volume = VALUES(stock_volume),stock_value = VALUES(stock_value),device_sold = VALUES(device_sold),lines_ordered = VALUES(lines_ordered)," +
                    "lines_attached = VALUES(lines_attached),open_issues = VALUES(open_issues),closed_issues = VALUES(closed_issues)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dealerPerformanceExternalDataAggregatorModelList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.resellerId)
                        ps.setInt(++index, row.year)
                        ps.setInt(++index, row.weekNumber)
                        ps.setInt(++index, row.month)
                        ps.setString(++index, row.Quarter)
                        ps.setBigDecimal(++index, row.numberOfMotorBikes)
                        ps.setBigDecimal(++index, row.numberOfVans)
                        ps.setBigDecimal(++index, row.numberOfTills)
                        ps.setBigDecimal(++index, row.linesConnected)
                        ps.setBigDecimal(++index, row.numberOfDevice)
                        ps.setBigDecimal(++index, row.sold)
                        ps.setBigDecimal(++index, row.deviceAttached)
                        ps.setBigDecimal(++index, row.totalAirtimeSold)
                        ps.setBigDecimal(++index, row.MPESAFLOAT)
                        ps.setBigDecimal(++index, row.transactionVolume)
                        ps.setBigDecimal(++index, row.transactionValue)
                        ps.setBigDecimal(++index, row.MPESAFLOATLEVEL)
                        ps.setBigDecimal(++index, row.stockVolume)
                        ps.setBigDecimal(++index, row.stockValue)
                        ps.setBigDecimal(++index, row.deviceSold)
                        ps.setBigDecimal(++index, row.linesOrdered)
                        ps.setBigDecimal(++index, row.linesAttached)
                        ps.setBigDecimal(++index, row.openIssues)
                        ps.setBigDecimal(++index, row.closedIssues)

                    },
                    getBatchSize: { dealerPerformanceExternalDataAggregatorModelList.size() }
            ] as BatchPreparedStatementSetter)
        }
    }

}

class DealerPerformanceExternalDataAggregatorModel {
    private String id;
    private int year;
    private int weekNumber;
    private int month;
    private String Quarter
    private String resellerId;
    private Double numberOfMotorBikes;
    private Double numberOfVans;
    private Double numberOfTills;
    private Double linesConnected;
    private Double numberOfDevice;
    private Double sold;
    private Double deviceAttached;
    private Double totalAirtimeSold;
    private Double MPESAFLOAT;
    private Double transactionVolume;
    private Double transactionValue;
    private Double MPESAFLOATLEVEL;
    private Double stockVolume;
    private Double stockValue;
    private Double deviceSold;
    private Double linesOrdered;
    private Double linesAttached;
    private Double openIssues;
    private Double closedIssues;

    DealerPerformanceExternalDataAggregatorModel(String id, int year, int weekNumber, int month, String quarter, String resellerId, Double numberOfMotorBikes, Double numberOfVans, Double numberOfTills, Double linesConnected, Double numberOfDevice, Double sold, Double deviceAttached, Double totalAirtimeSold, Double MPESAFLOAT, Double transactionVolume, Double transactionValue, Double MPESAFLOATLEVEL, Double stockVolume, Double stockValue, Double deviceSold, Double linesOrdered, Double linesAttached, Double openIssues, Double closedIssues) {
        this.id = id
        this.year = year
        this.weekNumber = weekNumber
        this.month = month
        this.Quarter = quarter
        this.resellerId = resellerId
        this.numberOfMotorBikes = numberOfMotorBikes
        this.numberOfVans = numberOfVans
        this.numberOfTills = numberOfTills
        this.linesConnected = linesConnected
        this.numberOfDevice = numberOfDevice
        this.sold = sold
        this.deviceAttached = deviceAttached
        this.totalAirtimeSold = totalAirtimeSold
        this.MPESAFLOAT = MPESAFLOAT
        this.transactionVolume = transactionVolume
        this.transactionValue = transactionValue
        this.MPESAFLOATLEVEL = MPESAFLOATLEVEL
        this.stockVolume = stockVolume
        this.stockValue = stockValue
        this.deviceSold = deviceSold
        this.linesOrdered = linesOrdered
        this.linesAttached = linesAttached
        this.openIssues = openIssues
        this.closedIssues = closedIssues
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

    int getMonth() {
        return month
    }

    void setMonth(int month) {
        this.month = month
    }

    String getQuarter() {
        return Quarter
    }

    void setQuarter(String quarter) {
        Quarter = quarter
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    Double getNumberOfMotorBikes() {
        return numberOfMotorBikes
    }

    void setNumberOfMotorBikes(Double numberOfMotorBikes) {
        this.numberOfMotorBikes = numberOfMotorBikes
    }

    Double getNumberOfVans() {
        return numberOfVans
    }

    void setNumberOfVans(Double numberOfVans) {
        this.numberOfVans = numberOfVans
    }

    Double getNumberOfTills() {
        return numberOfTills
    }

    void setNumberOfTills(Double numberOfTills) {
        this.numberOfTills = numberOfTills
    }

    Double getLinesConnected() {
        return linesConnected
    }

    void setLinesConnected(Double linesConnected) {
        this.linesConnected = linesConnected
    }

    Double getNumberOfDevice() {
        return numberOfDevice
    }

    void setNumberOfDevice(Double numberOfDevice) {
        this.numberOfDevice = numberOfDevice
    }

    Double getSold() {
        return sold
    }

    void setSold(Double sold) {
        this.sold = sold
    }

    Double getDeviceAttached() {
        return deviceAttached
    }

    void setDeviceAttached(Double deviceAttached) {
        this.deviceAttached = deviceAttached
    }

    Double getTotalAirtimeSold() {
        return totalAirtimeSold
    }

    void setTotalAirtimeSold(Double totalAirtimeSold) {
        this.totalAirtimeSold = totalAirtimeSold
    }

    Double getMPESAFLOAT() {
        return MPESAFLOAT
    }

    void setMPESAFLOAT(Double MPESAFLOAT) {
        this.MPESAFLOAT = MPESAFLOAT
    }

    Double getTransactionVolume() {
        return transactionVolume
    }

    void setTransactionVolume(Double transactionVolume) {
        this.transactionVolume = transactionVolume
    }

    Double getTransactionValue() {
        return transactionValue
    }

    void setTransactionValue(Double transactionValue) {
        this.transactionValue = transactionValue
    }

    Double getMPESAFLOATLEVEL() {
        return MPESAFLOATLEVEL
    }

    void setMPESAFLOATLEVEL(Double MPESAFLOATLEVEL) {
        this.MPESAFLOATLEVEL = MPESAFLOATLEVEL
    }

    Double getStockVolume() {
        return stockVolume
    }

    void setStockVolume(Double stockVolume) {
        this.stockVolume = stockVolume
    }

    Double getStockValue() {
        return stockValue
    }

    void setStockValue(Double stockValue) {
        this.stockValue = stockValue
    }

    Double getDeviceSold() {
        return deviceSold
    }

    void setDeviceSold(Double deviceSold) {
        this.deviceSold = deviceSold
    }

    Double getLinesOrdered() {
        return linesOrdered
    }

    void setLinesOrdered(Double linesOrdered) {
        this.linesOrdered = linesOrdered
    }

    Double getLinesAttached() {
        return linesAttached
    }

    void setLinesAttached(Double linesAttached) {
        this.linesAttached = linesAttached
    }

    Double getOpenIssues() {
        return openIssues
    }

    void setOpenIssues(Double openIssues) {
        this.openIssues = openIssues
    }

    Double getClosedIssues() {
        return closedIssues
    }

    void setClosedIssues(Double closedIssues) {
        this.closedIssues = closedIssues
    }
}