-- Portado de tools/sql/fort_staticobjects.sql (somente estrutura; dados estaticos ficam fora do Flyway)
CREATE TABLE IF NOT EXISTS `fort_staticobjects` (
  `fortId` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(30) NOT NULL,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `range_xmin` int(11) NOT NULL DEFAULT 0,
  `range_ymin` int(11) NOT NULL DEFAULT 0,
  `range_zmin` int(11) NOT NULL DEFAULT 0,
  `range_xmax` int(11) NOT NULL DEFAULT 0,
  `range_ymax` int(11) NOT NULL DEFAULT 0,
  `range_zmax` int(11) NOT NULL DEFAULT 0,
  `hp` int(11) NOT NULL DEFAULT 0,
  `pDef` int(11) NOT NULL DEFAULT 0,
  `mDef` int(11) NOT NULL DEFAULT 0,
  `openType` varchar(5) NOT NULL DEFAULT 'false',
  `commanderDoor` varchar(5) NOT NULL DEFAULT 'false',
  `objectType` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `id` (`fortId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

