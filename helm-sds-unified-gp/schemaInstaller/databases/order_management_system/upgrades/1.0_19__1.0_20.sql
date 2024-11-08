SET @orderTransactionType = 'SOLD_STOCK',
@orderTransactionTypeDescription = 'selling of stock in ISO raised in trip';
INSERT INTO `order_transaction_category_type`
(`type`, `description`)
VALUES
  (@orderTransactionType, @orderTransactionTypeDescription)
ON DUPLICATE KEY UPDATE
  type = @orderTransactionType,
  description = @orderTransactionTypeDescription;

ALTER TABLE `order_transaction` MODIFY `id` bigint(11) AUTO_INCREMENT;

ALTER TABLE `ledger_book` MODIFY `id` bigint(11) AUTO_INCREMENT;

CREATE TABLE IF NOT EXISTS `scheduler_info` (
	`id` int(11) unsigned NOT NULL AUTO_INCREMENT,
	`scheduler_name` varchar(256) DEFAULT NULL,
	`start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`end_time` datetime,
	`status` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` datetime NOT NULL,
  `locked_at` datetime NOT NULL,
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;