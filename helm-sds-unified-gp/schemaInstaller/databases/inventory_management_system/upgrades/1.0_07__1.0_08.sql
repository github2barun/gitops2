ALTER TABLE `nonserialized_inventory` ADD `ref_no` varchar(40) DEFAULT NULL;
ALTER TABLE `nonserialized_inventory`ADD `update_reason` varchar(1000) DEFAULT NULL;
ALTER TABLE `trackable_nonserialized_inventory` ADD `update_reason` varchar(1000) DEFAULT NULL ;
