-- =============================================================================
-- V131: General Shop NPC (Template 50017) & Town Spawns
-- =============================================================================

INSERT IGNORE INTO `npc` (
  `id`, `idTemplate`, `name`, `serverSideName`, `title`, `serverSideTitle`, `race`,
  `collision_radius`, `collision_height`, `level`, `sex`, `type`, `attackrange`,
  `hp`, `mp`, `hpreg`, `mpreg`, `str`, `con`, `dex`, `int`, `wit`, `men`, `exp`, `sp`,
  `patk`, `pdef`, `matk`, `mdef`, `atkspd`, `aggro`, `matkspd`, `rhand`, `lhand`, `armor`,
  `walkspd`, `runspd`, `faction_id`, `faction_range`, `isUndead`, `absorb_level`, `absorb_type`
) VALUES
(50017, 30598, 'General Shop', 1, 'Market & General Goods', 1, 0, 8.00, 23.00, 70, 'female', 'L2Merchant', 40, 3862, 1493, 11.85, 2.78, 40, 43, 30, 21, 20, 10, 0, 0, 1314, 470, 780, 382, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT');

INSERT IGNORE INTO `custom_npc` (
  `id`, `idTemplate`, `name`, `serverSideName`, `title`, `race`, `serverSideTitle`,
  `collision_radius`, `collision_height`, `level`, `sex`, `type`, `attackrange`,
  `hp`, `mp`, `hpreg`, `mpreg`, `str`, `con`, `dex`, `int`, `wit`, `men`, `exp`, `sp`,
  `patk`, `pdef`, `matk`, `mdef`, `atkspd`, `aggro`, `matkspd`, `rhand`, `lhand`, `armor`,
  `walkspd`, `runspd`, `faction_id`, `faction_range`, `isUndead`, `absorb_level`, `absorb_type`
) VALUES
(50017, 30598, 'General Shop', 1, 'Market & General Goods', 0, 1, 8.00, 23.00, 70, 'female', 'L2Merchant', 40, 3862, 1493, 11.85, 2.78, 40, 43, 30, 21, 20, 10, 0, 0, 1314, 470, 780, 382, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT');

-- Spawns nos principais centros urbanos ao lado do Buffer e nas vilas
DELETE FROM `custom_spawnlist` WHERE `npc_templateid` = 50017;

INSERT INTO `custom_spawnlist` (`location`, `count`, `npc_templateid`, `locx`, `locy`, `locz`, `randomx`, `randomy`, `heading`, `respawn_delay`, `loc_id`, `periodOfDay`) VALUES
('Giran Town - General Shop', 1, 50017, 83520, 147960, -3404, 0, 0, 16384, 60, 0, 0),
('Aden Town - General Shop', 1, 50017, 146850, 25840, -2013, 0, 0, 0, 60, 0, 0),
('Goddard Town - General Shop', 1, 50017, 147980, -55260, -2728, 0, 0, 48000, 60, 0, 0),
('Rune Town - General Shop', 1, 50017, 43920, -47710, -800, 0, 0, 50000, 60, 0, 0),
('Dion Town - General Shop', 1, 50017, 15740, 142920, -2704, 0, 0, 16000, 60, 0, 0),
('Gludio Town - General Shop', 1, 50017, -12710, 122816, -3114, 0, 0, 49152, 60, 0, 0),
('Gludin Village - General Shop', 1, 50017, -80720, 149810, -3044, 0, 0, 20480, 60, 0, 0),
('Oren Town - General Shop', 1, 50017, 83020, 53200, -1492, 0, 0, 31000, 60, 0, 0),
('Heine Town - General Shop', 1, 50017, 111500, 219413, -3536, 0, 0, 49000, 60, 0, 0),
('Hunter Village - General Shop', 1, 50017, 117160, 76950, -2689, 0, 0, 34000, 60, 0, 0),
('Schuttgart Town - General Shop', 1, 50017, 87160, -143448, -1293, 0, 0, 15584, 60, 0, 0),
('Talking Island - General Shop', 1, 50017, -84080, 244570, -3729, 0, 0, 40960, 60, 0, 0),
('Elven Village - General Shop', 1, 50017, 47030, 51475, -2977, 0, 0, 43000, 60, 0, 0),
('Dark Elven Village - General Shop', 1, 50017, 9780, 15560, -4574, 0, 0, 5892, 60, 0, 0),
('Orc Village - General Shop', 1, 50017, -45150, -112512, -235, 0, 0, 0, 60, 0, 0),
('Dwarven Village - General Shop', 1, 50017, 115180, -178140, -906, 0, 0, 0, 60, 0, 0);
