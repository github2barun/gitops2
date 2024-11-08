ALTER TABLE `orders`
ADD COLUMN IF NOT EXISTS `payment_agreement` VARCHAR(20);