DROP TABLE IF EXISTS `bi`.`reseller_registration`;
CREATE TABLE `bi`.`reseller_registration` (
                                              `id` varchar(100) NOT NULL,
                                              `parent_reseller_id` varchar(100) DEFAULT 'N/A',
                                              `reseller_id` varchar(100) DEFAULT 'N/A',
                                              `reseller_msisdn` varchar(100) DEFAULT 'N/A',
                                              `reseller_type` varchar(100) DEFAULT 'N/A',
                                              `region` varchar(100) DEFAULT 'N/A',
                                              `balance` varchar(100) DEFAULT '0',
                                              `registration_date` datetime DEFAULT NULL,
                                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;