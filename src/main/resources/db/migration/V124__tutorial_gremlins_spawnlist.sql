-- Inserção farta de Gremlins e Keltirs nos pontos de respawn inicial de todas as raças
-- Garante que iniciantes (ex: Cedric's Training Hall, Dark Elf Sanctuary) encontrem monstros
-- para a Quest 255 (Tutorial) e drop da Blue Gemstone (6353).

-- 1. Cedric's Training Hall (Talking Island - Human Fighters)
INSERT INTO `spawnlist` (`location`, `count`, `npc_templateid`, `locx`, `locy`, `locz`, `heading`, `respawn_delay`, `periodOfDay`, `random_zone`) VALUES
('talking_island_cedric', 1, 20001, -71050, 258250, -3140, 16384, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70850, 258050, -3140, 32768, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71150, 258350, -3140, 49152, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70750, 258180, -3140, 0, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71200, 258000, -3140, 16384, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70900, 258300, -3140, 32768, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71000, 257900, -3140, 49152, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70800, 258400, -3140, 0, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71100, 257800, -3140, 16384, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71250, 258150, -3140, 32768, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70700, 258300, -3140, 49152, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71300, 257950, -3140, 0, 15, 0, -1),
('talking_island_cedric', 1, 20001, -70650, 258200, -3140, 16384, 15, 0, -1),
('talking_island_cedric', 1, 20001, -71050, 257700, -3140, 32768, 15, 0, -1),
('talking_island_cedric', 1, 18342, -70950, 258150, -3140, 0, 15, 0, -1);

-- 2. Dark Elven Village (Shadowdell / Shilen Sanctuary - Dark Elves)
INSERT INTO `spawnlist` (`location`, `count`, `npc_templateid`, `locx`, `locy`, `locz`, `heading`, `respawn_delay`, `periodOfDay`, `random_zone`) VALUES
('darkelf_sanctuary', 1, 20418, 28450, 11150, -4230, 0, 15, 0, -1),
('darkelf_sanctuary', 1, 20418, 28300, 11200, -4230, 16384, 15, 0, -1),
('darkelf_sanctuary', 1, 20418, 28500, 10950, -4230, 32768, 15, 0, -1),
('darkelf_sanctuary', 1, 20418, 28250, 10900, -4230, 49152, 15, 0, -1),
('darkelf_sanctuary', 1, 20418, 28600, 11100, -4230, 0, 15, 0, -1),
('darkelf_sanctuary', 1, 20001, 28200, 11150, -4230, 16384, 15, 0, -1),
('darkelf_sanctuary', 1, 20001, 28400, 10800, -4230, 32768, 15, 0, -1),
('darkelf_sanctuary', 1, 20001, 28550, 10850, -4230, 49152, 15, 0, -1),
('darkelf_sanctuary', 1, 20001, 28350, 11300, -4230, 0, 15, 0, -1),
('darkelf_sanctuary', 1, 20001, 28480, 11250, -4230, 16384, 15, 0, -1);
