package com.lopez.l2j.game.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CombatPlayerRetaliationTest {

	private CombatService combatService;
	private CharTemplate playerTemplate;
	private PlayerCharacter player;
	private NpcTemplate monsterTemplate;
	private NpcInstance monster;

	@BeforeEach
	void setUp() {
		combatService = new CombatService();
		playerTemplate = new CharTemplateTable().get(0).orElseThrow();
		player = new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		player.inventory(new Inventory(player.objectId()));

		monsterTemplate = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 30, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		monster = new NpcInstance(0x30000001, monsterTemplate, 100, 100, 0, 0);
	}

	@Test
	void monsterAttackingPlayerReducesPlayerHp() {
		assertEquals(100.0, player.currentHp());

		int attempts = 0;
		while (player.currentHp() == 100.0 && attempts < 20) {
			attempts++;
			var hit = combatService.attackPlayer(monster, player, playerTemplate);
			if (hit.flags() != 0x80) { // nao miss
				assertTrue(hit.damage() > 0, "Dano contra o jogador deve ser positivo");
			}
		}

		assertTrue(player.currentHp() < 100.0, "HP do jogador deve ter sido reduzido pelo ataque do monstro");
	}

	@Test
	void deadMonsterCannotAttackPlayer() {
		monster.dead(true);
		monster.currentHp(0);

		var hit = combatService.attackPlayer(monster, player, playerTemplate);
		assertEquals(0, hit.damage());
	}
}
