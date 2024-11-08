# alter old table
ALTER TABLE payments RENAME to payments_old;

# create new payments table
# ---- Table for storing payment data independent of any order
CREATE TABLE `payments` (
  `payment_id` varchar(255) NOT NULL DEFAULT '',
  `data` longtext,
  `payment_mode` varchar(255) NOT NULL COMMENT 'POD/MPesa etc.',
  `total_amount` decimal(19,2) NOT NULL,
  `available_amount` decimal(19,2) NOT NULL,
  `payment_link` varchar(255) DEFAULT NULL,
  `generated_by` varchar(255) NOT NULL,
  `payee` varchar(255) NOT NULL,
  `payer` VARCHAR(255) NOT NULL,
  `create_timestamp` datetime NOT NULL,
  `status` varchar(50) NOT NULL COMMENT 'OPEN/SETTLED/PENDING/FAILED',
  PRIMARY KEY (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#---------------------------------------------------------------------------------------

CREATE TABLE `invoice_settlement` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `payment_id` varchar(255) NOT NULL DEFAULT '',
  `invoice_id` varchar(255) NOT NULL DEFAULT '',
  `total_amount` decimal(19,2) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `payments_stt_fk` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`),
  CONSTRAINT `invoice_stt_fk` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#------------------------------------MIGRATION------------------------------------------
#for all full_paid entries make settled entries
INSERT INTO `payments` (`payment_id`, `data`, `payment_mode`, `total_amount`, `available_amount`, `payment_link`, `generated_by`, `payee`,
 `payer`, `status`, `create_timestamp`)
select `payment_id`, `data`, `payment_mode`, `total_amount`, 0, `payment_link`, `generated_by`, `payee`, `payer`, "SETTLED",
`create_timestamp` from payments_old where payments_old.`status` = 'FULL_PAID' and payments_old.`payment_mode` <> 'M_PESA';

#for payments in failed and reversed state insert as is
INSERT INTO `payments` (`payment_id`, `data`, `payment_mode`, `total_amount`, `available_amount`, `payment_link`, `generated_by`, `payee`,
 `payer`, `status`, `create_timestamp`)
select `payment_id`, `data`, `payment_mode`, `total_amount`, `total_amount`, `payment_link`, `generated_by`, `payee`, `payer`, status,
`create_timestamp` from payments_old where payments_old.`status` in ("FAILED", "REVERSED") and payments_old.`payment_mode` <> 'M_PESA';

#add settlement entries for all full_paid payments (payments in settled state in new table)
INSERT INTO `invoice_settlement` (`payment_id`, `invoice_id`, `total_amount`)
select `payment_id`, `invoice_id`, `total_amount` from payments_old where payments_old.`status` = 'FULL_PAID' and payments_old.`payment_mode` <> 'M_PESA';

#add payment entries for pending payments
INSERT INTO `payments` (`payment_id`, `data`, `payment_mode`, `total_amount`, `available_amount`, `payment_link`, `generated_by`, `payee`,
 `payer`, `status`, `create_timestamp`)
select `payment_id`, `data`, `payment_mode`, `total_amount`, 0, `payment_link`, `generated_by`, `payee`, `payer`, "SETTLED",
 `create_timestamp` from payments_old where payments_old.`status` = 'PENDING' and payments_old.`payment_mode` <> 'M_PESA';

#add settlement entries for partial_paid payments
INSERT INTO `invoice_settlement` (`payment_id`, `invoice_id`, `total_amount`)
select `payment_id`, `invoice_id`, `total_amount` from payments_old where payments_old.`status` = 'PENDING' and
payments_old.payment_link = 'make-payment' and payments_old.`payment_mode` <> 'M_PESA';

#update new credit and due in payments table
INSERT INTO `payments` (`payment_id`, `data`, `payment_mode`, `total_amount`, `available_amount`, `payment_link`, `generated_by`, `payee`,
 `payer`, `status`, `create_timestamp`)
select concat("PY-", UUID()), "{}", "CASH", `unused_credit`, `unused_credit`, "system-adjustment", "SYSTEM", `payee`,`payer`,
 "OPEN", current_timestamp() from ledger_balance where ledger_balance.unused_credit > 0;