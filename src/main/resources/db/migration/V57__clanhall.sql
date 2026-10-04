-- Portado de tools/sql/clanhall.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clanhall` (
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(40) NOT NULL DEFAULT '',
  `ownerId` int(11) NOT NULL DEFAULT 0,
  `lease` int(10) NOT NULL DEFAULT 0,
  `desc` text NOT NULL,
  `location` varchar(15) NOT NULL DEFAULT '',
  `paidUntil` decimal(20,0) NOT NULL DEFAULT 0,
  `paidDayTime` decimal(20,0) NOT NULL DEFAULT 0,
  `Grade` decimal(1,0) NOT NULL DEFAULT 0,
  `paid` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

