SET FOREIGN_KEY_CHECKS = 0;
use imsbridge;
TRUNCATE TABLE `user_base`;

INSERT INTO `user_base` (`id`, `repo_name`, `forget_password_url`, `change_password_url`, `expire_password_url`, `login_url`)
VALUES
	(1, 'general', 'http://localhost:8033/dms/auth/forgetPassword', 'http://localhost:8033/dms/auth/changePassword', 'http://localhost:8033/dms/auth/expirePassword', 'http://localhost:8912/principalService'),
	(2, 'ldaprepo', '', '', '', 'http://localhost:8824/ldap/authenticateUser');


TRUNCATE TABLE `type_repo_map`;

INSERT INTO imsbridge.`type_repo_map` (`reseller_type`,`repo_id`)
	SELECT  r.`id`,'1' FROM Refill.`reseller_types` r;

SET FOREIGN_KEY_CHECKS = 1;