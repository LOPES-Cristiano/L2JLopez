-- Portado de tools/sql/class_list.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `class_list` (
  `class_name` varchar(20) NOT NULL DEFAULT '',
  `id` int(10) unsigned NOT NULL DEFAULT 0,
  `parent_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

