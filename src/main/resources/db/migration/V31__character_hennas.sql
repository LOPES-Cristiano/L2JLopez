-- Portado de tools/sql/character_hennas.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_hennas` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `symbol_id` int(11) DEFAULT NULL,
  `slot` int(11) NOT NULL DEFAULT 0,
  `class_index` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`slot`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

