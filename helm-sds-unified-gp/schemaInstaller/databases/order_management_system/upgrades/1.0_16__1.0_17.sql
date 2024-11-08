SET @orderType = 'PISO',
@orderType_Description = 'Push Inventory Sales Order - Payments Not Involved';
INSERT INTO `order_type`
(`order_type`, `description`)
VALUES
  (@orderType, @orderType_Description)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  description = @orderType_Description;
  
SET @orderState = 'RESERVED_WAIT_CONFIRM',
@orderState_Description = 'Indicates that the order is reserved for the receiver and is pending confirmation';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;
  
SET @orderState = 'RESERVED',
@orderState_Description = 'Indicates that the order is reserved';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;
  
SET @orderState = 'RESERVE_REJECTED',
@orderState_Description = 'Indicates that the order reservation is rejected';
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
FROM `order_states` WHERE `order_state` = 'RESERVED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'RESERVED_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'RESERVED_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'RESERVE_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'RESERVED_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'RESERVED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;