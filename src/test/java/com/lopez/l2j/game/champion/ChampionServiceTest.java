package com.lopez.l2j.game.champion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.drop.DropReward;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChampionServiceTest {

	private ChampionService championService;

	@BeforeEach
	void setUp() {
		championService = new ChampionService(true, 50, 20, 80, 8, 8.0, 8.0, 100, 6392, 1, 9);
	}

	private NpcTemplate createTemplate(int id, int level, String type, int hp) {
		return new NpcTemplate(
				id, id, "Test Mob " + id, false, "", false,
				8.0, 16.0, level, "male", type,
				40, hp, 100, 50, 40, 30, 20, 250, 333,
				0, 0, 0, 50, 100, 0, false, 1000L, 100);
	}

	@Test
	void canBeChampionValidatesCorrectly() {
		NpcTemplate validTemplate = createTemplate(20001, 40, "L2Monster", 1000);
		NpcInstance normalMob = new NpcInstance(1001, validTemplate, 0, 0, 0, 0);
		assertTrue(championService.canBeChampion(normalMob));

		// Min level check (level 15 < 20)
		NpcTemplate lowLvlTemplate = createTemplate(20002, 15, "L2Monster", 500);
		NpcInstance lowMob = new NpcInstance(1002, lowLvlTemplate, 0, 0, 0, 0);
		assertFalse(championService.canBeChampion(lowMob));

		// Max level check (level 85 > 80)
		NpcTemplate highLvlTemplate = createTemplate(20003, 85, "L2Monster", 5000);
		NpcInstance highMob = new NpcInstance(1003, highLvlTemplate, 0, 0, 0, 0);
		assertFalse(championService.canBeChampion(highMob));

		// Raid boss check
		NpcTemplate raidTemplate = createTemplate(29001, 50, "L2RaidBoss", 50000);
		NpcInstance raidBoss = new NpcInstance(1004, raidTemplate, 0, 0, 0, 0);
		assertFalse(championService.canBeChampion(raidBoss));

		// Minion check
		NpcTemplate minionTemplate = createTemplate(20004, 50, "L2Minion", 1000);
		NpcInstance minionMob = new NpcInstance(1005, minionTemplate, 0, 0, 0, 0);
		assertFalse(championService.canBeChampion(minionMob));

		// Guard check
		NpcTemplate guardTemplate = createTemplate(30001, 70, "L2Guard", 5000);
		NpcInstance guard = new NpcInstance(1006, guardTemplate, 0, 0, 0, 0);
		assertFalse(championService.canBeChampion(guard));
	}

	@Test
	void makeChampionSetsCorrectStateAndStats() {
		NpcTemplate template = createTemplate(20010, 45, "L2Monster", 2000);
		NpcInstance mob = new NpcInstance(2001, template, 0, 0, 0, 0);

		assertFalse(mob.isChampion());
		assertEquals(2000, mob.maxHp());

		championService.makeChampion(mob);

		assertTrue(mob.isChampion());
		assertEquals("Champion", mob.championTitle());
		assertEquals(8.0, mob.maxHpMul());
		assertEquals(16000, mob.maxHp());
		assertEquals(16000, mob.currentHp());
	}

	@Test
	void calculateExpAndSpWithChampionMultiplier() {
		NpcTemplate template = createTemplate(20020, 50, "L2Monster", 2000);
		NpcInstance mob = new NpcInstance(3001, template, 0, 0, 0, 0);

		// Regular mob
		assertEquals(1000L, championService.calculateExp(mob, 1000L));
		assertEquals(200, championService.calculateSp(mob, 200));

		// Champion mob
		championService.makeChampion(mob);
		assertEquals(8000L, championService.calculateExp(mob, 1000L));
		assertEquals(1600, championService.calculateSp(mob, 200));
	}

	@Test
	void applyDropMultipliersBoostsRewardsAndAddsMedal() {
		NpcTemplate template = createTemplate(20030, 40, "L2Monster", 2000);
		NpcInstance mob = new NpcInstance(4001, template, 0, 0, 0, 0);

		List<DropReward> initialRewards = new ArrayList<>();
		initialRewards.add(new DropReward(57, 100, true)); // Adena
		initialRewards.add(new DropReward(1000, 2, false)); // Normal item

		// Regular mob - rewards unchanged
		List<DropReward> normalResults = championService.applyDropMultipliers(mob, new ArrayList<>(initialRewards), 40);
		assertEquals(2, normalResults.size());
		assertEquals(100, normalResults.get(0).count());
		assertEquals(2, normalResults.get(1).count());

		// Champion mob - rewards multiplied and medal added
		championService.makeChampion(mob);
		List<DropReward> champResults = championService.applyDropMultipliers(mob, new ArrayList<>(initialRewards), 40);

		// Adena multiplied by 8
		assertEquals(800, champResults.get(0).count());
		// Item multiplied by 8
		assertEquals(16, champResults.get(1).count());

		// Event Medal (id 6392) added because player is lvl 40 vs mob lvl 40 and reward 100%
		boolean hasMedal = champResults.stream().anyMatch(r -> r.itemId() == 6392 && r.count() >= 1);
		assertTrue(hasMedal);
	}

	@Test
	void tryRollChampionRespectsChance() {
		ChampionService alwaysChamp = new ChampionService(true, 100, 20, 80, 8, 8.0, 8.0, 100, 6392, 1, 9);
		ChampionService neverChamp = new ChampionService(true, 0, 20, 80, 8, 8.0, 8.0, 100, 6392, 1, 9);

		NpcTemplate template = createTemplate(20040, 35, "L2Monster", 1500);

		NpcInstance mob1 = new NpcInstance(5001, template, 0, 0, 0, 0);
		alwaysChamp.tryRollChampion(mob1);
		assertTrue(mob1.isChampion());

		NpcInstance mob2 = new NpcInstance(5002, template, 0, 0, 0, 0);
		neverChamp.tryRollChampion(mob2);
		assertFalse(mob2.isChampion());
	}
}
