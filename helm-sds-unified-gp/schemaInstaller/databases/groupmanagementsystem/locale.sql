use groupmanagementsystem;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `group`;

INSERT INTO `group` (`group_id`, `name`, `description`, `status`, `minimum_members`, `maximum_members`, `effective_from`, `valid_until`, `created_at`, `last_modified`, `group_data`,`created_by`)
VALUES
  (1,'POS Group 1','POS Group 1','active',1,5,'2023-01-01 00:00:00','2099-07-01 00:00:00','2023-01-01 00:00:00','2023-01-01 00:00:00',X'7B2267726F757054797065223A22524547554C4152227D','operator'),
  (2,'POS Group 2','POS Group 2','active',1,5,'2023-01-01 00:00:00','2099-07-01 00:00:00','2023-01-01 00:00:00','2023-01-01 00:00:00',X'7B2267726F757054797065223A22524547554C4152227D','operator'),
  (3,'SCC_Campaign_Approval_Group_01','SCC Campaign Approval Group 01','active',1,5,'2023-01-01 00:00:00','2099-07-01 00:00:00','2023-01-01 00:00:00','2023-01-01 00:00:00',X'7B2267726F757054797065223A22524547554C4152227D','operator'),
  (4,'SCC_Campaign_Approval_Group_02','SCC Campaign Approval Group 02','active',1,5,'2023-01-01 00:00:00','2099-07-01 00:00:00','2023-01-01 00:00:00','2023-01-01 00:00:00',X'7B2267726F757054797065223A22524547554C4152227D','operator'),
  (5,'SCC_Campaign_Approval_Group_03','SCC Campaign Approval Group 03','active',1,5,'2023-01-01 00:00:00','2099-07-01 00:00:00','2023-01-01 00:00:00','2023-01-01 00:00:00',X'7B2267726F757054797065223A22524547554C4152227D','operator');


TRUNCATE TABLE `member`;

INSERT INTO `member` (`member_id`, `user_id`, `id_type`, `name`, `extra_params`)
VALUES
  (1, 'POS1-1-1-1', 'RESELLERID', 'POS1-1-1-1', X'7B7D'),
  (2, 'POS2-1-1-1', 'RESELLERID', 'POS2-1-1-1', X'7B7D'),
  (3, 'SCC_DMSR_10', 'RESELLERID', 'SCC_DMSR_10', '{"msisdn":"467002023010","email":"richa.sharma@seamless.se"}'),
  (4, 'SCC_DMSR_11', 'RESELLERID', 'SCC_DMSR_11', '{"msisdn":"467002023011","email":"richa.sharma@seamless.se"}'),
  (5, 'SCC_DMSR_12', 'RESELLERID', 'SCC_DMSR_12', '{"msisdn":"467002023012","email":"srinivas.varakala@seamless.se"}');

TRUNCATE TABLE `admin`;

INSERT INTO `admin` (`admin_id`, `user_id`, `id_type`, `name`, `extra_params`)
VALUES
  (1, 'AGENT1-1-1', 'RESELLERID', 'Agent1-1-1', X'7B7D'),
  (2, 'AGENT2-1-1', 'RESELLERID', 'Agent2-1-1', X'7B7D');

TRUNCATE TABLE `group_member`;

INSERT INTO `group_member` (`group_id`, `member_id`, `effective_from`, `effective_until`,`member_type`, `status`, `created_at`)
VALUES
  (1, 1, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (2, 2, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (3, 3, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (3, 4, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (3, 5, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (4, 3, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (4, 4, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (4, 5, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (5, 3, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (5, 4, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00'),
  (5, 5, '2023-01-01 00:00:00', '2099-07-01 00:00:00', 'member', 'active', '2023-01-01 00:00:00');


TRUNCATE TABLE `group_admin`;

INSERT INTO `group_admin` (`group_id`, `admin_id`, `effective_from`,`effective_until`, `status`, `created_at`)
VALUES
  (1,1,'2023-01-01 00:00:00','2099-07-01 00:00:00', 'active','2023-01-01 00:00:00'),
  (2,2,'2023-01-01 00:00:00','2099-07-01 00:00:00', 'active','2023-01-01 00:00:00');


TRUNCATE TABLE `operation`;

TRUNCATE TABLE `group_relation`;

TRUNCATE TABLE `workflow`;

INSERT INTO workflow (id, name, description, created_by, created_date, last_modified_date, valid_until, is_deleted)
VALUES
 ('4621c9e7-7c2c-4e7d-adff-6a318f8a9111', 'SCC_Campaign_Design_Approval_Workflow_01', 'SCC Campaign Design Approval Workflow 01', 'operator', '2023-01-01 00:00:00', '2023-01-01 00:00:00', NULL, 0),
 ('4621c9e7-7c2c-4e7d-adff-6a318f8a9222', 'SCC_Campaign_Design_Approval_Workflow_02', 'SCC Campaign Design Approval Workflow 02', 'operator', '2023-01-01 00:00:00', '2023-01-01 00:00:00', NULL, 0),
 ('4621c9e7-7c2c-4e7d-adff-6a318f8a9333', 'SCC_Campaign_Payout_Approval_Workflow_01', 'SCC Campaign Payout Approval Workflow 01', 'operator', '2023-01-01 00:00:00', '2023-01-01 00:00:00', NULL, 0),
 ('4621c9e7-7c2c-4e7d-adff-6a318f8a9444', 'SCC_Campaign_Payout_Approval_Workflow_02', 'SCC Campaign Payout Approval Workflow 02', 'operator', '2023-01-01 00:00:00', '2023-01-01 00:00:00', NULL, 0);

TRUNCATE TABLE `workflow_group_order`;

INSERT INTO groupmanagementsystem.workflow_group_order (id, workflow_id, workflow_order, group_id)
VALUES
 ('b54ac1eb-bf69-4514-b06e-5ba03346a111', '4621c9e7-7c2c-4e7d-adff-6a318f8a9111', 1, 3),
 ('b54ac1eb-bf69-4514-b06e-5ba03346a222', '4621c9e7-7c2c-4e7d-adff-6a318f8a9222', 1, 4),
 ('b54ac1eb-bf69-4514-b06e-5ba03346a333', '4621c9e7-7c2c-4e7d-adff-6a318f8a9333', 1, 5),
 ('b54ac1eb-bf69-4514-b06e-5ba03346a444', '4621c9e7-7c2c-4e7d-adff-6a318f8a9444', 1, 3);

TRUNCATE TABLE `workflow_tracker`;
  
SET FOREIGN_KEY_CHECKS = 1;