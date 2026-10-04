-- Portado de tools/sql/character_variables.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_variables` (
  `obj_id` int(11) NOT NULL DEFAULT 0,
  `type` varchar(86) NOT NULL DEFAULT '0',
  `name` varchar(100) CHARACTER SET utf8 NOT NULL DEFAULT '0',
  `value` varchar(333) CHARACTER SET utf8 NOT NULL DEFAULT '0',
  `expire_time` bigint(20) NOT NULL DEFAULT 0,
  UNIQUE KEY `prim` (`obj_id`,`type`,`name`) USING BTREE,
  KEY `obj_id` (`obj_id`) USING BTREE,
  KEY `type` (`type`) USING BTREE,
  KEY `name` (`name`) USING BTREE,
  KEY `value` (`value`) USING BTREE,
  KEY `expire_time` (`expire_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;

