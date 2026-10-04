-- Portado de tools/sql/cursed_weapons.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `cursed_weapons` (
  `itemId` int(11) NOT NULL DEFAULT 0,
  `charId` int(11) DEFAULT 0,
  `playerKarma` int(11) DEFAULT 0,
  `playerPkKills` int(11) DEFAULT 0,
  `nbKills` int(11) DEFAULT 0,
  `endTime` decimal(20,0) DEFAULT 0,
  PRIMARY KEY (`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

