-- Portado de tools/sql/character_raid_points.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_raid_points` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `boss_id` int(10) unsigned NOT NULL DEFAULT 0,
  `points` int(10) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

