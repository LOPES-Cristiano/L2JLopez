-- Portado de tools/sql/clan_notices.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clan_notices` (
  `clanID` int(32) NOT NULL DEFAULT 0,
  `notice` text NOT NULL,
  `enabled` varchar(5) NOT NULL DEFAULT '',
  PRIMARY KEY (`clanID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

