set FOREIGN_KEY_CHECKS=0;

ALTER TABLE `template` DROP INDEX `unique_template` ;
ALTER TABLE `template` ADD CONSTRAINT `unique_template` UNIQUE(component,type,version,query_param);

set FOREIGN_KEY_CHECKS=1;