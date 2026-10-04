-- Portado de tools/sql/posts.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `posts` (
  `post_id` int(8) NOT NULL DEFAULT 0,
  `post_owner_name` varchar(255) NOT NULL DEFAULT '',
  `post_ownerid` int(8) NOT NULL DEFAULT 0,
  `post_date` decimal(20,0) NOT NULL DEFAULT 0,
  `post_topic_id` int(8) NOT NULL DEFAULT 0,
  `post_forum_id` int(8) NOT NULL DEFAULT 0,
  `post_txt` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

