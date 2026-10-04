-- Portado de tools/sql/random_spawn.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `random_spawn` (
  `groupId` int(11) NOT NULL DEFAULT 0,
  `npcId` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `initialDelay` bigint(20) NOT NULL DEFAULT -1,
  `respawnDelay` bigint(20) NOT NULL DEFAULT -1,
  `despawnDelay` bigint(20) NOT NULL DEFAULT -1,
  `broadcastSpawn` varchar(5) NOT NULL DEFAULT 'false',
  `randomSpawn` varchar(5) NOT NULL DEFAULT 'true',
  PRIMARY KEY (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

