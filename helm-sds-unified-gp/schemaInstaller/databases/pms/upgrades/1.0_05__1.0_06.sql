ALTER TABLE related_product_variant ADD COLUMN quantity INT default 1;

INSERT INTO product_type (code, description) values ('batch', 'Type is used for batch products');