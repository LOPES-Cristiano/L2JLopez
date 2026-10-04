-- Portado de tools/sql/global_tasks.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `global_tasks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task` varchar(50) NOT NULL DEFAULT '',
  `type` varchar(50) NOT NULL DEFAULT '',
  `last_activation` decimal(20,0) NOT NULL DEFAULT 0,
  `param1` varchar(100) NOT NULL DEFAULT '',
  `param2` varchar(100) NOT NULL DEFAULT '',
  `param3` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

