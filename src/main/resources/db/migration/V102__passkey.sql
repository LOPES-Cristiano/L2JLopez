-- Portado de tools/sql/passkey.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `passkey` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `passkey` varchar(45) DEFAULT NULL,
  `question` varchar(55) NOT NULL,
  `answer` varchar(35) NOT NULL,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

