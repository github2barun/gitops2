ALTER TABLE `nonserialized_inventory`ADD `employee_id` varchar(60) DEFAULT NULL;
ALTER TABLE `serialized_inventory`ADD `employee_id` varchar(60) DEFAULT NULL;
ALTER TABLE `trackable_nonserialized_inventory`ADD `employee_id` varchar(60) DEFAULT NULL;