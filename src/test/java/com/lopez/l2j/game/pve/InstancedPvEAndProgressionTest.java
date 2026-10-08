package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários para a Onda C9:
 * PvE Instanciado, Dungeons, Solo Farm, Boss Dolls e Fusão de Itens com Herança de Enchant.
 *
 * <p>Critérios de Aceite oficiais:</p>
 * <ul>
 *   <li>Conclusão de dungeon liberando a premiação (Adena + Tokens de Evento).</li>
 *   <li>Teste de fusão confirmando que o resultado herdou o maior encantamento dos ingredientes.</li>
 *   <li>SoloFarm com compra em Adena e teleporte na conclusao.</li>
 *   <li>Boss Dolls no inventario aplicando passivas com regra estrita de nao-cumulatividade por chefe.</li>
 * </ul>
 */
class InstancedPvEAndProgressionTest {

	private DungeonService dungeonService;
	private SoloFarmService soloFarmService;
	private DollService dollService;
	private FusionItemService fusionItemService;

	@BeforeEach
	void setUp() {
		dungeonService = new DungeonService();
		soloFarmService = new SoloFarmService();
		dollService = new DollService();
		fusionItemService = new FusionItemService();
	}

	private PlayerCharacter createPlayer(int objId, String name, int x, int y, int z) {
		PlayerCharacter p = new PlayerCharacter(
				objId, "acc_" + objId, name, 78, 100000000L, 5000000, 0,
				88, 88, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, x, y, z, 0, 5000.0,
				3000.0, 4000.0
		);
		p.inventory(new Inventory(objId));
		return p;
	}

	private ItemInstance createItem(int objId, int itemId, String name, int count, int enchant) {
		ItemTemplate tmpl = ItemTemplate.etc(itemId, itemId, name, "other", "normal", 100, "none", 0, true, true, true, true);
		ItemInstance item = new ItemInstance(objId, tmpl, 0, count);
		item.enchant(enchant);
		return item;
	}

	@Test
	@DisplayName("Criterio Onda C9: Dungeon por estágios cronometrados concluída liberando premiação")
	void testDungeonStagesProgressionAndRewards() {
		PlayerCharacter player = createPlayer(1001, "DungeonHero", 83400, 148000, -3400);
		// Adiciona 1,000,000 Adena (ID 57) e 0 Tokens (ID 4037)
		ItemInstance adena = createItem(2001, 57, "Adena", 1000000, 0);
		ItemInstance tokens = createItem(2002, 4037, "Coin of Luck / Token", 1, 0);
		player.inventory().add(adena);
		player.inventory().add(tokens);

		// 1. Ingressa na Dungeon 1 ("Heretic HexTec" - taxa de 100,000 Adena)
		var enterResult = dungeonService.enterDungeon(player, 1);
		assertTrue(enterResult.success(), "Deve entrar na dungeon com sucesso");
		assertNotNull(enterResult.session());
		assertEquals(900000, adena.count(), "Deve descontar 100,000 Adena da taxa de entrada");
		assertNotEquals(0, player.instanceId(), "O jogador deve ser alocado em uma instancia isolada");

		DungeonService.DungeonSession session = enterResult.session();
		assertEquals(1, session.currentStageNumber(), "Inicia no estagio 1");

		// 2. Derrota todos os monstros do estagio 1 (3 monstros)
		dungeonService.onMonsterKilled(session.instanceId());
		dungeonService.onMonsterKilled(session.instanceId());
		dungeonService.onMonsterKilled(session.instanceId());
		assertEquals(2, session.currentStageNumber(), "Apos derrotar todos do estagio 1, avanca para o estagio 2");

		// 3. Derrota todos os monstros do estagio 2 (3 monstros)
		dungeonService.onMonsterKilled(session.instanceId());
		dungeonService.onMonsterKilled(session.instanceId());
		dungeonService.onMonsterKilled(session.instanceId());
		assertEquals(3, session.currentStageNumber(), "Apos derrotar todos do estagio 2, avanca para o estagio 3 (Boss)");

		// 4. Derrota o Boss no estagio final (1 chefe de raid)
		assertFalse(session.isCompleted());
		dungeonService.onMonsterKilled(session.instanceId());

		// 5. Conclusao com sucesso e premiacao
		assertTrue(session.isCompleted(), "Dungeon deve ser marcada como concluida com sucesso");
		assertEquals(1400000, adena.count(), "Deve receber 500,000 Adena de premiacao final (900k + 500k = 1.4M)");
		assertEquals(6, tokens.count(), "Deve receber 5 Tokens de Evento (4037) de premiacao somados ao 1 inicial");
	}

	@Test
	@DisplayName("Criterio Onda C9: Fusão de itens herda o valor de encantamento do item base (+8)")
	void testFusionItemInheritsHighestBaseEnchant() {
		PlayerCharacter player = createPlayer(1002, "FusionMaster", 83400, 148000, -3400);

		// Ingredientes:
		// 1) Infinity Blade (6367) encantada +8 (arma base)
		ItemInstance weaponBase = createItem(3001, 6367, "Infinity Blade", 1, 8);
		// 2) Scroll/Material (6393) quantidade 30
		ItemInstance material = createItem(3002, 6393, "Fusion Crystal", 30, 0);

		player.inventory().add(weaponBase);
		player.inventory().add(material);

		// Receita de Weapons: 6367 + 6393(x30) -> 6590 (Enhanced Blade) com 60% chance
		var weaponRecipes = fusionItemService.getRecipesByCategory("weapons");
		assertFalse(weaponRecipes.isEmpty(), "Deve carregar receitas de armas de FusionItems.xml");
		FusionItemService.FusionRecipe recipe = weaponRecipes.get(0);

		// Receita com 100% de chance para teste deterministico de herança
		FusionItemService.FusionRecipe deterministicRecipe = new FusionItemService.FusionRecipe(
				recipe.category(), recipe.ingredients(), recipe.resultItemId(), recipe.resultCount(),
				100, recipe.failItemId(), recipe.failCount()
		);

		var result = fusionItemService.fuse(player, deterministicRecipe, List.of(weaponBase, material));

		assertTrue(result.success(), "Fusao deve ser bem-sucedida com chance 100%");
		assertEquals(6590, result.resultItemId());
		assertEquals(8, result.resultingEnchant(), "O item resultante DEVE herdar o enchant +8 da arma base!");

		// Verifica que o novo item esta no inventario com +8
		var fusedOpt = player.inventory().byItemId(6590);
		assertTrue(fusedOpt.isPresent(), "O novo item deve constar no inventario");
		assertEquals(8, fusedOpt.get().enchant(), "O encantamento no inventario deve ser exatamente +8");
	}

	@Test
	@DisplayName("Criterio Onda C9: Fusão prioriza ingrediente equipado como item base para herança")
	void testFusionPrioritizesEquippedItem() {
		PlayerCharacter player = createPlayer(1003, "EquippedFusionMaster", 83400, 148000, -3400);

		// Ingrediente equipado +5
		ItemInstance equippedArmor = createItem(3010, 2407, "Imperial Breastplate (Equipped)", 1, 5);
		equippedArmor.location(ItemInstance.Location.PAPERDOLL, ItemSlots.CHEST);
		assertTrue(equippedArmor.isEquipped());

		// Ingrediente desequipado +3
		ItemInstance unequippedMat = createItem(3011, 57, "Adena Fee", 5000000, 0);

		player.inventory().add(equippedArmor);
		player.inventory().add(unequippedMat);

		FusionItemService.FusionRecipe recipe = new FusionItemService.FusionRecipe(
				"Armor", List.of(), 6373, 1, 100, 2407, 1
		);

		var result = fusionItemService.fuse(player, recipe, List.of(equippedArmor, unequippedMat));

		assertTrue(result.success());
		assertEquals(5, result.resultingEnchant(), "Deve herdar o encantamento do item equipado (+5)");
	}

	@Test
	@DisplayName("Criterio Onda C9: SoloFarm compra pacote de monstros por Adena e finaliza no limite")
	void testSoloFarmPackageLifecycle() {
		PlayerCharacter player = createPlayer(1004, "SoloFarmer", 83400, 148000, -3400);
		ItemInstance adena = createItem(4001, 57, "Adena", 200000, 0);
		player.inventory().add(adena);

		// Contrata 100 monstros (Custo = 100 * 1000 = 100,000 Adena)
		var result = soloFarmService.buyFarmPackage(player, 100);
		assertTrue(result.success());
		assertEquals(100000, adena.count(), "Deve descontar 100,000 Adena");
		assertEquals(SoloFarmService.ENTRY_X, player.x(), "Teleportado para entrada da arena");

		SoloFarmService.SoloFarmSession session = result.session();
		assertEquals(100, session.totalMobsBought());
		assertEquals(0, session.mobsKilled());

		// Simula 99 abates
		for (int i = 0; i < 99; i++) {
			soloFarmService.onMobKilled(session.instanceId());
		}
		assertFalse(session.isCompleted());

		// 100º abate -> Conclui a sessao e teleporta para fora
		soloFarmService.onMobKilled(session.instanceId());
		assertTrue(session.isCompleted());
		assertEquals(SoloFarmService.EXIT_X, player.x(), "Teleportado de volta para fora da arena apos conclusao");
		assertEquals(0, player.instanceId());
	}

	@Test
	@DisplayName("Criterio Onda C9: Boss Dolls no inventário com regra de não-cumulatividade por chefe")
	void testBossDollsNonCumulativeRule() {
		PlayerCharacter player = createPlayer(1005, "DollCollector", 83400, 148000, -3400);

		// Adiciona duas dolls de Baium: Lv 1 (poder 2.0) e Lv 3 (poder 6.0)
		ItemInstance baiumLv1 = createItem(5001, 6392, "Baium Doll Lv 1", 1, 0);
		ItemInstance baiumLv3 = createItem(5002, 20702, "Baium Doll Lv 3", 1, 0);

		// Adiciona uma doll de Zaken Lv 2 (poder 4.0)
		ItemInstance zakenLv2 = createItem(5003, 20722, "Zaken Doll Lv 2", 1, 0);

		player.inventory().add(baiumLv1);
		player.inventory().add(baiumLv3);
		player.inventory().add(zakenLv2);

		var activeDolls = dollService.getActiveDolls(player);
		assertEquals(2, activeDolls.size(), "Devem existir apenas 2 chefes ativos (Baium e Zaken)");

		// Baium deve ser considerado apenas no nivel 3 (poder 6.0, nao acumulando com o nivel 1)
		assertTrue(activeDolls.containsKey("Baium"));
		assertEquals(3, activeDolls.get("Baium").level(), "Apenas a doll de maior nivel de Baium deve estar ativa");
		assertEquals(6.0, activeDolls.get("Baium").power());

		// Zaken deve estar ativo no nivel 2 (poder 4.0)
		assertTrue(activeDolls.containsKey("Zaken"));
		assertEquals(2, activeDolls.get("Zaken").level());
		assertEquals(4.0, activeDolls.get("Zaken").power());

		// Total de poder: 6.0 + 4.0 = 10.0
		assertEquals(10.0, dollService.calculateTotalPower(player));
	}
}
