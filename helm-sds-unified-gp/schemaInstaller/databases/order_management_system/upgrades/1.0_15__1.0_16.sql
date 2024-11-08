ALTER TABLE `orders`
  MODIFY `seller` VARCHAR(20),
  MODIFY `buyer` VARCHAR(20),
  ADD COLUMN IF NOT EXISTS `sender` VARCHAR(20) NOT NULL AFTER `seller`,
  ADD COLUMN IF NOT EXISTS `receiver` VARCHAR(20) NOT NULL AFTER `buyer`;

ALTER TABLE `invoice`
  MODIFY `receiver` varchar(20) NOT NULL,
  MODIFY `generated_by` varchar(20) NOT NULL,
  ADD COLUMN IF NOT EXISTS `buyer` VARCHAR(20) NOT NULL AFTER `seller`;

-- Data Migration
UPDATE `orders`
SET
  `sender` = `seller`, `seller` = NULL,
  `receiver` = `buyer`, `buyer` = NULL
WHERE `order_type` IN ('IPO_ST', 'IPRO_ST', 'ISO_DST', 'ISO_ST', 'ISRO_ST');

UPDATE `orders`
SET
  `sender` = `seller`,
  `receiver` = `buyer`
WHERE `order_type` IN ('IPO', 'ISO');


UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.order.seller'),
    '$.order.sender',
    JSON_EXTRACT(`order_data`, '$.order.seller')
)
WHERE `order_type` IN ('IPO_ST', 'IPRO_ST', 'ISO_DST', 'ISO_ST', 'ISRO_ST')
    AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;

UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.seller'),
    '$.sender',
    JSON_EXTRACT(`order_data`, '$.seller')
)
WHERE `order_type` IN ('IPO_ST', 'IPRO_ST', 'ISO_DST', 'ISO_ST', 'ISRO_ST')
      AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;

UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.order.buyer'),
    '$.order.receiver',
    JSON_EXTRACT(`order_data`, '$.order.buyer')
)
WHERE `order_type` IN ('IPO_ST', 'IPRO_ST', 'ISO_DST', 'ISO_ST', 'ISRO_ST')
      AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;

UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.buyer'),
    '$.receivers',
    JSON_ARRAY(JSON_EXTRACT(`order_data`, '$.buyer'))
)
WHERE `order_type` IN ('IPO_ST', 'IPRO_ST', 'ISO_DST', 'ISO_ST', 'ISRO_ST')
      AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;


UPDATE `orders`
SET `order_data` = JSON_INSERT(`order_data`,
                               '$.order.sender',
                               JSON_EXTRACT(`order_data`, '$.order.seller'),
                               '$.order.receiver', JSON_EXTRACT(`order_data`, '$.order.buyer'))
WHERE `order_type` IN ('IPO', 'ISO')
      AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;

UPDATE `orders`
SET `order_data` = JSON_INSERT(`order_data`,
                               '$.receivers',
                               JSON_ARRAY(JSON_EXTRACT(`order_data`, '$.buyer')),
                               '$.sender',
                               JSON_EXTRACT(`order_data`, '$.seller')
)
WHERE `order_type` IN ('IPO', 'ISO')
      AND `order_data` != '{}'
      AND `order_data` IS NOT NULL ;

-- UPDATE Invoice Table

UPDATE `invoice`
    SET `buyer` = `receiver`;

-- NEW States and FLOW
SET @pod_Name = 'POD',
@pod_Description = 'Pay on Delivery';
INSERT INTO `payment_agreement`
(`name`, `description`)
VALUES
  (@pod_Name, @pod_Description)
ON DUPLICATE KEY UPDATE
  name = @pod_Name,
  description = @pod_Description;

SET @orderType = 'PO',
@orderType_Description = 'Purchase Order - Payments Involved';
INSERT INTO `order_type`
(`order_type`, `description`)
VALUES
  (@orderType, @orderType_Description)
ON DUPLICATE KEY UPDATE
  order_type = @orderType,
  description = @orderType_Description;

SET @orderState = 'PENDING_APPROVAL',
@orderState_Description = 'Indicates that the order is still pending approval by the user';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'REJECTED',
@orderState_Description = 'Indicates that the order has been rejected by the user';
INSERT INTO `order_states`
(`order_state`, `description`)
VALUES
  (@orderState, @orderState_Description)
ON DUPLICATE KEY UPDATE
  order_state = @orderState,
  description = @orderState_Description;

SET @orderState = 'SUBMITTED',
@orderState_Description = 'Indicates order has completed the purchase process and has been submitted to the order management system';
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
FROM `order_states` WHERE `order_state` = 'SUBMITTED';
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
FROM `order_states` WHERE `order_state` = 'SUBMITTED';
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
FROM `order_states` WHERE `order_state` = 'SUBMITTED';
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