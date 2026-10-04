-- Portado de tools/sql/character_offline_shop.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_offline_shop` (
  `shopid` int(11) NOT NULL,
  `itemid` int(11) NOT NULL,
  `count` int(11) DEFAULT NULL,
  `price` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

