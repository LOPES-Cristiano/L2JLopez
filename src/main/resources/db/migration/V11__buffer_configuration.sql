-- Portado de tools/sql/buffer_configuration.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_configuration` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `configDesc` varchar(30) DEFAULT NULL,
  `configInfo` varchar(150) DEFAULT NULL,
  `configName` varchar(30) DEFAULT NULL,
  `configValue` varchar(30) DEFAULT NULL,
  `usableValues` varchar(40) DEFAULT NULL,
  `canEditOnline` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=latin1;

