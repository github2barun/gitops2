ALTER TABLE `bi`.`total_kyc_sales` ADD COLUMN `parentDealerId` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `bi`.`distributor_wise_weekly_sales_summary` ADD COLUMN `reseller_path` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `bi`.`weekly_reseller_summary` ADD COLUMN `reseller_path` VARCHAR(200) DEFAULT NULL;

ALTER TABLE `bi`.`reseller_current_status` ADD COLUMN `MSISDN` varchar(200) DEFAULT NULL;

ALTER TABLE `bi`.`reseller_current_status` ADD COLUMN `email_responsible` varchar(200) DEFAULT NULL;

DROP TABLE IF EXISTS `bi`. `total_kyc_sales_by_location`;
CREATE TABLE `bi`.`total_kyc_sales_by_location` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `reseller_id` varchar(200) DEFAULT NULL,
  `sim_type` varchar(200) DEFAULT NULL,
  `sim_brand` varchar(200) DEFAULT NULL,
  `latitude` varchar(200) DEFAULT NULL,
  `longitude` varchar(200) DEFAULT NULL,
  `reseller_path` varchar(200) DEFAULT NULL,
  `region` varchar(200) DEFAULT NULL,
  `total_sales` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`distributor_wise_daily_sales_summary`;
CREATE TABLE `bi`.`distributor_wise_daily_sales_summary` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `distributorId` varchar(255) DEFAULT NULL,
    `weekNumber` int(4) DEFAULT NULL,
    `year` int(4) DEFAULT NULL,
    `total` bigint(25) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`daily_total_sales_summary`;
CREATE TABLE `bi`.`daily_total_sales_summary` (
   `date` date DEFAULT NULL,
    `total_sales` bigint(25) DEFAULT NULL,
    PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`daily_active_reseller_summary`;
CREATE TABLE  `bi`.`daily_active_reseller_summary` (
   `id` varchar(255) NOT NULL DEFAULT '',
    `reseller_parent` varchar(200) DEFAULT NULL,
    `region` varchar(200) DEFAULT NULL,
    `total_reseller` int(11) DEFAULT NULL,
    `active_reseller` int(11) DEFAULT NULL,
     `email` varchar(100) DEFAULT NULL,
      `date` date DEFAULT NULL,
      `weekNumber` int(4) DEFAULT NULL,
      `year` int(4) DEFAULT NULL,
      `name` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_inventory_stock`;
CREATE TABLE `reseller_inventory_stock` (
  `id` varchar(100) NOT NULL DEFAULT '',
  `product_sku` varchar(100) DEFAULT NULL,
  `owner_id` varchar(100) DEFAULT NULL,
  `status` varchar(100) DEFAULT NULL,
  `stock_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi`.`transaction_statistics_aggregator`;
CREATE TABLE `bi`.`transaction_statistics_aggregator` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `channel` varchar(200) DEFAULT NULL,
     `account_type` varchar(200) DEFAULT NULL,
     `transaction_type` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     `sum` decimal(20,5) DEFAULT NULL,
     `currency` varchar(200) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`transaction_failure_aggregator`;
CREATE TABLE `bi`.`transaction_failure_aggregator` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `channel` varchar(200) DEFAULT NULL,
     `transaction_profile` varchar(200) DEFAULT NULL,
     `failure_cause` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`buyer_wise_purchase_summary`;
CREATE TABLE `bi`.`buyer_wise_purchase_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`dealer_id` varchar(200) DEFAULT NULL,
`dealer_msisdn` varchar(200) DEFAULT NULL,
`dealer_name` varchar(200) DEFAULT NULL,
`seller_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`seller_wise_sales_summary`;
CREATE TABLE `bi`.`seller_wise_sales_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`dealer_id` varchar(200) DEFAULT NULL,
`dealer_msisdn` varchar(200) DEFAULT NULL,
`dealer_name` varchar(200) DEFAULT NULL,
`buyer_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`hourly_usage_statistics`;
CREATE TABLE `bi`.`hourly_usage_statistics` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `hour` time DEFAULT NULL,
    `channel` varchar(255) DEFAULT NULL,
    `ttl_txn_count` bigint(25) DEFAULT 0,
	`successful_txn_count` bigint(25) DEFAULT 0,
    `failed_txn_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;