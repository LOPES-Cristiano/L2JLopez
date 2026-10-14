-- Torna empilhaveis (stackable) os scrolls de enchant e as life stones para economizar slots no inventario.
--   729-732   : Scroll / Crystal Scroll: Enchant Weapon/Armor (A)
--   947-962   : Scroll / Crystal Scroll: Enchant Weapon/Armor (B, C, D, S)
--   6569-6578 : Blessed Scroll: Enchant Weapon/Armor (A, B, C, D, S)
--   8723-8762 : Life Stones (normal, Mid, High, Top - level 46 a 76)
-- O consumo (enchant/refine) ja remove 1 unidade por vez, entao pilhas sao seguras.

UPDATE `etcitem`
SET `consume_type` = 'stackable'
WHERE `item_id` BETWEEN 729 AND 732
   OR `item_id` BETWEEN 947 AND 962
   OR `item_id` BETWEEN 6569 AND 6578
   OR `item_id` BETWEEN 8723 AND 8762;

-- Mescla os itens que os jogadores ja possuem em varias instancias separadas numa unica pilha
-- (mantem o menor object_id com a soma das quantidades). Itens equipados (PAPERDOLL) nao sao tocados.
UPDATE `items` i
JOIN (
  SELECT `owner_id`, `item_id`, `loc`, IF(`loc` = 'FREIGHT', `loc_data`, 0) AS `grp`,
         MIN(`object_id`) AS `keep_id`, SUM(`count`) AS `total`
  FROM `items`
  WHERE (`item_id` BETWEEN 729 AND 732 OR `item_id` BETWEEN 947 AND 962
         OR `item_id` BETWEEN 6569 AND 6578 OR `item_id` BETWEEN 8723 AND 8762)
    AND `loc` IN ('INVENTORY', 'WAREHOUSE', 'CLANWH', 'FREIGHT', 'PET')
  GROUP BY `owner_id`, `item_id`, `loc`, IF(`loc` = 'FREIGHT', `loc_data`, 0)
  HAVING COUNT(*) > 1
) t ON i.`object_id` = t.`keep_id`
SET i.`count` = t.`total`;

DELETE i FROM `items` i
JOIN (
  SELECT `owner_id`, `item_id`, `loc`, IF(`loc` = 'FREIGHT', `loc_data`, 0) AS `grp`,
         MIN(`object_id`) AS `keep_id`
  FROM `items`
  WHERE (`item_id` BETWEEN 729 AND 732 OR `item_id` BETWEEN 947 AND 962
         OR `item_id` BETWEEN 6569 AND 6578 OR `item_id` BETWEEN 8723 AND 8762)
    AND `loc` IN ('INVENTORY', 'WAREHOUSE', 'CLANWH', 'FREIGHT', 'PET')
  GROUP BY `owner_id`, `item_id`, `loc`, IF(`loc` = 'FREIGHT', `loc_data`, 0)
  HAVING COUNT(*) > 1
) t ON i.`owner_id` = t.`owner_id`
   AND i.`item_id` = t.`item_id`
   AND i.`loc` = t.`loc`
   AND IF(i.`loc` = 'FREIGHT', i.`loc_data`, 0) = t.`grp`
   AND i.`object_id` <> t.`keep_id`;
