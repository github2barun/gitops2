ALTER TABLE `order_states` ADD COLUMN `name` VARCHAR (200);

UPDATE order_states
SET name = order_state;