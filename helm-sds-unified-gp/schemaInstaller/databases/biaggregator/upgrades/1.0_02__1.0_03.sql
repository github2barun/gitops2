DROP TABLE IF EXISTS `denomination_wise_sales_aggregation`;
CREATE TABLE `denomination_wise_sales_aggregation` (
`id` varchar(255) NOT NULL,
`end_time_day` date DEFAULT NULL,
`end_time_hour` time DEFAULT NULL,
`channel` varchar(200) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT 0,
`count` decimal(20,5) DEFAULT 0,
`denomination` decimal(20,5) DEFAULT 0,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `regional_std_sales_trend_aggregation`;
CREATE TABLE `regional_std_sales_trend_aggregation` (
`id` varchar(255) NOT NULL,
`aggregationDate` date DEFAULT NULL,
`account_type` varchar(200) DEFAULT NULL,
`resellerId` varchar(200) DEFAULT NULL,
`resellerMSISDN` varchar(200) DEFAULT NULL,
`resellerName` varchar(200) DEFAULT NULL,
`resellerTypeId` varchar(200) DEFAULT NULL,
`reseller_path` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(20) DEFAULT NULL,
`transactionAmount` decimal(20,5) DEFAULT NULL,
`reseller_commission` decimal(20,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
`zone` varchar(200) DEFAULT NULL,
`area` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `regional_receiver_wise_credit_transfer_summary`;
CREATE TABLE `regional_receiver_wise_credit_transfer_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`reseller_id` varchar(200) DEFAULT NULL,
`reseller_msisdn` varchar(200) DEFAULT NULL,
`reseller_name` varchar(200) DEFAULT NULL,
`reseller_type_id` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(25) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(255) DEFAULT NULL,
`receiver_account_type` varchar(255) DEFAULT NULL,
`zone` varchar(200) DEFAULT NULL,
`area` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;