-- Portado de tools/sql/fishing_championship.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fishing_championship` (
  `PlayerName` varchar(35) NOT NULL,
  `fishLength` float(10,2) NOT NULL,
  `rewarded` int(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

