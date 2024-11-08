DROP TABLE IF EXISTS `ersinstall`;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`jobs`;
CREATE TABLE `alertapp`.`jobs` (
  `job_key` varchar(132) NOT NULL,
  `reseller_msisdn` varchar(32) NOT NULL,
  `job_type` varchar(32) NOT NULL,
  `reseller_id` VARCHAR(200) NULL,
  `reseller_name` VARCHAR(200) NULL,
  `account_id` VARCHAR(200) NULL,
  `reseller_type` VARCHAR(200) NULL,
  `job_creation_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `alerts_limit` int(11) DEFAULT NULL,
  `sent_alerts` int(11) DEFAULT NULL,
  `is_valid` varchar(20) DEFAULT NULL,
  `last_alert_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `balance_threshold` DECIMAL(20,5) NULL DEFAULT 0,
  PRIMARY KEY (`job_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`jobs_history`;
CREATE TABLE `alertapp`.`jobs_history` (
  `job_key` varchar(132) NOT NULL,
  `job_type` varchar(32) NOT NULL,
  `reseller_msisdn` varchar(32) NOT NULL,
  `reseller_id` VARCHAR(200) NULL,
  `reseller_name` VARCHAR(200) NULL,
  `account_id` VARCHAR(200) NULL,
  `reseller_type` VARCHAR(200) NULL,
  `balance_threshold` DECIMAL(20,5) NULL DEFAULT 0,
  `alerts_limit` int(11) DEFAULT NULL,
  `sent_alerts` int(11) DEFAULT NULL,
  `job_creation_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`job_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`owner_resource_thresholds`;
CREATE TABLE IF NOT EXISTS `alertapp`.`owner_resource_thresholds` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `resource_owner_id` VARCHAR(60),
    `resource_owner_type` VARCHAR(60) NOT NULL,
    `resource_type` VARCHAR(255) NOT NULL,
    `resource_id` VARCHAR(255),
    `threshold` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT owner_resource_threshold_unq UNIQUE (resource_owner_id,resource_owner_type,resource_type,resource_id),
    INDEX `rsrc_own_id_rsrc_type_rsrc_id_idx` (`resource_owner_id`, `resource_owner_type`, `resource_type`, `resource_id`)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`low_stock_reseller_params`;
DROP TABLE IF EXISTS `alertapp`.`low_stock_level_conf`;
DROP TABLE IF EXISTS `alertapp`.`low_stock_conf`;

CREATE TABLE IF NOT EXISTS `alertapp`.`low_stock_conf` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `reseller_type_id` VARCHAR(60) NOT NULL,
    `reseller_account_type_id` VARCHAR(60) NOT NULL,
    `group_name` VARCHAR(60) NOT NULL,
    `comparison_type` SMALLINT NOT NULL,
    `enabled` bool DEFAULT false,
    PRIMARY KEY (`id`),
    CONSTRAINT low_stock_conf_unq UNIQUE (reseller_type_id,reseller_account_type_id,group_name)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `alertapp`.`low_stock_reseller_params` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `low_stock_conf_id` INT NOT NULL,
    `param_type` SMALLINT NOT NULL,
    `reseller_id` VARCHAR(100) NULL,
    `param_id` VARCHAR(60) NULL,
    `param_value` VARCHAR(200) NULL,
    PRIMARY KEY (`id`),
    constraint fk_low_stock_conf_low_stock_reseller_params foreign key (low_stock_conf_id) references low_stock_conf(id) on delete cascade,
    CONSTRAINT low_stock_reseller_params_unq UNIQUE (`low_stock_conf_id`,`reseller_id`,`param_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `alertapp`.`low_stock_level_conf` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `low_stock_conf_id` INT NOT NULL,
    `sequence` SMALLINT NOT NULL DEFAULT 0,
    `alert_action_type` SMALLINT NOT NULL,
    `sales_in_days` SMALLINT DEFAULT 0,
    `threshold_value` DECIMAL(10,2) DEFAULT 0,
    `notify_parent` BOOLEAN DEFAULT false,
    `more_sms_recipient` TEXT,
    `more_email_recipient` TEXT,
    `message` VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`id`),
    constraint fk_low_stock_conf_low_stock_level_conf foreign key(low_stock_conf_id) references low_stock_conf(id) on delete cascade
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`send_alert_queue`;
CREATE TABLE IF NOT EXISTS `alertapp`.`send_alert_queue` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `alert_type` VARCHAR(80) NOT NULL,
    `reseller_id` VARCHAR(60) NOT NULL,
    `reseller_type_id` VARCHAR(60) NOT NULL,
    `account_type_id` VARCHAR(60) NOT NULL,
    `processed` BOOLEAN DEFAULT false,
    `created_on` DATE,
    PRIMARY KEY (`id`)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE TRIGGER jobs_after_insert
AFTER INSERT
  ON jobs FOR EACH ROW

BEGIN

  -- Insert record into audit table
  REPLACE INTO jobs_history
  ( job_key,job_type,reseller_msisdn, 
  	reseller_id,reseller_name,account_id,
    reseller_type,balance_threshold,alerts_limit,sent_alerts)
  VALUES
  ( NEW.job_key,
    NEW.job_type,
     NEW.reseller_msisdn,
     NEW.reseller_id,
     NEW.reseller_name,
     NEW.account_id,
     NEW.reseller_type,
     NEW.balance_threshold,
     NEW.alerts_limit,
     NEW.sent_alerts );
END$$
DELIMITER ;


