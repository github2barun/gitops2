SET FOREIGN_KEY_CHECKS = 0;

use alertapp;

Truncate table owner_resource_thresholds;
INSERT INTO `owner_resource_thresholds` (`id`, `resource_owner_id`, `resource_owner_type`, `resource_type`, `resource_id`, `threshold`)
VALUES
	(1, 'RWM1', 'RWM', 'Physical Products', 'LTE SIM', 200);


SET FOREIGN_KEY_CHECKS = 1;
