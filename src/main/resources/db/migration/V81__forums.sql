-- Portado de tools/sql/forums.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `forums` (
  `forum_id` int(8) NOT NULL DEFAULT 0,
  `forum_name` varchar(255) NOT NULL DEFAULT '',
  `forum_parent` int(8) NOT NULL DEFAULT 0,
  `forum_post` int(8) NOT NULL DEFAULT 0,
  `forum_type` int(8) NOT NULL DEFAULT 0,
  `forum_perm` int(8) NOT NULL DEFAULT 0,
  `forum_owner_id` int(8) NOT NULL DEFAULT 0,
  UNIQUE KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

