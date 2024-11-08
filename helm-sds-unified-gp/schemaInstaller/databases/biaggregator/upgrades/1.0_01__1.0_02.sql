ALTER TABLE `bi`.`report_access_control` ADD COLUMN `dashboard_url_ids` VARCHAR(500);

CREATE TABLE `dashboard_url` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `dashboard_url` VARCHAR(1000) NOT NULL,
  `status` VARCHAR(10) NOT NULL DEFAULT 'active',
  `tab_name` VARCHAR(100) NOT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `bi`.`std_sales_trend_aggregation` ADD COLUMN `reseller_commission` double DEFAULT NULL;

CREATE TABLE `region_reseller_account_statement_daily_balance_aggregation` (
  `aggregationDate` date NOT NULL,
  `resellerId` varchar(50) NOT NULL,
  `accountTypeId` varchar(20) NOT NULL,
  `balanceBefore` decimal(65,5) DEFAULT NULL,
  `balanceAfter` decimal(65,5) DEFAULT NULL,
PRIMARY KEY (`aggregationDate`,`resellerId`,`accountTypeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`commission_pay_out_aggregator` ADD COLUMN `reseller_msisdn` VARCHAR(50) DEFAULT NULL;

ALTER TABLE `bi`.`report_access_control` ADD COLUMN `settings` tinyint(1) DEFAULT NULL;

CREATE TABLE `failed_voucher_generation_denom_day_wise` (
  `id` varchar(255) NOT NULL,
  `denomination` varchar(200) DEFAULT NULL,
  `status` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
`createdDate` date DEFAULT NULL,
`userId` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `voucher_generation_denom_day_wise` (
  `id` varchar(255) NOT NULL,
  `denomination` varchar(200) DEFAULT NULL,
  `resellerId` varchar(200) DEFAULT NULL,
`quantity` bigint(25) DEFAULT NULL,
`createdDate` date DEFAULT NULL,
`userId` varchar(200) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `voucher_expiry_denom_day_wise`;
CREATE TABLE `voucher_expiry_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	expiry_time_day date,
	denomination varchar(50),
	quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `unredeemed_voucher_reseller_denom`;
CREATE TABLE `unredeemed_voucher_reseller_denom` (
  `id` varchar(255) NOT NULL,
  `resellerId` varchar(200) DEFAULT NULL,
  `denomination` varchar(200) DEFAULT NULL,
  `totalUnredeemed` bigint(25) DEFAULT NULL,
  `totalstock` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `voucher_redemption_denom_day_wise`;
CREATE TABLE `voucher_redemption_denom_day_wise`(
       id varchar(255) PRIMARY KEY NOT NULL,
       redemptionDate date,
       denomination varchar(50),
       quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `vouchers_stock`;
CREATE TABLE `vouchers_stock` (
	`id` varchar(255) NOT NULL,
	`denomination` varchar(200) DEFAULT NULL,
	`totalstock` bigint(25) DEFAULT NULL,
	`totalRedeemed` bigint(25) DEFAULT NULL,
	`totalUnredeemed` bigint(25) DEFAULT NULL,
	`totalExpired` bigint(25) DEFAULT NULL,
	`totalRevoked` bigint(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `revoked_voucher_reseller_denom_day_wise`;
CREATE TABLE `revoked_voucher_reseller_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	revocationDate date,
	denomination varchar(50),
	resellerId varchar(200),
	quantity bigint(25)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `unredeemed_voucher_age_denom_day_wise`;
CREATE TABLE `unredeemed_voucher_age_denom_day_wise`(
	id varchar(255) PRIMARY KEY NOT NULL,
	age varchar(20) NOT NULL,
	denomination varchar(50) NOT NULL,
	totalUnredeemed bigint(25) NOT NULL,
	totalExpired bigint(25) NOT NULL,
	totalRevoked bigint(25) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `reseller_evoucher_sales_day_wise`;
CREATE TABLE `reseller_evoucher_sales_day_wise` (
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

