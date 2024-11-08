START TRANSACTION;


DROP TABLE IF EXISTS `alertapp`.`owner_type_resource_thresholds`;
CREATE TABLE IF NOT EXISTS `alertapp`.`owner_type_resource_thresholds` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `resource_owner_type` VARCHAR(60) NOT NULL,
    `resource_type` VARCHAR(255) NOT NULL,
    `resource_id` VARCHAR(255) NULL,
    `threshold` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT owner_type_resource_threshold_unq UNIQUE (resource_owner_type,resource_type,resource_id),
    INDEX `idx_ownr_type_rsrc_thrshlds_rsrc_ownr_type_rsrc_type_rsrc_id` (`resource_owner_type`, `resource_type`, `resource_id`)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `alertapp`.`owner_resource_thresholds`;
CREATE TABLE IF NOT EXISTS `alertapp`.`owner_resource_thresholds` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `resource_owner_id` VARCHAR(60) NOT NULL,
    `resource_owner_type` VARCHAR(60) NOT NULL,
    `resource_type` VARCHAR(255) NOT NULL,
    `resource_id` VARCHAR(255),
    `threshold` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT owner_resource_threshold_unq UNIQUE (resource_owner_id,resource_owner_type,resource_type,resource_id),
    INDEX `rsrc_own_id_rsrc_type_rsrc_id_idx` (`resource_owner_id`, `resource_owner_type`, `resource_type`, `resource_id`)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8;


COMMIT;