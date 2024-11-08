DROP TABLE IF EXISTS `bi`.`distributor_wise_weekly_sales_summary`;
CREATE TABLE `bi`.`distributor_wise_weekly_sales_summary` (
    `id` varchar(255) NOT NULL DEFAULT '',
    `year` int(4) DEFAULT NULL,
    `weekNumber` int(4) DEFAULT NULL,
    `dist_id` varchar(255) DEFAULT NULL,
    `region` varchar(255) DEFAULT NULL,
    `brand` varchar(255) DEFAULT NULL,
    `ttl_sales_count` bigint(25) DEFAULT 0,
    `activation_success_count` bigint(25) DEFAULT 0,
    `activation_rejection_count` bigint(25) DEFAULT 0,
    `automatic_validation_count` bigint(25) DEFAULT 0,
    `cutoff_count` bigint(25) DEFAULT 0,
    `mnp_sales_count` bigint(25) DEFAULT 0,
    `mnp_activation_success_count` bigint(25) DEFAULT 0,
    `mnp_activation_fail_count` bigint(25) DEFAULT 0,
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
