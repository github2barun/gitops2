CREATE OR REPLACE TABLE `related_product_variant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `variant_id` bigint(11) NOT NULL,
  `related_variant_id` bigint(11) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `related_product_fk_01` FOREIGN KEY (`variant_id`) REFERENCES `product_variant` (`variant_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `related_product_fk_02` FOREIGN KEY (`related_variant_id`) REFERENCES `product_variant` (`variant_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
alter table related_product_variant add constraint related_product_unique unique (variant_id,related_variant_id);