-- Portado de tools/sql/clan_skills.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `clan_skills` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(5) NOT NULL DEFAULT 0,
  `skill_name` varchar(26) DEFAULT NULL,
  PRIMARY KEY (`clan_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

