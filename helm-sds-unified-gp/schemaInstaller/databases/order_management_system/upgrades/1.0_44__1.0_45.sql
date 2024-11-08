SET @orderType = 'PO';

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'WAITING_RESERVATION';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'SUBMITTED';

INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
    ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

SET @orderType = 'SO';

INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
    ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

SET @orderType = 'SO_ST';

INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
    ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;