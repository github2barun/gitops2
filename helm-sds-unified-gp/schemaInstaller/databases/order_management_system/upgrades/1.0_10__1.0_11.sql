CREATE TABLE IF NOT EXISTS `order_transaction_category_type` (
  `type` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `order_transaction_category_type` (`type`, `description`)
VALUES
	('COLLECT_PAYMENT', 'collection of payment from pos'),
	('COLLECT_STOCK', 'collection of stock from warehouse'),
	('DELIVER_STOCK', 'delivery of stock to pos'),
	('DEPOSIT_PAYMENT', 'deposition of payment to warehouse'),
	('DEPOSIT_STOCK', 'deposit of stock to warehouse');

CREATE TABLE IF NOT EXISTS `order_transaction` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sender` varchar(256) DEFAULT NULL,
  `receiver` varchar(256) DEFAULT NULL,
  `transaction_category` varchar(256) DEFAULT NULL,
  `amount` decimal(19,2) NOT NULL,
  `trip_id` varchar(256) DEFAULT NULL,
  `status` varchar(256) DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `external_reference_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tx_category_type_fk` (`transaction_category`),
  CONSTRAINT `tx_category_type_fk` FOREIGN KEY (`transaction_category`) REFERENCES `order_transaction_category_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

CREATE TABLE `ledger_book` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `payer` varchar(256) DEFAULT NULL,
  `payee` varchar(256) DEFAULT NULL,
  `debit` decimal(19,2) DEFAULT NULL,
  `credit` decimal(19,2) DEFAULT NULL,
  `currency` varchar(256) DEFAULT NULL,
  `mode` varchar(256) DEFAULT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `payment_link` varchar(256) DEFAULT NULL,
  `trip_id` varchar(256) DEFAULT NULL,
  `status` varchar(256) DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `payment_id` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;