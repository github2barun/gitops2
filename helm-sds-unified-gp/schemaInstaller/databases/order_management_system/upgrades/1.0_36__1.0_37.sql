# table ledger_book - stores ledger entry against payment information
# ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `ledger_balance` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `payer` varchar(256) DEFAULT NULL,
  `payee` varchar(256) DEFAULT NULL,
  `due_amount` decimal(19,2) DEFAULT NULL,
  `unused_credit` decimal(19,2) DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `create_timestamp` datetime NOT NULL,
  `last_update_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Query for data migration
DROP table if exists temp;
CREATE TEMPORARY TABLE IF NOT EXISTS temp AS (select distinct payer, payee from ledger_book);

DROP PROCEDURE IF EXISTS migrateProc;
DELIMITER ;;
CREATE PROCEDURE migrateProc()
    BEGIN
        DECLARE n INT DEFAULT 0;
        DECLARE i INT DEFAULT 0;
        SELECT COUNT(*) FROM temp INTO n;
        SET i=0;
        WHILE i<n DO
              select `payer` into @payer from temp LIMIT i,1;
              select `payee` into @payee from temp LIMIT i,1;
              set @due = 0;
              set @credit = 0;
              WITH totalOpenSettledDebit (buyer, totalDebit) AS (
              SELECT
                l.`payer` AS 'buyer',
                SUM(l.`debit`) AS 'totalDebit'
              FROM
                `ledger_book` l
              WHERE
                l.`payee` = @payee
                AND l.`payer` = @payer
                AND l.`status` IN ('OPEN', 'SETTLED')
              GROUP BY
                l.`payer`
            ),
            totalSettledCredit (buyer, totalCredit) AS (
              SELECT
                l.`payer` AS 'buyer',
                SUM(l.`credit`) AS 'totalCredit'
              FROM
                `ledger_book` l
              WHERE
                l.`payee` = @payee
                AND l.`payer` = @payer
                AND l.`status` IN ('SETTLED')
              GROUP BY
                l.`payer`
            ),
            totalOpenCredit(buyer, totaOpenCredit) AS (
              SELECT
                l.`payer` AS 'buyer',
                SUM(l.`credit`) AS 'totalCredit'
              FROM
                `ledger_book` l
              WHERE
                l.`payee` = @payee
                AND l.`payer` = @payer
                AND l.`status` IN ('OPEN')
              GROUP BY
                l.`payer`
            )
            SELECT
              GREATEST(
                IFNULL(totalDebit, 0) - IFNULL(totalCredit, 0),
                0
              ) AS 'totalDue',
              IFNULL(totaOpenCredit, 0) AS 'totalUnusedCredit' into @due, @credit
            FROM
              totalOpenSettledDebit d
              LEFT JOIN totalSettledCredit c ON d.buyer = c.buyer
              LEFT JOIN totalOpenCredit o ON d.buyer = o.buyer;
              INSERT INTO ledger_balance(payer, payee, due_amount, unused_credit, data, create_timestamp, last_update_timestamp)
              values(@payer, @payee, @due, @credit, "{}", current_timestamp(),current_timestamp());
              SET i = i + 1;
        END WHILE;
END;
;;
DELIMITER ;
CALL migrateProc();

DROP table temp;
DROP PROCEDURE migrateProc;