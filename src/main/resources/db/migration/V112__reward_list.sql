-- Portado de tools/sql/reward_list.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `reward_list` (
  `charid` int(11) NOT NULL DEFAULT 0,
  `itemId` int(11) NOT NULL DEFAULT 0,
  `count` int(22) NOT NULL DEFAULT 0,
  `castle_name` varchar(50) NOT NULL DEFAULT '',
  `rewarded` int(2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

