CREATE TABLE IF NOT EXISTS `order_reason_type` (
  `code` varchar(255) NOT NULL DEFAULT '',
  `type` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`code`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

RENAME TABLE `order_reject_reason` TO `order_reason`;