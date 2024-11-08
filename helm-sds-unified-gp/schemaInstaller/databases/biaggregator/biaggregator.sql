/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table ersinstall
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



# Dump of table totaltransactions_channel
# ------------------------------------------------------------

DROP TABLE IF EXISTS `channel_wise_day_wise`;
CREATE TABLE `channel_wise_day_wise` (
  `id` varchar(255) NOT NULL,
  `operator` varchar(200) DEFAULT NULL,
  `end_time_day` date DEFAULT NULL,
  `sender_region` varchar(200) DEFAULT NULL,
  `channel` varchar(200) DEFAULT NULL,
  `account_type` varchar(200) DEFAULT NULL,
  `reseller_type` varchar(200) DEFAULT NULL,
  `transaction_type` varchar(200) DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  `amount` decimal(20,5) DEFAULT NULL,
  `currency` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `non_monetory_channel_wise_day_wise`;
CREATE TABLE `non_monetory_channel_wise_day_wise` (
  `id` varchar(255) NOT NULL,
  `operator` varchar(200) DEFAULT NULL,
  `end_time_day` date DEFAULT NULL,
  `sender_region` varchar(200) DEFAULT NULL,
  `channel` varchar(200) DEFAULT NULL,
  `account_type` varchar(200) DEFAULT NULL,
  `reseller_type` varchar(200) DEFAULT NULL,
  `transaction_type` varchar(200) DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  `amount` decimal(20,5) DEFAULT NULL,
  `currency` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `std_sales_trend_aggregation`;
CREATE TABLE `std_sales_trend_aggregation` (
`id` varchar(255) NOT NULL,
`aggregationDate` date DEFAULT NULL,
`account_type` varchar(200) DEFAULT NULL,
`resellerId` varchar(200) DEFAULT NULL,
`resellerMSISDN` varchar(200) DEFAULT NULL,
`resellerName` varchar(200) DEFAULT NULL,
`resellerTypeId` varchar(200) DEFAULT NULL,
`reseller_path` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(20) DEFAULT NULL,
`transactionAmount` decimal(20,5) DEFAULT NULL,
`reseller_commission` decimal(20,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `regional_std_sales_trend_aggregation`;
CREATE TABLE `regional_std_sales_trend_aggregation` (
`id` varchar(255) NOT NULL,
`aggregationDate` date DEFAULT NULL,
`account_type` varchar(200) DEFAULT NULL,
`resellerId` varchar(200) DEFAULT NULL,
`resellerMSISDN` varchar(200) DEFAULT NULL,
`resellerName` varchar(200) DEFAULT NULL,
`resellerTypeId` varchar(200) DEFAULT NULL,
`reseller_path` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(20) DEFAULT NULL,
`transactionAmount` decimal(20,5) DEFAULT NULL,
`reseller_commission` decimal(20,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
`zone` varchar(200) DEFAULT NULL,
`area` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `detail_balance_report_aggregation`;
CREATE TABLE `detail_balance_report_aggregation` (
  `id` varchar(255) NOT NULL,
  `reseller_id` varchar(200) NOT NULL,
  `reseller_name` varchar(200) DEFAULT NULL,
  `msisdn` varchar(200) DEFAULT NULL,
  `reseller_parent` varchar(200) DEFAULT NULL,
  `reseller_status` varchar(200) DEFAULT NULL,
  `reseller_type` varchar(200) DEFAULT NULL,
  `opening_balance` decimal(20,5) DEFAULT NULL,
  `closing_balance` decimal(20,5) DEFAULT NULL,
  `balance_transfer_in` decimal(20,5) DEFAULT NULL,
  `balance_transfer_out` decimal(20,5) DEFAULT NULL,
  `count_transfer_in` bigint(20) DEFAULT '0',
   `count_transfer_out` bigint(20) DEFAULT '0',
  `current_balance` decimal(20,5) DEFAULT NULL,
  `account_id` varchar(200) DEFAULT NULL,
  `reseller_path` varchar(200) DEFAULT NULL,
  `currency` varchar(200) DEFAULT NULL,
  `last_transaction_date` timestamp NULL DEFAULT NULL,
  `region` varchar(200) DEFAULT NULL,
  `account_type_id` varchar(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `receiver_wise_credit_transfer_summary`;
CREATE TABLE `receiver_wise_credit_transfer_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`reseller_id` varchar(200) DEFAULT NULL,
`reseller_msisdn` varchar(200) DEFAULT NULL,
`reseller_name` varchar(200) DEFAULT NULL,
`reseller_type_id` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(25) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(255) DEFAULT NULL,
`receiver_account_type` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `regional_receiver_wise_credit_transfer_summary`;
CREATE TABLE `regional_receiver_wise_credit_transfer_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`reseller_id` varchar(200) DEFAULT NULL,
`reseller_msisdn` varchar(200) DEFAULT NULL,
`reseller_name` varchar(200) DEFAULT NULL,
`reseller_type_id` varchar(200) DEFAULT NULL,
`region` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`unique_receiver_count` bigint(20) DEFAULT NULL,
`count` bigint(25) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(255) DEFAULT NULL,
`receiver_account_type` varchar(255) DEFAULT NULL,
`zone` varchar(200) DEFAULT NULL,
`area` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `last_transaction_aggregator`;
CREATE TABLE `last_transaction_aggregator` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) DEFAULT NULL,
  `reseller_msisdn` varchar(200) DEFAULT NULL,
  `reseller_name` varchar(200) DEFAULT NULL,
  `reseller_level` varchar(200) DEFAULT NULL,
  `account_id` varchar(200) DEFAULT NULL,
  `transaction_count` int(200) DEFAULT NULL,
  `balance` decimal(20,5) DEFAULT NULL,
  `total_credit` decimal(20,5) DEFAULT NULL,
  `last_transaction_date` date DEFAULT NULL,
  `last_transaction_type` varchar(200) DEFAULT NULL,
  `last_transaction_amount` decimal(20,5) DEFAULT NULL,
  `last_transaction_currency` varchar(200) DEFAULT NULL,
  `receiver_msisdn` varchar(200) DEFAULT NULL,
  `last_transaction_id` varchar(200) DEFAULT NULL,
  `region` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `hourly_total_transactions`;
CREATE TABLE `hourly_total_transactions` (
`id` varchar(255) NOT NULL,
`end_time_day` date DEFAULT NULL,
`end_time_hour` time DEFAULT NULL,
`sender_region` varchar(200) DEFAULT NULL,
`sender_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`channel` varchar(200) DEFAULT NULL,
`result_status` varchar(100) DEFAULT NULL,
`count` bigint(100) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `non_monetory_hourly_total_transactions`;
CREATE TABLE `non_monetory_hourly_total_transactions` (
`id` varchar(255) NOT NULL,
`end_time_day` date DEFAULT NULL,
`end_time_hour` time DEFAULT NULL,
`sender_region` varchar(200) DEFAULT NULL,
`sender_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`channel` varchar(200) DEFAULT NULL,
`result_status` varchar(100) DEFAULT NULL,
`count` bigint(100) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `electronic_recharge`;
CREATE TABLE `electronic_recharge` (
`id` varchar(255) NOT NULL DEFAULT '',
`aggregation_date` date DEFAULT NULL,
`channel` varchar(200) DEFAULT NULL,
`r2r_count` bigint(200) DEFAULT NULL,
`r2r_amount` decimal(20,5) DEFAULT NULL,
`r2s_count` bigint(200) DEFAULT NULL,
`r2s_amount` decimal(20,5) DEFAULT NULL,
`currency` varchar(11) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `low_stock_alert_aggregation`;
CREATE TABLE `low_stock_alert_aggregation` (
`resellerId` varchar(50) NOT NULL,
`resellerMSISDN` varchar(50) NOT NULL,
`resellerName` varchar(50) DEFAULT NULL,
`agentMSISDN` varchar(50) DEFAULT NULL,
`agentName` varchar(50) DEFAULT NULL,
`resellerRegion` varchar(50) DEFAULT NULL,
`resellerLocation` varchar(50) DEFAULT NULL,
`resellerCluster` varchar(50) DEFAULT NULL,
`stockThreshold` decimal(65,5) DEFAULT NULL,
`stockLevel` decimal(65,5) DEFAULT NULL,
`aggregationDate` datetime NOT NULL,
PRIMARY KEY (`resellerId`,`aggregationDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `std_ussd_sales_aggregation`;
CREATE TABLE `std_ussd_sales_aggregation` (
`id` varchar(255) NOT NULL DEFAULT '',
`aggregationDate` date NOT NULL,
`profileId` varchar(200) DEFAULT NULL,
`senderMSISDN` varchar(200) DEFAULT NULL,
`receiverMSISDN` varchar(200) DEFAULT NULL,
`quantity` int(11) DEFAULT NULL,
`amount` decimal(65,5) DEFAULT NULL,
`bonus_amount` decimal(65,5) DEFAULT NULL,
`currency` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `date_senderMSISDN` (`aggregationDate`,`senderMSISDN`),
KEY `date_receiverMSISDN` (`aggregationDate`,`receiverMSISDN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`report_list`;
CREATE TABLE `bi`.`report_list` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL DEFAULT '',
  `grouping` varchar(50) NOT NULL DEFAULT '',
  `query` varchar(5000) NOT NULL DEFAULT '',
  `data_source` varchar(20) NOT NULL DEFAULT '',
  `extra_field_1` varchar(1000) DEFAULT NULL,
  `extra_field_2` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`report_metadata`;
CREATE TABLE `bi`.`report_metadata`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT,
    `report_id`     varchar(50) NOT NULL DEFAULT '',
    `name`          varchar(50) NOT NULL DEFAULT '',
    `description`   varchar(255)          DEFAULT NULL,
    `type`          varchar(40)          DEFAULT NULL,
    `default_value` varchar(50)          DEFAULT NULL,
    `values`        varchar(5000)        DEFAULT NULL,
    `reg_ex`        varchar(100)         DEFAULT NULL,
    `extra_field_1` varchar(100)         DEFAULT NULL,
    `extra_field_2` varchar(100)         DEFAULT NULL,
    `is_editable`   boolean     NOT NULL DEFAULT true,
    `is_mandatory`  boolean     NOT NULL DEFAULT false,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`report_access_control`;
CREATE TABLE `bi`.`report_access_control` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type_role` varchar(100) NOT NULL DEFAULT '',
  `name` varchar(100) DEFAULT NULL,
  `report_list_ids` varchar(500) DEFAULT NULL,
  `dashboard_url_ids` varchar(500) DEFAULT NULL,
  `status` varchar(10) NOT NULL DEFAULT '',
  `settings` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`dashboard_url`;
CREATE TABLE `bi`.`dashboard_url` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `dashboard_url` VARCHAR(1000) NOT NULL,
  `status` VARCHAR(10) NOT NULL DEFAULT 'active',
  `tab_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`commission_pay_out_aggregator`;
CREATE TABLE `bi`.`commission_pay_out_aggregator` (
  `id` varchar(200) NOT NULL DEFAULT '',
  `reseller_name` varchar(200) DEFAULT NULL,
  `partner_name` varchar(200) DEFAULT NULL,
  `previous_banked_date` date DEFAULT NULL,
  `current_banked_date` date DEFAULT NULL,
  `sales_amount` decimal(20,5) DEFAULT NULL,
  `banked_amount` decimal(20,5) DEFAULT NULL,
  `pending_amount` decimal(20,5) DEFAULT NULL,
  `accumulated_commission` decimal(20,5) DEFAULT NULL,
  `receivable_commission` decimal(20,5) DEFAULT NULL,
  `withheld_commission` decimal(20,5) DEFAULT NULL,
  `banking_code` varchar(200) DEFAULT NULL,
  `reseller_id` varchar(200) DEFAULT NULL,
  `partner_id` varchar(200) DEFAULT NULL,
  `reseller_msisdn` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`subscribers_aggregation`;
CREATE TABLE `bi`.`subscribers_aggregation` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `subscriber_msisdn` varchar(50) DEFAULT NULL,
  `transaction_date` date DEFAULT NULL,
  `transaction_profile` varchar(50) DEFAULT NULL,
  `currency` varchar(50) DEFAULT NULL,
  `transaction_amount` decimal(20,5) DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`failed_voucher_generation_denom_day_wise`;
CREATE TABLE `bi`.`failed_voucher_generation_denom_day_wise` (
`id` varchar(255) NOT NULL,
`denomination` varchar(200) DEFAULT NULL,
`status` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
`createdDate` date DEFAULT NULL,
`userId` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`voucher_generation_denom_day_wise`;
CREATE TABLE `bi`.`voucher_generation_denom_day_wise` (
`id` varchar(255) NOT NULL,
`denomination` varchar(200) DEFAULT NULL,
`resellerId` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
`createdDate` date DEFAULT NULL,
`userId` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`voucher_expiry_denom_day_wise`;
CREATE TABLE `bi`.`voucher_expiry_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	expiryDate date,
	denomination varchar(50),
	quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`unredeemed_voucher_reseller_denom`;
CREATE TABLE `bi`.`unredeemed_voucher_reseller_denom` (
  `id` varchar(255) NOT NULL,
  `resellerId` varchar(200) DEFAULT NULL,
  `denomination` varchar(200) DEFAULT NULL,
  `totalUnredeemed` bigint(25) DEFAULT NULL,
  `totalstock` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`voucher_redemption_denom_day_wise`;
CREATE TABLE `bi`.`voucher_redemption_denom_day_wise`(
       id varchar(255) PRIMARY KEY NOT NULL,
       redemptionDate date,
       denomination varchar(50),
       quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`vouchers_stock`;
CREATE TABLE `bi`.`vouchers_stock` (
	`id` varchar(255) NOT NULL,
	`denomination` varchar(200) DEFAULT NULL,
	`totalstock` bigint(25) DEFAULT NULL,
	`totalRedeemed` bigint(25) DEFAULT NULL,
	`totalUnredeemed` bigint(25) DEFAULT NULL,
	`totalExpired` bigint(25) DEFAULT NULL,
	`totalRevoked` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`region_reseller_account_statement_daily_balance_aggregation`;
CREATE TABLE `bi`.`region_reseller_account_statement_daily_balance_aggregation` (
  `aggregationDate` date NOT NULL,
  `resellerId` varchar(50) NOT NULL,
  `accountTypeId` varchar(20) NOT NULL,
  `balanceBefore` decimal(65,5) DEFAULT NULL,
  `balanceAfter` decimal(65,5) DEFAULT NULL,
PRIMARY KEY (`aggregationDate`,`resellerId`,`accountTypeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`revoked_voucher_reseller_denom_day_wise`;
CREATE TABLE `bi`.`revoked_voucher_reseller_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	revocationDate date,
	denomination varchar(50),
	resellerId varchar(200),
	quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`unredeemed_voucher_age_denom_day_wise`;
CREATE TABLE `bi`.`unredeemed_voucher_age_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	age varchar(20) NOT NULL,
	denomination varchar(50) NOT NULL,
	totalUnredeemed bigint(25) NOT NULL,
	totalExpired bigint(25) NOT NULL,
	totalRevoked bigint(25) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_evoucher_sales_day_wise`;
CREATE TABLE `bi`.`reseller_evoucher_sales_day_wise` (
	`id` varchar(255) NOT NULL,
	`productId` varchar(200) DEFAULT NULL,
	`resellerId` varchar(200) DEFAULT NULL,
	`zone` varchar(80) DEFAULT NULL,
	`group1` varchar(80) DEFAULT NULL,
	`subGroup` varchar(80) DEFAULT NULL,
	`executionDate` date NOT NULL,
	`quantity` bigint(25) DEFAULT NULL,
	`amount` decimal(65,2) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`denomination_wise_sales_aggregation`;
CREATE TABLE `bi`.`denomination_wise_sales_aggregation` (
`id` varchar(255) NOT NULL,
`end_time_day` date DEFAULT NULL,
`end_time_hour` time DEFAULT NULL,
`channel` varchar(200) DEFAULT NULL,
`amount` decimal(20,5) DEFAULT 0,
`count` decimal(20,5) DEFAULT 0,
`denomination` decimal(20,5) DEFAULT 0,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`std_mobile_reseller_tran_stats_sales_aggregation`;
CREATE TABLE `bi`.`std_mobile_reseller_tran_stats_sales_aggregation` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `transaction_date` datetime NOT NULL,
  `msisdn` varchar(50) NOT NULL DEFAULT '',
  `profile` varchar(50) NOT NULL DEFAULT '',
  `reseller_id` varchar(50) NOT NULL DEFAULT '',
  `transaction_amount` double DEFAULT '0',
  `transaction_commission` decimal(65,5) DEFAULT '0.00000',
  `transaction_bonus` decimal(65,5) DEFAULT '0.00000',
  `transaction_count` int(11) DEFAULT '0',
  `reseller_account_type_id` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`transaction_date`,`msisdn`,`profile`,`reseller_id`,`reseller_account_type_id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1129 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`std_mobile_reseller_tran_stats_deposit_aggregation`;
CREATE TABLE `bi`.`std_mobile_reseller_tran_stats_deposit_aggregation` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `transaction_date` datetime NOT NULL,
  `msisdn` varchar(50) NOT NULL DEFAULT '',
  `profile` varchar(50) NOT NULL DEFAULT '',
  `reseller_id` varchar(50) NOT NULL DEFAULT '',
  `transaction_amount` double DEFAULT '0',
  `transaction_commission` decimal(65,5) DEFAULT '0.00000',
  `transaction_bonus` decimal(65,5) DEFAULT '0.00000',
  `transaction_count` int(11) DEFAULT '0',
  `reseller_account_type_id` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`transaction_date`,`msisdn`,`profile`,`reseller_id`,`reseller_account_type_id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`std_daily_transaction_summary_aggregation`;
CREATE TABLE `bi`.`std_daily_transaction_summary_aggregation` (
  `transactionDate` datetime NOT NULL,
  `transactionHour` int(2) DEFAULT NULL,
  `transactionReference` varchar(60) NOT NULL,
  `senderMSISDN` varchar(50) DEFAULT NULL,
  `senderResellerID` varchar(50) DEFAULT NULL,
  `receiverMSISDN` varchar(50) DEFAULT NULL,
  `displayReceiverMSISDN` varchar(50) DEFAULT NULL,
  `receiverResellerID` varchar(50) DEFAULT NULL,
  `transactionType` varchar(50) DEFAULT NULL,
  `amount` decimal(65,5) NOT NULL,
  `channel` varchar(50) NOT NULL,
  `resultStatus` varchar(50) NOT NULL,
  `externalID` varchar(50) DEFAULT NULL,
  `senderBalanceBefore` decimal(65,5) NOT NULL,
  `senderBalanceAfter` decimal(65,5) NOT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `resultDescription` varchar(50) NOT NULL,
  `receiverBalanceBefore` decimal(65,5) DEFAULT NULL,
  `receiverBalanceAfter` decimal(65,5) DEFAULT NULL,
  `resellerCommission` decimal(65,5) DEFAULT NULL,
  `resellerBonus` decimal(65,5) DEFAULT NULL,
  `receiverResellerCommission` decimal(65,5) DEFAULT NULL,
  `receiverResellerBonus` decimal(65,5) DEFAULT NULL,
  `senderResellerName` varchar(50) DEFAULT NULL,
  `receiverResellerName` varchar(50) DEFAULT NULL,
  `resellerParent` varchar(50) DEFAULT NULL,
  `region` VARCHAR(100) DEFAULT 'NO_REGION',
  `batchId` VARCHAR(45) DEFAULT NULL,
  `sender_reseller_account_type_id` varchar(50) DEFAULT NULL,
  `receiver_reseller_account_type_id` varchar(50) DEFAULT NULL,
  `voucher_serial` varchar(50) DEFAULT NULL,
  `sequential_number` SMALLINT(10) DEFAULT 0,
  PRIMARY KEY (`transactionReference`),
  INDEX std_daily_transaction_summary_aggregation_index (`transactionDate`, `transactionType`, `resultStatus`, `senderMSISDN`, `batchId`, `sender_reseller_account_type_id`, `receiver_reseller_account_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX region_index on std_daily_transaction_summary_aggregation(region);


CREATE OR REPLACE VIEW `bi`.`reseller_hierarchy_view` AS
    SELECT
        `detail_balance_report_aggregation`.`id` AS `id`,
        `detail_balance_report_aggregation`.`reseller_id` AS `reseller_id`,
        `detail_balance_report_aggregation`.`reseller_parent` AS `reseller_parent`,
        `detail_balance_report_aggregation`.`reseller_name` AS `reseller_name`,
        `detail_balance_report_aggregation`.`msisdn` AS `msisdn`,
        `detail_balance_report_aggregation`.`reseller_status` AS `status`,
        `detail_balance_report_aggregation`.`reseller_type` AS `reseller_type`,
        `detail_balance_report_aggregation`.`current_balance` AS `balance`,
        `detail_balance_report_aggregation`.`currency` AS `currency`,
        `detail_balance_report_aggregation`.`account_id` AS `account_Id`,
		`detail_balance_report_aggregation`.`account_type_id` AS `account_type_id`

    FROM
        `bi`.`detail_balance_report_aggregation`;

DROP TABLE IF EXISTS `bi`.`std_transaction_details_aggregation`;
CREATE TABLE `bi`.`std_transaction_details_aggregation`
    (
        `ers_reference`         VARCHAR(25) NOT NULL DEFAULT '',
        `date`                  date NOT NULL DEFAULT '0000-00-00',
        `reseller_id`           varchar(50)   DEFAULT NULL,
        `reseller_msisdn`       varchar(80)   DEFAULT NULL,
        `reseller_parent`       varchar(50)   DEFAULT NULL,
        `reseller_path`         varchar(200)  DEFAULT NULL,
        `receiver_msisdn`       varchar(80)   DEFAULT NULL,
        `transaction_type`      varchar(50)   DEFAULT NULL,
        `reseller_opening_balance`   decimal(65,5) DEFAULT '0.00000',
        `transaction_amount`         decimal(65,5) DEFAULT '0.00000',
        `reseller_closing_balance`   decimal(65,5) DEFAULT '0.00000',
        `reseller_name`                varchar(50) DEFAULT NULL,
        `receiver_opening_balance`   decimal(65,5) DEFAULT '0.00000',
        `receiver_closing_balance`   decimal(65,5) DEFAULT '0.00000',
        `end_time` datetime       DEFAULT '0000-00-00 00:00:00',
        `reseller_type_id`       varchar(80) DEFAULT NULL,
        `transaction_result` varchar(80) DEFAULT NULL,
        `region` varchar(45) DEFAULT 'NO_REGION',
        `channel` varchar(30),
        `status` varchar(30),
        `sender_reseller_account_type_id` varchar(50) DEFAULT NULL,
        `receiver_reseller_account_type_id` varchar(50) DEFAULT NULL,

        PRIMARY KEY (`ers_reference`)
    )
    ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`std_dealer_detail_aggregation`;
CREATE TABLE `bi`.`std_dealer_detail_aggregation` (
  `resellerId` VARCHAR(250) NOT NULL,
  `transactions_date` VARCHAR(45) NOT NULL,
  `resellerMSISDN` VARCHAR(45) NULL DEFAULT NULL,
  `ResellerName` VARCHAR(250) NULL DEFAULT NULL,
  `transferIn` decimal(65,5) NULL DEFAULT 0.00,
  `total_pinless_topups` decimal(65,5) NULL DEFAULT 0.00,
  `total_pin_topups` decimal(65,5) NULL DEFAULT 0.00,
  `current_balance` decimal(65,5) NULL DEFAULT 0.00,
  `reseller_path` VARCHAR(250) NULL DEFAULT NULL,
  `account_type_id` VARCHAR(45) NULL DEFAULT NULL,
  PRIMARY KEY (`resellerId`, `transactions_date`, `account_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE OR REPLACE VIEW `bi`.`std_dealer_detail_aggregation_view` AS
    SELECT
         `resellerId` AS `reseller_id`,
         `transactions_date` AS `transaction_date`,
         `resellerMSISDN` AS `msisdn`,
         `ResellerName` AS `reseller_name`,
         `transferIn` AS `transfer_in`,
         `total_pinless_topups` AS `total_pinless_topups`,
         `total_pin_topups` AS `total_pin_topups`,
         `current_balance` AS `balance`,
         `account_type_id` AS `account_type_id`
    FROM
        `bi`.`std_dealer_detail_aggregation`
    WHERE
        ( `transactions_date` BETWEEN (NOW() - INTERVAL (10 + 1) DAY) AND NOW())
    ORDER BY  `transactions_date` ASC;

DROP TABLE IF EXISTS `bi`.`total_kyc_sales`;
CREATE TABLE `bi`.`total_kyc_sales` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `end_time_day` date DEFAULT NULL,
    `posId` varchar(255) DEFAULT NULL,
    `posPath` varchar(255) DEFAULT NULL,
    `region` varchar(255) DEFAULT NULL,
    `weekNumber` int(4) DEFAULT NULL,
    `year` int(4) DEFAULT NULL,
    `simType` varchar(255) DEFAULT NULL,
    `brand` varchar(255) DEFAULT NULL,
    `brandCode` varchar(255) DEFAULT NULL,
    `brandPrefix` varchar(255) DEFAULT NULL,
    `parentDealerId` varchar(255) DEFAULT NULL,
    `count` bigint(25) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`report_category_mapping`;
CREATE TABLE `bi`.`report_category_mapping` (
    `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
    `category_name` varchar(100) NOT NULL DEFAULT '',
    `report_list_ids` varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_current_status`;
CREATE TABLE `bi`.`reseller_current_status`
(
    `id`                varchar(200) NOT NULL,
    `reseller_id`       varchar(200) NOT NULL DEFAULT '',
    `reseller_name`     varchar(200)          DEFAULT '',
    `reseller_path`     varchar(200)          DEFAULT '',
    `reseller_status`   varchar(200) NOT NULL DEFAULT '',
    `reseller_parent`   varchar(200)          DEFAULT '',
    `reseller_type_id`  varchar(200)          DEFAULT '',
    `region`            varchar(200)          DEFAULT NULL,
    `street`            varchar(200)          DEFAULT NULL,
    `suburb`            varchar(200)          DEFAULT NULL,
    `zip`               varchar(200)          DEFAULT NULL,
    `city`              varchar(200)          DEFAULT NULL,
    `country`           varchar(200)          DEFAULT NULL,
    `email`             varchar(200)          DEFAULT NULL,
    `MSISDN`            varchar(200)          DEFAULT NULL,
    `email_responsible` varchar(200)          DEFAULT NULL,
    `created_on`        date                  DEFAULT NULL,
    `created_by`        varchar(200)          DEFAULT NULL,
    `status_changed_on` date                  DEFAULT NULL,
    `status_changed_by` varchar(200)          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `bi`.`safaricom_dealer_information`;
CREATE TABLE `bi`.`safaricom_dealer_information`
(
    `id`                       varchar(200) NOT NULL,
    `reseller_id`              varchar(200) NOT NULL DEFAULT '',
    `dealer_hq_code`           varchar(200)          DEFAULT '',
    `dealer_hq_name`           varchar(200)          DEFAULT '',
    `dealer_branch_id`         varchar(200)          DEFAULT '',
    `dealer_branch_name`       varchar(200)          DEFAULT '',
    `dealer_code`              varchar(200)          DEFAULT '',
    `dealer_branch_manager_id` varchar(200)          DEFAULT NULL,
    `dealer_sales_area`        varchar(200)          DEFAULT NULL,
    `cluster`                  varchar(200)          DEFAULT NULL,
    `route`                    varchar(200)          DEFAULT NULL,
    `latitude`                 varchar(200)          DEFAULT NULL,
    `longitude`                varchar(200)          DEFAULT NULL,
    `channel_type`             varchar(200)          DEFAULT NULL,
    `no_of_bike`               varchar(100)          DEFAULT NULL,
    `no_of_van`                varchar(100)          DEFAULT NULL,
    `no_of_till`               varchar(100)          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

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
    `trip_id` varchar(200) DEFAULT NULL,
    `route_information` varchar(200) DEFAULT NULL,
    `operation_type` varchar(200) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`orders_quantity_aggregator`;
CREATE TABLE `bi`.`orders_quantity_aggregator`
(
    `id`                  varchar(200) NOT NULL,
    `transaction_date`    datetime              DEFAULT NULL,
    `transaction_number`  varchar(200)          DEFAULT NULL,
    `quantity`            varchar(200)          DEFAULT NULL,
    `product_code`        varchar(200)          DEFAULT NULL,
    `product_sku`         varchar(200) NOT NULL DEFAULT '',
    `product_description` varchar(200) NOT NULL DEFAULT '',
    `reseller_id`         varchar(200) NULL DEFAULT NULL,
    `order_type`          varchar(200) NOT NULL DEFAULT '',
    `buyer_id`            varchar(200) NULL DEFAULT NULL,
    `seller_id`           varchar(200) NULL DEFAULT NULL,
    `receiver_id`         varchar(200) NULL DEFAULT NULL,
    `sender_id`           varchar(200) NULL DEFAULT NULL,
    `drop_location_id`    varchar(200) NULL DEFAULT NULL,
    `pickup_location_id`  varchar(200) NULL DEFAULT NULL,
    `route_id`            varchar(200) NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`all_orders_aggregator`;
CREATE TABLE `bi`.`all_orders_aggregator` (
  `id` varchar(200) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `transaction_number` varchar(200) NOT NULL DEFAULT '',
  `invoice_value` bigint(20) DEFAULT NULL,
  `invoice_product_entry_value` bigint(20) DEFAULT NULL,
  `month_value` varchar(200) NOT NULL DEFAULT '',
  `quarter` varchar(200) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) NOT NULL DEFAULT '',
  `buyer_id` varchar(200) NOT NULL DEFAULT '',
  `seller_id` varchar(200) NOT NULL DEFAULT '',
  `receiver_id` varchar(200) NOT NULL DEFAULT '',
  `order_status` varchar(200) NOT NULL DEFAULT '',
  `invoice_id` varchar(200) NOT NULL DEFAULT '',
  `invoice_status` varchar(200) NOT NULL DEFAULT '',
  `order_id` varchar(200) NOT NULL DEFAULT '',
  `product_code` varchar(200) NOT NULL DEFAULT '',
  `product_id` varchar(200) NOT NULL DEFAULT '',
  `product_name` varchar(200) NOT NULL DEFAULT '',
  `product_description` varchar(200) NOT NULL DEFAULT '',
  `product_sku` varchar(200) NOT NULL DEFAULT '',
  `product_type` varchar(200) NOT NULL DEFAULT '',
  `category_path` varchar(200) NOT NULL DEFAULT '',
  `commission_type` varchar(200) NOT NULL DEFAULT '',
  `total_discount` decimal(20,2) DEFAULT NULL,
  `total_unit_price` decimal(20,2) DEFAULT NULL,
  `uom_quantity` bigint(20) DEFAULT NULL,
  `order_type` varchar(200) DEFAULT NULL,
  `reseller_type` varchar(200) NULL DEFAULT NULL,
  `drop_location_id` varchar(200) NULL DEFAULT NULL,
  `pickup_location_id` varchar(200) NULL DEFAULT NULL,
  `sender_id` varchar(200) NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`distributor_wise_weekly_sales_summary`;
CREATE TABLE `bi`.`distributor_wise_weekly_sales_summary` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `year` int(4) DEFAULT NULL,
    `weekNumber` int(4) DEFAULT NULL,
    `dist_id` varchar(255) DEFAULT NULL,
    `reseller_path` varchar(255) DEFAULT NULL,
    `region` varchar(255) DEFAULT NULL,
    `brand` varchar(255) DEFAULT NULL,
    `ttl_sales_count` bigint(25) DEFAULT 0,
    `activation_success_count` bigint(25) DEFAULT 0,
    `activation_rejection_count` bigint(25) DEFAULT 0,
    `automatic_validation_count` bigint(25) DEFAULT 0,
    `cutoff_count` bigint(25) DEFAULT 0,
    `mnp_sales_count` bigint(25) DEFAULT 0,
    `mnp_activation_success_count` bigint(25) DEFAULT 0,
    `mnp_activation_fail_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`hourly_cdr_usage_statistics_aggregation`;
CREATE TABLE `bi`.`hourly_cdr_usage_statistics_aggregation` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `hour` time DEFAULT NULL,
    `dealer_id` varchar(255) DEFAULT NULL,
    `dealer_type` varchar(255) DEFAULT NULL,
    `dealer_msisdn` varchar(255) DEFAULT NULL,
    `dealer_city` varchar(255) DEFAULT NULL,
    `dealer_district` varchar(255) DEFAULT NULL,
    `section` varchar(255) DEFAULT NULL,
    `cdr_usage_count` bigint(25) DEFAULT 0,
    `dealer_epos_terminal_id` varchar(255) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`dealer_stock_movement_aggregation`;
CREATE TABLE `bi`.`dealer_stock_movement_aggregation` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `seller_id` varchar(200) DEFAULT NULL,
     `buyer_id` varchar(200) DEFAULT NULL,
     `category` varchar(200) DEFAULT NULL,
     `sub_category` varchar(100) DEFAULT NULL,
     `product_type` varchar(100) DEFAULT NULL,
     `product_sku` varchar(200) DEFAULT NULL,
     `total_stocks_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_current_stock`;
CREATE TABLE `bi`.`reseller_current_stock`
(
    `id`                  varchar(100) NOT NULL DEFAULT '',
    `product`             varchar(100)          DEFAULT NULL,
    `reseller_id`         varchar(100)          DEFAULT NULL,
    `product_type`        varchar(100)          DEFAULT NULL,
    `status`              varchar(100)          DEFAULT NULL,
    `stock_count`         int(11) DEFAULT NULL,
    `brand`               varchar(100)          DEFAULT NULL,
    `distributor_id`      varchar(100)          DEFAULT NULL,
    `product_category`    varchar(100)          DEFAULT NULL,
    `product_subcategory` varchar(100)          DEFAULT NULL,
    `date`                date                  DEFAULT NULL,
    `weekNumber`          int(4) DEFAULT NULL,
    `year`                int(4) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

DROP TABLE IF EXISTS `bi`.`weekly_reseller_summary`;
CREATE TABLE `bi`.`weekly_reseller_summary` (
    `id` varchar(255),
    `week` int(11) DEFAULT NULL,
    `year` int(11) DEFAULT NULL,
    `distributor_id` varchar(200) DEFAULT NULL,
    `total_pos` int(11) DEFAULT NULL,
    `active_pos` int(11) DEFAULT NULL,
    `locked_pos` int(11) DEFAULT NULL,
    `reseller_path` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`. `total_kyc_sales_by_location`;
CREATE TABLE `bi`.`total_kyc_sales_by_location` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `reseller_id` varchar(200) DEFAULT NULL,
  `sim_type` varchar(200) DEFAULT NULL,
  `sim_brand` varchar(200) DEFAULT NULL,
  `latitude` varchar(200) DEFAULT NULL,
  `longitude` varchar(200) DEFAULT NULL,
  `reseller_path` varchar(200) DEFAULT NULL,
  `region` varchar(200) DEFAULT NULL,
  `total_sales` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`distributor_wise_daily_sales_summary`;
CREATE TABLE `bi`.`distributor_wise_daily_sales_summary` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `distributorId` varchar(255) DEFAULT NULL,
    `weekNumber` int(4) DEFAULT NULL,
    `year` int(4) DEFAULT NULL,
    `total` bigint(25) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`daily_total_sales_summary`;
CREATE TABLE `bi`.`daily_total_sales_summary` (
   `date` date DEFAULT NULL,
    `total_sales` bigint(25) DEFAULT NULL,
    PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`daily_active_reseller_summary`;
CREATE TABLE  `bi`.`daily_active_reseller_summary` (
   `id` varchar(255) NOT NULL DEFAULT '',
    `reseller_parent` varchar(200) DEFAULT NULL,
    `region` varchar(200) DEFAULT NULL,
    `total_reseller` int(11) DEFAULT NULL,
    `active_reseller` int(11) DEFAULT NULL,
     `email` varchar(100) DEFAULT NULL,
      `date` date DEFAULT NULL,
      `weekNumber` int(4) DEFAULT NULL,
      `year` int(4) DEFAULT NULL,
      `name` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_inventory_stock`;
CREATE TABLE `reseller_inventory_stock` (
  `id` varchar(100) NOT NULL DEFAULT '',
  `product_sku` varchar(100) DEFAULT NULL,
  `owner_id` varchar(100) DEFAULT NULL,
  `status` varchar(100) DEFAULT NULL,
  `stock_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `bi`.`total_transaction_summary`;
CREATE TABLE `bi`.`total_transaction_summary` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `dealer_type` varchar(200) DEFAULT NULL,
     `dealer_id` varchar(200) DEFAULT NULL,
     `dealer_msisdn` varchar(200) DEFAULT NULL,
     `transaction_type` varchar(200) DEFAULT NULL,
     `district` varchar(100) DEFAULT NULL,
     `area` varchar(100) DEFAULT NULL,
     `section` varchar(200) DEFAULT NULL,
     `city` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     `sum` decimal(20,5) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`transaction_statistics_aggregator`;
CREATE TABLE `bi`.`transaction_statistics_aggregator` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `channel` varchar(200) DEFAULT NULL,
     `account_type` varchar(200) DEFAULT NULL,
     `transaction_type` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     `sum` decimal(20,5) DEFAULT NULL,
     `currency` varchar(200) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`transaction_failure_aggregator`;
CREATE TABLE `bi`.`transaction_failure_aggregator` (
     `id` varchar(255) NOT NULL,
     `transaction_date` datetime DEFAULT NULL,
     `channel` varchar(200) DEFAULT NULL,
     `transaction_profile` varchar(200) DEFAULT NULL,
     `failure_cause` varchar(200) DEFAULT NULL,
     `transaction_count` bigint(100) DEFAULT NULL,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`buyer_wise_purchase_summary`;
CREATE TABLE `bi`.`buyer_wise_purchase_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`dealer_id` varchar(200) DEFAULT NULL,
`dealer_code` varchar(200) DEFAULT NULL,
`dealer_msisdn` varchar(200) DEFAULT NULL,
`dealer_name` varchar(200) DEFAULT NULL,
`seller_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`seller_wise_sales_summary`;
CREATE TABLE `bi`.`seller_wise_sales_summary` (
`id` varchar(255) NOT NULL,
`aggregation_date` date DEFAULT NULL,
`dealer_id` varchar(200) DEFAULT NULL,
`dealer_code` varchar(200) DEFAULT NULL,
`dealer_msisdn` varchar(200) DEFAULT NULL,
`dealer_name` varchar(200) DEFAULT NULL,
`buyer_reseller_type` varchar(200) DEFAULT NULL,
`transaction_type` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`hourly_usage_statistics`;
CREATE TABLE `bi`.`hourly_usage_statistics` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `date` date DEFAULT NULL,
    `hour` time DEFAULT NULL,
    `channel` varchar(255) DEFAULT NULL,
    `ttl_txn_count` bigint(25) DEFAULT 0,
	`successful_txn_count` bigint(25) DEFAULT 0,
    `failed_txn_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`lifecycle_transaction_aggregator`;
CREATE TABLE `bi`.`lifecycle_transaction_aggregator` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) DEFAULT NULL,
  `last_sender_balance_transaction` date DEFAULT NULL,
  `last_receiver_balance_transaction` date DEFAULT NULL,
  `last_sender_inventory_transaction` date DEFAULT NULL,
  `last_receiver_inventory_transaction` date DEFAULT NULL,
  `created_on` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`all_transaction_details`;
CREATE TABLE `bi`.`all_transaction_details`
(
    `id`                     varchar(255) NOT NULL DEFAULT '',
    `description`            varchar(200)          DEFAULT NULL,
    `seller_dealer_type`     varchar(200)          DEFAULT NULL,
    `seller_msisdn`          varchar(200)          DEFAULT NULL,
    `seller_terminal_id`     varchar(200)          DEFAULT NULL,
    `seller_id`              varchar(200)          DEFAULT NULL,
    `receivers_id`                 varchar(200)          DEFAULT NULL,
    `transaction_date`       datetime DEFAULT NULL,
    `buyer_reseller_type`    varchar(200) DEFAULT NULL,
    `buyer_msisdn`           varchar(200) DEFAULT NULL,
    `buyer_id`               varchar(200) DEFAULT NULL,
    `amount`                 varchar(200) DEFAULT NULL,
    `seller_opening_balance` varchar(200) DEFAULT NULL,
    `seller_closing_balance` varchar(200) DEFAULT NULL,
    `buyer_opening_balance`  varchar(200) DEFAULT NULL,
    `buyer_closing_balance`  varchar(200) DEFAULT NULL,
    `user_name`              varchar(200) DEFAULT NULL,
    `user_department`        varchar(200)          DEFAULT NULL,
    `payment_type`           varchar(200)          DEFAULT NULL,
    `transaction_reference`  varchar(200)          DEFAULT NULL,
    `source`                 varchar(200)          DEFAULT NULL,
    `seller_name`            varchar(200)          DEFAULT NULL,
    `area`                   varchar(200)          DEFAULT NULL,
    `section`                varchar(200)          DEFAULT NULL,
    `city_province`          varchar(200)          DEFAULT NULL,
    `district`               varchar(200)          DEFAULT NULL,
    `status`                 varchar(200)          DEFAULT NULL,
    `operation_type`                 varchar(200)          DEFAULT NULL,
    `csr_description`                 varchar(200)          DEFAULT NULL,
    `tag`                 varchar(200)          DEFAULT NULL,
    `productsku`                 varchar(200)          DEFAULT NULL,
    `reqSeq`                 varchar(200)          DEFAULT NULL,
    `rnn`                 varchar(200)          DEFAULT NULL,
    `senderAccountType`                 varchar(200)          DEFAULT NULL,
    `quantity`                  bigint(25)           DEFAULT NULL,
    `currency` varchar(200) DEFAULT NULL,
    `dealer_code` varchar(200) DEFAULT NULL,
    `auth_code` varchar(200) DEFAULT NULL,
    `transaction_status` varchar(200) DEFAULT NULL,
    `commission_group` varchar(200) DEFAULT NULL,
    `commission_rate` varchar(200) DEFAULT NULL,
    `commission_amount` varchar(200) DEFAULT NULL,
    `bill_id` varchar(200) DEFAULT NULL,
    `buyer_dealer_code` varchar(200) DEFAULT NULL,
    `branch_name` varchar(200) DEFAULT NULL,
    `order_id` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`total_monetary_transaction_summary`;
CREATE TABLE `bi`.`total_monetary_transaction_summary` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `dealer_type` varchar(200) DEFAULT NULL,
  `dealer_id` varchar(200) DEFAULT NULL,
  `dealer_msisdn` varchar(200) DEFAULT NULL,
  `dealer_epos_terminal_id` varchar(200) DEFAULT NULL,
  `transaction_type` varchar(200) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `section` varchar(200) DEFAULT NULL,
  `city` varchar(200) DEFAULT NULL,
  `productsku` varchar(200) DEFAULT NULL,
  `transaction_count` bigint(100) DEFAULT NULL,
  `sum` decimal(20,5) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`dealer_purchase_summary`;
CREATE TABLE `bi`.`dealer_purchase_summary` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `buyer_dealer_type` varchar(200) DEFAULT NULL,
  `buyer_dealer_id` varchar(200) DEFAULT NULL,
  `dealer_code` varchar(200) DEFAULT NULL,
  `buyer_dealer_msisdn` varchar(200) DEFAULT NULL,
  `buyer_dealer_status` varchar(200) DEFAULT NULL,
  `buyer_dealer_balance` varchar(200) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `section` varchar(200) DEFAULT NULL,
  `city` varchar(200) DEFAULT NULL,
  `seller_dealer_type` varchar(200) DEFAULT NULL,
  `purchase_frequency` bigint(100) DEFAULT NULL,
  `purchase_amount` decimal(20,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`survey_pos_monthly_average`;
CREATE TABLE `bi`.`survey_pos_monthly_average` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `posId` varchar(255) DEFAULT NULL,
  `posName` varchar(255) DEFAULT NULL,
  `channel` varchar(255) DEFAULT NULL,
  `month` int(11) DEFAULT NULL,
  `quarter` int(11) DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  `visibility` double(11,2) DEFAULT NULL,
  `availability` double(11,2) DEFAULT NULL,
  `advocacy` double(11,2) DEFAULT NULL,
  `count` int(11) DEFAULT NULL,
  `averageScore` double(11,2) DEFAULT NULL,
  `route` varchar(255) DEFAULT NULL,
  `area` varchar(255) DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `asmId` varchar(255) DEFAULT NULL,
  `rsmId` varchar(255) DEFAULT NULL,
  `tdrId` varchar(255) DEFAULT NULL,
  `tdrResellerPath` varchar(255) DEFAULT NULL,
  `hq` varchar(255) DEFAULT NULL,
  `branch` varchar(255) DEFAULT NULL,
  `hod` varchar(255) DEFAULT NULL,
  `director` varchar(255) DEFAULT NULL,
  `branchDesignation` varchar(255) DEFAULT NULL,
  `hqDesignation` varchar(255) DEFAULT NULL,
  `tdrDesignation` varchar(255) DEFAULT NULL,
  `rsmDesignation` varchar(255) DEFAULT NULL,
  `asmDesignation` varchar(255) DEFAULT NULL,
  `directorDesignation` varchar(255) DEFAULT NULL,
  `hodDesignation` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_registration`;
CREATE TABLE `bi`.`reseller_registration` (
  `id` varchar(100) NOT NULL,
  `parent_reseller_id` varchar(100) DEFAULT 'N/A',
  `reseller_id` varchar(100) DEFAULT 'N/A',
  `reseller_msisdn` varchar(100) DEFAULT 'N/A',
  `reseller_type` varchar(100) DEFAULT 'N/A',
  `region` varchar(100) DEFAULT 'N/A',
  `balance` varchar(100) DEFAULT '0',
  `registration_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`all_failure_transactions`;
CREATE TABLE `bi`.`all_failure_transactions`
(
    `id` varchar(255) NOT NULL DEFAULT '',
    `order_id` varchar(200) DEFAULT NULL,
    `transactionReference` varchar(200) DEFAULT NULL,
	`resultCode` varchar(200) DEFAULT NULL,
	`resultMesage` varchar(200) DEFAULT NULL,
	`channel` varchar(200) DEFAULT NULL,
    `user_name`  varchar(200) DEFAULT NULL,
    `clientComment` varchar(200) DEFAULT NULL,
	`transaction_date` datetime DEFAULT NULL,
    `sender_msisdn` varchar(200) DEFAULT NULL,
    `dealer_code` varchar(200) DEFAULT NULL,
    `seller_terminal_id`  varchar(200) DEFAULT NULL,
	`buyer_msisdn` varchar(200)  DEFAULT NULL,
	`productsku`  varchar(200)  DEFAULT NULL,
	`amount`  varchar(200) DEFAULT NULL,
	`commission_rate` varchar(200) DEFAULT NULL,
    `commission_amount` varchar(200) DEFAULT NULL,
	`auth_code` varchar(200) DEFAULT NULL,
	`user_id` varchar(200) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`all_trips_detail`;
CREATE TABLE `bi`.`all_trips_detail`
(
    `id`                 varchar(255) NOT NULL,
    `trip_date`          timestamp NULL DEFAULT NULL,
    `reseller_id`        varchar(200) DEFAULT NULL,
    `pos_id`             varchar(200) DEFAULT NULL,
    `transaction_number` varchar(200) DEFAULT NULL,
    `task_type`          varchar(200) DEFAULT NULL,
    `pos_status`         varchar(200) DEFAULT NULL,
    `warehouse_id` 		 varchar(200) DEFAULT NULL,
	`outlet_id` 		 varchar(200) DEFAULT NULL,
	`task_status` 		 varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`trip_outlet_visit`;
CREATE TABLE `bi`.`trip_outlet_visit`
(
    `id`                 varchar(255) NOT NULL,
    `trip_date`          timestamp NULL DEFAULT NULL,
    `reseller_id`        varchar(200) DEFAULT NULL,
    `total_outlets`      bigint(25) DEFAULT NULL,
    `completed_visit`    bigint(25) DEFAULT NULL,
    `pending_visit`      bigint(25) DEFAULT NULL,
    `dsa_strike_rate`    decimal(20,2) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`itemized_attachment_details`;
CREATE TABLE `bi`.`itemized_attachment_details`(
    `id` varchar(255) NOT NULL DEFAULT '',
    `dealer_hq_id` varchar(200) DEFAULT NULL,
    `item_code` varchar(200) DEFAULT NULL,
    `sim_sold` bigint(20) DEFAULT NULL,
    `attached_serial` varchar(200) DEFAULT NULL,
    `topup_amount` decimal(20,2) DEFAULT NULL,
    `sales_timestamp` datetime DEFAULT NULL,
    `activation_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`survey_feedback_all_scores`;
CREATE TABLE `bi`.`survey_feedback_all_scores` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `surveyDate` date NOT NULL,
  `resellerId` varchar(255) NOT NULL DEFAULT '',
  `agentId` varchar(255) NOT NULL DEFAULT '',
  `score` double DEFAULT NULL,
  `scorePercent` double NOT NULL,
  `taskType` varchar(255) DEFAULT NULL,
  `surveyorResellerPath` varchar(255) DEFAULT NULL,
  `agentResellerPath` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi`.`outlet_performance_report`;
CREATE TABLE `bi`.`outlet_performance_report`(
    `id` varchar(255) NOT NULL DEFAULT '',
    `reseller_id` varchar(100) DEFAULT NULL,
    `month` int(10) DEFAULT NULL,
    `year` int(10) DEFAULT NULL,
    `avg_mpesa_float` decimal(20,2) DEFAULT NULL,
    `avg_transaction_volume` decimal(20,2) DEFAULT NULL,
    `avg_transaction_value` decimal(20,2) DEFAULT NULL,
    `mpesa_float_level` decimal(20,2) DEFAULT NULL,
    `avg_stock_volume` decimal(20,2) DEFAULT NULL,
    `avg_stock_value` decimal(20,2) DEFAULT NULL,
    `devices_sold` bigint(20) DEFAULT NULL,
    `devices_attachment` bigint(20) DEFAULT NULL,
    `lines_ordered` bigint(20) DEFAULT NULL,
    `lines_attached` bigint(20) DEFAULT NULL,
    `open_issues` bigint(20) DEFAULT NULL,
    `closed_issues` bigint(20) DEFAULT NULL,
    `last_visit_comments` varchar(200) DEFAULT NULL,
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

DROP TABLE IF EXISTS `bi`.`reseller_current_status_additional_info`;
 CREATE TABLE `bi`.`reseller_current_status_additional_info` (
   `reseller_current_id` varchar(200) NOT NULL,
   `cnic` varchar(200) DEFAULT NULL,
   `msr_id` varchar(200) DEFAULT '',
   `msr_name` varchar(200) DEFAULT '',
   `msr_msisdn` varchar(200) DEFAULT '',
   `sfr_id` varchar(200) DEFAULT '',
   `sfr_name` varchar(200) DEFAULT '',
   `sfr_msisdn` varchar(200) DEFAULT '',
   `rso_id` varchar(200) DEFAULT NULL,
   `rso_name` varchar(200) DEFAULT '',
   `rso_msisdn` varchar(200) DEFAULT '',
   `birthday` date DEFAULT NULL,
   `postal_code` varchar(50) DEFAULT '',
   `contact_no` varchar(50) DEFAULT '',
   `batch_id` varchar(200) DEFAULT '',
   `circle` varchar(200) DEFAULT '',
   `max_daily_recharge_limit` varchar(200) DEFAULT '',
   `balance_threshold` varchar(50) DEFAULT '',
   `district` varchar(200) DEFAULT '',
   PRIMARY KEY (`reseller_current_id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`reseller_current_status_additional_info` ADD COLUMN `sales_area` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`reseller_current_status_additional_info` ADD COLUMN `latitude` varchar(50) DEFAULT NULL;
ALTER TABLE `bi`.`reseller_current_status_additional_info` ADD COLUMN `longitude` varchar(50) DEFAULT NULL;

DROP TABLE IF EXISTS `bi`.`std_daily_transaction_summary_aggregation_additional_details`;
CREATE TABLE `bi`.`std_daily_transaction_summary_aggregation_additional_details` (
    `transaction_end_date` datetime NOT NULL,
    `transaction_date` datetime NOT NULL,
    `transaction_reference` varchar(60) NOT NULL,
    `original_ers_reference` varchar(60) DEFAULT NULL,
    `commission_class` varchar(50) DEFAULT NULL,
    `opening_balance` decimal(65,5) NOT NULL DEFAULT 0.00000,
    `client_comment` varchar(500) DEFAULT NULL,
    `resource` varchar(50) DEFAULT NULL,
    `product_name` varchar(50) DEFAULT NULL,
    `transaction_profile` varchar(50) DEFAULT NULL,
    `result_code` varchar(50) DEFAULT NULL,
    `tax_rate` decimal(65,5) DEFAULT NULL,
    `tax` decimal(65,5) DEFAULT NULL,
    `account_valid_untill` datetime DEFAULT NULL,
    `expiry_after_recharge` datetime DEFAULT NULL,
    `expiry_before_recharge` datetime DEFAULT NULL,
    `sender_cell_id` varchar(50) default '',
    `receiver_cell_id` varchar(50) default '',
    PRIMARY KEY (`transaction_reference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`external_pos_stock_holding`;
CREATE TABLE `bi`.`external_pos_stock_holding` (
	`id` varchar(255) NOT NULL DEFAULT '',
	`stock_date` datetime DEFAULT NULL,
    `reseller_id` varchar(100) DEFAULT NULL,
    `reseller_type` varchar(50) DEFAULT NULL,
    `product_sku` varchar(100) DEFAULT NULL,
    `stock_qty_sold` bigint(20) DEFAULT NULL,
    `stock_qty_hand` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_account_statement_report`;
CREATE TABLE `bi`.`reseller_account_statement_report` (
	`id` varchar(255) NOT NULL DEFAULT '',
	`transaction_date` datetime DEFAULT NULL,
    `reseller_id` varchar(100) DEFAULT NULL,
    `opening_balance` bigint(20) DEFAULT NULL,
    `balance_transfer_in` bigint(20) DEFAULT NULL,
    `balance_transfer_out` bigint(20) DEFAULT NULL,
    `balance_in_hand` bigint(20) DEFAULT NULL,
    `currency` varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`operator_dealer_audits_logs`;
CREATE TABLE `bi`.`operator_dealer_audits_logs` (
   `transactionReference` varchar(60) NOT NULL,
   `transactionStartDate` datetime DEFAULT NULL,
   `transactionEndDate` datetime DEFAULT NULL,
   `senderResellerID` varchar(50) DEFAULT '',
   `receiverResellerID` varchar(50) DEFAULT '',
   `resellerStatus` varchar(50) DEFAULT '',
   `comments` varchar(100) DEFAULT '',
   `transactionType` varchar(50) DEFAULT '',
   `channel` varchar(50) DEFAULT '',
   `resultStatus` varchar(50) DEFAULT '',
   `resultDescription` varchar(50) DEFAULT '',
   PRIMARY KEY (`transactionReference`),
   KEY `operator_dealer_audits_logs_index` (`transactionStartDate`,`transactionEndDate`,`transactionType`,`resultStatus`,`senderResellerID`,`receiverResellerID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `sender_reseller_path` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `receiver_reseller_path` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `client_reference` VARCHAR(1000) DEFAULT NULL;

DROP TABLE IF EXISTS `bi`.`tpk_channel_transaction_mapping_master`;
CREATE TABLE `bi`.`tpk_channel_transaction_mapping_master` (
`ers_name` varchar(60) NOT NULL,
`tp_name` varchar(60) NOT NULL,
`tp_code` int not null,
`type` varchar(50) NOT NULL,
PRIMARY KEY (`ers_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Default values for above table */
INSERT INTO `bi`.`tpk_channel_transaction_mapping_master` (`ers_name`,`tp_name`, `tp_code`,`type`) VALUES
('API','WS', 7,'Channel')
,('BROADBAND','Subaccount Recharge', 93,'Transaction')
,('DATA_BUNDLE','Subaccount Recharge', 93,'Transaction')
,('EASY_CARD','Subaccount Recharge', 93,'Transaction')
,('GAMINGBOX','Subaccount Recharge', 93,'Transaction')
,('ILAQAYI_OFFER','Subaccount Recharge', 93,'Transaction')
,('Mobile-APP','WS', 7,'Channel')
,('POSTPAID_TOPUP','Normal Recharge', 1,'Transaction')
,('REVERSE_TOPUP','Recharge Rollback', 505,'Transaction')
,('SMS','SMS', 2,'Channel')
,('SPL_BUNDLE','Subaccount Recharge', 93,'Transaction')
,('TOPUP','Normal Recharge', 1,'Transaction')
,('USSD','USSD', 1,'Channel')
,('VOICE_OFFER','Subaccount Recharge', 93,'Transaction');

DROP TABLE IF EXISTS `bi`.`health_control_info`;
CREATE TABLE `bi`.`health_control_info` (
  `id` varchar(200) NOT NULL,
  `commission_name` varchar(200) NOT NULL DEFAULT '',
  `date_inserted` varchar(30) NOT NULL DEFAULT '',
  `commission_count` varchar(200) DEFAULT '',
  `commission_amount` varchar(200) DEFAULT '',
  `total_count` varchar(20) NOT NULL DEFAULT '',
  `total_amount` varchar(200) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`update_parent_reseller`;
CREATE TABLE `bi`.`update_parent_reseller` (
  `id` varchar(200) NOT NULL ,
  `transaction_reference` varchar(200) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) NOT NULL DEFAULT '',
  `parent_reseller_id` varchar(200) NOT NULL DEFAULT '',
  `result_code` varchar(30)  DEFAULT '',
  `result_message` varchar(300)  DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`tp_staff_monthly_payment`;
CREATE TABLE `bi`.`tp_staff_monthly_payment` (
  `id` varchar(200) NOT NULL ,
  `reseller_id` varchar(200) NOT NULL DEFAULT '',
  `reseller_name` varchar(200) NOT NULL DEFAULT '',
  `year` varchar(30) NOT NULL DEFAULT '',
  `month` varchar(30)  DEFAULT '',
  `start_date` varchar(200)  DEFAULT '',
  `end_date` varchar(200)  DEFAULT '',
  `totalNet_amount` varchar(300)  DEFAULT '',
  `lastUpdatedDate` varchar(300)  DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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


/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;