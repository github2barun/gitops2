ALTER table payments DROP FOREIGN KEY IF EXISTS invoice_id_fk;

ALTER table payments
MODIFY `order_id` varchar(255),
MODIFY `status` varchar(50);

INSERT INTO `order_states` (`order_state`, `description`)
VALUES
  ('PAYMENT_INITIATED', 'Payment initiated for order')
  ON DUPLICATE KEY UPDATE
    order_state = 'PAYMENT_INITIATED',
    description = 'Payment initiated for order';

SET @orderType = 'ISO';
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_INITIATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;