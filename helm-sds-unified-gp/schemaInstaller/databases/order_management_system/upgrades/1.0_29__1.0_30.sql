SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_product_quota_rule`;
CREATE TABLE `order_product_quota_rule` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `reseller_id` varchar(80) NOT NULL DEFAULT '',
  `product_sku` varchar(60) NOT NULL DEFAULT '',
  `quota_for` varchar(10) NOT NULL DEFAULT '',
  `multiple_of` decimal(18,2) unsigned DEFAULT NULL,
  `hourly_limit` decimal(18,2) unsigned DEFAULT NULL,
  `daily_limit` decimal(18,2) unsigned DEFAULT NULL,
  `weekly_limit` decimal(18,2) unsigned DEFAULT NULL,
  `monthly_limit` decimal(18,2) unsigned DEFAULT NULL,
  `create_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_update_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `reseller_product_quota_for` (`reseller_id`,`product_sku`,`quota_for`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `order_product_quota`;
CREATE TABLE `order_product_quota` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_product_quota_rule_id` bigint(11) unsigned NOT NULL,
  `quantity` decimal(18,2) NOT NULL,
  `create_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  CONSTRAINT `order_product_quota_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `order_product_quota_hourly`;
CREATE TABLE `order_product_quota_hourly` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_product_quota_rule_id` bigint(11) unsigned NOT NULL,
  `hour` smallint(2) NOT NULL,
  `date` date NOT NULL,
  `quantity` decimal(18,2) NOT NULL,
  `last_update_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_product_quota_rule_id` (`order_product_quota_rule_id`,`hour`,`date`),
  CONSTRAINT `order_product_quota_rule_hourly_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `order_product_quota_daily`;
CREATE TABLE `order_product_quota_daily` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_product_quota_rule_id` bigint(11) unsigned NOT NULL,
  `day` date NOT NULL,
  `quantity` decimal(18,2) NOT NULL,
  `last_update_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_product_quota_rule_id` (`order_product_quota_rule_id`,`day`),
  CONSTRAINT `order_product_quota_rule_daily_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `order_product_quota_weekly`;
CREATE TABLE `order_product_quota_weekly` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_product_quota_rule_id` bigint(11) unsigned NOT NULL,
  `week_no` smallint(2) NOT NULL,
  `year` int(4) NOT NULL,
  `quantity` decimal(18,2) NOT NULL,
  `last_update_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_product_quota_rule_id` (`order_product_quota_rule_id`,`week_no`,`year`),
  CONSTRAINT `order_product_quota_weekly_rule_weekly_fk` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `order_product_quota_monthly`;
CREATE TABLE `order_product_quota_monthly` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `order_product_quota_rule_id` bigint(11) unsigned NOT NULL,
  `month` varchar(10) NOT NULL DEFAULT '',
  `year` int(4) NOT NULL,
  `quantity` decimal(18,2) NOT NULL,
  `last_update_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_product_quota_rule_id` (`order_product_quota_rule_id`,`month`,`year`),
  CONSTRAINT `order_product_quota_rule_monthly_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

DROP TRIGGER IF EXISTS `order_product_quota_insert_trigger`;

DELIMITER ;;
CREATE DEFINER=`refill`@`%` TRIGGER `order_product_quota_insert_trigger` AFTER INSERT ON `order_product_quota` FOR EACH ROW
  BEGIN
    INSERT INTO `order_product_quota_hourly`
    SET `order_product_quota_rule_id` = new.`order_product_quota_rule_id`, `hour` = HOUR(new.`create_timestamp`), `date` = DATE(new.`create_timestamp`), `quantity`= GREATEST(0, new.`quantity`)
    ON DUPLICATE KEY UPDATE
      `quantity` = GREATEST(0, (`quantity` + (new.`quantity`))),
      `hour` = HOUR(new.`create_timestamp`),
      `date` = DATE(new.`create_timestamp`);

    INSERT INTO `order_product_quota_daily`
    SET `order_product_quota_rule_id` = new.`order_product_quota_rule_id`, `day` = DATE(new.`create_timestamp`), `quantity`= GREATEST(0, new.`quantity`)
    ON DUPLICATE KEY UPDATE
      `quantity` = GREATEST(0, (`quantity` + (new.`quantity`))),
      `day` = DATE(new.`create_timestamp`);

    INSERT INTO `order_product_quota_weekly`
    SET `order_product_quota_rule_id` = new.`order_product_quota_rule_id`, `week_no` = WEEKOFYEAR(new.`create_timestamp`), `year` = YEAR(new.`create_timestamp`), `quantity`= GREATEST(0, new.`quantity`)
    ON DUPLICATE KEY UPDATE
      `quantity` = GREATEST(0, (`quantity` + (new.`quantity`))),
      `week_no` = WEEKOFYEAR(new.`create_timestamp`),
      `year` = YEAR(new.`create_timestamp`);

    INSERT INTO `order_product_quota_monthly`
    SET `order_product_quota_rule_id` = new.`order_product_quota_rule_id`, `month` = MONTHNAME(new.`create_timestamp`), `year` = YEAR(new.`create_timestamp`), `quantity`= GREATEST(0, new.`quantity`)
    ON DUPLICATE KEY UPDATE
      `quantity` = GREATEST(0, (`quantity` + (new.`quantity`))),
      `month` = MONTHNAME(new.`create_timestamp`),
      `year` = YEAR(new.`create_timestamp`);
  END ;;
DELIMITER ;
