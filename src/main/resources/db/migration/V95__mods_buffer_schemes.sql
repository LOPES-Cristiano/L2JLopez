-- Portado de tools/sql/mods_buffer_schemes.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `mods_buffer_schemes` (
  `ownerId` int(10) unsigned NOT NULL DEFAULT 0,
  `id` int(10) unsigned NOT NULL DEFAULT 0,
  `level` int(10) unsigned NOT NULL DEFAULT 0,
  `scheme` varchar(20) NOT NULL DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

