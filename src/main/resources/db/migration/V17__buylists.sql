-- Portado de tools/sql/buylists.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `buylists` (
  `buylist_id` int(10) unsigned NOT NULL,
  `item_id` int(10) unsigned NOT NULL,
  `count` int(10) unsigned NOT NULL DEFAULT 0,
  `next_restock_time` bigint(20) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`buylist_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

