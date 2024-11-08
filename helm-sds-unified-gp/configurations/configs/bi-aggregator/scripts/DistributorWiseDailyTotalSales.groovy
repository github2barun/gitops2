package com.seamless.customer.bi.aggregator.aggregate

import com.seamless.customer.bi.aggregator.model.ReportIndex
import com.seamless.customer.bi.aggregator.util.DateFormatter
import com.seamless.customer.bi.aggregator.util.DateUtil
import com.seamless.customer.bi.aggregator.util.GenerateHash
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation
import org.elasticsearch.search.aggregations.bucket.composite.*
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

/**
 *
 *
 *
 *
 */
@Slf4j
//@DynamicMixin
public class DistributorWiseDailyTotalSales extends AbstractAggregator {
    static final def TABLE = "distributor_wise_daily_sales_summary"

    @Autowired
    RestHighLevelClient client;
    //private static final string OPERATORNAME = "operator";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${DateUtil.timeOffset:+5h+30m}')
    String timeOffset;

    @Value('${DistributorWiseDailyTotalSales.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${DistributorWiseDailyTotalSales.bulkInsertionModeFromDateString:2021-10-05}')
    String bulkInsertionModeFromDateString;

    @Value('${DistributorWiseDailyTotalSales.bulkInsertionModeToDateString:2021-10-08}')
    String bulkInsertionModeToDateString;

    @Value('${DistributorWiseDailyTotalSales.eventName:ADD_KYC}')
    String eventName

    @Transactional
    @Scheduled(cron = '${DistributorWiseDailyTotalSales.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("DistributorWiseDailyTotalSales Aggregator started***************************************************************************" + new Date());
        // def profileIdList = profileId.split(",")
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);  //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TotalKycSalesModel> totalDistributorSalesModelList = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString, eventName)
                    insertAggregation(totalDistributorSalesModelList);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                catch (Exception e){
                    log.error(e.getMessage())
                }

            }

        } else {
            List<ReportIndex> indices = DateUtil.getIndex();

            for (ReportIndex index : indices) {

              
                List<TotalKycSalesModel> totalDistributorSalesModelList = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate(), eventName);
                insertAggregation(totalDistributorSalesModelList);
            }
        }

        log.info("DistributorWiseDailyTotalSales Aggregator ended**************************************************************************");
    }


    private List<TotalKycSalesModel> aggregateDataES(String index, String fromDate, String toDate, String eventName) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate, eventName);
        searchRequest.source(searchSourceBuilder);
        List<TotalKycSalesModel> totalDistributorSalesModelList = generateResponse(searchRequest);
        return totalDistributorSalesModelList;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate, String eventName) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("timestamp")
                .field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);


        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("PosId").field("user.userId.keyword").missingBucket(true));
		CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalKycSales",
                sources).size(10000);

		if (!bulkInsertionMode) {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
                    .filter(QueryBuilders.rangeQuery("timestamp").gte("now"+timeOffset+"-3h/d").lt("now"+timeOffset+"+1h/d").includeLower(true).includeUpper(true))
            searchSourceBuilder.query(queryBuilder);
        }
        else{
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("eventName.keyword",eventName))
                    .filter(QueryBuilders.termsQuery("kyc.resultcode",0))
            searchSourceBuilder.query(queryBuilder);
        }

        searchSourceBuilder.aggregation(compositeBuilder).size(0);
        return searchSourceBuilder;
    }

    private List<TotalKycSalesModel> generateResponse(SearchRequest searchRequest) {
        List<TotalKycSalesModel> totalDistributorSalesModelList = new ArrayList<>();
       
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, COMMON_OPTIONS);
        } catch (Exception e) {
            log.error("Error mapping rule " + searchRequest + "\nError message : " + e);
        }

        log.info("*******Request:::: " + searchRequest.toString())
        RestStatus status = searchResponse.status();
        log.debug("response status -------------" + status);
		
		if (status == RestStatus.OK) {
            Aggregations aggregations = searchResponse.getAggregations();
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalKycSales");


            for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
                LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
                Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("timestamp"));
                Calendar calender = Calendar.getInstance();
                calender.setTime(dateTimeDay);
				String id = GenerateHash.createHashString(dateTimeDay.toString(), keyValuesMap.get("PosId"));
                TotalKycSalesModel totalKycSalesModel = new TotalKycSalesModel(id, dateTimeDay, keyValuesMap.get("PosId"),calender.get(calender.WEEK_OF_YEAR),
                        calender.get(calender.YEAR), bucket.getDocCount());

                totalDistributorSalesModelList.add(totalKycSalesModel);
            }
        }

        return totalDistributorSalesModelList;

    }

    private def insertAggregation(List totalDistributorSalesModelList) {

		Map<String,Integer> totalSalesDateWise = new HashMap<String, Integer>();
		def totalSalesCountPerDay=0;
        
		if (totalDistributorSalesModelList.size() != 0) {
            def sql = "INSERT INTO ${TABLE} (id,date,distributorId,weekNumber,year,total) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE total = VALUES(total)";
            log.debug(sql)
            def batchUpdate = jdbcTemplate.batchUpdate(sql, [
                    setValues   : { ps, i ->
                        def row = totalDistributorSalesModelList[i]
                        def index = 0
						def distributorSalesDate=new java.sql.Date(row.salesDate.getTime());
                        ps.setString(++index, row.id)
                        ps.setDate(++index,distributorSalesDate)
						ps.setString(++index, row.posId)
						if(totalSalesDateWise.containsKey(distributorSalesDate))
						{
							totalSalesCountPerDay=totalSalesDateWise.get(distributorSalesDate)
						}
						totalSalesCountPerDay=totalSalesCountPerDay+row.count
                        ps.setInt(++index, row.weekNumber)
                        ps.setInt(++index, row.year)
                        ps.setLong(++index, row.count)
						
						totalSalesDateWise.put(distributorSalesDate,totalSalesCountPerDay)
						totalSalesCountPerDay=0;

                    },
                    getBatchSize: { totalDistributorSalesModelList.size() }
            ] as BatchPreparedStatementSetter)
			
        }
		
		
		List<TotalSalesModel> totalSalesModelList = new ArrayList<>();
		
		for (Map.Entry<String,Integer> entry : totalSalesDateWise.entrySet())
		{
			TotalSalesModel TotalSalesModel = new TotalSalesModel(entry.getKey(),entry.getValue());
			totalSalesModelList.add(TotalSalesModel);
		}
		if (totalSalesModelList.size() != 0) {
			def sql = "INSERT INTO daily_total_sales_summary (date,total_sales) VALUES (?,?) ON DUPLICATE KEY UPDATE date = VALUES(date)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = totalSalesModelList[i]
						def index = 0
						ps.setDate(++index, row.salesDate)
						ps.setLong(++index, row.count)
					},
					getBatchSize: { totalSalesModelList.size() }
			] as BatchPreparedStatementSetter)
			
		}
		
    }

}
class TotalKycSalesModel {
    private String id;
    private Date salesDate;
    private String posId;
   
    
    private int weekNumber;
    private int year;
    
    private long count;


    public TotalKycSalesModel(String id, Date salesDate, String posId, int weekNumber,
                              int year,Long count) {
        this.id = id;
        this.salesDate = salesDate;
        this.posId=posId;
       
		this.count = count;
        this.weekNumber = weekNumber;
        this.year = year;
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPosId() {
        return posId;
    }

    int getWeekNumber() {
        return weekNumber
    }

    void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber
    }

    int getYear() {
        return year
    }

    void setYear(int year) {
        this.year = year
    }


    public void setPosId(String posId) {
        this.posId = posId;
    }

    public Date getSalesDate() {
        return salesDate;
    }

    public void setSalesDate(Date salesDate) {
        this.salesDate = salesDate;
    }

	 public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }


}

class TotalSalesModel{

	private Date salesDate;
	private long count;
	
	public TotalSalesModel(Date salesDate,Long count) {
		this.salesDate = salesDate;
		this.count = count;
	}


	public Date getSalesDate() {
		return salesDate;
	}

	public void setSalesDate(Date salesDate) {
		this.salesDate = salesDate;
	}

	 public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	
	
	}
