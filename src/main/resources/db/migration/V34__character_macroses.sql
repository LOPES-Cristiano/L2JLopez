-- Portado de tools/sql/character_macroses.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_macroses` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL DEFAULT 0,
  `icon` int(11) DEFAULT NULL,
  `name` varchar(40) DEFAULT NULL,
  `descr` varchar(80) DEFAULT NULL,
  `acronym` varchar(4) DEFAULT NULL,
  `commands` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`charId`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

