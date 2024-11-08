ALTER TABLE `inventory_management_system`.`nonserialized_inventory` DROP COLUMN `batch_id`;
ALTER TABLE `inventory_management_system`.`nonserialized_inventory` DROP COLUMN `box_history`;

ALTER TABLE `serialized_inventory` ADD CONSTRAINT `serialized_employee_owner_fk` FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`);
ALTER TABLE `nonserialized_inventory` ADD CONSTRAINT `nonserialized_employee_owner_fk`  FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`);
ALTER TABLE `trackable_nonserialized_inventory` ADD CONSTRAINT `trackable_employee_owner_fk`  FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`);
