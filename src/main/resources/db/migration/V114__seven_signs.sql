-- Portado de tools/sql/seven_signs.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `seven_signs` (
  `charId` int(11) NOT NULL DEFAULT 0,
  `cabal` varchar(4) NOT NULL DEFAULT '',
  `old_cabal` varchar(4) NOT NULL DEFAULT '',
  `seal` int(1) NOT NULL DEFAULT 0,
  `red_stones` int(11) NOT NULL DEFAULT 0,
  `green_stones` int(11) NOT NULL DEFAULT 0,
  `blue_stones` int(11) NOT NULL DEFAULT 0,
  `ancient_adena_amount` decimal(20,0) NOT NULL DEFAULT 0,
  `contribution_score` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

