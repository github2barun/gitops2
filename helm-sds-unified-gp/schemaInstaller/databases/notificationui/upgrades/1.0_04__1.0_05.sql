ALTER TABLE `transaction_messages`
ADD COLUMN `is_deletable` varchar(5)  NOT NULL DEFAULT 'true' CHECK (is_deletable IN ('true', 'false')) AFTER `message_template_type_id`;