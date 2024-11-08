ALTER TABLE `accountmanagement`.`accounts`
ADD COLUMN  `loanPenaltyAmount` decimal(20,5) default '0.00000' AFTER loanAmount;

ALTER TABLE `accountmanagement`.`accounts`
MODIFY COLUMN loanDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
