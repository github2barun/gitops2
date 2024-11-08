START TRANSACTION;

CREATE DATABASE IF NOT EXISTS accountmanagement;

USE accountmanagement;

SET FOREIGN_KEY_CHECKS = 0;


DROP TABLE IF EXISTS `accounts_expiry_info`;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
        `accountTypeId` varchar(20) NOT NULL,
        `accountId` varchar(80) NOT NULL,
        `owner` varchar(80) DEFAULT NULL,
        `masterOwner` VARCHAR(45) NULL,
        `accountName` varchar(30) DEFAULT NULL,
        `currency` varchar(4) DEFAULT NULL,
        `balance` decimal(20,5) DEFAULT NULL,
        `creditLimit` decimal(20,5) DEFAULT '0.00000',
        `version` int(11) NOT NULL DEFAULT '0',
        `status` varchar(20) NOT NULL DEFAULT 'Active',
        `password` varchar(64) DEFAULT NULL,
        `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `expiryDate` timestamp NOT NULL DEFAULT '1970-12-31 18:30:01',
        `extraFields` text COMMENT 'extra fields with key and element pairs in ISO 8859-1 charcater encoding',
        `loanEnable` varchar(45) DEFAULT NULL,
        `loanAmount` decimal(20,5) DEFAULT NULL,
        `loanPenaltyAmount` decimal(20,5) default '0.00000' ,
        `minAccountBalance` decimal(20,5) default 0.00000 null,
        `maxAccountBalance` decimal(20,5) default 0.00000 null,
        `minTransactionAmt` decimal(20,5) default 0.00000 null,
        `maxTransactionAmt` decimal(20,5) default 0.00000 null,
        `payLimit` decimal(20,5) default 0.00000 null,
        `availableAmount` decimal(20,5) default 0.00000 null,
        `reservedAmount` decimal(20,5) default 0.00000 null,
        `periodicCreditLimit` decimal(20,5) default 0.00000 null,
        `transactionsLimitCount` int(11) NOT NULL DEFAULT '0',
        `payLimitPeriod` int(11) NOT NULL DEFAULT '0',
        `accountValidFrom` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `description` text null,
        `loanDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`accountTypeId`,`accountId`),
        KEY `accountId_index` (`accountId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `counter`;
CREATE TABLE `counter` (
       `count` int(11) DEFAULT '0',
       `id` int(11) NOT NULL AUTO_INCREMENT,
       PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `counter` (`count`) VALUES (0);


DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
        `transactionKey` int(11) NOT NULL AUTO_INCREMENT,
        `accountTypeId` varchar(20) NOT NULL,
        `accountId` varchar(20) NOT NULL,
        `currency` varchar(4) DEFAULT NULL,
        `amount` decimal(20,5) NOT NULL,
        `balanceBefore` decimal(20,5) DEFAULT NULL,
        `balanceAfter` decimal(20,5) DEFAULT NULL,
        `importedReference` varchar(30) NOT NULL,
        `comment` varchar(80) DEFAULT NULL,
        `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`transactionKey`),
        KEY `importedReference_idx` (`importedReference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `accounts_expiry_info`;
CREATE TABLE `accounts_expiry_info` (
        `accountId` varchar(80) NOT NULL,
        `infinite_expiry` int(10) NOT NULL,
        `expirydate` datetime DEFAULT '1971-01-01 00:00:01',
        PRIMARY KEY (`accountId`),
        CONSTRAINT `accounts_expiry_info_ibfk_1` FOREIGN KEY (`accountId`) REFERENCES `accounts` (`accountId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS accounttypes;
CREATE TABLE accounttypes
(
    accountTypeId varchar(20) not null primary key,
    createDate datetime default current_timestamp() not null,
    lastModifiedDate datetime default current_timestamp() not null,
    expiryDate datetime default current_timestamp() not null,
    minAccountBalance decimal(20,5) default 0.00000 null,
    maxAccountBalance decimal(20,5) default 0.00000 null,
    minTransactionAmt decimal(20,5) default 0.00000 null,
    maxTransactionAmt decimal(20,5) default 0.00000 null,
    creditLimit decimal(20,5) default 0.00000 null,
    payLimit decimal(20,5) default 0.00000 null,
    countLimit decimal(20,5) default 0.00000 null,
    `currency` varchar(4) DEFAULT NULL,
    `url` TEXT NULL,
    accountSharingPolicy int,
    description text null,
    extraFields text null
)ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS account_sharing_policy;
CREATE TABLE account_sharing_policy (
      policy_key int NOT NULL AUTO_INCREMENT,
      name varchar(100) NOT NULL,
      value int NOT NULL UNIQUE,
      description varchar(1000) DEFAULT NULL,
      createDate datetime default current_timestamp() not null,
      lastModifiedDate datetime default current_timestamp() not null,
      PRIMARY KEY (policy_key),
      KEY policy_key_index (policy_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS ersinstall;
CREATE TABLE `ersinstall` (
      `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
      `Version` varchar(20) NOT NULL,
      `Status` tinyint(4) NOT NULL DEFAULT '0',
      `Script` varchar(200) NOT NULL,
      `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `deferred_transactions`;
create table deferred_transactions(
    id bigint NOT NULL AUTO_INCREMENT,
    scheduled_date date NOT NULL COMMENT 'UTC date of the date the import will be processed',
    status varchar(100) NOT NULL COMMENT 'The state' CHECK (status in ('pending','processing','processed','cancelled','failed')),
    data longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`data`)),
    system_token varchar(1000) NOT NULL,
    PRIMARY KEY (id)
);


SET FOREIGN_KEY_CHECKS = 1;

COMMIT;
