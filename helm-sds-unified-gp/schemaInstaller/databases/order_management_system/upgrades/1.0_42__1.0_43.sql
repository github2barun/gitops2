alter table order_type_state_transition drop constraint order_type_id_fk;
alter table order_type_state_transition add constraint order_type_id_fk FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) on delete cascade;

alter table order_type_state_transition drop constraint from_state_id_fk;
alter table order_type_state_transition add constraint from_state_id_fk FOREIGN KEY (`from_state_id`) REFERENCES `order_states` (`id`) on delete cascade;

alter table order_type_state_transition drop constraint to_state_id_fk;
alter table order_type_state_transition add constraint to_state_id_fk FOREIGN KEY (`to_state_id`) REFERENCES `order_states` (`id`) on delete cascade;

alter table orders drop constraint type_fk;
alter table orders add constraint type_fk FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) on delete cascade;

alter table orders drop constraint state_fk;
alter table orders add constraint state_fk FOREIGN KEY (`order_state`) REFERENCES `order_states` (`id`) on delete cascade;

alter table order_internal drop constraint order_id_fk;
alter table order_internal add constraint order_id_fk FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) on delete cascade;

alter table invoice drop constraint order_id_fk2;
alter table invoice add constraint order_id_fk2 FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) on delete cascade;

alter table order_reason drop constraint reject_order_id_fk;
alter table order_reason add constraint reject_order_id_fk FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) on delete cascade;

alter table user_payment_mode_mapping drop constraint order_type_fk;
alter table user_payment_mode_mapping add constraint order_type_fk FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) on delete cascade;

alter table user_payment_mode_mapping drop constraint payment_mode_fk;
alter table user_payment_mode_mapping add constraint payment_mode_fk FOREIGN KEY (`payment_mode`) REFERENCES `payment_mode` (`name`) on delete cascade;

alter table user_payment_agreement_mapping drop constraint ordr_type_fk;
alter table user_payment_agreement_mapping add constraint ordr_type_fk FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) on delete cascade;

alter table user_payment_agreement_mapping drop constraint payment_agreement_fk;
alter table user_payment_agreement_mapping add constraint payment_agreement_fk FOREIGN KEY (`payment_agreement`) REFERENCES `payment_agreement` (`name`) on delete cascade;

alter table order_transaction drop constraint tx_category_type_fk;
alter table order_transaction add constraint tx_category_type_fk FOREIGN KEY (`transaction_category`) REFERENCES `order_transaction_category_type` (`type`) on delete cascade;

alter table order_type_category drop constraint order_type_fk_key;
alter table order_type_category add constraint order_type_fk_key FOREIGN KEY (`order_type`) REFERENCES `order_type` (`order_type`) on delete cascade;

alter table order_product_quota drop constraint order_product_quota_fK;
alter table order_product_quota add constraint order_product_quota_fK FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) on delete cascade;

alter table order_product_quota_hourly drop constraint order_product_quota_rule_hourly_fK;
alter table order_product_quota_hourly add constraint order_product_quota_rule_hourly_fK FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) on delete cascade;

alter table order_product_quota_daily drop constraint order_product_quota_rule_daily_fK;
alter table order_product_quota_daily add constraint order_product_quota_rule_daily_fK FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) on delete cascade;

alter table order_product_quota_weekly drop constraint order_product_quota_weekly_rule_weekly_fk;
alter table order_product_quota_weekly add constraint order_product_quota_weekly_rule_weekly_fk FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) on delete cascade;

alter table order_product_quota_monthly drop constraint order_product_quota_rule_monthly_fK;
alter table order_product_quota_monthly add constraint order_product_quota_rule_monthly_fK FOREIGN KEY (`order_product_quota_rule_id`) REFERENCES `order_product_quota_rule` (`id`) on delete cascade;

alter table invoice_settlement drop constraint payments_stt_fk;
alter table invoice_settlement add constraint payments_stt_fk FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) on delete cascade;

alter table invoice_settlement drop constraint invoice_stt_fk;
alter table invoice_settlement add constraint invoice_stt_fk FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`) on delete cascade;

alter table credit_note drop constraint return_order_id_fK_credit;
alter table credit_note add constraint return_order_id_fK_credit FOREIGN KEY (`return_order_id`) REFERENCES `orders` (`order_id`) on delete cascade;

alter table credit_note drop constraint original_order_id_fK_credit;
alter table credit_note add constraint original_order_id_fK_credit FOREIGN KEY (`original_order_id`) REFERENCES `orders` (`order_id`) on delete cascade;
