-- Portado de tools/sql/accounts.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `accounts` (
  `login` varchar(45) NOT NULL DEFAULT '',
  `password` varchar(45) DEFAULT NULL,
  `lastactive` decimal(20,0) DEFAULT NULL,
  `accessLevel` int(11) NOT NULL DEFAULT 0,
  `lastIP` varchar(20) DEFAULT NULL,
  `lastServerId` int(11) NOT NULL DEFAULT 1,
  `allowed_ip` varchar(20) NOT NULL DEFAULT '*',
  `allowed_hwid` varchar(250) NOT NULL DEFAULT '*',
  `email` varchar(255) DEFAULT NULL,
  `created_time` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

