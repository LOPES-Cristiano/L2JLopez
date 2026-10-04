-- Portado de tools/sql/character_recipebook.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_recipebook` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `id` decimal(11,0) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`,`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

