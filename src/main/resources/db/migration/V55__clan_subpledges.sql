-- Portado de tools/sql/clan_subpledges.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clan_subpledges` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `sub_pledge_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(45) DEFAULT NULL,
  `leader_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`sub_pledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

