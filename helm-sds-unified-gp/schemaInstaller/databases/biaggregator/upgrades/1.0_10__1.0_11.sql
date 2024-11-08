DROP TABLE IF EXISTS `bi`.`lifecycle_transaction_aggregator`;
CREATE TABLE `bi`.`lifecycle_transaction_aggregator` (
  `id` varchar(255) NOT NULL DEFAULT '',
  `reseller_id` varchar(200) DEFAULT NULL,
  `last_sender_balance_transaction` date DEFAULT NULL,
  `last_receiver_balance_transaction` date DEFAULT NULL,
  `last_sender_inventory_transaction` date DEFAULT NULL,
  `last_receiver_inventory_transaction` date DEFAULT NULL,
  `created_on` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;