ALTER TABLE `cells` drop constraint cells_ibfk_1;
ALTER TABLE `cells` MODIFY region varchar(250) NOT NULL;

ALTER TABLE `REGION` ADD COLUMN `ID` varchar(250) NOT NULL;

CREATE TABLE `cachedLocationDetail` (
    `msisdnKey` varchar(64) NOT NULL,
    `content` text DEFAULT NULL,
    `last_modified` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`msisdnKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `allowedTransfers` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `fromRegion` varchar(250) NOT NULL,
    `toRegion` varchar(250) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;