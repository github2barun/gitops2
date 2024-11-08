import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
class DistributorWiseWeeklyResellerReport extends AbstractAggregator {

    static final def TABLE = "weekly_reseller_summary"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Scheduled(cron = '${DistributorWiseWeeklyResellerReport.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("DistributorWiseWeeklyResellerReport Aggregator started***************************************************************************" + new Date());
        List<WeeklyResellerSummaryResponse> weeklyResellerSummaryResponses = fetchData();
        List<WeeklyResellerSummary> weeklyResellerSummaries = getWeeklyResellerSummaryDataModels(weeklyResellerSummaryResponses);
        for (WeeklyResellerSummary weeklyResellerSummary : weeklyResellerSummaries) {
            insertData(weeklyResellerSummary);
        }
        log.info("DistributorWiseWeeklyResellerReport Aggregator ended*****************************************************************************");
    }

    private List<WeeklyResellerSummaryResponse> fetchData() {
        def sqlQuery = "SELECT tks.weekNumber, tks.year, rcs.reseller_parent, COUNT(DISTINCT(tks.posId)) as active_pos,(SELECT COUNT(rcs1.reseller_id) FROM reseller_current_status AS rcs1 WHERE rcs1.reseller_parent= rcs.reseller_parent) AS total_pos, (SELECT COUNT(*) FROM reseller_current_status AS rcs2 WHERE rcs2.reseller_status='Blocked' AND rcs2.reseller_parent= rcs.reseller_parent) AS locked_pos FROM reseller_current_status AS rcs LEFT JOIN total_kyc_sales AS tks ON tks.posId=rcs.reseller_parent GROUP BY tks.weekNumber, tks.year, rcs.reseller_parent";
        try {
            List<WeeklyResellerSummaryResponse> response = jdbcTemplate.query(sqlQuery,
                    new ResultSetExtractor<List<WeeklyResellerSummaryResponse>>() {
                        @Override
                        List<WeeklyResellerSummaryResponse> extractData(ResultSet rs) throws SQLException, DataAccessException {
                            List<WeeklyResellerSummaryResponse> list = new ArrayList<>();
                            while (rs.next()) {
                                WeeklyResellerSummaryResponse weeklyResellerSummaryResponse = new WeeklyResellerSummaryResponse();
                                weeklyResellerSummaryResponse.setYear(rs.getInt("year"));
                                weeklyResellerSummaryResponse.setWeek(rs.getInt("weekNumber"));
                                weeklyResellerSummaryResponse.setDistributorId(rs.getString("reseller_parent"));
                                weeklyResellerSummaryResponse.setActivePos(rs.getInt("active_pos"));
                                weeklyResellerSummaryResponse.setTotalPos(rs.getInt("total_pos"));
                                weeklyResellerSummaryResponse.setLockedPos(rs.getInt("locked_pos"));
                                list.add(weeklyResellerSummaryResponse);
                            }
                            return list;
                        }
                    })
            return response;
        } catch (Exception e) {
            log.error(e)
        }
    }

    private List<WeeklyResellerSummary> getWeeklyResellerSummaryDataModels(List<WeeklyResellerSummaryResponse> weeklyResellerSummaryResponses) {
        List<WeeklyResellerSummary> weeklyResellerSummaries = new ArrayList<>();
        for (WeeklyResellerSummaryResponse data : weeklyResellerSummaryResponses) {
            WeeklyResellerSummary weeklyResellerSummary = new WeeklyResellerSummary();
            String id = GenerateHash.createHashString(String.valueOf(data.getWeek()), String.valueOf(data.getYear()), data.getDistributorId());
            weeklyResellerSummary.setId(id);
            weeklyResellerSummary.setWeek(String.valueOf(data.getWeek()));
            weeklyResellerSummary.setYear(String.valueOf(data.getYear()));
            weeklyResellerSummary.setDistributorId(data.getDistributorId());
            weeklyResellerSummary.setActivePos(String.valueOf(data.getActivePos()));
            weeklyResellerSummary.setTotalPos(String.valueOf(data.getTotalPos()));
            weeklyResellerSummary.setLockedPos(String.valueOf(data.getLockedPos()));
            weeklyResellerSummaries.add(weeklyResellerSummary);
        }
        return weeklyResellerSummaries;
    }

    private void insertData(WeeklyResellerSummary weeklyResellerSummary) {

        log.info("Updating weekly_reseller_summary table in db...");
        if (weeklyResellerSummary != null) {
            def sql = "INSERT INTO ${TABLE} (id,week,year,distributor_id,active_pos,total_pos,locked_pos) VALUES (?,?,?,?,?,?,?)";
            log.debug(sql);
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = weeklyResellerSummary
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setString(++index, row.week)
                        ps.setString(++index, row.year)
                        ps.setString(++index, row.distributorId)
                        ps.setString(++index, row.activePos)
                        ps.setString(++index, row.totalPos)
                        ps.setString(++index, row.lockedPos)
                    },
                    getBatchSize: { 1 }
            ] as BatchPreparedStatementSetter)
            log.info("Updated 1 row in weekly_reseller_summary table");
        }
    }
}

class WeeklyResellerSummary {
    String id;
    String week;
    String year;
    String distributorId;
    String activePos;
    String totalPos;
    String lockedPos;

    WeeklyResellerSummary() {}

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getWeek() {
        return week
    }

    void setWeek(String week) {
        this.week = week
    }

    String getYear() {
        return year
    }

    void setYear(String year) {
        this.year = year
    }

    String getDistributorId() {
        return distributorId
    }

    void setDistributorId(String distributorId) {
        this.distributorId = distributorId
    }

    String getActivePos() {
        return activePos
    }

    void setActivePos(String activePos) {
        this.activePos = activePos
    }

    String getTotalPos() {
        return totalPos
    }

    void setTotalPos(String totalPos) {
        this.totalPos = totalPos
    }

    String getLockedPos() {
        return lockedPos
    }

    void setLockedPos(String lockedPos) {
        this.lockedPos = lockedPos
    }
}

class WeeklyResellerSummaryResponse {
    int week;
    int year;
    String distributorId;
    int activePos;
    int totalPos;
    int lockedPos;

    WeeklyResellerSummaryResponse() {}

    int getWeek() {
        return week
    }

    void setWeek(int week) {
        this.week = week
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }

    String getDistributorId() {
        return distributorId
    }

    void setDistributorId(String distributorId) {
        this.distributorId = distributorId
    }

    int getActivePos() {
        return activePos
    }

    void setActivePos(int activePos) {
        this.activePos = activePos
    }

    int getTotalPos() {
        return totalPos
    }

    void setTotalPos(int totalPos) {
        this.totalPos = totalPos
    }

    int getLockedPos() {
        return lockedPos
    }

    void setLockedPos(int lockedPos) {
        this.lockedPos = lockedPos
    }
}
