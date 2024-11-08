DROP TABLE IF EXISTS `weekly_reseller_summary`;
CREATE TABLE `weekly_reseller_summary` (
    `id` varchar(255),
    `week` int(11) DEFAULT NULL,
    `year` int(11) DEFAULT NULL,
    `distributor_id` varchar(200) DEFAULT NULL,
    `total_pos` int(11) DEFAULT NULL,
    `active_pos` int(11) DEFAULT NULL,
    `locked_pos` int(11) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
