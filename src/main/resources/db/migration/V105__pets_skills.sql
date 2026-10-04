-- Portado de tools/sql/pets_skills.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `pets_skills` (
  `templateId` int(6) NOT NULL DEFAULT 0,
  `minLvl` int(2) NOT NULL DEFAULT 0,
  `skillId` int(5) NOT NULL DEFAULT 0,
  `skillLvl` int(2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`templateId`,`skillId`,`skillLvl`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

