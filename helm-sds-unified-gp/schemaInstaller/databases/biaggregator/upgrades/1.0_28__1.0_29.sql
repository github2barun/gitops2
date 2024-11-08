DROP TABLE IF EXISTS `bi`.`tpk_channel_transaction_mapping_master`;
CREATE TABLE `bi`.`tpk_channel_transaction_mapping_master` (
`ers_name` varchar(60) NOT NULL,
`tp_name` varchar(60) NOT NULL,
`tp_code` int not null,
`type` varchar(50) NOT NULL,
PRIMARY KEY (`ers_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Default values for above table */
INSERT INTO `bi`.`tpk_channel_transaction_mapping_master` (`ers_name`,`tp_name`, `tp_code`,`type`) VALUES
('API','WS', 7,'Channel')
,('BROADBAND','Subaccount Recharge', 93,'Transaction')
,('DATA_BUNDLE','Subaccount Recharge', 93,'Transaction')
,('EASY_CARD','Subaccount Recharge', 93,'Transaction')
,('GAMINGBOX','Subaccount Recharge', 93,'Transaction')
,('ILAQAYI_OFFER','Subaccount Recharge', 93,'Transaction')
,('Mobile-APP','WS', 7,'Channel')
,('POSTPAID_TOPUP','Normal Recharge', 1,'Transaction')
,('REVERSE_TOPUP','Recharge Rollback', 505,'Transaction')
,('SMS','SMS', 2,'Channel')
,('SPL_BUNDLE','Subaccount Recharge', 93,'Transaction')
,('TOPUP','Normal Recharge', 1,'Transaction')
,('USSD','USSD', 1,'Channel')
,('VOICE_OFFER','Subaccount Recharge', 93,'Transaction');