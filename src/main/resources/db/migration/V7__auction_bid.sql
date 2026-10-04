-- Portado de tools/sql/auction_bid.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `auction_bid` (
  `id` int(11) NOT NULL DEFAULT 0,
  `auctionId` int(11) NOT NULL DEFAULT 0,
  `bidderId` int(11) NOT NULL DEFAULT 0,
  `bidderName` varchar(50) NOT NULL,
  `clan_name` varchar(50) NOT NULL,
  `maxBid` int(11) NOT NULL DEFAULT 0,
  `time_bid` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`auctionId`,`bidderId`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

