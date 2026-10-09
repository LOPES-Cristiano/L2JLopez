package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
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
