CREATE SCHEMA IF NOT EXISTS `contractmanagement` DEFAULT CHARACTER SET utf8 ;

CREATE TABLE `contractmanagement`.`ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `commission_contracts` (
  `contract_key` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `receiver_key1` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated but will check the usage in legacy system.',
  `receiver_key2` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated but will check the usage in legacy system.',
  `receiver_key3` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated but will check the usage in legacy system.',
  `country_key` int(11) NOT NULL DEFAULT 0,
  `distributor_key` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated but will discuss as this is very critical for backward compatibility',
  `chain_key` int(11) NOT NULL DEFAULT 0,
  `currency_key` int(11) NOT NULL DEFAULT 0,
  `id` varchar(100) NOT NULL DEFAULT '',
  `reseller_type_key` int(11) NOT NULL DEFAULT 0,
  `reseller_tag` varchar(45) DEFAULT NULL COMMENT 'Deprecated',
  `receiver_tag` varchar(45) DEFAULT NULL COMMENT 'Deprecated',
  `cloned_from` int(11) DEFAULT NULL,
  `contract_status` bit(1) DEFAULT b'1' COMMENT 'what are the possible values for the status column',
  `contract_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'what type data will go into this column',
  `created_by` varchar(50) DEFAULT NULL,
  `created_at` varchar(50) DEFAULT '',
  `modified_by` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`contract_key`),
  KEY `tmstmp_index` (`last_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

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

CREATE TABLE `dwa_contract_account_types` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `account_type` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


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
  `value_expression` varchar(256) DEFAULT NULL,
  `tag_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `dwa_contract_margin_rules` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `entry_key` int(11) DEFAULT NULL,
  `entry_range_key` int(11) DEFAULT NULL,
  `account_id` varchar(80) DEFAULT NULL,
  `pay_options_account_type_id` varchar(80) DEFAULT NULL,
  `contract_account_type_id` int(11) DEFAULT NULL,
  `value_type_id` int(11) DEFAULT NULL,
  `value_expression` varchar(256) DEFAULT NULL,
  `tag_id` int(11) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `margin_rule_key` (`entry_key`,`entry_range_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `dwa_contract_price_entries` (
  `entry_key` int(11) NOT NULL AUTO_INCREMENT,
  `contract_key` int(11) NOT NULL DEFAULT 0,
  `currency_key` int(11) NOT NULL DEFAULT 0,
  `product_key` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated, later on we will remove it',
  `valid_from` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `user_key` int(11) NOT NULL DEFAULT 0 COMMENT 'Deprecated, change it to created_by',
  `description` varchar(80) NOT NULL DEFAULT '' COMMENT 'Deprecated, will remove it',
  `comment` varchar(80) NOT NULL DEFAULT '',
  `created_by` varchar(50) DEFAULT NULL,
  `status` bit(1) DEFAULT b'1' COMMENT '1 implies true',
  `type` varchar(100) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'This is needed for audit purpose',
  PRIMARY KEY (`entry_key`),
  KEY `contract_key` (`contract_key`,`product_key`,`valid_from`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `dwa_contract_tags` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `dwa_contract_value_type` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `value_type` varchar(50) DEFAULT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

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

INSERT INTO `loc_countries` (select * from Refill.loc_countries);
INSERT INTO `loc_currencies` (select * from Refill.loc_currencies);
INSERT INTO `pay_options` (select * from Refill.pay_options);

INSERT INTO `dwa_contract_tags` (`id`, `tag_name`, `last_modified`)
VALUES
	(1, 'SENDER', '2021-04-27 13:55:45'),
	(2, 'RECEIVER', '2021-04-27 13:55:45'),
	(3, 'TRANSACTION_FEE', '2021-04-27 13:55:45'),
	(4, 'COMMISSION', '2021-04-27 13:55:45'),
	(5, 'ROYALTY', '2021-04-27 13:55:45'),
	(6, 'BONUS', '2021-04-27 13:55:45'),
	(7, 'TAX', '2021-04-27 13:55:45'),
	(8, 'DU_MARGIN', '2021-04-27 13:55:45'),
	(9, 'SEAMLESS_MARGIN', '2021-04-27 13:55:45'),
	(10, 'SENDER_COMMISSION', '2021-04-27 13:55:45'),
	(11, 'SENDER_BONUS', '2021-04-27 13:55:45'),
	(12, 'RECEIVER_COMMISSION', '2021-04-27 13:55:45'),
	(13, 'RECEIVER_BONUS', '2021-04-27 13:55:45'),
	(14, 'DISCOUNT', '2021-04-27 13:55:45'),
	(15, 'RECEIVER_DISCOUNT', '2021-04-27 13:55:45');
	
INSERT INTO `dwa_contract_value_type` (`id`, `value_type`, `last_modified`)
VALUES
	(1, 'Absolute', '2021-04-27 13:55:45'),
	(2, 'Percentage', '2021-04-27 13:55:45'),
	(3, 'Expression', '2021-04-27 13:55:45');
	
INSERT INTO `dwa_contract_account_types` (`id`, `account_type`, `last_modified`)
VALUES
	(1, 'Sender', '2021-04-27 13:55:45'),
	(2, 'Receiver', '2021-04-27 13:55:45'),
	(3, 'Fixed', '2021-04-27 13:55:45'),
	(4, 'ParentOfSender', '2021-04-27 13:55:45'),
	(5, 'ParentOfReceiver', '2021-04-27 13:55:45');

	