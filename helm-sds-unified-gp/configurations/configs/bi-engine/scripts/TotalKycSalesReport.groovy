//Groovy script for TotalKycSalesReport

import com.seamless.customer.bi.engine.request.ReportRequest
import com.seamless.customer.bi.engine.response.ReportResponse
import com.seamless.customer.bi.engine.response.ResultCode
import com.seamless.customer.bi.engine.service.IReportScriptBaseService
import com.seamless.customer.bi.engine.service.IReportScriptService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Slf4j
@Component
class TotalKycSalesReport extends IReportScriptBaseService implements IReportScriptService
{
    private final String query = "SELECT Ifnull(tks.brandcode, 'N/A') AS 'brandCode', Ifnull(tks.brand, 'N/A') AS 'brandName', Ifnull(tks.brandprefix, 'N/A') AS 'brandPrefix', Ifnull(SUM(tks.count), 0) AS 'soldStock', Ifnull(tks.end_time_day, 'N/A') AS 'date' FROM total_kyc_sales tks WHERE tks.end_time_day BETWEEN :fromDate AND :toDate AND('ALL' IN (:brand) OR tks.brand IN (:brand)) AND ('ALL' IN (:posId) OR tks.posid IN (:posId)) GROUP BY tks.brandcode, tks.brand, tks.brandprefix, tks.end_time_day";

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    long getRowCount(ReportRequest reportRequest)
    {
        String rows = getRows(namedParameterJdbcTemplate, reportRequest, query);
        if (Objects.nonNull(rows))
            try
            {
                return Long.parseLong(rows);
            }
            catch (NumberFormatException e)
            {
                log.error("Error occurred while getting rowCount : " + e.getMessage());
                return 0L;
            }
        else
            return 0L;
    }

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest)
    {
        log.info("GetAllRecords method called for: [" + reportRequest.getReportData().getReportName() + "]");

        // Get total records and also format the date as per SQL query
        long totalRecords = Long.parseLong(getRows(namedParameterJdbcTemplate, reportRequest, query));
        log.info(totalRecords + " records found for report name: " + reportRequest.getReportData().getReportName());

        String sqlQuery = addSortOrGroupBy(reportRequest, query);
        MapSqlParameterSource mapSqlParameterSource = prepareObjectArrayForQuery(reportRequest);
        sqlQuery = addLimitClauseInSql(reportRequest, sqlQuery);

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sqlQuery, mapSqlParameterSource);
        long endTime = System.currentTimeMillis();
        log.info("Time taken by actual query(in milliseconds): " + (endTime-startTime));

        ReportResponse reportResponse = new ReportResponse();
        reportResponse.setList(rows);
        reportResponse.setTotalRecordCount(totalRecords);

        reportResponse.setResultCode(ResultCode.SUCCESS.getResultCode());
        reportResponse.setResultDescription(ResultCode.SUCCESS.name());
        return reportResponse;
    }
}
