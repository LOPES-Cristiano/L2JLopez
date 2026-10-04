-- Portado de tools/sql/char_creation_items.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `char_creation_items` (
  `classId` smallint(6) NOT NULL,
  `itemId` smallint(6) unsigned NOT NULL,
  `amount` int(10) unsigned NOT NULL DEFAULT 1,
  `equipped` enum('true','false') NOT NULL DEFAULT 'false',
  PRIMARY KEY (`classId`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

