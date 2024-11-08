ALTER TABLE bi.std_daily_transaction_summary_aggregation
    ADD COLUMN `voucher_serial` varchar(50) DEFAULT NULL AFTER receiver_reseller_account_type_id,
    ADD COLUMN `sequential_number` SMALLINT(10) DEFAULT 0 AFTER voucher_serial;
