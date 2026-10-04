-- Portado de tools/sql/random_spawn_loc.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `random_spawn_loc` (
  `groupId` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT -1,
  PRIMARY KEY (`groupId`,`x`,`y`,`z`,`heading`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

