-- Portado de tools/sql/account_data.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `account_data` (
  `account_name` varchar(32) NOT NULL,
  `valueName` varchar(32) NOT NULL,
  `valueData` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`account_name`,`valueName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

