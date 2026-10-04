-- Portado de tools/sql/character_blocks.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_blocks` (
  `charId` int(10) unsigned NOT NULL,
  `name` varchar(35) NOT NULL,
  PRIMARY KEY (`charId`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

