-- Portado de tools/sql/fort_spawnlist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fort_spawnlist` (
  `fortId` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `npcId` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `spawnType` int(11) NOT NULL DEFAULT 0,
  `castleId` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `id` (`fortId`)
) ENGINE=InnoDB AUTO_INCREMENT=359 DEFAULT CHARSET=utf8;

