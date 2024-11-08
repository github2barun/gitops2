CREATE TABLE `alertapp`.`jobs` (
  `job_key` varchar(132) NOT NULL,
  `reseller_msisdn` varchar(32) NOT NULL,
  `job_type` varchar(32) NOT NULL,
  `reseller_id` VARCHAR(200) NULL,
  `reseller_name` VARCHAR(200) NULL,
  `account_id` VARCHAR(200) NULL,
  `reseller_type` VARCHAR(200) NULL,
  `job_creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `alerts_limit` int(11) DEFAULT NULL,
  `sent_alerts` int(11) DEFAULT NULL,
  `is_valid` varchar(20) DEFAULT NULL,
  `last_alert_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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

CREATE OR REPLACE ALGORITHM = UNDEFINED 
    DEFINER = `refill`@`localhost` 
    SQL SECURITY DEFINER
VIEW `alertapp`.`reseller_balance` AS
    select 
        `res`.`tag` AS `resellerId`,
        `res`.`name` AS `resellerName`,
        `dev`.`address` AS `resellerMSISDN`,
        `rt`.`id` AS `resellerTypeId`,
        `acc`.`balance` AS `resellerBalance`,
        `acc`.`currency` AS `resellerCurrency`,
        `acc`.`accountTypeId` AS `accountTypeId`,
		`acc`.`accountId` AS `accountId`

    from
        ((((`Refill`.`commission_receivers` `res`
        join `Refill`.`extdev_devices` `dev` ON ((`dev`.`owner_key` = `res`.`receiver_key`)))
        join `Refill`.`pay_prereg_accounts` `pay` ON ((`pay`.`owner_key` = `res`.`receiver_key`)))
        join `Refill`.`reseller_types` `rt` ON ((`rt`.`type_key` = `res`.`type_key`)))
        join `accounts`.`accounts` `acc` ON ((`acc`.`accountId` = `pay`.`account_nr`)))
    where
        ((`res`.`status` = 0)
            and (`acc`.`status` = 'Active')
            and (`dev`.`address` is not null)
            and dev.state =2)
    order by `acc`.`balance`;
    
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