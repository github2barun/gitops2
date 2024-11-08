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
class StockDashboardParentChild extends IReportScriptBaseService implements IReportScriptService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest) {
        log.info(reportRequest.getReportData().getReportName() + " Report started ******************************************************* : " + new Date());
        long groovyStartTime = System.currentTimeMillis();
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
        Map<String, Object> resultData = new HashMap();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> data = rows.get(i);
            String region = (String) data.get("region");
            String resellerId = (String) data.get("reseller_id");
            String resellerType = (String) data.get("reseller_type");
            String productSku = (String) data.get("product_sku");
            Integer stockCount = (Integer) data.get("stock_count");

            Map regionMap = (Map) resultData.getOrDefault(region, null);
            if (regionMap == null) {
                Map<String, Integer> resellers = new HashMap<>();
                resellers.put("totalCount", stockCount);
                resellers.put(resellerId, stockCount);

                Map<String, Object> resellerTypes = new HashMap<>();
                resellerTypes.put(resellerType, resellers);

                Map<String, Object> products = new HashMap<>();
                products.put(productSku, resellerTypes);

                resultData.put(region, products);
            } else {

                Map<String, Object> regionProductsMap = (Map<String, Object>) regionMap.get(productSku);

                if (regionProductsMap == null) {
                    Map<String, Integer> resellers = new HashMap<>();
                    resellers.put("totalCount", stockCount);
                    resellers.put(resellerId, stockCount);

                    Map<String, Object> resellerTypes = new HashMap<>();
                    resellerTypes.put(resellerType, resellers);

                    regionMap.put(productSku, resellerTypes);

                    resultData.put(region, regionMap);
                } else {
                    log.debug("********** regionMap is NOT null: " + regionMap)
                    Map<String, Object> productResellerTypesMap = (Map<String, Object>) regionProductsMap.get(resellerType);
                    if (productResellerTypesMap == null) {
                        Map<String, Integer> resellers = new HashMap<>();
                        resellers.put("totalCount", stockCount);
                        resellers.put(resellerId, stockCount);

                        regionProductsMap.put(resellerType, resellers);

                        regionMap.put(productSku, regionProductsMap);

                        resultData.put(region, regionMap);
                    }else{
                        log.debug("********** productResellerTypesMap is NOT null: " + regionMap)
                        Integer resellerCount = (Integer) productResellerTypesMap.get(resellerId);
                        if(resellerCount == null){

                            productResellerTypesMap.put(resellerId, stockCount);
                            Integer count = (Integer) productResellerTypesMap.get("totalCount");
                            productResellerTypesMap.put("totalCount",count+stockCount);

                            regionProductsMap.put(resellerType, productResellerTypesMap);

                            regionMap.put(productSku, regionProductsMap);

                            resultData.put(region, regionMap);
                        }else{
                            Integer tempTotalCount = (Integer) productResellerTypesMap.get("totalCount");
                            productResellerTypesMap.put("totalCount",tempTotalCount+stockCount);
                            productResellerTypesMap.put(resellerId, stockCount+resellerCount);
                        }
                    }
                }
            }
        }
        result.add(resultData)
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
        List productSKUs = rawRequest.get("productSKU") as List;
        List resellerIds = rawRequest.get("resellerId") as List;
        List resellerTypeIds = rawRequest.get("resellerTypeId") as List;
        String loggedInUserId = (String) rawRequest.get("dealerID");


        String sqlQuery = '''
                SELECT
                    IFNULL(rc_status.region, "N/A") as region,
                    IFNULL(rc_status.reseller_type_id, "N/A") as reseller_type,
                    IFNULL(rc_status.reseller_id, "N/A") as reseller_id,
                    IFNULL(rc_stock.product, "N/A") as product_sku,
                    SUM(rc_stock.stock_count) as stock_count
                FROM
                    reseller_current_stock rc_stock
                    JOIN reseller_current_status rc_status ON rc_status.reseller_id = rc_stock.reseller_id
                WHERE
                    rc_stock.date = current_date()
        '''
        String resellerId = '';
        if (resellerIds.size() == 1) {
            resellerId = "\'" + resellerIds.get(0) + "\'"
        } else if (resellerIds.size() > 1) {
            for (int i = 0; i < resellerIds.size(); i++) {
                resellerId = resellerId + "\'" + resellerIds.get(i) + "\',"
            }
            resellerId = resellerId.substring(0, resellerId.length() - 1)
        }

        if (loggedInUserId.equalsIgnoreCase("Operator")){
            sqlQuery = sqlQuery + ' AND ("ALL" in (' + resellerId + ') or rc_status.reseller_id in (' + resellerId + ')) AND ((rc_status.reseller_path LIKE CONCAT("' + loggedInUserId + '")) or (rc_status.reseller_path LIKE CONCAT(  "' + loggedInUserId + '" ,"/%"))) ';
         }
         else {
             sqlQuery = sqlQuery + ' AND ("ALL" in (' + resellerId + ') or rc_status.reseller_id in (' + resellerId + ')) AND ((rc_status.reseller_path LIKE CONCAT("%/","' + loggedInUserId + '")) or (rc_status.reseller_path LIKE CONCAT( "%/", "' + loggedInUserId + '", "/%" ))) ';
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

        String resellerTypeId = '';
        if (resellerTypeIds.size() == 1) {
            resellerTypeId = "\'" + resellerTypeIds.get(0) + "\'"
        } else if (resellerTypeIds.size() > 1) {
            for (int i = 0; i < resellerTypeIds.size(); i++) {
                resellerTypeId = resellerTypeId + "\'" + resellerTypeIds.get(i) + "\',"
            }
            resellerTypeId = resellerTypeId.substring(0, resellerTypeId.length() - 1)
        }
        sqlQuery = sqlQuery + ' AND ("ALL" in (' + resellerTypeId + ') or rc_status.reseller_type_id in (' + resellerTypeId + ')) ';
        sqlQuery += """GROUP BY
                    rc_stock.product,
                    rc_status.reseller_type_id,
                    rc_status.reseller_id
                ORDER BY
                    stock_count DESC"""
         return sqlQuery;
    }
}