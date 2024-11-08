DROP TABLE IF EXISTS `bi`.`tp_staff_monthly_payment`;
CREATE TABLE `bi`.`tp_staff_monthly_payment` (
  `id` varchar(200) NOT NULL ,
  `reseller_id` varchar(200) NOT NULL DEFAULT '',
  `reseller_name` varchar(200) NOT NULL DEFAULT '',
  `year` varchar(30) NOT NULL DEFAULT '',
  `month` varchar(30)  DEFAULT '',
  `start_date` varchar(200)  DEFAULT '',
  `end_date` varchar(200)  DEFAULT '',
  `totalNet_amount` varchar(300)  DEFAULT '',
  `lastUpdatedDate` varchar(300)  DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;