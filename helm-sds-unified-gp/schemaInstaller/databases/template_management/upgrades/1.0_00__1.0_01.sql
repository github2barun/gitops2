ALTER TABLE `template`
ADD COLUMN IF NOT EXISTS `query_param` JSON NULL CHECK (JSON_VALID(query_param))
AFTER `value`;