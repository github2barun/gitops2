package com.seamless.customer.bi.aggregator.aggregate

import groovy.util.logging.Slf4j

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.GenerateHash
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.rest.RestStatus;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate
import com.seamless.customer.bi.aggregator.util.DateUtil


@Slf4j
public class LastTransactionAggregation extends AbstractAggregator {
    static final def TABLE = "last_transaction_aggregator"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //fetch config setting for type of data
    @Value('${LastTransactionAggregation.hourwisedata:true}')
    boolean hourwise;
    @Value('${LastTransactionAggregation.hour:10}')
    int hours;

    @Value('${LastTransactionAggregation.profileId:CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP}')
    String profileId;

    @Value('${LastTransactionAggregation.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${LastTransactionAggregation.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${LastTransactionAggregation.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Transactional
    @Scheduled(cron = '${LastTransactionAggregation.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("LastTransactionAggregation Aggregator started*******************************************************************************" + new Date());
        def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList( bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    //List<LastTransactionWiseModel> lastTransactionWiseModelES = aggregateDataES(index, fromDateString, toDateString)
                    //insertAggregation(lastTransactionWiseModelES);
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList);
                    def reseller_transf_in = jdbcTemplate.queryForList("SELECT reseller_id as 'reseller_id',sum(count) as 'count_transfer_in',sum(amount) as 'balance_transfer_in' FROM bi.receiver_wise_credit_transfer_summary GROUP By reseller_id");
                    //log.info("Transfer In = ${reseller_transf_in}")
                    updateResellerTransfIn(reseller_transf_in)
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


                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(),profileIdList);
                def reseller_transf_in = jdbcTemplate.queryForList("SELECT reseller_id as 'reseller_id',sum(count) as 'count_transfer_in',sum(amount) as 'balance_transfer_in' FROM bi.receiver_wise_credit_transfer_summary GROUP By reseller_id");
                log.info("Transfer In = ${reseller_transf_in}")
                updateResellerTransfIn(reseller_transf_in)

            }
        }

        log.info("LastTransactionAggregation Aggregator ended**********************************************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate,String[] profileIdList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate,profileIdList);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(10));

        //List<LastTransactionWiseModel> lastTransactionWiseModels = generateResponse(searchRequest);
        String scrollId= generateResponse(searchRequest);
        SearchResponse searchScrollResponse=client.search(searchRequest, COMMON_OPTIONS);
        log.info("_________________hits size outside loop for the first time_____________________"+searchScrollResponse.getHits().size())
        //fetchScrollInput(firstScrollId);
        while(searchScrollResponse.getHits().size()!=0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueSeconds(30));
            log.info("*******Scroll Request:::: " + scrollRequest.toString());
            try {
                searchScrollResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
            }


            log.info("_________________hits size inside loop _____________________"+searchScrollResponse.getHits().size())

            scrollId = generateScrollResponse(searchScrollResponse);
        }


    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate,String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
            queryBuilder.filter(QueryBuilders.termsQuery("transactionStatus", "Success"))
            queryBuilder.filter(QueryBuilders.termsQuery("transactionProfile", profileID))
            searchSourceBuilder.query(queryBuilder).size(1000).sort("endTime", SortOrder.ASC)
        }
        else {
            BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                   queryBuilder .filter(QueryBuilders.termsQuery("transactionStatus.keyword", "Success"))
                   queryBuilder.filter(QueryBuilders.termsQuery("transactionProfile.keyword", profileID))
                   queryBuilder.filter(QueryBuilders.rangeQuery("endTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.query(queryBuilder).size(1000);
}

        return searchSourceBuilder;
    }

    private String generateScrollResponse(SearchResponse searchScrollResponse){

        List<LastTransactionWiseModel> lastTransactionWiseModels = new ArrayList<>();
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);



        if (status == RestStatus.OK) {
            SearchHits searchHits= searchScrollResponse.getHits();
            log.info("no of hits after 1st request"+ searchHits.size());
            for(SearchHit searchHit: searchHits.getHits()) {

                Map<String,String> searchHitMap=searchHit.getSourceAsMap();
                Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("endTime"));
                String id = GenerateHash.createHashString(searchHitMap.get("senderResellerId"), searchHitMap.get("endTime"));
                String SenderBalance= searchHitMap.get("senderBalanceValueAfter");
                if( SenderBalance==null){
                    SenderBalance = "0.0";
                }
                LastTransactionWiseModel lastTransactionWiseModel = new LastTransactionWiseModel(id,searchHitMap.get("senderResellerId"),searchHitMap.get("senderMSISDN"),searchHitMap.get("senderResellerName"),searchHitMap.get("senderResellerType"),searchHitMap.get("senderAccountType"),1,Double.valueOf(SenderBalance),0.00d,dateTimeDay,searchHitMap.get("transactionProfile"),Double.valueOf(searchHitMap.get("transactionAmount")),searchHitMap.get("currency"),searchHitMap.get("receiverMSISDN"),searchHitMap.get("ersReference"),searchHitMap.get("senderRegion"));
                lastTransactionWiseModels.add(lastTransactionWiseModel);
            }

        }
        insertAggregation(lastTransactionWiseModels);
        return searchScrollResponse.getScrollId();
    }

    private String generateResponse(SearchRequest searchRequest) {
        List<LastTransactionWiseModel> lastTransactionWiseModels = new ArrayList<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.debug("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);


        if (status == RestStatus.OK) {
            SearchHits searchHits= searchResponse.getHits();
            for(SearchHit searchHit: searchHits.getHits()) {

                Map<String,String> searchHitMap=searchHit.getSourceAsMap();
                Date dateTimeDay = DateFormatter.formatDate(searchHitMap.get("endTime"));
                String id = GenerateHash.createHashString(searchHitMap.get("senderResellerId"), searchHitMap.get("endTime"));
                String SenderBalance= searchHitMap.get("senderBalanceValueAfter");
                if( SenderBalance==null){
                    SenderBalance = "0.0";
                }
                LastTransactionWiseModel lastTransactionWiseModel = new LastTransactionWiseModel(id,searchHitMap.get("senderResellerId"),searchHitMap.get("senderMSISDN"),searchHitMap.get("senderResellerName"),searchHitMap.get("senderResellerType"),searchHitMap.get("senderAccountType"),1,Double.valueOf(SenderBalance),0.00d,dateTimeDay,searchHitMap.get("transactionProfile"),Double.valueOf(searchHitMap.get("transactionAmount")),searchHitMap.get("currency"),searchHitMap.get("receiverMSISDN"),searchHitMap.get("ersReference"),searchHitMap.get("senderRegion"));
                lastTransactionWiseModels.add(lastTransactionWiseModel);
            }

            insertAggregation( lastTransactionWiseModels);
            return searchResponse.getScrollId();

        }


    }

    private def insertAggregation(List lastTransactionWiseModels) {

        log.info("LastTransactionAggregation Aggregated into ${lastTransactionWiseModels.size()} rows.")
        if (lastTransactionWiseModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,reseller_id,reseller_msisdn,reseller_name,reseller_level,account_id,transaction_count,balance,total_credit,last_transaction_date,last_transaction_type,last_transaction_amount,last_transaction_currency,receiver_msisdn,last_transaction_id,region) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"+
                    " ON DUPLICATE KEY UPDATE balance = VALUES(balance),total_credit = values(total_credit)+total_credit,last_transaction_date = VALUES(last_transaction_date),last_transaction_type = VALUES(last_transaction_type),last_transaction_currency = VALUES(last_transaction_currency),last_transaction_id = VALUES(last_transaction_id),receiver_msisdn = VALUES(receiver_msisdn),last_transaction_amount = VALUES(last_transaction_amount)"
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = lastTransactionWiseModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.resellerMSISDN)
                        ps.setString(++index, row.resellerName)
                        ps.setString(++index, row.resellerLevel)
                        ps.setString(++index, row.accountId)
                        ps.setInt(++index, row.transactionCount)
                        ps.setDouble(++index, row.balance)
                        ps.setDouble(++index, row.totalCredit)
                        ps.setDate(++index, new java.sql.Date(row.lastTransactionDate.getTime()))
                        ps.setString(++index, row.lastTransactionType)
                        ps.setDouble(++index, row.lastTransactionAmount)
                        ps.setString(++index, row.lastTransactionCurrency)
                        ps.setString(++index, row.receiverMSISDN)
                        ps.setString(++index, row.lastTransactionId)
                        ps.setString(++index, row.region)

                    },
                    getBatchSize: { lastTransactionWiseModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }

    private def updateResellerTransfIn(List aggregation)
    {
        log.debug("Start Updating Reseller Transfer In ")
        if(aggregation)
        {
            def sql="UPDATE ${TABLE} SET total_credit=? WHERE reseller_id =?"
            aggregation.eachWithIndex { row, index ->

                def updateStatement = jdbcTemplate.update(sql, [setValues: { ps ->
                    ps.setString(1, row.balance_transfer_in.toString())
                    ps.setString(2, row.reseller_id.toString())
                }] as PreparedStatementSetter)
            }
        }
        log.debug("Finish Updating Reseller Transfer In ")
    }

}
class LastTransactionWiseModel {
    private String id;
    private String resellerId;
    private String resellerMSISDN;
    private String resellerName;
    private String resellerLevel;
    // private String accountTypeId;
    private String accountId;
    private int transactionCount;
    private double balance;
    private double totalCredit;
    private Date lastTransactionDate;
    private String lastTransactionType;
    private double lastTransactionAmount;
    private String lastTransactionCurrency;
    private String receiverMSISDN;
    private String lastTransactionId;
    private String region;

    public LastTransactionWiseModel(String id, String resellerId, String resellerMSISDN, String resellerName, String resellerLevel, String accountId, int transactionCount, double balance, double totalCredit, Date lastTransactionDate, String lastTransactionType, double lastTransactionAmount, String lastTransactionCurrency, String receiverMSISDN, String lastTransactionId, String region) {
        this.id = id;
        this.resellerId = resellerId;
        this.resellerMSISDN = resellerMSISDN;
        this.resellerName = resellerName;
        this.resellerLevel = resellerLevel;
        //this.accountTypeId = accountTypeId;
        this.accountId = accountId;
        this.transactionCount = transactionCount;
        this.balance = balance;
        this.totalCredit = totalCredit;
        this.lastTransactionDate = lastTransactionDate;
        this.lastTransactionType = lastTransactionType;
        this.lastTransactionAmount = lastTransactionAmount;
        this.lastTransactionCurrency = lastTransactionCurrency;
        this.receiverMSISDN = receiverMSISDN;
        this.lastTransactionId = lastTransactionId;
        this.region = region;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResellerId() {
        return resellerId;
    }

    public void setResellerId(String resellerId) {
        this.resellerId = resellerId;
    }

    public String getResellerMSISDN() {
        return resellerMSISDN;
    }

    public void setResellerMSISDN(String resellerMSISDN) {
        this.resellerMSISDN = resellerMSISDN;
    }

    public String getResellerName() {
        return resellerName;
    }

    public void setResellerName(String resellerName) {
        this.resellerName = resellerName;
    }

    public String getResellerLevel() {
        return resellerLevel;
    }

    public void setResellerLevel(String resellerLevel) {
        this.resellerLevel = resellerLevel;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(double totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Date getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(Date lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public String getLastTransactionType() {
        return lastTransactionType;
    }

    public void setLastTransactionType(String lastTransactionType) {
        this.lastTransactionType = lastTransactionType;
    }

    public double getLastTransactionAmount() {
        return lastTransactionAmount;
    }

    public void setLastTransactionAmount(double lastTransactionAmount) {
        this.lastTransactionAmount = lastTransactionAmount;
    }
    public String getLastTransactionCurrency() {
        return lastTransactionCurrency;
    }

    public void setLastTransactionCurrency(String lastTransactionCurrency) {
        this.lastTransactionCurrency = lastTransactionCurrency;
    }
    public String getReceiverMSISDN() {
        return receiverMSISDN;
    }

    public void setReceiverMSISDN(String receiverMSISDN) {
        this.receiverMSISDN = receiverMSISDN;
    }

    public String getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(String lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}