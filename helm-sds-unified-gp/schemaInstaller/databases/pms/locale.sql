SET FOREIGN_KEY_CHECKS = 0;
use pms;


# Dump of table ersinstall
# ------------------------------------------------------------
TRUNCATE TABLE `ersinstall`;
INSERT INTO `ersinstall` (`VersionKey`, `Version`, `Status`, `Script`, `last_modified`)
VALUES
	(1,'1.0_08',1,'Fresh installation','2022-11-02 15:13:27');


# Dump of table operation
# ------------------------------------------------------------
TRUNCATE TABLE `operation`;
INSERT INTO `operation` (`operation_id`, `code`, `created_stamp`, `description`, `name`, `updated_stamp`)
VALUES
	(1,'operation1','2021-03-08 16:54:36','Operation 1','Operation 1','2021-03-08 16:54:36'),
	(2,'RESERVE_INVENTORY','2021-03-10 14:23:16','Reserve Inventory','RESERVE_INVENTORY','2021-03-10 19:12:13'),
	(3,'RELEASE_INVENTORY','2021-03-10 14:23:35','Release Inventory','RELEASE_INVENTORY','2021-03-10 19:13:16'),
	(4,'ACTIVATE_INVENTORY','2021-03-10 14:23:51','Activate Inventory','ACTIVATE_INVENTORY','2021-03-10 19:12:41'),
	(5,'TRANSFER_INVENTORY','2021-03-10 14:24:07','Transfer Inventory','TRANSFER_INVENTORY','2021-03-10 19:13:34'),
	(6,'INTRANSIT_INVENTORY','2021-03-10 14:24:21','Intransit Inventory','INTRANSIT_INVENTORY','2021-03-10 19:13:00'),
	(7,'DEPOSIT_INVENTORY','2021-03-10 14:25:27','Deposit Inventory','DEPOSIT_INVENTORY','2021-03-10 19:14:16'),
	(8,'DELIVER_INVENTORY','2021-03-10 14:26:18','Deliver Inventory','DELIVER_INVENTORY','2021-03-10 19:13:58'),
	(9,'DMA\'D_INVENTORY','2021-03-12 19:33:13','DMA\'D Inventory','DMA\'D Inventory','2021-03-12 19:33:13'),
	(10,'SIM_SUSPEND','2021-03-08 16:54:36','Suspend','Suspend','2021-03-08 16:54:36'),
	(11,'PINGED_INVENTORY','2021-03-08 16:54:36','Pinged Inventory','Pinged Inventory','2021-03-08 16:54:36'),
	(12,'INVALIDATE_INVENTORY','2021-03-08 16:54:36','INVALIDATE_INVENTORY','INVALIDATE_INVENTORY','2021-03-08 16:54:36'),
	(13,'REVERSE_INVENTORY','2021-03-08 16:54:36','REVERSE_INVENTORY','REVERSE_INVENTORY','2021-03-08 16:54:36');


# Dump of table product
# ------------------------------------------------------------
TRUNCATE TABLE `product`;
INSERT INTO `product` (`product_id`, `code`, `name`, `description`, `product_type_id`, `workflow_id`, `supplier_id`, `image_url`, `product_status_id`, `is_category`, `data`, `created_at`, `updated_at`, `available_from`, `available_until`, `path`)
VALUES
	(14, NULL, 'Physical Products', 'Physical Products', NULL, NULL, NULL, '', 1, 1, X'7B7D', '2023-07-04 00:00:00', '2023-07-04 13:04:16', '2023-07-04', '2030-12-31', NULL),
	(15, 'Data SIM', 'Data SIM', 'Data SIM', 1, 1, 1, 'http://10.91.9.129/api/osm/v1/resource/inventory/50a17edf-39f1-4cf4-a28b-6581a768c570.jpeg', NULL, 0, X'7B7D', '2023-07-04 00:00:00', '2023-07-04 13:04:18', '2023-07-04', '2030-12-31', 'Physical Products/Data SIM/'),
	(26, 'Voice', 'Voice', 'Voice', 1, 1, 1, '', NULL, 0, X'7B7D', '2023-07-04 00:00:00', '2023-07-04 13:04:20', '2023-07-04', '2030-12-31', 'Physical Products/Voice/'),
	(112, 'LTE SIM', 'LTE SIM', 'LTE SIM', 1, 1, 1, '', NULL, 0, X'7B7D', '2023-07-04 00:00:00', '2023-07-04 13:04:21', '2023-07-04', '2030-12-31', 'Physical Products/LTE SIM/');



# Dump of table product_closure
# ------------------------------------------------------------
TRUNCATE TABLE `product_closure`;
INSERT INTO `product_closure` (`ancestor`, `descendant`, `depth`)
VALUES
	(14, 14, 0),
	(14, 15, 1),
	(14, 26, 1),
	(14, 112, 1),
	(15, 15, 0),
	(26, 26, 0),
	(112, 112, 0);



# Dump of table product_status
# ------------------------------------------------------------
TRUNCATE TABLE `product_status`;
INSERT INTO `product_status` (`product_status_id`, `code`, `description`)
VALUES
	(1,'available','Product is available'),
	(2,'blocked','Product is blocked'),
	(3,'decommissioned','Product is decommissioned');


# Dump of table product_tax_map
# ------------------------------------------------------------
TRUNCATE TABLE `product_tax_map`;
INSERT INTO `product_tax_map` (`product_id`, `tax_id`)
VALUES
	(15, 2),
	(15, 4),
	(26, 2),
	(112, 2),
	(112, 4);


# Dump of table product_type
# ------------------------------------------------------------
TRUNCATE TABLE `product_type`;
INSERT INTO `product_type` (`product_type_id`, `code`, `description`)
VALUES
	(1,'serialised','Type is used for serialised products'),
	(2,'non-serialised','Type is used for non-serialised products'),
	(3,'services','Type is used for services products'),
	(4,'trackable-non-serialised','Type is used for trackable products'),
	(5,'services-b2b','Type is used for services related to B2B'),
	(6,'services-topup','Type is used for services related to Topup operations'),
	(7,'services-custom','Type is used for custom services operations'),
	(8,'batch','Type is used for batch products'),
	(9,'services-data-bundle','Type is used for Databundle products');

# Dump of table product_variant
# ------------------------------------------------------------
TRUNCATE TABLE `product_variant`;
INSERT INTO `product_variant` (`variant_id`, `product_id`, `product_sku`, `product_variant_name`, `product_variant_description`, `supplier_ref`, `EAN`, `variant_status_id`, `currency`, `image_url`, `data`, `unit_price`, `variable_price`, `uom`, `available_from`, `available_until`, `created_at`, `updated_at`)
VALUES
	(9, 15, 'Data SIM', '', '', '1', 'Data SIM', 1, 'BDT', NULL, X'7B22557073656C6C50726F647563744F7074696F6E223A226E6F222C22766F6C756D65223A7B22756E6974223A22222C2276616C7565223A302E307D2C22756F6D223A7B22756E6974223A22222C227175616E74697479223A302E307D2C22496E7465726E6174696F6E616C20526F616D696E67223A224E6F222C227465726D73223A22222C22776569676874223A7B22756E6974223A22222C2276616C7565223A302E307D2C2277617272616E7479223A7B22756E6974223A22222C2276616C7565223A302E307D7D', 0.00, 0, NULL, '2023-07-04', '2030-12-31', '2023-07-04 00:00:00', '2023-07-04 13:04:44'),
	(15, 26, 'Voice', NULL, NULL, '', 'Voice', 1, 'BDT', NULL, X'7B22557073656C6C50726F647563744F7074696F6E223A226E6F222C22766F6C756D65223A7B22756E6974223A22222C2276616C7565223A302E307D2C22756F6D223A7B22756E6974223A22222C227175616E74697479223A6E756C6C7D2C227465726D73223A22222C22776569676874223A7B22756E6974223A22222C2276616C7565223A302E307D2C2277617272616E7479223A7B22756E6974223A22222C2276616C7565223A302E307D7D', 0.00, 0, NULL, '2023-07-04', '2030-12-31', '2023-07-04 00:00:00', '2023-07-04 13:04:46'),
	(77, 112, 'LTE SIM', '', '', '1', 'LTE SIM', 1, 'BDT', '', X'7B22557073656C6C50726F647563744F7074696F6E223A226E6F222C22766F6C756D65223A7B22756E6974223A22222C2276616C7565223A302E307D2C22756F6D223A7B22756E6974223A22222C227175616E74697479223A302E307D2C227465726D73223A22222C22776569676874223A7B22756E6974223A22222C2276616C7565223A302E307D2C2277617272616E7479223A7B22756E6974223A22222C2276616C7565223A302E307D7D', 0.00, 0, NULL, '2023-07-04', '2030-12-31', '2023-07-04 00:00:00', '2023-07-04 13:04:47');

# Dump of table state
# ------------------------------------------------------------
TRUNCATE TABLE `state`;
INSERT INTO `state` (`state_id`, `available_from`, `available_until`, `code`, `description`, `name`)
VALUES
	(1,'2020-01-01','2030-01-01','Available','Available State','Available'),
	(2,'2020-01-01','2030-01-01','Reserved','Reserved State','Reserved'),
	(3,'2020-01-02','2030-01-01','active','Active state','Active'),
	(4,'2020-01-02','2030-01-01','Deposited','Deposited','Deposited'),
	(5,'2020-01-02','2030-01-01','Delivered','Delivered','Delivered'),
	(6,'2020-01-02','2030-01-01','In Transit','In Transit','Locked'),
	(7,'2020-01-01','2030-01-01','Sold','Sold','Sold'),
	(8,'2020-01-01','2030-01-01','ping','Ping State','Ping'),
	(9,'2020-01-01','2030-01-01','dma','Dma State','DMA'),
	(10,'2020-01-01','2030-01-01','Unavailable','For missing, stolen, or lost items','Unavailable');

# Dump of table state_transition
# ------------------------------------------------------------
TRUNCATE TABLE `state_transition`;
INSERT INTO `state_transition` (`state_transition_id`, `available_from`, `available_until`, `business_actions`, `business_rules`, `from_state_id`, `operation_id`, `to_state_id`)
VALUES
	(1,'2020-01-01','2030-01-01','','',1,1,2),
	(2,'2020-01-02','2030-01-01','','',1,2,2),
	(3,'2020-01-02','2030-01-01','','',1,4,3),
	(4,'2020-01-02','2030-01-01','','',2,6,6),
	(5,'2020-01-02','2030-01-01','','',2,3,1),
	(6,'2020-01-02','2030-01-01','','',6,5,1),
	(7,'2020-01-02','2030-01-01','','',8,4,3),
	(8,'2020-01-02','2030-01-01','','',6,8,5),
	(9,'2020-01-02','2030-01-01','','',6,7,4),
	(10,'2020-01-01','2030-01-01','','',2,5,1),
	(11,'2020-01-01','2030-01-01','','',1,9,9),
	(12,'2020-01-01','2030-01-01','','',2,10,5),
	(13,'2020-01-02','2030-01-01','','',1,11,9),
	(14,'2020-01-02','2030-01-01','','',5,4,1),
	(15,'2020-01-02','2030-01-01',NULL,NULL,1,5,1),
	(16,'2020-01-02','2030-01-01',NULL,NULL,1,8,1),
	(17,'2020-01-02','2030-01-01',NULL,NULL,6,8,1),
	(18,'2020-01-02','2030-01-01',NULL,NULL,6,6,1),
	(19,'2020-01-01','2030-01-01','','',2,5,7),
	(20,'2020-01-01','2030-01-01','','',4,6,6),
	(21, '2020-01-02', '2030-01-01', '', '', 1, 11, 8),
	(23, '2020-01-02', '2030-01-01', '', '', 9, 4, 3),
	(24, '2020-01-02', '2030-01-01', '', '', 9, 11, 8),
	(25, '2020-01-02', '2030-01-01', '', '', 8, 11, 8),
	(26, '2020-01-01', '2030-01-01', '', '', 8, 9, 9),
	(27, '2020-01-01', '2030-01-01', '', '', 9, 9, 9);


# Dump of table supplier
# ------------------------------------------------------------
TRUNCATE TABLE `supplier`;
INSERT INTO `supplier` (`supplier_id`, `code`, `name`, `description`, `data`, `created_stamp`, `updated_stamp`)
VALUES
	(1,'operator','operator','this is the default supplier for seamless one',NULL,'2022-11-02 15:13:28','2022-11-02 15:13:28');

# Dump of table tax
# ------------------------------------------------------------
TRUNCATE TABLE `tax`;
INSERT INTO `tax` (`tax_id`, `tax_type`, `description`, `percent_value`, `fixed_value`, `data`, `created_stamp`, `updated_stamp`)
VALUES
	(2,'VAT','Value-added tax',2.00,0.00,X'7B7D','2022-11-23 09:33:39','2022-11-23 09:35:30'),
	(4,'Tax','abcd',0.00,10.00,X'7B7D','2022-11-24 06:29:23','2023-01-12 13:03:52');

# Dump of table variant_status
# ------------------------------------------------------------
TRUNCATE TABLE `variant_status`;
INSERT INTO `variant_status` (`variant_status_id`, `code`, `description`)
VALUES
	(1,'available','Product variant is available'),
	(2,'blocked','Product variant is blocked'),
	(3,'decommissioned','Product variant is decommissioned');


# Dump of table workflow
# ------------------------------------------------------------
TRUNCATE TABLE `workflow`;
INSERT INTO `workflow` (`workflow_id`, `created_at`, `description`, `name`, `updated_at`)
VALUES
	(1,'2021-05-15 08:03:27','Standard Product Workflow','Standard Product Workflow','2021-05-15 08:03:27');

# Dump of table workflow_state_transition
# ------------------------------------------------------------
TRUNCATE TABLE `workflow_state_transition`;
INSERT INTO `workflow_state_transition` (`id`, `state_transition_id`, `workflow_id`)
VALUES
	(1,1,1),
	(2,2,1),
	(3,3,1),
	(4,4,1),
	(5,5,1),
	(6,6,1),
	(7,7,1),
	(8,8,1),
	(9,9,1),
	(10,10,1),
	(11,15,1),
	(12,16,1),
	(13,11,1),
	(14,17,1),
	(15,18,1),
	(16,19,1),
	(17,20,1),
	(18,21,1),
	(19,23,1),
	(20,24,1),
    (21, 25, 1),
	(22, 26, 1),
	(23, 27, 1);


SET FOREIGN_KEY_CHECKS = 1;