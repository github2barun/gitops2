DROP TABLE IF EXISTS `bi`.`hourly_cdr_usage_statistics_aggregation`;
CREATE TABLE `bi`.`hourly_cdr_usage_statistics_aggregation` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `hour` time DEFAULT NULL,
    `dealer_id` varchar(255) DEFAULT NULL,
    `dealer_type` varchar(255) DEFAULT NULL,
    `dealer_msisdn` varchar(255) DEFAULT NULL,
    `dealer_city` varchar(255) DEFAULT NULL,
    `dealer_district` varchar(255) DEFAULT NULL,
    `section` varchar(255) DEFAULT NULL,
    `cdr_usage_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`total_transaction_summary`;
CREATE TABLE `bi`.`total_transaction_summary` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `dealer_type` varchar(200) DEFAULT NULL,
     `dealer_id` varchar(200) DEFAULT NULL,
     `dealer_msisdn` varchar(200) DEFAULT NULL,
     `transaction_type` varchar(200) DEFAULT NULL,
     `district` varchar(100) DEFAULT NULL,
     `area` varchar(100) DEFAULT NULL,
     `section` varchar(200) DEFAULT NULL,
     `city` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     `sum` decimal(20,5) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`dealer_stock_movement_aggregation`;
CREATE TABLE `bi`.`dealer_stock_movement_aggregation` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `seller_id` varchar(200) DEFAULT NULL,
     `buyer_id` varchar(200) DEFAULT NULL,
     `category` varchar(200) DEFAULT NULL,
     `sub_category` varchar(100) DEFAULT NULL,
     `product_type` varchar(100) DEFAULT NULL,
     `product_sku` varchar(200) DEFAULT NULL,
     `total_stocks_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`reseller_current_stock` ADD COLUMN `product_cateory` varchar(200) DEFAULT NULL;

ALTER TABLE `bi`.`reseller_current_stock` ADD COLUMN `product_subcateory` varchar(200) DEFAULT NULL;



