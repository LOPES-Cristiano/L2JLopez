-- Bloody Pixy (31845) e monstros do tipo L2FriendlyMob (Pixy, Treant, Blight Treant)
-- nao devem possuir aggro contra jogadores normais/pacificos.
UPDATE `npc`
SET `aggro` = 0
WHERE `id` = 31845 OR `type` = 'L2FriendlyMob';
