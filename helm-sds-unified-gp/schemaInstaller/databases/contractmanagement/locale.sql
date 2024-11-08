START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;
use contractmanagement;
TRUNCATE TABLE `loc_countries`;


INSERT INTO `loc_countries` (`country_key`, `abbreviation`, `name`, `system_primary`, `date_format`, `time_format`, `decimal_separator`, `thousands_separator`, `number_format`, `number_regexp`, `primary_currency_key`, `primary_language_key`, `last_modified`, `selectable`, `country_code`, `MSISDN_significant_digits`)
    VALUES
    	(1, 'BN', 'Bangladesh', 1, 'yyyy-MM-dd', 'HH:mm', '.', ',', '', '', 1, 0, '2024-09-24 12:00:32', 1, '880', 7);



TRUNCATE TABLE `loc_currencies`;


INSERT INTO `loc_currencies` (`currency_key`, `country_key`, `abbreviation`, `name`, `symbol`, `natural_format`, `minorcur_decimals`, `minorcur_name`, `last_modified`, `selectable`, `currency_code`)
VALUES
	(1, 1, 'BDT', 'Bangladesh', 'BDT', 'BDT{0}', 2, 'BDT', '2024-09-24 13:23:41', 1, 1),
	(2, 0, '%', 'Percent', '%', '{0}%', 2, '', '2024-09-24 13:23:42', 0, 0);



TRUNCATE TABLE `pay_options`;


INSERT INTO `pay_options` (`account_type_key`, `id`, `name`, `url`, `status`, `description`, `balance_check`, `reseller_account_type`, `account_sharing_policy`, `last_modified`, `payment_currency_key`, `min_account_balance`, `max_account_balance`, `min_transaction_amount`, `max_transaction_amount`)
VALUES
	(1, 'RESELLER', 'Reseller account', 'http://localhost:8092/accountsystem', 0, 'Reseller credit account', 1, 1, 0, '2016-01-19 14:37:30', 1, 0, 0, 0, 0),
    (2, 'AIRTIME', 'Subscriber airtime account', 'http://localhost:8091/accountlinksimulator', 0, 'Subscriber prepaid airtime account', 1, 0, 0, '2021-09-15 16:27:13', 1, 0, 0, 0, 0),
	(3, 'BOOKKEEPING', 'Book Keeping account', 'http://localhost:8092/accountsystem', 0, 'Book Keeping airtime account', 1, 1, 0, '2021-09-15 16:27:13', 1, 0, 0, 0, 0);




TRUNCATE TABLE `dwa_contract_account_types`;


INSERT INTO `dwa_contract_account_types` (`id`, `account_type`, `last_modified`)
VALUES
	(1, 'Sender', '2021-07-02 11:17:02'),
	(2, 'Receiver', '2021-07-02 11:17:02'),
	(3, 'Fixed', '2021-07-02 11:17:02'),
	(4, 'ParentOfSender', '2021-07-02 11:17:02'),
	(5, 'ParentOfReceiver', '2021-07-02 11:17:02'),
	(6, 'DynamicPayee', '2021-04-27 13:55:45');



TRUNCATE TABLE `dwa_contract_audit_entries`;
INSERT INTO `dwa_contract_audit_entries` (`id`, `contract_key`, `product_key`, `entry_key`, `entry_range_key`, `from_amount`, `to_amount`, `valid_from`, `valid_until`, `margin_rule_key`, `account_id`, `pay_options_account_type_id`, `contract_account_type_id`, `value_type_id`, `value_expression`, `tag_id`)
VALUES
	(1, 1, 77, 3, 3, 1, 10000, '2023-07-05 13:03:58', '2023-07-05 13:26:36', 5, '', 1, 1, 3, 'x', 1),
	(2, 1, 77, 3, 3, 1, 10000, '2023-07-05 13:03:58', '2023-07-05 13:26:36', 6, '', 3, 2, 3, '-x', 2),
	(3, 1, 9, 1, 1, 1, 100000, '2023-07-05 12:55:31', '2023-07-06 06:56:37', 1, '', 1, 1, 3, 'x', 1),
	(4, 1, 9, 1, 1, 1, 100000, '2023-07-05 12:55:31', '2023-07-06 06:56:37', 2, '', 1, 2, 3, '-x', 2);


TRUNCATE TABLE `dwa_contract_margin_rules`;
INSERT INTO `dwa_contract_margin_rules` (`id`, `entry_key`, `entry_range_key`, `account_id`, `pay_options_account_type_id`, `contract_account_type_id`, `value_type_id`, `value_expression`, `tag_id`, `last_modified`, `ftl_grammar`, `group_id`)
VALUES
	(3, 2, 2, '', 'RESELLER', 1, 3, 'x', 1, '2023-07-05 13:03:39', 'null', NULL),
	(4, 2, 2, '', 'BOOKKEEPING', 2, 3, '-x', 2, '2023-07-05 13:03:39', 'null', NULL),
	(7, 3, 4, '', 'RESELLER', 1, 3, 'x', 1, '2023-07-05 13:26:36', 'null', NULL),
	(8, 3, 4, '', 'BOOKKEEPING', 2, 3, '-x', 2, '2023-07-05 13:26:36', 'null', NULL),
	(9, 1, 5, '', 'RESELLER', 1, 3, 'x', 1, '2023-07-06 06:56:37', 'null', NULL),
	(10, 1, 5, '', 'RESELLER', 2, 3, '-x', 2, '2023-07-06 06:56:37', 'null', NULL);


TRUNCATE TABLE `dwa_contract_price_entries`;
INSERT INTO `dwa_contract_price_entries` (`entry_key`, `contract_key`, `currency_key`, `product_key`, `valid_from`, `created_by`, `comment`, `status`, `type`, `last_modified`)
VALUES
	(1, 1, 1, 9, '2023-07-05 12:55:31', 'Operator', '', 1, 'type1', '2023-07-05 12:56:38'),
	(2, 1, 1, 15, '2023-07-05 13:01:40', 'Operator', '', 1, 'type1', '2023-07-05 13:03:39'),
	(3, 1, 1, 77, '2023-07-05 13:03:58', 'Operator', '', 1, 'type1', '2023-07-05 13:04:47');

TRUNCATE TABLE `dwa_price_entry_ranges`;
INSERT INTO `dwa_price_entry_ranges` (`entry_key`, `from_amount`, `to_amount`, `reseller_margin`, `customer_margin`, `margin_type`, `customer_bonus`, `bonus_type`, `id`, `last_modified`)
VALUES
	(2, 0, 10000, 0, 0, 0, 0, 0, 2, '2023-07-05 13:03:39'),
	(3, 0, 10000, 0, 0, 0, 0, 0, 4, '2023-07-05 13:26:36'),
	(1, 0, 100000, 0, 0, 0, 0, 0, 5, '2023-07-06 06:56:37');







TRUNCATE TABLE `dwa_contract_tags`;

INSERT INTO `dwa_contract_tags` (`id`, `tag_name`, `last_modified`)
VALUES
	(1, 'SENDER', '2022-11-07 13:07:27'),
	(2, 'RECEIVER', '2022-11-07 13:07:27'),
	(3, 'TRANSACTION_FEE', '2022-11-07 13:07:27'),
	(4, 'COMMISSION', '2022-11-07 13:07:27'),
	(5, 'ROYALTY', '2022-11-07 13:07:27'),
	(6, 'BONUS', '2022-11-07 13:07:27'),
	(7, 'TAX', '2022-11-07 13:07:27'),
	(8, 'SEAMLESS_MARGIN', '2022-11-07 13:07:27'),
	(9, 'SENDER_COMMISSION', '2022-11-07 13:07:27'),
	(10, 'SENDER_BONUS', '2022-11-07 13:07:27'),
	(11, 'RECEIVER_COMMISSION', '2022-11-07 13:07:27'),
	(12, 'RECEIVER_BONUS', '2022-11-07 13:07:27'),
	(13, 'DISCOUNT', '2022-11-07 13:07:27'),
	(14, 'RECEIVER_DISCOUNT', '2022-11-07 13:07:27');


TRUNCATE TABLE `dwa_contract_value_type`;

INSERT INTO `dwa_contract_value_type` (`id`, `value_type`, `last_modified`)
VALUES
	(1, 'Absolute', '2022-11-07 13:07:27'),
	(2, 'Percentage', '2022-11-07 13:07:27'),
	(3, 'Expression', '2022-11-07 13:07:27'),
	(4, 'FreeMarker', '2022-11-07 13:07:27');

Truncate Table commission_contracts;
INSERT INTO `commission_contracts` (`contract_key`, `id`, `name`, `description`, `country_key`, `reseller_type_key`, `cloned_from`, `contract_status`, `contract_data`, `created_by`, `created_at`, `modified_by`, `last_modified`)
VALUES
	(1, 'Default', 'Default', '', 1, 0, NULL, 1, X'7B22636F6E74726163745F74797065223A22526567756C6172222C22636F6D6D697373696F6E5F6D6F6465223A22496E7374616E74227D', 'Operator', '2023-07-04 11:44:27', NULL, '2023-07-04 11:44:27');



SET FOREIGN_KEY_CHECKS = 1;

COMMIT;