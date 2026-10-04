-- Portado de tools/sql/castle_manor_production.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `castle_manor_production` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `seed_id` int(11) NOT NULL DEFAULT 0,
  `can_produce` int(11) NOT NULL DEFAULT 0,
  `start_produce` int(11) NOT NULL DEFAULT 0,
  `seed_price` int(11) NOT NULL DEFAULT 0,
  `period` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`castle_id`,`seed_id`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

