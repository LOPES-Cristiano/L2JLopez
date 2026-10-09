package com.lopez.l2j.game.skill;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillEnchantServiceTest {

	private SkillTreeTable skillTreeTable;
	private SkillTable skillTable;
	private SkillService skillService;
	private SkillEnchantService enchantService;
	private PlayerCharacter player;

	@BeforeEach
	void setUp() {
		Config.ENCHANT_SKILL_SP_BOOK_NEEDED = true;
		Config.ENCH_SKILL_SP_NEEDED = true;
		Config.ENCH_SKILL_XP_NEEDED = true;

		skillTreeTable = new SkillTreeTable(Path.of("data", "xml", "player", "skills"));
		skillTable = new SkillTable(Path.of("data", "xml", "stats", "skills"));
		skillService = new SkillService(skillTable, skillTreeTable, null, false);
		enchantService = new SkillEnchantService(skillTreeTable, skillService, null);

		player = new PlayerCharacter(1001, "TestHero", "Tester", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		player.level(76);
		player.classId(88); // 88 = Duelist (3rd class)
		player.sp(10_000_000);
		player.exp(100_000_000L);
		player.inventory(new Inventory(player.objectId()));
	}

	@Test
	@DisplayName("SkillEnchantService: Identificação correta de início de rota (+1) para exigência de Giant's Codex")
	void testIsFirstEnchantLevel() {
		assertTrue(SkillEnchantService.isFirstEnchantLevel(101));
		assertTrue(SkillEnchantService.isFirstEnchantLevel(141));
		assertTrue(SkillEnchantService.isFirstEnchantLevel(181));
		assertTrue(SkillEnchantService.isFirstEnchantLevel(221));

		assertFalse(SkillEnchantService.isFirstEnchantLevel(102));
		assertFalse(SkillEnchantService.isFirstEnchantLevel(105));
		assertFalse(SkillEnchantService.isFirstEnchantLevel(142));
	}

	@Test
	@DisplayName("SkillEnchantService: Taxa de sucesso escala de acordo com o nível do jogador (76, 77, 78+)")
	void testSuccessRateScaling() {
		var sOpt = skillTreeTable.getEnchantSkill(1, 101); // Triple Slash +1
		assertTrue(sOpt.isPresent(), "Triple Slash +1 deve existir na tabela");
		var s = sOpt.get();

		assertEquals(s.rate76(), enchantService.getSuccessRate(s, 76));
		assertEquals(s.rate77(), enchantService.getSuccessRate(s, 77));
		assertEquals(s.rate78(), enchantService.getSuccessRate(s, 78));
		assertEquals(s.rate78(), enchantService.getSuccessRate(s, 80));
	}

	@Test
	@DisplayName("SkillEnchantService: Consulta de informações e requisitos de SP/EXP e Giant's Codex")
	void testGetEnchantInfo() {
		var infoOpt = enchantService.getEnchantInfo(player, 1, 101);
		assertTrue(infoOpt.isPresent());
		var info = infoOpt.get();
		assertEquals(1, info.skillId());
		assertEquals(101, info.level());
		assertTrue(info.spCost() > 0);
		assertTrue(info.expCost() > 0);
		assertTrue(info.requiresBook(), "Nível 101 deve exigir Giant's Codex");

		var info102Opt = enchantService.getEnchantInfo(player, 1, 102);
		assertTrue(info102Opt.isPresent());
		assertFalse(info102Opt.get().requiresBook(), "Nível 102 NÃO deve exigir Giant's Codex");
	}

	@Test
	@DisplayName("SkillEnchantService: Bloqueia jogadores abaixo do nível 76")
	void testLevelRequirement() {
		player.level(75);
		player.skills().put(1, 37); // Base level do Triple Slash

		var result = enchantService.enchantSkill(player, 1, 101);
		assertEquals(SkillEnchantService.ResultType.INVALID_CONDITION, result.type());
	}

	@Test
	@DisplayName("SkillEnchantService: Bloqueia se o jogador não tiver o Secret Book of Giants no nível +1")
	void testMissingBookRequirement() {
		player.skills().put(1, 37);
		// Sem o item 6622 no inventário
		var result = enchantService.enchantSkill(player, 1, 101);
		assertEquals(SkillEnchantService.ResultType.MISSING_ITEM, result.type());
	}

	@Test
	@DisplayName("SkillEnchantService: Bloqueia se o jogador não tiver SP suficiente")
	void testMissingSpRequirement() {
		player.skills().put(1, 37);
		player.sp(100); // SP insuficiente (requer ~306.000)

		// Adiciona Giant's Codex
		var bookTpl = ItemTemplate.etc(6622, 6622, "Giant's Codex", "none", "normal", 10, "none", 500000, true, true, true, true);
		var bookItem = new ItemInstance(5001, bookTpl, 1, 1);
		player.inventory().add(bookItem);

		var result = enchantService.enchantSkill(player, 1, 101);
		assertEquals(SkillEnchantService.ResultType.NOT_ENOUGH_SP, result.type());
	}

	@Test
	@DisplayName("SkillEnchantService: Executa encantamento consumindo recursos e garantindo integridade")
	void testEnchantExecution() {
		player.skills().put(1, 37);

		var bookTpl = ItemTemplate.etc(6622, 6622, "Giant's Codex", "none", "normal", 10, "none", 500000, true, true, true, true);
		var bookItem = new ItemInstance(5001, bookTpl, 1, 1);
		player.inventory().add(bookItem);

		long initialExp = player.exp();
		int initialSp = player.sp();

		var result = enchantService.enchantSkill(player, 1, 101);
		assertNotNull(result);

		// EXP e SP foram consumidos
		assertTrue(player.exp() < initialExp);
		assertTrue(player.sp() < initialSp);

		if (result.isSuccess()) {
			assertEquals(101, player.skillLevel(1), "Em sucesso o skill deve estar no nivel 101");
			assertEquals(SkillEnchantService.ResultType.SUCCESS, result.type());
		} else {
			assertEquals(37, player.skillLevel(1), "Na falha o skill deve ter retornado ao nivel base 37");
			assertEquals(SkillEnchantService.ResultType.FAIL, result.type());
		}
	}
}
