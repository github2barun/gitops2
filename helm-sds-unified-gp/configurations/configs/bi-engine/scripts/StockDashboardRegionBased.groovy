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
class StockDashboardRegionBased extends IReportScriptBaseService implements IReportScriptService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest) {
        log.info(reportRequest.getReportData().getReportName() + " Report started ******************************************************* : " + new Date());
        long groovyStartTime = System.currentTimeMillis();
        Map<String, Object> rawRequest = reportRequest.getRawRequest()
        List regions = rawRequest.get("region") as List;
        List productSKUs = rawRequest.get("productSKU") as List;

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
        long groovyEndTime = System.currentTimeMillis();
        log.info("Total Time taken in report execution (in milli seconds): " + (groovyEndTime - groovyStartTime));
        log.info(reportRequest.getReportData().getReportName() + " Report finished ******************************************************* : " + new Date());
        return reportResponse;
    }


    List<Map<String, Object>> processData(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> processedData = new HashMap();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> data = rows.get(i);
            String region = (String) data.get("region");
            String productSku = (String) data.get("product_sku");
            Integer stockCount = (Integer) data.get("stock_count");

            Map regionMap = (Map) processedData.getOrDefault(region, null);
            if (regionMap == null) {
                Map<String, Integer> products = new HashMap<>();
                products.put(productSku, stockCount);
                processedData.put(region, products);
            } else {
                log.debug("********** regionMap is NOT null: " + regionMap)

                Integer stockCountProduct = (Integer) regionMap.get(productSku);
                System.out.println(stockCountProduct);
                if (stockCountProduct == null) {
                    regionMap.put(productSku, stockCount);
                } else {
                    Integer stockCountTotal = stockCountProduct + stockCount;
                    regionMap.put(productSku, stockCountTotal);
                }
                processedData.put(region, regionMap);
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
        String loggedInUserId = (String) rawRequest.get("dealerID");
        List productSKUs = rawRequest.get("productSKU") as List;

        String sqlQuery = '''
                SELECT
                    IFNULL(rc_status.region, "N/A") as region,
                    IFNULL(rc_stock.product, "N/A") as `product_sku`,
                    SUM(rc_stock.stock_count) as stock_count
                FROM reseller_current_stock rc_stock
                JOIN reseller_current_status rc_status ON rc_status.reseller_id = rc_stock.reseller_id
                WHERE
                 rc_status.region!=\'\' AND rc_stock.date = current_date()
        '''

        String region = '';
        if (regions.size() == 1) {
            region = "\'" + regions.get(0) + "\'"
        } else if (regions.size() > 1) {
            for (int i = 0; i < regions.size(); i++) {
                region = region + "\'" + regions.get(i) + "\',"
            }
            region = region.substring(0, region.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + region + ') or rc_status.region in (' + region + ')) ';

       if (loggedInUserId.equalsIgnoreCase("Operator")){
            sqlQuery = sqlQuery + ' AND ((rc_status.reseller_path LIKE CONCAT("' + loggedInUserId + '")) or (rc_status.reseller_path LIKE CONCAT(  "' + loggedInUserId + '" ,"/%"))) ';
         }
         else {
             sqlQuery = sqlQuery + ' AND ((rc_status.reseller_path LIKE CONCAT("%/","' + loggedInUserId + '")) or (rc_status.reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '", "/%" ))) ';
         }
        String productSKU = '';
        if (productSKUs.size() == 1) {
            productSKU = "\'" + productSKUs.get(0) + "\'"
        } else if (productSKUs.size() > 1) {
            for (int i = 0; i < productSKUs.size(); i++) {
                productSKU = productSKU + "\'" + productSKUs.get(i) + "\',"
            }
            productSKU = productSKU.substring(0, productSKU.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + productSKU + ') or rc_stock.product in (' + productSKU + ')) ';


        sqlQuery += """GROUP BY
                    rc_status.region,rc_stock.product
                ORDER BY
                    stock_count DESC"""

        return sqlQuery;

    }
}