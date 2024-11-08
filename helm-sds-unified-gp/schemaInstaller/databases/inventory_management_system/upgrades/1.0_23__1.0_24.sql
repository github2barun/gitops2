drop function if exists createUniqueBoxIdTNSI;
DELIMITER //
create function createUniqueBoxIdTNSI() returns varchar(40)
begin
    declare temp varchar(40);
    set temp = concat('B00',(select NEXTVAL(BOX_ID_SEQ)));
    while exists (select 1 from trackable_nonserialized_inventory tnsi where tnsi.`batch_id` = temp) do
            set temp = concat('B00',(select NEXTVAL(BOX_ID_SEQ)));
        end while;
    return temp;
end //

drop trigger if exists create_box_id_tnsi;
DELIMITER //
CREATE DEFINER=`refill`@`%` TRIGGER create_box_id_tnsi BEFORE UPDATE ON trackable_nonserialized_inventory
    FOR EACH ROW
BEGIN
    declare boxId varchar(40);
    declare oldBoxId varchar(40);
    IF NEW.`batch_id` IS NULL THEN
        select createUniqueBoxIdTNSI() into @boxId;
        select `batch_id` from trackable_nonserialized_inventory where id = NEW.`id` into @oldBoxId;
        SET NEW.`box_history` = CONCAT( IFNULL(CONCAT(NEW.`box_history`,'/'),CONCAT(@oldBoxId, '/')), @boxId);
        SET NEW.`batch_id` = @boxId;
    END IF;
END;//
DELIMITER ;
