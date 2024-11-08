USE `inventory_management_system`;

INSERT INTO `box_type` (`id`, `name`, `parent`, `path`, `level`, `created_stamp`, `is_wrapper`)
VALUES
(1, 'pallete', NULL, 'pallete/', 1, '2023-07-10 09:51:57', 1),
(2, 'box', 1, 'pallete/box/', 2, '2023-07-10 09:51:57', 1),
(3, 'brick', 2, 'pallete/box/brick/', 3, '2023-07-10 09:51:57', 0),
(4, 'sim_box', NULL, 'sim_box/', 1, '2023-07-10 09:51:57', 0),
(5, 'system_box', NULL, 'system_box/', 1, '2023-07-10 09:51:57', 0);
