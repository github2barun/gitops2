CREATE TABLE `processed_chunk`
(
    `import_id`            BIGINT       NOT NULL COMMENT 'Unique identifier of the import',
    `chunk_id`             BIGINT       NOT NULL COMMENT 'chunk id',
    `result`               json CHECK (JSON_VALID(result)),
    PRIMARY KEY (`import_id`,`chunk_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
