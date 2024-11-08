
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
public class UnredeemedVouchersAgeingReportAggregator extends AbstractAggregator {
	static final def TABLE = "unredeemed_voucher_age_denom_day_wise"

	static class ReportAggregation {
		private String id;
		private String age;
		private String denomination;
		private long totalUnredeemed;
		private long totalExpired;
		private long totalRevoked;

		public ReportAggregation(String age, String denomination, long totalUnredeemed, long totalRevoked) {
			this.id = GenerateHash.createHashString(age, denomination);
			this.age = age;
			this.denomination = denomination;
			this.totalUnredeemed = totalUnredeemed;
			this.totalExpired = 0;
			this.totalRevoked = totalRevoked;
		}
		
		public String getAge() {
			return age;
		}
		
		public String getId() {
			return id;
		}

		public String getDenomination() {
			return denomination;
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
			return "ReportAggregation{" +
					"id='" + id + '\'' +
					", age=" + age + '\'' +
					", denomination=" + denomination + '\'' +
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
	@Scheduled(cron = '${UnredeemedVouchersAgeingReportAggregator.cron:*/3 * * * * ?}')
	public void aggregate() {

		log.info("UnredeemedVouchersAgeingReportAggregator Aggregator started***************************************************************************" + new Date());
		
		try {
			def denominationStateSql = 
				"select case " + 
							"when created >= DATE_ADD(DATE(now()), interval -1 MONTH) then '1MONTH' " + 
							"when created >= DATE_ADD(DATE(now()), interval -3 MONTH) then '3MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -6 MONTH) then '6MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -9 MONTH) then '9MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -1 YEAR) then '1YEAR' " +
							"else '1YEAR_OVER' " + 
							"end as age, unit_value as denomination, voucher_name as state, count(*) as count " +
				"from dwa_vouchers, dwa_voucher_states " +
				"where vouchers_status_id = voucher_state_code " +
				"  and voucher_name <> 'REDEEMED' " +
				"group by age, denomination, state " +
				"order by 1, 2";

			List<ReportAggregation> aggregationList = new ArrayList<ReportAggregation>();
									   
			JdbcTemplate voucherDbJdbcTemplate = new JdbcTemplate(createDatasource());
			
			long startTime = System.currentTimeMillis();
			def denominationStateResult = voucherDbJdbcTemplate.queryForList(denominationStateSql);
			if (denominationStateResult) {
				String agePrevious = null;
				String denominationPrevious = null;
				String age = null;
				String denomination = null;
				String state = null;
				long totalUnredeemed = 0L;
				long totalRevoked = 0L;

				denominationStateResult.eachWithIndex { row, index ->
					age = row.age;
					denomination = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)
					state = row.state;
					Long count = (long)row.count;
					
					if (agePrevious != null && !agePrevious.equals(age) || denominationPrevious != null && !denominationPrevious.equals(denomination)) {
						ReportAggregation reportAggregation = new ReportAggregation(agePrevious, denominationPrevious, totalUnredeemed, totalRevoked);
						aggregationList.add(reportAggregation);
						totalUnredeemed = 0L;
						totalRevoked = 0L;
					}
 
					totalUnredeemed += count;
					if ('BLOCKED'.equals(state)){
						totalRevoked = count;
					}
					
					agePrevious =  age;
					denominationPrevious = denomination;
				}
				
				//add also the last one
				ReportAggregation reportAggregation = new ReportAggregation(age, denomination, totalUnredeemed, totalRevoked);
				aggregationList.add(reportAggregation);
			}
			log.info("stock data obtained in " + (System.currentTimeMillis() - startTime) + " ms");

			def denominationExpiredSql =
				"select case " +
							"when created >= DATE_ADD(DATE(now()), interval -1 MONTH) then '1MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -3 MONTH) then '3MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -6 MONTH) then '6MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -9 MONTH) then '9MONTH' " +
							"when created >= DATE_ADD(DATE(now()), interval -1 YEAR) then '1YEAR' " +
							"else '1YEAR_OVER' " +
							"end as age, unit_value as denomination, count(*) as countExpiry " +
				"from dwa_vouchers, dwa_voucher_states " +
				"where vouchers_status_id = voucher_state_code " +
				"  and voucher_name <> 'REDEEMED' " +
				"  and expiry_date < now() " +
				"group by age, denomination ";
			
			startTime = System.currentTimeMillis();
			def denominationStateResultExpired = voucherDbJdbcTemplate.queryForList(denominationExpiredSql);
			if (denominationStateResultExpired) {
				denominationStateResultExpired.eachWithIndex { row, index ->
					String age = row.age;
					String denomination = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)
					for (ReportAggregation reportAggregation : aggregationList) {
						if (age.equals(reportAggregation.getAge()) && denomination.equals(reportAggregation.getDenomination())) {
							reportAggregation.setTotalExpired((long)row.countExpiry);
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

		log.info("UnredeemedVouchersAgeingReportAggregator Aggregator ended**************************************************************************");
	}

	private def createDatasource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(vouchersdbDriver);
		dataSource.setUrl(vouchersdbURL);
		dataSource.setUsername(vouchersdbUsername);
		dataSource.setPassword(DatabaseUtil.decryptPasswordIfEncryptionEnabled(vouchersdbPasswordEncryptionEnabled, vouchersdbPassword));
		
		return dataSource;
	}
	
	private def insertAggregation(List<ReportAggregation> aggregationList) {

		log.info("UnredeemedVouchersAgeingReportAggregator Aggregated into ${aggregationList.size()} rows.")
		if (aggregationList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,age,denomination,totalUnredeemed,totalExpired,totalRevoked) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE" +
					  "  totalUnredeemed = VALUES(totalUnredeemed), totalExpired = VALUES(totalExpired), totalRevoked = VALUES(totalRevoked)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = aggregationList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setString(++index, row.age)
						ps.setString(++index, row.denomination)
						ps.setLong(++index, row.totalUnredeemed)
						ps.setLong(++index, row.totalExpired)
						ps.setLong(++index, row.totalRevoked)
					},
					getBatchSize: { aggregationList.size() }
			] as BatchPreparedStatementSetter)
		}
	}
}
