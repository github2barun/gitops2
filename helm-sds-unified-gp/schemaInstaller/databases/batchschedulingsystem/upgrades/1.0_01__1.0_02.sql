ALTER TABLE batch_pool ADD COLUMN	error_file_id       varchar(45)  DEFAULT NULL COMMENT 'The unique Object Storage id (reference id) of the reason of failure uploaded file';
ALTER TABLE batch_pool ADD COLUMN 	error_file_name     varchar(45)  DEFAULT NULL;
ALTER TABLE batch_pool MODIFY batch_status varchar(20);