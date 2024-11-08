START TRANSACTION;

CREATE
DATABASE IF NOT EXISTS order_management_system;

USE order_management_system;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `reseller_types`;
CREATE TABLE `reseller_types` (
  `type` varchar(20) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


DROP TABLE IF EXISTS `order_type`;
CREATE TABLE `order_type` (
  `order_type` varchar(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`order_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order_states - stores states for ONLY primary orders
# ------------------------------------------------------------
DROP TABLE IF EXISTS `order_states`;
CREATE TABLE `order_states` (
  `id` int(2) NOT NULL AUTO_INCREMENT,
  `order_state` varchar(60) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_state` (`order_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order_type_state_transition - stores state transitions for both internal and primary orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_type_state_transition`;
CREATE TABLE `order_type_state_transition` (
  `order_type` varchar(20) NOT NULL,
  `from_state_id` int(2) NOT NULL,
  `to_state_id` int(2) NOT NULL,
  PRIMARY KEY (`order_type`,`from_state_id`,`to_state_id`),
  KEY `from_state_id_fk` (`from_state_id`),
  KEY `to_state_id_fk` (`to_state_id`),
  KEY `order_type_id_fk` (`order_type`),
  CONSTRAINT `order_type_id_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) ON DELETE CASCADE,
  CONSTRAINT `from_state_id_fk` FOREIGN KEY (`from_state_id`) REFERENCES `order_states` (`id`) ON DELETE CASCADE,
  CONSTRAINT `to_state_id_fk` FOREIGN KEY (`to_state_id`) REFERENCES `order_states` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order -  stores primary order details
# ------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `order_id` varchar(255) NOT NULL,
  `order_type` varchar(20) NOT NULL,
  `trip_id` varchar(20) DEFAULT NULL,
  `buyer` varchar(20),
  `seller` varchar(20),
  `sender` varchar(20) NOT NULL,
  `receiver` varchar(20) NOT NULL,
  `initiator` varchar(20) NOT NULL,
  `order_data` longtext NOT NULL,
  `order_state` int(2) NOT NULL,
  `payment_agreement` VARCHAR(20),
  `create_timestamp` datetime DEFAULT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `type_fk` (`order_type`),
  KEY `state_fk` (`order_state`),
  CONSTRAINT `type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) ON DELETE CASCADE,
  CONSTRAINT `state_fk` FOREIGN KEY (`order_state`) REFERENCES `order_states` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_internal - stores internal order details
# ------------------------------------------------------------
DROP TABLE IF EXISTS `order_internal`;
CREATE TABLE `order_internal` (
  `order_internal_id` varchar(255) NOT NULL COMMENT 'DO/Shipment ID',
  `order_type` varchar(20) DEFAULT NULL,
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `receiver` varchar(20) DEFAULT NULL,
  `order_data` longtext NOT NULL,
  `order_state` VARCHAR(255) NOT NULL,
  `create_timestamp` datetime DEFAULT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`order_internal_id`),
  KEY `order_id_fk` (`order_id`),
  CONSTRAINT `order_id_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table invoice -  stores invoice details against (primary order + seller)
# ------------------------------------------------------------
DROP TABLE IF EXISTS `invoice`;
CREATE TABLE `invoice` (
  `invoice_id` varchar(255) NOT NULL DEFAULT '',
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `seller` varchar(20) NOT NULL,
  `buyer` varchar(20) NOT NULL,
  `receiver` varchar(20) NOT NULL,
  `generated_by` varchar(20) NOT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `due_amount` decimal(19,2) NOT NULL,
  `payment_mode` varchar(255) NOT NULL COMMENT 'POD/MPesa etc.',
  `status` varchar(50) NOT NULL COMMENT 'Paid/Due/NA etc.',
  `data` longtext DEFAULT NULL,
  `create_timestamp` datetime NOT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`invoice_id`),
  UNIQUE KEY `order_id_seller` (`order_id`,`seller`),
  CONSTRAINT `order_id_fk2` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table payments - stores payments made against some invoice
# ------------------------------------------------------------
DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
  `payment_id` varchar(255) NOT NULL DEFAULT '',
  `data` longtext,
  `payment_mode` varchar(255) NOT NULL COMMENT 'POD/MPesa etc.',
  `total_amount` decimal(19,2) NOT NULL,
  `available_amount` decimal(19,2) NOT NULL,
  `payment_link` varchar(255) DEFAULT NULL,
  `generated_by` varchar(255) NOT NULL,
  `payee` varchar(255) NOT NULL,
  `payer` VARCHAR(255) NOT NULL,
  `create_timestamp` datetime NOT NULL,
  `status` varchar(50) NOT NULL COMMENT 'OPEN/SETTLED/PENDING/FAILED',
  PRIMARY KEY (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_reason - stores reasons for ONLY primary orders
# ------------------------------------------------------------
DROP TABLE IF EXISTS `order_reason`;
CREATE TABLE `order_reason` (
  `id` int(2) NOT NULL AUTO_INCREMENT,
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `reason_description` varchar(255) DEFAULT NULL,
  `reason_provided_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `reject_order_id_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_reason_type - stores order reason type
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_reason_type`;
CREATE TABLE IF NOT EXISTS `order_reason_type` (
  `code` varchar(255) NOT NULL DEFAULT '',
  `type` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`code`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table ersinstall - stores upgrades information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table payment_agreement - stores payment agreement information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `payment_agreement`;
CREATE TABLE IF NOT EXISTS `payment_agreement` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table payment_mode - stores payment mode information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `payment_mode`;
CREATE TABLE IF NOT EXISTS `payment_mode` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table user_payment_mode_mapping - stores payment mode mapping with reseller_type/resellerId information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_payment_mode_mapping`;
CREATE TABLE IF NOT EXISTS `user_payment_mode_mapping` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `reseller_type` varchar(255) DEFAULT '',
  `reseller_id` varchar(255) DEFAULT '',
  `order_type` varchar(20) NOT NULL DEFAULT '',
  `payment_mode` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_payment_mode` (`reseller_type`,`reseller_id`,`order_type`,`payment_mode`),
  KEY `order_type_fk` (`order_type`),
  KEY `payment_mode_fk` (`payment_mode`),
  CONSTRAINT `order_type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) ON DELETE CASCADE,
  CONSTRAINT `payment_mode_fk` FOREIGN KEY (`payment_mode`) REFERENCES `payment_mode` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table user_payment_agreement_mapping - stores payment agreement mapping with reseller_type/resellerId information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_payment_agreement_mapping`;
CREATE TABLE IF NOT EXISTS `user_payment_agreement_mapping` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `reseller_type` varchar(255) DEFAULT '',
  `reseller_id` varchar(255) DEFAULT '',
  `order_type` varchar(20) NOT NULL DEFAULT '',
  `payment_agreement` varchar(20) NOT NULL DEFAULT '',
  KEY `ordr_type_fk` (`order_type`),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_payment_agreement` (`reseller_type`,`reseller_id`,`order_type`,`payment_agreement`),
  KEY `payment_agreement_fk` (`payment_agreement`),
  CONSTRAINT `ordr_type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) ON DELETE CASCADE,
  CONSTRAINT `payment_agreement_fk` FOREIGN KEY (`payment_agreement`) REFERENCES `payment_agreement` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_transaction_category_type - stores type of order transaction information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_transaction_category_type`;
CREATE TABLE IF NOT EXISTS `order_transaction_category_type` (
  `type` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_transaction - stores order transaction data related to some order information
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_transaction`;
CREATE TABLE IF NOT EXISTS `order_transaction` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `sender` varchar(256) DEFAULT NULL,
  `receiver` varchar(256) DEFAULT NULL,
  `transaction_category` varchar(256) DEFAULT NULL,
  `amount` decimal(19,2) NOT NULL,
  `trip_id` varchar(256) DEFAULT NULL,
  `status` varchar(256) DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `external_reference_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tx_category_type_fk` (`transaction_category`),
  CONSTRAINT `tx_category_type_fk` FOREIGN KEY (`transaction_category`) REFERENCES `order_transaction_category_type` (`type`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

# table order_type_category - stores the categories for order-types, can be used to configure
#  the ui related description, categorization for some ordre type
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_type_category`;
CREATE TABLE IF NOT EXISTS `order_type_category` (
  `order_category_name` varchar(20) NOT NULL,
  `order_type` varchar(20) NOT NULL,
  `order_category` varchar(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`order_category_name`),
  KEY `order_type_fk_key` (`order_type`),
  CONSTRAINT `order_type_fk_key` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) ON DELETE CASCADE,
  CONSTRAINT chk_order_category CHECK (order_category IN ('RETURN', 'REVERSAL', 'ORDER', 'STOCK_TRANSFER','MANUAL_ADJUSTMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `scheduler_info`;
CREATE TABLE IF NOT EXISTS `scheduler_info` (
	`id` int(11) unsigned NOT NULL AUTO_INCREMENT,
	`scheduler_name` varchar(256) DEFAULT NULL,
	`start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`end_time` datetime,
	`status` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `shedlock`;
CREATE TABLE IF NOT EXISTS `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` datetime NOT NULL,
  `locked_at` datetime NOT NULL,
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
  CONSTRAINT `order_product_quota_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) ON DELETE CASCADE
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
  CONSTRAINT `order_product_quota_rule_hourly_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) ON DELETE CASCADE
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
  CONSTRAINT `order_product_quota_rule_daily_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) ON DELETE CASCADE
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
  CONSTRAINT `order_product_quota_weekly_rule_weekly_fk` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) ON DELETE CASCADE
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
  CONSTRAINT `order_product_quota_rule_monthly_fK` FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `invoice_settlement`;
CREATE TABLE `invoice_settlement` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `payment_id` varchar(255) NOT NULL DEFAULT '',
  `invoice_id` varchar(255) NOT NULL DEFAULT '',
  `total_amount` decimal(19,2) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `payments_stt_fk` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) ON DELETE CASCADE,
  CONSTRAINT `invoice_stt_fk` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `ledger_balance`;
CREATE TABLE IF NOT EXISTS `ledger_balance` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `payer` varchar(256) DEFAULT NULL,
  `payee` varchar(256) DEFAULT NULL,
  `due_amount` decimal(19,2) DEFAULT NULL,
  `unused_credit` decimal(19,2) DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `create_timestamp` datetime NOT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table credit_note -  stores credit note details against (return order + seller)
# ------------------------------------------------------------

DROP TABLE IF EXISTS `credit_note`;
CREATE TABLE `credit_note` (
  `credit_note_id` varchar(255) NOT NULL DEFAULT '',
  `return_order_id` varchar(255) NOT NULL COMMENT 'Return order ID',
  `original_order_id` varchar(255) NOT NULL COMMENT 'Original order ID which is going to return',
  `seller` varchar(20) NOT NULL,
  `buyer` varchar(20) NOT NULL,
  `receiver` varchar(20) NOT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `status` varchar(50) NOT NULL COMMENT 'PENDING etc.',
  `data` longtext DEFAULT NULL,
  `create_timestamp` datetime NOT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`credit_note_id`),
  UNIQUE KEY `return_order_id_seller` (`return_order_id`,`seller`),
  CONSTRAINT `return_order_id_fK_credit` FOREIGN KEY (`return_order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `original_order_id_fK_credit` FOREIGN KEY (`original_order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_returnable_inventory_count -  stores the returnable items count for some order
# --------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS `order_returnable_inventory_count`;
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


################# Triggers #################
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

CREATE TABLE `sequence` (
                    `sequence_id` bigint(18) unsigned NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (`sequence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


SET FOREIGN_KEY_CHECKS = 1;
COMMIT;