INSERT INTO `order_states` (`id`, `order_state`, `description`)
VALUES
  (35, 'FAILED', 'Failed order status');

INSERT INTO `order_type_state_transition` (`order_type`, `from_state_id`, `to_state_id`)
VALUES
  ('IPO', 1, 35),
  ('IPO_ST', 1, 35),
  ('IPRO_ST', 1, 35),
  ('ISO_D', 1, 35),
  ('ISO_DST', 1, 35),
  ('ISO_ST', 1, 35),
  ('ISRO_ST', 1, 35),
  ('PO', 1, 35),
  ('SO', 1, 35),
  ('PISO', 21, 35),
  ('ISO', 1, 35);