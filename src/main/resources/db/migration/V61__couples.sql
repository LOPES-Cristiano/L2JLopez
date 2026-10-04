-- Portado de tools/sql/couples.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `couples` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player1Id` int(11) NOT NULL DEFAULT 0,
  `player2Id` int(11) NOT NULL DEFAULT 0,
  `maried` varchar(5) DEFAULT NULL,
  `affiancedDate` decimal(20,0) DEFAULT 0,
  `weddingDate` decimal(20,0) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

