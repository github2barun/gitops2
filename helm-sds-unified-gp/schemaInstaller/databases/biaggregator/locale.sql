use bi;

TRUNCATE TABLE report_list;
TRUNCATE TABLE report_metadata;
TRUNCATE TABLE report_channel_access_control;
TRUNCATE TABLE report_category_mapping;
TRUNCATE TABLE report_access_control;
TRUNCATE TABLE reseller_current_status;
TRUNCATE TABLE reseller_current_status_additional_info;

INSERT INTO `report_list` (`id`, `name`, `grouping`, `query`, `data_source`, `extra_field_1`, `extra_field_2`)
VALUES
    (34, 'search_transaction', 'search', '{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"transactionNumber\":{\"value\":\"<:transactionNumber:>\"}}},{\"terms\":{\"transactionNumber.keyword\":\"<-:transactionNumber:->\",\"boost\":1}}]}}]}}}}', 'elastic', null, null),
    (35, 'transaction_status_v2', 'search', '{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"query\":{\"bool\":{\"must\":[{\"term\":{\"_id\":\"<:ers_reference:>\"}},{\"term\":{\"senderMSISDN\":\"<:reseller_msisdn:>\"}}]}}}}', 'elastic', null, null),
    (43, 'All Transactions Report', 'sales', '{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"size\":\"<:size:>\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"ersReference\":{\"value\":\"<:ErsReference:>\"}}},{\"term\":{\"ersReference.keyword\":\"<:ErsReference:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"batchId\":{\"value\":\"<:batchId:>\"}}},{\"term\":{\"batchId.keyword\":\"<:batchId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerId\":{\"value\":\"<:SenderResellerId:>\"}}},{\"term\":{\"senderResellerId.keyword\":\"<:SenderResellerId:>\"}},{\"term\":{\"senderResellerId.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerType\":{\"value\":\"<:SenderResellerType:>\"}}},{\"term\":{\"senderResellerType.keyword\":\"<:SenderResellerType:>\"}},{\"term\":{\"senderResellerType.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerType\":{\"value\":\"<:ReceiverResellerType:>\"}}},{\"term\":{\"receiverResellerType.keyword\":\"<:ReceiverResellerType:>\"}},{\"term\":{\"receiverResellerType.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerId\":{\"value\":\"<:ReceiverResellerId:>\"}}},{\"term\":{\"receiverResellerId.keyword\":\"<:ReceiverResellerId:>\"}},{\"term\":{\"receiverResellerId.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderMSISDN\":{\"value\":\"<:SenderMSISDN:>\"}}},{\"term\":{\"senderMSISDN.keyword\":\"<:SenderMSISDN:>\"}},{\"term\":{\"senderMSISDN.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverMSISDN\":{\"value\":\"<:ReceiverMSISDN:>\"}}},{\"term\":{\"receiverMSISDN.keyword\":\"<:ReceiverMSISDN:>\"}},{\"term\":{\"receiverMSISDN.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"transactionStatus\":{\"value\":\"<:ResultStatus:>\"}}},{\"term\":{\"transactionStatus.keyword\":\"<:ResultStatus:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"transactionProfile\":{\"value\":\"<:TransactionProfile:>\"}}},{\"term\":{\"transactionProfile.keyword\":\"<:TransactionProfile:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"productSKU\":{\"value\":\"<:ProductSKU:>\"}}},{\"term\":{\"productSKU.keyword\":\"<:ProductSKU:>\"}}]}},{\"range\":{\"timestamp\":{\"from\":\"<:fromDate:>\",\"to\":\"<:toDate:>\"}}},{\"range\":{\"transactionAmount\":{\"gte\":\"<:fromAmount:>\",\"lte\":\"<:toAmount:>\"}}}]}},\"sort\":[{\"timestamp\":{\"order\":\"<:sort:>\"}}],\"_source\":{\"includes\":[\"ersReference\",\"timestamp\",\"senderResellerId\",\"senderMSISDN\",\"receiverResellerId\",\"receiverMSISDN\",\"transactionProfile\",\"productSKU\",\"transactionAmount\",\"resultCode\",\"resultMessage\",\"originalErsReference\",\"senderResellerType\",\"receiverResellerType\",\"transactionStatus\",\"batchId\"],\"excludes\":[]}}}', 'elastic', NULL, NULL),
    (44, 'Is_reversible_transaction', 'audit', '{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wildcard\":{\"ersReference.keyword\":{\"value\":\"<:ErsReference:>\"}}}]}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"_source\":{\"includes\":[\"isTransactionReversible\"]},\"size\":\"1\"}}', 'elastic', null, null),
    (45, 'pending_transactions', 'audit', '{\"elasticIndex\":{\"indexName\":\"pending_transactions\",\"isDataWeeklyIndexed\":false},\"elasticQuery\":{\"size\":\"<:size:>\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"ersReference\":{\"value\":\"<:ErsReference:>\"}}},{\"term\":{\"ersReference.keyword\":\"<:ErsReference:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"batchId\":{\"value\":\"<:batchId:>\"}}},{\"term\":{\"batchId.keyword\":\"<:batchId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerId\":{\"value\":\"<:SenderResellerId:>\"}}},{\"term\":{\"senderResellerId.keyword\":\"<:SenderResellerId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerType\":{\"value\":\"<:SenderResellerType:>\"}}},{\"term\":{\"senderResellerType.keyword\":\"<:SenderResellerType:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerType\":{\"value\":\"<:ReceiverResellerType:>\"}}},{\"term\":{\"receiverResellerType.keyword\":\"<:ReceiverResellerType:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerId\":{\"value\":\"<:ReceiverResellerId:>\"}}},{\"term\":{\"receiverResellerId.keyword\":\"<:ReceiverResellerId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderMSISDN\":{\"value\":\"<:SenderMSISDN:>\"}}},{\"term\":{\"senderMSISDN.keyword\":\"<:SenderMSISDN:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverMSISDN\":{\"value\":\"<:ReceiverMSISDN:>\"}}},{\"term\":{\"receiverMSISDN.keyword\":\"<:ReceiverMSISDN:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"transactionStatus\":{\"value\":\"<:ResultStatus:>\"}}},{\"term\":{\"transactionStatus.keyword\":\"<:ResultStatus:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"transactionProfile\":{\"value\":\"<:TransactionProfile:>\"}}},{\"term\":{\"transactionProfile.keyword\":\"<:TransactionProfile:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"productSKU\":{\"value\":\"<:ProductSKU:>\"}}},{\"term\":{\"productSKU.keyword\":\"<:ProductSKU:>\"}}]}},{\"range\":{\"timestamp\":{\"from\":\"<:fromDate:>\",\"to\":\"<:toDate:>\"}}},{\"range\":{\"transactionAmount\":{\"gte\":\"<:fromAmount:>\",\"lte\":\"<:toAmount:>\"}}}]}},\"sort\":[{\"timestamp\":{\"order\":\"<:sort:>\"}}],\"_source\":{\"includes\":[\"ersReference\",\"timestamp\",\"senderResellerId\",\"senderMSISDN\",\"receiverResellerId\",\"receiverMSISDN\",\"transactionProfile\",\"productSKU\",\"transactionAmount\",\"resultCode\",\"resultMessage\",\"originalErsReference\",\"senderResellerType\",\"receiverResellerType\",\"transactionStatus\",\"batchId\"],\"excludes\":[]}}}', 'elastic', null, null),
    (46, 'Hierarchy Transaction Search', 'sales', '{\"elasticIndex\":{\"indexName\":\"data_lake_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"size\":\"<:size:>\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"ersReference\":{\"value\":\"<:ErsReference:>\"}}},{\"term\":{\"ersReference.keyword\":\"<:ErsReference:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"batchId\":{\"value\":\"<:batchId:>\"}}},{\"term\":{\"batchId.keyword\":\"<:batchId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerId\":{\"value\":\"<:SenderResellerId:>\"}}},{\"term\":{\"senderResellerId.keyword\":\"<:SenderResellerId:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerId\":{\"value\":\"<:ReceiverResellerId:>\"}}},{\"term\":{\"receiverResellerId.keyword\":\"<:ReceiverResellerId:>\"}},{\"term\":{\"receiverResellerId.keyword\":\"\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverResellerType\":{\"value\":\"<:ReceiverResellerType:>\"}}},{\"term\":{\"receiverResellerType.keyword\":\"<:ReceiverResellerType:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderMSISDN\":{\"value\":\"<:SenderMSISDN:>\"}}},{\"term\":{\"senderMSISDN.keyword\":\"<:SenderMSISDN:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"receiverMSISDN\":{\"value\":\"<:ReceiverMSISDN:>\"}}},{\"term\":{\"receiverMSISDN.keyword\":\"<:ReceiverMSISDN:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"resultMessage\":{\"value\":\"<:ResultStatus:>\"}}},{\"term\":{\"resultMessage.keyword\":\"<:ResultStatus:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"transactionProfile\":{\"value\":\"<:TransactionProfile:>\"}}},{\"term\":{\"transactionProfile.keyword\":\"<:TransactionProfile:>\"}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"productSKU\":{\"value\":\"<:ProductSKU:>\"}}},{\"term\":{\"productSKU.keyword\":\"<:ProductSKU:>\"}}]}},{\"range\":{\"timestamp\":{\"from\":\"<:fromDate:>\",\"to\":\"<:toDate:>\"}}},{\"range\":{\"transactionAmount\":{\"gte\":\"<:fromAmount:>\",\"lte\":\"<:toAmount:>\"}}},{\"bool\":{\"should\":[{\"wildcard\":{\"senderResellerPath.keyword\":{\"wildcard\":\"<:resellerPath:>*\",\"boost\":1}}},{\"term\":{\"receiverResellerId.keyword\":{\"value\":\"<:userId:>\",\"boost\":1}}}],\"adjust_pure_negative\":true,\"boost\":1}}]}},\"sort\":[{\"timestamp\":{\"order\":\"<:sort:>\"}}],\"_source\":{\"includes\":[\"ersReference\",\"timestamp\",\"senderResellerId\",\"senderMSISDN\",\"receiverResellerId\",\"receiverMSISDN\",\"transactionProfile\",\"productSKU\",\"transactionAmount\",\"resultCode\",\"resultMessage\",\"originalErsReference\",\"batchId\",\"senderResellerType\",\"receiverResellerType\"],\"excludes\":[]}}}', 'elastic', null, null),
    (105, 'top_reseller', 'support', 'SELECT IFNULL(resellerTypeId,\'N/A\') AS \'reseller_level\', IFNULL(account_type,\'N/A\') AS \'account_type\', IFNULL(resellerName,\'N/A\') AS \'reseller_name\', IFNULL(resellerId,\'N/A\') AS \'reseller_id\', IFNULL(resellerMSISDN,\'N/A\') AS \'reseller_MSISDN\', CAST(IFNULL(SUM(transactionAmount),0) AS DECIMAL) AS amount, IFNULL(SUM(COUNT),0) AS \'quantity\', IFNULL(currency,\'BDT\') AS \'currency\', IFNULL(region, \'N/A\') AS \'region\' FROM std_sales_trend_aggregation WHERE aggregationDate BETWEEN :fromDate AND :toDate AND( \'ALL\' IN (:reseller_type) OR resellerTypeId IN (:reseller_type)) AND ( \'ALL\' IN (:accountType) OR account_type IN (:accountType)) GROUP BY resellerId ORDER BY :sortBy DESC', 'mysql', null, null),
    (107, 'reseller_with_zero_balance', 'sales', 'SELECT IFNULL(rcs.reseller_type_id,\'N/A\') AS \'Reseller Level\', IFNULL(rcs.reseller_id,\'N/A\') AS \'reseller_id\', IFNULL(acc.accountTypeId, \'RESELLER\') AS \'Account Type\', IF( l.`last_transaction_date` BETWEEN :fromDate AND :toDate, l.`last_transaction_date`, \'N/A\') AS \'Last Date of Transaction\', IF ( l.`last_transaction_date` BETWEEN :fromDate AND :toDate,l.`last_transaction_amount`, 0) AS \'Amount Of Last Transaction\', COALESCE(d.`balance_transfer_in`,0) AS \'Total Credit Since Activation\', IFNULL(l.`last_transaction_currency`,\'BDT\') AS \'currency\', IFNULL(rcs.region, \'N/A\') AS \'region\', IFNULL(acc.balance, 0) AS \'balance\' FROM bi.reseller_current_status rcs JOIN accountmanagement.accounts acc ON rcs.reseller_id = acc.accountId LEFT JOIN bi.detail_balance_report_aggregation d ON rcs.reseller_id = d.reseller_id LEFT JOIN bi.`last_transaction_aggregator` l ON ( rcs.`reseller_id`=l.`reseller_id`) WHERE ( acc.balance = 0) AND ( \'ALL\' IN (:reseller_level) OR rcs.reseller_type_id IN (:reseller_level)) AND ( \'ALL\' IN (:accountType) OR acc.accountTypeId IN (:accountType)) ORDER BY rcs.`reseller_id` ASC', 'mysql', null, null),
    (112, 'electronic_recharge_per_day', 'marketing', 'SELECT IFNULL(aggregation_date,\'N/A\') AS \'aggregation_date\', IFNULL(channel,\'N/A\') AS \'channel\', IFNULL(r2r_count,0) AS \'r2r_count\', FORMAT(IFNULL(r2r_amount,0),2) AS \'r2r_amount\', IFNULL( r2s_count,0) AS \'r2s_count\', FORMAT(IFNULL(r2s_amount,0),2) AS \'r2s_amount\', IFNULL(currency,\'BDT\') AS \'currency\' FROM electronic_recharge WHERE aggregation_date BETWEEN :fromDate AND :toDate AND( \'ALL\' IN (:channel) OR channel IN (:channel)) ORDER BY aggregation_date DESC', 'mysql', null, null),
    (116, 'active_inactive_reseller', 'sales', 'SELECT IFNULL(COUNT(DISTINCT(tag)),0) AS `Total Count`, IFNULL(t2.active,0) AS \'Active Count\', IFNULL((COUNT(DISTINCT(tag))-active),0) AS `Inactive Count`, IFNULL(ROUND(active*100/COUNT(DISTINCT(tag)),2),0) AS `Active%`, IFNULL(ROUND(((COUNT(DISTINCT(tag))-active))*100/COUNT(DISTINCT(tag)),2),0) AS `Inactive%` FROM Refill.commission_receivers CROSS JOIN( SELECT COUNT(DISTINCT(resellerId)) AS `active` FROM std_sales_trend_aggregation WHERE aggregationDate BETWEEN :fromDate AND :toDate)t2 WHERE status=0', 'mysql', null, null),
    (119, 'dormant_reseller', 'sales', 'SELECT DISTINCT rcs.reseller_id, rcs.MSISDN AS \'reseller_msisdn\', rcs.reseller_type_id AS \'reseller_type\', IFNULL(DATE(t2.last_transaction_date),\'N/A\') AS \'last_transaction_date\', IFNULL(t2.receiver_msisdn,\'N/A\') AS \'Receiver MSISDN\', IFNULL(t2.last_transaction_type,\'N/A\') AS \'last_transaction_type\', IFNULL(t1.balance_transfer_in,0) AS \'total_credit_since_activation\' , FORMAT(IFNULL(ama.balance,0),2) AS \'current_balance\', IFNULL(t1.currency,\'BDT\') AS \'currency\', DATEDIFF(now(), IFNULL(t2.last_transaction_date, CURRENT_DATE())) AS \'Ageing\', IFNULL(t2.account_id, \'N/A\') AS \'account_id\', IFNULL(rcs.region, \'N/A\') AS \'region\' FROM bi.reseller_current_status rcs LEFT JOIN bi.detail_balance_report_aggregation t1 ON rcs.reseller_id = t1.reseller_id LEFT JOIN bi.last_transaction_aggregator t2 ON rcs.reseller_id = t2.reseller_id LEFT JOIN accountmanagement.accounts ama ON rcs.reseller_id = ama.accountId WHERE rcs.reseller_id NOT IN( SELECT reseller_id FROM bi.last_transaction_aggregator WHERE last_transaction_date BETWEEN :fromDate AND CURRENT_DATE()) AND ( \'ALL\' IN (:reseller_type) OR rcs.reseller_type_id IN (:reseller_type)) AND ( \'ALL\' IN (:reseller_Id) OR rcs.reseller_id = :reseller_Id) ORDER BY rcs.reseller_id', 'mysql', null, null),
    (121, 'reseller_balance_report', 'reseller', 'select DATE_FORMAT(now() ,\'%Y-%m-%d %h:%m:%s\') AS \'Date Time\', rcs.reseller_id AS \'Reseller Id\', rcs.reseller_name AS \'Reseller Name\', IFNULL(rcs.MSISDN, \'N/A\') AS \'MSISDN\', IFNULL(acc.accountTypeId, \'N/A\') AS \'Account Type\', ifnull(rcs.region,\'N/A\') AS \'Region\', rcs.reseller_parent AS \'Reseller Parent\', rcs.reseller_status AS \'Reseller Status\', rcs.reseller_type_id AS \'Reseller Type\', acc.balance AS \'Current Balance\' from bi.reseller_current_status rcs JOIN accountmanagement.accounts acc ON rcs.reseller_id = acc.accountId WHERE( \'ALL\' IN (:reseller_type) OR rcs.reseller_type_id IN (:reseller_type)) AND ( \'All\' IN (:reseller_Id) OR rcs.reseller_id IN (:reseller_Id))  AND ( \'ALL\' IN (:accountType) OR acc.accountTypeId IN (:accountType)) ', 'mysql', null, null),
    (139, 'transaction_summary_report', 'sales', 'SELECT IFNULL(ssta.resellerId, \"N/A\") AS \'resellerId\', IFNULL(ssta.resellerMSISDN, \"N/A\") AS \'resellerMSISDN\', IFNULL(ssta.resellerTypeId, \"N/A\") AS \'resellerType\', IFNULL(ssta.transaction_type, \"N/A\") AS \'transactionType\', IFNULL(dbra.current_balance, \"N/A\") AS \'currentBalance\', IFNULL(dbra.reseller_parent, \"N/A\") AS \'resellerParent\', IFNULL(ssta.count, \"N/A\") AS \'transactionCount\', IFNULL(ssta.currency, \"BDT\") AS \'currency\', IFNULL(ssta.transactionAmount, \"N/A\") AS \'transactionAmount\', IFNULL(dbra.reseller_path, \"N/A\") AS \'resellerPath\' FROM bi.std_sales_trend_aggregation ssta LEFT JOIN bi.detail_balance_report_aggregation dbra ON ssta.resellerId = dbra.reseller_id WHERE( \'ALL\' IN(:transactionType) OR ssta.transaction_type IN (:transactionType)) AND ( \'ALL\' IN (:reseller_Id) OR ssta.resellerId IN (:reseller_Id)) AND ssta.aggregationDate BETWEEN :fromDate AND :toDate', 'mysql', null, null),
    (152, 'transaction_details_report', 'sales', 'SELECT DATE_FORMAT(atd.transaction_date, \"%Y-%m-%d %H:%i:%s\") AS \'Date\', atd.transaction_reference AS \'ERS Reference\', IF( atd.client_reference=\'null\', \"N/A\", atd.client_reference) AS \'Client Reference\', IFNULL(atd.seller_msisdn, \"N/A\") AS \'Sender MSISDN\', IFNULL(atd.seller_closing_balance,\"N/A\") AS \'Sender Balance\', IFNULL(atd.buyer_msisdn,\"N/A\") AS \"Receiver MSISDN\", IFNULL(atd.amount,\"N/A\") AS \'Transaction Amount\', IFNULL(atd.operation_type, \"N/A\") AS \'Transaction Profile\', IFNULL(atd.productsku, \"N/A\") AS \'Product Name/Denomination\', IFNULL(atd.transaction_status, \"N/A\") AS \'Status\', IFNULL(atd.seller_id, \"N/A\") AS \'Sender Reseller Id\', IFNULL(atd.currency, \"BDT\") AS \'Currency\', IFNULL(dbra.region, \'N/A\') AS \'region\' FROM all_transaction_details atd LEFT JOIN detail_balance_report_aggregation dbra ON atd.seller_id = dbra.reseller_id WHERE transaction_date BETWEEN :fromDate AND :toDate AND ( \"ALL\" IN (:sender) OR \'\' = (:sender) OR ( :sender) IS NULL OR atd.seller_msisdn IN (:sender)) AND ( \"ALL\" IN (:transactionType) OR atd.operation_type IN (:transactionType)) AND ( \"ALL\" IN (:reseller_type) OR atd.seller_dealer_type IN (:reseller_type)) AND ( \"ALL\" IN (:reseller_Id) OR atd.seller_id IN (:reseller_Id))', 'mysql', null, null),
    (510, 'transaction_statistics', 'marketing', 'SELECT IFNULL(ts.channel,\'N/A\') AS Channel, IFNULL(ts.account_type,\'N/A\') AS \'Account Type\', IFNULL(ts.transaction_type,\'N/A\') AS \'Transaction Type\', IFNULL(SUM(ts.count),0) AS \'Successful Transaction Count\', FORMAT(IFNULL(SUM(ts.amount),0),2) AS \'Successful Transaction Amount\' , IFNULL(ts.currency,\'N/A\') AS \'Currency\' FROM channel_wise_day_wise ts WHERE ts.end_time_day BETWEEN :fromDate AND :toDate AND( \'ALL\' IN (:channel) OR ts.channel IN (:channel)) AND ( \'ALL\' IN (:accountType) OR ts.account_type IN (:accountType)) AND ( \'ALL\' IN (:transactionType) OR transaction_type IN (:transactionType)) GROUP BY ts.channel, ts.account_type, ts.transaction_type UNION SELECT \'Total Successful Transaction Count\', IFNULL(SUM(COUNT),0), \'Total Successful Transaction Amount\', IFNULL(SUM(amount),0), \'Currency\' , IFNULL(currency,\'N/A\') FROM channel_wise_day_wise WHERE end_time_day BETWEEN :fromDate AND :toDate AND ( \'ALL\' IN (:channel) OR channel IN (:channel)) AND ( \'ALL\' IN (:accountType) OR account_type IN (:accountType)) AND ( \'ALL\' IN (:transactionType) OR transaction_type IN (:transactionType))', 'mysql', null, null),
    (511, 'sales_trend_report', 'sales', 'SELECT IFNULL(t1.resellerId,\'N/A\') AS \'reseller_id\', IFNULL(t1.resellerName,\'N/A\') AS \'reseller_name\', IFNULL(t1.resellerMSISDN,\'N/A\') AS \'reseller_msisdn\', IFNULL(t1.resellerTypeId,\'N/A\') AS \'reseller_type_id\', IFNULL(t2.average_trend,0) AS \'average trend\', IFNULL(t2.last_week,0) AS \'last_week\', IFNULL(ROUND(t2.last_week * 100.0 / t2.average_trend, 1),\'0\') AS \'Change%\', IFNULL(t1.region, \'N/A\') AS \'region\' FROM( SELECT resellerId AS \"reseller_id\", resellerMSISDN AS \"Reseller MSISDN\" , resellerTypeId AS \"reseller_type_id\", FORMAT(SUM( IF( `aggregationDate` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 4 WEEK) AND CURRENT_DATE(),`transactionAmount`/4,0)),2) AS \"average_trend\", FORMAT(SUM( IF ( `aggregationDate` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 1 WEEK) AND CURRENT_DATE(),`transactionAmount`,0) ),2) AS \"last_week\", MAX(`aggregationDate`) AS \"max_created\" FROM std_sales_trend_aggregation GROUP BY resellerId) AS t2 JOIN std_sales_trend_aggregation t1 ON t2.reseller_id = t1.resellerId AND t2.max_created = t1.aggregationDate WHERE ( \'ALL\' IN (:reseller_type ) OR t1.resellerTypeId IN (:reseller_type)) AND ROUND(t2.last_week * 100.0 / t2.average_trend, 1) > :deviationThreshold GROUP BY resellerid ORDER BY `Change%`', 'mysql', null, null),
    (512, 'purchase_trend_report', 'sales', 'SELECT IFNULL(t1.reseller_id,\'N/A\') AS \'reseller_id\', IFNULL(t1.reseller_name,\'N/A\') AS \'reseller_name\', IFNULL(t1.reseller_msisdn,\'N/A\') AS \'reseller_msisdn\', IFNULL(t1.reseller_type_id,\'N/A\') AS \'reseller_type_id\', IFNULL(t2.average_trend,0) AS \'average_trend\', IFNULL(t2.last_week,0) AS \'last_week\', IFNULL(ROUND(t2.last_week * 100.0 / t2.average_trend, 1),\"0\") AS \'Change%\', IFNULL(t1.region, \'N/A\') AS \'region\' FROM( SELECT reseller_id AS \'reseller_id\', reseller_msisdn AS \'Reseller MSISDN\', reseller_type_id AS \'reseller_type_id\', SUM( IF( `aggregation_date` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 4 WEEK) AND CURRENT_DATE(),`amount`/4,0)) AS \'average_trend\', SUM( IF ( `aggregation_date` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 1 WEEK) AND CURRENT_DATE(),`amount`,0) ) AS \"last_week\", MAX(`aggregation_date`) AS \"max_created\" FROM receiver_wise_credit_transfer_summary GROUP BY reseller_id) AS t2 JOIN receiver_wise_credit_transfer_summary t1 ON t2.reseller_id = t1.reseller_id AND t2.max_created = t1.aggregation_date WHERE ( \'ALL\' IN (:reseller_type) OR t1.reseller_type_id IN (:reseller_type)) AND ROUND(t2.last_week * 100.0 / t2.average_trend, 1) > :deviationThreshold GROUP BY reseller_id ORDER BY `Change%`', 'mysql', null, null),
    (513, 'transaction_failure_causes_report', 'support', 'SELECT IFNULL(ts.channel,\'N/A\')    AS channel, IFNULL(transaction_type,\'N/A\') AS \'transaction_type\' , IFNULL(SUM(ts.count),0)        AS \'failure_transaction_count\', IFNULL(result_status,\'N/A\')    AS \'failure_reason\' FROM hourly_total_transactions     ts WHERE ts.end_time_day BETWEEN :fromDate AND :toDate AND ( \'ALL\' IN (:channel) OR  ts.channel IN (:channel)) AND ( result_status != \'SUCCESS\') GROUP BY ts.channel, result_status ORDER BY `failure_transaction_count` DESC', 'mysql', null, null),
    (502,'transaction_status', 'search', '{"elasticIndex":{"indexName":"data_lake_","isDataWeeklyIndexed":true},"elasticQuery":{"query":{"bool":{"must":[{"term":{"_id":"<:ers_reference:>"}},{"term":{"senderMSISDN":"<:reseller_msisdn:>"}}]}}}}', 'elastic', null, null),
    (503,'last_transaction_of_type', 'search', '{"elasticIndex":{"indexName":"data_lake_","isDataWeeklyIndexed":true},"elasticQuery":{"sort":[{"timestamp":{"order":"desc"}}],"query":{"bool":{"must":[{"term":{"transactionType.keyword":"<:transaction_type:>"}},{"term":{"senderMSISDN.keyword":"<:reseller_msisdn:>"}}]}}}}', 'elastic', null, null),
    (504,'last_transaction', 'search', '{"elasticIndex":{"indexName":"data_lake_","isDataWeeklyIndexed":true},"elasticQuery":{"sort":[{"timestamp":{"order":"desc"}}],"query":{"bool":{"must":[{"term":{"senderMSISDN.keyword":"<:reseller_msisdn:>"}}]}}}}', 'elastic', null, null),
    (505,'sales_today', 'sales', 'select count(*) as count, IFNULL(sum(amount), 0) as amount, IFNULL(currency, ''BDT'') as currency from bi.all_transaction_details where seller_msisdn = :reseller_msisdn and transaction_status = "Success" and operation_type = "TOPUP" and date(transaction_date) = CURRENT_DATE', 'mysql', null, null),
    (506,'sales_period', 'sales', 'select count(*) as count, IFNULL(sum(amount), 0) as amount, IFNULL(currency, ''BDT'') as currency from bi.all_transaction_details where seller_msisdn = :reseller_msisdn and date(transaction_date) between :fromDate and :toDate and transaction_status = "Success" and operation_type = "TOPUP"', 'mysql', null, null),
    (507,'sales_per_receiver', 'sales', 'select count(*) as count, IFNULL(sum(amount), 0) as amount, IFNULL(currency, ''BDT'') as currency from bi.all_transaction_details where seller_msisdn = :reseller_msisdn and buyer_msisdn = :receiver_MSISDN and transaction_status = "Success" and operation_type = "TOPUP"', 'mysql', null, null),
    (508,'deposit_today', 'sales', 'select count(*) as count, IFNULL(sum(amount), 0) as amount, IFNULL(currency, ''BDT'') as currency from bi.all_transaction_details where buyer_msisdn = :reseller_msisdn and transaction_status = "Success" and operation_type in ("CREDIT_TRANSFER", "TRANSFER") and date(transaction_date) = CURRENT_DATE', 'mysql', null, null),
    (509,'deposit_period', 'sales', 'select count(*) as count, IFNULL(sum(amount), 0) as amount, IFNULL(currency, ''BDT'') as currency from bi.all_transaction_details where buyer_msisdn = :reseller_msisdn and date(transaction_date) between :fromDate and :toDate  and transaction_status = "Success" and operation_type in ("CREDIT_TRANSFER", "TRANSFER")', 'mysql', null, null),
    (518, 'stock_holding_report', 'sales', 'SELECT rt.name AS reseller_type, cr.tag AS reseller_id, si.product_sku, COUNT(si.serial_number) AS current_stock, IFNULL(ort.threshold,\'\') AS threshold_value, IFNULL(rep.parameter_value,\'\') AS region, IFNULL(rep1.parameter_value,\'\') AS city, IFNULL(rep2.parameter_value,\'\') AS suburb FROM Refill.commission_receivers cr INNER JOIN Refill.reseller_types rt ON rt.type_key = cr.type_key LEFT JOIN inventory_management_system.serialized_inventory si ON si.owner_id = cr.tag LEFT JOIN alertapp.owner_resource_thresholds ort ON ort.resource_owner_id = si.owner_id AND ort.resource_id = si.product_sku LEFT JOIN Refill.reseller_extra_params rep ON rep.receiver_key = cr.receiver_key AND rep.parameter_key = \'region\' LEFT JOIN Refill.reseller_extra_params rep1 ON rep1.receiver_key = cr.receiver_key AND rep1.parameter_key = \'city\' LEFT JOIN Refill.reseller_extra_params rep2 ON rep2.receiver_key = cr.receiver_key AND rep2.parameter_key = \'suburb\' WHERE ( \'ALL\' IN (:reseller_type) OR rt.id IN (:reseller_type) ) AND si.workflow_state_id = 1 AND si.created_stamp BETWEEN \'2023-07-12 00:00:00\' AND \'2024-07-14 00:00:00\' AND ( \'ALL\' IN (:productSKU) OR si.product_sku IN (:productSKU) ) AND ( cr.reseller_path LIKE CONCAT(\"%\", :loggedInResellerId, \"/%\") OR cr.tag = :loggedInResellerId ) AND ( \'ALL\' IN (:reseller_Id) OR cr.tag IN (:reseller_Id) ) GROUP BY cr.tag, si.product_sku', 'mysql', NULL, NULL),
    (520, 'stock_holding_report_weekly_parent_child', 'sales', 'SELECT IFNULL(rcst.reseller_id, \"N/A\") AS \"ResellerID\", IFNULL(rcst.YEAR, \"N/A\") AS \"Year\", IFNULL(rcst.weekNumber, \"N/A\") AS \"WeekNumber\", IFNULL(rcst.product, \"N/A\") AS \"ProductSku\", SUM(rcst.stock_count) AS \"StockQuantity\", IFNULL(rcs.region,\"N/A\") AS \"Region\", IFNULL(rcsai.sales_area,\"N/A\") AS \"SalesArea\" FROM bi.reseller_current_stock rcst LEFT JOIN bi.reseller_current_status rcs ON rcst.reseller_id = rcs.reseller_id LEFT JOIN bi.reseller_current_status_additional_info rcsai ON rcst.reseller_id = rcsai.reseller_current_id WHERE rcst.YEAR >=(:fromYear) AND YEAR <=(:toYear) AND rcst.weekNumber >=(:fromWeek) AND rcst.weekNumber <= (:toWeek) AND ( \"ALL\" IN (:reseller_Id) OR rcst.reseller_id IN (:reseller_Id)) AND ( \"ALL\" IN ( :productSku ) OR rcst.product IN ( :productSku ) ) AND (\"ALL\" IN ( :reseller_type ) OR rcs.reseller_type_id IN ( :reseller_type )) AND (rcs.reseller_path LIKE CONCAT(\"%\", :loggedInResellerId, \"/%\") OR rcs.reseller_id = :loggedInResellerId OR rcs.reseller_path = :loggedInResellerId) GROUP BY ProductSku, ResellerID, YEAR, WeekNumber', 'mysql', NULL, NULL),
    (522, 'reseller_last_pending_transaction', 'search', '{\"elasticIndex\":{\"indexName\":\"pending_transactions\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"sort\":[{\"timestamp\":{\"order\":\"desc\"}}],\"query\":{\"bool\":{\"must\":[{\"term\":{\"transactionProfile.keyword\":\"RESELLER_LINK_SUB_RESELLER\"}},{\"term\":{\"receiverMSISDN.keyword\":\"<:reseller_msisdn:>\"}}]}}}}', 'elastic', null, null),
    (524, 'Transaction Details By Hierarchy', 'sales', 'SELECT DATE_FORMAT(atd.transaction_date, \"%Y-%m-%d %H:%i:%s\") AS \'Date\', atd.transaction_reference AS \'ERS Reference\', IF( atd.client_reference=\'null\', \"N/A\", atd.client_reference) AS \'Client Reference\', IFNULL(atd.seller_msisdn, \"N/A\") AS \'Sender MSISDN\', IFNULL(atd.seller_closing_balance,\"N/A\") AS \'Sender Balance\', IFNULL(atd.buyer_msisdn,\"N/A\") AS \"Receiver MSISDN\", IFNULL(atd.amount,\"N/A\") AS \'Transaction Amount\', IFNULL(atd.operation_type, \"N/A\") AS \'Transaction Profile\', IFNULL(atd.productsku, \"N/A\") AS \'Product Name/Denomination\', IFNULL(atd.transaction_status, \"N/A\") AS \'Status\', IFNULL(atd.seller_id, \"N/A\") AS \'Sender Reseller Id\', IFNULL(atd.currency, \"BDT\") AS \'Currency\', IFNULL(rcs.region, \'N/A\') AS \'region\' FROM all_transaction_details atd LEFT JOIN bi.reseller_current_status rcs ON atd.seller_id = rcs.reseller_id WHERE transaction_date BETWEEN :fromDate AND :toDate AND ( \"ALL\" IN (:transaction_profile) OR atd.operation_type IN (:transaction_profile)) AND ( \"ALL\" IN (:reseller_type) OR atd.seller_dealer_type IN (:reseller_type)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN atd.sender_reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE atd.seller_id IN (:reseller_Id) END ', 'mysql', null, null),
    (525, 'Transaction Summary_By Hierarchy', 'reseller', 'SELECT IFNULL(ssta.resellerId, \"N/A\") AS \'resellerId\', IFNULL(ssta.resellerMSISDN, \"N/A\") AS \'resellerMSISDN\', IFNULL(ssta.resellerTypeId, \"N/A\") AS \'resellerType\', IFNULL(ssta.transaction_type, \"N/A\") AS \'transactionType\', IFNULL(dbra.current_balance, \"N/A\") AS \'currentBalance\', IFNULL(dbra.reseller_parent, \"N/A\") AS \'resellerParent\', IFNULL(ssta.count, \"N/A\") AS \'transactionCount\', IFNULL(ssta.currency, \"BDT\") AS \'currency\', IFNULL(ssta.transactionAmount, \"N/A\") AS \'transactionAmount\', IFNULL(dbra.reseller_path, \"N/A\") AS \'resellerPath\' FROM bi.std_sales_trend_aggregation ssta LEFT JOIN bi.detail_balance_report_aggregation dbra ON ssta.resellerId = dbra.reseller_id WHERE ssta.aggregationDate BETWEEN :fromDate AND :toDate AND( \'ALL\' IN(:transaction_profile) OR ssta.transaction_type IN (:transaction_profile)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN ssta.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE ssta.resellerId IN (:reseller_Id) END ', 'mysql', null, null),
    (526, 'Reseller_Balance_Report By Hierarchy', 'reseller', 'SELECT DATE_FORMAT(now() ,\'%Y-%m-%d %h:%m:%s\') AS \'Date Time\', rcs.reseller_id AS \'Reseller Id\', rcs.reseller_name AS \'Reseller Name\', IFNULL(rcs.MSISDN, \'N/A\') AS \'MSISDN\', IFNULL(acc.accountTypeId, \'N/A\') AS \'Account Type\', ifnull(rcs.region,\'N/A\') AS \'Region\', rcs.reseller_parent AS \'Reseller Parent\', rcs.reseller_status AS \'Reseller Status\', rcs.reseller_type_id AS \'Reseller Type\', acc.balance AS \'Current Balance\' FROM bi.reseller_current_status rcs JOIN accountmanagement.accounts acc ON rcs.reseller_id = acc.accountId WHERE( \'ALL\' IN(:reseller_type) OR rcs.reseller_type_id IN(:reseller_type)) AND( \'ALL\' IN (:account_type) OR acc.accountTypeId IN (:account_type)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN rcs.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE rcs.reseller_id IN (:reseller_Id) END', 'mysql', null, null),
    (527, 'Reseller_With_Zero_Balance By Hierarchy', 'sales', 'SELECT IFNULL(rcs.reseller_type_id,\'N/A\') AS \'Reseller Level\', IFNULL(rcs.reseller_id,\'N/A\') AS \'reseller_id\', IFNULL(acc.accountTypeId, \'RESELLER\') AS \'Account Type\', IF( l.`last_transaction_date` BETWEEN :fromDate AND :toDate, l.`last_transaction_date`, \'N/A\') AS \'Last Date of Transaction\', IF ( l.`last_transaction_date` BETWEEN :fromDate AND :toDate,l.`last_transaction_amount`, 0) AS \'Amount Of Last Transaction\', COALESCE(d.`balance_transfer_in`,0) AS \'Total Credit Since Activation\', IFNULL(l.`last_transaction_currency`,\'BDT\') AS \'currency\', IFNULL(rcs.region, \'N/A\') AS \'region\', IFNULL(acc.balance, 0) AS \'balance\' FROM bi.reseller_current_status rcs JOIN accountmanagement.accounts acc ON rcs.reseller_id = acc.accountId LEFT JOIN bi.detail_balance_report_aggregation d ON rcs.reseller_id = d.reseller_id LEFT JOIN bi.`last_transaction_aggregator` l ON ( rcs.`reseller_id`=l.`reseller_id`) WHERE ( acc.balance = 0) AND ( \'ALL\' IN (:reseller_level) OR rcs.reseller_type_id IN (:reseller_level)) AND ( \'ALL\' IN (:accountType) OR acc.accountTypeId IN (:accountType)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN rcs.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE rcs.reseller_id IN (:reseller_Id) END ORDER BY rcs.`reseller_id` ASC', 'mysql', null, null),
    (529, 'dormant_reseller By Hierarchy', 'sales', 'SELECT DISTINCT rcs.reseller_id, rcs.MSISDN AS \'reseller_msisdn\', rcs.reseller_type_id AS \'reseller_type\', IFNULL(DATE(t2.last_transaction_date),\'N/A\') AS \'last_transaction_date\', IFNULL(t2.receiver_msisdn,\'N/A\') AS \'Receiver MSISDN\', IFNULL(t2.last_transaction_type,\'N/A\') AS \'last_transaction_type\', IFNULL(t1.balance_transfer_in,0) AS \'total_credit_since_activation\' , FORMAT(IFNULL(ama.balance,0),2) AS \'current_balance\', IFNULL(t1.currency,\'BDT\') AS \'currency\', DATEDIFF(now(), IFNULL(t2.last_transaction_date, CURRENT_DATE())) AS \'Ageing\', IFNULL(t2.account_id, \'N/A\') AS \'account_id\', IFNULL(rcs.region, \'N/A\') AS \'region\' FROM bi.reseller_current_status rcs LEFT JOIN bi.detail_balance_report_aggregation t1 ON rcs.reseller_id = t1.reseller_id LEFT JOIN bi.last_transaction_aggregator t2 ON rcs.reseller_id = t2.reseller_id LEFT JOIN accountmanagement.accounts ama ON rcs.reseller_id = ama.accountId WHERE rcs.reseller_id NOT IN( SELECT reseller_id FROM bi.last_transaction_aggregator WHERE last_transaction_date BETWEEN :fromDate AND CURRENT_DATE()) AND ( \'ALL\' IN (:reseller_type) OR rcs.reseller_type_id IN (:reseller_type)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN rcs.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE rcs.reseller_Id IN (:reseller_Id) END ORDER BY rcs.reseller_id', 'mysql', null, null),
    (530, 'purchase_trend_report By Hierarchy', 'sales', 'SELECT IFNULL(t1.reseller_id,\'N/A\') AS \'reseller_id\', IFNULL(t1.reseller_name,\'N/A\') AS \'reseller_name\', IFNULL(t1.reseller_msisdn,\'N/A\') AS \'reseller_msisdn\', IFNULL(t1.reseller_type_id,\'N/A\') AS \'reseller_type_id\', IFNULL(t2.average_trend,0) AS \'average_trend\', IFNULL(t2.last_week,0) AS \'last_week\', IFNULL(ROUND(t2.last_week * 100.0 / t2.average_trend, 1),\"0\") AS \'Change%\', IFNULL(t1.region, \'N/A\') AS \'region\' FROM( SELECT reseller_id AS \'reseller_id\', reseller_msisdn AS \'Reseller MSISDN\', reseller_type_id AS \'reseller_type_id\', SUM( IF( `aggregation_date` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 4 WEEK) AND CURRENT_DATE(),`amount`/4,0)) AS \'average_trend\', SUM( IF ( `aggregation_date` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 1 WEEK) AND CURRENT_DATE(),`amount`,0) ) AS \"last_week\", MAX(`aggregation_date`) AS \"max_created\" FROM receiver_wise_credit_transfer_summary GROUP BY reseller_id) AS t2 JOIN receiver_wise_credit_transfer_summary t1 ON t2.reseller_id = t1.reseller_id AND t2.max_created = t1.aggregation_date JOIN bi.reseller_current_status rcs ON t2.reseller_id = rcs.reseller_id WHERE ( \'ALL\' IN (:reseller_type) OR t1.reseller_type_id IN (:reseller_type)) AND ROUND(t2.last_week * 100.0 / t2.average_trend, 1) > :deviationThreshold AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN rcs.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE rcs.reseller_id IN (:reseller_Id) END GROUP BY reseller_id ORDER BY `Change%`', 'mysql', null, null),
    (531, 'sales_trend_report By Hierarchy', 'sales', 'SELECT IFNULL(t1.resellerId,\'N/A\') AS \'reseller_id\', IFNULL(t1.resellerName,\'N/A\') AS \'reseller_name\', IFNULL(t1.resellerMSISDN,\'N/A\') AS \'reseller_msisdn\', IFNULL(t1.resellerTypeId,\'N/A\') AS \'reseller_type_id\', IFNULL(t2.average_trend,0) AS \'average trend\', IFNULL(t2.last_week,0) AS \'last_week\', IFNULL(ROUND(t2.last_week * 100.0 / t2.average_trend, 1),\'0\') AS \'Change%\', IFNULL(t1.region, \'N/A\') AS \'region\' FROM( SELECT resellerId AS \"reseller_id\", resellerMSISDN AS \"Reseller MSISDN\" , resellerTypeId AS \"reseller_type_id\", FORMAT(SUM( IF( `aggregationDate` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 4 WEEK) AND CURRENT_DATE(),`transactionAmount`/4,0)),2) AS \"average_trend\", FORMAT(SUM( IF ( `aggregationDate` BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 1 WEEK) AND CURRENT_DATE(),`transactionAmount`,0) ),2) AS \"last_week\", MAX(`aggregationDate`) AS \"max_created\" FROM std_sales_trend_aggregation GROUP BY resellerId) AS t2 JOIN std_sales_trend_aggregation t1 ON t2.reseller_id = t1.resellerId AND t2.max_created = t1.aggregationDate WHERE ( \'ALL\' IN (:reseller_type ) OR t1.resellerTypeId IN (:reseller_type)) AND ROUND(t2.last_week * 100.0 / t2.average_trend, 1) > :deviationThreshold AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN t1.reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE t1.resellerId IN (:reseller_Id) END GROUP BY resellerid ORDER BY `Change%` ', 'mysql', null, null),
    (516, 'all_orders_parent_child', 'sales', 'SELECT IFNULL(rcs.reseller_id, \"N/A\") as \"Reseller Id\",  IFNULL(rcs.reseller_name, \"N/A\") as \"Reseller Name\", IFNULL(rcs.region, \"N/A\") as \"Region\", IFNULL(ai.sales_area, \"N/A\") as \"Sales Area\", IFNULL(ols.order_status, \"N/A\") as \"Status\",  Count(*) as \"Total Orders\", Sum(CASE WHEN ol.total_unit_price is not null THEN ol.total_unit_price ELSE 0 END) AS \"Total Amount\", IFNULL( DATE_FORMAT(ols.transaction_date, \"%d-%m-%Y\"), \"N/A\") as \"TransDate\" FROM bi.all_orders_status_aggregator ols,bi.all_orders_aggregator ol, bi.reseller_current_status_additional_info ai, bi.reseller_current_status rcs WHERE ols.order_id = ol.order_id AND rcs.reseller_id = ols.reseller_id AND ( ai.reseller_current_id = ols.reseller_id ) AND (\"ALL\" IN (:reseller_Id) or ols.reseller_id IN (:reseller_Id))  AND (\"ALL\" IN (:status) or ols.order_status in (:status)) AND (\"ALL\" IN (:salesArea) or ai.sales_area IN (:salesArea) ) AND ( \"ALL\" IN (:region) or rcs.region IN (:region) )  AND (rcs.reseller_type_id IN (\"SubDistributor\" ,\"FranchiseShop\")) AND (ols.transaction_date BETWEEN :fromDate AND :toDate) GROUP BY rcs.reseller_id,  ols.order_status, rcs.region,ai.sales_area,DATE_FORMAT(ols.transaction_date, \"%d-%m-%Y\")\n', 'mysql', NULL, NULL),
    (533, 'stock_dashboard_region_based', 'Dashboard', 'StockDashboardRegionBased', 'groovy', NULL, NULL),
    (534, 'stock_dashboard_parent_child', 'Dashboard', 'StockDashboardParentChild', 'groovy', NULL, NULL),
   	(535, 'audit_log_report', 'Audit', '{\"elasticIndex\":{\"indexName\":\"audit_\",\"isDataWeeklyIndexed\":true},\"elasticQuery\":{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"wildcard\":{\"transactionNumber\":{\"value\":\"<:transactionNumber:>\"}}},{\"terms\":{\"transactionNumber.keyword\":\"<-:transactionNumber:->\",\"boost\":1}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"eventName\":{\"value\":\"<:eventName:>\"}}},{\"terms\":{\"eventName.keyword\":\"<-:eventName:->\",\"boost\":1}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"user.userId\":{\"value\":\"*\"}}}]}},{\"bool\":{\"should\":[{\"wildcard\":{\"user.resellerMSISDN\":{\"value\":\"*\"}}}]}},{\"range\":{\"timestamp\":{\"from\":\"<:fromDate:>\",\"to\":\"<:toDate:>\"}}}]}},\"script_fields\":{\"timestamp\":{\"script\":\"doc[\'timestamp\'].value.toString(\'yyyy-MM-dd HH:mm:ss\')\"}},\"_source\":{\"includes\":[\"transactionNumber\",\"timestamp\",\"eventName\",\"user.userId\",\"user.resellerMSISDN\",\"channel\",\"resultCode\",\"resultMessage\",\"transactionProfile\"],\"excludes\":[]}}}', 'elastic', NULL, NULL),
   	(537, 'order_dashboard_parent_child', 'Dashboard', 'OrderDashboard', 'groovy', NULL, NULL),
   	(538, 'order_dashboard_region_based', 'Dashboard', 'OrderDashboard', 'groovy', NULL, NULL),
   	(547, 'top_reseller By Hierarchy', 'support', 'SELECT IFNULL(resellerTypeId,\'N/A\') AS \'reseller_level\', IFNULL(account_type,\'N/A\') AS \'account_type\', IFNULL(resellerName,\'N/A\') AS \'reseller_name\', IFNULL(resellerId,\'N/A\') AS \'reseller_id\', IFNULL(resellerMSISDN,\'N/A\') AS \'reseller_MSISDN\', CAST(IFNULL(SUM(transactionAmount),0) AS DECIMAL) AS amount, IFNULL(SUM(COUNT),0) AS \'quantity\', IFNULL(currency,\'BDT\') AS \'currency\', IFNULL(region, \'N/A\') AS \'region\' FROM std_sales_trend_aggregation WHERE aggregationDate BETWEEN :fromDate AND :toDate AND( \'ALL\' IN (:reseller_type) OR resellerTypeId IN (:reseller_type)) AND ( \'ALL\' IN (:accountType) OR account_type IN (:accountType)) AND CASE WHEN \'ALL\' IN (:reseller_Id) THEN reseller_path LIKE CONCAT(\'%\', :resellerPath,\'%\') ELSE resellerId IN (:reseller_Id) END GROUP BY resellerId ORDER BY :sortBy DESC', 'mysql', null, null),
    (549, 'sim_distribution_report', 'sales', 'WITH stock_received AS ( SELECT rsis.reseller_id, rsis.productSKU, SUM(rsis.stock_count) AS stock_qty_received FROM reseller_stock_inout_stats rsis WHERE rsis.status = \'Available\' AND rsis.date BETWEEN :fromDate AND :toDate GROUP BY rsis.reseller_id, rsis.productSKU ), stock_distributed AS ( SELECT rcs.reseller_id, rsis.productSKU, rsis.status, (CASE WHEN SUM(rsis.stock_count)>0 then SUM(rsis.stock_count) else 0 END) AS stock_count FROM reseller_current_status rcs INNER JOIN reseller_current_status rcs2 ON rcs2.reseller_path LIKE CONCAT(rcs.reseller_path, \"%\") LEFT JOIN reseller_stock_inout_stats rsis ON ( rsis.reseller_id = rcs2.reseller_id ) WHERE rsis.status IN (\'Ping\', \'Dma\', \'Active\') AND rsis.date BETWEEN :fromDate AND :toDate AND ( rsis.last_date IS NULL or rsis.last_date BETWEEN :fromDate AND :toDate ) GROUP BY rcs.reseller_id, rsis.productSKU, rsis.status ), products AS ( SELECT DISTINCT productSKU FROM reseller_stock_inout_stats ) SELECT rcs.reseller_type_id AS reseller_type, rcs.reseller_id AS reseller_id,  pr.productSKU, sr.stock_qty_received, SUM(sd.stock_count) post_sales, SUM( CASE WHEN sd.status = \'Dma\' THEN sd.stock_count ELSE 0 END ) AS dma, SUM( CASE WHEN sd.status = \'Ping\' THEN sd.stock_count ELSE 0 END ) AS ping, SUM( CASE WHEN sd.status = \'Active\' THEN sd.stock_count ELSE 0 END ) AS active, rcs.region, rcs.city, rcs.suburb FROM reseller_current_status rcs JOIN products pr LEFT JOIN stock_received sr ON ( rcs.reseller_id = sr.reseller_id and pr.productSKU = sr.productSKU ) LEFT JOIN stock_distributed sd ON ( sd.reseller_id = rcs.reseller_id and pr.productSKU = sd.productSKU ) WHERE (\'ALL\' IN (:productSKU) OR pr.productSKU IN(:productSKU)) AND (\'ALL\' IN (:reseller_type) OR rcs.reseller_type_id IN(:reseller_type)) AND (\'ALL\' IN (:reseller_Id) OR rcs.reseller_id IN(:reseller_Id)) AND (rcs.reseller_path LIKE CONCAT(\"%\", :loggedInResellerId, \"/%\") OR rcs.reseller_id = :loggedInResellerId OR rcs.reseller_path = :loggedInResellerId) GROUP BY rcs.reseller_id, pr.productSKU', 'mysql', NULL, NULL),
	(550, 'orders_live_map_report', 'sales', 'SELECT order_id AS \"orderId\", Date_format(transaction_date, \"%d-%m-%y %H:%i\") AS \"dateAndTime\", sender_id AS \"mentor\", receiver_id AS \"agent\", latitude, longitude, SUM(quantity) AS \"quantity\", GROUP_CONCAT(IF(batch_id = \'\',null,batch_id)  SEPARATOR \',\') AS \'boxId\', GROUP_CONCAT(product_sku SEPARATOR \",\") AS \"productSku\" FROM all_orders_live_map_aggregator WHERE (\"ALL\" IN (:reseller_Id) OR reseller_id IN (:reseller_Id)) AND (\"ALL\" IN (:reseller_type) OR reseller_type IN ( :reseller_type )) AND (transaction_date BETWEEN :fromDate AND :toDate) AND order_status = \"TRANSFERRED\" GROUP BY order_id', 'mysql', NULL, NULL);

INSERT INTO `report_metadata` (`id`, `report_id`, `name`, `type`, `default_value`, `values`, `reg_ex`, `extra_field_1`, `extra_field_2`, `is_editable`, `is_mandatory`)
VALUES
    (125, '43', 'ErsReference', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (126, '43', 'SenderResellerId', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (127, '43', 'ReceiverResellerId', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (128, '43', 'SenderMSISDN', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (129, '43', 'ReceiverMSISDN', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (130, '43', 'TransactionProfile', 'text', 'ALL', 'ALL,BROADBAND,CREDIT_TRANSFER,DATA_BUNDLE,GAMINGBOX,ILAQAYI_OFFER,POSTPAID_TOPUP,PULLBACK,REVERSE_CREDIT_TRANSFER,REVERSE_TOPUP,SPL_BUNDLE,SPL_BUNDLE,SUPPORT_TRANSFER,Topup,VOICE_OFFER', NULL, NULL, NULL, 1, 0),
    (131, '43', 'ResultStatus', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (132, '43', 'ProductSKU', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (133, '43', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
    (134, '43', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
    (135, '43', 'sort', 'select', 'desc', 'asc,desc', NULL, NULL, NULL, 1, 0),
    (136, '43', 'size', 'select', '50', '5,10,50,100,500,1000,5000', NULL, NULL, NULL, 1, 0),
    (137, '43', 'fromAmount', 'text', '0', NULL, NULL, NULL, NULL, 1, 0),
    (138, '43', 'toAmount', 'text', '100000', NULL, NULL, NULL, NULL, 1, 0),
    (200, '43', 'SenderResellerType', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (201, '43', 'ReceiverResellerType', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (202, '43', 'batchId', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (203, '46', 'sort', 'select', 'desc', 'asc,desc', null, null, null, true, false),
    (204, '46', 'size', 'select', '50', '5,10,50,100,500,1000,5000', null, null, null, true, false),
    (25, '107', 'fromDate', 'date', null, null, null, null, null, true, false),
    (26, '107', 'toDate', 'date', null, null, null, null, null, true, false),
    (27, '107', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (28, '107', 'reseller_level', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (53, '116', 'fromDate', 'date', null, null, null, null, null, true, false),
    (54, '116', 'toDate', 'date', null, null, null, null, null, true, false),
    (62, '119', 'fromDate', 'date', '', null, null, null, null, true, false),
    (65, '119', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (332, '119', 'reseller_Id', 'text', 'ALL', null, null, null, null, true, false),
    (233, '139', 'fromDate', 'date', null, null, null, null, null, true, false),
    (234, '139', 'toDate', 'date', null, null, null, null, null, true, false),
    (235, '139', 'transactionType', 'select', 'ALL', 'ALL,CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,TOPUP,REVERSE_TOPUP', null, null, null, true, false),
    (236, '139', 'reseller_Id', 'text', 'ALL', null, null, null, null, true, false),
    (244, '510', 'fromDate', 'date', null, null, null, null, null, true, false),
    (245, '510', 'toDate', 'date', null, null, null, null, null, true, false),
    (246, '510', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (247, '510', 'channel', 'select', 'ALL', 'ALL,USSD,WEBSERVICE,WEB,MOBILE', null, null, null, true, false),
    (333, '510', 'transactionType', 'select', 'ALL', 'ALL,CREDIT_TRANSFER,TOPUP', null, null, null, true, false),
    (248, '513', 'fromDate', 'date', null, null, null, null, null, true, false),
    (249, '513', 'toDate', 'date', null, null, null, null, null, true, false),
    (250, '513', 'channel', 'select', 'ALL', 'ALL,USSD,WEBSERVICE,WEB,MOBILE', null, null, null, true, false),
    (281, '512', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (282, '512', 'deviationThreshold', 'text', '0', null, null, null, null, true, false),
    (283, '511', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (284, '511', 'deviationThreshold', 'text', '0', null, null, null, null, true, false),
    (288, '152', 'fromDate', 'date', null, null, null, null, null, true, false),
    (289, '152', 'toDate', 'date', null, null, null, null, null, true, false),
    (290, '152', 'transactionType', 'select', 'ALL', 'ALL,TOPUP,SUPPORT_TRANSFER,CREDIT_TRANSFER', null, null, null, true, false),
    (291, '152', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (292, '152', 'sender', 'text', 'ALL', null, null, null, null, true, false),
    (372, '152', 'reseller_Id', 'text', 'ALL', 'ALL', null, null, null, true, false),
    (318, '112', 'fromDate', 'date', null, null, null, null, null, true, false),
    (319, '112', 'toDate', 'date', null, null, null, null, null, true, false),
    (320, '112', 'channel', 'select', 'ALL', 'ALL,USSD,WEBSERVICE,WEB,MOBILE', null, null, null, true, false),
    (321, '105', 'fromDate', 'date', null, null, null, null, null, true, false),
    (322, '105', 'toDate', 'date', null, null, null, null, null, true, false),
    (323, '105', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (324, '105', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (325, '105', 'sortBy', 'select', 'amount', 'amount,quantity', null, 'sort', null, true, false),
    (326, '121', 'reseller_Id', 'text', 'ALL', null, null, null, null, true, false),
    (327, '121', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (329, '121', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (306,'509', 'reseller_msisdn', 'text', null, null, null, null, null, 1, 0),
    (307,'509', 'fromDate', 'date', null, null, null, null, null, 1, 0),
    (308,'509', 'toDate', 'date', null, null, null, null, null, 1, 0),
    (309,'508', 'reseller_msisdn', 'text', null, null, null, null, null, 1, 0),
    (310,'507', 'receiver_MSISDN', 'text', null, null, null, null, null, 1, 0),
    (311,'507', 'reseller_msisdn', 'text', null, null, null, null, null, 1, 0),
    (312,'506', 'reseller_msisdn', 'text', null, null, null, null, null, 1, 0),
    (313,'506', 'fromDate', 'date', null, null, null, null, null, 1, 0),
    (314,'506', 'toDate', 'date', null, null, null, null, null, 1, 0),
    (315,'505', 'reseller_msisdn', 'text', null, null, null, null, null, 1, 0),
    (316,'502', 'ersReference', 'text', null, null, null, null, null, 1, 0),
    (317,'502', 'resellerMsisdn', 'text', null, null, null, null, null, 1, 0),
    (381, '524', 'fromDate', 'date', null, null, null, null, null, true, false),
    (382, '524', 'toDate', 'date', null, null, null, null, null, true, false),
    (383, '524', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, '{\"values_from_api\":\"true\"} ', null, false, false),
    (385, '524', 'transaction_profile', 'select', 'ALL', 'ALL,CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,REVERSE_TOPUP,TOPUP', null, null, null, true, false),
    (387, '524', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (388, '524', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (389, '525', 'transaction_profile', 'select', 'ALL', 'ALL,CREDIT_TRANSFER,REVERSE_CREDIT_TRANSFER,REVERSE_TOPUP,TOPUP', null, null, null, true, false),
    (390, '525', 'fromDate', 'date', null, null, null, null, null, true, false),
    (391, '525', 'toDate', 'date', null, null, null, null, null, true, false),
    (392, '525', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, false),
    (396, '525', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (397, '526', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
    (398, '526', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, '{\"values_from_api\":\"true\", \"dependent_label\":\"reseller_Id\"}', null, true, false),
    (400, '526', 'account_type', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (402, '526', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (409, '527', 'fromDate', 'date', null, null, null, null, null, true, false),
    (410, '527', 'toDate', 'date', null, null, null, null, null, true, false),
    (411, '527', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (412, '527', 'reseller_level', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (413, '527', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (414, '527', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
    (435, '529', 'fromDate', 'date', '', null, null, null, null, true, false),
    (436, '529', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (439, '529', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (440, '529', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
    (425, '530', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (426, '530', 'deviationThreshold', 'text', '0', null, null, null, null, true, false),
    (427, '530', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (428, '530', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
    (431, '531', 'reseller_type', 'text', 'ALL', null, null, null, null, true, false),
    (432, '531', 'deviationThreshold', 'text', '0', null, null, null, null, true, false),
    (433, '531', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (434, '531', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
	(31, '516', 'fromDate', 'date', '', NULL, NULL, NULL, NULL, 1, 0),
	(365, '516', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
	(366, '516', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', NULL, NULL, NULL, '{\"resellerTypeId\": \"SubDistributor,FranchiseShop\"}', 1, 0),
	(441, '516', 'region', 'multiselect', NULL, '[{key:\"Goteborg\",val:\"Goteborg\"},{key:\"Malmo\",val:\"Malmo\"},{key:\"Nacka\",val:\"Nacka\"},{key:\"Stockholm East\",val:\"Stockholm East\"}]', NULL, NULL, NULL, 1, 0),
	(442, '516', 'salesArea', 'multiselect', NULL, '[{key:\"Stockholm South\",val:\"Stockholm South\"},{key:\"Central Malmo\",val:\"Central Malmo\"},{key:\"Lund\",val:\"Lund\"},{key:\"Huddinge\",val:\"Huddinge\"},{key:\"Helsingborg\",val:\"Helsingborg\"}]', NULL, NULL, '{\"depends_on\":53}', 1, 0),
    (3430, '516', 'status', 'treeview', NULL, '[{\"label\":\"Open Status\",\"value\":1,\"parentId\":null,\"children\":[{\"label\":\"TRANSFER_WAIT_CONFIRM\",\"value\":\"TRANSFER_WAIT_CONFIRM\",\"parentId\":1,\"children\":[]},{\"label\":\"SUBMITTED\",\"value\":\"SUBMITTED\",\"parentId\":1,\"children\":[]},{\"label\":\"TRANSFER_REJECTED\",\"value\":\"TRANSFER_REJECTED\",\"parentId\":1,\"children\":[]},{\"label\":\"RETURN_TRANSFER_WAIT_CONFIRM\",\"value\":\"RETURN_TRANSFER_WAIT_CONFIRM\",\"parentId\":1,\"children\":[]},{\"label\":\"RETURN_TRANSFER_REJECTED\",\"value\":\"RETURN_TRANSFER_REJECTED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_CREATED\",\"value\":\"EXTERNAL_CREATED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM\",\"value\":\"EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_RETURN_TRANSFERRED\",\"value\":\"EXTERNAL_RETURN_TRANSFERRED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_RETURN_TRANSFER_REJECTED\",\"value\":\"EXTERNAL_RETURN_TRANSFER_REJECTED\",\"parentId\":1,\"children\":[]},{\"label\":\"PARTIALLY_TRANSFERRED\",\"value\":\"PARTIALLY_TRANSFERRED\",\"parentId\":1,\"children\":[]},{\"label\":\"PAYMENT_WAIT_CONFIRM\",\"value\":\"PAYMENT_WAIT_CONFIRM\",\"parentId\":1,\"children\":[]},{\"label\":\"PAYMENT_FAILED\",\"value\":\"PAYMENT_FAILED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_WAIT_CREATED\",\"value\":\"EXTERNAL_WAIT_CREATED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_CREATED_WITH_ERROR\",\"value\":\"EXTERNAL_CREATED_WITH_ERROR\",\"parentId\":1,\"children\":[]},{\"label\":\"PENDING_APPROVAL\",\"value\":\"PENDING_APPROVAL\",\"parentId\":1,\"children\":[]},{\"label\":\"RESERVED_WAIT_CONFIRM\",\"value\":\"RESERVED_WAIT_CONFIRM\",\"parentId\":1,\"children\":[]},{\"label\":\"RESERVED\",\"value\":\"RESERVED\",\"parentId\":1,\"children\":[]},{\"label\":\"RESERVE_REJECTED\",\"value\":\"RESERVE_REJECTED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_SCHEDULED\",\"value\":\"EXTERNAL_SCHEDULED\",\"parentId\":1,\"children\":[]},{\"label\":\"EXTERNAL_PROCESS_FAILED\",\"value\":\"EXTERNAL_PROCESS_FAILED\",\"parentId\":1,\"children\":[]},{\"label\":\"RETURN_SUBMITTED\",\"value\":\"RETURN_SUBMITTED\",\"parentId\":1,\"children\":[]}]},{\"label\":\"Close Status\",\"value\":1,\"parentId\":null,\"children\":[{\"label\":\"TRANSFERRED\",\"value\":\"TRANSFERRED\",\"parentId\":1,\"children\":[]},{\"label\":\"RETURN_TRANSFERRED\",\"value\":\"RETURN_TRANSFERRED\",\"parentId\":1,\"children\":[]},{\"label\":\"REJECTED\",\"value\":\"REJECTED\",\"parentId\":1,\"children\":[]}]}]', NULL, NULL, NULL, 1, 0),
	(443, '533', 'region', 'select', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(445, '534', 'productSKU', 'productSKUList', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(446, '534', 'resellerId', 'parentChildBasedResellerList', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(447, '534', 'resellerTypeId', 'select', 'ALL', 'branch,pos', NULL, NULL, NULL, 1, 0),
   	(448, '535', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(449, '535', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(450, '535', 'transactionNumber', 'text', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(451, '535', 'eventName', 'select', 'ALL', 'ALL,Login,logout,changeResellerType,resellerChangeState,addReseller,ADD_CATEGORY,DELETE_CATEGORY,UPDATE_CATEGORY,ADD_PRODUCT,DELETE_PRODUCT,UPDATE_PRODUCT,ADD_PRODUCT_VARIANT,DELETE_PRODUCT_VARIANT,UPDATE_PRODUCT_VARIANT,updateUser,addResellerUsers,updateReseller,addResellerRole,updateResellerRole,deleteResellerRole,createContract,updateContract,createPasswordPolicy,updatePasswordPolicy,deletePasswordPolicy,changeParent,TRANSFER,TOPUP,IMPORT_INVENTORY,ADD_TAX,UPDATE_TAX,biFetchTransaction,createContractPriceEntries,updateContractPriceEntries,deleteContractPriceEntries,updateResellerType,createResellerType,deleteUsers', NULL, NULL, NULL, 1, 0),
   	(458, '537', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(459, '537', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(460, '537', 'resellerId', 'parentChildBasedResellerList', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(461, '537', 'status', 'select', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(462, '537', 'region', 'select', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(463, '537', 'dealerType', 'select', 'ALL', 'ALL,Seller,Buyer', NULL, NULL, NULL, 1, 0),
   	(464, '538', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(465, '538', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
   	(466, '538', 'resellerId', 'locationBasedResellerList', 'ALL', 'ALL', NULL, 'region,salesArea,cluster,route', '{\"resellerTypeId\":\"SubDistributor,FranchiseShop\"}', 1, 0),
   	(467, '538', 'status', 'select', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(468, '538', 'region', 'select', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
   	(469, '538', 'dealerType', 'select', 'ALL', 'ALL,Seller,Buyer', NULL, NULL, NULL, 1, 0),
   	(470, '533', 'productSKU', 'productSKUList', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
    (342, '518', 'reseller_type', 'dependentResellerList', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS', NULL, '{\"values_from_api\":\"true\", \"dependent_label\":\"reseller_Id\"}', NULL, 1, 0),
    (343,'518', 'reseller_Id', 'dependentResellerList', 'ALL', NULL, NULL, '{\"visibility\":\"False\"}', NULL, 1, 0),
	(344, '518', 'productSKU', 'productSKUList', 'ALL', 'ALL', NULL, NULL, NULL, 1, 0),
	(345, '518', 'loggedInResellerId', 'LoggedInUser', NULL, NULL, NULL, '{\"visibility\":\"false\"}', NULL, 1, 0),
    (351, '520', 'fromYear', 'select', '2021', '2022,2023', NULL, NULL, NULL, 1, 0),
	(352, '520', 'toYear', 'select', '2022', '2022,2023', NULL, NULL, NULL, 1, 0),
	(353, '520', 'fromWeek', 'select', '1', '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52', NULL, NULL, NULL, 1, 0),
	(354, '520', 'toWeek', 'select', '2', '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52', NULL, NULL, NULL, 1, 0),
	(357, '520', 'productSku', 'productSKUList', 'ALL', NULL, NULL, NULL, NULL, 1, 0),
	(3419, '520', 'reseller_type', 'dependentResellerList', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS', NULL, '{\"values_from_api\":\"true\", \"dependent_label\":\"reseller_Id\"}', NULL, 1, 0),
    (3420, '520', 'reseller_Id', 'dependentResellerList', 'ALL', NULL, NULL, '{\"visibility\":\"False\"}', NULL, 1, 0),
	(3432, '520', 'loggedInResellerId', 'text', NULL, NULL, NULL, '{\"visibility\":\"false\"}', NULL, 1, 0),
    (3423, '547', 'fromDate', 'date', null, null, null, null, null, true, false),
    (3424, '547', 'toDate', 'date', null, null, null, null, null, true, false),
    (3425, '547', 'reseller_type', 'select', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS,Bank,Warehouse,ASM,OperatorAgent', null, null, null, true, false),
    (3426, '547', 'accountType', 'select', 'ALL', 'ALL,RESELLER', null, null, null, true, false),
    (3427, '547', 'sortBy', 'select', 'amount', 'amount,quantity', null, 'sort', null, true, false),
    (3428, '547', 'resellerPath', 'text', null, null, null, '{\"visibility\":\"False\"}', null, true, false),
    (3429, '547', 'reseller_Id', 'parentChildBasedResellerList', 'ALL', null, null, null, null, true, true),
	(3433, '549', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
	(3434, '549', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
	(3435, '549', 'reseller_type', 'dependentResellerList', 'ALL', 'ALL,Operator,Distributor,SubDistributor,Agent,FranchiseShop,POS', NULL, '{\"values_from_api\":\"true\", \"dependent_label\":\"reseller_Id\"}', NULL, 1, 0),
	(3436, '549', 'reseller_Id', 'dependentResellerList', 'ALL', NULL, NULL, '{\"visibility\":\"False\"}', NULL, 1, 0),
	(3437, '549', 'productSKU', 'productSKUList', 'ALL', 'ALL', NULL, NULL, NULL, 1, 0),
	(3438, '549', 'loggedInResellerId', 'LoggedInUser', NULL, NULL, NULL, '{\"visibility\":\"false\"}', NULL, 1, 0),
	(3439, '550', 'fromDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
	(3440, '550', 'toDate', 'date', NULL, NULL, NULL, NULL, NULL, 1, 0),
	(3441, '550', 'reseller_type', 'dependentResellerList', 'ALL', NULL, NULL, '{\"values_from_api\":\"true\", \"dependent_label\":\"reseller_Id\"}', NULL, 1, 0),
	(3442, '550', 'reseller_Id', 'dependentResellerList', 'ALL', NULL, NULL, '{\"visibility\":\"False\"}', NULL, 1, 0),
	(3443, '550', 'loggedInResellerId', 'text', NULL, NULL, NULL, '{\"visibility\":\"false\"}', NULL, 1, 0);

INSERT INTO `dashboard_url` (`id`, `dashboard_url`, `status`, `tab_name`)
VALUES
	(1, '/views/POC-StockHoldingReport/RWM-Dashboard', 'active', 'tableau');



INSERT INTO report_channel_access_control (id, channel, report_list_ids, status)
VALUES
    (1, 'web', '516,517,518,519,520,119,116,107,121,105,112,152,510,511,512,513,139,524,525,526,527,530,531,529,533,534,535,538,547,549,550', 'active'),
    (2, 'ussd', '516,517,518,519,520,522', 'active'),
    (3, 'app', '516,518,520', 'active');


INSERT INTO report_access_control (id, type_role, name, report_list_ids, dashboard_url_ids, status, settings)
VALUES
    (1, 'Operator', null, '516,549,533,535', '1', 'active', true),
    (2, 'RWM', null, '516,518,520,522,524,525,510,112,513,531,530,533,534,549', 1, 'active', true),
    (6, 'Distributor', null, '516,549,533', '1', 'active', true),
    (8, 'Agent', null, '533', '1', 'active', true),
    (10, 'Mentor', null, '516,549,533', '1', 'active', true);

INSERT INTO `report_category_mapping` (`id`, `category_name`, `report_list_ids`)
VALUES
	(1, 'Report', '516,517,518,519,520,119,116,107,121,105,112,152,510,511,512,513,139,524,525,526,527,530,531,529,547,549'),
	(2, 'Audit', '535');


CREATE OR REPLACE VIEW `bi`.`reseller_hierarchy_view` AS
 SELECT
        `ext_dev`.`address` AS `id`,
        `com_rec`.`tag` AS `reseller_id`,
        `com_rcv`.`tag` AS `reseller_parent`,
        `com_rec`.`name` AS `reseller_name`,
        `ext_dev`.`address` AS `msisdn`,
        (case `com_rec`.`status` when '0' then 'Active' when '1' then 'Blocked' when '2' then 'Blocked' when '3' then 'Frozen' else 'Disabled' end) AS `status`,
        `res_type`.`name` AS `reseller_type`,
        `acc`.`balance`AS `balance`,
        'BDT' AS `currency`,
        (case when `acc`.`accountId` IS NULL then 'N/A' else `acc`.`accountId` end ) AS `account_Id`,
		(case when `acc`.`accountTypeId` IS NULL then 'N/A' else `acc`.`accountTypeId` end )AS `account_type_id`

    FROM
        `Refill`.`commission_receivers` AS `com_rec`
	INNER JOIN
        `Refill`.`extdev_devices` AS `ext_dev` ON (`ext_dev`.`owner_key` = `com_rec`.`receiver_key`)
	INNER JOIN
        `Refill`.`reseller_types` AS `res_type` ON (`res_type`.`type_key`=`com_rec`.`type_key`)
	LEFT JOIN
        `Refill`.`reseller_hierarchy` AS `res_hier` ON (`com_rec`.`receiver_key`=`res_hier`.`child_key`)
	LEFT JOIN
        `Refill`.`commission_receivers` AS `com_rcv` ON (`com_rcv`.`receiver_key`=`res_hier`.`parent_key`)
	LEFT JOIN
		`Refill`.`pay_prereg_accounts` AS `pre_acc` ON `pre_acc`.`owner_key` = `com_rec`.`receiver_key`
	LEFT JOIN
		`accountmanagement`.`accounts` AS `acc` ON `acc`.`accountId` = `pre_acc`.`account_nr`;


 INSERT IGNORE INTO `bi`.`reseller_current_status`(`id`,
  `reseller_id`, `reseller_name`, `reseller_path`,
  `reseller_status`, `reseller_parent`,
  `reseller_type_id`, `region`, `street`,
  `zip`, `city`, `country`, `email`,
  `MSISDN`, `email_responsible`, `created_on`,
  `created_by`, `status_changed_on`,
  `status_changed_by`
   )
  SELECT md5(`cr`.`tag`),
  `cr`.`tag`, `cr`.`name`, `cr`.`reseller_path`,
   (case `cr`.`status` when '0' then 'Active' when '1' then 'Blocked' when '2' then 'Blocked' when '3' then 'Frozen' else 'Disabled' end),
   (case `cr`.`reseller_path` when `cr`.`reseller_path` LIKE '_/%' then LEFT(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2), LENGTH(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2)) - LENGTH(SUBSTRING_INDEX(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2),'/',-1))-1) else `cr`.`reseller_path` end),
   `res_type`.`id`,
   (case `ext`.`parameter_key` when 'region' then `ext`.`parameter_value` else '' end),
   '','','','','',`ext_dev`.`address`,'',`cr`.`time_created`,'', '', ''
  FROM
  `Refill`.`commission_receivers` AS `cr`
   LEFT JOIN `Refill`.`reseller_extra_params` AS `ext` ON (`cr`.`receiver_key` = `ext`.`receiver_key`)
   LEFT JOIN `Refill`.`extdev_devices` AS `ext_dev` ON (`ext_dev`.`owner_key` = `cr`.`receiver_key`)
   LEFT JOIN `Refill`.`reseller_types` AS `res_type` ON (`res_type`.`type_key` = `cr`.`type_key`);


 INSERT IGNORE INTO `bi`.`reseller_current_status`(`id`,
   `reseller_id`, `reseller_name`, `reseller_path`,
   `reseller_status`, `reseller_parent`,
   `reseller_type_id`, `region`, `street`,
   `zip`, `city`, `country`, `email`,
   `MSISDN`, `email_responsible`, `created_on`,
   `created_by`, `status_changed_on`,
   `status_changed_by`
    )
   SELECT md5(`cr`.`tag`),
   `cr`.`tag`, `cr`.`name`, `cr`.`reseller_path`,
    (case `cr`.`status` when '0' then 'Active' when '1' then 'Blocked' when '2' then 'Blocked' when '3' then 'Frozen' else 'Disabled' end),
    (case `cr`.`reseller_path` when `cr`.`reseller_path` LIKE '_/%' then LEFT(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2), LENGTH(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2)) - LENGTH(SUBSTRING_INDEX(SUBSTRING_INDEX(`cr`.`reseller_path`, '/', -2),'/',-1))-1) else `cr`.`reseller_path` end),
    `res_type`.`id`,
    (case `ext`.`parameter_key` when 'region' then `ext`.`parameter_value` else '' end),
    '','','','','',`ext_dev`.`address`,'',`cr`.`time_created`,'', '', ''
   FROM
   `Refill`.`commission_receivers` AS `cr`
    LEFT JOIN `Refill`.`reseller_extra_params` AS `ext` ON (`cr`.`receiver_key` = `ext`.`receiver_key`)
    LEFT JOIN `Refill`.`extdev_devices` AS `ext_dev` ON (`ext_dev`.`owner_key` = `cr`.`receiver_key`)
    LEFT JOIN `Refill`.`reseller_types` AS `res_type` ON (`res_type`.`type_key` = `cr`.`type_key`)
   WHERE `ext`.`parameter_key` = "region"
 ON DUPLICATE KEY UPDATE `region`=VALUES(`region`);


 INSERT IGNORE INTO `bi`.`reseller_current_status_additional_info`
  (`reseller_current_id`,`cnic`, `msr_id`, `msr_name`,`msr_msisdn`,`sfr_id`,`sfr_name`, `sfr_msisdn`,
 `rso_id`, `rso_name`,`rso_msisdn`,`birthday`, `postal_code`, `contact_no`, `batch_id`,`circle`, `max_daily_recharge_limit`,
 `balance_threshold`,`district`, `sales_area`, `latitude`,`longitude`
  )
 SELECT `cr`.`tag`,'',`cr`.`tag`,`cr`.`name`, `ext_dev`.`address`,'','','','','','','','',
       `ext_dev`.`address`,'','','','','',
   (case `ext`.`parameter_key` when 'salesArea' then `ext`.`parameter_value` else '' end) AS `saleArea`,'',''
 FROM
   `Refill`.`commission_receivers` AS `cr`
   LEFT JOIN `Refill`.`reseller_extra_params` AS `ext` ON (`cr`.`receiver_key` = `ext`.`receiver_key`)
   LEFT JOIN `Refill`.`extdev_devices` AS `ext_dev` ON (`ext_dev`.`owner_key` = `cr`.`receiver_key`);


 INSERT IGNORE INTO `bi`.`reseller_current_status_additional_info`
   (`reseller_current_id`,`cnic`, `msr_id`, `msr_name`,`msr_msisdn`,`sfr_id`,`sfr_name`, `sfr_msisdn`,
  `rso_id`, `rso_name`,`rso_msisdn`,`birthday`, `postal_code`, `contact_no`, `batch_id`,`circle`, `max_daily_recharge_limit`,
  `balance_threshold`,`district`, `sales_area`, `latitude`,`longitude`
   )
  SELECT `cr`.`tag`,'',`cr`.`tag`,`cr`.`name`, `ext_dev`.`address`,'','','','','','','','',
        `ext_dev`.`address`,'','','','','',
    (case `ext`.`parameter_key` when 'salesArea' then `ext`.`parameter_value` else '' end) AS `saleArea`,'',''
  FROM
    `Refill`.`commission_receivers` AS `cr`
    LEFT JOIN `Refill`.`reseller_extra_params` AS `ext` ON (`cr`.`receiver_key` = `ext`.`receiver_key`)
    LEFT JOIN `Refill`.`extdev_devices` AS `ext_dev` ON (`ext_dev`.`owner_key` = `cr`.`receiver_key`)
  WHERE `ext`.`parameter_key` = "salesArea"
   ON DUPLICATE KEY UPDATE `sales_area`=VALUES(`sales_area`);


