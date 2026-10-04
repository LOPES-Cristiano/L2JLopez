-- Portado de tools/sql/auction_watch.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `auction_watch` (
  `charId` int(10) unsigned NOT NULL DEFAULT 0,
  `auctionId` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`auctionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

