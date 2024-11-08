import com.seamless.customer.bi.aggregator.aggregate.AbstractAggregator
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import wslite.rest.RESTClient
import wslite.rest.RESTClientException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired


@Slf4j
@Transactional
public class DealerStockOwnershipAggregator extends AbstractAggregator {

    static final def TABLE = "dealer_stock_ownership_aggregation"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${DealerStockOwnershipAggregator.imsUrl:}')
    String imsUrl;

    @Transactional
    @Scheduled(cron = '${DealerStockOwnershipAggregator.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("stock ownership agg started")
        ImsResponse imsResponse = fetchData()
        List<StockDataModel> dataModels = getModel(imsResponse)
        insertData(dataModels)
        log.info("stock ownership agg ended")
    }

    private ImsResponse fetchData() {
        RESTClient client = new RESTClient(imsUrl)
        def path = "v1/stock-ownership-report"
        def response
        try {

            response = client.get(path: path)
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("response fetched from ims!")
            log.debug(response.contentAsString)
            return objectMapper.readValue(response.contentAsString,ImsResponse.class)
        } catch (RESTClientException e) {
            log.error(e)
        }
    }

    private List getModel(ImsResponse res){
        List modelList=new ArrayList()

        for (StockData data: res.data){

            String id = GenerateHash.createHashString(data.productSku,data.ownerId,data.productType,data.status)
            log.info(id)
            StockDataModel model = new StockDataModel(id,data.productSku,data.ownerId,data.productType,data.status,data.stockCount)
            log.info(model.stockCount)
            modelList.add(model)

        }

        return modelList;

    }

    private void insertData(List<StockDataModel> dataModels){

        log.info("updating stock count in db...")
        def sql = "INSERT INTO ${TABLE} (id,owner_id,product_category,product_subcategory,product_type,product_sku,status,stock) VALUES (?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE stock=VALUES(stock)"

            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dataModels[i]
                        def index = 0
                        ps.setString(++index,row.id)
						ps.setString(++index,row.ownerId)
                        ps.setString(++index,row.productType)
						ps.setString(++index,row.productType)
                        ps.setString(++index,row.productType)
						ps.setString(++index,row.productSku)
                        ps.setString(++index,row.status)
                        ps.setString(++index,row.stockCount)
                    },
                    getBatchSize: { dataModels.size() }
            ] as BatchPreparedStatementSetter)
    }

}


class StockData {

    String productSku
    String ownerId
    String productType
    String status
    String stockCount

    StockData(){}

    StockData(String productSku, String ownerId, String productType, String status, String stockCount) {
        this.productSku = productSku
        this.ownerId = ownerId
        this.productType = productType
        this.status = status
        this.stockCount = stockCount
    }
}

class StockDataModel extends StockData {
    String id

    StockDataModel(String id, String productSku, String ownerId, String productType, String status, String stockCount) {
        super(productSku,ownerId,productType,status,stockCount)
        this.id = id

    }
}

class ImsResponse {
    int resultCode
    String resultDescription
    StockData[] data

    ImsResponse(){}

    ImsResponse(int resultCode, String resultDescription, StockData[] data) {
        this.resultCode = resultCode
        this.resultDescription = resultDescription
        this.data = data
    }
}
