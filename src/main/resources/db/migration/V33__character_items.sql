-- Portado de tools/sql/character_items.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `character_items` (
  `owner_id` int(11) DEFAULT NULL,
  `item_id` int(11) DEFAULT NULL,
  `count` bigint(20) DEFAULT 1,
  `enchant_level` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

