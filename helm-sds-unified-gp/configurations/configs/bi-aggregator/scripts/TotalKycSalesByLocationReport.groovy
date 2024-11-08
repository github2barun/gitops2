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
import org.elasticsearch.script.Script
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
public class TotalKycSalesByLocationReport extends AbstractAggregator {
    static final def TABLE = "total_kyc_sales_by_location"

    @Autowired
    RestHighLevelClient client;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value('${TotalKycSalesByLocationReport.bulkInsertionMode:false}')
    boolean bulkInsertionMode;

    @Value('${TotalKycSalesByLocationReport.bulkInsertionModeFromDateString:2021-10-05}')
    String bulkInsertionModeFromDateString;

    @Value('${TotalKycSalesByLocationReport.bulkInsertionModeToDateString:2021-10-08}')
    String bulkInsertionModeToDateString;
    
    @Value('${TotalKycSalesByLocationReport.eventName:ADD_KYC}')
    String eventName

	@Value('${DateUtil.timeOffset:+5h+30m}')
	String timeOffset;
	
    @Transactional
    @Scheduled(cron = '${TotalKycSalesByLocationReport.cron:*/3 * * * * ?}')
    public void aggregate() {

        log.info("TotalKycSalesByLocation Aggregator started***************************************************************************" + new Date());
        if (bulkInsertionMode) {

            log.info("bulkInsertionModeFromDateString: " + bulkInsertionModeFromDateString);
            log.info("bulkInsertionModeToDateString: " + bulkInsertionModeToDateString);

            List<String> indices = DateUtil.getIndexList(bulkInsertionModeFromDateString, bulkInsertionModeToDateString);  //need to change

            for (String index : indices) {
                //fetch data from ES
                try {
                    List<TotalKycSalesByRegionAggregatorModel> transactionSummaryModels = aggregateDataES(index,
                            bulkInsertionModeFromDateString, bulkInsertionModeToDateString)
                    insertAggregation(transactionSummaryModels);
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

                log.info(index.toString())
                //fetch data from ES
                List<TotalKycSalesByRegionAggregatorModel> transactionSummaryModels = aggregateDataES(index.getIndexName(), index
                        .getStartDate(), index.getEndDate());
                insertAggregation(transactionSummaryModels);
            }
        }

        log.info("TotalKycSalesByLocation Aggregator ended**************************************************************************");
    }


    private List<TotalKycSalesByRegionAggregatorModel> aggregateDataES(String index, String fromDate, String toDate) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = fetchInput(fromDate, toDate);
        searchRequest.source(searchSourceBuilder);
        List<TotalKycSalesByRegionAggregatorModel> transactionSummaryModels = generateResponse(searchRequest);
        return transactionSummaryModels;
    }

    private SearchSourceBuilder fetchInput(String fromDate, String toDate) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        DateHistogramValuesSourceBuilder dateHistoByDay = new DateHistogramValuesSourceBuilder("timestamp")
                .field("timestamp").fixedInterval(DateHistogramInterval.days(1)).format("iso8601").missingBucket(true);
        

        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(dateHistoByDay);
        sources.add(new TermsValuesSourceBuilder("resellerId").field("user.userId.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("resellerPath").field("user.resellerPath.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("simType").field("kyc.simType.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("simBrand").field("kyc.brandName.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("latitude").field("kyc.locLatitude.keyword").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("longitude").field("kyc.locLongitude.keyword").missingBucket(true));
        //sources.add(new TermsValuesSourceBuilder("timestamp_date").field("timestamp").missingBucket(true));
        sources.add(new TermsValuesSourceBuilder("region").field("kyc.rms.region.name.keyword").missingBucket(true));

        CompositeAggregationBuilder compositeBuilder = new CompositeAggregationBuilder("TotalKycSalesByLocation",
                sources).size(10000);

        
			//compositeBuilder.subAggregation(AggregationBuilders.sum("sumOfTransactionAmounts").field("oms.transactionAmount"))

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

    private List<TotalKycSalesByRegionAggregatorModel> generateResponse(SearchRequest searchRequest) {
        List<TotalKycSalesByRegionAggregatorModel> transactionSummaryModelList = new ArrayList<>();
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
            ParsedComposite parsedComposite = aggregations.asMap().get("TotalKycSalesByLocation");


			
			for (ParsedMultiBucketAggregation.ParsedBucket bucket : parsedComposite.getBuckets()) {
				LinkedHashMap<String, String> keyValuesMap = bucket.getKey();
				Date dateTimeDay = DateFormatter.formatDate(keyValuesMap.get("timestamp"));
				Calendar calender = Calendar.getInstance();
				calender.setTime(dateTimeDay);

				String id = GenerateHash.createHashString(dateTimeDay.toString(),
										 keyValuesMap.get("resellerId"),keyValuesMap.get("simType"),keyValuesMap.get("simBrand"),
						keyValuesMap.get("latitude"), keyValuesMap.get("longitude"));

					TotalKycSalesByRegionAggregatorModel transactionSummaryModel = new TotalKycSalesByRegionAggregatorModel(id,dateTimeDay,
										 keyValuesMap.get("resellerId"),keyValuesMap.get("simType"),keyValuesMap.get("simBrand"),
					   Double.parseDouble(keyValuesMap.get("latitude")),Double.parseDouble(keyValuesMap.get("longitude")), keyValuesMap.get("resellerPath"),
												keyValuesMap.get("region"), bucket.getDocCount());

				transactionSummaryModelList.add(transactionSummaryModel);
			}
		}

		return transactionSummaryModelList;
    }

	private def insertAggregation(List transactionSummaryModelList) {
		
		log.info("TotalKycSalesByLocation Aggregated into ${transactionSummaryModelList.size()} rows.")
		if (transactionSummaryModelList.size() != 0) {
			def sql = "INSERT INTO ${TABLE} (id,transaction_date,reseller_id,sim_type,sim_brand,latitude,longitude,reseller_path,region," +
					"total_sales) VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE  total_sales = VALUES(total_sales)";
			log.debug(sql)
			def batchUpdate = jdbcTemplate.batchUpdate(sql, [
					setValues   : { ps, i ->
						def row = transactionSummaryModelList[i]
						def index = 0
						ps.setString(++index, row.id)
						ps.setString(++index,""+new java.sql.Date(row.date.getTime()))
						ps.setString(++index, row.resellerId)
						ps.setString(++index,row.simType)
						ps.setString(++index,row.simBrand)
						ps.setString(++index,String.format("%.2f",row.latitude))
						ps.setString(++index,String.format("%.2f",row.longitude))
						ps.setString(++index, row.resellerPath)
						ps.setString(++index,row.region)
						ps.setString(++index, ""+row.ttlKycSalesCount)
				   },
					getBatchSize: { transactionSummaryModelList.size() }
			] as BatchPreparedStatementSetter)
		}
		
	}
		
}
class TotalKycSalesByRegionAggregatorModel {
    private String id;
    private Date date;
    private String resellerId;
    private String resellerPath;
    private String simType;
    private String simBrand;
    private double latitude;
    private double longitude;
    private String region;
    private long ttlKycSalesCount;

	TotalKycSalesByRegionAggregatorModel() {
	}

	TotalKycSalesByRegionAggregatorModel(String id,Date date, String resellerId,
									 String simType, String simBrand, double latitude, double longitude,
									String resellerPath, String region, long ttlKycSalesCount) {
		this.id = id
		this.date = date
		this.resellerId = resellerId
		this.resellerPath = resellerPath
		this.simType = simType
		this.simBrand = simBrand
		this.latitude = latitude
		this.longitude = longitude
		this.region = region
		this.ttlKycSalesCount = ttlKycSalesCount
	}

	String getId() {
        return id
    }

	void setId(String id) {
        this.id = id
    }

	Date getDate() {
        return date
    }

	void setDate(Date date) {
        this.date = date
    }

	String getResellerId() {
        return resellerId
    }

	void setResellerId(String resellerId) {
        this.resellerId = resellerId
    }

	String getResellerPath() {
        return resellerPath
    }

	void setResellerPath(String resellerPath) {
        this.resellerPath = resellerPath
    }

	String getSimType() {
        return simType
    }

	void setSimType(String simType) {
        this.simType = simType
    }
	String getSimBrand() {
        return simBrand
    }

	void setSimBrand(String simBrand) {
        this.simBrand = simBrand
    }

	double getLatitude() {
        return latitude
    }

	void setLatitude(double latitude) {
        this.latitude = latitude
    }

	double getLongitude() {
        return longitude
    }

	 void setLongitude(double longitude) {
        this.longitude = longitude
    }

	String getRegion() {
        return region
    }
	void setRegion(String region) {
        this.region = region
    }
	long getTtlKycSalesCount() {
        return ttlKycSalesCount
    }
	 void setTtlKycSalesCount(long ttlKycSalesCount) {
        this.ttlKycSalesCount = ttlKycSalesCount
    }
}