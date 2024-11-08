# table ersinstall - stores upgrades information
# ------------------------------------------------------------

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
CREATE TABLE `object_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uploaded_by` varchar(255) NOT NULL,
  `resource_owner` varchar(255) NOT NULL,
  `uploaded_for` varchar(255) NOT NULL,
  `resource_path` varchar(255) NOT NULL,
  `resource_id` varchar(255) NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `resource_path` varchar(255),
  `data` longtext DEFAULT NULL,
  `create_date` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;