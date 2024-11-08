ALTER TABLE nonserialized_inventory drop column uom;
ALTER TABLE nonserialized_inventory ADD `box_history` mediumtext DEFAULT NULL;