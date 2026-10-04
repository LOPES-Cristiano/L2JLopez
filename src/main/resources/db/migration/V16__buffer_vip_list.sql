-- Portado de tools/sql/buffer_vip_list.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_vip_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `playerId` varchar(40) DEFAULT NULL,
  `playerName` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

