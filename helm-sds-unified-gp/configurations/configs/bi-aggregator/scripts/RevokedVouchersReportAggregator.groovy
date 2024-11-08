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
 */
@Slf4j
//@DynamicMixin
public class RevokedVouchersReportAggregator extends AbstractAggregator {
	static final def TABLE = "revoked_voucher_reseller_denom_day_wise"

	static class ReportAggregation {
		private String id;
		private String resellerId;
		private String denomination;
		private long quantity;
		private Date revocationDate;
		
		public ReportAggregation(String resellerId, String denomination, Date revocationDate, long quantity) {
			this.id = GenerateHash.createHashString(resellerId, denomination, revocationDate.toString());
			this.resellerId = resellerId;
			this.denomination = denomination;
			this.revocationDate = revocationDate;
			this.quantity = quantity;
		}
		
		public String getId() {
			return id;
		}

		public String getResellerId() {
			return resellerId;
		}

		public String getDenomination() {
			return denomination;
		}
		
		public long getQuantity() {
			return quantity;
		}

		public Date getRevocationDate() {
			return revocationDate;
		}

		@Override
		public String toString() {
			return "ReportAggregation{" +
					"id='" + id + '\'' +
					", resellerId='" + resellerId + '\'' +
					", denomination=" + denomination + '\'' +
					", quantity='" + quantity + '\'' +
					", revocationDate=" + revocationDate + '\'' +
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
			
	@Value('${RevokedVouchersReportAggregator.lookBackPeriodDays:5}')
	String lookBackPeriodDays;

	@Value('${RevokedVouchersReportAggregator.revokedState:BLOCKED}')
	String revokedState;

	private static final String QUERY =
		"select	IFNULL(v.owner_id, '') as owner_id, v.unit_value, date(v.last_modified) as revoke_date, count(*) as quantity " +
		"from 	dwa_vouchers v " +
			"inner join dwa_voucher_states vs on v.vouchers_status_id = vs.voucher_state_code " +
		"where vs.voucher_name = ? " +
		  "and v.last_modified >= DATE_ADD(DATE(now()), interval ? DAY) " +
		"group by IFNULL(v.owner_id, ''), v.unit_value, date(v.last_modified) " +
		"order by 3;";
	
	@Transactional
	@Scheduled(cron = '${RevokedVouchersReportAggregator.cron:*/3 * * * * ?}')

	public void aggregate() {

		log.info("RevokedVouchersReportAggregator Aggregator started***************************************************************************" + new Date());
		log.info("lookBackPeriodDays: " + lookBackPeriodDays);
		
		List<ReportAggregation> aggList = new ArrayList<>();
		
		JdbcTemplate voucherDbJdbcTemplate = new JdbcTemplate(createDatasource());

		long startTime = System.currentTimeMillis();
		def queryResult = voucherDbJdbcTemplate.queryForList(QUERY, revokedState, -Integer.parseInt(lookBackPeriodDays)); // negative value
		
		if (queryResult) {
			queryResult.eachWithIndex { row, index ->
				String ownerId = ("".equals(row.owner_id)) ? null : row.owner_id; // we need NULL in aggregation table so we can display 'N/A' in report
				String denom = String.valueOf((row.unit_value == (long)row.unit_value) ? (long)row.unit_value : row.unit_value); // cut float 0 decimal (if any)
				ReportAggregation reportAggregation = new ReportAggregation(ownerId, denom, row.revoke_date, row.quantity);
				aggList.add(reportAggregation);
		   }
	   }
	   log.info("aggregation data obtained in " + (System.currentTimeMillis() - startTime) + " ms");
	   
	   startTime = System.currentTimeMillis();
	   insertAggregation(aggList);
	   log.info("aggregation data wrriten in " + (System.currentTimeMillis() - startTime) + " ms");

		log.info("RevokedVouchersReportAggregator Aggregator ended**************************************************************************");
	}

	private def createDatasource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(vouchersdbDriver);
		dataSource.setUrl(vouchersdbURL);
		dataSource.setUsername(vouchersdbUsername);
		dataSource.setPassword(DatabaseUtil.decryptPasswordIfEncryptionEnabled(vouchersdbPasswordEncryptionEnabled, vouchersdbPassword));
		
		return dataSource;
	}
	
	private def insertAggregation(List<ReportAggregation> aggList) {

		log.info("ResellerUnredeemedVoucherAggregator Aggregated into ${aggList.size()} rows.")
		if (aggList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,revocationDate,denomination,resellerId,quantity) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = aggList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setDate(++index, row.revocationDate)
						ps.setString(++index, row.denomination)
						ps.setString(++index, row.resellerId)
						ps.setLong(++index, row.quantity)
					},
					getBatchSize: { aggList.size() }
			] as BatchPreparedStatementSetter)
		}
	}
}