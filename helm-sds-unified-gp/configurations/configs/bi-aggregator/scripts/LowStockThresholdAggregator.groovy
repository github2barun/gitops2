package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.ers.interfaces.platform.internal.suggestedstock.SuggestedStockProvider
import com.seamless.ers.interfaces.platform.internal.suggestedstock.response.SuggestedStockResponse
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.Timestamp


@Slf4j
public class LowStockThresholdAggregator  extends AbstractAggregator {
    static final def TABLE = "low_stock_alert_aggregation"

    static final def ACCOUNTSSQL = "select accountId as accountId, balance as balance from accounts.accounts where status='Active' and accountId=? and accountTypeId='RESELLER'"

    static final def AGENT_SUP_LOC = "select agent_msisdn as agentMSISDN, agent_name as agentName from Refill.agent_supervisor_loc_map where agent_region=? and agent_location=? and agent_cluster=? and agent_MSISDN=?"

    static final def RESELLERSQL = """
        SELECT c.tag AS resellerId, 
        dev.address AS resellerMSISDN,
        c.name AS resellerName,
        c.chain_store_id AS resellerNationalId,
        rt.id AS resellerType,
        co.name AS resellerContractId,
        c.reseller_path AS resellerPath,
        c.rgroup AS resellerZone,
        c.last_modified,
        c.type_key as resellerLevel,
        c.rgroup as resellerRegion,
        c.subrgroup as resellerLocation,
        c.subsubrgroup as resellerCluster
        FROM Refill.commission_receivers c
        LEFT JOIN Refill.extdev_devices as dev
        ON (dev.owner_key = c.receiver_key)
        LEFT JOIN Refill.commission_contracts co ON (co.contract_key = c.contract_key)
        LEFT JOIN Refill.reseller_types rt ON (rt.type_key = c.type_key)
        LEFT JOIN Refill.pay_prereg_accounts pa ON (pa.owner_key=c.receiver_key) group by resellerMSISDN,resellerName,resellerRegion,resellerLocation,resellerCluster
        """

    static final def dateFormat = "yyyy-MM-dd HH:mm:ss"

    private def stockReportRecords

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Autowired
    @Qualifier(value="refill")
    private JdbcTemplate refill

    @Autowired
    @Qualifier(value="accounts")
    private JdbcTemplate accounts

    @Autowired(required=false)
    SuggestedStockProvider stockService;

    @Transactional
    @Scheduled(cron  = '${LowStockThresholdAggregator.cron:0 0/30 * * * ?}')
    public void aggregate() {
        def date = getDate()
        def threshold
        def accountTransactions
        def agentInformation
        def stockReportMap
        stockReportRecords = new ArrayList()
        def resellers = refill.query(RESELLERSQL, [setValues: { ps ->
            use(TimeCategory) {
            }
        }] as PreparedStatementSetter, new ColumnMapRowMapper())

        log.debug("Got ${resellers.size()} resellers from refill.")

        for (resellerRecord in resellers) {

            if(resellerRecord.resellerMSISDN == null || resellerRecord.resellerMSISDN.equals(""))
                continue
            threshold =getThresholdStock(new String(resellerRecord.resellerMSISDN))
            if(threshold != null && !threshold.isNaN()){
                accountTransactions = accounts.query(ACCOUNTSSQL, [setValues: { ps ->
                    use(TimeCategory) {
                        ps.setString(1, resellerRecord.resellerId)
                    }
                }] as PreparedStatementSetter, new ColumnMapRowMapper())

                log.debug("Received ${accountTransactions.size()} account transactions from accounts")

                agentInformation = refill.query(AGENT_SUP_LOC, [setValues : { ps ->
                    use(TimeCategory) {
                        ps.setString(1, resellerRecord.resellerRegion)
                        ps.setString(2, resellerRecord.resellerLocation)
                        ps.setString(3, resellerRecord.resellerCluster)
                        ps.setString(4, resellerRecord.resellerMSISDN)
                    }
                }] as PreparedStatementSetter, new ColumnMapRowMapper())

                log.debug("Received ${agentInformation.size()} agent record/s from table agent_supervisor_loc_map")

                if(accountTransactions.size() == 1 && agentInformation.size() == 1) {
                    if(threshold > accountTransactions[0].balance) {
                        stockReportMap = [
                                aggregationDate : (date),
                                resellerId : resellerRecord.resellerId,
                                resellerMSISDN : resellerRecord.resellerMSISDN,
                                resellerName : resellerRecord.resellerName,
                                agentMSISDN : agentInformation[0].agentMSISDN,
                                agentName : agentInformation[0].agentName,
                                resellerRegion : resellerRecord.resellerRegion,
                                resellerLocation : resellerRecord.resellerLocation,
                                resellerCluster : resellerRecord.resellerCluster,
                                stockThreshold : threshold,
                                stockLevel : accountTransactions[0].balance
                        ]
                        stockReportRecords.add(stockReportMap)
                    }
                    else if(threshold == null){
                        log.debug("Call to Threshold Stock service returned null")
                    }
                }
                else if(accountTransactions.size() > 1)
                    log.error("Two or more accounts exists with same reseller Id " + resellerRecord.resellerId + ", so excluded the Reseller");
                else if(agentInformation.size() > 1)
                    log.error("Two or more agents belong to same "+resellerRecord.resellerRegion +" : " + resellerRecord.resellerLocation + " : " + resellerRecord.resellerCluster)
            }
            else
                log.debug("Excluded reseller " + resellerRecord.resellerId + "from the report")

        }
        updateAggregation()
    }

    private def updateAggregation() {
        log.debug("Aggregated into ${stockReportRecords.size()} rows.")
        if(stockReportRecords) {
            def sql = "INSERT into ${TABLE} (aggregationDate, resellerMSISDN, resellerName, agentMSISDN, agentName, resellerRegion, resellerLocation, resellerCluster, stockThreshold, stockLevel, resellerId) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues: {    ps, i ->
                        def row = stockReportRecords[i]
                        def index = 0
                        ps.setTimestamp(++index, new Timestamp(System.currentTimeMillis()))
                        ps.setString(++index, row.resellerMSISDN)
                        ps.setString(++index, row.resellerName)
                        ps.setString(++index, row.agentMSISDN)
                        ps.setString(++index, row.agentName)
                        ps.setString(++index, row.resellerRegion)
                        ps.setString(++index, row.resellerLocation)
                        ps.setString(++index, row.resellerCluster)
                        ps.setBigDecimal(++index, row.stockThreshold)
                        ps.setBigDecimal(++index, row.stockLevel)
                        ps.setString(++index,row.resellerId)
                    },
                    getBatchSize: { stockReportRecords.size() }
            ] as BatchPreparedStatementSetter)
        }
    }

    private getDate = {

        def date = new Date()

        use(TimeCategory) { date = date }

        return date
    }

    public Double getThresholdStock(String resellerMSISDN)
    {
        SuggestedStockResponse stockResponse = null;
        Double stockThreshold = Double.NaN;
        if(stockService != null){
            stockResponse = stockService.getThreshold(resellerMSISDN);

            if(stockResponse !=null && stockResponse.getReturnValue() != null && stockResponse.getReturnValue().length() != 0){
                log.debug("Threshold stock for reseller " + resellerMSISDN + "\t" +stockResponse.getReturnValue());
                try{
                    stockThreshold = new Double(stockResponse.getReturnValue());
                }
                catch(NumberFormatException e)
                {
                    log.debug(resellerMSISDN+" might be configured as unmonitored reseller in alertapp.");
                    log.debug("Error in formating threshold value returned by Stock Service call for reseller: "+resellerMSISDN + e);
                    return Double.NaN;
                }
            }
            else{
                log.info("WebService Call to get threshold stock for reseller " + resellerMSISDN + "retruned null");
                return stockThreshold;
            }
        }else{
            log.error("Consumer for SuggestedStockService is not created");
            return stockThreshold;
        }
        return stockThreshold;
    }
}
