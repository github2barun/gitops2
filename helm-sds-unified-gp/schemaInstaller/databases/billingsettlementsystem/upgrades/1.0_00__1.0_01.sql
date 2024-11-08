DROP TABLE IF EXISTS `automated_billing_configuration`;
-- Create syntax for TABLE 'automated_billing_configuration'
CREATE TABLE `automated_billing_configuration`
(
    `vendor`                varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The vendor name',
    `reseller`              varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The reseller uid',
    `mode`               	varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The invoice mode (manual or automatic)',
    `period`       			varchar(255) COLLATE utf8_unicode_ci 	      COMMENT 'The invoice period (daily, weekly, monthly, quarterly, yearly)',
    `initiation_date`       datetime                                      COMMENT 'The initiation date',
    `cycle_start_date`      datetime							          COMMENT 'The billing cycle start date',
    `cycle_end_date`       	datetime                                      COMMENT 'The billing cycle end date',
    `settlement_agreement`  varchar(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The settlement agreement mode (internal or external)',
    `settlement_mode`       varchar(255) COLLATE utf8_unicode_ci          COMMENT 'The settlement mode',
    PRIMARY KEY (`vendor`,`reseller`), 
    INDEX automated_billing_configuration_vendor_idx (vendor ASC),
    INDEX automated_billing_configuration_reseller_idx (reseller ASC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci
    COMMENT 'Automated billing configuration';