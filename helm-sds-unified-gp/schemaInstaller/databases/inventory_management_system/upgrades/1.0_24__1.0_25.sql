alter table owner add reseller_path varchar (255);
update owner set reseller_path = (select reseller_path from Refill.commission_receivers where tag = owner.`owner_id`);