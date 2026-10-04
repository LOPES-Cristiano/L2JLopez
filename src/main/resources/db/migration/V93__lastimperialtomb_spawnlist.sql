-- Portado de tools/sql/lastimperialtomb_spawnlist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `lastimperialtomb_spawnlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `count` int(9) NOT NULL DEFAULT 0,
  `npc_templateid` int(9) NOT NULL DEFAULT 0,
  `locx` int(9) NOT NULL DEFAULT 0,
  `locy` int(9) NOT NULL DEFAULT 0,
  `locz` int(9) NOT NULL DEFAULT 0,
  `randomx` int(9) NOT NULL DEFAULT 0,
  `randomy` int(9) NOT NULL DEFAULT 0,
  `heading` int(9) NOT NULL DEFAULT 0,
  `respawn_delay` int(9) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `key_npc_templateid` (`npc_templateid`)
) ENGINE=InnoDB AUTO_INCREMENT=133 DEFAULT CHARSET=utf8;

