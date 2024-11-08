START TRANSACTION;


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



COMMIT;