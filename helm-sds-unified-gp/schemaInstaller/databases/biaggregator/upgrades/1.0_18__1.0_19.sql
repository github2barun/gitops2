DROP TABLE IF EXISTS `bi`.`reseller_current_status_additional_info`;
CREATE TABLE `bi`.`reseller_current_status_additional_info` (
                                                           `reseller_current_id` varchar(200) NOT NULL,
                                                           `cnic` varchar(200) NOT NULL DEFAULT '',
                                                           `msr_id` varchar(200) DEFAULT '',
                                                           `msr_name` varchar(200) DEFAULT '',
                                                           `msr_msisdn` varchar(200) DEFAULT '',
                                                           `sfr_id` varchar(200) DEFAULT '',
                                                           `sfr_name` varchar(200) DEFAULT '',
                                                           `sfr_msisdn` varchar(200) DEFAULT '',
                                                           `rso_id` varchar(200) NOT NULL DEFAULT '',
                                                           `rso_name` varchar(200) DEFAULT '',
                                                           `rso_msisdn` varchar(200) DEFAULT '',
                                                           `birthday` date,
                                                           `postal_code` varchar(50) DEFAULT '',
                                                           `contact_no`  varchar(50) DEFAULT '',
                                                           `batch_id` varchar(200) default '',
                                                           `circle` varchar(200) default '',
                                                           `max_daily_recharge_limit` varchar(200) default '',
                                                           `balance_threshold` varchar (50) default '',
                                                           PRIMARY KEY (`reseller_current_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bi`.`std_daily_transaction_summary_aggregation_additional_details`;
CREATE TABLE `bi`.`std_daily_transaction_summary_aggregation_additional_details` (
                                                                                `transaction_end_date` datetime NOT NULL,
                                                                                `transaction_date` datetime NOT NULL,
                                                                                `transaction_reference` varchar(60) NOT NULL,
                                                                                `original_ers_reference` varchar(60) DEFAULT NULL,
                                                                                `commission_class` varchar(50) DEFAULT NULL,
                                                                                `opening_balance` decimal(65,5) NOT NULL DEFAULT 0.00000,
                                                                                `client_comment` varchar(500) DEFAULT NULL,
                                                                                `resource` varchar(50) DEFAULT NULL,
                                                                                `product_name` varchar(50) DEFAULT NULL,
                                                                                `transaction_profile` varchar(50) DEFAULT NULL,
                                                                                `result_code` varchar(50) DEFAULT NULL,
                                                                                `tax_rate` decimal(65,5) DEFAULT NULL,
                                                                                `tax` decimal(65,5) DEFAULT NULL,
                                                                                `account_valid_untill` datetime DEFAULT NULL,
                                                                                `expiry_after_recharge` datetime DEFAULT NULL,
                                                                                `expiry_before_recharge` datetime DEFAULT NULL,
                                                                                PRIMARY KEY (`transaction_reference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;