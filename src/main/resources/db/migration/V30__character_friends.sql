-- Portado de tools/sql/character_friends.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_friends` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `friendId` int(10) unsigned NOT NULL DEFAULT 0,
  `friend_name` varchar(35) NOT NULL DEFAULT '',
  PRIMARY KEY (`charId`,`friend_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

