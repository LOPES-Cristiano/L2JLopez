-- Portado de tools/sql/armor.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `armor` (
  `item_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(100) DEFAULT NULL,
  `bodypart` varchar(15) NOT NULL DEFAULT '',
  `crystallizable` varchar(5) NOT NULL DEFAULT '',
  `armor_type` varchar(5) NOT NULL DEFAULT '',
  `weight` int(5) NOT NULL DEFAULT 0,
  `material` varchar(15) NOT NULL DEFAULT '',
  `crystal_type` varchar(4) NOT NULL DEFAULT '',
  `avoid_modify` int(1) NOT NULL DEFAULT 0,
  `duration` int(3) NOT NULL DEFAULT 0,
  `lifetime` int(11) DEFAULT -1,
  `p_def` int(3) NOT NULL DEFAULT 0,
  `m_def` int(2) NOT NULL DEFAULT 0,
  `mp_bonus` int(3) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  `crystal_count` int(4) DEFAULT NULL,
  `sellable` varchar(5) NOT NULL DEFAULT 'true',
  `dropable` varchar(5) NOT NULL DEFAULT 'true',
  `destroyable` varchar(5) NOT NULL DEFAULT 'true',
  `tradeable` varchar(5) NOT NULL DEFAULT 'true',
  `skills_item` varchar(70) NOT NULL DEFAULT '',
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

