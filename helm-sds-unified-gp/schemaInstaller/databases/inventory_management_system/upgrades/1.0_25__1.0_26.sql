drop function if exists createUniqueBoxIdTNSI;
drop trigger if exists create_box_id_tnsi;

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

DELIMITER //
CREATE DEFINER=`refill`@`%` TRIGGER create_box_id_tnsi BEFORE UPDATE ON trackable_nonserialized_inventory
                                                                            FOR EACH ROW
BEGIN
    declare boxId varchar(40);
    declare oldBoxId varchar(40);
    declare oldBoxHistory MEDIUMTEXT;
    IF NEW.`batch_id` IS NULL THEN
        select createUniqueBoxIdTNSI() into @boxId;
        select `batch_id` from trackable_nonserialized_inventory where id = NEW.`id` into @oldBoxId;
        select `box_history` from trackable_nonserialized_inventory where id = NEW.`id` into @oldBoxHistory;
        SET NEW.`box_history` = CONCAT(IFNULL(CONCAT(@oldBoxHistory,'/'), CONCAT(@oldBoxId, '/')), @boxId);
        SET NEW.`batch_id` = @boxId;
    END IF;
END;//
DELIMITER ;