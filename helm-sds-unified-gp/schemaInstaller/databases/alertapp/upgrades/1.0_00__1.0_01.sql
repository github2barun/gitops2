create or replace view reseller_balance as
select 
	res.tag as resellerId, 
	dev.address as resellerMSISDN,
	rt.id as resellerTypeId, 
	acc.balance as resellerBalance,
	acc.currency as resellerCurrency,
	acc.accountTypeId as accountTypeId
from 
	Refill.commission_receivers res
inner join 
	Refill.extdev_devices dev on dev.owner_key = res.receiver_key
inner join 
	Refill.pay_prereg_accounts pay on pay.owner_key = res.receiver_key
inner join 
	Refill.reseller_types rt on rt.type_key = res.type_key
inner join 
	accounts.accounts acc on acc.accountId = pay.account_nr
where 
	res.status = 0
	and acc.status = 'Active'
	and dev.address is not null
order by 
	resellerBalance asc