-- Portado de tools/sql/fishing_championship_date.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fishing_championship_date` (
  `finish_date` decimal(20,0) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

