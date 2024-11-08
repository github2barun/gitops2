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
import org.springframework.jdbc.core.ResultSetExtractor
import java.sql.ResultSet


@Slf4j
@Transactional
public class ResellerInventoryAPIReport extends AbstractAggregator {

    static final def TABLE = "reseller_inventory_stock"

    @Autowired
    protected JdbcTemplate jdbcTemplate

    @Value('${ResellerInventoryAPIReport.imsUrl:}')
    String imsUrl;

    @Transactional
    @Scheduled(cron = '${ResellerInventoryAPIReport.cron:*/3 * * * * ?}')
    public void aggregate() {
        log.info("reseller Inventory agg started")
        ImsResponse imsResponse = fetchData()
        List<StockDataModel> dataModels = getModel(imsResponse)
        insertData(dataModels)
        log.info("reseller Inventory agg ended")
    }

    private ImsResponse fetchData() {
        RESTClient client = new RESTClient(imsUrl)
        def path = "v1/stock-ownership-report"
        def response
        try {

            response = client.get(path: path)
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("response fetched from ims!")
            
            return objectMapper.readValue(response.contentAsString,ImsResponse.class)
        } catch (RESTClientException e) {
            log.error(e)
        }
    }

    private List getModel(ImsResponse res){
        List modelList=new ArrayList()

       for (StockData data: res.data.inventories){

            String id = GenerateHash.createHashString(data.productSku,data.ownerId)
            
            StockDataModel model = new StockDataModel(id,data.productSku,data.ownerId,data.status,data.stockCount)
           
            modelList.add(model)

        }

        return modelList;

    }

    private void insertData(List<StockDataModel> dataModels){

		
		def sqlQuery = "select id from reseller_inventory_stock"
		ArrayList<String> existingId = new ArrayList<String>()
		try {
			def activeReseller= jdbcTemplate.query(sqlQuery,
					new ResultSetExtractor() {
						@Override
						ArrayList<String> extractData(ResultSet rs) {
							ArrayList<String> list= new ArrayList<String>()
							while (rs.next()) {
								existingId.add(rs.getString("id"))
							}
							return list
						}
					})
		} catch (Exception e) {
			log.error(e)
		}
		
		
		 
        def sql = "INSERT INTO ${TABLE} (id,product_sku,owner_id,status,stock_count) VALUES (?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE stock_count=VALUES(stock_count)"

            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = dataModels[i]
                        def index = 0
						ps.setString(++index,row.id)
						existingId.remove(row.id)
						ps.setString(++index,row.productSku)
                        ps.setString(++index,row.ownerId)
						ps.setString(++index,row.status)
                        ps.setString(++index,row.stockCount)
                    },
                    getBatchSize: { dataModels.size() }
            ] as BatchPreparedStatementSetter)
			
			String listId = "";
			for (String s : existingId)
			{
			   listId = listId + "'" + s + "',";
			}
			
			if(listId!="") 
			{
				listId = listId.substring(0, listId.length() - 1);
				sql = "update  ${TABLE} set stock_count=? where id in (" + listId  + ")"
				batchUpdate = jdbcTemplate.update(sql,0)
			}
    }

}



class Data {
	Integer total;
	StockData[] inventories;
}

class StockData {

    String productSku
    String ownerId
    String status
    String stockCount

    StockData(){}

    StockData(String productSku, String ownerId , String status, String stockCount) {
        this.productSku = productSku
        this.ownerId = ownerId
		this.status = status
        this.stockCount = stockCount
    }
}

class StockDataModel extends StockData {
    String id

    StockDataModel(String id, String productSku, String ownerId, String status, String stockCount) {
        super(productSku,ownerId,status,stockCount)
        this.id = id

    }
}

class ImsResponse {
    int resultCode
    String resultDescription
    Data data

    ImsResponse(){}

    ImsResponse(int resultCode, String resultDescription, StockData[] data) {
        this.resultCode = resultCode
        this.resultDescription = resultDescription
        this.data = data
    }
}
