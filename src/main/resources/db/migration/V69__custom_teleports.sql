-- Portado de tools/sql/custom_teleports.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `custom_teleports` (
  `description` varchar(75) DEFAULT NULL,
  `id` decimal(11,0) NOT NULL DEFAULT 0,
  `loc_x` decimal(9,0) DEFAULT NULL,
  `loc_y` decimal(9,0) DEFAULT NULL,
  `loc_z` decimal(9,0) DEFAULT NULL,
  `price` decimal(6,0) DEFAULT NULL,
  `fornoble` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

