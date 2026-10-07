-- Corrige spawn acidental de Crystalline Beast (20418) no santuário inicial de Dark Elves para Gremlins (18342)
UPDATE `spawnlist`
SET `npc_templateid` = 18342
WHERE `location` = 'darkelf_sanctuary' AND `npc_templateid` = 20418;
