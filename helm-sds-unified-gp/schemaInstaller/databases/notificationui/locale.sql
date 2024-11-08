CREATE DATABASE IF NOT EXISTS notificationui;

USE notificationui;
SET FOREIGN_KEY_CHECKS = 0;
Truncate table languages;
insert into languages (`code`,`name`) values ('en','English');
insert into languages (`code`,`name`) values ('fr','French');
insert into languages (`code`,`name`) values ('ar','Arabic');

truncate table recipients;
insert into recipients (`recipient_type`) values ('Initiator');
insert into recipients (`recipient_type`) values ('Sender');
insert into recipients (`recipient_type`) values ('Receiver');
insert into recipients (`recipient_type`) values ('Expression');

truncate table status;
insert into status (`status`) values ('Active');
insert into status (`status`) values ('Blocked');
insert into status (`status`) values ('Deactivated');

truncate table message_types;
insert into message_types (`message_type`) values ('SMS');
insert into message_types (`message_type`) values ('EMAIL');
insert into message_types (`message_type`) values ('MOBILE_PUSH');
insert into message_types (`message_type`) values ('WEB');

truncate table message_template_types;
insert into message_template_types (`message_template_type`) values ('DEFAULT');
insert into message_template_types (`message_template_type`) values ('CUSTOM');

INSERT INTO `notificationui`.`recipients` (`id`,`recipient_type`)
 VALUES
 (5,'Campaign');

INSERT INTO `notificationui`.`transaction_categories` (`id`,`name`)
 VALUES
 (1,'COMMISSION_PAYOUT'),
 (2,'CAMPAIGN_DESIGN_APPROVAL_NOTIFICATION'),
 (3,'CAMPAIGN_DESIGN_REJECTION_NOTIFICATION'),
 (4,'CAMPAIGN_PAYOUT_APPROVAL_NOTIFICATION'),
 (5,'CAMPAIGN_PAYOUT_REJECTION_NOTIFICATION'),
 (6,'CAMPAIGN_ANNOUNCEMENT_NOTIFICATION');

INSERT INTO `notificationui`.`transaction_classifiers` (`id`,`tag`,`transaction_classifier`)
 VALUES
 (1,'COMMISSION_MANAGEMENT_SYSTEM','COMMISSION_MANAGEMENT_SYSTEM');

INSERT INTO `notificationui`.`transaction_keywords` (`category_id`,`name`,`template_value`)
 VALUES
 (1, 'campaignId', '<#if (transaction.getData()["campaignId"])??> ${(transaction.getData()["campaignId"])}</#if>'),
 (1, 'campaignName', '<#if (transaction.getData()["campaignName"])??> ${(transaction.getData()["campaignName"])}</#if>'),
 (1, 'campaignDescription', '<#if (transaction.getData()["campaignDescription"])??> ${(transaction.getData()["campaignDescription"])}</#if>'),
 (1, 'campaignType', '<#if (transaction.getData()["campaignType"])??> ${(transaction.getData()["campaignType"])}</#if>'),
 (1, 'campaignStartDate', '<#if (transaction.getData()["campaignStartDate"])??> ${(transaction.getData()["campaignStartDate"])}</#if>'),
 (1, 'campaignEndDate', '<#if (transaction.getData()["campaignEndDate"])??> ${(transaction.getData()["campaignEndDate"])}</#if>'),
 (1, 'netPayoutAmount', '<#if (transaction.getData()["netPayoutAmount"])??> ${(transaction.getData()["netPayoutAmount"])}</#if>'),
 (1, 'kpiName', '<#if (transaction.getData()["kpiName"])??> ${(transaction.getData()["kpiName"])}</#if>'),
 (1, 'receiverName', '<#if (transaction.getData()["receiverName"])??> ${(transaction.getData()["receiverName"])}</#if>'),
 (1, 'receiverMsisdn', '<#if (transaction.getData()["receiverMsisdn"])??> ${(transaction.getData()["receiverMsisdn"])}</#if>'),
 (1, 'paymentDate', '<#if (transaction.getData()["paymentDate"])??> ${(transaction.getData()["paymentDate"])}</#if>'),
 (1, 'transactionId', '<#if (transaction.getData()["transactionId"])??> ${(transaction.getData()["transactionId"])}</#if>'),
 (2, 'campaignId', '${(transaction.getData()["campaignId"])!}'),
 (2, 'campaignName', '${(transaction.getData()["campaignName"])!}'),
 (2, 'campaignDescription', '${(transaction.getData()["campaignDescription"])!}'),
 (2, 'campaignType', '${(transaction.getData()["campaignType"])!}'),
 (2, 'campaignStartDate', '${(transaction.getData()["campaignStartDate"])!}'),
 (2, 'campaignEndDate', '${(transaction.getData()["campaignEndDate"])!}'),
 (2, 'workflowName', '${(transaction.getData()["workflowName"])!}'),
 (3, 'campaignId', '${(transaction.getData()["campaignId"])!}'),
 (3, 'campaignName', '${(transaction.getData()["campaignName"])!}'),
 (3, 'campaignDescription', '${(transaction.getData()["campaignDescription"])!}'),
 (3, 'campaignType', '${(transaction.getData()["campaignType"])!}'),
 (3, 'campaignStartDate', '${(transaction.getData()["campaignStartDate"])!}'),
 (3, 'campaignEndDate', '${(transaction.getData()["campaignEndDate"])!}'),
 (3, 'workflowName', '${(transaction.getData()["workflowName"])!}'),
 (4, 'campaignId', '${(transaction.getData()["campaignId"])!}'),
 (4, 'campaignName', '${(transaction.getData()["campaignName"])!}'),
 (4, 'campaignDescription', '${(transaction.getData()["campaignDescription"])!}'),
 (4, 'campaignType', '${(transaction.getData()["campaignType"])!}'),
 (4, 'campaignStartDate', '${(transaction.getData()["campaignStartDate"])!}'),
 (4, 'campaignEndDate', '${(transaction.getData()["campaignEndDate"])!}'),
 (4, 'workflowName', '${(transaction.getData()["workflowName"])!}'),
 (4, 'netPayoutAmount', '${(transaction.getData()["netPayoutAmount"])!}'),
 (4, 'payoutCycleDate', '${(transaction.getData()["payoutCycleDate"])!}'),
 (5, 'campaignId', '${(transaction.getData()["campaignId"])!}'),
 (5, 'campaignName', '${(transaction.getData()["campaignName"])!}'),
 (5, 'campaignDescription', '${(transaction.getData()["campaignDescription"])!}'),
 (5, 'campaignType', '${(transaction.getData()["campaignType"])!}'),
 (5, 'campaignStartDate', '${(transaction.getData()["campaignStartDate"])!}'),
 (5, 'campaignEndDate', '${(transaction.getData()["campaignEndDate"])!}'),
 (5, 'workflowName', '${(transaction.getData()["workflowName"])!}'),
 (5, 'netPayoutAmount', '${(transaction.getData()["netPayoutAmount"])!}'),
 (5, 'payoutCycleDate', '${(transaction.getData()["payoutCycleDate"])!}'),
 (6, 'campaignId', '${(transaction.getData()["campaignId"])!}'),
 (6, 'campaignName', '${(transaction.getData()["campaignName"])!}'),
 (6, 'campaignDescription', '${(transaction.getData()["campaignDescription"])!}'),
 (6, 'campaignType', '${(transaction.getData()["campaignType"])!}'),
 (6, 'campaignStartDate', '${(transaction.getData()["campaignStartDate"])!}'),
 (6, 'campaignEndDate', '${(transaction.getData()["campaignEndDate"])!}');

INSERT INTO `notificationui`.`transaction_messages` (`msg_tag`, `msg_template`, `language_id`, `recipient_id`, `classifier_id`, `status_id`, `message_type_id`, `message_template_type_id`, `is_deletable`, `activation_date`, `created_by`, `created_date`, `modified_by`, `modified_date`, `expression_value`, `condition_expression`)
VALUES
 ('emailCampaignPayoutDisbursementTemplate', 'Hi <#if (transaction.getData()[\"receiverName\"])??> ${(transaction.getData()[\"receiverName\"])}</#if>,\n\nYou have earned Commission of <#if (transaction.getData()[\"netPayoutAmount\"])??> ${(transaction.getData()[\"netPayoutAmount\"])}</#if> BDT for achieving the below campaign. \n Campaign Id: <#if (transaction.getData()[\"campaignId\"])??> ${(transaction.getData()[\"campaignId\"])}</#if>\nCampaign Name: <#if (transaction.getData()[\"campaignName\"])??> ${(transaction.getData()[\"campaignName\"])}</#if>\nCampaign Description: <#if (transaction.getData()[\"campaignDescription\"])??> ${(transaction.getData()[\"campaignDescription\"])}</#if>\nCampaign Type: <#if (transaction.getData()[\"campaignType\"])??> ${(transaction.getData()[\"campaignType\"])}</#if>\nCampaign Start Date: <#if (transaction.getData()[\"campaignStartDate\"])??> ${(transaction.getData()[\"campaignStartDate\"])}</#if>\nCampaign End Date: <#if (transaction.getData()[\"campaignEndDate\"])??> ${(transaction.getData()[\"campaignEndDate\"])}</#if>\n Kpi Name: <#if (transaction.getData()[\"kpiName\"])??> ${(transaction.getData()[\"kpiName\"])}</#if>\n Receiver Name: <#if (transaction.getData()[\"receiverName\"])??> ${(transaction.getData()[\"receiverName\"])}</#if>\n Receiver Msisdn: <#if (transaction.getData()[\"receiverMsisdn\"])??> ${(transaction.getData()[\"receiverMsisdn\"])}</#if>\n Payment Date: <#if (transaction.getData()[\"paymentDate\"])??> ${(transaction.getData()[\"paymentDate\"])}</#if>\n ref: <#if (transaction.getData()[\"transactionId\"])??> ${(transaction.getData()[\"transactionId\"])}</#if>', '1', '5', '1', '1', '2', '2', 'false', '2023-03-14 07:10:00', 'NotLoggedIn', '2023-03-14 07:12:25', 'NotLoggedIn', '2023-03-14 07:35:19', NULL, NULL),
 ('smsCampaignPayoutDisbursementTemplate', 'Hi <#if (transaction.getData()[\"receiverName\"])??> ${(transaction.getData()[\"receiverName\"])}</#if>,\n\nYou have earned Commission of <#if (transaction.getData()[\"netPayoutAmount\"])??> ${(transaction.getData()[\"netPayoutAmount\"])}</#if> BDT for achieving the below campaign. \n Campaign Id: <#if (transaction.getData()[\"campaignId\"])??> ${(transaction.getData()[\"campaignId\"])}</#if>\nCampaign Name: <#if (transaction.getData()[\"campaignName\"])??> ${(transaction.getData()[\"campaignName\"])}</#if>\nCampaign Description: <#if (transaction.getData()[\"campaignDescription\"])??> ${(transaction.getData()[\"campaignDescription\"])}</#if>\nCampaign Type: <#if (transaction.getData()[\"campaignType\"])??> ${(transaction.getData()[\"campaignType\"])}</#if>\nCampaign Start Date: <#if (transaction.getData()[\"campaignStartDate\"])??> ${(transaction.getData()[\"campaignStartDate\"])}</#if>\nCampaign End Date: <#if (transaction.getData()[\"campaignEndDate\"])??> ${(transaction.getData()[\"campaignEndDate\"])}</#if>\n Kpi Name: <#if (transaction.getData()[\"kpiName\"])??> ${(transaction.getData()[\"kpiName\"])}</#if>\n Receiver Name: <#if (transaction.getData()[\"receiverName\"])??> ${(transaction.getData()[\"receiverName\"])}</#if>\n Receiver Msisdn: <#if (transaction.getData()[\"receiverMsisdn\"])??> ${(transaction.getData()[\"receiverMsisdn\"])}</#if>\n Payment Date: <#if (transaction.getData()[\"paymentDate\"])??> ${(transaction.getData()[\"paymentDate\"])}</#if>\n ref: <#if (transaction.getData()[\"transactionId\"])??> ${(transaction.getData()[\"transactionId\"])}</#if>', '1', '5', '1', '1', '1', '2', 'false', '2023-03-14 07:12:00', 'NotLoggedIn', '2023-03-14 07:13:24', 'NotLoggedIn', '2023-03-14 07:35:19', NULL, NULL),
 ('sccCommissionSuccessNotificationTemplate', 'Hi,\n\nCommission Calculation with transaction id : \n<#if (transaction.getData()[\"txnId\"])??> ${(transaction.getData()[\"txnId\"])}</#if>\nof campaign : \n<#if (transaction.getData()[\"campaignName\"])??> ${(transaction.getData()[\"campaignName\"])}</#if>\nfor reseller : \n<#if (transaction.getData()[\"resellerName\"])??> ${(transaction.getData()[\"resellerName\"])}</#if>\nhas been successful', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('sccCommissionFailureNotificationTemplate', 'Hi,\n\nCommission Calculation with transaction id : \n<#if (transaction.getData()[\"txnId\"])??> ${(transaction.getData()[\"txnId\"])}</#if>\nof campaign : \n<#if (transaction.getData()[\"campaignName\"])??> ${(transaction.getData()[\"campaignName\"])}</#if>\nfor reseller : \n<#if (transaction.getData()[\"resellerName\"])??> ${(transaction.getData()[\"resellerName\"])}</#if>\nhas failed.\nerror : <#if (transaction.getData()[\"errorMessage\"])??> ${(transaction.getData()[\"errorMessage\"])}</#if>', 1, 5, 1, 1, 2, 1, 'true', '2022-09-14 18:48:00', 'NotLoggedIn', '2022-09-14 18:56:17', 'NotLoggedIn', '2022-09-15 17:56:29', NULL, NULL),
 ('emailCampaignAnnouncementNotificationTemplate', '', 1, 5, 1, 1, 2, 2, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsCampaignAnnouncementNotificationTemplate', '', 1, 5, 1, 1, 1, 2, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailApprovalNotificationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsApprovalNotificationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailWorkflowApprovedNotificationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsWorkflowApprovedNotificationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailWorkflowRejectedNotificationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsWorkflowRejectedNotificationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailWorkflowCompletedNotificationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsWorkflowCompletedNotificationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailAdminWorkflowCreationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsAdminWorkflowCreationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('emailCreatorWorkflowCreationTemplate', '', 1, 5, 1, 1, 2, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('smsCreatorWorkflowCreationTemplate', '', 1, 5, 1, 1, 1, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webApprovalNotificationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webWorkflowApprovedNotificationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webWorkflowRejectedNotificationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webWorkflowCompletedNotificationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webAdminWorkflowCreationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL),
 ('webCreatorWorkflowCreationTemplate', '', 1, 5, 1, 1, 4, 1, 'true', '2022-09-15 17:56:00', 'NotLoggedIn', '2022-09-15 17:57:23', 'NotLoggedIn', '2022-09-15 17:57:23', NULL, NULL);


UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
A campaign design has been created. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
A payout has been initiated. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been created.
</#switch>'
WHERE msg_tag='emailAdminWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
Please approve the design of campaign.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
Please approve the commission payout.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
Please approve the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='emailApprovalNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
You have created a campaign design. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
You initiated a payout. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
You have created the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='emailCreatorWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The payout has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been approved.
</#switch>'
WHERE msg_tag='emailWorkflowApprovedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The approval process for the campaign design is complete. The campaign is active.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The approval process for the payout is complete.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The approval process for workflow ${transaction.getData()["workflowName"]!} is complete.
</#switch>'
WHERE msg_tag='emailWorkflowCompletedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been rejected.
<#break>
<#case "SCCPAYOUT">
The payout has been rejected.
<#break>
<#case "SCC_AUTO_REJECT">
The campaign design has been automatically rejected because the approval time has expired.
<#break>
<#case "SCCPAYOUT_AUTO_REJECT">
The payout has been automatically rejected because the approval time has expired.
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been rejected.
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCC">
<#case "SCCPAYOUT">
<#case "SCC_AUTO_REJECT">
<#case "SCCPAYOUT_AUTO_REJECT">
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCCPAYOUT">
<#case "SCCPAYOUT_AUTO_REJECT">
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
</#switch>'
WHERE msg_tag='emailWorkflowRejectedNotificationTemplate';


UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
A campaign design has been created. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
A payout has been initiated. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been created.
</#switch>'
WHERE msg_tag='smsAdminWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
Please approve the design of campaign.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
Please approve the commission payout.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
Please approve the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='smsApprovalNotificationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
You have created a campaign design. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
You initiated a payout. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
You have created the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='smsCreatorWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The payout has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been approved.
</#switch>'
WHERE msg_tag='smsWorkflowApprovedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The approval process for the campaign design is complete. The campaign is active.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The approval process for the payout is complete.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The approval process for workflow ${transaction.getData()["workflowName"]!} is complete.
</#switch>'
WHERE msg_tag='smsWorkflowCompletedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been rejected.
<#break>
<#case "SCCPAYOUT">
The payout has been rejected.
<#break>
<#case "SCC_AUTO_REJECT">
The campaign design has been automatically rejected because the approval time has expired.
<#break>
<#case "SCCPAYOUT_AUTO_REJECT">
The payout has been automatically rejected because the approval time has expired.
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been rejected.
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCC">
<#case "SCCPAYOUT">
<#case "SCC_AUTO_REJECT">
<#case "SCCPAYOUT_AUTO_REJECT">
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCCPAYOUT">
<#case "SCCPAYOUT_AUTO_REJECT">
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
</#switch>'
WHERE msg_tag='smsWorkflowRejectedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
A campaign design has been created. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
A payout has been initiated. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been created.
</#switch>'
WHERE msg_tag='webAdminWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
Please approve the design of campaign.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
Please approve the commission payout.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
Please approve the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='webApprovalNotificationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
You have created a campaign design. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
You initiated a payout. It is pending approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
You have created the workflow ${transaction.getData()["workflowName"]!}.
</#switch>'
WHERE msg_tag='webCreatorWorkflowCreationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The payout has been approved. It is pending next approval.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been approved.
</#switch>'
WHERE msg_tag='webWorkflowApprovedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
The approval process for the campaign design is complete. The campaign is active.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
<#case "SCCPAYOUT">
The approval process for the payout is complete.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
<#break>
<#default>
The approval process for workflow ${transaction.getData()["workflowName"]!} is complete.
</#switch>'
WHERE msg_tag='webWorkflowCompletedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET  msg_template='Hi,

<#switch transaction.getData()["channel"]>
<#case "SCC">
The campaign design has been rejected.
<#break>
<#case "SCCPAYOUT">
The payout has been rejected.
<#break>
<#case "SCC_AUTO_REJECT">
The campaign design has been automatically rejected because the approval time has expired.
<#break>
<#case "SCCPAYOUT_AUTO_REJECT">
The payout has been automatically rejected because the approval time has expired.
<#break>
<#default>
The workflow ${transaction.getData()["workflowName"]!} has been rejected.
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCC">
<#case "SCCPAYOUT">
<#case "SCC_AUTO_REJECT">
<#case "SCCPAYOUT_AUTO_REJECT">
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}
<#break>
</#switch>
<#switch transaction.getData()["channel"]>
<#case "SCCPAYOUT">
<#case "SCCPAYOUT_AUTO_REJECT">
Payout amount: ${transaction.getData()["netPayoutAmount"]!}
Payout cycle date: ${transaction.getData()["payoutCycleDate"]!}
</#switch>'
WHERE msg_tag='webWorkflowRejectedNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

We are pleased to inform you about a new campaign.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}'
WHERE msg_tag='emailCampaignAnnouncementNotificationTemplate';

UPDATE notificationui.transaction_messages
SET msg_template='Hi,

We are pleased to inform you about a new campaign.
Campaign name: ${transaction.getData()["campaignName"]!}
Campaign Id: ${transaction.getData()["campaignId"]!}
Campaign description: ${transaction.getData()["campaignDescription"]!}
Campaign type: ${transaction.getData()["campaignType"]!}
Campaign start date: ${transaction.getData()["campaignStartDate"]!}
Campaign end date: ${transaction.getData()["campaignEndDate"]!}'
WHERE msg_tag='smsCampaignAnnouncementNotificationTemplate';

SET FOREIGN_KEY_CHECKS = 1;
