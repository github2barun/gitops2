START TRANSACTION;

CREATE DATABASE IF NOT EXISTS batchschedulingsystem;

USE batchschedulingsystem;
SET FOREIGN_KEY_CHECKS = 0;
-- ------------------------------------
--  Table structure for `ersinstall`
-- ------------------------------------
DROP TABLE IF EXISTS `ersinstall`;
CREATE TABLE `ersinstall`
(
    `versionKey`    smallint(6)  NOT NULL AUTO_INCREMENT,
    `version`       varchar(20)  NOT NULL,
    `status`        tinyint(4)   NOT NULL DEFAULT '0',
    `Script`        varchar(200) NOT NULL,
    `last_modified` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`VersionKey`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `processed_chunk`;
DROP TABLE IF EXISTS `batch_pool`;
DROP TABLE IF EXISTS `bulk_imports`;

CREATE TABLE `bulk_imports`
(
    `import_id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Unique identifier of the import',
    `import_file_id`       varchar(45)  NOT NULL COMMENT 'The unique Object Storage id (reference id) of the uploaded file',
    `import_file_name`     varchar(45)  DEFAULT NULL,
    `import_file_location` varchar(255) DEFAULT NULL,
    `import_file_format`   varchar(10)  DEFAULT NULL COMMENT 'The file format. Eg. CSV',
    `import_type`          varchar(20)  NOT NULL COMMENT 'Import type discriminator: Reseller, Inventory, Transaction',
    `import_subtype`       varchar(40)  DEFAULT NULL COMMENT 'Import type import_type\'s subtype: CNA, SIM',
    `import_definition`    varchar(5)   NOT NULL COMMENT 'Actions to be taken in case of import file errors aka \'skipOnError\': true or false' CHECK (import_definition IN ('true', 'false')),
    `uploaded_time`        datetime     NOT NULL COMMENT 'UTC Timestamp of upload via REST',
    `uploaded_by`          varchar(60)  DEFAULT NULL COMMENT 'The entity that perform the upload',
    `schedule_type` varchar(16) DEFAULT NULL COMMENT 'One of: ''approval-pending'',''scheduledAt'', ''immediate'' or ''scheduled'' or ''rejected''' CHECK (`schedule_type` in ('scheduledAt','immediate','scheduled','approval-pending','rejected')),
    `scheduled_time`       datetime     NOT NULL COMMENT 'UTC timestamp of the time the import will be processed',
    `bulk_import_desc`     varchar(100) DEFAULT NULL,
    `created_date`         datetime     NOT NULL COMMENT 'The DB record creation timestamp, in UTC',
    `last_modified_date`   datetime     NOT NULL COMMENT 'The DB record last updated timestamp, in UTC',
    `extra_params`         json         CHECK (JSON_VALID(extra_params)),
    PRIMARY KEY (`import_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE INDEX bulk_import__import_file_id on bulk_imports (import_file_id);
CREATE INDEX bulk_import__scheduled_time on bulk_imports (scheduled_time ASC);

CREATE TABLE `batch_pool`
(
    `batch_id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'Unique identifier of the batch',
    `import_id`          BIGINT   NOT NULL COMMENT 'The import reference id',
    `batch_status`       varchar(20) NOT NULL COMMENT 'The Batch state machine' CHECK (`batch_status` in ('pending-approval','pending','processing','processed','partially-processed','cancelled','failed','rejected','validation-failed')),
    `total_records`      int(11)  NOT NULL COMMENT 'Import file number of records (not including the column header)',
    `processed_records`  int(11)  NOT NULL COMMENT 'Batch processed records <= total_records)' CHECK ( total_records >= batch_pool.processed_records),
    `success_records`    int(11)  NOT NULL COMMENT 'Batch processed OK records <= processed_records)',
    `failed_records`     int(11)  NOT NULL COMMENT 'Batch processed KO records <= processed_records)',
    `retriable_file_id`       varchar(45)  DEFAULT NULL COMMENT 'The unique Object Storage id (reference id) of the retriable uploaded file',
    `retriable_file_name`     varchar(45)  DEFAULT NULL,
    `retriable_file_records`  int(11)	DEFAULT NULL COMMENT 'The number of records of the retriable file, (not including the column header)',
    `error_file_id`       varchar(45)  DEFAULT NULL COMMENT 'The unique Object Storage id (reference id) of the reason of failure uploaded file',
    `error_file_name`     varchar(45)  DEFAULT NULL,
    `created_date`       datetime NOT NULL COMMENT 'The DB record creation timestamp, in UTC',
    `last_modified_date` datetime NOT NULL COMMENT 'The DB record last updated timestamp, in UTC',
    `batch_desc`         varchar(100) DEFAULT NULL,
    `retry_count`        int(11)  NOT NULL COMMENT 'Batch current retry count <= max_retry_count',
    `max_retry_count`    int(11)  NOT NULL COMMENT 'Batch current maximum retry count',
    `extra_params`       json CHECK (JSON_VALID(extra_params)),
    PRIMARY KEY (`batch_id`,`import_id`),
    CONSTRAINT `import_id` FOREIGN KEY (`import_id`) REFERENCES `bulk_imports` (`import_id`) ON
        DELETE RESTRICT ON
        UPDATE CASCADE,
    CONSTRAINT `record_summary` CHECK ( total_records >= processed_records
        AND
                                        processed_records = (failed_records + success_records) )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE INDEX batch_pool__batch_status on batch_pool (batch_status);

CREATE TABLE `processed_chunk`
(
    `import_id`            BIGINT       NOT NULL COMMENT 'Unique identifier of the import',
    `chunk_id`             BIGINT       NOT NULL COMMENT 'chunk id',
    `result`               json CHECK (JSON_VALID(result)),
    PRIMARY KEY (`import_id`,`chunk_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


SET FOREIGN_KEY_CHECKS = 1;

COMMIT;