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

DROP TABLE IF EXISTS `bi`.`all_trips_detail`;
CREATE TABLE `bi`.`all_trips_detail`
(
    `id`          varchar(255) NOT NULL,
    `trip_date`   timestamp NULL DEFAULT NULL,
    `reseller_id` varchar(200) DEFAULT NULL,
    `pos_id`      varchar(200) DEFAULT NULL,
    `outlet_name` varchar(200) DEFAULT NULL,
    `task_type`   varchar(200) DEFAULT NULL,
    `task_status` varchar(200) DEFAULT NULL,
    `channel`     varchar(200) DEFAULT NULL,
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

DROP TABLE IF EXISTS `bi`.`trip_outlet_visit`;
CREATE TABLE `bi`.`trip_outlet_visit`
(
    `id`                 varchar(255) NOT NULL,
    `trip_date`          timestamp NULL DEFAULT NULL,
    `reseller_id`        varchar(200) DEFAULT NULL,
    `total_outlets`      bigint(25) DEFAULT NULL,
    `completed_visit`    bigint(25) DEFAULT NULL,
    `pending_visit`      bigint(25) DEFAULT NULL,
    `dsa_strike_rate`    bigint(25) DEFAULT NULL,
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

ALTER TABLE `bi`.`all_transaction_details` MODIFY COLUMN `amount` varchar(255);
ALTER TABLE `bi`.`all_transaction_details` MODIFY COLUMN `seller_opening_balance` varchar(255);
ALTER TABLE `bi`.`all_transaction_details` MODIFY COLUMN `seller_closing_balance` varchar(255);
ALTER TABLE `bi`.`all_transaction_details` MODIFY COLUMN `buyer_opening_balance` varchar(255);
ALTER TABLE `bi`.`all_transaction_details` MODIFY COLUMN `buyer_closing_balance` varchar(255);

ALTER TABLE `bi`.`safaricom_dealer_information` ADD COLUMN `cluster` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`safaricom_dealer_information` ADD COLUMN `route` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`safaricom_dealer_information` ADD COLUMN `latitude` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`safaricom_dealer_information` ADD COLUMN `longitude` varchar(200) DEFAULT NULL;

ALTER TABLE `bi`.`all_orders_aggregator` ADD COLUMN `order_type` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`all_orders_aggregator` ADD COLUMN `reseller_type` varchar(200) DEFAULT NULL;
ALTER TABLE `bi`.`all_orders_aggregator` ADD COLUMN `drop_location_id` varchar(200) DEFAULT NULL;

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
