
package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.util.DatabaseUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class SystemVoucherStockReportAggregator extends AbstractAggregator {
    static final def TABLE = "vouchers_stock"

    static class SystemVoucherStockReportAggregatorModel {
		private String id;
        private String denomination;
        private long totalStock;
        private long totalRedeemed;
        private long totalUnredeemed;       
        private long totalExpired;
        private long totalRevoked;

        public SystemVoucherStockReportAggregatorModel(String denomination, long totalStock, long totalRedeemed, long totalUnredeemed, long totalExpired, long totalRevoked) {
            this.id = GenerateHash.createHashString(denomination);
            this.denomination = denomination;
            this.totalStock = totalStock;
            this.totalRedeemed = totalRedeemed;
            this.totalUnredeemed = totalUnredeemed;
            this.totalExpired = totalExpired;
            this.totalRevoked = totalRevoked;
        }
		
		public String getId() {
            return id;
        }

        public String getDenomination() {
            return denomination;
        }

        public long getTotalStock() {
            return totalStock;
        }

        public long getTotalRedeemed() {
            return totalRedeemed;
        }
        
        public long getTotalUnredeemed() {
            return totalUnredeemed;
        }

        public long getTotalExpired() {
            return totalExpired;
        }

        public void setTotalExpired(long totalExpired) {
            this.totalExpired = totalExpired;
        }

        public long getTotalRevoked() {
            return totalRevoked;
        }

        @Override
        public String toString() {
            return "SystemVoucherStockReportAggregatorModel{" +
                    "id='" + id + '\'' +
                    ", denomination=" + denomination + '\'' +
                    ", totalStock=" + totalStock + '\'' +
                    ", totalRedeemed=" + totalRedeemed + '\'' +                   
                    ", totalUnredeemed='" + totalUnredeemed + '\'' +
                    ", totalExpired=" + totalExpired + '\'' +
                    ", totalRevoked=" + totalRevoked + '\'' +                          
                    '}';
        }		
	}

    @Autowired
    protected JdbcTemplate jdbcTemplate;

	@Value('${db.vouchersdb.driver}')
	String vouchersdbDriver;

	@Value('${db.vouchersdb.url}')
	String vouchersdbURL;

	@Value('${db.vouchersdb.username}')
	String vouchersdbUsername;

	@Value('${db.vouchersdb.password}')
	String vouchersdbPassword;

	@Value('${db.vouchersdb.db_password_encryption_enabled:false}')
	Boolean vouchersdbPasswordEncryptionEnabled;
	
    @Transactional
    @Scheduled(cron = '${SystemVoucherStockReportAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("SystemVoucherStockReportAggregator Aggregator started***************************************************************************" + new Date());
        
        try {
            def denominationStateSql = "select unit_value as 'denomination', voucher_name as 'state', count(*) as 'count' from dwa_vouchers, dwa_voucher_states " +
                                       "where vouchers_status_id = voucher_state_code group by denomination, state order by 1, 2";

			List<SystemVoucherStockReportAggregatorModel> aggregationList = new ArrayList<SystemVoucherStockReportAggregatorModel>();
									   
			JdbcTemplate voucherDbJdbcTemplate = new JdbcTemplate(createDatasource());
			
			long startTime = System.currentTimeMillis();
            def denominationStateResult = voucherDbJdbcTemplate.queryForList(denominationStateSql);			
            if (denominationStateResult) {								
                String denominationPrevious = null;
                String denomination = null;
                String state = null;
                long totalStock = 0L;
                long totalRedeemed = 0L;
                long totalUnredeemed = 0L;
                long totalRevoked = 0L;

                denominationStateResult.eachWithIndex { row, index ->
					denomination = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)					
                    state = row.state;
                    Long count = (long)row.count;
                    if (denominationPrevious != null && !denominationPrevious.equals(denomination)) {
                        SystemVoucherStockReportAggregatorModel systemVoucherStockReportAggregatorModel = new SystemVoucherStockReportAggregatorModel(denominationPrevious, totalStock, totalRedeemed, totalUnredeemed, 0, totalRevoked);
                        aggregationList.add(systemVoucherStockReportAggregatorModel);
                        totalStock = 0L;
                        totalRedeemed = 0L;
                        totalUnredeemed = 0L;
                        totalRevoked = 0L;
                    }
 
                    totalStock += count;
                    if ('REDEEMED'.equals(state)) {
                       totalRedeemed = count;
                    } else {
                        totalUnredeemed += count;
                        if ('BLOCKED'.equals(state)){
                            totalRevoked = count;
                        }
                    }
                    
                    denominationPrevious = denomination;

                }

                //add also the last one
                SystemVoucherStockReportAggregatorModel systemVoucherStockReportAggregatorModel = new SystemVoucherStockReportAggregatorModel(denomination, totalStock, totalRedeemed, totalUnredeemed, 0, totalRevoked);
                aggregationList.add(systemVoucherStockReportAggregatorModel);
            }
			log.info("stock data obtained in " + (System.currentTimeMillis() - startTime) + " ms");
			
            def denominationExpiredSql = "select unit_value as 'denomination', count(*) as 'countExpiry' from dwa_vouchers, dwa_voucher_states " +
                                         "where vouchers_status_id = voucher_state_code and voucher_name <> 'REDEEMED' and expiry_date < now() group by denomination order by 1";
			
			startTime = System.currentTimeMillis();
            def denominationStateResultExpired = voucherDbJdbcTemplate.queryForList(denominationExpiredSql);
            if (denominationStateResultExpired) {
                denominationStateResultExpired.eachWithIndex { row, index ->
					String denomination = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)
                    for (SystemVoucherStockReportAggregatorModel stockReportModelItem : aggregationList) {
                        if (denomination.equals(stockReportModelItem.getDenomination())) {
                            stockReportModelItem.setTotalExpired((long)row.countExpiry);
                        }
                    }

                }
            }
			log.info("expiry data obtained in " + (System.currentTimeMillis() - startTime) + " ms");
			
			startTime = System.currentTimeMillis();			
            insertAggregation(aggregationList);         
			log.info("aggregation data wrriten in " + (System.currentTimeMillis() - startTime) + " ms");
			
        } catch (Exception e) {
			log.error(e.getMessage())
		}

        log.info("SystemVoucherStockReportAggregator Aggregator ended**************************************************************************");
    }

	private def createDatasource() {	
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(vouchersdbDriver);
		dataSource.setUrl(vouchersdbURL);
		dataSource.setUsername(vouchersdbUsername);
		dataSource.setPassword(DatabaseUtil.decryptPasswordIfEncryptionEnabled(vouchersdbPasswordEncryptionEnabled, vouchersdbPassword));
		
		return dataSource;
	}
	
    private def insertAggregation(List aggregationList) {

        log.info("SystemVoucherStockReportAggregator Aggregated into ${aggregationList.size()} rows.")
        if (aggregationList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,denomination,totalStock,totalRedeemed,totalUnredeemed,totalExpired,totalRevoked) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE" + 
                      " totalStock = VALUES(totalStock), totalRedeemed = VALUES(totalRedeemed), totalUnredeemed = VALUES(totalUnredeemed), totalExpired = VALUES(totalExpired), totalRevoked = VALUES(totalRevoked)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = aggregationList[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.denomination)
                        ps.setLong(++index, row.totalStock)
                        ps.setLong(++index, row.totalRedeemed)
                        ps.setLong(++index, row.totalUnredeemed)
                        ps.setLong(++index, row.totalExpired)
                        ps.setLong(++index, row.totalRevoked)                     
                    },
                    getBatchSize: { aggregationList.size() }
            ] as BatchPreparedStatementSetter)
        }

    }
}
