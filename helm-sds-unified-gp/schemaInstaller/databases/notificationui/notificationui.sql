START TRANSACTION;


CREATE
DATABASE IF NOT EXISTS notificationui;

USE notificationui;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ANSI';

-- -----------------------------------------------------
-- Table `languages`
-- This table contains all supported languages like en, ar, fr etc
-- -----------------------------------------------------

CREATE TABLE `languages` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(2) NOT NULL,
  `name` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_CODE` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------
-- Table `recipients`
-- This table contains all message recipients like sender, reciever, initiator etc
-- -----------------------------------------------------

CREATE TABLE `recipients` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `recipient_type` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_RECIPIENT_TYPE` (`recipient_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------
-- Table `transaction_categories`
-- This table contains all transaction types like TOPUP, TRANSFER, VOUCHER, RESELLER_MANAGEMENT etc
-- -----------------------------------------------------

CREATE TABLE `transaction_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_CATEGORY_ID` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------
-- Table `transaction_keywords`
-- This table contains all key words for every transaction like TopupAmount, SenderCommission, SenderMSISDN, ReceiverMSISDN etc
-- Based on transaction category and txe  transaction every keyword holds a freemarker expression
-- -----------------------------------------------------

CREATE TABLE `transaction_keywords` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) unsigned DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `template_value` varchar(256) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_NAME_CATEGORY_ID` (`name`,`category_id`),
  KEY `FK_CATEGORY_ID_TXN_CATEGORIES_ID` (`category_id`),
  CONSTRAINT `FK_CATEGORY_ID_TXN_CATEGORIES_ID` FOREIGN KEY (`category_id`) REFERENCES `transaction_categories` (`id`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------
-- Table `transaction_classifiers`
-- This table contains transaction's profiles TOPUP, BILL_PAYMENT, CREDIT_TRANSFER, CASHIN, CASHOUT etc
-- The transaction_profile holds a regexp like .*/Reseller/BILL_PAYMENT/REQUEST/Completed/resultCode=0 which we put in TXE
-- -----------------------------------------------------

CREATE TABLE `transaction_classifiers` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tag` varchar(100) DEFAULT NULL,
  `transaction_classifier` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_UNIQUE_TAG` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



-- -----------------------------------------------------
-- Table `transaction_messages`
-- This table contains notification messages with unique tag and freemarker expression template
-- -----------------------------------------------------

CREATE TABLE `transaction_messages` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `msg_tag` varchar(100) DEFAULT NULL,
  `msg_template` varchar(2048) DEFAULT NULL,
  `language_id` bigint(20) unsigned DEFAULT NULL,
  `recipient_id` bigint(20) unsigned DEFAULT NULL,
  `classifier_id` bigint(20) unsigned DEFAULT NULL,
  `status_id` bigint(20) DEFAULT NULL,
  `message_type_id` bigint(20) unsigned DEFAULT NULL,
  `message_template_type_id` bigint(20) unsigned DEFAULT NULL,
  `is_deletable` varchar(5)   NOT NULL DEFAULT 'true' CHECK (is_deletable IN ('true', 'false')),
  `activation_date` datetime DEFAULT NULL,
  `created_by` varchar(30) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(30) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `expression_value` varchar(500) DEFAULT NULL,
  `condition_expression` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_MSG_TAG` (`msg_tag`,`message_type_id`),
  KEY `FK_LANGUAGE_ID_TO_LANGUAGES_ID` (`language_id`),
  KEY `FK_RECIPIENT_ID_TO_RECIPIENTS_ID` (`recipient_id`),
  KEY `FK_STATUS_ID_TO_STATUS_ID` (`status_id`),
  KEY `FK_CLASSIFIER_ID_TO_TXN_CLASSIFIERS_ID` (`classifier_id`),
  KEY `FK_MESSAGE_TYPE_ID_TO_MESSAGE_TYPE_ID` (`message_type_id`),
  KEY `FK_MESSAGE_TEMPLATE_TYPE_ID_TO_MESSAGE_TEMPLATE_TYPE_ID` (`message_template_type_id`),
  CONSTRAINT `FK_RECIPIENT_ID_TO_RECIPIENTS_ID` FOREIGN KEY (`recipient_id`) REFERENCES `recipients` (`id`),
  CONSTRAINT `FK_LANGUAGE_ID_TO_LANGUAGES_ID` FOREIGN KEY (`language_id`) REFERENCES `languages` (`id`),
  CONSTRAINT `FK_STATUS_ID_TO_STATUS_ID` FOREIGN KEY (`status_id`) REFERENCES `status` (`id`),
  CONSTRAINT `FK_CLASSIFIER_ID_TO_TXN_CLASSIFIERS_ID` FOREIGN KEY (`classifier_id`) REFERENCES `transaction_classifiers` (`id`),
  CONSTRAINT `FK_MESSAGE_TYPE_ID_TO_MESSAGE_TYPES_ID` FOREIGN KEY (`message_type_id`) REFERENCES `message_types` (`id`),
  CONSTRAINT `FK_MESSAGE_TEMPLATE_TYPE_ID_TO_MESSAGE_TEMPLATE_TYPE_ID` FOREIGN KEY (`message_template_type_id`) REFERENCES `message_template_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_STATUS` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `message_types` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `message_type` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_MESSAGE_TYPE` (`message_type`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
  
CREATE TABLE `message_template_types` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `message_template_type` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_MESSAGE_TEMPLATE_TYPE` (`message_template_type`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `conditions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag` varchar(255) NOT NULL,
  `condition_expression` varchar(1024) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_CONDITION_TAG` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- DEFAULT DATA FOR LANGUAGES AND RECIPIENTS --

insert into languages (`code`,`name`) values ('en','English');
insert into languages (`code`,`name`) values ('fr','French');
insert into languages (`code`,`name`) values ('ar','Arabic');

insert into recipients (`recipient_type`) values ('Initiator');
insert into recipients (`recipient_type`) values ('Sender');
insert into recipients (`recipient_type`) values ('Receiver');
insert into recipients (`recipient_type`) values ('Expression');

insert into status (`status`) values ('Active');
insert into status (`status`) values ('Blocked');
insert into status (`status`) values ('Deactivated');

insert into message_types (`message_type`) values ('SMS');
insert into message_types (`message_type`) values ('EMAIL');
insert into message_types (`message_type`) values ('MOBILE_PUSH');
insert into message_types (`message_type`) values ('WEB');

insert into message_template_types (`message_template_type`) values ('DEFAULT');
insert into message_template_types (`message_template_type`) values ('CUSTOM');


SET FOREIGN_KEY_CHECKS = 1;

COMMIT;


