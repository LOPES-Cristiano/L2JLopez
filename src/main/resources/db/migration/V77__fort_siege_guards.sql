-- Portado de tools/sql/fort_siege_guards.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fort_siege_guards` (
  `fortId` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `npcId` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `respawnDelay` int(11) NOT NULL DEFAULT 0,
  `isHired` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `id` (`fortId`)
) ENGINE=InnoDB AUTO_INCREMENT=3708 DEFAULT CHARSET=utf8;

