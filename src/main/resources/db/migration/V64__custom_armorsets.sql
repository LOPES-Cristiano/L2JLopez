-- Portado de tools/sql/custom_armorsets.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `custom_armorsets` (
  `id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `chest` smallint(5) unsigned NOT NULL DEFAULT 0,
  `legs` smallint(5) unsigned NOT NULL DEFAULT 0,
  `head` smallint(5) unsigned NOT NULL DEFAULT 0,
  `gloves` smallint(5) unsigned NOT NULL DEFAULT 0,
  `feet` smallint(5) unsigned NOT NULL DEFAULT 0,
  `skill_id` smallint(5) unsigned NOT NULL DEFAULT 0,
  `skill_lvl` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `skillset_id` smallint(5) unsigned NOT NULL DEFAULT 0,
  `shield` smallint(5) unsigned NOT NULL DEFAULT 0,
  `shield_skill_id` smallint(5) unsigned NOT NULL DEFAULT 0,
  `enchant6skill` smallint(5) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`,`chest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

