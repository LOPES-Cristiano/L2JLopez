package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.ai.NpcAiService;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrandBossAndNpcConfigurationTest {

	private boolean origAnnounceRaid;
	private int origDeltaAggro;

	@BeforeEach
	void setUp() {
		origAnnounceRaid = Config.ANNOUNCE_RAID_SPAWN;
		origDeltaAggro = Config.ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE;
	}

	@AfterEach
	void tearDown() {
		Config.ANNOUNCE_RAID_SPAWN = origAnnounceRaid;
		Config.ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = origDeltaAggro;
	}

	@Test
	void testGrandBossRespawnParametersFromConfig() {
		GrandBossManager manager = new GrandBossManager(null, null);

		var antharas = manager.getBoss(GrandBossManager.ANTHARAS).orElseThrow();
		assertEquals(11520, antharas.minRespawnMinutes());
		assertEquals(15840, antharas.maxRespawnMinutes());

		var baium = manager.getBoss(GrandBossManager.BAIUM).orElseThrow();
		assertEquals(7200, baium.minRespawnMinutes());
		assertEquals(10080, baium.maxRespawnMinutes());

		var queenAnt = manager.getBoss(GrandBossManager.QUEEN_ANT).orElseThrow();
		assertEquals(1140, queenAnt.minRespawnMinutes());
		assertEquals(2160, queenAnt.maxRespawnMinutes());

		var zaken = manager.getBoss(GrandBossManager.ZAKEN).orElseThrow();
		assertEquals(2400, zaken.minRespawnMinutes());
		assertEquals(3600, zaken.maxRespawnMinutes());

		var valakas = manager.getBoss(GrandBossManager.VALAKAS).orElseThrow();
		assertEquals(11520, valakas.minRespawnMinutes());
		assertEquals(15840, valakas.maxRespawnMinutes());

		var sailren = manager.getBoss(GrandBossManager.SAILREN).orElseThrow();
		assertEquals(720, sailren.minRespawnMinutes());
		assertEquals(2160, sailren.maxRespawnMinutes());
	}

	@Test
	void testAnnounceRaidSpawnWhenEnabled() {
		GameWorld world = new GameWorld();
		List<GameServerPacket> broadcasted = new ArrayList<>();

		PlayerCharacter listener = createPlayer(1001, "Listener", 75);
		world.add(new TestOnlinePlayer(listener, broadcasted::add));

		NpcTemplate raidTemplate = new NpcTemplate(25001, 25001, "RaidBossTest", true, "", false,
				9.0, 24.0, 80, "male", "L2RaidBoss", 40, 200000, 10000, 1500, 1500, 1500, 1500,
				250, 333, 0, 0, 0, 50, 120, 0, false, 0L, 0);

		NpcTemplateTable table = NpcTemplateTable.of(List.of(raidTemplate));
		ObjectIdFactory ids = ObjectIdFactory.sequential(10000);
		SpawnService spawnService = new SpawnService(null, table, world, ids);

		// 1. Com announce desativado: nenhum CreatureSay broadcasted
		Config.ANNOUNCE_RAID_SPAWN = false;
		spawnService.spawn(25001, 0, 0, 0, 0);
		boolean hadAnnouncement = broadcasted.stream().anyMatch(p -> p instanceof CreatureSay cs && cs.channel() == CreatureSay.ANNOUNCEMENT);
		assertFalse(hadAnnouncement, "Com AnnounceRaidSpawn=false nao deve anunciar");

		// 2. Com announce ativado: transmite mensagem de spawn do RaidBoss
		broadcasted.clear();
		Config.ANNOUNCE_RAID_SPAWN = true;
		spawnService.spawn(25001, 100, 100, 0, 0);

		boolean announced = broadcasted.stream().anyMatch(p -> p instanceof CreatureSay cs
				&& cs.channel() == CreatureSay.ANNOUNCEMENT
				&& cs.text().contains("RaidBossTest")
				&& cs.text().contains("spawned"));
		assertTrue(announced, "Com AnnounceRaidSpawn=true deve enviar CreatureSay anunciando o spawn");
	}

	@Test
	void testDeltaAggroMobNoAttackPlayer() {
		GameWorld world = new GameWorld();
		CombatService combatService = new CombatService();
		CharTemplateTable charTemplates = new CharTemplateTable();
		NpcAiService aiService = new NpcAiService(world, combatService, charTemplates);

		// Monstro nível 20 agressivo com range 300
		NpcTemplate mobTemplate = new NpcTemplate(20001, 20001, "AggroWolf", true, "", false,
				9.0, 24.0, 20, "male", "L2Monster", 40, 1000, 500, 100, 100, 100, 100,
				250, 333, 0, 0, 0, 50, 120, 300, false, 0L, 0);

		NpcInstance mob = new NpcInstance(5001, mobTemplate, 0, 0, 0, 0);
		world.addNpc(mob);

		// Jogador nível 26 (diferença de 6 níveis acima do monstro)
		PlayerCharacter player = createPlayer(2001, "Hero", 26);
		world.add(new TestOnlinePlayer(player, p -> {}));

		// Caso A: Delta configurado para 5 níveis -> Jogador lvl 26 está 6 acima (> 5), mob deve IGNORAR
		Config.ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = 5;
		aiService.tick();
		aiService.tick(); // a cada 2 ticks checa aggro
		assertFalse(mob.inCombat(), "Com Delta=5, mob lvl 20 deve ignorar player lvl 26");

		// Caso B: Delta configurado para 8 níveis -> Jogador lvl 26 está 6 acima (< 8), mob deve AGREDAR
		Config.ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = 8;
		aiService.tick();
		aiService.tick();
		assertTrue(mob.inCombat(), "Com Delta=8, mob lvl 20 deve agrar player lvl 26");
	}

	private static PlayerCharacter createPlayer(int charId, String name, int level) {
		PlayerCharacter player = new PlayerCharacter(charId, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				800, 300, 300, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 800.0, 300.0, 300.0);
		player.level(level);
		return player;
	}

	private static class TestOnlinePlayer implements GameWorld.OnlinePlayer {
		private final PlayerCharacter character;
		private final java.util.function.Consumer<GameServerPacket> consumer;

		TestOnlinePlayer(PlayerCharacter character, java.util.function.Consumer<GameServerPacket> consumer) {
			this.character = character;
			this.consumer = consumer;
		}

		@Override public int objectId() { return character.objectId(); }
		@Override public String name() { return character.name(); }
		@Override public int x() { return character.x(); }
		@Override public int y() { return character.y(); }
		@Override public int z() { return character.z(); }
		@Override public PlayerCharacter character() { return character; }
		@Override public void send(GameServerPacket packet) { consumer.accept(packet); }
	}
}
