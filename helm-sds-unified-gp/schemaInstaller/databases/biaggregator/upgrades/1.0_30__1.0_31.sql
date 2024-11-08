DROP TABLE IF EXISTS `bi`.`update_parent_reseller`;
CREATE TABLE `bi`.`update_parent_reseller` (
  `id` varchar(200) NOT NULL ,
  `transaction_reference` varchar(200) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) NOT NULL DEFAULT '',
  `parent_reseller_id` varchar(200) NOT NULL DEFAULT '',
  `result_code` varchar(30)  DEFAULT '',
  `result_message` varchar(300)  DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;