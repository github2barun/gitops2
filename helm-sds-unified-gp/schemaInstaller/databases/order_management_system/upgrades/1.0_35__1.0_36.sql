SELECT `id` INTO @paymentWaitConfirm
FROM `order_states` WHERE `order_state` = 'PAYMENT_WAIT_CONFIRM';
SELECT `id` INTO @transferWaitConfirm
FROM `order_states` WHERE `order_state` = 'TRANSFER_WAIT_CONFIRM';

update invoice i
set i.`status`='PENDING_CONFIRMATION'
where i.`invoice_id` in (
  select i.`invoice_id`
  from invoice i
    join orders on i.`order_id` = orders.`order_id`
  where orders.order_state
        in (@paymentWaitConfirm, @transferWaitConfirm) and i.`status`='PENDING'
);