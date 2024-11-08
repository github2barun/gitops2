SET @orderType = 'IPRO_ST';
SET @orderState = 'EXTERNAL_CREATE',
@orderState_Description = 'initial state on order creation external';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM',
@orderState_Description = 'order return waiting confirmation external';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'EXTERNAL_RETURN_TRANSFERRED',
@orderState_Description = 'order return complete external';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'EXTERNAL_RETURN_TRANSFER_REJECTED',
@orderState_Description = 'return order rejected external';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATE';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;


SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATE';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFERRED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFERRED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_RETURN_TRANSFER_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;