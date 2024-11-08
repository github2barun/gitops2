package com.seamless.customer.bi.aggregator.aggregate

import com.fasterxml.jackson.databind.ObjectMapper
import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import wslite.rest.RESTClient
import wslite.rest.RESTClientException

@Slf4j
@Transactional
public class StockOwnerShipReport extends AbstractAggregator {

    static final def TABLE = "reseller_current_stock"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${StockOwnerShipReport.imsUrl:}')
    String imsUrl;
    @Value('${StockOwnerShipReport.offset:0}')
    Integer offset
    @Value('${StockOwnerShipReport.limit:100}')
    Integer limit;
    @Value('${StockOwnerShipReport.status:Available}')
    String status;

    @Transactional
    @Scheduled(cron = '${StockOwnerShipReport.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("********** StockOwnerShipReport Aggregator started at " + new Date());
        ImsResponse imsResponse = fetchData()
        List<StockDataModel> dataModels = getModel(imsResponse)
        insertData(dataModels)
        log.info("********** StockOwnerShipReport Aggregator ended at " + new Date());
    }

    private ImsResponse fetchData() {
        RESTClient client = new RESTClient(imsUrl)

        ImsResponse imsResponse;
        def path = "v1/stock-ownership-report?offset=" + offset + "&limit=" + limit + "&status=" + status
        def response
        try {
            while (true) {
                path = "v1/stock-ownership-report?offset=" + offset + "&limit=" + limit + "&status=" + status

                response = client.get(path: path)
                ObjectMapper objectMapper = new ObjectMapper();
                log.info("response fetched from ims!")
                log.debug(response.contentAsString)
                if (imsResponse == null) {
                    imsResponse = objectMapper.readValue(response.contentAsString, ImsResponse.class)
                } else {
                    imsResponse.data.inventories = imsResponse.data.inventories + (objectMapper.readValue(response.contentAsString, ImsResponse.class).data.inventories)
                }
                if ((objectMapper.readValue(response.contentAsString, ImsResponse.class).data.inventories.length < limit)) {
                    break;
                }
                offset = offset + limit;
            }
            return imsResponse;
        } catch (RESTClientException e) {
            log.error(e)
        }
    }

    private List getModel(ImsResponse res) {
        List modelList = new ArrayList()

        if (res.data != null && res.data.inventories != null && res.data.inventories.length != 0) {
            for (StockData data : res.data.inventories) {
                String ownerId = data.ownerId
                java.text.DateFormat df = new java.text.SimpleDateFormat("ddMMyyyy");
                String dateStr = df.format(new Date())
                Date dateObj = df.parse(dateStr)
                log.info("Date: " + dateObj)

                Calendar cal = Calendar.getInstance();
                cal.setTime(dateObj);

                log.info("Week#: " + cal.get(Calendar.WEEK_OF_YEAR))
                Integer weekNumber = cal.get(Calendar.WEEK_OF_YEAR);
                log.info("Year: " + cal.get(Calendar.YEAR))
                Integer year = cal.get(Calendar.YEAR)

                String id = GenerateHash.createHashString(
                        data.productSku,
                        data.productCategory,
                        data.productSubCategory,
                        ownerId,
                        dateStr
                )

                log.info(id)
                StockDataModel model = new StockDataModel(id, weekNumber, year, dateObj, data.productSku, data.productSubCategory,
                        ownerId, data.productType, data.status, data.productCategory, data.stockCount)

                modelList.add(model)
            }
        }
        return modelList;
    }

    private void insertData(List<StockDataModel> dataModels) {

        log.info("updating stock count in db...")
        def sql = "INSERT INTO ${TABLE} (id,product,reseller_id,product_type,product_category,product_subcategory,status,stock_count,date,weekNumber,year) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE stock_count=VALUES(stock_count)"

        def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                setValues   : { ps, i ->
                    def row = dataModels[i]
                    def index = 0
                    ps.setString(++index, row.id)
                    ps.setString(++index, row.productSku)
                    ps.setString(++index, row.ownerId)
                    ps.setString(++index, row.productType)
                    ps.setString(++index, row.productCategory)
                    ps.setString(++index, row.productSubCategory)
                    ps.setString(++index, row.status)
                    ps.setLong(++index, row.stockCount)
                    ps.setDate(++index, new java.sql.Date(row.date.getTime()))
                    ps.setLong(++index, row.weekNumber)
                    ps.setLong(++index, row.year)
                },
                getBatchSize: { dataModels.size() }
        ] as BatchPreparedStatementSetter)
    }

}

class StockData {
    String productSku
    String productSubCategory
    String ownerId
    String productType
    String status
    String productCategory
    Long stockCount

    StockData() {}

    StockData(String productSku, String productSubCategory, String ownerId, String productType, String status, String productCategory, Long stockCount) {
        this.productSku = productSku
        this.productSubCategory = productSubCategory
        this.ownerId = ownerId
        this.productType = productType
        this.status = status
        this.productCategory = productCategory
        this.stockCount = stockCount
    }
}

class StockDataModel extends StockData {
    String id
    Integer weekNumber
    Integer year
    Date date

    StockDataModel(String id, Integer weekNumber, Integer year, Date date, String productSku, String productSubCategory,
                   String ownerId, String productType, String status, String productCategory, Long stockCount) {
        super(productSku, productSubCategory, ownerId, productType, status, productCategory, stockCount)
        this.id = id
        this.weekNumber = weekNumber
        this.year = year
        this.date = date
    }
}

class Data {
    Integer total;
    StockData[] inventories;
}

class ImsResponse {
    int resultCode
    String resultDescription
    Data data

    ImsResponse() {}

    ImsResponse(int resultCode, String resultDescription, Data data) {
        this.resultCode = resultCode
        this.resultDescription = resultDescription
        this.data = data
    }
}