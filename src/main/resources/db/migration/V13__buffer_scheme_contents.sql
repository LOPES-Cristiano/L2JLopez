-- Portado de tools/sql/buffer_scheme_contents.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_scheme_contents` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `scheme_id` int(11) DEFAULT NULL,
  `skill_id` int(8) DEFAULT NULL,
  `skill_level` int(4) DEFAULT NULL,
  `buff_class` int(2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1136 DEFAULT CHARSET=latin1;

