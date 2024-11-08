ALTER TABLE `bi`.`std_daily_transaction_summary_aggregation_additional_details`
    ADD COLUMN `sender_cell_id` varchar(50) default '';

ALTER TABLE `bi`.`std_daily_transaction_summary_aggregation_additional_details`
    ADD COLUMN `receiver_cell_id` varchar(50) default '';


DROP TABLE IF EXISTS `bi`.`operator_dealer_audits_logs`;
CREATE TABLE `bi`.`operator_dealer_audits_logs` (
   `transactionReference` varchar(60) NOT NULL,
   `transactionStartDate` datetime DEFAULT NULL,
   `transactionEndDate` datetime DEFAULT NULL,
   `senderResellerID` varchar(50) DEFAULT '',
   `receiverResellerID` varchar(50) DEFAULT '',
   `resellerStatus` varchar(50) DEFAULT '',
   `comments` varchar(100) DEFAULT '',
   `transactionType` varchar(50) DEFAULT '',
   `channel` varchar(50) DEFAULT '',
   `resultStatus` varchar(50) DEFAULT '',
   `resultDescription` varchar(50) DEFAULT '',
   PRIMARY KEY (`transactionReference`),
   KEY `operator_dealer_audits_logs_index` (`transactionStartDate`,`transactionEndDate`,`transactionType`,`resultStatus`,`senderResellerID`,`receiverResellerID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;