SET @orderType = 'IPO',
@orderType_Description = 'Instant Purchase Order - Payments Involved';
INSERT INTO `order_type`
(`order_type`, `description`)
VALUES
  (@orderType, @orderType_Description)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  description = @orderType_Description;

UPDATE `order_states`
SET `order_state` = 'EXTERNAL_CREATED', `description` = 'external order creation state'
WHERE `order_state` = 'EXTERNAL_CREATE';

SET @orderState = 'EXTERNAL_WAIT_CREATED',
@orderState_Description = 'initial state of external order creation';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'EXTERNAL_CREATED_WITH_ERROR',
@orderState_Description = 'some error occurred after external order created';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'CREATED';
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
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
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

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
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

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATED_WITH_ERROR';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SET @orderType = 'IPO_ST',
@orderType_Description = 'Instant Purchase Order - Stock Transfer';
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
FROM `order_states` WHERE `order_state` = 'TRANSFER_WAIT_CONFIRM';
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

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATED_WITH_ERROR';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_CREATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;