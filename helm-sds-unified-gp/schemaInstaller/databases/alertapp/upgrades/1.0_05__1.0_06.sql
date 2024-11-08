START TRANSACTION;

DROP TABLE IF EXISTS `alertapp`.`owner_type_resource_thresholds`;
ALTER TABLE `alertapp`.`owner_resource_thresholds` MODIFY `resource_owner_id` VARCHAR(60);

COMMIT;