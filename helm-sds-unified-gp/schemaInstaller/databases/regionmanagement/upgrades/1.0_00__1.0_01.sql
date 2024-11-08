ALTER TABLE `REGION`
    ADD COLUMN `location` varchar(64);

ALTER TABLE `REGION`
    ADD COLUMN `cluster` varchar(64);

ALTER TABLE `REGION`
    ADD COLUMN `distributors` varchar(64);

ALTER TABLE `REGION`
    ADD COLUMN `ova_accounts` varchar(45);
