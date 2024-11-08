SET @orderType = 'ISO';
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
order_type = @orderType,
from_state_id = @fromStateId,
to_state_id = @toStateId;
