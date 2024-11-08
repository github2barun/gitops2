package com.seamless.customer.bi.engine.scripts


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

/***
 *
 *
 *
 */
@Slf4j
@Component
class OrderDashboard extends IReportScriptBaseService implements IReportScriptService {
    private final String TOTAL_ORDERS_KEY = "TotalOrders"
    private final String REGION_KEY = "Region"

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest) {
        log.info(reportRequest.getReportData().getReportName() +" Report started ******************************************************* : " + new Date());
        long groovyStartTime = System.currentTimeMillis();
        Map<String, Object> rawRequest = reportRequest.getRawRequest()
        List resellerIds = rawRequest.get("resellerId") as List;
        List statuses = rawRequest.get("status") as List;
        List regions = rawRequest.get("region") as List;

        String sqlQuery = getQuery(reportRequest);

        log.info("Search Request Query: " + sqlQuery)

        // Get reportRequest with format date value for sql-query
        ReportRequest request = formatDateForSqlQuery(reportRequest)
        sqlQuery = addSortOrGroupBy(request, sqlQuery)
        MapSqlParameterSource mapSqlParameterSource = prepareObjectArrayForQuery(request)

        long startTime = System.currentTimeMillis()
        List<Map<String, Object>> rowsCount = namedParameterJdbcTemplate.queryForList(prepareCountQuery(sqlQuery), mapSqlParameterSource)
        long endTime = System.currentTimeMillis()
        log.debug("Time taken by count query(in milli seconds): " + (endTime - startTime))

        startTime = System.currentTimeMillis()
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sqlQuery, mapSqlParameterSource)
        rows = processData(rows)
        endTime = System.currentTimeMillis()
        log.debug("Time taken by actual query(in milli seconds): " + (endTime - startTime))

        ReportResponse reportResponse = new ReportResponse()
        reportResponse.setList(rows)
        long totalRecords = Long.parseLong(rowsCount.get(0).get("countRecord").toString())
        reportResponse.setTotalRecordCount(totalRecords)

        log.info(totalRecords + " raw records found for report name: " + request.getReportData().getReportName())

        reportResponse.setResultCode(ResultCode.SUCCESS.getResultCode())
        reportResponse.setResultDescription(ResultCode.SUCCESS.name())

        long groovyEndTime = System.currentTimeMillis();
        log.info("Total Time taken in report execution (in milli seconds): " + (groovyEndTime - groovyStartTime));
        log.info(reportRequest.getReportData().getReportName() +" Report finished ******************************************************* : " + new Date());
        return reportResponse
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
        String loggedInUserId = (String) rawRequest.get("dealerID");
        List resellerIds = rawRequest.get("resellerId") as List;
        List statuses = rawRequest.get("status") as List;
        List regions = rawRequest.get("region") as List;


        String sqlQuery = '''
            SELECT IFNULL(rcs.region, "N/A") as "Region", Count(*) as "TotalOrders"
            FROM bi.all_orders_status_aggregator ol, bi.reseller_current_status rcs
            WHERE (ol.transaction_date between :fromDate and :toDate)
             AND rcs.reseller_type_id IN ("SubDistributor", "FranchiseShop")
             ''';

        String resId = '';
        if (resellerIds.size() == 1) {
            resId = "\'" + resellerIds.get(0) + "\'"
        } else if (resellerIds.size() > 1) {
            for (int i = 0; i < resellerIds.size(); i++) {
                resId = resId + "\'" + resellerIds.get(i) + "\',"
            }
            resId = resId.substring(0, resId.length() - 1)
        }
        //sqlQuery = sqlQuery + ' AND ("ALL" in ('+ resId +') or rcs.reseller_id in ('+ resId +')) ';

        String status = '';
        if (statuses.size() == 1) {
            status = "\'" + statuses.get(0) + "\'"
        } else if (statuses.size() > 1) {
            for (int i = 0; i < statuses.size(); i++) {
                status = status + "\'" + statuses.get(i) + "\',"
            }
            status = status.substring(0, status.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + status + ') or ol.order_status in (' + status + ')) ';

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

        if (loggedInUserId.equalsIgnoreCase("Operator")){
             sqlQuery = sqlQuery + ' AND (( ol.seller_id IN (select reseller_id from reseller_current_status where (reseller_path LIKE CONCAT( "' + loggedInUserId + '" ) OR reseller_path LIKE CONCAT( "' + loggedInUserId + '", "/%" )) AND reseller_type_id in ("SubDistributor", "FranchiseShop"))) OR ( ol.drop_location_id IN (select reseller_id from reseller_current_status where (reseller_path LIKE CONCAT(  "' + loggedInUserId + '") OR reseller_path LIKE CONCAT( "' + loggedInUserId + '", "/%" )) AND reseller_type_id in ("SubDistributor", "FranchiseShop")))) ';
         }
        else {
             sqlQuery = sqlQuery + ' AND (( ol.seller_id IN (select reseller_id from reseller_current_status where (reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '" ) OR reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '", "/%" )) AND reseller_type_id in ("SubDistributor", "FranchiseShop"))) OR ( ol.drop_location_id IN (select reseller_id from reseller_current_status where (reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '") OR reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '", "/%" )) AND reseller_type_id in ("SubDistributor", "FranchiseShop")))) ';
         }

        String dealerType = rawRequest.get("dealerType")

         if (org.apache.commons.lang3.StringUtils.isBlank(dealerType)) {
                 sqlQuery += ' AND ((rcs.reseller_id = ol.seller_id ) or (rcs.reseller_id = ol.drop_location_id )) AND (("All" in (' + resId + ')) or (ol.seller_id in ('+ resId +')) or (ol.drop_location_id in ('+ resId +')))'
         } else if (dealerType.equalsIgnoreCase("SELLER")) {
                  sqlQuery += ' AND (rcs.reseller_id = ol.seller_id ) AND (("All" in (' + resId + ')) or (ol.seller_id in ('+ resId +')))'
         } else if (dealerType.equalsIgnoreCase("BUYER")) {
                  sqlQuery += ' AND (rcs.reseller_id = ol.buyer_id) AND (ol.buyer_id in ('+ resId +'))'
         } else if (  dealerType.equalsIgnoreCase("DROP_LOCATION")) {
        	    sqlQuery += ' AND (rcs.reseller_id = ol.drop_location_id) AND (("All" in (' + resId + ')) or (ol.drop_location_id in ('+ resId +')) )'
         } else if (  dealerType.equalsIgnoreCase("RECEIVER")) {
                 sqlQuery += ' AND (rcs.reseller_id = ol.receiver_id)  AND (ol.receiver_id in ('+ resId +')) '
         } else if (  dealerType.equalsIgnoreCase("PICKUP_LOCATION")) {
                  sqlQuery += ' AND (rcs.reseller_id = ol.pickup_location_id) AND ol.pickup_location_id in ('+ resId +') '
         } else if (  dealerType.equalsIgnoreCase("SENDER")) {
                  sqlQuery += ' AND (rcs.reseller_id = ol.sender_id) AND (ol.sender_id in ('+ resId +')) '
         }

        sqlQuery += ' group by rcs.region '
        return sqlQuery;
    }

    List<Map<String, Object>> processData(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> processedData = new HashMap();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> data = rows.get(i)
            String region = data.get(REGION_KEY) as String
            Double totalOrders = data.get(TOTAL_ORDERS_KEY) as Double
            processedData.put(region, totalOrders)
        }

        result.add(processedData)
        log.debug("********** response:" + result)
        return result;
    }
}
