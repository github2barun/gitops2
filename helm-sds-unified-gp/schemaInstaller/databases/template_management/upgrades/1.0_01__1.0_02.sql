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


INSERT INTO `template_type` (`template_type`)
SELECT DISTINCT (type) FROM `template`;

INSERT INTO `component_type` (`component_type`)
SELECT DISTINCT (component) FROM `template`;

ALTER TABLE `template`
ADD CONSTRAINT `template_ibfk_1` FOREIGN KEY (`type`) REFERENCES `template_type` (`template_type`),
ADD CONSTRAINT `template_ibfk_2` FOREIGN KEY (`component`) REFERENCES `component_type` (`component_type`);


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









