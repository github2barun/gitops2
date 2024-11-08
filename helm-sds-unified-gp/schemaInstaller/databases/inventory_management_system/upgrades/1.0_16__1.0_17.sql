ALTER TABLE inventory_management_system.reserved_inventory_cache
    ADD COLUMN ref_no varchar(40) NULL;
ALTER TABLE inventory_management_system.reserved_inventory_cache
    ADD COLUMN batch_id varchar(40) NULL;