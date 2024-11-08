CREATE TABLE IF NOT EXISTS `payment_agreement` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `payment_mode` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `user_payment_mode_mapping` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `reseller_type` varchar(255) DEFAULT '',
  `reseller_id` varchar(255) DEFAULT '',
  `order_type` varchar(20) NOT NULL DEFAULT '',
  `payment_mode` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_payment_mode` (`reseller_type`,`reseller_id`,`order_type`,`payment_mode`),
  KEY `order_type_fk` (`order_type`),
  KEY `payment_mode_fk` (`payment_mode`),
    CONSTRAINT `order_type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`),
    CONSTRAINT `payment_mode_fk` FOREIGN KEY (`payment_mode`) REFERENCES `payment_mode` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `user_payment_agreement_mapping` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `reseller_type` varchar(255) DEFAULT '',
  `reseller_id` varchar(255) DEFAULT '',
  `order_type` varchar(20) NOT NULL DEFAULT '',
  `payment_agreement` varchar(20) NOT NULL DEFAULT '',
  KEY `ordr_type_fk` (`order_type`),
  PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_payment_agreement` (`reseller_type`,`reseller_id`,`order_type`,`payment_agreement`),
  KEY `payment_agreement_fk` (`payment_agreement`),
    CONSTRAINT `ordr_type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`),
    CONSTRAINT `payment_agreement_fk` FOREIGN KEY (`payment_agreement`) REFERENCES `payment_agreement` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET @npr_Name = 'NO_PAYMENT_REQD',
@npr_Description = 'No Payment Required';
INSERT INTO `payment_mode`
(`name`, `description`)
VALUES
  (@npr_Name, @npr_Description)
ON DUPLICATE KEY UPDATE
  name = @npr_Name,
  description = @npr_Description;

SET @cash_Name = 'CASH',
@cash_Description = 'Cash On Delivery';
INSERT INTO `payment_mode`
(`name`, `description`)
VALUES
  (@cash_Name, @cash_Description)
ON DUPLICATE KEY UPDATE
  name = @cash_Name,
  description = @cash_Description;

SET @m_pesa_Name = 'M_PESA',
@m_pesa_Description = 'M_Pesa wallet money';
INSERT INTO `payment_mode`
(`name`, `description`)
VALUES
  (@m_pesa_Name, @m_pesa_Description)
ON DUPLICATE KEY UPDATE
  name = @m_pesa_Name,
  description = @m_pesa_Description;

SET @na_Name = 'NA',
@na_Description = 'Not applicable';
INSERT INTO `payment_agreement`
(`name`, `description`)
VALUES
  (@na_Name, @na_Description)
ON DUPLICATE KEY UPDATE
  name = @na_Name,
  description = @na_Description;

SET @upfront_Name = 'UPFRONT',
@upfront_Description = 'Payment at the time of order placement';
INSERT INTO `payment_agreement`
(`name`, `description`)
VALUES
  (@upfront_Name, @upfront_Description)
ON DUPLICATE KEY UPDATE
  name = @upfront_Name,
  description = @upfront_Description;

SET @consign_Name = 'CONSIGNMENT',
@consign_Description = 'Payment at time of delivery';
INSERT INTO `payment_agreement`
(`name`, `description`)
VALUES
  (@consign_Name, @consign_Description)
ON DUPLICATE KEY UPDATE
  name = @consign_Name,
  description = @consign_Description;