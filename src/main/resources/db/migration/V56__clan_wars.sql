-- Portado de tools/sql/clan_wars.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clan_wars` (
  `clan1` varchar(35) NOT NULL DEFAULT '',
  `clan2` varchar(35) NOT NULL DEFAULT '',
  `expiry_time` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan1`,`clan2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

