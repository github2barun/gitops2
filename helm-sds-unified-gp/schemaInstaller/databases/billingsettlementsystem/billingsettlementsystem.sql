START TRANSACTION;

CREATE DATABASE IF NOT EXISTS billingsettlementsystem;

USE billingsettlementsystem;
SET FOREIGN_KEY_CHECKS = 0;
-- ------------------------------------
--  Table structure for `ersinstall`
-- ------------------------------------
DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall`
(
    `VersionKey`    smallint(6)  NOT NULL AUTO_INCREMENT,
    `Version`       varchar(20)  NOT NULL,
    `Status`        tinyint(4)   NOT NULL DEFAULT '0',
    `Script`        varchar(200) NOT NULL,
    `last_modified` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`VersionKey`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
    COMMENT 'System versioning';

DROP TABLE IF EXISTS `trip`;
DROP TABLE IF EXISTS `invoice`;

-- Create syntax for TABLE 'invoice'
CREATE TABLE `invoice`
(
    `invoice_id`         BIGINT                               NOT NULL AUTO_INCREMENT COMMENT 'The invoice id',
    `total_trips`        int(11)                              NOT NULL COMMENT 'The total number of trips for which the invoice was generated',
    `from_date`          datetime                             NOT NULL COMMENT 'Date for the farthest trip involved in the invoice',
    `to_date`            datetime                             NOT NULL COMMENT 'Date for the nearest trip involved in the invoice',
    `total_fare`         double                               NOT NULL COMMENT 'The total fare of trips for which the invoice was generated',
    `paid_amount`        double COMMENT 'The amount paid at this time',
    `status`             varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The status of the invoice (pending, partially resolved, resolved)',
    `created_date`       datetime                             NOT NULL COMMENT 'The creation date of the invoice',
    `last_modified_date` datetime                             NOT NULL COMMENT 'The date when the invoice was last modified',
    INDEX invoice_from_date_idx (from_date ASC),
    INDEX invoice_to_date_idx (from_date ASC),
    INDEX invoice_status_idx (status ASC),
    INDEX invoice_created_date_idx (created_date ASC),
    PRIMARY KEY (`invoice_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci
    COMMENT 'Invoice table';

-- Create syntax for TABLE 'trip'
CREATE TABLE `trip`
(
    `trip_id`                varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The trip id',
    `agent_id`               varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The assgined agent for the trip',
    `pickup_location`        varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The pickup location',
    `drop_location`          varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The drop location',
    `date`                   datetime                             NOT NULL COMMENT 'The date of the trip',
    `trip_fare`              double                               NOT NULL COMMENT 'The trip fare',
    `invoice_id`             bigint(20) COMMENT 'The invoice by which this trip will be paid',
    `vendor`                 varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The vendor who took care of the trip',
    `reseller`               varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The reseller who intiated the trip',
    `logistic_type`          varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The logistic type',
    `self_load`               varchar(2) COLLATE utf8_unicode_ci   NOT NULL COMMENT 'The selfload',
    `priority`               varchar(25) COLLATE utf8_unicode_ci  NOT NULL COMMENT 'The priority of the trip',
    `total_orders_processed` int(11)                              NOT NULL COMMENT 'The total order processed',
    `total_trip_capacity`    double                               NOT NULL COMMENT 'The total trip capacity',
    `billing_status`         varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The billing status (pending, requested, partially closed, closed)',
    `created_date`           datetime                             NOT NULL COMMENT 'The creation date of the trip in the billing and settlement system environment',
    `last_modified_date`     datetime                             NOT NULL COMMENT 'The date when the trip was last modified',
    PRIMARY KEY (`trip_id`),
    INDEX trip_agent_id_idx (agent_id ASC),
    INDEX trip_date_idx (date ASC),
    INDEX trip_invoice_id_idx (invoice_id ASC),
    INDEX trip_billing_status_idx (billing_status ASC),
    INDEX trip_created_date_idx (created_date ASC),
    CONSTRAINT `invoice_id` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`) ON
        DELETE RESTRICT ON
        UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci
    COMMENT 'Trip table';

DROP TABLE IF EXISTS `automated_billing_configuration`; 

-- Create syntax for TABLE 'automated_billing_configuration'
CREATE TABLE `automated_billing_configuration`
(
    `vendor`                varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The vendor name',
    `reseller`              varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The reseller uid',
    `mode`               	varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The invoice mode (manual or automatic)',
    `period`       			varchar(255) COLLATE utf8_unicode_ci 	      COMMENT 'The invoice period (daily, weekly, monthly, quarterly, yearly)',
    `initiation_date`       datetime                                      COMMENT 'The initiation date',
    `cycle_start_date`      datetime							          COMMENT 'The billing cycle start date',
    `cycle_end_date`       	datetime                                      COMMENT 'The billing cycle end date',
    `settlement_agreement`  varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The settlement agreement mode (internal or external)',
    `settlement_mode`       varchar(255) COLLATE utf8_unicode_ci 		  COMMENT 'The settlement mode',
    PRIMARY KEY (`vendor`,`reseller`), 
    INDEX automated_billing_configuration_vendor_idx (vendor ASC),
    INDEX automated_billing_configuration_reseller_idx (reseller ASC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci
    COMMENT 'Automated billing configuration';
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;