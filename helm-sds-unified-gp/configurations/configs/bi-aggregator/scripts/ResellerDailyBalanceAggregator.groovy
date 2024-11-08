package com.seamless.customer.bi.aggregator.aggregate

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
import org.springframework.beans.factory.annotation.Value

/**
 * Aggregates transaction rows per reseller with corresponding
 * balance before and after values.
 *
 * @author Tirthankar Mitra
 */

//@DynamicMixin
@Slf4j
public class ResellerDailyBalanceAggregator extends AbstractAggregator
{
    static final def DAILY_BALANCE_TABLE = "region_reseller_account_statement_daily_balance_aggregation"

    static final
    def ACCOUNTSSQL = "SELECT t.accountId AS resellerId,t.accountTypeId, t.balanceBefore, t.balanceAfter, t.createDate AS date  FROM transactions t WHERE t.createDate >= ? and t.createDate <= DATE_ADD(DATE_ADD(?,INTERVAL 1 DAY),INTERVAL -1 SECOND) ORDER BY t.transactionKey"

    static final def dateFormat = "yyyy-MM-dd HH:mm:ss"

    @Value('${ResellerDailyBalanceAggregator.limit:10}')
    int limit

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("accounts")
    private JdbcTemplate accounts

    @Value('${ResellerDailyBalanceAggregator.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${ResellerDailyBalanceAggregator.bulkInsertionModeFromDateString:2020-08-03}')
    String bulkInsertionModeFromDateString;

    @Value('${ResellerDailyBalanceAggregator.bulkInsertionModeToDateString:2020-08-09}')
    String bulkInsertionModeToDateString;

    @Transactional
    @Scheduled(cron = '${ResellerDailyBalanceAggregator.cron:*/20 * * * * ?}')
    public void aggregate()
    {
        log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
        log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);
//        def todayDate = new Date()
//        def date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
//        def tempDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())

        def accountTransactions = accounts.query(ACCOUNTSSQL, [setValues: { ps ->
            use(TimeCategory)
                    {
//                        tempDate = tempDate + limit
//                        if (todayDate.format(dateFormat) >= tempDate.format(dateFormat))
//                        {
//                            todayDate = tempDate
//                        }
                        // log.info("Fetching transaction from date : ${date} to : ${todayDate} ")
                        ps.setString(1, bulkInsertionModeFromDateString)
                        ps.setString(2, bulkInsertionModeToDateString)
                    }
        }] as PreparedStatementSetter, new ColumnMapRowMapper())

        log.info("Got ${accountTransactions.size()} account transactions from accounts")

        if (accountTransactions) {

            def sql = "insert into ${DAILY_BALANCE_TABLE} (aggregationDate, resellerId,accountTypeId, balanceBefore, balanceAfter) values (?, ?, ?, ?,?) ON DUPLICATE KEY UPDATE balanceAfter=VALUES(balanceAfter)"
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   :
                            { ps, i ->
                                def onlyDate = accountTransactions[i].date.clearTime()
                                def toSqlTimestamp =
                                        { Date onlyDate1 ->
                                            new java.sql.Timestamp(onlyDate1.time)
                                        }
                                log.debug("row: " + accountTransactions[i])
                                ps.setTimestamp(1, toSqlTimestamp(onlyDate))
                                ps.setString(2, accountTransactions[i].resellerId)
                                ps.setString(3, accountTransactions[i].accountTypeId)
                                ps.setBigDecimal(4, accountTransactions[i].balanceBefore)
                                ps.setBigDecimal(5, accountTransactions[i].balanceAfter)
                            },
                    getBatchSize:
                            { accountTransactions.size() }
            ] as BatchPreparedStatementSetter)
        }
        
    }

}