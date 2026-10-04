-- Portado de tools/sql/castle_doorupgrade.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `castle_doorupgrade` (
  `doorId` int(11) NOT NULL DEFAULT 0,
  `hp` tinyint(4) NOT NULL DEFAULT 0,
  `castleId` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`doorId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

