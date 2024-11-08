START TRANSACTION;

CREATE
DATABASE IF NOT EXISTS access_management;

USE access_management;



SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall`
(
    `VersionKey`    smallint(6) NOT NULL AUTO_INCREMENT,
    `Version`       varchar(20)  NOT NULL,
    `Status`        tinyint(4) NOT NULL DEFAULT '0',
    `Script`        varchar(200) NOT NULL,
    `last_modified` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `policy`;
CREATE TABLE `policy`
(
    id              smallint(6) NOT NULL AUTO_INCREMENT,
    name            varchar(80)                        NOT NULL,
    description     text NULL DEFAULT NULL,
    available_from  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL,
    available_until datetime DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `module_endpoints`;
CREATE TABLE `module_endpoints`
(
    id              smallint (8) NOT NULL AUTO_INCREMENT,
    module          varchar(80)                            NOT NULL,
    module_feature  varchar(100) DEFAULT NULL,
    component_name  varchar(1000)                          NOT NULL,
    channel         VARCHAR(50)  DEFAULT 'ALL',
    endpoint        text                                   NOT NULL,
    http_method     varchar(50)                            NOT NULL,
    content_type    varchar(80)                            NOT NULL,
    description     text NULL DEFAULT NULL,
    available_from  datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    available_until datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    default_flag tinyint(4) NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `policy_endpoint_map`;
CREATE TABLE `policy_endpoint_map`
(
    id          smallint (8) NOT NULL AUTO_INCREMENT,
    policy_id   smallint(6) NOT NULL,
    endpoint_id smallint (8) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_policy_endpoint_map_policy_id`
        FOREIGN KEY (`policy_id`) REFERENCES `policy` (`id`)
            ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_policy_endpoint_map_endpoint_id`
        FOREIGN KEY (`endpoint_id`) REFERENCES `module_endpoints` (`id`)
            ON DELETE NO ACTION ON UPDATE NO ACTION,
    UNIQUE KEY `policy_endpoint_map_unique` (`policy_id`,`endpoint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `resellertype_policy_map`;
CREATE TABLE `resellertype_policy_map`
(
    id            smallint (8) NOT NULL AUTO_INCREMENT,
    reseller_type varchar(80) NOT NULL,
    user_role varchar(80) DEFAULT NULL,
    policy_id     smallint(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_resellertype_policy_map_policy_id`
        FOREIGN KEY (`policy_id`) REFERENCES `policy` (`id`)
            ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `app_features`;
CREATE TABLE app_features
(
    id                  INT          NOT NULL AUTO_INCREMENT,
    feature_code        VARCHAR(100) NOT NULL,
    feature_description VARCHAR(100) NULL,
    feature_value       MEDIUMTEXT,
    enabled             TINYINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_features_code UNIQUE (feature_code)
) ENGINE = InnoDB
AUTO_INCREMENT = 1
DEFAULT CHARACTER SET = utf8mb3;


DROP TABLE IF EXISTS `app_features_hierarchy`;
CREATE TABLE app_features_hierarchy
(
    id                  INT(8) NOT NULL AUTO_INCREMENT,
    app_type            VARCHAR(45)  NOT NULL,
    feature_code        VARCHAR(100) NOT NULL,
    parent_feature_code VARCHAR(100) NULL DEFAULT NULL,
    feature_value       MEDIUMTEXT NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_features_hierarchy (feature_code,app_type),
    INDEX               fk_policy_feature_map_feature_code_idx (feature_code ASC),
    INDEX               fk_app_feature_hierarchy_feature_parent_code_idx (parent_feature_code ASC),
    CONSTRAINT fk_policy_feature_map_feature_code
        FOREIGN KEY (feature_code)
            REFERENCES access_management.app_features (feature_code)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
    CONSTRAINT fk_app_feature_hierarchy_feature_parent_code
        FOREIGN KEY (parent_feature_code)
            REFERENCES access_management.app_features (feature_code)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
) ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

DROP TABLE IF EXISTS `app_hierarchy`;
CREATE TABLE app_hierarchy
(
    id                       INT(11) NOT NULL AUTO_INCREMENT,
    app_feature_hierarchy_id INT(8) NOT NULL,
    policy_id                SMALLINT(6) NOT NULL,
    enabled                  TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX                    fk_policy_app_feature_idx (policy_id ASC),
    INDEX                    fk_app_feature_hierarchy_id_idx (app_feature_hierarchy_id ASC),
    CONSTRAINT fk_policy_app_feature
        FOREIGN KEY (policy_id)
            REFERENCES access_management.policy (id)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
    CONSTRAINT fk_app_feature_hierarchy_id
        FOREIGN KEY (app_feature_hierarchy_id)
            REFERENCES access_management.app_features_hierarchy (id)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
) ENGINE = InnoDB
    AUTO_INCREMENT = 1
DEFAULT CHARACTER SET = utf8mb3;

DROP TABLE IF EXISTS `gateway_management`;
CREATE TABLE `gateway_management` (
                                      `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
                                      `gateway_code` varchar(30) NOT NULL DEFAULT '' UNIQUE,
                                      `gateway_type_channel` varchar(50) NOT NULL DEFAULT '',
                                      `service_port` varchar(20) DEFAULT NULL,
                                      `source_type` varchar(30) DEFAULT NULL,
                                      `login` varchar(50) DEFAULT '',
                                      `password` text NOT NULL,
                                      `content_type` varchar(30) DEFAULT NULL,
                                      `auth_type` varchar(30) DEFAULT NULL,
                                      `status` int(4) NOT NULL DEFAULT 0,
                                      `network_code` varchar(20) DEFAULT NULL,
                                      `ip_auth` text DEFAULT NULL,
                                      `created_by` varchar(50) NOT NULL DEFAULT '',
                                      `created_date` TIMESTAMP ,
                                      `modified_date` TIMESTAMP ,
                                      `modified_by` varchar(50) NOT NULL DEFAULT '',
                                      `sms_notification` varchar(50) DEFAULT NULL,
                                      `tps_control` int(11) DEFAULT NULL,
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;

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

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;