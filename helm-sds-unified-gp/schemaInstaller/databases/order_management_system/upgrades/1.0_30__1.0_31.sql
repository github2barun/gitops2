SET @orderState = 'REVERSED',
@orderState_Description = 'Indicates that the order has been reversed';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderType = 'ISO';
SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'TRANSFERRED';
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

SET @orderType = 'ISO';
  SELECT `id` INTO @fromStateId
  FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
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

SET @orderType = 'ISO';
      SELECT `id` INTO @fromStateId
      FROM `order_states` WHERE `order_state` = 'TRANSFER_WAIT_CONFIRM';
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