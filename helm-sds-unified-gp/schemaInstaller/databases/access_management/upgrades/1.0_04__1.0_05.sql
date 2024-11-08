START TRANSACTION;

ALTER TABLE module_endpoints ALTER channel SET DEFAULT 'ALL';

update module_endpoints SET channel = 'ALL' where channel is null;

COMMIT;