-- Portado de tools/sql/character_subclasses.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_subclasses` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `class_id` int(2) NOT NULL DEFAULT 0,
  `exp` decimal(20,0) NOT NULL DEFAULT 0,
  `sp` decimal(11,0) NOT NULL DEFAULT 0,
  `level` int(2) NOT NULL DEFAULT 40,
  `class_index` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

