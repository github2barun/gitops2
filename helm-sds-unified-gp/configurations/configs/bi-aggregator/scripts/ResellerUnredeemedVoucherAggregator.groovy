
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
public class ResellerUnredeemedVoucherAggregator extends AbstractAggregator {
    static final def TABLE = "unredeemed_voucher_reseller_denom"

    static class ResellerUnredeemedVoucherAggregatorModel {
		private String id;
        private String resellerId;
        private String denomination;       
        private long totalUnredeemed;
        private long totalStock;

        public ResellerUnredeemedVoucherAggregatorModel(String resellerId, String denomination, long totalUnredeemed, long totalStock) {
            this.id = GenerateHash.createHashString(resellerId, denomination);
            this.resellerId = resellerId;
            this.denomination = denomination;
            this.totalUnredeemed = totalUnredeemed;
            this.totalStock = totalStock;
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
        
        public long getTotalUnredeemed() {
            return totalUnredeemed;
        }

        public long getTotalStock() {
            return totalStock;
        }

        @Override
        public String toString() {
            return "ResellerUnredeemedVoucherAggregatorModel{" +
                    "id='" + id + '\'' +
                    ", resellerId='" + resellerId + '\'' +
                    ", denomination=" + denomination + '\'' +                   
                    ", totalUnredeemed='" + totalUnredeemed + '\'' +
                    ", totalStock=" + totalStock + '\'' +                          
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
    @Scheduled(cron = '${ResellerUnredeemedVoucherAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("ResellerUnredeemedVoucherAggregator Aggregator started***************************************************************************" + new Date());
        
        try {
			def inputSql = 	"select a.resellerId, a.denomination, a.totalStock, IFNULL(b.totalUnredeemed, 0) as totalUnredeemed " +
							"from (" +
									"select	IFNULL(owner_id, '') as resellerId, unit_value as denomination, count(*) as totalStock " +
									"from 	dwa_vouchers, dwa_voucher_states " +
									"where 	voucher_state_code = vouchers_status_id " + 
									"group by IFNULL(owner_id, ''), unit_value " +
								  ") a left outer join " + 
								  "(" +
								  	"select 	IFNULL(owner_id, '') as resellerId, unit_value as denomination, count(vouchers_status_id) as totalUnredeemed " +
									 "from		dwa_vouchers, dwa_voucher_states " + 
									 "where 	voucher_state_code = vouchers_status_id " + 
									 	   "and voucher_name <> 'REDEEMED' " +
									 "group by IFNULL(owner_id, ''), unit_value" +
								  ") b " + 
								  "on a.resellerId = b.resellerId and a.denomination = b.denomination";
						   
			List<ResellerUnredeemedVoucherAggregatorModel> resellerUnredeemedVoucherAggregatorModels = new ArrayList<>();
		
			JdbcTemplate voucherDbJdbcTemplate = new JdbcTemplate(createDatasource());

			long startTime = System.currentTimeMillis();
			def resellerUnredeemedAggregation = voucherDbJdbcTemplate.queryForList(inputSql);		   
			if (resellerUnredeemedAggregation) {              
				resellerUnredeemedAggregation.eachWithIndex { row, index ->
					String resellerId = ("".equals(row.resellerId)) ? null : row.resellerId; // we need NULL in aggregation table so we can display 'N/A' in report				
					String denom = String.valueOf((row.denomination == (long)row.denomination) ? (long)row.denomination : row.denomination); // cut float 0 decimal (if any)
					
					ResellerUnredeemedVoucherAggregatorModel resellerUnredeemedVoucherAggregatorModel = new ResellerUnredeemedVoucherAggregatorModel(resellerId, denom, (long)row.totalUnredeemed, (long)row.totalStock);
					resellerUnredeemedVoucherAggregatorModels.add(resellerUnredeemedVoucherAggregatorModel);				
			   }   
           }           
		   log.info("aggregation data obtained in " + (System.currentTimeMillis() - startTime) + " ms");
		   
		   startTime = System.currentTimeMillis();
		   insertAggregation(resellerUnredeemedVoucherAggregatorModels);
		   log.info("aggregation data wrriten in " + (System.currentTimeMillis() - startTime) + " ms");
		   
        } catch (Exception e){
			log.error(e.getMessage())
		}

        log.info("ResellerUnredeemedVoucherAggregator Aggregator ended**************************************************************************");
    }

	private def createDatasource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(vouchersdbDriver);
		dataSource.setUrl(vouchersdbURL);
		dataSource.setUsername(vouchersdbUsername);
		dataSource.setPassword(DatabaseUtil.decryptPasswordIfEncryptionEnabled(vouchersdbPasswordEncryptionEnabled, vouchersdbPassword));
		
		return dataSource;
	}

    private def insertAggregation(List ResellerUnredeemedVoucherAggregatorModels) {

        log.info("ResellerUnredeemedVoucherAggregator Aggregated into ${ResellerUnredeemedVoucherAggregatorModels.size()} rows.")
        if (ResellerUnredeemedVoucherAggregatorModels.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,resellerId,denomination,totalUnredeemed,totalStock) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE totalUnredeemed = VALUES(totalUnredeemed), totalStock = VALUES(totalStock)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = ResellerUnredeemedVoucherAggregatorModels[i]
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.denomination)
                        ps.setLong(++index, row.totalUnredeemed)
                        ps.setLong(++index, row.totalStock)                     
                    },
                    getBatchSize: { ResellerUnredeemedVoucherAggregatorModels.size() }
            ] as BatchPreparedStatementSetter)
        }

    }
}