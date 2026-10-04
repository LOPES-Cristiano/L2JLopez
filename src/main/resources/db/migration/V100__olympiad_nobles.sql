-- Portado de tools/sql/olympiad_nobles.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `olympiad_nobles` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `class_id` decimal(3,0) NOT NULL DEFAULT 0,
  `olympiad_points` decimal(10,0) NOT NULL DEFAULT 0,
  `competitions_done` decimal(3,0) NOT NULL DEFAULT 0,
  `competitions_won` decimal(3,0) NOT NULL DEFAULT 0,
  `competitions_lost` decimal(3,0) NOT NULL DEFAULT 0,
  `competitions_drawn` decimal(3,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

