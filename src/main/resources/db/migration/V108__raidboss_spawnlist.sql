-- Portado de tools/sql/raidboss_spawnlist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `raidboss_spawnlist` (
  `boss_id` int(11) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 0,
  `loc_x` int(11) NOT NULL DEFAULT 0,
  `loc_y` int(11) NOT NULL DEFAULT 0,
  `loc_z` int(11) NOT NULL DEFAULT 0,
  `zone_size` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `respawn_min_delay` int(11) NOT NULL DEFAULT 43200,
  `respawn_max_delay` int(11) NOT NULL DEFAULT 129600,
  `respawn_time` bigint(20) NOT NULL DEFAULT 0,
  `currentHp` decimal(8,0) DEFAULT NULL,
  `currentMp` decimal(8,0) DEFAULT NULL,
  `broadcastSpawn` varchar(16) DEFAULT 'false',
  PRIMARY KEY (`boss_id`,`loc_x`,`loc_y`,`loc_z`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

