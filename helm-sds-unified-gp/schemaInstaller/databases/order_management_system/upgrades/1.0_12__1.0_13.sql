UPDATE `orders`
SET `order_data` = JSON_SET(order_data, '$.transactionType', 'INSTANT_SALES_ORDER_ST')
WHERE `order_type` = 'ISO_ST';

UPDATE `orders`
SET `order_data` = JSON_SET(order_data, '$.transactionType', 'INSTANT_PURCHASE_RETURN_ORDER_ST')
WHERE `order_type` = 'IPRO_ST';

UPDATE `orders`
SET `order_data` = JSON_SET(order_data, '$.transactionType', 'INSTANT_SALES_RETURN_ORDER_ST')
WHERE `order_type` = 'ISRO_ST';

UPDATE `orders`
SET `order_data` = JSON_SET(order_data, '$.transactionType', 'INSTANT_SALES_ORDER')
WHERE `order_type` = 'ISO';