-- Portado de tools/sql/grandboss_intervallist.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `grandboss_intervallist` (
  `boss_id` int(11) NOT NULL,
  `respawn_time` decimal(20,0) NOT NULL,
  `state` int(11) NOT NULL,
  PRIMARY KEY (`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

