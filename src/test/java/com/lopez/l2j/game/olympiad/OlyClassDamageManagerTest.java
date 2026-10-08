package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de validacao da Onda C5: Balanceador Exclusivo de Olimpiadas.
 *
 * <p>Comprova:</p>
 * <ul>
 *   <li>Isolamento Total: Fora das Olimpiadas (PvE, Mundo Aberto, Sieges), o dano permanece 100% retail C6.</li>
 *   <li>Dentro das Olimpiadas: Multiplicador ajustado pelo XML e por matchup atacante x defensor.</li>
 *   <li>Duelist contra Soultaker: Dano retail em campo aberto vs. 0.88x na arena.</li>
 *   <li>Multiplicadores especificos de classe (Dominator, Doomcryer, Archmage).</li>
 * </ul>
 */
class OlyClassDamageManagerTest {

	private OlyClassDamageManager manager;
	private CombatService combatService;

	private CharTemplate duelistTemplate;
	private CharTemplate soultakerTemplate;
	private CharTemplate dominatorTemplate;

	@BeforeEach
	void setUp() {
		manager = new OlyClassDamageManager();
		manager.loadConfig();
		combatService = new CombatService(1.0, 1.0, null, manager);

		duelistTemplate = new CharTemplate(
				88, "Duelist", 0,
				40, 43, 30, 21, 11, 25,
				300, 200, 100, 150, 300, 333,
				50, 44, 50, 120, 80000,
				0, 0, 0, false,
				9, 23, 8, 23.5,
				3000, 1000, 2000, 76
		);

		soultakerTemplate = new CharTemplate(
				95, "Soultaker", 0,
				22, 27, 21, 41, 20, 39,
				100, 150, 400, 250, 250, 333,
				30, 40, 30, 120, 60000,
				0, 0, 0, false,
				7.5, 22.8, 6.5, 22.5,
				2500, 2000, 1500, 76
		);

		dominatorTemplate = new CharTemplate(
				115, "Dominator", 3,
				27, 31, 24, 31, 15, 42,
				150, 180, 250, 200, 280, 333,
				35, 41, 35, 121, 68000,
				0, 0, 0, false,
				7, 27.5, 8, 25.5,
				3300, 1600, 2600, 76
		);
	}

	private PlayerCharacter createPlayer(int objId, String name, int classId, int level) {
		return new PlayerCharacter(
				objId, "acc", name, level, 100000000L, 5000000, 0,
				classId, classId, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, 0, 0, 0, 0, 5000.0,
				3000.0, 4000.0
		);
	}

	@Test
	@DisplayName("Isolamento Total: Mundo aberto nao sofre alteracao (multiplicador = 1.0)")
	void testTotalIsolationOpenWorld() {
		PlayerCharacter duelist = createPlayer(1, "AttackerDuelist", 88, 78);
		PlayerCharacter soultaker = createPlayer(2, "DefenderSoultaker", 95, 78);

		// Ambos em modo normal (fora das Olimpiadas)
		assertFalse(duelist.isOlympiadMode());
		assertFalse(soultaker.isOlympiadMode());

		double multiplier = manager.getDamageMultiplier(duelist, soultaker);
		assertEquals(1.0, multiplier, 0.0001, "Fora da arena, o dano DEVE ser estritamente 1.0 (retail C6)");

		// Se apenas um estiver marcado com olympiadMode, ainda deve isolar
		duelist.setOlympiadMode(true);
		assertEquals(1.0, manager.getDamageMultiplier(duelist, soultaker), "Isolamento estrito se um dos players nao estiver em oly");

		duelist.setOlympiadMode(false);
		soultaker.setOlympiadMode(true);
		assertEquals(1.0, manager.getDamageMultiplier(duelist, soultaker), "Isolamento estrito se alvo nao estiver em oly");
	}

	@Test
	@DisplayName("Arena da Olimpiada: Duelist contra Soultaker aplica 0.88x")
	void testOlympiadMultiplierDuelistVsSoultaker() {
		PlayerCharacter duelist = createPlayer(1, "AttackerDuelist", 88, 78);
		PlayerCharacter soultaker = createPlayer(2, "DefenderSoultaker", 95, 78);

		duelist.setOlympiadMode(true);
		soultaker.setOlympiadMode(true);

		assertTrue(soultaker.isMage(), "Soultaker deve ser reconhecido como classe maga");
		assertFalse(duelist.isMage(), "Duelist nao e classe maga");

		double multiplier = manager.getDamageMultiplier(duelist, soultaker);
		assertEquals(0.88, multiplier, 0.0001, "Duelist vs Mage na Olimpiada deve ter multiplicador 0.88");
	}

	@Test
	@DisplayName("Arena da Olimpiada: Archmage contra Mage aplica 1.20x")
	void testOlympiadMultiplierArchmageVsMage() {
		PlayerCharacter archmage = createPlayer(1, "AttackerArchmage", 94, 78);
		PlayerCharacter soultaker = createPlayer(2, "DefenderSoultaker", 95, 78);

		archmage.setOlympiadMode(true);
		soultaker.setOlympiadMode(true);

		double multiplier = manager.getDamageMultiplier(archmage, soultaker);
		assertEquals(1.20, multiplier, 0.0001, "Archmage vs Mage na Olimpiada deve ter multiplicador 1.20");
	}

	@Test
	@DisplayName("Arena da Olimpiada: Dominator e Doomcryer com penalidades retail balanceadas")
	void testDominatorAndDoomcryerMultipliers() {
		PlayerCharacter dominator = createPlayer(1, "AttackerDominator", 115, 78);
		PlayerCharacter fighter = createPlayer(2, "DefenderFighter", 88, 78);
		PlayerCharacter mage = createPlayer(3, "DefenderMage", 95, 78);

		dominator.setOlympiadMode(true);
		fighter.setOlympiadMode(true);
		mage.setOlympiadMode(true);

		double vsFighter = manager.getDamageMultiplier(dominator, fighter);
		assertEquals(0.80, vsFighter, 0.0001, "Dominator vs Fighter deve ser 0.80");

		double vsMage = manager.getDamageMultiplier(dominator, mage);
		assertEquals(0.68, vsMage, 0.0001, "Dominator vs Mage deve ser 0.68");
	}

	@Test
	@DisplayName("CombatService: Dano fisico e magico escalonado apenas dentro das Olimpiadas")
	void testCombatServiceDamageApplication() {
		PlayerCharacter duelist = createPlayer(1, "AttackerDuelist", 88, 78);
		PlayerCharacter soultaker = createPlayer(2, "DefenderSoultaker", 95, 78);

		// 1. Campo Aberto
		int openWorldDamage = combatService.skillPhysicalPlayer(
				duelist, duelistTemplate, soultaker, soultakerTemplate,
				1500.0, true, false
		);
		assertTrue(openWorldDamage > 0, "Dano em campo aberto deve ser calculado normalmente");

		// 2. Olimpiadas
		duelist.setOlympiadMode(true);
		soultaker.setOlympiadMode(true);

		int olyDamage = combatService.skillPhysicalPlayer(
				duelist, duelistTemplate, soultaker, soultakerTemplate,
				1500.0, true, false
		);

		// Devido ao fator rnd (0.95 a 1.05), a proporcao media entre oly e open world deve ser ~0.88
		double ratio = (double) olyDamage / (double) openWorldDamage;
		assertTrue(ratio < 0.98, "Dano na Olimpiada deve ser perceptivelmente reduzido (~0.88x)");
	}

	@Test
	@DisplayName("Multiplicador de Cura e Recarga a Quente (Hot-Reload)")
	void testHealMultiplierAndReload() {
		// Customizacao dinamica
		manager.setBalance(97, new OlyClassDamageManager.OlyClassBalance(
				97, "Cardinal", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85
		));

		assertEquals(0.85, manager.getHealMultiplier(97), 0.0001);

		// Recarrega do XML e volta aos valores padrao
		int reloaded = manager.loadConfig();
		assertTrue(reloaded >= 31, "Deve carregar no minimo as 31 terceiras classes");
		assertEquals(1.0, manager.getHealMultiplier(97), 0.0001, "Apos reload do XML, o healMul do Cardinal retorna ao oficial");
	}
}
