INSERT INTO `order_type` (`order_type`, `description`)
VALUES
  ('IPRO_ST', 'Instant Purchase Return Order - Stock Transfer'),
  ('ISO_DST', 'Instant Sales Order - Stock Transfer - Digital Stock'),
  ('ISO_ST', 'Instant Sales Order - Stock Transfer'),
  ('ISRO_ST', 'Instant Sales Return Order - Stock Transfer');


INSERT INTO `order_states` (`id`, `order_state`, `description`)
VALUES
  (1, 'CREATED', 'initial state on order creation'),
  (2, 'TRANSFER_WAIT_CONFIRM', 'order waiting confirmation'),
  (3, 'TRANSFERRED', 'order completed - stock transferred'),
  (4, 'TRANSFER_REJECTED', 'order rejected'),
  (5, 'RETURN_TRANSFERRED', 'order return complete'),
  (6, 'RETURN_TRANSFER_WAIT_CONFIRM', 'order return waiting confirmation'),
  (7, 'RETURN_TRANSFER_REJECTED', 'return order rejected');


INSERT INTO `order_type_state_transition` (`order_type`, `from_state_id`, `to_state_id`)
VALUES
  ('IPRO_ST', 1, 5),
  ('IPRO_ST', 1, 6),
  ('IPRO_ST', 6, 5),
  ('IPRO_ST', 6, 7),
  ('ISO_DST', 1, 3),
  ('ISO_ST', 1, 2),
  ('ISO_ST', 1, 3),
  ('ISO_ST', 2, 3),
  ('ISO_ST', 2, 4),
  ('ISRO_ST', 1, 5);
