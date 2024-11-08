ALTER TABLE reserved_inventory_cache MODIFY ref_no VARCHAR(60);
ALTER TABLE serialized_inventory MODIFY ref_no VARCHAR(60);
ALTER TABLE trackable_nonserialized_inventory MODIFY ref_no VARCHAR(60);
ALTER TABLE nonserialized_inventory MODIFY ref_no VARCHAR(60);