package com.lopez.l2j.game.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.boss.epic.VanHalterService;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VanHalterAndSubclassChestsTest {

	private GameWorld world;
	private NpcTemplateTable templates;
	private ObjectIdFactory objectIds;
	private DoorTable doorService;
	private VanHalterService vanHalterService;
	private FourSepulchersService fourSepulchersService;
	private BossManager bossManager;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		templates = Mockito.mock(NpcTemplateTable.class);
		objectIds = ObjectIdFactory.sequential(1000);
		doorService = Mockito.mock(DoorTable.class);

		Mockito.when(templates.get(Mockito.anyInt())).thenAnswer(inv -> {
			int id = inv.getArgument(0);
			return Optional.of(new NpcTemplate(id, id, "TestNpc_" + id, false, "", false, 10.0, 15.0, 80, "male",
					"L2GrandBoss", 40, 10000, 5000, 100, 100, 100, 100, 200, 200, 0, 0, 0, 50, 100, 0, false));
		});

		vanHalterService = new VanHalterService(world, templates, objectIds, null, doorService);
		fourSepulchersService = new FourSepulchersService(null, ObjectIdFactory.sequential(5000));

		bossManager = new BossManager(
				null, null, null, null, null, null, null, null, null, null,
				vanHalterService, fourSepulchersService, world, templates, objectIds);
	}

	@Test
	@DisplayName("Van Halter: Invocacao, ajudantes e ciclo de vida de combate")
	void testVanHalterLifecycle() {
		assertEquals(BossStatus.NOTSPAWN, vanHalterService.status());

		vanHalterService.spawnVanHalter();
		assertEquals(BossStatus.ALIVE, vanHalterService.status());
		assertTrue(vanHalterService.activeVanHalter().isPresent());
		assertEquals(VanHalterService.VAN_HALTER, vanHalterService.activeVanHalter().get().npcId());

		// Minions gerados no mundo
		assertTrue(world.totalNpcs() >= 5);

		// Morte de Van Halter
		PlayerCharacter killer = new PlayerCharacter(101, "TestAccount", "Inquisitor", 80, 0, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);

		NpcInstance vhNpc = vanHalterService.activeVanHalter().get();
		bossManager.onBossKilled(vhNpc, killer, null);

		assertEquals(BossStatus.INTERVAL, vanHalterService.status());
	}

	@Test
	@DisplayName("Fate's Whisper: Morte de Cabrio, Hallate, Kernon e Golkonda invoca baus de quest")
	void testFatesWhisperBossChestsSpawn() {
		PlayerCharacter killer = new PlayerCharacter(102, "TestAccount", "Hero", 75, 0, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);

		// Shax / Cabrio (25035) -> Coffer of the Dead (31027)
		NpcTemplate cabrioTpl = templates.get(25035).orElseThrow();
		NpcInstance cabrio = new NpcInstance(objectIds.nextId(), cabrioTpl, 100, 200, 300, 0);
		bossManager.onBossKilled(cabrio, killer, null);

		boolean foundCoffer = world.npcs().stream().anyMatch(n -> n.npcId() == 31027);
		assertTrue(foundCoffer, "Coffer of the Dead (31027) deve surgir na morte de Cabrio");

		// Kernon (25054) -> Kernon's Chest (31028)
		NpcTemplate kernonTpl = templates.get(25054).orElseThrow();
		NpcInstance kernon = new NpcInstance(objectIds.nextId(), kernonTpl, 150, 250, 350, 0);
		bossManager.onBossKilled(kernon, killer, null);

		boolean foundKernonChest = world.npcs().stream().anyMatch(n -> n.npcId() == 31028);
		assertTrue(foundKernonChest, "Kernon's Chest (31028) deve surgir na morte de Kernon");

		// Golkonda (25126) -> Golkonda's Chest (31029)
		NpcTemplate golkTpl = templates.get(25126).orElseThrow();
		NpcInstance golkonda = new NpcInstance(objectIds.nextId(), golkTpl, 180, 280, 380, 0);
		bossManager.onBossKilled(golkonda, killer, null);

		boolean foundGolkChest = world.npcs().stream().anyMatch(n -> n.npcId() == 31029);
		assertTrue(foundGolkChest, "Golkonda's Chest (31029) deve surgir na morte de Golkonda");

		// Hallate (25220) -> Hallate's Chest (31030)
		NpcTemplate hallateTpl = templates.get(25220).orElseThrow();
		NpcInstance hallate = new NpcInstance(objectIds.nextId(), hallateTpl, 200, 300, 400, 0);
		bossManager.onBossKilled(hallate, killer, null);

		boolean foundHallateChest = world.npcs().stream().anyMatch(n -> n.npcId() == 31030);
		assertTrue(foundHallateChest, "Hallate's Chest (31030) deve surgir na morte de Hallate");
	}
}
