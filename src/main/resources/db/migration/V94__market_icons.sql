-- Portado de tools/sql/market_icons.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `market_icons` (
  `itemId` int(6) NOT NULL,
  `itemIcon` varchar(60) NOT NULL,
  PRIMARY KEY (`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

