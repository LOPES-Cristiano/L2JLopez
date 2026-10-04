-- Portado de tools/sql/buffer_scheme_list.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_scheme_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` varchar(40) DEFAULT NULL,
  `scheme_name` varchar(36) DEFAULT NULL,
  `mod_accepted` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

