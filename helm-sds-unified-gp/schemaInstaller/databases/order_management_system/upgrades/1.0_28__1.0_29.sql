SET @orderType = 'ISO_D',
@orderType_Description = 'Instant Sales Order Digital Stock - Payments Involved';
INSERT INTO `order_type`
(`order_type`, `description`)
VALUES
  (@orderType, @orderType_Description)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  description = @orderType_Description;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'CREATED';

SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'TRANSFER_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

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

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'TRANSFER_WAIT_CONFIRM';

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

SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'TRANSFER_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

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

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';

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