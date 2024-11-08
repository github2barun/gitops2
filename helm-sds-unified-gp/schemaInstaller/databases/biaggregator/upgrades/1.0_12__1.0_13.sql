DROP TABLE IF EXISTS `bi`.`dealer_balance_recharge_details`;
CREATE TABLE `bi`.`dealer_balance_recharge_details`
(
    `id`                    varchar(255) NOT NULL DEFAULT '',
    `seller_dealer_type`    varchar(200)          DEFAULT NULL,
    `seller_msisdn`         varchar(200)          DEFAULT NULL,
    `seller_terminal_id`    varchar(200)          DEFAULT NULL,
    `seller_id`             varchar(200)          DEFAULT NULL,
    `transaction_date`      timestamp,
    `buyer_reseller_type`   varchar(200)          DEFAULT NULL,
    `buyer_msisdn`          varchar(200)          DEFAULT NULL,
    `buyer_id`              varchar(200)          DEFAULT NULL,
    `amount`                bigint(25) DEFAULT NULL,
    `payment_type`          varchar(200)          DEFAULT NULL,
    `transaction_reference` varchar(200)          DEFAULT NULL,
    `source`                varchar(200)          DEFAULT NULL,
    `seller_name`           varchar(200)          DEFAULT NULL,
    `area`                  varchar(200)          DEFAULT NULL,
    `section`               varchar(200)          DEFAULT NULL,
    `city_province`         varchar(200)          DEFAULT NULL,
    `district`              varchar(200)          DEFAULT NULL,
    `status`                varchar(200)          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
