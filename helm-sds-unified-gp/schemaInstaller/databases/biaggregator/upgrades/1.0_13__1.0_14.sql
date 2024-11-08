DROP TABLE IF EXISTS `bi`.`total_monetary_transaction_summary`;
CREATE TABLE `bi`.`total_monetary_transaction_summary` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `dealer_type` varchar(200) DEFAULT NULL,
  `dealer_id` varchar(200) DEFAULT NULL,
  `dealer_msisdn` varchar(200) DEFAULT NULL,
  `dealer_epos_terminal_id` varchar(200) DEFAULT NULL,
  `transaction_type` varchar(200) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `section` varchar(200) DEFAULT NULL,
  `city` varchar(200) DEFAULT NULL,
  `transaction_count` bigint(100) DEFAULT NULL,
  `sum` decimal(20,5) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `bi`.`dealer_purchase_summary`;
CREATE TABLE `bi`.`dealer_purchase_summary` (
  `id` varchar(255) NOT NULL,
  `transaction_date` datetime DEFAULT NULL,
  `buyer_dealer_type` varchar(200) DEFAULT NULL,
  `buyer_dealer_id` varchar(200) DEFAULT NULL,
  `buyer_dealer_msisdn` varchar(200) DEFAULT NULL,
  `buyer_dealer_status` varchar(200) DEFAULT NULL,
  `buyer_dealer_balance` varchar(200) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `section` varchar(200) DEFAULT NULL,
  `city` varchar(200) DEFAULT NULL,
  `seller_dealer_type` varchar(200) DEFAULT NULL,
  `purchase_frequency` bigint(100) DEFAULT NULL,
  `purchase_amount` decimal(20,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;