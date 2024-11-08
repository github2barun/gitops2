START TRANSACTION;

ALTER TABLE `bi`.`std_daily_transaction_summary_aggregation`
    ADD COLUMN `displayReceiverMSISDN` varchar(50) DEFAULT NULL AFTER `receiverMSISDN`;

COMMIT;