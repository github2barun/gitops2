INSERT INTO `order_states` (`order_state`, `description`)
VALUES
	('REVERSE_INITIATED', 'Order reverse initiated'),
	('REVERSE_FAILED', 'Order reverse failed'),
	('REVERSE_WAIT_CONFIRM', 'Order waiting confirmation for reverse'),
	('REVERSE_INCONSISTENT', 'Order reverse in inconsistent state'),
	('REVERSE_REJECTED', 'Order reverse in rejected state');
	
SET @orderType = 'ISO_DST';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  

  
SET @orderType = 'ISO_D';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SET @orderType = 'ISO';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INITIATED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_FAILED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_INCONSISTENT';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;
  
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_WAIT_CONFIRM';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REVERSE_REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
  (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  from_state_id = @fromStateId,
  to_state_id = @toStateId;

