-- Portado de tools/sql/auction.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `auction` (
  `id` int(11) NOT NULL DEFAULT 0,
  `sellerId` int(11) NOT NULL DEFAULT 0,
  `sellerName` varchar(50) NOT NULL DEFAULT 'NPC',
  `sellerClanName` varchar(50) NOT NULL DEFAULT '',
  `itemType` varchar(25) NOT NULL DEFAULT '',
  `itemId` int(11) NOT NULL DEFAULT 0,
  `itemObjectId` int(11) NOT NULL DEFAULT 0,
  `itemName` varchar(40) NOT NULL DEFAULT '',
  `itemQuantity` int(11) NOT NULL DEFAULT 0,
  `startingBid` int(11) NOT NULL DEFAULT 0,
  `currentBid` int(11) NOT NULL DEFAULT 0,
  `endDate` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`itemType`,`itemId`,`itemObjectId`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

