START TRANSACTION;

ALTER TABLE `bi`.`report_list` MODIFY `query` VARCHAR(8000) NOT NULL DEFAULT '';

ALTER TABLE `bi`.`report_metadata` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL AFTER `name`;

COMMIT;