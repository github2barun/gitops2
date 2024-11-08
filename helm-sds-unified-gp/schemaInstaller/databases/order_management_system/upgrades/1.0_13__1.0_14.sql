# table order_type_category - stores the categories for order-types, can be used to configure
#  the ui related description, categorization for some ordre type
# ------------------------------------------------------------
DROP TABLE IF EXISTS `order_type_category`;

CREATE TABLE `order_type_category` (
  `order_category_name` varchar(20) NOT NULL,
  `order_type` varchar(20) NOT NULL,
  `order_category` varchar(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`order_category_name`),
  KEY `order_type_fk_key` (`order_type`),
  CONSTRAINT `order_type_fk_key` FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`),
  CONSTRAINT chk_order_category CHECK (order_category IN ('RETURN', 'REVERSAL', 'ORDER', 'STOCK_TRANSFER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;