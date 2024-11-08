DROP TABLE IF EXISTS `product_publish_version`;
CREATE TABLE `product_publish_version` (
  `version_id` int(11) unsigned NOT NULL,
  `publish_date` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `remarks` text DEFAULT NULL,
  PRIMARY KEY (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;