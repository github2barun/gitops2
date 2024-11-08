ALTER TABLE `kyc_state_transition_permissions`
ADD COLUMN IF NOT EXISTS `criteria` VARCHAR(1000);