ALTER TABLE `object_details` ADD COLUMN `resource_path` varchar(255) DEFAULT NULL AFTER `resource_type`;
ALTER TABLE `object_details` DROP INDEX `rtype_rown_filename_idx`;