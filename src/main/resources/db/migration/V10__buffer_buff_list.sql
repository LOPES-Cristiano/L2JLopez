-- Portado de tools/sql/buffer_buff_list.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_buff_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `buff_class` int(2) DEFAULT NULL,
  `buffType` varchar(10) DEFAULT NULL,
  `buffId` int(5) DEFAULT 0,
  `buffLevel` int(5) DEFAULT NULL,
  `forClass` tinyint(1) DEFAULT NULL,
  `canUse` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=latin1;

