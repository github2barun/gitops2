SET @orderType = 'RO';
SET @orderType_Description = 'Return Order - Payments Involved';
INSERT INTO `order_type`
(`order_type`, `description`)
VALUES
    (@orderType, @orderType_Description)
ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    description = @orderType_Description;

SET @orderState = 'RETURN_SUBMITTED';
SET @orderState_Description = 'Return order submission status';
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
FROM `order_states` WHERE `order_state` = 'PENDING_APPROVAL';
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
FROM `order_states` WHERE `order_state` = 'RETURN_SUBMITTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
    (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PENDING_APPROVAL';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'RETURN_SUBMITTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
    (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

SELECT `id` INTO @fromStateId
FROM `order_states` WHERE `order_state` = 'PENDING_APPROVAL';
SELECT `id` INTO @toStateId
FROM `order_states` WHERE `order_state` = 'REJECTED';
INSERT INTO `order_type_state_transition`
(`order_type`, `from_state_id`, `to_state_id`)
VALUES
    (@orderType, @fromStateId, @toStateId)
ON DUPLICATE KEY UPDATE
    order_type = @orderType,
    from_state_id = @fromStateId,
    to_state_id = @toStateId;

# table credit_note -  stores credit note details against (return order + seller)
# ------------------------------------------------------------

CREATE TABLE `credit_note` (
    `credit_note_id` varchar(255) NOT NULL DEFAULT '',
    `return_order_id` varchar(255) NOT NULL COMMENT 'Return order ID',
    `original_order_id` varchar(255) NOT NULL COMMENT 'Original order ID which is going to return',
    `seller` varchar(20) NOT NULL,
    `buyer` varchar(20) NOT NULL,
    `receiver` varchar(20) NOT NULL,
    `total_amount` decimal(19,2) NOT NULL,
    `status` varchar(50) NOT NULL COMMENT 'PENDING etc.',
    `data` longtext DEFAULT NULL,
    `create_timestamp` datetime NOT NULL,
    `last_update_timestamp` datetime DEFAULT NULL,
    PRIMARY KEY (`credit_note_id`),
    UNIQUE KEY `return_order_id_seller` (`return_order_id`,`seller`),
    CONSTRAINT `return_order_id_fK_credit` FOREIGN KEY (`return_order_id`) REFERENCES `orders` (`order_id`),
    CONSTRAINT `original_order_id_fK_credit` FOREIGN KEY (`original_order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `payment_mode` (`name`, `description`)
VALUES
    ('CREDIT_NOTE', 'Credit Note Money');