ALTER TABLE dwa_contract_margin_rules
ADD COLUMN ftl_grammar TEXT DEFAULT NULL COMMENT 'stores corresponding ftl grammar map per condition '
AFTER value_expression;

ALTER TABLE dwa_contract_margin_rules
MODIFY value_expression TEXT DEFAULT NULL COMMENT 'stores both algebraic and freemarker expression';