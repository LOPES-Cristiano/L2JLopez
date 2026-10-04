-- Portado de tools/sql/buffer_hall_schemes.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buffer_hall_schemes` (
  `object_id` int(10) unsigned NOT NULL DEFAULT 0,
  `scheme_name` varchar(16) NOT NULL DEFAULT 'default',
  `skills` varchar(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

