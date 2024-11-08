CREATE TABLE `message_template_types` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `message_template_type` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_MESSAGE_TEMPLATE_TYPE` (`message_template_type`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `transaction_messages`
ADD COLUMN `message_template_type_id` bigint(20) unsigned DEFAULT NULL AFTER `message_type_id`,
ADD KEY `FK_MESSAGE_TEMPLATE_TYPE_ID_TO_MESSAGE_TEMPLATE_TYPE_ID` (`message_template_type_id`),
ADD CONSTRAINT `FK_MESSAGE_TEMPLATE_TYPE_ID_TO_MESSAGE_TEMPLATE_TYPE_ID` FOREIGN KEY (`message_template_type_id`) REFERENCES `message_template_types` (`id`);

insert into message_template_types (`message_template_type`) values ('DEFAULT');
insert into message_template_types (`message_template_type`) values ('CUSTOM');

update transaction_messages set message_template_type_id=1;