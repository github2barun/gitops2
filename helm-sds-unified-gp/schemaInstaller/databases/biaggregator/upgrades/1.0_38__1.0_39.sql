DROP TABLE IF EXISTS `bi`.`all_orders_live_map_aggregator`;
CREATE TABLE `bi`.`all_orders_live_map_aggregator` (
  `id` varchar(200) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `transaction_number` varchar(200) NOT NULL DEFAULT '',
  `order_id` varchar(200) NOT NULL DEFAULT '',
  `order_status` varchar(200) NOT NULL DEFAULT '',
  `order_type` varchar(200) DEFAULT NULL,
  `reseller_id` varchar(200) DEFAULT '',
  `sender_id` varchar(200) DEFAULT NULL,
  `buyer_id` varchar(200) DEFAULT '',
  `seller_id` varchar(200) DEFAULT '',
  `receiver_id` varchar(200) DEFAULT '',
  `drop_location_id` varchar(200) DEFAULT NULL,
  `pickup_location_id` varchar(200) DEFAULT NULL,
  `product_code` varchar(200) NOT NULL DEFAULT '',
  `product_sku` varchar(200) NOT NULL DEFAULT '',
  `product_name` varchar(200) NOT NULL DEFAULT '',
  `product_description` varchar(200) NOT NULL DEFAULT '',
  `product_type` varchar(200) NOT NULL DEFAULT '',
  `batch_id` varchar(200) DEFAULT '',
  `category_path` varchar(200) NOT NULL DEFAULT '',
  `quantity` bigint(20) DEFAULT NULL,
  `reseller_type` varchar(200) DEFAULT NULL,
  `latitude` varchar(200) DEFAULT '',
  `longitude` varchar(200) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

DROP TABLE IF EXISTS `bi`.`reseller_stock_inout_stats`;
CREATE TABLE `bi`.`reseller_stock_inout_stats` (
  `id` varchar(100) NOT NULL,
  `reseller_id` varchar(100) NOT NULL,
  `productSKU` varchar(100) NOT NULL,
  `status` varchar(100) NOT NULL,
  `stock_count` int(11) NOT NULL,
  `date` date NOT NULL,
  `last_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE reseller_current_status ADD COLUMN suburb VARCHAR(200);