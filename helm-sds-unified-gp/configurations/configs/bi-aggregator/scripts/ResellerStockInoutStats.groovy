package com.seamless.customer.bi.aggregator.aggregate;

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

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime


/**
 *
 *
 *
 *
 */
@Slf4j
public class ResellerStockInoutStats extends AbstractAggregator {
    static final def TABLE = "reseller_stock_inout_stats"
    @Autowired
    RestHighLevelClient client;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Value('${ResellerStockInoutStats.scrollSize:200}')
    int scrollSize;

    @Value('${ResellerStockInoutStats.eventName:BULK_ADD_TDR,ORDER_ACTION,BULK_UPDATE_BY_SIM_ACTIVATION_FILE_UPLOAD}')
    String eventName;

    @Value('${ResellerStockInoutStats.inventoryInsertionEventName:BULK_ADD_TDR}')
    String inventoryInsertionEventName;

    @Value('${ResellerStockInoutStats.inventoryChangeStateEventName:BULK_UPDATE_BY_SIM_ACTIVATION_FILE_UPLOAD}')
    String inventoryChangeStateEventName;

    @Value('${ResellerStockInoutStats.transferInventoryEventName:TRANSFER_INVENTORY}')
    String transferInventoryEventName;

    @Value('${ResellerStockInoutStats.transferInventoryStateName:Available}')
    String transferInventoryStateName;

    @Transactional
    @Scheduled(cron = '${ResellerStockInoutStats.cron:0/10 0 * * * ?}')
    public void aggregate() {

        log.info("ResellerStockInoutStats Aggregator started**************************************************************** at " + new Date());
        def eventNameList = eventName.split(",")
        //fetch data from ES and insert into table
        List<ReportIndex> indices = DateUtil.getIndex(24,24);
        for (ReportIndex index : indices) {
            log.info(index.toString())
            aggregateDataES(index.getIndexName(), index
                    .getStartDate(), index.getEndDate(), eventNameList);
        }
        log.info("ResellerStockInoutStats Aggregator ended**************************************************************************");
    }

    private void aggregateDataES(String index, String fromDate, String toDate, String[] eventNameList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = buildESQuery(fromDate,toDate,eventNameList);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(10));
        List<StockModel> inventory = new ArrayList<>();
        SearchResponse searchResponse = generateResponse(searchRequest,inventory);

        String scrollId =  searchResponse.getScrollId();
        log.info("hits size outside loop for the first time:::"+searchResponse.getHits().size())
        while(searchResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(1));
            try {
                searchResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
            }

            log.info("_________________hits size inside loop _____________________"+searchResponse.getHits().size())
            scrollId = generateScrollResponse(searchResponse,inventory);
        }

        log.debug("Sending "+inventory.size()+" rows to be added to the table");
        insertAggregation(inventory);
        log.info("insertion in table complete");

    }

    private SearchSourceBuilder buildESQuery( String fromDate, String toDate, String[] eventNameList) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(scrollSize);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("eventName.keyword",eventNameList))
                .filter(QueryBuilders.termsQuery("resultCode.keyword",0,200))
                .filter(QueryBuilders.rangeQuery("timestamp").gte(fromDate).lt(toDate))
        searchSourceBuilder.sort("timestamp", SortOrder.ASC).query(queryBuilder);

        return searchSourceBuilder;
    }

    private SearchResponse generateResponse(SearchRequest searchRequest,List<StockModel> inventory) {
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
            SearchHits searchHits = searchResponse.getHits();
            log.info("No of hits -------------" + searchHits.size());
            for(SearchHit searchHit: searchHits.getHits()) {
                fetchInventory(searchHit,inventory);
            }
            log.info("loop finish******************");
        }
        return searchResponse;

    }

    private String generateScrollResponse(SearchResponse searchScrollResponse,List<StockModel> inventory){
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits= searchScrollResponse.getHits();
            log.info("no of hits after 1st request: "+ searchHits.size());
            for(SearchHit searchHit: searchHits.getHits()) {
                fetchInventory(searchHit,inventory);
            }

        }
        return searchScrollResponse.getScrollId();
    }

    private void fetchInventory(SearchHit searchHit,List<StockModel> inventory) {
        Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        DateFormat timestampParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String dateStr = df.format(new Date());
        Date dateObj = df.parse(dateStr);
        Date timestamp=timestampParser.parse(searchHitMap.get("timestamp"))
        if (dateObj.getDate()!=timestamp.getDate()) {
            log.debug("Skipped record with transaction number:  "+ searchHitMap.get("transactionNumber") + " with timestamp "+searchHitMap.get("timestamp"));
            return;
        }
        try {
            if(searchHitMap.get("eventName").toString().equalsIgnoreCase(inventoryInsertionEventName)) {
                List<Map<String,String>> addedInventories = searchHitMap.get("addedInventory");
                log.debug("Adding "+addedInventories.size()+" items from insertion");
                    for(HashMap<String,String> item:addedInventories) {
                        String resellerId = item.get("resellerId");
                        String productSKU = item.get("productSKU");
                        boolean present = false;
                        for (StockModel stock : inventory) {
                            if (stock.getResellerId().equals(resellerId) && stock.getProductSKU().equals(productSKU)) {
                                log.debug("Added " + item.get("quantity") + " inventory for reseller " + stock.getResellerId())
                                stock.addQuantity(item.get("quantity"));
                                present = true;
                                break;
                            }
                        }
                        if (!present) {
                            String id = GenerateHash.createHashString(resellerId, productSKU, item.get("status"), dateStr,null);
                            log.debug("Generated id " + id + " for reseller " + resellerId + " & product " + productSKU + " for date " + dateStr);
                            StockModel stock = new StockModel(id,
                                    resellerId,
                                    productSKU,
                                    item.get("status"),
                                    item.get("quantity"),
                                    dateObj,
                                    null);
                            inventory.add(stock);
                        }
                    }
            }

            if(searchHitMap.get("eventName").toString().equalsIgnoreCase(inventoryChangeStateEventName)){
                List<Map<String,String>> updatedInventories = searchHitMap.get("updatedInventory");
                log.debug("Updating "+updatedInventories.size()+" items from activation file");
                for(HashMap<String,String> item:updatedInventories) {
                    String resellerId = item.get("resellerId");
                    String productSKU = item.get("productSku");
                    String status = item.get("status");
                    String lastDateString=item.get("lastUpdatedTimestamp");
                    Date lastDate=null;
                    DateFormat lastDateParser = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
                    if(lastDateString!=null && !lastDateString.equals("N/A"))
                    {
                        lastDate=lastDateParser.parse(lastDateString);
                    }
                    boolean present = false;
                    for (StockModel stock : inventory) {
                        if (stock.getResellerId().equals(resellerId) && stock.getProductSKU().equals(productSKU) && stock.getStatus().equals(status) && ((stock.getLastDate()==null && lastDate==null)||(stock.getLastDate()!=null && lastDate!=null && lastDate.compareTo(stock.getLastDate())==0 ))) {
                            log.debug("Added " + item.get("quantity") + " inventories for reseller " + stock.getResellerId()+" with status "+status+" and last date "+lastDateString);
                            stock.addQuantity(item.get("quantity"));
                            present = true;
                            break;
                        }
                    }
                    if (!present) {
                        String id = GenerateHash.createHashString(resellerId, productSKU, item.get("status"), dateStr,(lastDate==null?null:df.format(lastDate)));
                        log.debug("Generated id " + id + " for reseller " + resellerId + " & product " + productSKU +" & state " + item.get("status") + " for date " + dateStr);
                        StockModel stock = new StockModel(id,
                                resellerId,
                                productSKU,
                                item.get("status"),
                                item.get("quantity"),
                                dateObj,
                                lastDate);
                        inventory.add(stock);
                    }
                }
            }

            if(searchHitMap.get("ims.eventName").toString().equalsIgnoreCase(transferInventoryEventName)) {
                ArrayList<Map<String,String>> transferredInventory = searchHitMap.get("ims.items");
                String buyerId=searchHitMap.get("ims.buyer.id");
                log.debug("Adding Order "+searchHitMap.get("ims.orderId")+" with "+transferredInventory.size()+" items");
                for(Map<String,String> item:transferredInventory) {
                    String productSKU = item.get("productSku");
                    boolean present = false;
                    for (StockModel stock : inventory) {
                        if (stock.getResellerId().equals(buyerId) && stock.getProductSKU().equals(productSKU)) {
                            log.debug("Added " + item.get("quantity") + " inventory for reseller " + stock.getResellerId());
                            stock.addQuantity(item.get("quantity"));
                            present = true;
                            break;
                        }
                    }
                    if (!present) {
                        String id = GenerateHash.createHashString(buyerId, productSKU, transferInventoryStateName, dateStr,null);
                        log.debug("Generated id " + id + " for reseller " + buyerId + " & product " + productSKU + " for date " + dateStr);
                        StockModel stock = new StockModel(id,
                                buyerId,
                                productSKU,
                                transferInventoryStateName,
                                item.get("quantity"),
                                dateObj,
                                null);
                        inventory.add(stock);
                    }
                }
            }
        }
        catch (Exception e){
            log.error("Skipped record with transaction number:  "+ searchHitMap.get("transactionNumber") + " due to error"+e);
        }
    }

    private def insertAggregation(List stock) {
        log.info("Inserting stock into "+TABLE+" ");
        if (stock.size() != 0) {
            def sql = """INSERT INTO ${TABLE}
            (id,reseller_id,productSKU,status,stock_count,date,last_date)
            VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE stock_count=VALUES(stock_count)"""

            jdbcTemplate.batchUpdate(sql,[
                    setValues: { ps, i ->
                        def row = stock[i]
                        //def index = 0
                        ps.setString(1,row.id)
                        ps.setString(2,row.resellerId)
                        ps.setString(3,row.productSKU)
                        ps.setString(4,row.status)
                        ps.setString(5,row.stockCount)
                        ps.setDate(6,(row.date !=null) ? new java.sql.Date(row.date.getTime()): new java.sql.Date(LocalDateTime.now()))
                        ps.setDate(7,(row.lastDate !=null) ? new java.sql.Date(row.lastDate.getTime()) : null)
                    },
                    getBatchSize: { stock.size() }
            ] as BatchPreparedStatementSetter)
            log.info("Data inserted in reseller_stock_inout_stats table: "+stock.size());
        }
        else {
            log.info("List size empty. Could not insert any rows in table");
        }
    }
}

class StockModel {
    private String id;
    private String resellerId;
    private String productSKU;
    private String status;
    private BigInteger stockCount;
    private Date date;
    private Date lastDate;


    public StockModel(String id,String resellerId,String productSKU,String status,
                                 String stockCount, Date date, Date lastDate) {
        super();
        this.id=id;
        this.resellerId = resellerId;
        this.productSKU = productSKU;
        this.status = status;
        this.stockCount = new BigInteger(stockCount);
        this.date=date;
        this.lastDate=lastDate;

    }

    String getId() {
        return id
    }

    String getResellerId() {
        return resellerId
    }

    String getProductSKU() {
        return productSKU
    }

    String getStatus() {
        return status
    }

    String getStockCount() {
        return stockCount.toString()
    }

    Date getDate() {
        return date
    }

    Date getLastDate() {
        return lastDate
    }

    void addQuantity(String newQuantity) {
        stockCount = stockCount.add(new BigInteger(newQuantity));
    }
}