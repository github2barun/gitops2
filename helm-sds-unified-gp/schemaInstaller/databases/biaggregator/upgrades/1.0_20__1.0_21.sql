DROP TABLE IF EXISTS `bi`.`ims_data_aggregator`;
CREATE TABLE `bi`.`ims_data_aggregator`
(
    `id` varchar(255) NOT NULL DEFAULT '',
	`transaction_date` datetime DEFAULT NULL,
    `transaction_number` varchar(200) DEFAULT NULL,
    `quantity` varchar(200) DEFAULT NULL,
    `product_code` varchar(200) DEFAULT NULL,
	`product_sku` varchar(200) DEFAULT NULL,
	`seller_id` varchar(200) DEFAULT NULL,
	`buyer_id` varchar(200) DEFAULT NULL,
    `reseller_id` varchar(200) DEFAULT NULL,
    `reseller_type` varchar(200) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`outlet_trip_gms_summary`;
CREATE TABLE `bi`.`outlet_trip_gms_summary`
(
    `id`                  varchar(255) NOT NULL DEFAULT '',
    `summary_date`        date         NOT NULL,
    `reseller_id`         varchar(255) NOT NULL DEFAULT '',
    `reseller_name`       varchar(255) NOT NULL DEFAULT '',
    `total_outlet_visits` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


ALTER TABLE `bi`.`all_trips_detail` ADD COLUMN IF NOT EXISTS `transaction_number` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`all_trips_detail` ADD COLUMN IF NOT EXISTS `pos_status` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`all_trips_detail` DROP COLUMN IF EXISTS `outlet_name`;
ALTER TABLE `bi`.`all_trips_detail` DROP COLUMN IF EXISTS `task_status`;
ALTER TABLE `bi`.`all_trips_detail` DROP COLUMN IF EXISTS `channel`;

ALTER TABLE `bi`.`all_orders_aggregator` ADD COLUMN IF NOT EXISTS `pickup_location_id` varchar(200) NULL DEFAULT NULL;
ALTER TABLE `bi`.`all_orders_aggregator` ADD COLUMN IF NOT EXISTS `sender_id` varchar(200) NULL DEFAULT NULL;

ALTER TABLE `bi`.`trip_outlet_visit` MODIFY `dsa_strike_rate` decimal(20,2);


DROP TABLE IF EXISTS `bi`.`report_channel_access_control`;
CREATE TABLE `bi`.`report_channel_access_control`
(
    `id`              int(11) unsigned NOT NULL AUTO_INCREMENT,
    `channel`         varchar(100) NOT NULL DEFAULT 'ALL',
    `report_list_ids` varchar(500)          DEFAULT NULL,
    `status`          varchar(10)  NOT NULL DEFAULT 'active',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`dealer_performance_external_weekly_sales_summary`;
CREATE TABLE `bi`.`dealer_performance_external_weekly_sales_summary` (
  	`id` varchar(200) NOT NULL,
  	`reseller_id` varchar(200) NOT NULL DEFAULT '',
  	`year` int(4) DEFAULT NULL,
    `week` int(4) DEFAULT NULL,
    `month` int(4) DEFAULT NULL,
    `quarter` varchar(10) NOT NULL DEFAULT '',
  	`number_of_motorbikes` decimal(65,5) DEFAULT '0.00000',
	`number_of_vans` decimal(65,5) DEFAULT '0.00000',
	`number_of_tills` decimal(65,5) DEFAULT '0.00000',
	`lines_connected` decimal(65,5) DEFAULT '0.00000',
	`number_of_device` decimal(65,5) DEFAULT '0.00000',
	`sold` decimal(65,5) DEFAULT '0.00000',
	`device_attached` decimal(65,5) DEFAULT '0.00000',
	`total_airtime_sold` decimal(65,5) DEFAULT '0.00000',
	`MPESAFLOAT` decimal(65,5) DEFAULT '0.00000',
	`transaction_volume` decimal(65,5) DEFAULT '0.00000',
	`transaction_value` decimal(65,5) DEFAULT '0.00000',
	`MPESAFLOATLEVEL` decimal(65,5) DEFAULT '0.00000',
	`stock_volume` decimal(65,5) DEFAULT '0.00000',
	`stock_value` decimal(65,5) DEFAULT '0.00000',
	`device_sold` decimal(65,5) DEFAULT '0.00000',
	`lines_ordered` decimal(65,5) DEFAULT '0.00000',
	`lines_attached` decimal(65,5) DEFAULT '0.00000',
	`open_issues` decimal(65,5) DEFAULT '0.00000',
	`closed_issues` decimal(65,5) DEFAULT '0.00000',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE bi.report_metadata ADD COLUMN IF NOT EXISTS `is_editable` boolean NOT NULL DEFAULT true;
ALTER TABLE bi.report_metadata ADD COLUMN IF NOT EXISTS `is_mandatory` boolean NOT NULL DEFAULT false;
