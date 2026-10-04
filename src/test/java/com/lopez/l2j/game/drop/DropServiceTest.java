package com.lopez.l2j.game.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DropServiceTest {

	private static final int MOB_ID = 20001;
	private static final int ADENA_ID = com.lopez.l2j.game.item.TestItems.ADENA;
	private static final int ITEM_SWORD_ID = com.lopez.l2j.game.item.TestItems.SHORT_SWORD;

	private InMemoryDropTable dropTable;
	private DropService dropService;

	static class InMemoryDropTable implements DropTable {
		private final List<DropData> drops = new ArrayList<>();

		void add(DropData data) {
			drops.add(data);
		}

		@Override
		public List<DropData> getDrops(int mobId) {
			return drops.stream().filter(d -> d.mobId() == mobId).toList();
		}

		@Override
		public int size() {
			return drops.size();
		}
	}

	@BeforeEach
	void setUp() {
		dropTable = new InMemoryDropTable();
		// 100% chance de adena (min 100, max 200)
		dropTable.add(new DropData(MOB_ID, ADENA_ID, 100, 200, 0, DropData.MAX_CHANCE));
		// 100% chance de espada (min 1, max 1)
		dropTable.add(new DropData(MOB_ID, ITEM_SWORD_ID, 1, 1, 1, DropData.MAX_CHANCE));
		// 0% chance de outro item
		dropTable.add(new DropData(MOB_ID, 999, 1, 1, 1, 0));

		dropService = new DropService(dropTable, 1.0, 1.0, 1.0, true);
	}

	@Test
	void rollsConfiguredDropsCorrectly() {
		List<DropReward> rewards = dropService.rollDrops(MOB_ID);
		assertEquals(2, rewards.size());

		var adena = rewards.stream().filter(DropReward::isAdena).findFirst().orElseThrow();
		assertTrue(adena.count() >= 100 && adena.count() <= 200);

		var sword = rewards.stream().filter(r -> r.itemId() == ITEM_SWORD_ID).findFirst().orElseThrow();
		assertEquals(1, sword.count());
		assertFalse(sword.isAdena());
	}

	@Test
	void rewardsPlayerAndSendsSystemMessages() {
		PlayerCharacter player = new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		player.inventory(new com.lopez.l2j.game.item.Inventory(player.objectId()));

		var repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		var inventoryService = new InventoryService(
				com.lopez.l2j.game.item.TestItems.table(),
				repo,
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000),
				0);

		List<GameServerPacket> sentPackets = new ArrayList<>();
		List<DropReward> rewarded = dropService.rewardMonsterDeath(
				player,
				MOB_ID,
				inventoryService,
				sentPackets::add);

		assertEquals(2, rewarded.size());
		// Jogador recebeu adena no inventario
		assertTrue(player.inventory().adena() >= 100);
		// Jogador recebeu a espada
		assertTrue(player.inventory().items().stream().anyMatch(i -> i.itemId() == ITEM_SWORD_ID));

		// Foram enviadas mensagens de sistema (Adena e Item)
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.YOU_PICKED_UP_S1_ADENA));
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.YOU_PICKED_UP_S1));

		// Foram enviados InventoryUpdate e StatusUpdate de peso
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.InventoryUpdate iu && !iu.items().isEmpty()));
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.StatusUpdate su
				&& su.attributes().stream().anyMatch(a -> a.id() == GameServerPacket.StatusUpdate.CUR_LOAD)));
	}
}
