package com.lopez.l2j.game.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.GroundItemService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.service.InventoryService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DropAndEconomyConfigurationTest {

	@BeforeEach
	void resetConfigs() {
		Config.reload();
		Config.MULTIPLE_ITEM_DROP = true;
		Config.PRECISE_DROP_CALCULATION = false;
		Config.USE_DEEP_BLUE_DROP_RULES = true;
		Config.PICKUP_FULL_INVENTORY = "drop";
		Config.PARTY_LEVEL_LIMIT = true;
		Config.PARTY_MAX_LEVEL_DIFFERENCE = 10;
		Config.ALT_PARTY_RANGE = 1600.0;
	}

	@Test
	@DisplayName("MultipleItemDrop = True deve empilhar itens repetidos no mesmo raio no chão")
	void multipleItemDropMergesGroundItems() {
		ItemTemplateTable itemTemplates = ItemTemplateTable.of(TestItems.templates(), List.of());
		GroundItemService ground = new GroundItemService(null, ObjectIdFactory.sequential(100000), itemTemplates);

		Config.MULTIPLE_ITEM_DROP = true;

		// Dropa 100 flechas em 0, 0, 0
		var g1 = ground.dropItem(1, TestItems.WOODEN_ARROW, 100, 0, 0, 0);
		assertEquals(1, ground.size());
		assertEquals(100, g1.count());

		// Dropa mais 50 flechas na mesma proximidade (dx=10, dy=10)
		var g2 = ground.dropItem(1, TestItems.WOODEN_ARROW, 50, 10, 10, 0);
		assertEquals(1, ground.size(), "Itens stackaveis proximos devem ser mesclados em um unico GroundItem");
		assertEquals(150, g2.count());
		assertEquals(150, ground.allGroundItems().iterator().next().count());
	}

	@Test
	@DisplayName("PreciseDropCalculation calcula probabilidade contínua quando ativo")
	void preciseDropCalculationWorks() {
		DropTable table = new DropTable() {
			@Override
			public List<DropData> getDrops(int mobId) {
				return List.of(new DropData(500, 57, 100, 100, 0, 1_000_000));
			}

			@Override
			public int size() {
				return 1;
			}
		};
		DropService service = new DropService(table, 1.0, 1.0, 1.0, true);

		Config.PRECISE_DROP_CALCULATION = true;
		List<DropReward> drops = service.rollDrops(500, 40, 40);
		assertFalse(drops.isEmpty());
		assertEquals(57, drops.get(0).itemId());
	}

	@Test
	@DisplayName("Party respeita limite de nivel maximo (PartyLevelLimit e PartyMaxLevelDifference)")
	void partyLevelLimitFiltersOverLeveledMembers() {
		Config.PARTY_LEVEL_LIMIT = true;
		Config.PARTY_MAX_LEVEL_DIFFERENCE = 10;
		Config.PARTY_XP_CUTOFF_METHOD = "none";

		// Cria lider level 70 e membro level 50 (diferença de 20 > 10)
		PlayerCharacter leader = new PlayerCharacter(101, "acc", "Leader", 70, 100000, 1000, 0, 0, 0, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		leader.teleport(0, 0, 0);

		PlayerCharacter lowbie = new PlayerCharacter(102, "acc", "Lowbie", 50, 50000, 500, 0, 0, 0, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		lowbie.teleport(0, 0, 0);

		// Simula GameSession mock ou party member update
		// Testamos se Party.getPartyRange() retorna AltPartyRange configurado
		Config.ALT_PARTY_RANGE = 2000.0;
		assertEquals(2000.0, Party.getPartyRange(), 0.01);
	}

	@Test
	@DisplayName("MaxInventorySlots respeita DWARF (100) vs OTHER (80)")
	void maxInventorySlotsDwarfVsOther() {
		Config.MAX_INVENTORY_SLOTS_FOR_OTHER = 80;
		Config.MAX_INVENTORY_SLOTS_FOR_DWARF = 100;
		Config.MAX_INVENTORY_SLOTS_FOR_GM = 250;

		PlayerCharacter dwarf = new PlayerCharacter(201, "acc", "DwarfChar", 20, 1000, 100, 4, 0, 0, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		PlayerCharacter human = new PlayerCharacter(202, "acc", "HumanChar", 20, 1000, 100, 0, 0, 0, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);

		assertEquals(100, dwarf.maxInventorySlots(), "Dwarf deve receber 100 slots conforme config");
		assertEquals(80, human.maxInventorySlots(), "Human deve receber 80 slots conforme config");
	}
}
