-- Portado de tools/sql/server_data.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `server_data` (
  `valueName` varchar(64) NOT NULL,
  `valueData` varchar(200) NOT NULL,
  PRIMARY KEY (`valueName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

