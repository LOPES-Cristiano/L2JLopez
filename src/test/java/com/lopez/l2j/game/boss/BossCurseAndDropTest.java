package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BossCurseAndDropTest {

	private CombatService combatService;

	@BeforeEach
	public void setup() {
		Config.PARALIZE_ON_RAID_LEVEL_DIFF = true;
		Config.RAID_MAX_LEVEL_DIFF = 8;
		Config.QUEEN_ANT_MAX_SAFE_LEVEL = 48;
		Config.RATE_RAID_DROP_ITEMS = 2.0f;
		Config.AUTO_LOOT_RAID = false;

		combatService = new CombatService(1.0, 1.0, null, null);
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
	public void testRaidCurseTriggersOnHighLevelPlayer() {
		PlayerCharacter highLevelPlayer = createPlayer(1, "Hero", 80);
		NpcTemplate bossTpl = createTemplate(25001, "TestRaid", 50, "L2RaidBoss");
		NpcInstance boss = new NpcInstance(1001, bossTpl, 0, 0, 0, 0);

		// Player 80 vs Boss 50: diff = 30 > 8 -> Should trigger raid curse!
		assertTrue(combatService.checkRaidCurse(highLevelPlayer, boss),
				"Raid curse must trigger when player level exceeds boss level by > 8");
	}

	@Test
	public void testRaidCurseDoesNotTriggerOnAppropriateLevel() {
		PlayerCharacter normalPlayer = createPlayer(2, "Warrior", 55);
		NpcTemplate bossTpl = createTemplate(25001, "TestRaid", 50, "L2RaidBoss");
		NpcInstance boss = new NpcInstance(1001, bossTpl, 0, 0, 0, 0);

		// Player 55 vs Boss 50: diff = 5 <= 8 -> Safe!
		assertFalse(combatService.checkRaidCurse(normalPlayer, boss),
				"Raid curse must NOT trigger when level difference is <= 8");
	}

	@Test
	public void testQueenAntSpecialSafeLevel() {
		PlayerCharacter safePlayer = createPlayer(3, "Twink", 48);
		PlayerCharacter overLevelPlayer = createPlayer(4, "Overlevel", 49);

		// Queen Ant (29001, level 40)
		NpcTemplate qaTpl = createTemplate(29001, "Queen Ant", 40, "L2GrandBoss");
		NpcInstance qa = new NpcInstance(2001, qaTpl, 0, 0, 0, 0);

		// 48 is safe level
		assertFalse(combatService.checkRaidCurse(safePlayer, qa), "Level 48 should be safe for Queen Ant");

		// 49 exceeds Queen Ant safe level 48
		assertTrue(combatService.checkRaidCurse(overLevelPlayer, qa), "Level 49 must trigger Raid Curse on Queen Ant");
	}

	@Test
	public void testNormalMonsterMinionDoesNotTriggerRaidCurse() {
		PlayerCharacter highLevelPlayer = createPlayer(5, "HighHero", 80);
		NpcTemplate minionTpl = createTemplate(20118, "NormalMinion", 30, "L2Monster");
		NpcInstance normalMinion = new NpcInstance(3001, minionTpl, 0, 0, 0, 0);
		normalMinion.masterObjectId(100); // Pertence a um monstro comum (ex: 20117)
		// Nao e minion de raid boss:
		assertFalse(normalMinion.isRaidMinion(), "Minion de monstro comum nao deve ser considerado raid minion");
		assertFalse(combatService.checkRaidCurse(highLevelPlayer, normalMinion),
				"Atacar lacaio de monstro comum nunca deve aplicar Raid Curse");
	}

	@Test
	public void testRaidBossMinionTriggersRaidCurseOnOverlevel() {
		PlayerCharacter highLevelPlayer = createPlayer(6, "HighHero", 80);
		NpcTemplate raidMinionTpl = createTemplate(25002, "RaidMinion", 40, "L2RaidMinion");
		NpcInstance raidMinion = new NpcInstance(3002, raidMinionTpl, 0, 0, 0, 0);
		raidMinion.masterObjectId(1001);
		raidMinion.raidMinion(true);

		assertTrue(raidMinion.isRaidMinion(), "Deve ser reconhecido como raid minion");
		assertTrue(combatService.checkRaidCurse(highLevelPlayer, raidMinion),
				"Atacar lacaio de Raid Boss com level excessivo deve aplicar Raid Curse");
	}

	@Test
	public void testGmNeverTriggersRaidCurse() {
		PlayerCharacter gmPlayer = createPlayer(7, "AdminGM", 80);
		gmPlayer.accessLevel(100);
		assertTrue(gmPlayer.isGm(), "Player deve ser GM");

		NpcTemplate bossTpl = createTemplate(25001, "LowRaid", 20, "L2RaidBoss");
		NpcInstance boss = new NpcInstance(1002, bossTpl, 0, 0, 0, 0);

		assertFalse(combatService.checkRaidCurse(gmPlayer, boss),
				"GM nunca deve receber Raid Curse ao atacar Raid Boss");
	}

	@Test
	public void testRaidDropRateMultiplier() {
		assertEquals(2.0f, Config.RATE_RAID_DROP_ITEMS, "Raid drop rate must be 2.0");
		assertFalse(Config.AUTO_LOOT_RAID, "AUTO_LOOT_RAID must be false for ground drop scattering");
	}
}
