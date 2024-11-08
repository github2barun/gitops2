DROP TABLE IF EXISTS `bi`.`orders_quantity_aggregator`;
CREATE TABLE `bi`.`orders_quantity_aggregator`
(
    `id`                  varchar(200) NOT NULL,
    `transaction_date`    datetime              DEFAULT NULL,
    `transaction_number`  varchar(200)          DEFAULT NULL,
    `quantity`            varchar(200)          DEFAULT NULL,
    `product_code`        varchar(200)          DEFAULT NULL,
    `product_sku`         varchar(200) NOT NULL DEFAULT '',
    `product_description` varchar(200) NOT NULL DEFAULT '',
    `reseller_id`         varchar(200) NULL DEFAULT NULL,
    `order_type`          varchar(200) NOT NULL DEFAULT '',
    `buyer_id`            varchar(200) NULL DEFAULT NULL,
    `seller_id`           varchar(200) NULL DEFAULT NULL,
    `receiver_id`         varchar(200) NULL DEFAULT NULL,
    `sender_id`           varchar(200) NULL DEFAULT NULL,
    `drop_location_id`    varchar(200) NULL DEFAULT NULL,
    `pickup_location_id`  varchar(200) NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`outlet_performance_report`;
CREATE TABLE `bi`.`outlet_performance_report`(
    `id` varchar(255) NOT NULL DEFAULT '',
    `reseller_id` varchar(100) DEFAULT NULL,
    `month` int(10) DEFAULT NULL,
    `year` int(10) DEFAULT NULL,
    `avg_mpesa_float` decimal(20,2) DEFAULT NULL,
    `avg_transaction_volume` decimal(20,2) DEFAULT NULL,
    `avg_transaction_value` decimal(20,2) DEFAULT NULL,
    `mpesa_float_level` decimal(20,2) DEFAULT NULL,
    `avg_stock_volume` decimal(20,2) DEFAULT NULL,
    `avg_stock_value` decimal(20,2) DEFAULT NULL,
    `devices_sold` bigint(20) DEFAULT NULL,
    `devices_attachment` bigint(20) DEFAULT NULL,
    `lines_ordered` bigint(20) DEFAULT NULL,
    `lines_attached` bigint(20) DEFAULT NULL,
    `open_issues` bigint(20) DEFAULT NULL,
    `closed_issues` bigint(20) DEFAULT NULL,
    `last_visit_comments` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`ims_data_aggregator` ADD COLUMN `trip_id` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`ims_data_aggregator` ADD COLUMN `route_information` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`ims_data_aggregator` ADD COLUMN `operation_type` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`orders_quantity_aggregator` ADD COLUMN `route_id` varchar(200) DEFAULT NULL;
