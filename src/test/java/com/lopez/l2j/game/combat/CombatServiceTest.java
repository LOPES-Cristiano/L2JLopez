package com.lopez.l2j.game.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CombatServiceTest {

	private CombatService combatService;
	private CharTemplate playerTemplate;
	private PlayerCharacter player;
	private NpcTemplate monsterTemplate;
	private NpcInstance monster;

	@BeforeEach
	void setUp() {
		combatService = new CombatService();
		playerTemplate = new com.lopez.l2j.game.template.CharTemplateTable().get(0).orElseThrow();

		player = new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		player.inventory(new Inventory(player.objectId()));

		// Gremlin (id 20001, lvl 1, maxHp 30, pDef 30, pAtk 10, type L2Monster)
		monsterTemplate = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 30, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		monster = new NpcInstance(0x30000001, monsterTemplate, 100, 100, 0, 0);
	}

	@Test
	void attackReducesMonsterHpAndCanKill() {
		assertEquals(30, monster.currentHp());
		assertFalse(monster.isDead());

		// Ataca até o monstro morrer
		int attempts = 0;
		while (!monster.isDead() && attempts < 20) {
			attempts++;
			var hit = combatService.attackNpc(player, playerTemplate, monster);
			if (hit.flags() != 0x80) { // se não foi miss
				assertTrue(hit.damage() > 0, "Dano deve ser maior que 0");
			}
		}

		assertTrue(monster.isDead(), "Monstro deve morrer apos sofrer dano suficiente");
		assertEquals(0, monster.currentHp());
	}

	@Test
	void deadMonsterCannotBeAttacked() {
		monster.dead(true);
		monster.currentHp(0);

		var hit = combatService.attackNpc(player, playerTemplate, monster);
		assertTrue(hit.isDead());
		assertEquals(0, hit.damage());
	}

	@Test
	void initialSkillAgainstHighLevelMonsterIsResistedAndDealsMinimalDamage() {
		// Monstro nível 80 (ex: Antharas / Mob de FoG / Varka)
		var lvl80Template = new NpcTemplate(22000, 22000, "HighLevelMonster", false, "", false, 10.0, 15.0, 80, "male",
				"L2Monster", 10000, 500, 500, 100, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		var highLvlMob = new NpcInstance(0x30000002, lvl80Template, 100, 100, 0, 0);

		// Jogador nível 1 usando skill inicial (Wind Strike nível 1, magicLevel 1, power 12.0)
		var hit = combatService.skillMagicNpc(player, playerTemplate, highLvlMob, 12.0, 1, false, false);

		assertTrue(hit.resisted(), "Skill inicial contra monstro level 80 deve falhar e ser resistida (diferença de 79 níveis)");
		assertEquals(1, hit.damage(), "Dano contra monstro level 80 quando resistido deve ser exatamente 1");
	}

	@Test
	void highLevelSkillAgainstHighLevelMonsterDealsFullDamage() {
		var lvl80Template = new NpcTemplate(22000, 22000, "HighLevelMonster", false, "", false, 10.0, 15.0, 80, "male",
				"L2Monster", 10000, 500, 500, 100, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		var highLvlMob = new NpcInstance(0x30000002, lvl80Template, 100, 100, 0, 0);

		// Jogador nível 80 usando Hydro Blast (magicLevel 74, power 108.0)
		var highLvlPlayer = new PlayerCharacter(0x10000002, "archmage", "Archmage", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				3000, 2000, 1500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 3000.0, 2000.0, 1500.0);

		var hit = combatService.skillMagicNpc(highLvlPlayer, playerTemplate, highLvlMob, 108.0, 74, false, false);
		assertTrue(hit.damage() > 10, "Skill de nível compatível deve causar dano normal");
	}

	@Test
	void initialDebuffAgainstHighLevelMonsterNeverLands() {
		var lvl80Template = new NpcTemplate(22000, 22000, "HighLevelMonster", false, "", false, 10.0, 15.0, 80, "male",
				"L2Monster", 10000, 500, 500, 100, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		var highLvlMob = new NpcInstance(0x30000002, lvl80Template, 100, 100, 0, 0);

		// Debuff de nível 1 contra monstro nível 80 (diferença >= 15 níveis)
		for (int i = 0; i < 50; i++) {
			boolean lands = combatService.debuffLands(80.0, 1, 1, highLvlMob, false, false);
			assertFalse(lands, "Debuff inicial nunca deve pegar em monstro de nível 80 (diferença extrema)");
		}
	}

	@Test
	void pvpInitialSkillAgainstHighLevelPlayerIsResisted() {
		var highLvlTarget = new PlayerCharacter(0x10000003, "target80", "Target", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				5000, 2000, 2000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 5000.0, 2000.0, 2000.0);

		// Skill inicial de mago (magicLevel 1, power 12) contra jogador nível 80
		int pvpDamage = combatService.skillMagicPlayer(player, playerTemplate, highLvlTarget, playerTemplate, 12.0, 1, false, false);
		assertEquals(1, pvpDamage, "No PvP, skill inicial contra alvo nível 80 deve ser resistida causando 1 de dano");
	}
}
