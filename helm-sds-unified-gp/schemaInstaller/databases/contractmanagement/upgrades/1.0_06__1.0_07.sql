ALTER TABLE product_variant_rule_association MODIFY COLUMN created_date DATETIME NOT NULL DEFAULT current_timestamp;
ALTER TABLE product_variant_rule_association MODIFY COLUMN valid_from DATETIME NOT NULL DEFAULT current_timestamp;
ALTER TABLE product_variant_rule_association MODIFY COLUMN valid_until DATETIME NOT NULL;

ALTER TABLE rule_fields_association MODIFY COLUMN created_date DATETIME NOT NULL DEFAULT current_timestamp;
ALTER TABLE rule_fields_association MODIFY COLUMN valid_from DATETIME NOT NULL DEFAULT current_timestamp;
ALTER TABLE rule_fields_association MODIFY COLUMN valid_until DATETIME NOT NULL;