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
}
