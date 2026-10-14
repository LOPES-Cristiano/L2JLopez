package com.lopez.l2j.game.instance.foursepulchers;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FourSepulchersServiceTest {

	private FourSepulchersService fourSepulchersService;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	private ItemTemplate questTemplate(int itemId) {
		return ItemTemplate.etc(itemId, itemId, "Quest Item", "quest", "none", 1, "none", 0, false, false, false, false);
	}

	@BeforeEach
	void setUp() {
		fourSepulchersService = new FourSepulchersService(null, ObjectIdFactory.sequential(1000));
	}

	@Test
	void testCanEnterValidation() {
		PlayerCharacter leader = dummyPlayer(1, "SepulcherLeader", 78);
		List<PlayerCharacter> smallParty = List.of(leader, dummyPlayer(2, "P2", 78));

		// Falha: grupo com menos de 4 membros
		assertFalse(fourSepulchersService.canEnter(leader, smallParty));

		List<PlayerCharacter> fullParty = List.of(
				leader,
				dummyPlayer(2, "P2", 78),
				dummyPlayer(3, "P3", 78),
				dummyPlayer(4, "P4", 78)
		);

		// Falha: sem Pass of Darkness
		assertFalse(fourSepulchersService.canEnter(leader, fullParty));

		// Adiciona Pass of Darkness (7075)
		leader.inventory().add(new ItemInstance(10, questTemplate(FourSepulchersService.PASS_OF_DARKNESS), leader.objectId(), 1));
		assertTrue(fourSepulchersService.canEnter(leader, fullParty));
	}

	@Test
	void testMausoleumProgressionAndHalishaDefeat() {
		PlayerCharacter leader = dummyPlayer(1, "SepulcherLeader", 78);
		List<PlayerCharacter> fullParty = List.of(
				leader,
				dummyPlayer(2, "P2", 78),
				dummyPlayer(3, "P3", 78),
				dummyPlayer(4, "P4", 78)
		);
		leader.inventory().add(new ItemInstance(10, questTemplate(FourSepulchersService.PASS_OF_DARKNESS), leader.objectId(), 1));

		int mausoleumId = FourSepulchersService.SEPULCHER_CONQUERORS;
		assertTrue(fourSepulchersService.enterSepulcher(mausoleumId, leader, fullParty));
		// Pass of darkness consumido
		assertTrue(leader.inventory().byItemId(FourSepulchersService.PASS_OF_DARKNESS).isEmpty());
		assertEquals(1, fourSepulchersService.getMausoleum(mausoleumId).stage());

		// Avanca das salas 1 ate 5
		for (int i = 1; i <= 4; i++) {
			assertTrue(fourSepulchersService.clearRoom(mausoleumId, leader));
		}
		assertEquals(5, fourSepulchersService.getMausoleum(mausoleumId).stage());
		assertTrue(leader.inventory().byItemId(FourSepulchersService.CHAPEL_KEY).isPresent());

		// Derrota Sombra de Halisha na 5ª sala
		assertTrue(fourSepulchersService.defeatShadowOfHalisha(mausoleumId, leader));
		// Lider recebe Goblet of Aethelred (7256)
		assertTrue(leader.inventory().byItemId(FourSepulchersService.GOBLET_CONQUERORS).isPresent());
		// Mausoleu volta ao estado livre
		assertEquals(0, fourSepulchersService.getMausoleum(mausoleumId).stage());
	}

	@Test
	void testCombineFourGobletsForImperialTombPass() {
		PlayerCharacter player = dummyPlayer(100, "Archaeologist", 80);

		// Adiciona os 4 calices
		player.inventory().add(new ItemInstance(1, questTemplate(FourSepulchersService.GOBLET_CONQUERORS), player.objectId(), 1));
		player.inventory().add(new ItemInstance(2, questTemplate(FourSepulchersService.GOBLET_EMPERORS), player.objectId(), 1));
		player.inventory().add(new ItemInstance(3, questTemplate(FourSepulchersService.GOBLET_SAGES), player.objectId(), 1));
		player.inventory().add(new ItemInstance(4, questTemplate(FourSepulchersService.GOBLET_JUDGES), player.objectId(), 1));

		assertTrue(fourSepulchersService.combineFourGoblets(player));

		// Calices consumidos
		assertTrue(player.inventory().byItemId(FourSepulchersService.GOBLET_CONQUERORS).isEmpty());
		assertTrue(player.inventory().byItemId(FourSepulchersService.GOBLET_EMPERORS).isEmpty());
		assertTrue(player.inventory().byItemId(FourSepulchersService.GOBLET_SAGES).isEmpty());
		assertTrue(player.inventory().byItemId(FourSepulchersService.GOBLET_JUDGES).isEmpty());

		// Recebe Passe do Tumulo Imperial (8073)
		assertTrue(player.inventory().byItemId(FourSepulchersService.IMPERIAL_TOMB_PASS).isPresent());
	}

	@Test
	void testSepulcherWithInventoryServiceAndGameWorldSpawnsAndEvictAll() {
		com.lopez.l2j.game.item.InMemoryItemRepository repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		ItemTemplate passTpl = questTemplate(FourSepulchersService.PASS_OF_DARKNESS);
		ItemTemplate keyTpl = questTemplate(FourSepulchersService.CHAPEL_KEY);
		ItemTemplate gobTpl = questTemplate(FourSepulchersService.GOBLET_CONQUERORS);
		com.lopez.l2j.game.item.ItemTemplateTable itemTable = com.lopez.l2j.game.item.ItemTemplateTable.of(List.of(passTpl, keyTpl, gobTpl), List.of());
		ObjectIdFactory ids = ObjectIdFactory.sequential(6000);
		com.lopez.l2j.game.service.InventoryService invService = new com.lopez.l2j.game.service.InventoryService(itemTable, repo, ids, 0);
		com.lopez.l2j.game.world.GameWorld world = new com.lopez.l2j.game.world.GameWorld();
		com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates = com.lopez.l2j.game.npc.NpcTemplateTable.of(List.of());

		FourSepulchersService fullService = new FourSepulchersService(itemTable, ids, invService, world, npcTemplates, repo);

		PlayerCharacter leader = dummyPlayer(10, "SepLeader", 78);
		List<PlayerCharacter> party = List.of(
				leader,
				dummyPlayer(20, "P2", 78),
				dummyPlayer(30, "P3", 78),
				dummyPlayer(40, "P4", 78)
		);
		invService.addItem(leader.inventory(), FourSepulchersService.PASS_OF_DARKNESS, 1, "Test");

		java.util.List<com.lopez.l2j.network.game.packet.GameServerPacket> leaderPackets = new java.util.ArrayList<>();
		world.add(new com.lopez.l2j.game.world.GameWorld.OnlinePlayer() {
			@Override public int objectId() { return leader.objectId(); }
			@Override public String name() { return leader.name(); }
			@Override public int x() { return leader.x(); }
			@Override public int y() { return leader.y(); }
			@Override public int z() { return leader.z(); }
			@Override public PlayerCharacter character() { return leader; }
			@Override public void send(com.lopez.l2j.network.game.packet.GameServerPacket packet) { leaderPackets.add(packet); }
		});

		int mausoleumId = FourSepulchersService.SEPULCHER_CONQUERORS;
		assertTrue(fullService.tryEnter(mausoleumId, leader, party));
		// Pass of Darkness removed from DB
		assertTrue(repo.findInventory(leader.objectId()).isEmpty());
		assertTrue(leaderPackets.stream().anyMatch(p -> p instanceof com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation));
		assertEquals(4, fullService.getSpawnedMonsters(mausoleumId).size());

		// Advance wave grants Chapel Key in DB and openNextRoomDoor consumes it
		assertTrue(fullService.advanceWave(mausoleumId, leader));
		assertTrue(repo.findInventory(leader.objectId()).stream().anyMatch(i -> i.itemId() == FourSepulchersService.CHAPEL_KEY));
		assertTrue(fullService.openNextRoomDoor(mausoleumId, leader));
		assertTrue(repo.findInventory(leader.objectId()).stream().noneMatch(i -> i.itemId() == FourSepulchersService.CHAPEL_KEY));

		// EvictAll cleans up monsters and teleports to lobby
		fullService.evictAll();
		assertEquals(0, world.totalNpcs());
		assertEquals(FourSepulchersService.LOBBY_X, leader.x());
	}
}
