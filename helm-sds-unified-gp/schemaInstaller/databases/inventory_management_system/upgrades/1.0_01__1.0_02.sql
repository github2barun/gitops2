CREATE SEQUENCE BOX_ID_SEQ START WITH 1 INCREMENT BY 1 ;

ALTER TABLE `serialized_inventory` ADD `box_history` mediumtext DEFAULT NULL;

ALTER TABLE serialized_inventory ADD COLUMN inventory_id_type VARCHAR(255) NULL DEFAULT 'SERIAL';