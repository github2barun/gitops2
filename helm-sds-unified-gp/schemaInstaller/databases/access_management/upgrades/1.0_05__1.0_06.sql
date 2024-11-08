ALTER TABLE gateway_management DROP INDEX login;

ALTER TABLE gateway_management MODIFY sms_notification varchar(50) null;