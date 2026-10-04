-- Portado de tools/sql/quest_global_data.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `quest_global_data` (
  `quest_name` varchar(40) NOT NULL DEFAULT '',
  `var` varchar(20) NOT NULL DEFAULT '',
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`quest_name`,`var`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

