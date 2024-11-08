ALTER TABLE `kyc_operations`
ADD COLUMN IF NOT EXISTS `criteria` VARCHAR(1000);
ALTER TABLE `kyc_state_transitions`
ADD COLUMN IF NOT EXISTS `mandatory_business_actions` TEXT AFTER `business_rules`;