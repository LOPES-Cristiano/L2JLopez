-- Portado de tools/sql/castle_trapupgrade.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `castle_trapupgrade` (
  `castleId` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `towerIndex` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `level` tinyint(3) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`towerIndex`,`castleId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

