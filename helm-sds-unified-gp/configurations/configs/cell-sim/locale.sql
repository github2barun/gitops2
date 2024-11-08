USE `cellsim`;


DROP TABLE IF EXISTS `cellid_mapping`;

CREATE TABLE `cellid_mapping` (
  `msisdn` varchar(255) NOT NULL,
  `cellid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`msisdn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

LOCK TABLES `cellid_mapping` WRITE;
/*!40000 ALTER TABLE `cellid_mapping` DISABLE KEYS */;

INSERT INTO `cellid_mapping` (`msisdn`, `cellid`)
VALUES
	('467000000001','526'),
	('467000000002','585');



DROP TABLE IF EXISTS `ersinstall`;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT 0,
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
