SET @orderType_IPO = 'IPO';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_PROCESS_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SET @orderType_IPO_ST = 'IPO_ST';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO_ST, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO_ST,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO_ST, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO_ST,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_WAIT_CREATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'EXTERNAL_PROCESS_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType_IPO_ST, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType_IPO_ST,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;