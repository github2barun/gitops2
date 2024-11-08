ALTER TABLE `order_reason`
  CHANGE COLUMN `reject_reason` `reason_description` varchar(255) DEFAULT NULL,
  CHANGE COLUMN `rejected_by` `reason_provided_by` varchar(255) NOT NULL;