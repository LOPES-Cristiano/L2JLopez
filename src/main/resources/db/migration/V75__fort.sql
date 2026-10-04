-- Portado de tools/sql/fort.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fort` (
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(25) NOT NULL,
  `siegeDate` decimal(20,0) NOT NULL DEFAULT 0,
  `lastOwnedTime` decimal(20,0) NOT NULL DEFAULT 0,
  `owner` int(11) NOT NULL DEFAULT 0,
  `fortType` int(1) NOT NULL DEFAULT 0,
  `state` int(1) NOT NULL DEFAULT 0,
  `castleId` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

