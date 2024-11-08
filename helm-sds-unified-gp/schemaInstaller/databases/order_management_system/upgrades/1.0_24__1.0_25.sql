UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.canConfirmEligibleList'),
    '$.canConfirmEligibleMap',
    JSON_OBJECT("ORDER_TRANSACTION_RELATED", JSON_EXTRACT(`order_data`, '$.canConfirmEligibleList'))
) where orders.order_data <> "{}";

UPDATE `orders`
SET `order_data` = JSON_INSERT(
    JSON_REMOVE(`order_data`, '$.canRejectEligibleList'),
    '$.canRejectEligibleMap',
    JSON_OBJECT("ORDER_TRANSACTION_RELATED", JSON_EXTRACT(`order_data`, '$.canRejectEligibleList'))
) where orders.order_data <> "{}";