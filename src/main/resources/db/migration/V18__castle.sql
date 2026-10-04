-- Portado de tools/sql/castle.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `castle` (
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(25) NOT NULL,
  `taxPercent` int(11) NOT NULL DEFAULT 15,
  `newTaxPercent` int(11) NOT NULL DEFAULT 15,
  `newTaxDate` decimal(20,0) NOT NULL DEFAULT 0,
  `treasury` int(11) NOT NULL DEFAULT 0,
  `bloodaliance` int(11) NOT NULL DEFAULT 0,
  `siegeDate` decimal(20,0) NOT NULL DEFAULT 0,
  `regTimeOver` enum('true','false') NOT NULL DEFAULT 'true',
  `regTimeEnd` decimal(20,0) NOT NULL DEFAULT 0,
  `AutoTime` enum('true','false') NOT NULL DEFAULT 'false',
  `showNpcCrest` enum('true','false') NOT NULL DEFAULT 'false',
  PRIMARY KEY (`name`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

