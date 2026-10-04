-- Portado de tools/sql/etcitem.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `etcitem` (
  `item_id` decimal(11,0) NOT NULL DEFAULT 0,
  `name` varchar(100) DEFAULT NULL,
  `crystallizable` varchar(5) DEFAULT NULL,
  `item_type` varchar(14) DEFAULT NULL,
  `weight` decimal(4,0) DEFAULT NULL,
  `consume_type` varchar(9) DEFAULT NULL,
  `material` varchar(11) DEFAULT NULL,
  `crystal_type` varchar(4) NOT NULL DEFAULT 'none',
  `duration` decimal(5,0) DEFAULT NULL,
  `lifetime` int(11) DEFAULT -1,
  `price` decimal(11,0) DEFAULT NULL,
  `crystal_count` int(4) DEFAULT NULL,
  `sellable` varchar(5) DEFAULT NULL,
  `dropable` varchar(5) DEFAULT NULL,
  `destroyable` varchar(5) DEFAULT NULL,
  `tradeable` varchar(5) DEFAULT NULL,
  `skill` varchar(70) DEFAULT '0-0;',
  `html` varchar(5) DEFAULT 'false',
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

