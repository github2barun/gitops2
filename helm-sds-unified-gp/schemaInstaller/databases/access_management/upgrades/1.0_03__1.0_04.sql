DROP TABLE IF EXISTS `gateway_management`;
CREATE TABLE `gateway_management` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `gateway_code` varchar(30) NOT NULL DEFAULT '' UNIQUE,
  `gateway_type_channel` varchar(50) NOT NULL DEFAULT '',
  `service_port` varchar(20) DEFAULT NULL,
  `source_type` varchar(30) DEFAULT NULL,
  `login` varchar(50) NOT NULL DEFAULT '' UNIQUE,
  `password` text NOT NULL,
  `content_type` varchar(30) DEFAULT NULL,
  `auth_type` varchar(30) DEFAULT NULL,
  `status` int(4) NOT NULL DEFAULT 0,
  `network_code` varchar(20) DEFAULT NULL,
  `ip_auth` text DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT '',
  `created_date` TIMESTAMP ,
  `modified_date` TIMESTAMP ,
  `modified_by` varchar(50) NOT NULL DEFAULT '',
  `sms_notification` varchar(50) NOT NULL DEFAULT '',
  `tps_control` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;