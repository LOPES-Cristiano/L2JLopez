-- Portado de tools/sql/character_data.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_data` (
  `charId` int(11) NOT NULL,
  `valueName` varchar(32) NOT NULL,
  `valueData` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`charId`,`valueName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

