-- Portado de tools/sql/heroes.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `heroes` (
  `charId` decimal(11,0) NOT NULL DEFAULT 0,
  `char_name` varchar(45) NOT NULL DEFAULT '',
  `class_id` decimal(3,0) NOT NULL DEFAULT 0,
  `count` decimal(3,0) NOT NULL DEFAULT 0,
  `played` decimal(1,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

