-- =============================================================================
-- V132: Corrige o crystal_type (Grade) das Boss Jewels no banco de dados
-- =============================================================================
-- No Interlude oficial:
--   6660 Ring of Queen Ant    -> Grade B ('b') - Level 40
--   6662 Ring of Core         -> Grade A ('a') - Level 50
--   6661 Earring of Orfen     -> Grade A ('a') - Level 50
--   6659 Zaken's Earring      -> Grade S ('s') - Level 60
--   6658 Ring of Baium        -> Grade S ('s') - Level 75
--   6656 Earring of Antharas  -> Grade S ('s') - Level 79
--   6657 Necklace of Valakas  -> Grade S ('s') - Level 85
--   8191 Frintezza's Necklace -> Grade A ('a') - Level 74
-- Estavam todas com crystal_type = 's', causando Grade Penalty indevido em
-- personagens abaixo do level 76 ao equipar Queen Ant, Core, Orfen ou Frintezza.

UPDATE `armor` SET `crystal_type` = 'b' WHERE `item_id` = 6660;
UPDATE `armor` SET `crystal_type` = 'a' WHERE `item_id` IN (6661, 6662, 8191);
UPDATE `armor` SET `crystal_type` = 's' WHERE `item_id` IN (6656, 6657, 6658, 6659);

UPDATE `custom_armor` SET `crystal_type` = 'b' WHERE `item_id` = 6660;
UPDATE `custom_armor` SET `crystal_type` = 'a' WHERE `item_id` IN (6661, 6662, 8191);
UPDATE `custom_armor` SET `crystal_type` = 's' WHERE `item_id` IN (6656, 6657, 6658, 6659);
