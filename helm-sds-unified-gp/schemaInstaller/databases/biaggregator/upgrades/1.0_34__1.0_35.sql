START TRANSACTION;

ALTER TABLE `bi`.`all_trips_detail` ADD COLUMN `warehouse_id` VARCHAR(200) DEFAULT NULL AFTER `pos_status`;

ALTER TABLE `bi`.`all_trips_detail` ADD COLUMN `outlet_id` VARCHAR(200) DEFAULT NULL AFTER `warehouse_id`;

ALTER TABLE `bi`.`all_trips_detail` ADD COLUMN `task_status` VARCHAR(200) DEFAULT NULL AFTER `outlet_id`;

COMMIT;