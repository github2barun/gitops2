package com.seamless.customer.bi.engine.scripts

import com.seamless.customer.bi.engine.request.ReportRequest
import com.seamless.customer.bi.engine.response.ReportResponse
import com.seamless.customer.bi.engine.response.ResultCode
import com.seamless.customer.bi.engine.service.IReportScriptBaseService
import com.seamless.customer.bi.engine.service.IReportScriptService
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Log4j
@Component
class RegionalDashboard extends IReportScriptBaseService implements IReportScriptService {
    private final String TOTAL_COUNT_KEY = "totalCounts"
    private final String LINES_CONNECTED_KEY = "linesConnected"
    private final String AIRTIME_SOLD_KEY = "airtimeSold"
    private final String DEVICE_SOLD_KEY = "deviceSold"

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest) {
        log.info(reportRequest.getReportData().getReportName() + " Report started ******************************************************* : " + new Date());
        long groovyStartTime = System.currentTimeMillis();

        Map<String, Object> rawRequest = reportRequest.getRawRequest()
        List regions = rawRequest.get("region") as List;
        List resellerIds = rawRequest.get("resellerId") as List;
        List resellerTypeIds = rawRequest.get("resellerTypeId") as List;

        String sqlQuery = getQuery(reportRequest);
        log.info("Search Request Query: " + sqlQuery);

        // Get reportRequest with format date value for sql-query
        ReportRequest request = formatDateForSqlQuery(reportRequest);
        sqlQuery = addSortOrGroupBy(request, sqlQuery);
        MapSqlParameterSource mapSqlParameterSource = prepareObjectArrayForQuery(request);

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> rowsCount = namedParameterJdbcTemplate.queryForList(prepareCountQuery(sqlQuery), mapSqlParameterSource);
        long endTime = System.currentTimeMillis();
        log.debug("Time taken by count query(in milli seconds): " + (endTime - startTime));

        startTime = System.currentTimeMillis();
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sqlQuery, mapSqlParameterSource);
        rows = processData(rows);
        endTime = System.currentTimeMillis();
        log.debug("Time taken by actual query(in milli seconds): " + (endTime - startTime));

        ReportResponse reportResponse = new ReportResponse();
        reportResponse.setList(rows);
        long totalRecords = Long.parseLong(rowsCount.get(0).get("countRecord").toString());
        reportResponse.setTotalRecordCount(totalRecords);

        log.info(totalRecords + " raw records found for report name: " + request.getReportData().getReportName());

        reportResponse.setResultCode(ResultCode.SUCCESS.getResultCode());
        reportResponse.setResultDescription(ResultCode.SUCCESS.name());

        long groovyEndTime = System.currentTimeMillis();
        log.info("Total Time taken in report execution (in milli seconds): " + (groovyEndTime - groovyStartTime));
        log.info(reportRequest.getReportData().getReportName() + " Report finished ******************************************************* : " + new Date());
        return reportResponse;
    }

    List<Map<String, Object>> processData(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> processedData = new HashMap();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> data = rows.get(i)
            String region = data.get("region") as String
            String resellerId = data.get("resellerId") as String
            Double linesConnected = data.get(LINES_CONNECTED_KEY) as Double
            Double airtimeSold = data.get(AIRTIME_SOLD_KEY) as Double
            Double deviceSold = data.get(DEVICE_SOLD_KEY) as Double

            Map regionMap = processedData.getOrDefault(region, null)
            if (regionMap == null) {
                regionMap = new HashMap<String, Object>()

                Map<String, Double> lnctd = new HashMap<>()
                lnctd.put(resellerId, linesConnected)
                lnctd.put(TOTAL_COUNT_KEY, linesConnected)

                Map<String, Double> atsd = new HashMap<>()
                atsd.put(resellerId, airtimeSold)
                atsd.put(TOTAL_COUNT_KEY, airtimeSold)

                Map<String, Double> dvsd = new HashMap<>()
                dvsd.put(resellerId, deviceSold)
                dvsd.put(TOTAL_COUNT_KEY, deviceSold)

                regionMap.put(LINES_CONNECTED_KEY, lnctd)
                regionMap.put(AIRTIME_SOLD_KEY, atsd)
                regionMap.put(DEVICE_SOLD_KEY, dvsd)

                processedData.put(region, regionMap)
            } else {
                log.debug("********** regionMap is NOT null: " + regionMap)

                Map<String, Object> linesConn = regionMap.get(LINES_CONNECTED_KEY)
                Double linesConnReseller = linesConn.getOrDefault(resellerId, 0D) as Double
                linesConnReseller = linesConnReseller + linesConnected
                Double linesConnTotal = linesConn.getOrDefault(TOTAL_COUNT_KEY, 0D) as Double
                linesConnTotal = linesConnTotal + linesConnected
                linesConn.put(resellerId, linesConnReseller)
                linesConn.put(TOTAL_COUNT_KEY, linesConnTotal)

                Map<String, Object> airTimeSld = regionMap.get(AIRTIME_SOLD_KEY)
                Double airTimeSldReseller = airTimeSld.getOrDefault(resellerId, 0D) as Double
                airTimeSldReseller = airTimeSldReseller + airtimeSold
                Double airTimeSldTotal = airTimeSld.getOrDefault(TOTAL_COUNT_KEY, 0D) as Double
                airTimeSldTotal = airTimeSldTotal + airtimeSold
                airTimeSld.put(resellerId, airTimeSldReseller)
                airTimeSld.put(TOTAL_COUNT_KEY, airTimeSldTotal)

                Map<String, Object> deviceSld = regionMap.get(DEVICE_SOLD_KEY)
                Double deviceSldReseller = deviceSld.getOrDefault(resellerId, 0D) as Double
                deviceSldReseller = deviceSldReseller + deviceSold
                Double deviceSldTotal = deviceSld.getOrDefault(TOTAL_COUNT_KEY, 0D) as Double
                deviceSldTotal = deviceSldTotal + deviceSold
                deviceSld.put(resellerId, deviceSldReseller)
                deviceSld.put(TOTAL_COUNT_KEY, deviceSldTotal)

                regionMap.put(LINES_CONNECTED_KEY, linesConn)
                regionMap.put(AIRTIME_SOLD_KEY, airTimeSld)
                regionMap.put(DEVICE_SOLD_KEY, deviceSld)

                processedData.put(region, regionMap)
            }
        }
        result.add(processedData)
        log.debug("********** response:" + result)
        return result;
    }

    @Override
    public long getRowCount(ReportRequest reportRequest)
    {
        log.info("GetRowCount method called for: [" + reportRequest.getReportData().getReportName() + "]");
        String rows = getRows(namedParameterJdbcTemplate, reportRequest, getQuery(reportRequest));
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

    String getQuery(ReportRequest reportRequest) {
        Map<String, Object> rawRequest = reportRequest.getRawRequest()
        List regions = rawRequest.get("region") as List;
        List resellerIds = rawRequest.get("resellerId") as List;
        List resellerTypeIds = rawRequest.get("resellerTypeId") as List;

        String sqlQuery = '''
        SELECT dp.id, rcs.region, dp.reseller_id as "resellerId", dp.lines_connected as "linesConnected",
               dp.total_airtime_sold as "airtimeSold", dp.device_sold as "deviceSold"
        FROM bi.dealer_performance_external_weekly_sales_summary dp, bi.reseller_current_status rcs
        WHERE dp.reseller_id = rcs.reseller_id
          AND ("ALL" in (:month) or dp.month in (:month))
          AND ("ALL" in (:year) or dp.year in (:year))

          ''';

        String region = '';
        if (regions.size() == 1) {
            region = "\'" + regions.get(0) + "\'"
        } else if (regions.size() > 1) {
            for (int i = 0; i < regions.size(); i++) {
                region = region + "\'" + regions.get(i) + "\',"
            }
            region = region.substring(0, region.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + region + ') or rcs.region in (' + region + ')) ';

        String resellerId = '';
        if (resellerIds.size() == 1) {
            resellerId = "\'" + resellerIds.get(0) + "\'"
        } else if (resellerIds.size() > 1) {
            for (int i = 0; i < resellerIds.size(); i++) {
                resellerId = resellerId + "\'" + resellerIds.get(i) + "\',"
            }
            resellerId = resellerId.substring(0, resellerId.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + resellerId + ') or rcs.reseller_id in (' + resellerId + ')) ';

        String resellerTypeId = '';
        if (resellerTypeIds.size() == 1) {
            resellerTypeId = "\'" + resellerTypeIds.get(0) + "\'"
        } else if (resellerTypeIds.size() > 1) {
            for (int i = 0; i < resellerTypeIds.size(); i++) {
                resellerTypeId = resellerTypeId + "\'" + resellerTypeIds.get(i) + "\',"
            }
            resellerTypeId = resellerTypeId.substring(0, resellerTypeId.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + resellerTypeId + ') or rcs.reseller_type_id in (' + resellerTypeId + ')) ';

        sqlQuery = sqlQuery + '''
          AND rcs.reseller_type_id in ("branch","pos","hq_mpesa")
        '''
        return sqlQuery;
    }


}