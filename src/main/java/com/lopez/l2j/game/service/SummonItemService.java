package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.pet.PetDataTable;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.summon.SummonItemsTable;

/**
 * Bridge de compatibilidade para {@link com.lopez.l2j.game.summon.SummonItemService}.
 */
public class SummonItemService extends com.lopez.l2j.game.summon.SummonItemService {

	public SummonItemService(SummonItemsTable summonItemsTable,
							 NpcTemplateTable npcTemplates,
							 SpawnService spawnService,
							 InventoryService inventoryService,
							 ObjectIdFactory idFactory) {
		super(summonItemsTable, npcTemplates, spawnService, inventoryService, idFactory);
	}

	public SummonItemService(SummonItemsTable summonItemsTable,
							 NpcTemplateTable npcTemplates,
							 SpawnService spawnService,
							 InventoryService inventoryService,
							 ObjectIdFactory idFactory,
							 PetService petService,
							 PetDataTable petDataTable) {
		super(summonItemsTable, npcTemplates, spawnService, inventoryService, idFactory, petService, petDataTable);
	}
}
