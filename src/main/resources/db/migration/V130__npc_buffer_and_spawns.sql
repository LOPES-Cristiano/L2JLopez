-- =============================================================================
-- V130: NPC Buffer Templates (L2JDream / L2Interlude 50016 & 50005, Lucera 40010 & 40011)
--       and Town Spawns in custom_spawnlist
-- =============================================================================

INSERT IGNORE INTO `npc` (
  `id`, `idTemplate`, `name`, `serverSideName`, `title`, `serverSideTitle`, `race`,
  `collision_radius`, `collision_height`, `level`, `sex`, `type`, `attackrange`,
  `hp`, `mp`, `hpreg`, `mpreg`, `str`, `con`, `dex`, `int`, `wit`, `men`, `exp`, `sp`,
  `patk`, `pdef`, `matk`, `mdef`, `atkspd`, `aggro`, `matkspd`, `rhand`, `lhand`, `armor`,
  `walkspd`, `runspd`, `faction_id`, `faction_range`, `isUndead`, `absorb_level`, `absorb_type`
) VALUES
(50016, 30598, 'NPC Buffer', 1, 'Scheme & Macro Buffer', 1, 0, 8.00, 23.00, 70, 'male', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(50005, 30599, 'Buffer VIP', 1, 'VIP Buffer', 1, 0, 8.00, 23.00, 70, 'female', 'L2BufferVip', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(40010, 31228, 'Mentor Buffer', 1, 'Lucera Buffer', 1, 0, 9.00, 16.00, 70, 'male', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(40011, 31229, 'Event Buffer', 1, 'Lucera Event Buffer', 1, 0, 9.00, 16.00, 70, 'female', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT');

INSERT IGNORE INTO `custom_npc` (
  `id`, `idTemplate`, `name`, `serverSideName`, `title`, `race`, `serverSideTitle`,
  `collision_radius`, `collision_height`, `level`, `sex`, `type`, `attackrange`,
  `hp`, `mp`, `hpreg`, `mpreg`, `str`, `con`, `dex`, `int`, `wit`, `men`, `exp`, `sp`,
  `patk`, `pdef`, `matk`, `mdef`, `atkspd`, `aggro`, `matkspd`, `rhand`, `lhand`, `armor`,
  `walkspd`, `runspd`, `faction_id`, `faction_range`, `isUndead`, `absorb_level`, `absorb_type`
) VALUES
(50016, 30598, 'NPC Buffer', 1, 'Scheme & Macro Buffer', 0, 1, 8.00, 23.00, 70, 'male', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(50005, 30599, 'Buffer VIP', 1, 'VIP Buffer', 0, 1, 8.00, 23.00, 70, 'female', 'L2BufferVip', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(40010, 31228, 'Mentor Buffer', 1, 'Lucera Buffer', 0, 1, 9.00, 16.00, 70, 'male', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT'),
(40011, 31229, 'Event Buffer', 1, 'Lucera Event Buffer', 0, 1, 9.00, 16.00, 70, 'female', 'L2Buffer', 40, 2444, 1345, 7.50, 2.70, 40, 43, 30, 21, 20, 10, 0, 0, 688, 295, 470, 216, 278, 0, 333, 0, 0, 0, 50, 120, NULL, 0, 0, 0, 'LAST_HIT');

-- Clean up any existing duplicate buffer spawns before inserting town spawns
DELETE FROM `custom_spawnlist` WHERE `npc_templateid` IN (50016, 50005, 40010);

INSERT INTO `custom_spawnlist` (`location`, `count`, `npc_templateid`, `locx`, `locy`, `locz`, `randomx`, `randomy`, `heading`, `respawn_delay`, `loc_id`, `periodOfDay`) VALUES
('Giran Town - NPC Buffer', 1, 50016, 83440, 147960, -3404, 0, 0, 16384, 60, 0, 0),
('Giran Town - Lucera Buffer', 1, 40010, 83350, 147960, -3404, 0, 0, 16384, 60, 0, 0),
('Giran Town - Buffer VIP', 1, 50005, 83485, 147960, -3404, 0, 0, 16384, 60, 0, 0),
('Aden Town - NPC Buffer', 1, 50016, 146783, 25840, -2013, 0, 0, 0, 60, 0, 0),
('Goddard Town - NPC Buffer', 1, 50016, 147920, -55260, -2728, 0, 0, 48000, 60, 0, 0),
('Rune Town - NPC Buffer', 1, 50016, 43850, -47710, -800, 0, 0, 50000, 60, 0, 0),
('Dion Town - NPC Buffer', 1, 50016, 15675, 142920, -2704, 0, 0, 16000, 60, 0, 0),
('Gludio Town - NPC Buffer', 1, 50016, -12780, 122816, -3114, 0, 0, 49152, 60, 0, 0),
('Gludin Village - NPC Buffer', 1, 50016, -80790, 149810, -3044, 0, 0, 20480, 60, 0, 0),
('Oren Town - NPC Buffer', 1, 50016, 82950, 53200, -1492, 0, 0, 31000, 60, 0, 0),
('Heine Town - NPC Buffer', 1, 50016, 111430, 219413, -3536, 0, 0, 49000, 60, 0, 0),
('Hunter Village - NPC Buffer', 1, 50016, 117090, 76950, -2689, 0, 0, 34000, 60, 0, 0),
('Schuttgart Town - NPC Buffer', 1, 50016, 87090, -143448, -1293, 0, 0, 15584, 60, 0, 0),
('Talking Island - NPC Buffer', 1, 50016, -84150, 244570, -3729, 0, 0, 40960, 60, 0, 0),
('Elven Village - NPC Buffer', 1, 50016, 46960, 51475, -2977, 0, 0, 43000, 60, 0, 0),
('Dark Elven Village - NPC Buffer', 1, 50016, 9710, 15560, -4574, 0, 0, 5892, 60, 0, 0),
('Orc Village - NPC Buffer', 1, 50016, -45220, -112512, -235, 0, 0, 0, 60, 0, 0),
('Dwarven Village - NPC Buffer', 1, 50016, 115110, -178140, -906, 0, 0, 0, 60, 0, 0);
