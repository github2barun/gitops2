START TRANSACTION;

ALTER TABLE `bi`.`std_daily_transaction_summary_aggregation` ADD COLUMN `receiverResellerCommission` decimal(65,5) DEFAULT NULL;
ALTER TABLE `bi`.`std_daily_transaction_summary_aggregation` ADD COLUMN `receiverResellerBonus` decimal(65,5) DEFAULT NULL;

COMMIT;