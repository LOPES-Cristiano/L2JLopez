package com.lopez.l2j.game.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerPenaltiesTest {

	private CharTemplate template;
	private PlayerCharacter player;
	private Inventory inventory;
	private float originalAltWeightLimit;

	@BeforeEach
	void setUp() {
		originalAltWeightLimit = com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT;
		com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT = 1.0f;
		template = new CharTemplateTable().get(0).orElseThrow();
		player = new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		inventory = new Inventory(player.objectId());
		player.inventory(inventory);
	}

	@org.junit.jupiter.api.AfterEach
	void tearDown() {
		com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT = originalAltWeightLimit;
	}

	@Test
	@DisplayName("Player Lv 1 equipando arma Grade S recebe Grade Penalty 5 e perde precisao e atk")
	void gradePenaltyAppliedWhenEquippingHighGradeWeapon() {
		// Arma Grade S
		var draconicBow = ItemTemplate.weapon(7575, 7575, "Draconic Bow", "lrhand", "bow", 1650, "s",
				581, 132, 293, 12, -3, 0, 0, 48000000, true, true, true, true);
		var weaponItem = new ItemInstance(0x20000001, draconicBow, player.objectId(), 1);
		inventory.add(weaponItem);
		inventory.equip(weaponItem);

		var stats = PlayerStats.calculate(player, template);
		assertEquals(5, stats.gradePenalty(), "Grade penalty deve ser 5 para No-Grade usando Grade S");
		// Lv 1 base accuracy = 34. Penalidade de arma Grade S: -16 * 5 = -80. Bow hitModify: -3. Total = 34 - 3 - 80 = -49
		assertTrue(stats.accuracy() < 0, "Precisão deve despencar com Grade Penalty severo");
	}

	@Test
	@DisplayName("Player Lv 76 equipando item Grade S nao recebe Grade Penalty")
	void noGradePenaltyForLevel76EquippingGradeS() {
		player.level(76);

		var draconicBow = ItemTemplate.weapon(7575, 7575, "Draconic Bow", "lrhand", "bow", 1650, "s",
				581, 132, 293, 12, -3, 0, 0, 48000000, true, true, true, true);
		var weaponItem = new ItemInstance(0x20000001, draconicBow, player.objectId(), 1);
		inventory.add(weaponItem);
		inventory.equip(weaponItem);

		var stats = PlayerStats.calculate(player, template);
		assertEquals(0, stats.gradePenalty(), "Personagem Lv 76 com Expertise Grade S nao deve ter penalidade");
		assertTrue(stats.accuracy() > 100, "Precisao de Lv 76 com arco S deve ser alta");
	}

	@Test
	@DisplayName("Penalidade de peso niveis 0 a 4 reduz velocidade proporcionalmente")
	void weightPenaltyReducesRunSpeedProgressively() {
		var baseStats = PlayerStats.calculate(player, template);
		int baseRunSpeed = baseStats.runSpeed();
		assertEquals(0, baseStats.weightPenalty(), "Inventario vazio deve ter weightPenalty 0");

		// Criar itens pesados para testar faixas de peso
		// maxLoad padrão de Human Fighter com CON 43 é 81.900 (conBonus 1.187 * 69000)
		var heavyItemTemplate = ItemTemplate.etc(99999, 99999, "Heavy Lead", "material", "asset",
				1000, "none", 1, true, true, true, true);

		// Nivel 1: 50% - 66% (45.000 / 81.900 ~ 54.9%)
		var stack1 = new ItemInstance(0x30000001, heavyItemTemplate, player.objectId(), 45);
		inventory.add(stack1);
		var stats1 = PlayerStats.calculate(player, template);
		assertEquals(1, stats1.weightPenalty(), "Peso > 50% deve disparar penalidade 1");
		assertEquals(baseRunSpeed, stats1.runSpeed(), "Penalidade nivel 1 nao altera runSpeed diretamente");

		// Nivel 2: 66% - 80% (60.000 / 81.900 ~ 73.2%)
		inventory.remove(stack1);
		var stack2 = new ItemInstance(0x30000002, heavyItemTemplate, player.objectId(), 60);
		inventory.add(stack2);
		var stats2 = PlayerStats.calculate(player, template);
		assertEquals(2, stats2.weightPenalty(), "Peso > 66% deve disparar penalidade 2");
		assertEquals((int) Math.round(baseRunSpeed * 0.67), stats2.runSpeed(), "Penalidade nivel 2 reduz velocidade em 33%");

		// Nivel 3: 80% - 100% (70.000 / 81.900 ~ 85.4%)
		inventory.remove(stack2);
		var stack3 = new ItemInstance(0x30000003, heavyItemTemplate, player.objectId(), 70);
		inventory.add(stack3);
		var stats3 = PlayerStats.calculate(player, template);
		assertEquals(3, stats3.weightPenalty(), "Peso > 80% deve disparar penalidade 3");
		assertEquals((int) Math.round(baseRunSpeed * 0.50), stats3.runSpeed(), "Penalidade nivel 3 reduz velocidade pela metade");

		// Nivel 4: >= 100% (85.000 / 81.900 ~ 103.7%)
		inventory.remove(stack3);
		var stack4 = new ItemInstance(0x30000004, heavyItemTemplate, player.objectId(), 85);
		inventory.add(stack4);
		var stats4 = PlayerStats.calculate(player, template);
		assertEquals(4, stats4.weightPenalty(), "Peso >= 100% deve disparar penalidade 4 (travado)");
		assertEquals(1, stats4.runSpeed(), "Penalidade nivel 4 trava o personagem em velocidade 1");
	}

	@Test
	@DisplayName("Equipar escudo reduz evasion em 8 pontos conforme regra oficial")
	void shieldReducesEvasionBy8() {
		var statsWithoutShield = PlayerStats.calculate(player, template);
		int evasionWithout = statsWithoutShield.evasion();

		var shieldTemplate = ItemTemplate.armor(102, 102, "Round Shield", "lhand", "none", 1340, "none",
				0, 0, 100, true, true, true, true);
		var shield = new ItemInstance(0x20000010, shieldTemplate, player.objectId(), 1);
		inventory.add(shield);
		inventory.equip(shield);

		var statsWithShield = PlayerStats.calculate(player, template);
		assertEquals(evasionWithout - 8, statsWithShield.evasion(), "Equipar escudo deve reduzir -8 na evasao");
	}

	@Test
	@DisplayName("Progresso de nivel aumenta monótona e proporcionalmente P.Atk, P.Def e Accuracy")
	void levelProgressionIncreasesStatsMonotonically() {
		var statsLv1 = PlayerStats.calculate(player, template);

		player.level(40);
		var statsLv40 = PlayerStats.calculate(player, template);

		player.level(75);
		var statsLv75 = PlayerStats.calculate(player, template);

		assertTrue(statsLv40.accuracy() > statsLv1.accuracy(), "Accuracy no Lv 40 deve ser maior que Lv 1");
		assertTrue(statsLv75.accuracy() > statsLv40.accuracy(), "Accuracy no Lv 75 deve ser maior que Lv 40");

		assertTrue(statsLv40.pAtk() > statsLv1.pAtk(), "P.Atk no Lv 40 deve ser maior que Lv 1");
		assertTrue(statsLv75.pAtk() > statsLv40.pAtk(), "P.Atk no Lv 75 deve ser maior que Lv 40");

		assertTrue(statsLv40.pDef() > statsLv1.pDef(), "P.Def no Lv 40 deve ser maior que Lv 1");
		assertTrue(statsLv75.pDef() > statsLv40.pDef(), "P.Def no Lv 75 deve ser maior que Lv 40");
	}

	@Test
	@DisplayName("Adena e Itens de Quest nunca adicionam peso ao inventario")
	void adenaAndQuestItemsDoNotContributeToWeight() {
		var adenaTpl = ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 1, true, true, true, true);
		var adena = new ItemInstance(0x20000020, adenaTpl, player.objectId(), 100_000_000);
		inventory.add(adena);

		var questTpl = ItemTemplate.etc(99998, 99998, "Quest Proof", "quest", "normal", 1000, "none", 0, false, false, false, false);
		var questItem = new ItemInstance(0x20000021, questTpl, player.objectId(), 50);
		inventory.add(questItem);

		assertEquals(0, inventory.currentLoad(), "Adena e quest items devem ter peso zero");
		var stats = PlayerStats.calculate(player, template);
		assertEquals(0, stats.weightPenalty(), "Peso zero nao pode ter penalidade");
	}

	@Test
	@DisplayName("Personagem sobrecarregado (Level 4) recupera velocidade normal ao descartar peso")
	void overloadedCharacterRecoversSpeedWhenWeightDropped() {
		var baseStats = PlayerStats.calculate(player, template);
		int normalSpeed = baseStats.runSpeed();

		var heavyItemTemplate = ItemTemplate.etc(99999, 99999, "Heavy Lead", "material", "asset",
				1000, "none", 1, true, true, true, true);
		var heavy = new ItemInstance(0x30000005, heavyItemTemplate, player.objectId(), 100);
		inventory.add(heavy);

		var overloadedStats = PlayerStats.calculate(player, template);
		assertEquals(4, overloadedStats.weightPenalty(), "Com 100.000 de peso deve atingir penalidade 4");
		assertEquals(1, overloadedStats.runSpeed(), "Velocidade deve ser travada em 1");

		// Remove o item pesado (simula destruir no lixo ou depositar no warehouse)
		inventory.remove(heavy);

		// Adiciona apenas 20 de peso (0.02%)
		var lightTpl = ItemTemplate.etc(99997, 99997, "Light Herb", "material", "asset",
				1, "none", 1, true, true, true, true);
		var light = new ItemInstance(0x30000006, lightTpl, player.objectId(), 20);
		inventory.add(light);

		assertEquals(20, inventory.currentLoad());
		var recoveredStats = PlayerStats.calculate(player, template);
		assertEquals(0, recoveredStats.weightPenalty(), "Com 0.02% de peso, a penalidade deve ser 0");
		assertEquals(normalSpeed, recoveredStats.runSpeed(), "Velocidade deve voltar ao normal");
	}

	@Test
	@DisplayName("AltWeightLimit moderado e astronomico (ex: 999999999999) nao causam overflow e elevam limite corretamente")
	void altWeightLimitCalculatesCorrectlyWithoutOverflow() {
		var statsBase = PlayerStats.calculate(player, template);
		int baseMaxLoad = statsBase.maxLoad();
		assertTrue(baseMaxLoad > 0);

		// Multiplicador 2x
		com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT = 2.0f;
		var stats2x = PlayerStats.calculate(player, template);
		assertEquals(baseMaxLoad * 2, stats2x.maxLoad(), "AltWeightLimit=2 deve dobrar o limite de peso");

		// Multiplicador astronômico (ex: 999999999999f) - caso de usuário querendo peso ilimitado
		com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT = 999999999999.0f;
		var statsMax = PlayerStats.calculate(player, template);
		assertEquals(Integer.MAX_VALUE, statsMax.maxLoad(), "AltWeightLimit gigante deve atingir Integer.MAX_VALUE sem overflow");
		assertEquals(0, statsMax.weightPenalty(), "Com Integer.MAX_VALUE nao deve haver penalidade de peso");

		// Teste com carga pesada e limite astronômico
		var heavyItemTemplate = ItemTemplate.etc(99999, 99999, "Heavy Lead", "material", "asset",
				1000, "none", 1, true, true, true, true);
		var heavy = new ItemInstance(0x30000007, heavyItemTemplate, player.objectId(), 100_000);
		inventory.add(heavy);
		var statsWithHeavy = PlayerStats.calculate(player, template);
		assertEquals(0, statsWithHeavy.weightPenalty(), "Carga pesada com limite infinito nao deve sofrer penalidade");
	}

	@Test
	@DisplayName("IncreaseWeightLimitByLevel aumenta capacidade de carga proporcionalmente ao nivel")
	void increaseWeightLimitByLevelIncreasesMaxLoad() {
		com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT = 1.0f;
		com.lopez.l2j.config.Config.INCREASE_WEIGHT_LIMIT_BY_LEVEL = true;

		player.level(1);
		var statsLv1 = PlayerStats.calculate(player, template);

		player.level(80);
		var statsLv80 = PlayerStats.calculate(player, template);

		assertTrue(statsLv80.maxLoad() > statsLv1.maxLoad(),
				"Limite de peso no Lv 80 deve ser maior que no Lv 1 quando IncreaseWeightLimitByLevel estiver ativado");
	}
}
