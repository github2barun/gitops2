CREATE DATABASE IF NOT EXISTS template_management;

USE template_management;

DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall` (
    `VersionKey` SMALLINT(6) NOT NULL AUTO_INCREMENT,
    `Version` VARCHAR(20) NOT NULL,
    `Status` TINYINT(4) NOT NULL DEFAULT '0',
    `Script` VARCHAR(200) NOT NULL,
    `last_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`VersionKey`)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8;


DROP TABLE IF EXISTS `component_type`;
CREATE TABLE `component_type` (
  `component_type` varchar(250) NOT NULL DEFAULT '',
  PRIMARY KEY (`component_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `reseller_type`;
CREATE TABLE `reseller_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reseller_type` varchar(80) NOT NULL DEFAULT '',
  `user_role` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `template_type`;
CREATE TABLE `template_type` (
  `template_type` varchar(250) NOT NULL DEFAULT '',
  PRIMARY KEY (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



DROP TABLE IF EXISTS `template`;
CREATE TABLE `template` (
  `id` SMALLINT(6) NOT NULL AUTO_INCREMENT,
  `component` VARCHAR(250) NOT NULL,
  `type` VARCHAR(250) NOT NULL,
  `name` VARCHAR(500) NOT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `value` TEXT NOT NULL,
  `query_param` JSON NULL CHECK (JSON_VALID(query_param)),
  `channel` VARCHAR(250) NULL DEFAULT NULL,
  `language` VARCHAR(50) NOT NULL DEFAULT 'en',
  `extra_col1` TEXT NULL DEFAULT NULL,
  `version` SMALLINT NOT NULL,
  `available_from` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `available_until` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `unique_template` UNIQUE (`component` , `type`, `version`, `query_param`),
  CONSTRAINT `template_ibfk_1` FOREIGN KEY (`type`) REFERENCES `template_type` (`template_type`)
  -- CONSTRAINT `template_ibfk_2` FOREIGN KEY (`component`) REFERENCES `component_type` (`component_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `template_reseller_type_mapping`;
CREATE TABLE `template_reseller_type_mapping` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `reseller_type_id` int(11) NOT NULL,
  `template_id` smallint(6) NOT NULL,
  `template_type` varchar(250) DEFAULT '',
  `component_type` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reseller_template_component_constraint` (`reseller_type_id`,`template_type`,`component_type`),
  KEY `template_id` (`template_id`),
  KEY `template_type` (`template_type`),
  KEY `component_type` (`component_type`),
  CONSTRAINT `template_reseller_type_mapping_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `template` (`id`),
  CONSTRAINT `template_reseller_type_mapping_ibfk_2` FOREIGN KEY (`template_type`) REFERENCES `template_type` (`template_type`),
  CONSTRAINT `template_reseller_type_mapping_ibfk_3` FOREIGN KEY (`component_type`) REFERENCES `component_type` (`component_type`),
  CONSTRAINT `template_reseller_type_mapping_ibfk_4` FOREIGN KEY (`reseller_type_id`) REFERENCES `reseller_type` (`id`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;