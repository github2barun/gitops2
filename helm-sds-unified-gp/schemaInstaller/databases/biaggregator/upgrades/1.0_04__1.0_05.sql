DROP TABLE IF EXISTS `bi`.`total_kyc_sales`;
CREATE TABLE `bi`.`total_kyc_sales` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `end_time_day` date DEFAULT NULL,
  `posId` varchar(255) DEFAULT NULL,
  `brand` varchar(255) DEFAULT NULL,
  `count` bigint(25) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `report_metadata` MODIFY `values` varchar(1000);