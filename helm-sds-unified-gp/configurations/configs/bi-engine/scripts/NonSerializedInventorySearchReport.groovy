// Groovy script for NonSerializedInventorySearchReport

import com.fasterxml.jackson.databind.ObjectMapper
import com.seamless.customer.bi.engine.request.ReportRequest
import com.seamless.customer.bi.engine.response.ReportResponse
import com.seamless.customer.bi.engine.response.ResultCode
import com.seamless.customer.bi.engine.service.IReportScriptBaseService
import com.seamless.customer.bi.engine.service.IReportScriptService
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.core.CountResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class NonSerializedInventorySearchReport extends IReportScriptBaseService implements IReportScriptService
{
    private final String query = "{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"query\":{\"bool\":{\"must\":[{\"nested\":{\"path\":\"itemDetails\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"itemDetails.productType.keyword\":{\"value\":\"<:productType:>\"}}},{\"terms\":{\"itemDetails.productType.keyword\":\"<-:productType:->\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"itemDetails.productSku.keyword\":{\"value\":\"<:productSku:>\"}}},{\"terms\":{\"itemDetails.productSku.keyword\":\"<-:productSku:->\"}}]}},{\"nested\":{\"path\":\"itemDetails.ranges\",\"query\":{\"bool\":{\"should\":[{\"range\":{\"itemDetails.ranges.startSerial\":{\"gte\":\"<:startSerial:>\",\"lte\":\"<:endSerial:>\"}}},{\"range\":{\"itemDetails.ranges.endSerial\":{\"gte\":\"<:startSerial:>\",\"lte\":\"<:endSerial:>\"}}}]}}}}]}},\"inner_hits\":{}}},{\"match\":{\"eventName\":\"SUBSCRIBER_SALE\"}},{\"match\":{\"resultCode\":\"0\"}},{\"range\":{\"timestamp\":{\"from\":\"<:fromDate:>\",\"to\":\"<:toDate:>\"}}}]}}}}";
    private static final String TOTAL = "total";
    private static final String ITEM_DETAILS = "itemDetails";
    private static final String RANGES = "ranges";
    private static final String START_SERIAL = "startSerial";
    private static final String END_SERIAL = "endSerial";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    long getRowCount(ReportRequest reportRequest)
    {
        CountResponse countResponse = executeElasticsearchQueryForCount(reportRequest, objectMapper, restHighLevelClient, query);

        if (Objects.nonNull(countResponse) && countResponse.getCount() !=0)
            return countResponse.getCount();
        else
            return 0L;
    }

    @Override
    ReportResponse getAllRecords(ReportRequest reportRequest)
    {
        log.info("GetAllRecords method called for: [" + reportRequest.getReportData().getReportName() + "]");

        //Define scroll size : Default value is 6000
        final int scrollSize = 6000;

        // Get the count for elastic search query
        int countSize = (int) getRowCount(reportRequest);

        // Setting the size as per count-response of elastic search query
        reportRequest.getRawRequest().replace("size", countSize);

        // Get the searchResponse from elastic search
        Set<SearchResponse> searchResponses = executeElasticSearchQuery(reportRequest, objectMapper, restHighLevelClient, query, scrollSize);

        // Get Hits from searchResponse
        List<SearchHits> searchHits = getSearchHits(searchResponses);

        // Custom logic to get the required result-data from elastic search
        ReportResponse reportResponse;
        if (!searchResponses.isEmpty() || !searchHits.isEmpty())
            reportResponse = getResult(searchHits, reportRequest);
        else
            reportResponse = invalidResponse("SearchResponse or SearchHits is null/empty while retrieving data from elastic search");

        return reportResponse;
    }

    private ReportResponse getResult(List<SearchHits> searchHits, ReportRequest reportRequest)
    {
        ReportResponse reportResponse = new ReportResponse();
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> totalQuantity = new HashMap<>();
        String productSku = reportRequest.getRawRequest().get("productSku").toString();
        double quantity = 0;
        long recordCount = 0L;
        // Get Item Details from InnerHits
        for (SearchHits hits : searchHits)
        {
            List<Map<String, Object>> itemDetails = getItemDetails(hits);
            List<Map<String, String>> ranges = getRanges(itemDetails, productSku);
            quantity += getTotalQuantity(ranges, reportRequest);
            // To see the itemDetails list in response
            //responseList.addAll(itemDetails);
            recordCount += getTotalRecordCount(hits);
        }
        totalQuantity.put(TOTAL, quantity);
        responseList.add(totalQuantity);
        reportResponse.setList(responseList);
        reportResponse.setTotalRecordCount(recordCount);
        reportResponse.setResultCode(ResultCode.SUCCESS.getResultCode());
        reportResponse.setResultDescription(ResultCode.SUCCESS.name());
        return reportResponse;
    }

    private static List<Map<String,Object>> getItemDetails(SearchHits hits)
    {
        List<Map<String,Object>> listOfItemDetail = new ArrayList<>();
        if (Objects.nonNull(hits)) {
            for (SearchHit hit : hits.getHits())
            {
                Map<String, SearchHits> innerHits = hit.getInnerHits();
                SearchHit[] itemDetails = innerHits.get(ITEM_DETAILS).getHits();
                for (SearchHit itemDetail : itemDetails)
                {
                    listOfItemDetail.add(itemDetail.getSourceAsMap());
                }
            }
        }
        return listOfItemDetail;
    }

    private static List<Map<String, String>> getRanges(List<Map<String, Object>> itemDetails, String productSku)
    {
        List<Map<String, String>> ranges = new ArrayList<>();
        if (!itemDetails.isEmpty()) {
            for (Map<String, Object> itemDetail : itemDetails)
            {
                String sku = itemDetail.get("productSku").toString();
                if (productSku.equals("ALL") || productSku.equalsIgnoreCase(sku)) {
                    ranges.addAll((List<Map<String, String>>) itemDetail.get(RANGES));
                }
            }
        }
        return ranges;
    }

    private static double getTotalQuantity(List<Map<String, String>> ranges, ReportRequest reportRequest)
    {
        List<String> quantity = new ArrayList<>();
        if (!ranges.isEmpty()) {
            try
            {	// Getting quantity of products within the given range
                for (Map<String, String> range : ranges) {
                    int startSerial = Integer.parseInt(range.get(START_SERIAL));
                    int endSerial = Integer.parseInt(range.get(END_SERIAL));
                    int startRange = Integer.parseInt(reportRequest.getRawRequest().get(START_SERIAL).toString());
                    int endRange = Integer.parseInt(reportRequest.getRawRequest().get(END_SERIAL).toString());
                    int qty = 0;
                    for (int k = startSerial; k <= endSerial; k++) {
                        if (k >= startRange && k <= endRange) {
                            qty++;
                        }
                    }
                    // Getting the non-zero quantity
                    if (qty !=0) {
                        quantity.add(String.valueOf(qty));
                    }
                }
            }
            catch (NumberFormatException e) {
                log.error("In Ranges: startSerial, endSerial should be a number" + e.getMessage());
            }
            catch (Exception e) {
                log.error("Error happened while fetching startSerial, endSerial from ranges" + e.getMessage());
            }
        }

        // Getting the total-quantity for the given range
        double count = 0;
        if (!quantity.isEmpty()) {
            for (String qty : quantity)
            {
                count+=Double.parseDouble(qty);
            }
        }
        return count;
    }

}
