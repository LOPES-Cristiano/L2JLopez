-- Portado de tools/sql/character_offline.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_offline` (
  `charId` int(11) NOT NULL,
  `shopid` int(11) NOT NULL,
  `mode` tinyint(4) NOT NULL DEFAULT 0,
  `packaged` tinyint(4) NOT NULL DEFAULT 0,
  `title` varchar(255) DEFAULT NULL,
  `endTime` decimal(20,0) DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

