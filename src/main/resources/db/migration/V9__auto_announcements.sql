-- Portado de tools/sql/auto_announcements.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `auto_announcements` (
  `id` int(11) NOT NULL,
  `initial` bigint(20) NOT NULL,
  `delay` bigint(20) NOT NULL,
  `cycle` int(11) NOT NULL,
  `memo` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

