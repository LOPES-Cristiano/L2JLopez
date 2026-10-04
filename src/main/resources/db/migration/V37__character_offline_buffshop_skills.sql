-- Portado de tools/sql/character_offline_buffshop_skills.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_offline_buffshop_skills` (
  `charId` int(10) unsigned NOT NULL,
  `item` int(10) unsigned NOT NULL DEFAULT 0,
  `price` bigint(20) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

