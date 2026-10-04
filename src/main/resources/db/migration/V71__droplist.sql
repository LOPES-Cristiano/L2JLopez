-- Portado de tools/sql/droplist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `droplist` (
  `mobId` int(11) NOT NULL DEFAULT 0,
  `itemId` int(11) NOT NULL DEFAULT 0,
  `min` int(11) NOT NULL DEFAULT 0,
  `max` int(11) NOT NULL DEFAULT 0,
  `category` int(11) NOT NULL DEFAULT 0,
  `chance` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`mobId`,`itemId`,`category`),
  KEY `key_mobId` (`mobId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

