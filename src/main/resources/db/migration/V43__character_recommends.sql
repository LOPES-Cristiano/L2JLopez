-- Portado de tools/sql/character_recommends.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_recommends` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `target_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

