# Dump of table order_reason_type
# ------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
use order_management_system;
TRUNCATE TABLE `order_reason_type`;
INSERT INTO `order_reason_type` (`code`, `type`, `description`)
VALUES
  ('BAD_PRODUCT','REJECT','Bad product received'),
  ('INCORRECT_PRODUCT','RETURN','Incorrect product received'),
  ('OTHER','REJECT','Desribe what\'s wrong with the item'),
  ('OTHER','RETURN','Describe what\'s wrong with the item');



TRUNCATE TABLE `user_payment_agreement_mapping`;

INSERT INTO `user_payment_agreement_mapping` (`reseller_type`, `reseller_id`, `order_type`, `payment_agreement`)
VALUES
    ('operator', '', 'ISO_ST', 'NA'),
    ('Distributor', '', 'ISO_ST', 'NA'),
	('RWM', '', 'ISO_ST', 'NA'),
	('Mentor', '', 'ISO_ST', 'NA'),
    ('Agent', '', 'ISO_ST', 'NA');


# Dump of table user_payment_mode_mapping
# ------------------------------------------------------------

TRUNCATE TABLE `user_payment_mode_mapping`;

INSERT INTO `user_payment_mode_mapping` (`reseller_type`, `reseller_id`, `order_type`, `payment_mode`)
VALUES
    ('operator', '', 'ISO_ST', 'NO_PAYMENT_REQD'),
    ('Distributor', '', 'ISO_ST', 'NO_PAYMENT_REQD'),
	('RWM', '', 'ISO_ST', 'NO_PAYMENT_REQD'),
	('Mentor', '', 'ISO_ST', 'NO_PAYMENT_REQD'),
    ('Agent', '', 'ISO_ST', 'NO_PAYMENT_REQD');

TRUNCATE TABLE `order_type_category`;
INSERT INTO `order_type_category` (`order_category_name`, `order_type`, `order_category`, `description`, `label`)
VALUES
  ('ISO_ST', 'ISO_ST', 'STOCK_TRANSFER', 'Instant Sales Order Stock Transfer', 'Instant Sales Order Stock Transfer');

TRUNCATE TABLE `order_type`;
INSERT INTO `order_type` (`order_type`, `description`)
VALUES
  ('ISO_ST', 'Instant Sales Order - Stock Transfer');

TRUNCATE TABLE `order_states`;
INSERT INTO `order_states` (`id`, `order_state`, `description`)
VALUES
  (1, 'CREATED', 'initial state on order creation'),
  (2, 'TRANSFER_WAIT_CONFIRM', 'order waiting confirmation'),
  (3, 'TRANSFERRED', 'order completed - stock transferred'),
  (4, 'TRANSFER_REJECTED', 'order rejected'),
  (5, 'RETURN_TRANSFERRED', 'order return complete'),
  (6, 'RETURN_TRANSFER_WAIT_CONFIRM', 'order return waiting confirmation'),
  (7, 'RETURN_TRANSFER_REJECTED', 'return order rejected'),
  (8, 'EXTERNAL_CREATED', 'external order creation state'),
  (9, 'EXTERNAL_RETURN_TRANSFER_WAIT_CONFIRM', 'order return waiting confirmation external'),
  (10, 'EXTERNAL_RETURN_TRANSFERRED', 'order return complete external'),
  (11, 'EXTERNAL_RETURN_TRANSFER_REJECTED', 'return order rejected external'),
  (12, 'PARTIALLY_TRANSFERRED', 'order partially completed'),
  (13, 'PAYMENT_WAIT_CONFIRM', 'order payment waiting confirmation'),
  (14, 'PAYMENT_FAILED', 'order payment failed'),
  (15, 'EXTERNAL_WAIT_CREATED', 'initial state of external order creation'),
  (16, 'EXTERNAL_CREATED_WITH_ERROR', 'some error occurred after external order created'),
  (17, 'PENDING_APPROVAL', 'Indicates that the order is still pending approval by the user'),
  (18, 'REJECTED', 'Indicates that the order has been rejected by the user'),
  (19, 'SUBMITTED', 'Indicates order has completed the purchase process and has been submitted to the order management system'),
  (20, 'RESERVED_WAIT_CONFIRM', 'Indicates that the order is reserved for the receiver and is pending confirmation'),
  (21, 'RESERVED', 'Indicates the order is reserved'),
  (22, 'RESERVE_REJECTED', 'Indicates that the order reservation is rejected'),
  (23, 'EXTERNAL_SCHEDULED', 'Indicates that the external order is schedule'),
  (24, 'EXTERNAL_PROCESS_FAILED', 'Indicates that the external order failed to process'),
  (25, 'REVERSED', 'Indicates that the order has been reversed'),
  (26, 'PAYMENT_INITIATED', 'Payment initiated for order'),
  (27, 'REVERSE_INITIATED', 'Order reverse initiated'),
  (28, 'REVERSE_FAILED', 'Order reverse failed'),
  (29, 'REVERSE_WAIT_CONFIRM', 'Order waiting confirmation for reverse'),
  (30, 'REVERSE_INCONSISTENT', 'Order reverse in inconsistent state'),
  (31, 'REVERSE_REJECTED', 'Order reverse in rejected state'),
  (32, 'RETURN_SUBMITTED', 'Return order submission status'),
  (33, 'WAITING_RESERVATION', 'Indicates that the order is waiting for reservation again due to stolen/lost inventory'),
  (34, 'RETURN_INCOMPLETE', 'Indicates that the order is returned with lost/stolen items'),
  (35, 'FAILED', 'Failed order status'),
  (36, 'EXTERNAL_APPROVED', 'External system approved the order'),
  (37, 'EXTERNAL_REJECTED', 'External system rejected the order'),
  (38, 'EXTERNAL_CLOSED', 'External system closed the order'),
  (39, 'EXTERNAL_CANCEL', 'External system canceled the order');

TRUNCATE TABLE `order_type_state_transition`;
INSERT INTO `order_type_state_transition` (`order_type`, `from_state_id`, `to_state_id`)
VALUES
  ('ISO_ST', 1, 2),
  ('ISO_ST', 1, 3),
  ('ISO_ST', 2, 3),
  ('ISO_ST', 2, 4),
  ('ISO_ST', 1, 35);

TRUNCATE TABLE `payment_mode`;
INSERT INTO `payment_mode` (`name`, `description`)
VALUES
  ('NO_PAYMENT_REQD', 'No Payment Required'),
  ('CASH', 'Cash on Delivery'),
  ('M_PESA', 'M_Pesa wallet money'),
  ('ERP', 'Externally booked'),
  ('CARD', 'Card payment'),
  ('CREDIT_NOTE', 'Credit Note Money');

TRUNCATE TABLE `payment_agreement`;
INSERT INTO `payment_agreement` (`name`, `description`)
VALUES
  ('NA', 'Not applicable'),
  ('UPFRONT', 'Payment at the time of order placement'),
  ('CONSIGNMENT', 'Payment at time of delivery'),
  ('POD', 'Pay on Delivery'),
  ('PAY_LATER', 'Pay Later');

TRUNCATE TABLE `order_transaction_category_type`;
INSERT INTO `order_transaction_category_type` (`type`, `description`)
VALUES
	('COLLECT_PAYMENT', 'collection of payment from pos'),
	('COLLECT_STOCK', 'collection of stock from warehouse'),
	('DELIVER_STOCK', 'delivery of stock to pos'),
	('DEPOSIT_PAYMENT', 'deposition of payment to warehouse'),
	('DEPOSIT_STOCK', 'deposit of stock to warehouse'),
	('SOLD_STOCK', 'selling of stock in ISO raised in trip'),
    ('MISSING_PAYMENT', 'missing payment in trip');

 SET FOREIGN_KEY_CHECKS = 1;
