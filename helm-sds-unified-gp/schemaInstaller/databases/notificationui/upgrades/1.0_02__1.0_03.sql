CREATE TABLE `message_types` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `message_type` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_MESSAGE_TYPE` (`message_type`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `transaction_messages`
ADD COLUMN `message_type_id` bigint(20) unsigned DEFAULT NULL AFTER `status_id`,
DROP INDEX UK_MSG_TAG,
ADD UNIQUE KEY `UK_MSG_TAG` (`msg_tag`,`message_type_id`),
ADD KEY `FK_MESSAGE_TYPE_ID_TO_MESSAGE_TYPE_ID` (`message_type_id`),
ADD CONSTRAINT `FK_MESSAGE_TYPE_ID_TO_MESSAGE_TYPE_ID` FOREIGN KEY (`message_type_id`) REFERENCES `message_types` (`id`);

insert into message_types (`message_type`) values ('SMS');
insert into message_types (`message_type`) values ('EMAIL');
insert into message_types (`message_type`) values ('MOBILE_PUSH');