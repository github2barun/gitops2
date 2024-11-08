ALTER TABLE batch_pool MODIFY COLUMN `batch_status` varchar(16) NOT NULL COMMENT 'The Batch state machine' CHECK ( batch_status IN ('pending-approval', 'pending', 'processing', 'processed', 'cancelled', 'failed', 'rejected'));

ALTER TABLE bulk_imports MODIFY COLUMN schedule_type  varchar(16) DEFAULT NULL COMMENT 'One of: ''approval-pending'',''scheduledAt'', ''immediate'' or ''scheduled'' or ''rejected''' CHECK (`schedule_type` in ('scheduledAt','immediate','scheduled','approval-pending', 'rejected'));
ALTER TABLE batch_pool DROP PRIMARY KEY, ADD PRIMARY KEY (batch_id,import_id);
ALTER TABLE batch_pool ADD COLUMN	retriable_file_id       varchar(45)  DEFAULT NULL COMMENT 'The unique Object Storage id (reference id) of the retriable uploaded file';
ALTER TABLE batch_pool ADD COLUMN 	retriable_file_name     varchar(45)  DEFAULT NULL;
ALTER TABLE batch_pool ADD COLUMN   retriable_file_records  int(11)	DEFAULT NULL COMMENT 'The number of records of the retriable file, (not including the column header)';
