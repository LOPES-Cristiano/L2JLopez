-- Portado de tools/sql/castle_siege_guards.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `castle_siege_guards` (
  `castleId` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `npcId` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `respawnDelay` int(11) NOT NULL DEFAULT 0,
  `isHired` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `id` (`castleId`)
) ENGINE=InnoDB AUTO_INCREMENT=4382 DEFAULT CHARSET=utf8;

