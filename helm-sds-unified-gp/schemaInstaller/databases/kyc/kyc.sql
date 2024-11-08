START TRANSACTION;

CREATE DATABASE IF NOT EXISTS kyc;

USE kyc;

SET FOREIGN_KEY_CHECKS = 0;
-- ------------------------------------
--  Table structure for `ersinstall`
-- ------------------------------------
DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `kyc_state_transition_permissions`;
DROP TABLE IF EXISTS `kyc_state_transitions`;
DROP TABLE IF EXISTS `kyc_operations`;
DROP TABLE IF EXISTS `kyc_states`;

CREATE TABLE `kyc_states` (
  `id` smallint(6) NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` text NULL DEFAULT NULL,
  `available_from` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `available_until` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX (`code`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `kyc_operations` (
    `id` smallint(6) NOT NULL AUTO_INCREMENT,
    `initial_state_id` smallint(6) NULL DEFAULT NULL,
    `code` varchar(100) NOT NULL,
    `name` varchar(200) NOT NULL,
    `description` text NULL DEFAULT NULL,
    `criteria` varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_operation_initial_state_id_idx` (`initial_state_id`),
    INDEX `fk_operation_code_idx` (`code`),
    CONSTRAINT `fk_operation_initial_state_id`
    FOREIGN KEY (`initial_state_id`)
    REFERENCES `kyc_states` (`id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `kyc_state_transitions` (
    `id` smallint(6) NOT NULL AUTO_INCREMENT,
    `from_state_id` smallint(6) NOT NULL,
    `operation_id` smallint(6) NOT NULL,
    `to_state_id` smallint(6) NOT NULL,
    `business_rules` text NULL DEFAULT NULL,
    `mandatory_business_actions` text NULL DEFAULT NULL,
    `business_actions` text NULL DEFAULT NULL,
    `available_from` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `available_until` datetime NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_state_transition_from_state_id_idx` (`from_state_id`),
    INDEX `fk_state_transition_to_state_id_idx` (`to_state_id`),
    INDEX `fk_state_transition_operation_id_idx` (`operation_id`),
    CONSTRAINT `fk_state_transition_from_state_id`
    FOREIGN KEY (`from_state_id`)
    REFERENCES `kyc_states` (`id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_state_transition_to_state_id`
    FOREIGN KEY (`to_state_id`)
    REFERENCES `kyc_states` (`id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_state_transition_operation_id`
    FOREIGN KEY (`operation_id`)
    REFERENCES `kyc_operations` (`id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




CREATE TABLE `kyc_state_transition_permissions` (
    `id` smallint(6) NOT NULL AUTO_INCREMENT,
    `state_transition_id` smallint(6) NOT NULL,
    `reseller_type` varchar(200) NULL DEFAULT NULL,
    `role_id` varchar(32) NULL DEFAULT NULL,
    `criteria` varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX (`role_id`),
    INDEX `fk_state_transition_permission_state_transition_id_idx` (`state_transition_id`),
    CONSTRAINT `fk_state_transition_permission_state_transition_id`
    FOREIGN KEY (`state_transition_id`)
    REFERENCES `kyc_state_transitions` (`id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION,
    UNIQUE KEY `state_transition_unique` (`reseller_type`,`role_id`,`state_transition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `kyc_templates`;

CREATE TABLE `kyc_templates` (
  `template_name` varchar(45) NOT NULL,
  `template_value` varchar(8000) NOT NULL DEFAULT '',
  PRIMARY KEY (`template_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;