create table deferred_transactions
(
    id             bigint      NOT NULL AUTO_INCREMENT,
    scheduled_date date        NOT NULL COMMENT 'UTC date of the date the import will be processed',
    status         varchar(10) NOT NULL COMMENT 'The state' CHECK (status in ('pending','processing','processed','cancelled','failed')),
    data           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`data`)),
    PRIMARY KEY (id)
);