# table order_type - stores different types of primary orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_type`;

CREATE TABLE `order_type` (
  `order_type` varchar(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`order_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order_states - stores states for ONLY primary orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_states`;

CREATE TABLE `order_states` (
  `id` int(2) NOT NULL AUTO_INCREMENT,
  `order_state` varchar(60) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_state` (`order_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order_type_state_transition - stores state transitions for both internal and primary orders
# ------------------------------------------------------------
DROP TABLE IF EXISTS `order_type_state_transition`;

CREATE TABLE `order_type_state_transition` (
  `order_type` varchar(20) NOT NULL,
  `from_state_id` int(2) NOT NULL,
  `to_state_id` int(2) NOT NULL,
  PRIMARY KEY (`order_type`,`from_state_id`,`to_state_id`),
  KEY `from_state_id_fk` (`from_state_id`),
  KEY `to_state_id_fk` (`to_state_id`),
  KEY `order_type_id_fk` (`order_type`),
  CONSTRAINT `order_type_id_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`),
  CONSTRAINT `from_state_id_fk` FOREIGN KEY (`from_state_id`) REFERENCES `order_states` (`id`),
  CONSTRAINT `to_state_id_fk` FOREIGN KEY (`to_state_id`) REFERENCES `order_states` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table order -  stores primary order details
# ------------------------------------------------------------

DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders` (
  `order_id` varchar(255) NOT NULL,
  `order_type` varchar(20) NOT NULL,
  `trip_id` varchar(20) DEFAULT NULL,
  `buyer` varchar(20) NOT NULL,
  `seller` varchar(20) NOT NULL,
  `initiator` varchar(20) NOT NULL,
  `order_data` longtext NOT NULL,
  `order_state` int(2) NOT NULL,
  `create_timestamp` datetime DEFAULT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `type_fk` (`order_type`),
  KEY `state_fk` (`order_state`),
  CONSTRAINT `type_fk` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`),
  CONSTRAINT `state_fk` FOREIGN KEY (`order_state`) REFERENCES `order_states` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_internal - stores internal order details
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_internal`;

CREATE TABLE `order_internal` (
  `order_internal_id` varchar(255) NOT NULL COMMENT 'DO/Shipment ID',
  `order_type` varchar(20) DEFAULT NULL,
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `receiver` varchar(20) DEFAULT NULL,
  `order_data` longtext NOT NULL,
  `create_timestamp` datetime DEFAULT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  `order_state` int(2) NOT NULL,
  PRIMARY KEY (`order_internal_id`),
  KEY `order_id_fk` (`order_id`),
  CONSTRAINT `order_id_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# table invoice -  stores invoice details against (primary order + seller)
# ------------------------------------------------------------

DROP TABLE IF EXISTS `invoice`;

CREATE TABLE `invoice` (
  `invoice_id` varchar(255) NOT NULL DEFAULT '',
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `seller` varchar(20) NOT NULL,
  `data` longtext DEFAULT NULL,
  `payment_mode` varchar(255) NOT NULL COMMENT 'POD/MPesa etc.',
  `total_amount` decimal(19,2) NOT NULL,
  `due_amount` decimal(19,2) NOT NULL,
  `generated_by` varchar(255) NOT NULL,
  `receiver` varchar(255) NOT NULL,
  `create_timestamp` datetime NOT NULL,
  `status` varchar(10) NOT NULL COMMENT 'Paid/Due/NA etc.',
  PRIMARY KEY (`invoice_id`),
  UNIQUE KEY `order_id_seller` (`order_id`,`seller`),
  CONSTRAINT `order_id_fk2` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# table payments - stores payments made against some invoice
# ------------------------------------------------------------

DROP TABLE IF EXISTS `payments`;

CREATE TABLE `payments` (
  `payment_id` varchar(255) NOT NULL DEFAULT '',
  `invoice_id` varchar(255) NOT NULL DEFAULT '',
  `order_id` varchar(255) NOT NULL DEFAULT '' COMMENT 'Primary/DO/Shipment ID',
  `data` longtext NOT NULL,
  `payment_mode` varchar(255) NOT NULL COMMENT 'POD/MPesa etc.',
  `total_amount` decimal(19,2) NOT NULL,
  `payment_link` varchar(255) DEFAULT NULL,
  `generated_by` varchar(255) NOT NULL,
  `receiver` varchar(255) NOT NULL,
  `create_timestamp` datetime NOT NULL,
  `status` varchar(10) NOT NULL COMMENT 'Paid/Due/NA etc.',
  PRIMARY KEY (`payment_id`),
  KEY `invoice_id_fk` (`invoice_id`),
  CONSTRAINT `invoice_id_fk` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table order_states - stores states for ONLY primary orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_reject_reason`;

CREATE TABLE `order_reject_reason` (
  `id` int(2) NOT NULL AUTO_INCREMENT,
  `order_id` varchar(255) NOT NULL COMMENT 'Primary order ID',
  `reject_reason` varchar(255) DEFAULT NULL,
  `rejected_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `reject_order_id_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
