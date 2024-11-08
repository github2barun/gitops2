ALTER TABLE pms.product MODIFY COLUMN `name` varchar(255) NOT NULL;
ALTER TABLE product_variant MODIFY COLUMN `product_sku` varchar(255) NOT NULL;
ALTER TABLE product_variant MODIFY COLUMN `product_variant_name` varchar(255);
