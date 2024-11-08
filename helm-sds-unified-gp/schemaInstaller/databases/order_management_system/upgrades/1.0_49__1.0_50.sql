CREATE TABLE IF NOT EXISTS `order_returnable_inventory_count` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_id` varchar(255) NOT NULL,
  `product_sku` varchar(255) NOT NULL,
  `quantity` decimal(20,0) NOT NULL,
  `unit_product` varchar(255) DEFAULT NULL,
  `unit_quantity` decimal(20,0) DEFAULT NULL,
  `ratio` decimal(20,0) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
