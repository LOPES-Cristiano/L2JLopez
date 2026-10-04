-- Portado de tools/sql/character_quests_item.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_quests_item` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `questId` int(10) NOT NULL,
  `itemId` decimal(11,0) NOT NULL,
  PRIMARY KEY (`charId`,`questId`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

