START TRANSACTION;

CREATE
DATABASE IF NOT EXISTS object_store_manager;

USE object_store_manager;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# table object_details - stores object details
# ------------------------------------------------------------
CREATE TABLE object_store_manager.`object_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(255) NOT NULL,
  `resource_path` varchar(255),
  `resource_id` varchar(255) NOT NULL,
  `resource_owner` varchar(255) NOT NULL,
  `uploaded_by` varchar(255) NOT NULL,
  `uploaded_for` varchar(255) NOT NULL,
  `data` longtext DEFAULT NULL,
  `original_file_name` varchar(255) DEFAULT NULL,
  `create_date` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `rtype_rid_idx` (`resource_type`, `resource_id`),
  UNIQUE KEY `rtype_rown_filename_idx` (`resource_type`, `resource_owner`, `uploaded_by`, `uploaded_for`, `original_file_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;