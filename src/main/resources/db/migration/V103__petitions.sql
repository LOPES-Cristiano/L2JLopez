-- Portado de tools/sql/petitions.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `petitions` (
  `petition_id` int(11) NOT NULL AUTO_INCREMENT,
  `charId` int(11) NOT NULL DEFAULT 0,
  `petition_txt` text NOT NULL,
  `status` varchar(255) NOT NULL DEFAULT 'New',
  PRIMARY KEY (`petition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

