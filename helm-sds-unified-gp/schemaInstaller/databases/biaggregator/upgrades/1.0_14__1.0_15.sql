ALTER table `bi`.`hourly_cdr_usage_statistics_aggregation` ADD COLUMN `dealer_epos_terminal_id` varchar(255) DEFAULT NULL;

ALTER TABLE `bi`.`hourly_cdr_usage_statistics_aggregation` CHANGE COLUMN `hour` `hour` VARCHAR(20) NULL DEFAULT NULL ;

ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `senderAccountType` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `quantity` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `currency` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `dealer_code` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `auth_code` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `transaction_status` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `commission_group` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `commission_rate` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `commission_amount` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `bill_id` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `buyer_dealer_code` varchar(255) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `branch_name` varchar(255) DEFAULT NULL;

ALTER TABLE `bi`.`total_monetary_transaction_summary` ADD COLUMN `productsku` varchar(255) DEFAULT NULL;

ALTER TABLE `bi`.`buyer_wise_purchase_summary` ADD COLUMN `dealer_code` varchar(255) DEFAULT NULL;

ALTER TABLE `bi`.`seller_wise_sales_summary` ADD COLUMN `dealer_code` varchar(255) DEFAULT NULL;

ALTER TABLE `bi`.`dealer_purchase_summary` ADD COLUMN `buyer_dealer_code` varchar(255) DEFAULT NULL;
