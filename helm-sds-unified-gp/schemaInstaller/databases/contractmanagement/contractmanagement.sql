START TRANSACTION;

CREATE SCHEMA IF NOT EXISTS `contractmanagement` DEFAULT CHARACTER SET utf8 ;

USE contractmanagement;

SET FOREIGN_KEY_CHECKS = 0;


DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `contractmanagement`.`ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `commission_contracts`;
CREATE TABLE `commission_contracts` (
  `contract_key` int(11) NOT NULL AUTO_INCREMENT,
  `id` varchar(255) NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `country_key` int(11) NOT NULL DEFAULT 0,
  `reseller_type_key` int(11) NOT NULL DEFAULT 0,
  `cloned_from` varchar(255) DEFAULT NULL COMMENT 'Name of contract from where it is cloned from',
  `contract_status` int(1) DEFAULT 0 COMMENT 'what are the possible values for the status column',
  `contract_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'what type data will go into this column' CHECK (json_valid(`contract_data`)),
  `created_by` varchar(45) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `modified_by` varchar(45) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`contract_key`),
  KEY `tmstmp_index` (`last_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `commission_shop_settings`;
CREATE TABLE `commission_shop_settings` (
  `shop_key` int(11) NOT NULL AUTO_INCREMENT,
  `reseller_key` int(11) NOT NULL DEFAULT 0,
  `shop_tag` varchar(80) NOT NULL DEFAULT '',
  `shop_name` varchar(80) NOT NULL DEFAULT '',
  `shop_type` varchar(80) NOT NULL DEFAULT '',
  `product_selection_tag` varchar(80) NOT NULL DEFAULT '',
  `payment_options_tag` varchar(80) NOT NULL DEFAULT '',
  `application` varchar(36) NOT NULL DEFAULT '',
  `default_language` varchar(16) NOT NULL DEFAULT '',
  `custom_parameters` blob NOT NULL,
  `appearence_base` varchar(80) NOT NULL DEFAULT '',
  `appearence_addition` varchar(80) NOT NULL DEFAULT '',
  `receipt_template_key` int(11) NOT NULL DEFAULT 0,
  `product_range_key` int(11) NOT NULL DEFAULT 0,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`shop_key`),
  KEY `product_range_key` (`product_range_key`),
  KEY `tmstmp_index` (`last_modified`),
  KEY `shop_tag` (`shop_tag`),
  KEY `type_product_range` (`shop_type`,`product_range_key`),
  KEY `reseller_key` (`reseller_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `dwa_contract_account_types`;
CREATE TABLE `dwa_contract_account_types` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `account_type` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `dwa_contract_audit_entries`;
CREATE TABLE `dwa_contract_audit_entries` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `contract_key` int(11) DEFAULT 0,
  `product_key` int(11) DEFAULT 0,
  `entry_key` int(11) DEFAULT NULL,
  `entry_range_key` int(11) DEFAULT NULL,
  `from_amount` bigint(20) DEFAULT 0,
  `to_amount` bigint(20) DEFAULT 0,
  `valid_from` datetime DEFAULT '0000-00-00 00:00:00',
  `valid_until` timestamp NULL DEFAULT current_timestamp(),
  `margin_rule_key` int(11) DEFAULT 0,
  `account_id` varchar(80) DEFAULT NULL,
  `pay_options_account_type_id` int(11) DEFAULT NULL,
  `contract_account_type_id` int(11) DEFAULT NULL,
  `value_type_id` int(11) DEFAULT NULL,
  `value_expression` text DEFAULT NULL,
  `tag_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `dwa_contract_margin_rules`;
CREATE TABLE `dwa_contract_margin_rules` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `entry_key` int(11) DEFAULT NULL,
  `entry_range_key` int(11) DEFAULT NULL,
  `account_id` varchar(80) DEFAULT NULL,
  `pay_options_account_type_id` varchar(80) DEFAULT NULL,
  `contract_account_type_id` int(11) DEFAULT NULL,
  `value_type_id` int(11) DEFAULT NULL,
  `value_expression` text DEFAULT NULL COMMENT 'stores both algebraic and freemarker expression',
  `tag_id` int(11) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `ftl_grammar` text DEFAULT NULL COMMENT 'stores corresponding ftl grammar map per condition ',
  `group_id` int(10) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `margin_rule_key` (`entry_key`,`entry_range_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;



DROP TABLE IF EXISTS `dwa_contract_price_entries`;
CREATE TABLE `dwa_contract_price_entries` (
  `entry_key` int(11) NOT NULL AUTO_INCREMENT,
  `contract_key` int(11) NOT NULL DEFAULT 0,
  `currency_key` int(11) NOT NULL DEFAULT 0,
  `product_key` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated, later on we will remove it',
  `valid_from` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `created_by` varchar(45) DEFAULT NULL,
  `comment` varchar(80) NOT NULL DEFAULT '',
  `status` bit DEFAULT 1 COMMENT '1 implies true',
  `type` varchar(100) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'This is needed for audit purpose',
  PRIMARY KEY (`entry_key`),
  KEY `contract_key` (`contract_key`,`product_key`,`valid_from`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `dwa_contract_tags`;
CREATE TABLE `dwa_contract_tags` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `dwa_contract_value_type`;
CREATE TABLE `dwa_contract_value_type` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `value_type` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `dwa_price_entry_ranges`;
CREATE TABLE `dwa_price_entry_ranges` (
  `entry_key` int(11) NOT NULL DEFAULT 0,
  `from_amount` bigint(20) NOT NULL DEFAULT 0,
  `to_amount` bigint(20) NOT NULL DEFAULT 0,
  `reseller_margin` int(11) NOT NULL DEFAULT 0 COMMENT 'We will keep it for the sake of backward compatibility',
  `customer_margin` int(11) NOT NULL DEFAULT 0 COMMENT 'We will keep it for the sake of backward compatibility',
  `margin_type` int(11) NOT NULL DEFAULT 0 COMMENT 'We will keep it for the sake of backward compatibility',
  `customer_bonus` int(11) NOT NULL DEFAULT 0 COMMENT 'We will keep it for the sake of backward compatibility',
  `bonus_type` int(11) NOT NULL DEFAULT 0 COMMENT 'We will keep it for the sake of backward compatibility',
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `entry_key` (`entry_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `pay_options`;
CREATE TABLE `pay_options` (
  `account_type_key` int(11) NOT NULL AUTO_INCREMENT,
  `id` varchar(32) NOT NULL DEFAULT '',
  `name` varchar(64) NOT NULL DEFAULT '',
  `url` varchar(64) NOT NULL DEFAULT '',
  `status` int(4) NOT NULL DEFAULT 1,
  `description` varchar(255) NOT NULL DEFAULT '',
  `balance_check` tinyint(1) NOT NULL DEFAULT 0,
  `reseller_account_type` tinyint(1) NOT NULL DEFAULT 0,
  `account_sharing_policy` tinyint(1) NOT NULL DEFAULT 0,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `payment_currency_key` int(11) NOT NULL DEFAULT 0,
  `min_account_balance` bigint(20) NOT NULL DEFAULT 0,
  `max_account_balance` bigint(20) NOT NULL DEFAULT 0,
  `min_transaction_amount` bigint(20) NOT NULL DEFAULT 0,
  `max_transaction_amount` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`account_type_key`),
  UNIQUE KEY `id_index` (`id`),
  KEY `tmstmp_index` (`last_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `loc_countries`;
CREATE TABLE `loc_countries` (
  `country_key` int(11) NOT NULL AUTO_INCREMENT,
  `abbreviation` varchar(10) NOT NULL DEFAULT '',
  `name` varchar(40) NOT NULL DEFAULT '',
  `system_primary` tinyint(1) NOT NULL DEFAULT 0,
  `date_format` varchar(40) NOT NULL DEFAULT '',
  `time_format` varchar(40) NOT NULL DEFAULT '',
  `decimal_separator` varchar(5) NOT NULL DEFAULT '',
  `thousands_separator` varchar(5) NOT NULL DEFAULT '',
  `number_format` varchar(40) NOT NULL DEFAULT '',
  `number_regexp` varchar(40) NOT NULL DEFAULT '',
  `primary_currency_key` int(11) NOT NULL DEFAULT 0,
  `primary_language_key` int(11) NOT NULL DEFAULT 0,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `selectable` tinyint(4) NOT NULL DEFAULT 0,
  `country_code` varchar(5) NOT NULL DEFAULT '',
  `MSISDN_significant_digits` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`country_key`),
  KEY `tmstmp_index` (`last_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `loc_currencies`;
CREATE TABLE `loc_currencies` (
  `currency_key` int(11) NOT NULL AUTO_INCREMENT,
  `country_key` int(11) NOT NULL DEFAULT 0,
  `abbreviation` varchar(10) NOT NULL DEFAULT '',
  `name` varchar(40) NOT NULL DEFAULT '',
  `symbol` varchar(10) NOT NULL DEFAULT '',
  `natural_format` varchar(255) NOT NULL DEFAULT '',
  `minorcur_decimals` tinyint(4) NOT NULL DEFAULT 0,
  `minorcur_name` varchar(20) NOT NULL DEFAULT '',
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `selectable` tinyint(4) NOT NULL DEFAULT 1,
  `currency_code` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`currency_key`),
  KEY `tmstmp_index` (`last_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `transaction_field`;
CREATE TABLE `transaction_field` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `field_key` varchar(40) NOT NULL,
  `field_value_list` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `rules`;
CREATE TABLE `rules` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(40) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `rule_fields_association`;
CREATE TABLE `rule_fields_association` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL,
  `field_key` varchar(50) NOT NULL,
  `field_value` longtext NOT NULL,
  `created_by` varchar(40) NOT NULL,
  `created_date` DATETIME NOT NULL DEFAULT current_timestamp,
  `valid_from` DATETIME NOT NULL DEFAULT current_timestamp,
  `valid_until` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `product_variant_rule_association`;
CREATE TABLE `product_variant_rule_association` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL,
  `product_variant_id_sku_list` longtext NOT NULL,
  `created_by` varchar(40) NOT NULL,
  `created_date` DATETIME NOT NULL DEFAULT current_timestamp,
  `valid_from` DATETIME NOT NULL DEFAULT current_timestamp,
  `valid_until` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;