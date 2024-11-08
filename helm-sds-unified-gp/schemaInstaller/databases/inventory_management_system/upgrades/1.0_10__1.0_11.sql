CREATE TABLE IF NOT EXISTS `reserved_inventory_cache` (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `owner_id` varchar(60) DEFAULT NULL,
    `location_id` varchar(60)  DEFAULT NULL,
    `product_sku` varchar(255) NOT NULL,
    `product_type` varchar(60)  DEFAULT NULL,
    `serial_number` DECIMAL(40, 0),
    `start_no` BIGINT(11) NOT NULL,
    `end_no` BIGINT(11) NOT NULL,
    `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `inventory_cache_owner_fk1` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`),
    CONSTRAINT `inventory_cache_location_fk2` FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;