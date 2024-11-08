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
public class VouchersExpiryReportAggregator extends AbstractAggregator {
	static final def TABLE = "voucher_expiry_denom_day_wise"
	static final def AGG_NAME = "VouchersExpiryReportAggregator";
	
	static class ReportAggregation {
		private String id;
		private String denomination;
		private Date expiryDate;
		private long quantity;
		
		public ReportAggregation(String denomination, Date expiryDate, long quantity) {
			this.id = GenerateHash.createHashString(denomination, expiryDate.toString());
			this.denomination = denomination;
			this.expiryDate = expiryDate;
			this.quantity = quantity;
		}
		
		public String getId() { return id; }
		public String getDenomination() { return denomination; }
		public Date getExpiryDate() { return expiryDate; }
		public long getQuantity() { return quantity; }
		
		@Override
		public String toString() {
			return "ReportAggregation {" +
					"id='" + id + '\'' +
					", denomination=" + denomination + '\'' +
					", expiryDate='" + expiryDate + '\'' +
					", quantity='" + quantity + '\'' +
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
	@Scheduled(cron = '${VouchersExpiryReportAggregator.cron:*/3 * * * * ?}')
	
	public void aggregate() {
		Date currentTime = new Date();
		
		log.info(AGG_NAME + " Aggregator started ************************************" + currentTime);

		def inputSql = "select unit_value as denomination, DATE(expiry_date) as expiry_date, count(*) as quantity " +
						"from dwa_vouchers, dwa_voucher_states " +
						"where voucher_state_code = vouchers_status_id and voucher_name <> 'REDEEMED' " +
						"group by unit_value, DATE(expiry_date)";
		
		List<ReportAggregation> reportAggregationList = new ArrayList<>();
		
		JdbcTemplate voucherDbJdbcTemplate = new JdbcTemplate(createDatasource());
		
		long startTime = System.currentTimeMillis();
		def selectResult = voucherDbJdbcTemplate.queryForList(inputSql);
		if (selectResult) {
			selectResult.eachWithIndex { row, index ->
				String denom = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)
				ReportAggregation aggregation = new ReportAggregation(denom, row.expiry_date, (long)row.quantity);
				reportAggregationList.add(aggregation);
		   }
	   }
	   log.info("aggregation data obtained in " + (System.currentTimeMillis() - startTime) + " ms");
	   
	   startTime = System.currentTimeMillis();
	   insertAggregation(reportAggregationList);
	   log.info("aggregation data wrriten in " + (System.currentTimeMillis() - startTime) + " ms");

		log.info(AGG_NAME + " Aggregator ended******************************" + new Date());
	}

	private def createDatasource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(vouchersdbDriver);
		dataSource.setUrl(vouchersdbURL);
		dataSource.setUsername(vouchersdbUsername);
		dataSource.setPassword(DatabaseUtil.decryptPasswordIfEncryptionEnabled(vouchersdbPasswordEncryptionEnabled, vouchersdbPassword));

		return dataSource;
	}

	private def insertAggregation(List<ReportAggregation> reportAggregationList) {

		log.info("${AGG_NAME} aggregated into ${reportAggregationList.size()} rows.")
		
		if (reportAggregationList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,denomination,quantity,expiryDate) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = reportAggregationList[i]
						def index = 0
						ps.setString(++index, row.getId())
						ps.setString(++index, row.getDenomination())
						ps.setLong(++index, row.getQuantity())
						ps.setDate(++index, new java.sql.Date(row.getExpiryDate().getTime()))
					},
					getBatchSize: { reportAggregationList.size() }
			] as BatchPreparedStatementSetter)
		}
	}
}
