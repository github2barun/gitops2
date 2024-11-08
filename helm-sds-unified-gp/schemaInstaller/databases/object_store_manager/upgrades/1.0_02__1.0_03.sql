ALTER TABLE `object_details` DROP COLUMN `resource_path`;
ALTER TABLE `object_details` ADD UNIQUE `rtype_rid_idx` (`resource_type`, `resource_id`);