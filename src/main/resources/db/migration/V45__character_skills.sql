-- Portado de tools/sql/character_skills.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_skills` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 1,
  `skill_name` varchar(40) DEFAULT NULL,
  `class_index` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`skill_id`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

