CREATE DATABASE IF NOT EXISTS pms;
USE pms;

-- State table

DROP TABLE IF EXISTS `state`;
CREATE TABLE `state` (
    `state_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `available_from` date NOT NULL,
    `available_until` date NOT NULL,
    `code` varchar(100) NOT NULL,
    `description` text DEFAULT NULL,
    `name` varchar(200) NOT NULL,
    PRIMARY KEY (`state_id`),
    UNIQUE KEY `state_code_index` (`code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Operation table

DROP TABLE IF EXISTS `operation`;
CREATE TABLE `operation` (
    `operation_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `code` varchar(100) NOT NULL,
    `created_stamp` datetime NOT NULL,
    `description` text DEFAULT NULL,
    `name` varchar(200) NOT NULL,
    `updated_stamp` datetime NOT NULL,
    PRIMARY KEY (`operation_id`),
    UNIQUE KEY `operation_code_index` (`code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;





-- State transition table
DROP TABLE IF EXISTS `state_transition`;
CREATE TABLE `state_transition` (
    `state_transition_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `available_from` date NOT NULL,
    `available_until` date NOT NULL,
    `business_actions` text DEFAULT NULL,
    `business_rules` text DEFAULT NULL,
    `from_state_id` bigint(20) NOT NULL,
    `operation_id` bigint(20) NOT NULL,
    `to_state_id` bigint(20) NOT NULL,
    PRIMARY KEY (`state_transition_id`),
    KEY `fk_state_transition_from_state_id_idx` (`from_state_id`),
    KEY `fk_state_transition_to_state_id_idx` (`to_state_id`),
    KEY `fk_state_transition_operation_id_idx` (`operation_id`),
    CONSTRAINT `fk_state_transition_from_state_id_idx` FOREIGN KEY (`from_state_id`) REFERENCES `state` (`state_id`),
    CONSTRAINT `fk_state_transition_operation_id_idx` FOREIGN KEY (`operation_id`) REFERENCES `operation` (`operation_id`),
    CONSTRAINT `fk_state_transition_to_state_id_idx` FOREIGN KEY (`to_state_id`) REFERENCES `state` (`state_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- State transition permission table
CREATE TABLE `state_transition_permission` (
    `id` smallint(6) NOT NULL AUTO_INCREMENT,
    `state_transition_id` bigint(6) NOT NULL,
    `reseller_type` varchar(200) NULL DEFAULT NULL,
    `role_id` varchar(32) NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX (`role_id`),
    INDEX `fk_state_transition_permission_state_transition_id_idx` (`state_transition_id`),
    CONSTRAINT `fk_state_transition_permission_state_transition_id`
    FOREIGN KEY (`state_transition_id`)
    REFERENCES `state_transition` (`state_transition_id`)
    ON DELETE NO ACTION ON UPDATE NO ACTION,
    UNIQUE KEY `state_transition_unique` (`reseller_type`,`role_id`,`state_transition_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `workflow`;
CREATE TABLE `workflow` (
    `workflow_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at` datetime NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `name` varchar(60) NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`workflow_id`),
    UNIQUE KEY `workflow_name_unique_constraint` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE OR REPLACE TABLE `workflow_state_transition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `state_transition_id` bigint(11) NOT NULL,
  `workflow_id` bigint(11) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `workflow_state_fk_01` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`workflow_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `state_transition_fk_01` FOREIGN KEY (`state_transition_id`) REFERENCES `state_transition` (`state_transition_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
alter table workflow_state_transition add constraint workflow_state_transition_unique unique (workflow_id,state_transition_id);

CREATE TABLE `supplier` (
  `supplier_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `name` varchar(255),
  `description` varchar(255),
  `data` json CHECK (JSON_VALID(data)),
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product_status` (
  `product_status_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `description` varchar(60),
  PRIMARY KEY (`product_status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `variant_status` (
  `variant_status_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `description` varchar(60),
  PRIMARY KEY (`variant_status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product_type` (
  `product_type_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(60) NOT NULL,
  `description` varchar(60),
  PRIMARY KEY (`product_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product` (
  `product_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(60),
  `name` varchar(255)  NOT NULL,
  `description` varchar(255),
  `product_type_id` bigint(11),
  `workflow_id` bigint(11),
  `supplier_id` bigint(11),  
  `image_url` varchar(255),  
  `product_status_id` bigint(11),
  `is_category` boolean NOT NULL DEFAULT 0,
  `data` json CHECK (JSON_VALID(data)),
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `available_from` date,
  `available_until` date,
  `path` varchar(255),
  PRIMARY KEY (`product_id`),
  KEY `product_fk_01` (`workflow_id`),
  CONSTRAINT `product_fk_01` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`workflow_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `product_fk_02` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`supplier_id`),
  CONSTRAINT `product_fk_03` FOREIGN KEY (`product_type_id`) REFERENCES `product_type` (`product_type_id`),
  CONSTRAINT `product_fk_04` FOREIGN KEY (`product_status_id`) REFERENCES `product_status` (`product_status_id`),
  CONSTRAINT `product_unique_01` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product_closure` (
  `ancestor` bigint(11) NOT NULL,
  `descendant` bigint(11) NOT NULL,
  `depth` int(11) NOT NULL,
  PRIMARY KEY (`ancestor`,`descendant`),
  KEY `product_type_closure_fk_01` (`ancestor`),
  KEY `product_type_closure_fk_02` (`descendant`),
  CONSTRAINT `product_type_closure_fk_01` FOREIGN KEY (`ancestor`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `product_type_closure_fk_02` FOREIGN KEY (`descendant`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product_variant` (
  `variant_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(11) NOT NULL,
  `product_sku` varchar(255) NOT NULL,
  `product_variant_name` varchar(255),
  `product_variant_description` varchar(255),
  `supplier_ref` varchar(60),
  `EAN` varchar(255),
  `variant_status_id` bigint(11),
  `currency` varchar(60),  
  `image_url` varchar(255),
  `data` json CHECK (JSON_VALID(data)),
  `unit_price` double(18,2) DEFAULT '0.00',
  `variable_price` BOOLEAN DEFAULT '0',
  `uom` varchar(20),
  `available_from` date,
  `available_until` date,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`variant_id`),
  KEY `product_variant_fk_01` (`product_id`),
  CONSTRAINT `product_variant_fk_01` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `product_variant_fk_02` FOREIGN KEY (`variant_status_id`) REFERENCES `variant_status` (`variant_status_id`),
  CONSTRAINT `product_variant_unique_01` UNIQUE (`product_sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX SKU_INDEX ON product_variant(product_sku);

CREATE TABLE `tax` (
  `tax_id` bigint(11) NOT NULL AUTO_INCREMENT,
  `tax_type` varchar(60) NOT NULL,
  `description` varchar(255),
  `percent_value` double(18,2) NOT NULL,
  `fixed_value` double(18,2) NOT NULL,
  `data` json CHECK (JSON_VALID(data)),
  `created_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tax_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `product_tax_map` (
  `product_id` bigint(11) NOT NULL,
  `tax_id` bigint(11) NOT NULL,
  PRIMARY KEY (`product_id`,`tax_id`),
  KEY `product_tax_map_fk_01` (`product_id`),
  KEY `product_tax_map_fk_02` (`tax_id`),
  CONSTRAINT `product_tax_map_fk_01` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `product_tax_map_fk_02` FOREIGN KEY (`tax_id`) REFERENCES `tax` (`tax_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE OR REPLACE TABLE `related_product_variant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `variant_id` bigint(11) NOT NULL,
  `related_variant_id` bigint(11) NOT NULL,
  `relation_type` varchar(10) DEFAULT 'RELATED',
  PRIMARY KEY (`id`),
  CONSTRAINT `related_product_fk_01` FOREIGN KEY (`variant_id`) REFERENCES `product_variant` (`variant_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `related_product_fk_02` FOREIGN KEY (`related_variant_id`) REFERENCES `product_variant` (`variant_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
alter table related_product_variant add constraint related_product_unique unique (variant_id,related_variant_id);

DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO variant_status (code, description) values ('available', 'Product variant is available');
INSERT INTO variant_status (code, description) values ('blocked', 'Product variant is blocked');
INSERT INTO variant_status (code, description) values ('decommissioned', 'Product variant is decommissioned');

INSERT INTO product_status (code, description) values ('available', 'Product is available');
INSERT INTO product_status (code, description) values ('blocked', 'Product is blocked');
INSERT INTO product_status (code, description) values ('decommissioned', 'Product is decommissioned');

INSERT INTO product_type (code, description) values ('serialised', 'Type is used for serialised products');
INSERT INTO product_type (code, description) values ('non-serialised', 'Type is used for non-serialised products');
INSERT INTO product_type (code, description) values ('services', 'Type is used for services products');
INSERT INTO product_type (code, description) values ('trackable-non-serialised', 'Type is used for trackable products');
INSERT INTO product_type (code, description) values ('services-b2b', 'Type is used for services related to B2B');
INSERT INTO product_type (code, description) values ('services-topup', 'Type is used for services related to Topup operations');
INSERT INTO product_type (code, description) values ('services-custom', 'Type is used for custom services operations');
INSERT INTO product_type (code, description) values ('batch', 'Type is used for batch products');

ALTER TABLE related_product_variant ADD COLUMN quantity INT default 1;

ALTER TABLE tax MODIFY COLUMN percent_value double(4,2);
ALTER TABLE tax MODIFY COLUMN fixed_value double(18,2);


DROP TABLE IF EXISTS `product_publish_version`;
CREATE TABLE `product_publish_version` (
  `version_id` int(11) unsigned NOT NULL,
  `publish_date` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `remarks` text DEFAULT NULL,
  PRIMARY KEY (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;