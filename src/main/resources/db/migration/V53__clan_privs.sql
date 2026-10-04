-- Portado de tools/sql/clan_privs.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clan_privs` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `rank` int(11) NOT NULL DEFAULT 0,
  `party` int(11) NOT NULL DEFAULT 0,
  `privilleges` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`rank`,`party`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

