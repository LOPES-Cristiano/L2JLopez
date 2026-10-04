-- Portado de tools/sql/vanhalter_spawnlist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `vanhalter_spawnlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `location` varchar(40) NOT NULL DEFAULT '',
  `count` int(9) NOT NULL DEFAULT 0,
  `npc_templateid` int(9) NOT NULL DEFAULT 0,
  `locx` int(9) NOT NULL DEFAULT 0,
  `locy` int(9) NOT NULL DEFAULT 0,
  `locz` int(9) NOT NULL DEFAULT 0,
  `randomx` int(9) NOT NULL DEFAULT 0,
  `randomy` int(9) NOT NULL DEFAULT 0,
  `heading` int(9) NOT NULL DEFAULT 0,
  `respawn_delay` int(9) NOT NULL DEFAULT 0,
  `loc_id` int(9) NOT NULL DEFAULT 0,
  `periodOfDay` decimal(2,0) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `key_npc_templateid` (`npc_templateid`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8;

