DROP TABLE IF EXISTS `bi`.`all_orders_status_aggregator`;
CREATE TABLE `bi`.`all_orders_status_aggregator` (
 `id` varchar(200) NOT NULL,
 `transaction_date` datetime DEFAULT NULL,
 `transaction_number` varchar(200) NOT NULL DEFAULT '',
 `month_value` varchar(200) NOT NULL DEFAULT '',
 `quarter` varchar(200) NOT NULL DEFAULT '',
 `order_id` varchar(200) NOT NULL,
 `order_status` varchar(200) NOT NULL DEFAULT '',
 `order_type` varchar(200) DEFAULT NULL,
 `reseller_type` varchar(200) DEFAULT NULL,
 `reseller_id` varchar(200) NOT NULL DEFAULT '',
 `buyer_id` varchar(200) NOT NULL DEFAULT '',
 `sender_id` varchar(200) DEFAULT NULL,
 `seller_id` varchar(200) NOT NULL DEFAULT '',
 `receiver_id` varchar(200) NOT NULL DEFAULT '',
 `drop_location_id` varchar(200) DEFAULT NULL,
 `pickup_location_id` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
