package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.aggregate.ScrollableAbstractAggregator
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.scheduling.annotation.Scheduled

import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
class GroupManagementTripOutletVisitDailyAggregator extends ScrollableAbstractAggregator {

    static final def TABLE = "outlet_trip_gms_summary"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Scheduled(cron = '${GroupManagementTripOutletVisitDailyAggregator.cron:*/3 * * * * ?}')
    void aggregate() {
        log.info("********** GroupManagementTripOutletVisitDailyAggregator started at " + new java.util.Date());
        List<GroupManagementTripOutletVisitDailyAggregatorResponse> gmsTripOutletVisitDailyResponses = fetchData();
        // List<WeeklyResellerSummary> weeklyResellerSummaries = getWeeklyResellerSummaryDataModels(gmsTripOutletVisitDailyResponse);
        for (GroupManagementTripOutletVisitDailyAggregatorResponse gmsTripOutletVisitDailyResponse : gmsTripOutletVisitDailyResponses) {
            insertData(gmsTripOutletVisitDailyResponse);
        }
        log.info("********** GroupManagementTripOutletVisitDailyAggregator ended at " + new java.util.Date());
    }

    private List<GroupManagementTripOutletVisitDailyAggregatorResponse> fetchData() {
        def sqlOutletVisitsCount = "SELECT gma.user_id, gma.name, count(gmga.group_id) as total_outlets FROM groupmanagementsystem.admin gma, groupmanagementsystem.group_admin gmga, groupmanagementsystem.group_member gmgm WHERE gma.admin_id = gmga.admin_id AND gmga.group_id = gmgm.group_id AND gmgm.member_type = 'member' GROUP BY gma.user_id";
        try {
            List<GroupManagementTripOutletVisitDailyAggregatorResponse> response = jdbcTemplate.query(sqlOutletVisitsCount,
                    new ResultSetExtractor<List<GroupManagementTripOutletVisitDailyAggregatorResponse>>() {
                        @Override
                        List<GroupManagementTripOutletVisitDailyAggregatorResponse> extractData(ResultSet rs) throws SQLException, DataAccessException {
                            List<GroupManagementTripOutletVisitDailyAggregatorResponse> list = new ArrayList<>();
                            while (rs.next()) {

                                Date todayDate = new Date(System.currentTimeMillis())

                                String resellerId = rs.getString("user_id");
                                String resellerName = rs.getString("name");
                                Long totalOutletVisits = rs.getLong("total_outlets")

                                String id = GenerateHash.createHashString(todayDate.toString(), resellerId);

                                GroupManagementTripOutletVisitDailyAggregatorResponse gmsTripOutletVisitDailyResponse =
                                        new GroupManagementTripOutletVisitDailyAggregatorResponse(id, todayDate, resellerId, resellerName, totalOutletVisits)
                                list.add(gmsTripOutletVisitDailyResponse)
                            }
                            return list;
                        }
                    })
            return response;
        } catch (Exception e) {
            log.error(e)
        }
    }


    private void insertData(GroupManagementTripOutletVisitDailyAggregatorResponse gmsTripOutletVisitDailyResponse) {
        log.info("Updating outlet_trip_gms_summary table in db...");
        if (gmsTripOutletVisitDailyResponse != null) {
            def sql = "INSERT INTO ${TABLE} (id,summary_date,reseller_id,reseller_name,total_outlet_visits) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE total_outlet_visits = VALUES(total_outlet_visits)";
            log.debug(sql);
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = gmsTripOutletVisitDailyResponse
                        def index = 0
                        ps.setString(++index, row.id)
                        ps.setDate(++index, row.summaryDate)
                        ps.setString(++index, row.resellerId)
                        ps.setString(++index, row.resellerName)
                        ps.setLong(++index, row.totalOutletVisits)
                    },
                    getBatchSize: { 1 }
            ] as BatchPreparedStatementSetter)
        }
        log.info("GroupManagementTripOutletVisitDailyAggregator Aggregated into 1 row.")
    }
}

class GroupManagementTripOutletVisitDailyAggregatorResponse {
    private String id
    private Date summaryDate
    private String resellerId
    private String resellerName
    private Long totalOutletVisits

    GroupManagementTripOutletVisitDailyAggregatorResponse() {}

    GroupManagementTripOutletVisitDailyAggregatorResponse(String id, Date summaryDate, String resellerId, String resellerName, Long totalOutletVisits) {
        this.id = id
        this.summaryDate = summaryDate
        this.resellerId = resellerId
        this.resellerName = resellerName
        this.totalOutletVisits = totalOutletVisits
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    Date getSummaryDate() {
        return summaryDate
    }

    void setSummaryDate(Date summaryDate) {
        this.summaryDate = summaryDate
    }

    String getResellerId() {
        return resellerId
    }

    void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

    String getResellerName() {
        return resellerName
    }

    void setResellerName(String resellerName) {
        this.resellerName = resellerName
    }

    Long getTotalOutletVisits() {
        return totalOutletVisits
    }

    void setTotalOutletVisits(Long totalOutletVisits) {
        this.totalOutletVisits = totalOutletVisits
    }
}