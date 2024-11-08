import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.Timestamp

@Slf4j
public class StdDealerDetailsAggregator extends AbstractAggregator {
    static final def TABLE = "std_dealer_detail_aggregation"
    static final def BALANCE = """SELECT
		c.name AS resellerName,
		c.tag AS resellerId,
		dev.address AS resellerMSISDN,
		c.reseller_path AS resellerPath,
		0 AS transferIn,
		0 AS totalPinlessTopups,
		0 AS totalPinTopups,
		c.time_created AS date,
		b.balance AS currentBalance,
		b.accountTypeId AS accountTypeId
		FROM accountmanagement.accounts b
		JOIN Refill.pay_prereg_accounts p ON (b.accountId = p.account_nr)
		JOIN Refill.commission_receivers c ON (p.owner_key = c.receiver_key)
		JOIN Refill.extdev_devices dev ON (c.receiver_key = dev.owner_key)
		JOIN Refill.commission_contracts d ON (d.contract_key = c.contract_key);
		"""

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${StdDealerDetailsAggregator.allowed_profiles:TOPUP,CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,SUPPORT_TRANSFER,PURCHASE}')
    String profileId

    @Value('${StdDealerDetailsAggregator.transfer_in_profiles:CREDIT_TRANSFER,SUPPORT_TRANSFER}')
    String transferInAllowedProfiles

    @Value('${StdDealerDetailsAggregator.topup_profiles:TOPUP}')
    String topupAllowedProfiles

    @Value('${StdDealerDetailsAggregator.purchase_profiles:PURCHASE}')
    String purchaseAllowedProfiles

    @Value('${StdDealerDetailsAggregator.reversal_profiles:REVERSE_CREDIT_TRANSFER}')
    String reversalAllowedProfiles

    @Value('${StdDealerDetailsAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${StdDealerDetailsAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${StdDealerDetailsAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Value('${StdDealerDetailsAggregator.deleteDataAfterDays:60}')
    Integer deleteDataAfterDays;

    @Value('${StdDealerDetailsAggregator.enableFetch:false}')
    private boolean enableFetch

    @Autowired
    @Qualifier("refill")
    private JdbcTemplate refill

    @Transactional
    @Scheduled(cron = '${StdDealerDetailsAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("*************************************************** StdDealerDetailsAggregator Aggregator started ********************************************" + new Date());
        def profileIdList = profileId.split(",")
        def transferInProfileList = transferInAllowedProfiles.split(",")
        def topUpProfilesList = topupAllowedProfiles.split(",")
        def purchaseProfilesList = purchaseAllowedProfiles.split(",")
        def reversalProfileList = reversalAllowedProfiles.split(",")


        if (enableFetch) {
            def resellers = refill.query(BALANCE, new ColumnMapRowMapper())

            log.info("Got StdDealerDetailsAggregator ${resellers.size()} activate reseller from refill.")
            def sqlReseller = """
			INSERT INTO ${TABLE} 
			(resellerId, transactions_date, resellerMSISDN, ResellerName, transferIn, total_pinless_topups, total_pin_topups, current_balance, reseller_path, account_type_id) 
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE 
			transferIn=transferIn+VALUES(transferIn),
			total_pinless_topups=total_pinless_topups+VALUES(total_pinless_topups),
			total_pin_topups=total_pin_topups+VALUES(total_pin_topups),
			current_balance=VALUES(current_balance),
			reseller_path=VALUES(reseller_path),
			ResellerName=VALUES(ResellerName),
			account_type_id=VALUES(account_type_id)
			"""
            if (resellers) {
                log.info("reseller exits")
                jdbcTemplate.batchUpdate(sqlReseller, [
                        setValues   : { ps, i ->
                            ps.setString(1, resellers[i].resellerId)
                            ps.setTimestamp(2, resellers[i].date.clearTime())
                            ps.setString(3, resellers[i].resellerMSISDN)
                            ps.setString(4, resellers[i].resellerName)
                            ps.setBigDecimal(5, resellers[i].transferIn)
                            ps.setBigDecimal(6, resellers[i].totalPinlessTopups)
                            ps.setBigDecimal(7, resellers[i].totalPinTopups)
                            ps.setBigDecimal(8, resellers[i].currentBalance)
                            ps.setString(9, resellers[i].resellerPath)
                            ps.setString(10, resellers[i].accountTypeId)
                        },
                        getBatchSize: { resellers.size() }
                ] as BatchPreparedStatementSetter)
                log.info("Data inserted in StdDealerDetailsAggregator")
            }
            enableFetch = false;
        }


        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);

            for (String index : indices) {
                //fetch data from ES
                try {
                    aggregateDataES(index, bulkInsertionModeFromDateString, bulkInsertionModeToDateString, profileIdList, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList);

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
                aggregateDataES(index.getIndexName(), index.getStartDate(), index.getEndDate(), profileIdList, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList);

            }
        }

        log.info("*************************************** StdDealerDetailsAggregator Aggregator ended ******************************************");
    }


    private void aggregateDataES(String index, String fromDate, String toDate, String[] profileIdList, String[] transferInProfileList, String[] topUpProfilesList, String[] purchaseProfilesList, String[] reversalProfileList) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, profileIdList);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(2));

        String scrollId = generateResponse(searchRequest, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList);
        SearchResponse searchScrollResponse = client.search(searchRequest, COMMON_OPTIONS);
        log.info("_________________hits size outside loop for the first time_____________________" + searchScrollResponse.getHits().size())

        while (searchScrollResponse.getHits().size() != 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueSeconds(30));
            log.info("******* Scroll Request:::: " + scrollRequest.toString());
            try {
                searchScrollResponse = client.scroll(scrollRequest, COMMON_OPTIONS);
            } catch (Exception e) {
                log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
            }


            log.info("_________________hits size inside loop _____________________" + searchScrollResponse.getHits().size())

            scrollId = generateScrollResponse(searchScrollResponse, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList);
        }
        cleanData()
    }


    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String[] profileID) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
            searchSourceBuilder.size(1000).sort("EndTime", SortOrder.ASC).query(queryBuilder);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("ResultStatus", "SUCCESS"))
                    .filter(QueryBuilders.termsQuery("TransactionProfile", profileID))
                    .filter(QueryBuilders.rangeQuery("EndTime").gte(fromDate).lt(toDate))
            searchSourceBuilder.size(1000).sort("EndTime", SortOrder.ASC).query(queryBuilder);
        }

        return searchSourceBuilder;
    }


    private String generateResponse(SearchRequest searchRequest, String[] transferInProfileList, String[] topUpProfilesList, String[] purchaseProfilesList, String[] reversalProfileList) {
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.info("******* Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.info("response status -------------" + status);
        if (status == RestStatus.OK) {
            SearchHits searchHits = searchResponse.getHits();
            log.info("Total search hits founds = " + searchHits.getHits().size())
            prepareAndInsertTransactions(searchHits, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList)
        }
        return searchResponse.getScrollId();
    }

    private String generateScrollResponse(SearchResponse searchScrollResponse, String[] transferInProfileList, String[] topUpProfilesList, String[] purchaseProfilesList, String[] reversalProfileList) {
        RestStatus status = searchScrollResponse.status();
        log.info("scroll response status -------------" + status);

        if (status == RestStatus.OK) {
            SearchHits searchHits = searchScrollResponse.getHits();
            log.info("no of hits after 1st request" + searchHits.size());
            prepareAndInsertTransactions(searchHits, transferInProfileList, topUpProfilesList, purchaseProfilesList, reversalProfileList)
        }
        return searchScrollResponse.getScrollId();
    }

    private def cleanData() {
        log.info("StdDealerDetailsAggregator --> cleaning data from before " + deleteDataAfterDays + " days.")
        def query = "DELETE FROM ${TABLE} WHERE transactions_date <= DATE_SUB(NOW() , INTERVAL ${deleteDataAfterDays} DAY)"
        jdbcTemplate.update(query)

    }


    private void prepareAndInsertTransactions(SearchHits searchHits, String[] transferInProfileList, String[] topUpProfilesList, String[] purchaseProfilesList, String[] reversalProfileList) {
        log.info("..... preparing to insert transactions ......")
        List<DealerDetailsModel> dealerDetailsModelArrayList = new ArrayList<>();
        for (SearchHit searchHit : searchHits.getHits()) {
            Map<String, String> searchHitMap = searchHit.getSourceAsMap()

            Date transactionDate = DateFormatter.formatDate(searchHitMap.get("EndTime"))
            Timestamp timestamp = new Timestamp(transactionDate.getTime())
            Timestamp date = timestamp.clearTime()
            log.info("date = " + date)
            String profile = searchHitMap.get("TransactionProfile")

            if (transferInProfileList.contains(profile) || reversalProfileList.contains(profile)) {
                String resellerId = searchHitMap.get("SenderResellerId")
                log.info("resellerId = " + resellerId)

                String resellerMSISDN = searchHitMap.get("SenderMSISDN")
                log.info("resellerMSISDN = " + resellerMSISDN)

                String resellerPath = searchHitMap.get("SenderResellerPath")
                log.info("resellerPath = " + resellerPath)

                String resellerName = searchHitMap.get("SenderResellerName")
                log.info("resellerName = " + resellerName)

                String parentResellerId = searchHitMap.get("SenderResellerParent")
                log.info("parentResellerId = " + parentResellerId)

                String accountTypeId = searchHitMap.get("SenderAccountType")
                log.info("accountTypeId = " + accountTypeId)

                Double currentBalance
                if (searchHitMap.get("SenderBalanceValueAfter") != null) {
                    currentBalance = Double.valueOf(searchHitMap.get("SenderBalanceValueAfter"))
                } else {
                    currentBalance = 0
                }
                log.info("currentBalance = " + currentBalance.toString())

                String receiverResellerId = searchHitMap.get("ReceiverResellerId")
                log.info("receiverResellerId = " + receiverResellerId)

                String receiverResellerMSISDN = searchHitMap.get("ReceiverMSISDN")
                log.info("receiverResellerMSISDN = " + receiverResellerMSISDN)

                String receiverResellerPath = searchHitMap.get("ReceiverResellerPath")
                log.info("receiverResellerPath = " + receiverResellerPath)

                String receiverResellerName = searchHitMap.get("ReceiverResellerName")
                log.info("receiverResellerName = " + receiverResellerName)

                String receiverParentResellerId = searchHitMap.get("ReceiverResellerParent")
                log.info("receiverParentResellerId = " + receiverParentResellerId)

                String receiverAccountTypeId = searchHitMap.get("ReceiverAccountType")
                log.info("receiverAccountTypeId = " + receiverAccountTypeId)

                Double receiverCurrentBalance
                if (searchHitMap.get("ReceiverBalanceValueAfter") != null) {
                    receiverCurrentBalance = Double.valueOf(searchHitMap.get("ReceiverBalanceValueAfter"))
                } else {
                    receiverCurrentBalance = 0
                }
                log.info("receiverCurrentBalance = " + receiverCurrentBalance.toString())

                DealerDetailsModel dealerDetailsModel = new DealerDetailsModel(date, resellerId, resellerMSISDN, resellerPath, resellerName, parentResellerId, currentBalance, accountTypeId)
                DealerDetailsModel receiverDealerDetailsModel = new DealerDetailsModel(date, receiverResellerId, receiverResellerMSISDN, receiverResellerPath, receiverResellerName, receiverParentResellerId, receiverCurrentBalance, receiverAccountTypeId)

                dealerDetailsModelArrayList.add(dealerDetailsModel)
                dealerDetailsModelArrayList.add(receiverDealerDetailsModel)
            }


            if (topUpProfilesList.contains(profile) || purchaseProfilesList.contains(profile)) {
                String resellerId = searchHitMap.get("SenderResellerId")
                log.info("resellerId = " + resellerId)

                String resellerMSISDN = searchHitMap.get("SenderMSISDN")
                log.info("resellerMSISDN = " + resellerMSISDN)

                String resellerPath = searchHitMap.get("SenderResellerPath")
                log.info("resellerPath = " + resellerPath)

                String resellerName = searchHitMap.get("SenderResellerName")
                log.info("resellerName = " + resellerName)

                String parentResellerId = searchHitMap.get("SenderResellerParent")
                log.info("parentResellerId = " + parentResellerId)

                String accountTypeId = searchHitMap.get("SenderAccountType")
                log.info("accountTypeId = " + accountTypeId)

                Double currentBalance
                if (searchHitMap.get("SenderBalanceValueAfter") != null) {
                    currentBalance = Double.valueOf(searchHitMap.get("SenderBalanceValueAfter"))
                } else {
                    currentBalance = 0
                }
                log.info("currentBalance = " + currentBalance.toString())

                DealerDetailsModel dealerDetailsModel = new DealerDetailsModel(date, resellerId, resellerMSISDN, resellerPath, resellerName, parentResellerId, currentBalance, accountTypeId)
                dealerDetailsModelArrayList.add(dealerDetailsModel)

            }

        }
        log.info("dealerDetailsModelArrayList size = " + dealerDetailsModelArrayList.size())
        insertAggregation(dealerDetailsModelArrayList);
    }

    private def insertAggregation(List aggregation)
    {
        log.info("Aggregated into ${aggregation.size()} rows.")
        if(aggregation)
        {
            def sql = """
			REPLACE INTO ${TABLE} 
			(resellerId, transactions_date, resellerMSISDN, ResellerName, transferIn, total_pinless_topups, total_pin_topups, current_balance, reseller_path, account_type_id) 
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			"""
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues:
                            { ps, i ->
                                def row = aggregation[i]
                                int index=0
                                ps.setString(++index, row.resellerId)
                                ps.setTimestamp(++index, row.date)
                                ps.setString(++index,row.resellerMSISDN)
                                ps.setString(++index,row.resellerName)
                                ps.setBigDecimal(++index,row.transferIn)
                                ps.setBigDecimal(++index,row.totalPinlessTopups)
                                ps.setBigDecimal(++index,row.totalPinTopups)
                                ps.setBigDecimal(++index,row.currentBalance)
                                ps.setString(++index,row.resellerPath)
                                ps.setString(++index,row.accountTypeId)
                            },
                    getBatchSize:
                            { aggregation.size() }
            ] as BatchPreparedStatementSetter)
        }
    }


    class DealerDetailsModel {
        private Timestamp date
        private String resellerId
        private String resellerMSISDN
        private String resellerPath
        private String resellerName
        private String parentResellerId
        private Double transferIn = 0
        private Double totalPinlessTopups = 0
        private Double totalPinTopups = 0
        private Double currentBalance
        private String accountTypeId

        DealerDetailsModel(Timestamp date, String resellerId, String resellerMSISDN, String resellerPath, String resellerName, String parentResellerId, Double currentBalance, String accountTypeId) {
            this.date = date
            this.resellerId = resellerId
            this.resellerMSISDN = resellerMSISDN
            this.resellerPath = resellerPath
            this.resellerName = resellerName
            this.parentResellerId = parentResellerId
            this.currentBalance = currentBalance
            this.accountTypeId = accountTypeId
        }

        Timestamp getDate() {
            return date
        }

        void setDate(Timestamp date) {
            this.date = date
        }

        String getResellerId() {
            return resellerId
        }

        void setResellerId(String resellerId) {
            this.resellerId = resellerId
        }

        String getResellerMSISDN() {
            return resellerMSISDN
        }

        void setResellerMSISDN(String resellerMSISDN) {
            this.resellerMSISDN = resellerMSISDN
        }

        String getResellerPath() {
            return resellerPath
        }

        void setResellerPath(String resellerPath) {
            this.resellerPath = resellerPath
        }

        String getResellerName() {
            return resellerName
        }

        void setResellerName(String resellerName) {
            this.resellerName = resellerName
        }

        String getParentResellerId() {
            return parentResellerId
        }

        void setParentResellerId(String parentResellerId) {
            this.parentResellerId = parentResellerId
        }

        Double getTransferIn() {
            return transferIn
        }

        void setTransferIn(Double transferIn) {
            this.transferIn = transferIn
        }

        Double getTotalPinlessTopups() {
            return totalPinlessTopups
        }

        void setTotalPinlessTopups(Double totalPinlessTopups) {
            this.totalPinlessTopups = totalPinlessTopups
        }

        Double getTotalPinTopups() {
            return totalPinTopups
        }

        void setTotalPinTopups(Double totalPinTopups) {
            this.totalPinTopups = totalPinTopups
        }

        Double getCurrentBalance() {
            return currentBalance
        }

        void setCurrentBalance(Double currentBalance) {
            this.currentBalance = currentBalance
        }

        String getAccountTypeId() {
            return accountTypeId
        }

        void setAccountTypeId(String accountTypeId) {
            this.accountTypeId = accountTypeId
        }
    }


}
