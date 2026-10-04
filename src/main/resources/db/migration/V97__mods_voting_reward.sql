-- Portado de tools/sql/mods_voting_reward.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `mods_voting_reward` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `data` varchar(255) NOT NULL,
  `scope` varchar(255) NOT NULL,
  `time` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

