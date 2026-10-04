-- Portado de tools/sql/character_herolist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_herolist` (
  `charId` int(11) NOT NULL DEFAULT 0,
  `enddate` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

