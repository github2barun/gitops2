-- NEW payment agreement
SET @pod_Name = 'PAY_LATER',
@pod_Description = 'Pay Later';
INSERT INTO `payment_agreement`
(`name`, `description`)
VALUES
  (@pod_Name, @pod_Description)
ON DUPLICATE KEY UPDATE
  name = @pod_Name,
  description = @pod_Description;