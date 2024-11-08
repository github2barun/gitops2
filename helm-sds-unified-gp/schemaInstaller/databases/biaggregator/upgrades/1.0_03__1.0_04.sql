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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
  `senderResellerName` varchar(50) DEFAULT NULL,
  `receiverResellerName` varchar(50) DEFAULT NULL,
  `resellerParent` varchar(50) DEFAULT NULL,
  `region` VARCHAR(100) DEFAULT 'NO_REGION',
  `batchId` VARCHAR(45) DEFAULT NULL,
  `sender_reseller_account_type_id` varchar(50) DEFAULT NULL,
  `receiver_reseller_account_type_id` varchar(50) DEFAULT NULL,
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


