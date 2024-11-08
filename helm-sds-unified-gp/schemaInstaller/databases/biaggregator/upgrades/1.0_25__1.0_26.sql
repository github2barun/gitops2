ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `sender_reseller_path` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `receiver_reseller_path` VARCHAR(1000) DEFAULT NULL;
ALTER TABLE `bi`.`all_transaction_details` ADD COLUMN `client_reference` VARCHAR(1000) DEFAULT NULL;
