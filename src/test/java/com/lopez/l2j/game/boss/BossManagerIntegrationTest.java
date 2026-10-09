package com.lopez.l2j.game.boss;

import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.epic.*;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BossManagerIntegrationTest {

	private BossManager bossManager;
	private AntharasService antharas;
	private ValakasService valakas;
	private BaiumService baium;
	private SailrenService sailren;

	@BeforeEach
	public void setup() {
		antharas = new AntharasService(null, null, null, null, null);
		valakas = new ValakasService(null, null, null, null);
		baium = new BaiumService(null, null, null, null);
		sailren = new SailrenService(null, null, null, null);

		bossManager = new BossManager(
				null, null, antharas, valakas, baium, sailren, null, null, null, null
		);
		bossManager.init();
	}

	private PlayerCharacter createPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	private NpcTemplate createTemplate(int npcId, String name, int level, String type) {
		return new NpcTemplate(npcId, npcId, name, false, "", false, 10.0, 15.0, level, "male",
				type, 40, 10000, 1000, 100, 100, 100, 100, 200, 200, 0, 0, 0, 50, 100, 0, false);
	}

	@Test
	public void testShowBossNpcHtmlSpecialBossNpcs() {
		PlayerCharacter player = createPlayer(1, "Adventurer", 75);
		List<GameServerPacket> sentPackets = new ArrayList<>();

		// Heart of Warding (Antharas entrance)
		NpcTemplate tplHeart = createTemplate(BossManager.NPC_HEART_OF_WARDING, "Heart of Warding", 70, "L2Npc");
		NpcInstance npcHeart = new NpcInstance(1001, tplHeart, 0, 0, 0, 0);

		boolean handled = bossManager.showBossNpcHtml(player, npcHeart, sentPackets::add);
		assertTrue(handled, "Heart of Warding must be handled by BossManager");
		assertEquals(1, sentPackets.size());

		// Teleport Cube
		sentPackets.clear();
		NpcTemplate tplCube = createTemplate(BossManager.NPC_ANTHARAS_CUBE, "Teleportation Cube", 70, "L2Npc");
		NpcInstance npcCube = new NpcInstance(1002, tplCube, 0, 0, 0, 0);

		handled = bossManager.showBossNpcHtml(player, npcCube, sentPackets::add);
		assertTrue(handled, "Antharas Cube must be handled by BossManager");
		assertEquals(1, sentPackets.size());

		// Regular mob/npc should NOT be intercepted
		sentPackets.clear();
		NpcTemplate tplRegular = createTemplate(20001, "Gremlin", 1, "L2Monster");
		NpcInstance npcRegular = new NpcInstance(1003, tplRegular, 0, 0, 0, 0);

		handled = bossManager.showBossNpcHtml(player, npcRegular, sentPackets::add);
		assertFalse(handled, "Regular monsters must not be intercepted as boss entrance NPCs");
		assertEquals(0, sentPackets.size());
	}

	@Test
	public void testBossDeathDispatch() {
		PlayerCharacter killer = createPlayer(10, "Slayer", 80);

		// Antharas death
		NpcTemplate antTpl = createTemplate(29019, "Antharas", 79, "L2GrandBoss");
		NpcInstance antBoss = new NpcInstance(5001, antTpl, 0, 0, 0, 0);

		assertDoesNotThrow(() -> bossManager.onBossKilled(antBoss, killer, null));
		assertEquals(BossStatus.INTERVAL, antharas.getStatus());
	}
}
