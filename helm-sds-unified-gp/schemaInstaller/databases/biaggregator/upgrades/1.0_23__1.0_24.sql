DROP TABLE IF EXISTS `bi`.`external_pos_stock_holding`;
CREATE TABLE `bi`.`external_pos_stock_holding` (
	`id` varchar(255) NOT NULL DEFAULT '',
	`stock_date` datetime DEFAULT NULL,
    `reseller_id` varchar(100) DEFAULT NULL,
    `reseller_type` varchar(50) DEFAULT NULL,
    `product_sku` varchar(100) DEFAULT NULL,
    `stock_qty_sold` bigint(20) DEFAULT NULL,
    `stock_qty_hand` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`reseller_account_statement_report`;
CREATE TABLE `bi`.`reseller_account_statement_report` (
	`id` varchar(255) NOT NULL DEFAULT '',
	`transaction_date` datetime DEFAULT NULL,
    `reseller_id` varchar(100) DEFAULT NULL,
    `opening_balance` bigint(20) DEFAULT NULL,
    `balance_transfer_in` bigint(20) DEFAULT NULL,
    `balance_transfer_out` bigint(20) DEFAULT NULL,
    `balance_in_hand` bigint(20) DEFAULT NULL,
    `currency` varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bi`.`report_list` ADD COLUMN `extra_field_1` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`report_list` ADD COLUMN `extra_field_2` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`report_metadata` MODIFY `values` varchar(5000);
