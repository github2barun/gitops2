ALTER TABLE `serialized_inventory` ADD `is_deleted` tinyint(4) NOT NULL DEFAULT '0' ;
ALTER TABLE `trackable_nonserialized_inventory` ADD `is_deleted` tinyint(4) NOT NULL DEFAULT '0' ;
ALTER TABLE `nonserialized_inventory` ADD `is_deleted` tinyint(4) NOT NULL DEFAULT '0' ;