DROP TABLE IF EXISTS `bi`.`health_control_info`;
CREATE TABLE `bi`.`health_control_info` (
  `id` varchar(200) NOT NULL,
  `commission_name` varchar(200) NOT NULL DEFAULT '',
  `date_inserted` varchar(30) NOT NULL DEFAULT '',
  `commission_count` varchar(200) DEFAULT '',
  `commission_amount` varchar(200) DEFAULT '',
  `total_count` varchar(20) NOT NULL DEFAULT '',
  `total_amount` varchar(200) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
