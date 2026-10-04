-- Portado de tools/sql/henna_trees.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `henna_trees` (
  `class_id` decimal(10,0) NOT NULL DEFAULT 0,
  `symbol_id` decimal(10,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`class_id`,`symbol_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

