-- Portado de tools/sql/items.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `items` (
  `owner_id` int(11) DEFAULT NULL,
  `object_id` int(11) NOT NULL DEFAULT 0,
  `item_id` int(11) DEFAULT NULL,
  `count` int(11) DEFAULT NULL,
  `enchant_level` int(11) DEFAULT NULL,
  `loc` varchar(10) DEFAULT NULL,
  `loc_data` int(11) DEFAULT NULL,
  `time_of_use` int(11) DEFAULT NULL,
  `custom_type1` int(11) DEFAULT 0,
  `custom_type2` int(11) DEFAULT 0,
  `mana_left` decimal(5,0) NOT NULL DEFAULT -1,
  `attributes` varchar(50) DEFAULT '',
  `process` varchar(64) NOT NULL DEFAULT '',
  `creator_id` int(11) DEFAULT NULL,
  `first_owner_id` int(11) NOT NULL,
  `creation_time` decimal(16,0) DEFAULT NULL,
  `data` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`object_id`),
  KEY `key_owner_id` (`owner_id`),
  KEY `key_loc` (`loc`),
  KEY `key_item_id` (`item_id`),
  KEY `key_time_of_use` (`time_of_use`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

