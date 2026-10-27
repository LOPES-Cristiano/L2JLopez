package com.lopez.l2j;

import com.lopez.l2j.game.boss.DualBossRewardService;
import com.lopez.l2j.game.combat.BalanceProfileService;
import com.lopez.l2j.game.craft.DwarfCraftBonus;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.movie.MovieMakerService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.recipe.RecipeEntry;
import com.lopez.l2j.game.recipe.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedCombatAndBossRewardTest {

	private BalanceProfileService balanceService;
	private DualBossRewardService dualBossRewardService;
	private MovieMakerService movieMakerService;
	private RecipeService recipeService;

	@BeforeEach
	void setUp() {
		balanceService = new BalanceProfileService();
		dualBossRewardService = new DualBossRewardService();
		movieMakerService = new MovieMakerService();
		recipeService = new RecipeService();
	}

	@Test
	@DisplayName("BalanceProfileService deve registrar e fornecer multiplicadores de classe e matchup")
	void testBalanceProfileMultipliers() {
		balanceService.setClassMultiplier(88, 1.15, 1.0, 1.05, 1.0);
		balanceService.setMatchupMultiplier(88, 94, 1.20);

		assertThat(balanceService.getPAtkMultiplier(88)).isEqualTo(1.15);
		assertThat(balanceService.getPDefMultiplier(88)).isEqualTo(1.05);
		assertThat(balanceService.getMatchupMultiplier(88, 94)).isEqualTo(1.20);
		// Consulta de matchup nao configurado (ex: 88 vs 88) deve retornar default 1.0
		assertThat(balanceService.getMatchupMultiplier(88, 88)).isEqualTo(1.0);
	}

	@Test
	@DisplayName("DualBossRewardService deve identificar Top DPS e Last Attacker e premiar adequadamente")
	void testDualBossRewardTopDpsAndLastHit() {
		PlayerCharacter dps1 = new PlayerCharacter(3001, "TopDpsWarrior");
		PlayerCharacter dps2 = new PlayerCharacter(3002, "SecondDps");
		PlayerCharacter lastHit = new PlayerCharacter(3003, "FinisherMage");

		NpcTemplate bossTpl = NpcTemplate.fallback(29001, "QueenAnt", "L2RaidBoss");
		NpcInstance boss = new NpcInstance(50001, bossTpl, 0, 0, 0, 0);

		// Dps1 causa 50.000 de dano, Dps2 causa 20.000
		dualBossRewardService.recordDamage(boss.objectId(), dps1, 50000);
		dualBossRewardService.recordDamage(boss.objectId(), dps2, 20000);

		assertThat(dualBossRewardService.getTopDps(boss.objectId())).isEqualTo(dps1);

		// Morte do boss com golpe final de lastHit
		dualBossRewardService.rewardOnBossKill(boss, lastHit);

		// Top DPS recebe 5M Adena + 5 Glittering Medals
		assertThat(dps1.inventory().count(57)).isEqualTo(5_000_000);
		assertThat(dps1.inventory().count(6393)).isEqualTo(5);

		// Last Attacker recebe 2.5M Adena + 10 Event Medals
		assertThat(lastHit.inventory().count(57)).isEqualTo(2_500_000);
		assertThat(lastHit.inventory().count(6392)).isEqualTo(10);
	}

	@Test
	@DisplayName("DwarfCraftBonus deve identificar classes Anas e aplicar bonus no enchant e craft")
	void testDwarfCraftAndEnchantBonus() {
		PlayerCharacter dwarf = new PlayerCharacter(4001, "WarsmithDwarf");
		dwarf.classId(57); // Warsmith
		PlayerCharacter elf = new PlayerCharacter(4002, "SpellsingerElf");
		elf.classId(27); // Spellsinger

		assertThat(DwarfCraftBonus.isDwarf(dwarf)).isTrue();
		assertThat(DwarfCraftBonus.isDwarf(elf)).isFalse();

		// Teste no RecipeService
		RecipeEntry entry = new RecipeEntry(1, "TestRecipe", 0, 1, 1878, 1, 30, 60, true, List.of());
		int elfRate = recipeService.calculateCraftSuccessRate(entry, elf);
		int dwarfRate = recipeService.calculateCraftSuccessRate(entry, dwarf);

		assertThat(elfRate).isEqualTo(60);
		assertThat(dwarfRate).isEqualTo(65); // 60 + 5% bonus de anao
	}

	@Test
	@DisplayName("MovieMakerService deve carregar sequencias pre-definidas com SpecialCamera")
	void testMovieMakerServiceSequences() {
		var antharasSeq = movieMakerService.getSequence("antharas_intro");
		assertThat(antharasSeq).isNotNull();
		assertThat(antharasSeq.scenes()).isNotEmpty();

		var firstScene = antharasSeq.scenes().get(0);
		var packet = firstScene.toPacket();
		assertThat(packet).isNotNull();
	}

	@Test
	@DisplayName("RecipeService deve carregar todas as 865 receitas de data/xml/world/recipes.xml com IDs oficiais do cliente Interlude")
	void testRecipeCatalogXmlIdsAndSoulshots() {
		assertThat(recipeService.getAllRecipes().size()).isGreaterThanOrEqualTo(800);

		// Soulshots D/C/B/A/S-Grade devem ter recipeId 20..24 (e nao 18..22 que no
		// cliente sao Necklace of Anguish/Wisdom)
		assertThat(recipeService.getRecipeByItemId(1804)).isPresent().get()
				.extracting(RecipeEntry::id, RecipeEntry::productId, RecipeEntry::level)
				.containsExactly(20, 1463, 2); // Soulshot: D-Grade
		assertThat(recipeService.getRecipeByItemId(1805)).isPresent().get()
				.extracting(RecipeEntry::id, RecipeEntry::productId, RecipeEntry::level)
				.containsExactly(21, 1464, 4); // Soulshot: C-Grade
		assertThat(recipeService.getRecipeByItemId(1806)).isPresent().get()
				.extracting(RecipeEntry::id, RecipeEntry::productId, RecipeEntry::level)
				.containsExactly(22, 1465, 6); // Soulshot: B-Grade
		assertThat(recipeService.getRecipeByItemId(1807)).isPresent().get()
				.extracting(RecipeEntry::id, RecipeEntry::productId, RecipeEntry::level)
				.containsExactly(23, 1466, 7); // Soulshot: A-Grade
		assertThat(recipeService.getRecipeByItemId(1808)).isPresent().get()
				.extracting(RecipeEntry::id, RecipeEntry::productId, RecipeEntry::level)
				.containsExactly(24, 1467, 8); // Soulshot: S-Grade

		// ID 18 e 19 no cliente sao Necklace of Anguish (1802) e Necklace of Wisdom
		// (1803)
		assertThat(recipeService.getRecipe(18)).isPresent().get()
				.extracting(RecipeEntry::recipeItemId, RecipeEntry::productId)
				.containsExactly(1802, 907);
		assertThat(recipeService.getRecipe(19)).isPresent().get()
				.extracting(RecipeEntry::recipeItemId, RecipeEntry::productId)
				.containsExactly(1803, 908);
	}
}
