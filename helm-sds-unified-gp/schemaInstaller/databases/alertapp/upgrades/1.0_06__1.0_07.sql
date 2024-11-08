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
    `message` VARCHAR(200) DEFAULT '',
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