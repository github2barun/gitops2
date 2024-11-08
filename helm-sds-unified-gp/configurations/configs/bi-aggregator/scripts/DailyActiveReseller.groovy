import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.scheduling.annotation.Scheduled

import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat

@Slf4j
class DailyActiveReseller extends AbstractAggregator {

    static final def TABLE = "daily_active_reseller_summary"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Scheduled(cron = '${DailyActiveReseller.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("DailyActiveReseller Aggregator started***************************************************************************" + new Date());
        List<DailyActiveResellerResponse> dailyActiveResellerResponses = fetchData();
        // List<WeeklyResellerSummary> weeklyResellerSummaries = getWeeklyResellerSummaryDataModels(dailyActiveResellerResponse);
        for (DailyActiveResellerResponse dailyActiveResellerResponse : dailyActiveResellerResponses) {
            insertData(dailyActiveResellerResponse);
        }
        log.info("DailyActiveReseller Aggregator ended*****************************************************************************");
    }

    private List<DailyActiveResellerResponse> fetchData() {
        def sqlQuery = "SELECT s2.reseller_id,s2.reseller_name FROM reseller_current_status s1 ,reseller_current_status s2 WHERE s1.reseller_parent=s2.reseller_id"
        Map<String,String> activeReseller = new HashMap<String, String>()
        try {
            activeReseller= jdbcTemplate.query(sqlQuery,
                    new ResultSetExtractor() {
                        @Override
                        Map<String,String> extractData(ResultSet rs) {
                            Map<String,String> map= new HashMap<String, String>()
                            while (rs.next()) {
                                map.put(rs.getString("reseller_id"),rs.getString("reseller_name"))
                            }
                            return map
                        }
                    })
        } catch (Exception e) {
        }
            def sqlQueryTotalactiveReseller = "SELECT rc.reseller_parent,rc.region,COUNT(rc.reseller_id) AS total_reseller,(SELECT COUNT(irc.reseller_id) FROM reseller_current_status irc WHERE irc.reseller_parent=rc.reseller_parent AND irc.reseller_status='Active' GROUP BY irc.reseller_parent ) AS active_reseller,rc.email FROM reseller_current_status rc GROUP BY rc.reseller_parent";

        try {
            List<DailyActiveResellerResponse> response = jdbcTemplate.query(sqlQueryTotalactiveReseller,
                    new ResultSetExtractor<List<DailyActiveResellerResponse>>() {
                        @Override
                        List<DailyActiveResellerResponse> extractData(ResultSet rs) throws SQLException, DataAccessException {
                            List<DailyActiveResellerResponse> list = new ArrayList<>();
                            while (rs.next()) {
                                java.sql.Date todayDate=new java.sql.Date(System.currentTimeMillis())
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(todayDate);
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd")
                                String strDate = formatter.format(todayDate)
                                String id = GenerateHash.createHashString(strDate, rs.getString("reseller_parent"));
                                DailyActiveResellerResponse dailyActiveResellerResponse = new DailyActiveResellerResponse()
                                dailyActiveResellerResponse.setId(id)
                                dailyActiveResellerResponse.setDistributorId(rs.getString("reseller_parent"))
                                dailyActiveResellerResponse.setRegion(rs.getString("region"))
                                dailyActiveResellerResponse.setTotalDistributor(rs.getString("total_reseller"))
                                dailyActiveResellerResponse.setActiveDistributor(rs.getString("active_reseller"))
                                dailyActiveResellerResponse.setEmail(rs.getString("email"))
                                dailyActiveResellerResponse.setResellerDate(todayDate)
                                dailyActiveResellerResponse.setWeekNumber(calendar.get(calendar.WEEK_OF_YEAR))
                                dailyActiveResellerResponse.setYear(calendar.get(calendar.YEAR))
                                if(activeReseller.containsKey(rs.getString("reseller_parent")))
                                {
                                    dailyActiveResellerResponse.setName(activeReseller.get(rs.getString("reseller_parent")))
                                }else{
                                    dailyActiveResellerResponse.setName(rs.getString("reseller_parent"))
                                }

                                //dailyActiveResellerResponse.setName(rs.getString("name")!=null?rs.getString("name"):rs.getString("reseller_parent"))

                                list.add(dailyActiveResellerResponse)
                            }
                            return list;
                        }
                    })
            return response;
        } catch (Exception e) {
            log.error(e)
        }
    }


    private void insertData(DailyActiveResellerResponse dailyActiveResellerResponse) {
        log.info("Updating Daily_Active_reseller_summary table in db...");
        if (dailyActiveResellerResponse != null) {
            def sql = "INSERT INTO ${TABLE} (id,reseller_parent,region,total_reseller,active_reseller,email,date,weekNumber,year,name) VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id = VALUES(id)";
            log.debug(sql);
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dailyActiveResellerResponse
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.distributorId)
                        ps.setString(++index, row.region)
                        ps.setString(++index, row.totalDistributor)
                        ps.setString(++index, row.activeDistributor)
                        ps.setString(++index, row.email)
                        ps.setDate(++index, row.resellerDate)
                        ps.setInt(++index, row.weekNumber)
                        ps.setInt(++index, row.year)
                        ps.setString(++index, row.name)
                    },
                    getBatchSize: { 1 }
            ] as BatchPreparedStatementSetter)
            log.info("Updated 1 row in Daily_Active_reseller_summary table");
        }
    }
}

class DailyActiveResellerResponse {
    private String id
    private String distributorId
    private String region
    private String activeDistributor
    private String totalDistributor
    private String email
    private Date resellerDate
    private int weekNumber
    private int year
    private String name

    DailyActiveResellerResponse() {}

    public String getId() {
        return id
    }
    public void setId(String id) {
        this.id = id
    }
    public String getDistributorId() {
        return distributorId
    }
    public void setDistributorId(String distributorId) {
        this.distributorId = distributorId
    }
    public String getRegion() {
        return region
    }
    public void setRegion(String region) {
        this.region = region
    }
    public String getActiveDistributor() {
        return activeDistributor
    }
    public void setActiveDistributor(String activeDistributor) {
        this.activeDistributor = activeDistributor
    }
    public String getTotalDistributor() {
        return totalDistributor
    }
    public void setTotalDistributor(String totalDistributor) {
        this.totalDistributor = totalDistributor
    }
    public String getEmail() {
        return email
    }
    public void setEmail(String email) {
        this.email = email
    }
    public int getWeekNumber() {
        return weekNumber
    }
    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber
    }
    public int getYear() {
        return year
    }
    public void setYear(int year) {
        this.year = year
    }
    public Date getResellerDate() {
        return resellerDate;
    }
    public void setResellerDate(Date resellerDate) {
        this.resellerDate = resellerDate;
    }
    public String getName() {
        return name
    }
    public void setName(String name) {
        this.name = name
    }
}
	