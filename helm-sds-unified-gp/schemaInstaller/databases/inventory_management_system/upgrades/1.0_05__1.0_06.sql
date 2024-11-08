SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `serialized_inventory` DROP CONSTRAINT `fk2`;
ALTER TABLE `nonserialized_inventory` DROP CONSTRAINT `ns_fk2`;
ALTER TABLE `trackable_nonserialized_inventory` DROP CONSTRAINT `tns_fk2`;

TRUNCATE TABLE `location`;

ALTER TABLE `location` DROP COLUMN `location_id`;
ALTER TABLE `location` ADD PRIMARY KEY (`owner_id`);
ALTER TABLE `location` RENAME TO `owner_address`;

SET FOREIGN_KEY_CHECKS = 1;

UPDATE `serialized_inventory` SET `location_id` = NULL;
UPDATE `nonserialized_inventory` SET `location_id` = NULL;
UPDATE `trackable_nonserialized_inventory` SET `location_id` = NULL;

ALTER TABLE `serialized_inventory` ADD CONSTRAINT `fk2`  FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`);
ALTER TABLE `nonserialized_inventory` ADD CONSTRAINT `ns_fk2`  FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`);
ALTER TABLE `trackable_nonserialized_inventory` ADD CONSTRAINT `tns_fk2`  FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`);