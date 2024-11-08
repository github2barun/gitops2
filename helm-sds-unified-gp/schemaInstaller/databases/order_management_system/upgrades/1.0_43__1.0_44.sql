SET @orderType = 'PO';

SET @orderState = 'WAITING_RESERVATION',
@orderState_Description = 'Indicates that the order is waiting for reservation again due to stolen/lost inventory';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
(@orderState, @orderState_Description)
    ON DUPLICATE KEY UPDATE
    order_state = @orderState,
    description = @orderState_Description;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'SUBMITTED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'WAITING_RESERVATION';

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


SET @orderType = 'SO';

INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
    ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

SET @orderType = 'RO';

SET @orderState = 'RETURN_INCOMPLETE',
@orderState_Description = 'Indicates that the order is returned with lost/stolen items';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
(@orderState, @orderState_Description)
    ON DUPLICATE KEY UPDATE
    order_state = @orderState,
    description = @orderState_Description;


SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'RETURN_SUBMITTED';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'RETURN_INCOMPLETE';

INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
(@orderType, @fromStateId, @toStateId)
    ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

INSERT INTO `order_transaction_category_type` (`type`, `description`)
VALUES ('MISSING_PAYMENT', 'missing payment in trip');
