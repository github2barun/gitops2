START TRANSACTION;

CREATE
DATABASE IF NOT EXISTS inventory_management_system;

USE inventory_management_system;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `owner` (
  `owner_id` varchar(60) NOT NULL,
  `name` varchar(60)  DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `type` varchar(60) DEFAULT NULL,
  `data` longtext,
  `reseller_path` varchar (255),
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `owner_address` (
  `owner_id` varchar(60) NOT NULL,
  `address_line1` varchar(255) DEFAULT NULL,
  `address_line2` varchar(60) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `pincode` varchar(10) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `latitude` varchar(100) DEFAULT '0.00',
  `longitude` varchar(100) DEFAULT '0.00',
  `data` longtext,
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`owner_id`),
  KEY `location_fk_01` (`owner_id`),
  CONSTRAINT `location_fk_01` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `serialized_inventory` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_sku` varchar(255) NOT NULL,
  `serial_text` varchar(40) DEFAULT '',
  `serial_number` varchar(40) NULL,
  `inventory_condition` varchar(40) NULL,
  `workflow_state_id` BIGINT(11) DEFAULT NULL,
  `location_id` varchar(60) DEFAULT NULL,
  `batch_id` varchar(40) DEFAULT NULL,
  `box_history` mediumtext DEFAULT NULL,
  `inventory_id_type` VARCHAR(255) NULL DEFAULT 'SERIAL',  
  `owner_id` varchar(60) NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT '0',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0',
  `table_tag` varchar(40) DEFAULT NULL,
  `resource_id` VARCHAR(100) DEFAULT NULL,
  `ref_no` varchar(60) DEFAULT NULL,
  `data` longtext,
  `update_reason` varchar(1000) DEFAULT NULL,
  `employee_id` varchar(60) DEFAULT NULL,
  `owner_history` mediumtext DEFAULT NULL,
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UC_product_serial_number` (product_sku, serial_number),
  FULLTEXT KEY `data` (`data`),
  CONSTRAINT `fk1` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `fk2` FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `serialized_employee_owner_fk` FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`),
  INDEX `serial_number_idx` (`serial_number`),
  INDEX `owner_id_serial_number_idx` (`owner_id`,`serial_number`),
  INDEX `batch_id_owner_id_serial_number_workflow_state_id_idx` (`batch_id`, `owner_id`, `serial_number`, `workflow_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `nonserialized_inventory` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_sku` varchar(255) NOT NULL,
  `quantity` double(20,5) DEFAULT NULL,
  `location_id` varchar(60) DEFAULT NULL,
  `owner_id` varchar(60) NOT NULL,
  `workflow_state_id` BIGINT(11) DEFAULT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT '0',
  `is_deleted` tinyint(4) DEFAULT 0 NOT NULL,
  `data` longtext,
  `ref_no` varchar(60) DEFAULT NULL,
  `update_reason` varchar(1000) DEFAULT NULL,
  `employee_id` varchar(60) DEFAULT NULL,
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inventory_condition` varchar(40) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ns_fk1` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `ns_fk2` FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `nonserialized_employee_owner_fk`  FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `trackable_nonserialized_inventory` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_sku` varchar(255) NOT NULL,
  `quantity` double(20,5) DEFAULT NULL,
  `uom` varchar(10) DEFAULT NULL,
  `location_id` varchar(60) DEFAULT NULL,
  `owner_id` varchar(60) NOT NULL,
  `workflow_state_id` BIGINT(11) DEFAULT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT '0',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0',
  `data` longtext,
  `batch_id` varchar(40) DEFAULT NULL,
  `box_history` mediumtext DEFAULT NULL,
  `start_no` BIGINT(11) NOT NULL,
  `end_no` BIGINT(11) NOT NULL,
  `ref_no` varchar(60) DEFAULT NULL,
  `owner_history` mediumtext DEFAULT NULL,
  `update_reason` varchar(1000) DEFAULT NULL,
  `employee_id` varchar(60) DEFAULT NULL,
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resource_id` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `tns_fk1` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `tns_fk2` FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`),
  CONSTRAINT `trackable_employee_owner_fk`  FOREIGN KEY (`employee_id`) REFERENCES `owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `serialized_inventory_transition` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_sku` varchar(255) NOT NULL,
  `batch_id` varchar(40) DEFAULT NULL,
  `start_serial` BIGINT(11) NOT NULL,
  `end_serial` BIGINT(11) NOT NULL,
  `from_owner` varchar(60) DEFAULT NULL,
  `to_owner` varchar(60) DEFAULT NULL,
  `initiator` varchar(60) DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `box_id` varchar(60) DEFAULT NULL,
  `workflow_state_id` bigint(11) DEFAULT NULL,
  `created_stamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_stamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `plan` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `plan_id` varchar(60) DEFAULT NULL,
  `plan_name` varchar(50) DEFAULT NULL,
  `plan_external_name` varchar(50) DEFAULT NULL,
  `brand_code` varchar(11) DEFAULT NULL,
  `offer_type` varchar(50) DEFAULT NULL,
  `price` double(18,2) DEFAULT NULL,
  `start_date` timestamp NULL DEFAULT NULL,
  `end_date` timestamp NULL DEFAULT NULL,
  `service_class` varchar(50) DEFAULT NULL,
  `created_stamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_stamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UC_plan_id_brand_code` (`plan_id`,`brand_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IF NOT EXISTS serial_number_idx ON serialized_inventory(serial_number);
CREATE INDEX IF NOT EXISTS batch_id_owner_id_serial_number_workflow_state_id_idx ON serialized_inventory(batch_id, owner_id, serial_number, workflow_state_id);
CREATE INDEX IF NOT EXISTS owner_id_serial_number_idx ON serialized_inventory(owner_id, serial_number);

CREATE SEQUENCE BOX_ID_SEQ START WITH 1 INCREMENT BY 1 ;

CREATE TABLE IF NOT EXISTS `reserved_inventory_cache` (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `owner_id` varchar(60) DEFAULT NULL,
    `location_id` varchar(60)  DEFAULT NULL,
    `product_sku` varchar(255) NOT NULL,
    `product_type` varchar(60)  DEFAULT NULL,
    `serial_number` DECIMAL(40, 0),
    `start_no` BIGINT(11) NOT NULL,
    `end_no` BIGINT(11) NOT NULL,
    `ref_no` varchar(60) DEFAULT NULL,
    `batch_id` varchar(40) DEFAULT NULL,
    `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `inventory_cache_owner_fk1` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`owner_id`),
    CONSTRAINT `inventory_cache_location_fk2` FOREIGN KEY (`location_id`) REFERENCES `owner` (`owner_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE reserved_inventory_cache MODIFY start_no DECIMAL(40, 0);
ALTER TABLE reserved_inventory_cache MODIFY end_no DECIMAL(40, 0);
ALTER TABLE trackable_nonserialized_inventory MODIFY start_no DECIMAL(40, 0);
ALTER TABLE trackable_nonserialized_inventory MODIFY end_no DECIMAL(40, 0);

CREATE TABLE shedlock(name VARCHAR(64) NOT NULL, lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));

ALTER TABLE serialized_inventory DROP COLUMN table_tag;

CREATE TABLE `box_type` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(60) NOT NULL,
    `parent` int(11) DEFAULT NULL,
    `path` varchar(255) NOT NULL,
    `level` int(4) NOT NULL,
    `created_stamp` timestamp NOT NULL DEFAULT current_timestamp(),
    `is_wrapper` tinyint(4) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `box` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `name` varchar(60) NOT NULL,
   `type` int(11) NOT NULL,
   `parent` int(11) DEFAULT NULL,
   `path` varchar(255) NOT NULL,
   `created_stamp` timestamp NOT NULL DEFAULT current_timestamp(),
   PRIMARY KEY (`id`),
   KEY `type` (`type`),
   CONSTRAINT `box_ibfk_1` FOREIGN KEY (`type`) REFERENCES `box_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

drop function if exists createUniqueBoxIdTNSI;
DELIMITER //
create function createUniqueBoxIdTNSI() returns varchar(40)
begin
    declare temp varchar(40);
    set temp = concat('B00',(select NEXTVAL(BOX_ID_SEQ)));
    while exists (select 1 from trackable_nonserialized_inventory tnsi where tnsi.`batch_id` = temp) do
            set temp = concat('B00',(select NEXTVAL(BOX_ID_SEQ)));
        end while;
    return temp;
end //

drop trigger if exists create_box_id_tnsi;
DELIMITER //
CREATE DEFINER=`refill`@`%` TRIGGER create_box_id_tnsi BEFORE UPDATE ON trackable_nonserialized_inventory
    FOR EACH ROW
BEGIN
    declare boxId varchar(40);
    declare oldBoxId varchar(40);
    declare oldBoxHistory MEDIUMTEXT;
    IF NEW.`batch_id` IS NULL THEN
        select createUniqueBoxIdTNSI() into @boxId;
        select `batch_id` from trackable_nonserialized_inventory where id = NEW.`id` into @oldBoxId;
        select `box_history` from trackable_nonserialized_inventory where id = NEW.`id` into @oldBoxHistory;
        SET NEW.`box_history` = CONCAT(IFNULL(CONCAT(@oldBoxHistory,'/'), CONCAT(@oldBoxId, '/')), @boxId);
        SET NEW.`batch_id` = @boxId;
    END IF;
END;//
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

