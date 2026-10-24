package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.minion.MinionTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RaidBossSpawnManagerTest {

	private RaidBossSpawnManager spawnManager;

	@BeforeEach
	public void setup() {
		Config.RAID_MIN_RESPAWN_MULTIPLIER = 1.0f;
		Config.RAID_MAX_RESPAWN_MULTIPLIER = 1.0f;
		Config.RAID_BOSS_P_ATK_MODIFIER = 1.0f;
		Config.RAID_BOSS_M_ATK_MODIFIER = 1.0f;
		Config.RAID_BOSS_MAX_HP_MODIFIER = 1.0f;
		Config.RAID_BOSS_MAX_MP_MODIFIER = 1.0f;
		Config.RAID_BOSS_P_DEF_MODIFIER = 1.0f;
		Config.RAID_BOSS_M_DEF_MODIFIER = 1.0f;

		spawnManager = new RaidBossSpawnManager(null, null, null, null, null);
	}

	@Test
	public void testSpawnBossSpawnsMinionsAndDespawnsOnDeath() {
		MinionTable minionTable = new MinionTable("data/xml/world/minion.xml");
		minionTable.load();

		NpcTemplateTable templates = new NpcTemplateTable() {
			@Override
			public Optional<NpcTemplate> get(int npcId) {
				String type = (npcId >= 25000 && npcId < 26000) ? "L2RaidBoss" : "L2Minion";
				return Optional.of(NpcTemplate.fallback(npcId, "NPC_" + npcId, type));
			}

			@Override
			public int size() {
				return 3;
			}
		};
		GameWorld world = new GameWorld();
		ObjectIdFactory objectIds = ObjectIdFactory.sequential(100000);

		RaidBossSpawnManager mgr = new RaidBossSpawnManager(null, templates, world, objectIds, null, minionTable);
		// Raid Boss 25273 tem 3 minions 25274 + 1 minion 25275 = 4 minions em minion.xml
		var record = new RaidBossSpawnManager.RaidBossRecord(
				25273, 1, 1000, 2000, -3000, 0, 0,
				3600, 7200, 0L, 50000.0, 10000.0, false
		);
		mgr.getStoredInfo().put(25273, record);

		Optional<NpcInstance> bossOpt = mgr.spawnBoss(25273);
		assertTrue(bossOpt.isPresent(), "Raid Boss 25273 deve ser gerado");
		NpcInstance boss = bossOpt.get();

		assertTrue(boss.hasMinions(), "Raid Boss 25273 deve ter seus minions gerados junto com ele");
		assertEquals(4, boss.minions().size(), "Raid Boss 25273 possui 4 minions configurados em minion.xml");
		assertEquals(5, world.totalNpcs(), "Mundo deve conter o Raid Boss + 4 minions");

		for (NpcInstance minion : boss.minions()) {
			assertTrue(minion.isRaidMinion(), "Minions do Raid Boss devem ser marcados como raidMinion");
			assertEquals(boss.objectId(), minion.masterObjectId(), "Minions devem apontar para o objectId do Raid Boss");
			assertTrue(world.npc(minion.objectId()).isPresent(), "Minion deve estar registrado no GameWorld");
		}

		// Ao matar o Raid Boss, seus minions devem ser despawnados
		mgr.onBossKilled(boss, null);
		assertTrue(boss.minions().isEmpty(), "Lista de minions do Raid Boss deve ser limpa na morte");
		assertEquals(1, world.totalNpcs(), "Apenas o corpo do Raid Boss permanece no mundo ate o decay; minions sao removidos");
	}

	@Test
	public void testCalculateRespawnDelayWithinBounds() {
		var record = new RaidBossSpawnManager.RaidBossRecord(
				25001, 1, 0, 0, 0, 0, 0,
				3600, 7200, 0L, 10000.0, 5000.0, true
		);

		long delaySec = spawnManager.calculateRespawnDelaySec(record);
		assertTrue(delaySec >= 3600, "Respawn delay should be at least minDelay");
		assertTrue(delaySec <= 7200, "Respawn delay should be at most maxDelay");
	}

	@Test
	public void testCalculateRespawnDelayWithMultiplier() {
		Config.RAID_MIN_RESPAWN_MULTIPLIER = 0.5f;
		Config.RAID_MAX_RESPAWN_MULTIPLIER = 0.5f;

		var record = new RaidBossSpawnManager.RaidBossRecord(
				25001, 1, 0, 0, 0, 0, 0,
				3600, 3600, 0L, 10000.0, 5000.0, true
		);

		long delaySec = spawnManager.calculateRespawnDelaySec(record);
		assertEquals(1800, delaySec, "50% multiplier should half the respawn delay");
	}

	@Test
	public void testStatusAndStorage() {
		var record = new RaidBossSpawnManager.RaidBossRecord(
				25002, 1, 100, 200, -300, 0, 0,
				1000, 2000, System.currentTimeMillis() + 60_000L, 5000.0, 2000.0, false
		);

		spawnManager.getStoredInfo().put(25002, record);

		assertEquals(RaidBossSpawnManager.Status.DEAD, spawnManager.getStatus(25002));
		assertTrue(spawnManager.isDefined(25002));
		assertFalse(spawnManager.isDefined(99999));
		assertEquals(RaidBossSpawnManager.Status.UNDEFINED, spawnManager.getStatus(99999));
	}

	@Test
	public void testBossStatModifiers() {
		Config.RAID_BOSS_MAX_HP_MODIFIER = 2.5f;
		Config.RAID_BOSS_P_ATK_MODIFIER = 1.5f;

		assertEquals(2.5f, Config.RAID_BOSS_MAX_HP_MODIFIER);
		assertEquals(1.5f, Config.RAID_BOSS_P_ATK_MODIFIER);
	}
}
