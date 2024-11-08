ALTER TABLE `bi`.`report_metadata` MODIFY `type` varchar(40);

ALTER TABLE `bi`.`reseller_current_stock` ADD COLUMN `date` date DEFAULT NULL;
ALTER TABLE `bi`.`reseller_current_stock` ADD COLUMN `weekNumber` int(4) DEFAULT NULL;
ALTER TABLE `bi`.`reseller_current_stock` ADD COLUMN `year` int(4) DEFAULT NULL;

ALTER TABLE `bi`.`all_transaction_details` MODIFY `quantity`  bigint(25);
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `order_id` varchar(200) DEFAULT NULL;
