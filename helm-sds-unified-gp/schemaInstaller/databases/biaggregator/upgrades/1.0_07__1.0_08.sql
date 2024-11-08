DROP TABLE IF EXISTS `reseller_current_stock`;
CREATE TABLE `reseller_current_stock` (
  `id` varchar(100) NOT NULL DEFAULT '',
  `product` varchar(100) DEFAULT NULL,
  `reseller_id` varchar(100) DEFAULT NULL,
  `product_type` varchar(100) DEFAULT NULL,
  `status` varchar(100) DEFAULT NULL,
  `stock_count` int(11) DEFAULT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `distributor_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
