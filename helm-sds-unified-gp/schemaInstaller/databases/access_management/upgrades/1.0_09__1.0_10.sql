ALTER TABLE policy_endpoint_map MODIFY COLUMN id INT;

DROP TABLE IF EXISTS `master_resource`;
CREATE TABLE `master_resource`
(
    id          SMALLINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(256) NULL DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource`
(
    id          SMALLINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(256) NULL DEFAULT NULL,
    endpoints   VARCHAR(256) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `resource_mapping`;
CREATE TABLE `resource_mapping`
(
    id                 SMALLINT NOT NULL AUTO_INCREMENT,
    master_resource_id SMALLINT NOT NULL,
    resource_id        SMALLINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_resource_mapping_master_resource_id`
        FOREIGN KEY (`master_resource_id`) REFERENCES `master_resource` (`id`),
    CONSTRAINT `fk_resource_mapping_resource_id`
        FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`),
    UNIQUE KEY `resource_mapping_unique` (`master_resource_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `policy_resource_mapping_applied`;
CREATE TABLE `policy_resource_mapping_applied`
(
    id                 SMALLINT NOT NULL AUTO_INCREMENT,
    policy_id          SMALLINT NOT NULL,
    master_resource_id SMALLINT NOT NULL,
    resource_id        SMALLINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_policy_resource_mapping_applied_policy_id`
        FOREIGN KEY (`policy_id`) REFERENCES `policy` (`id`),
    CONSTRAINT `fk_policy_resource_mapping_applied_master_resource_id`
        FOREIGN KEY (`master_resource_id`) REFERENCES `master_resource` (`id`),
    CONSTRAINT `fk_policy_resource_mapping_applied_resource_id`
        FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`),
    UNIQUE KEY `policy_resource_mapping_applied_unique` (`policy_id`,`master_resource_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;