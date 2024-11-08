INSERT INTO `order_states` (`id`, `order_state`, `description`)
VALUES
  (36, 'EXTERNAL_APPROVED', 'External System Approved the order'),
  (37, 'EXTERNAL_REJECTED', 'External System Rejected the order'),
  (38, 'EXTERNAL_CLOSED', 'External System Closed the order'),
  (39, 'EXTERNAL_CANCEL', 'External System Canceled the order');

INSERT INTO `order_type_state_transition` (`order_type`, `from_state_id`, `to_state_id`)
VALUES
  ('IPO', 8, 36),
  ('IPO', 8, 37),
  ('IPO', 36, 38),
  ('IPO', 36, 39);