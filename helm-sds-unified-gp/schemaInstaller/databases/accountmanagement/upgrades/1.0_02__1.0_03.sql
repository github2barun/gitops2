ALTER TABLE `accountmanagement`.`accounts`
ADD COLUMN `masterOwner` VARCHAR(45) NULL AFTER `owner`;