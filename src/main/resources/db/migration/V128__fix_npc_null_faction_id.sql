-- Corrige faction_id na tabela npc quando gravado com string literal 'NULL' ou vazio
-- Evita que monstros neutros (ex: Gremlins, Keltirs) chamem assistencia indevida
UPDATE `npc`
SET `faction_id` = NULL
WHERE `faction_id` = 'NULL' OR `faction_id` = '' OR `faction_id` = 'none';
