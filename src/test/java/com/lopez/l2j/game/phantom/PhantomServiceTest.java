package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários para a Onda C8:
 * Simulação de População (Phantoms / Fake Players) com 7 Kits de Combate e Virtual Threads.
 *
 * <p>Critério de Aceite oficial:</p>
 * <ul>
 *   <li>Simulação de combate de 1 Phantom guerreiro e 1 Phantom curador contra monstros.</li>
 *   <li>Validação do uso de skills ofensivas pelo guerreiro e curas automáticas no aliado pelo curador.</li>
 *   <li>Kiting tático de arqueiros, consumo automático de poções e comércio social em cidades.</li>
 * </ul>
 */
class PhantomServiceTest {

	private PhantomService phantomService;

	@BeforeEach
	void setUp() {
		phantomService = new PhantomService(true, null);
	}

	private PlayerCharacter createPlayer(int objId, String name, int classId, int x, int y, int z) {
		return new PlayerCharacter(
				objId, "acc_" + objId, name, 78, 100000000L, 5000000, 0,
				classId, classId, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, x, y, z, 0, 5000.0,
				3000.0, 4000.0
		);
	}

	private NpcInstance createMonster(int objId, int x, int y, int z, int hp) {
		NpcTemplate template = new NpcTemplate(
				20100, 20100, "Ketra Orc Raider", false, "", false,
				10.0, 15.0, 75, "male", "L2Monster", 75,
				hp, 2000, 1500, 1000, 500, 400,
				150, 100, 100, 50, 100, 10000, 1000, 1000, false
		);
		return new NpcInstance(objId, template, x, y, z, 0);
	}

	@Test
	@DisplayName("Criterio Onda C8: 7 Kits de Combate Oficiais definidos e configurados")
	void testAllSevenCombatKitsDefined() {
		assertEquals(7, PhantomCombatKit.values().length, "Devem existir exatamente 7 kits de combate");

		assertNotNull(PhantomCombatKit.FIGHTER_BURST);
		assertNotNull(PhantomCombatKit.FIGHTER_DPS);
		assertNotNull(PhantomCombatKit.ARCHER_KITE);
		assertNotNull(PhantomCombatKit.MAGE_NUKE);
		assertNotNull(PhantomCombatKit.DAGGER);
		assertNotNull(PhantomCombatKit.MAGE_DOT_CC);
		assertNotNull(PhantomCombatKit.HEALER_SUPPORT);

		assertTrue(PhantomCombatKit.ARCHER_KITE.isKiting(), "Archer deve ter flag de kiting ativa");
		assertTrue(PhantomCombatKit.HEALER_SUPPORT.isHealer(), "Healer deve ter flag de healer ativa");
		assertFalse(PhantomCombatKit.FIGHTER_BURST.isHealer());
	}

	@Test
	@DisplayName("Criterio Onda C8: Phantom Guerreiro (FIGHTER_BURST) usa skills do kit ao enfrentar monstro")
	void testFighterBurstMonsterCombat() {
		// Phantom Gladiator/Duelist nível 78 com MP cheio
		PlayerCharacter warrior = createPlayer(950001, "PhantomWarrior", 88, 1000, 1000, -3000);
		NpcInstance monster = createMonster(50001, 1050, 1000, -3000, 5000);

		PhantomAction action = phantomService.processTick(
				warrior,
				PhantomCombatKit.FIGHTER_BURST,
				PhantomBehaviorMode.HUNTING,
				List.of(monster),
				List.of()
		);

		assertNotNull(action);
		assertEquals(PhantomAction.ActionType.CAST_SKILL, action.type(), "Guerreiro com MP disponivel deve conjurar skill de dano do kit");
		assertEquals(monster.objectId(), action.targetId());
		assertEquals(261, action.skillId(), "Deve usar Triple Sonic Buster (skillId 261)");
	}

	@Test
	@DisplayName("Criterio Onda C8: Phantom Curador (HEALER_SUPPORT) prioriza curar aliado ferido com HP < 70%")
	void testHealerSupportPrioritizesWoundedAlly() {
		// Healer nível 78
		PlayerCharacter healer = createPlayer(950002, "PhantomHealer", 97, 1000, 1000, -3000);

		// Guerreiro aliado ferido (HP atual = 1500 / 5000 = 30%)
		PlayerCharacter woundedWarrior = createPlayer(950001, "PhantomWarrior", 88, 1020, 1000, -3000);
		woundedWarrior.currentHp(1500.0);

		// Monstro proximo que tambem poderia ser atacado
		NpcInstance monster = createMonster(50002, 1100, 1000, -3000, 5000);

		PhantomAction action = phantomService.processTick(
				healer,
				PhantomCombatKit.HEALER_SUPPORT,
				PhantomBehaviorMode.HUNTING,
				List.of(monster),
				List.of(woundedWarrior)
		);

		assertNotNull(action);
		assertEquals(PhantomAction.ActionType.CAST_SKILL, action.type(), "Curador DEVE priorizar curar o aliado ferido ao inves de bater no monstro");
		assertEquals(woundedWarrior.objectId(), action.targetId(), "O alvo da acao deve ser o aliado ferido");
		assertEquals(1218, action.skillId(), "Deve usar Greater Battle Heal (skillId 1218)");
	}

	@Test
	@DisplayName("Criterio Onda C8: Phantom Arqueiro (ARCHER_KITE) recua (kiting) quando o monstro se aproxima demais")
	void testArcherKitingTactics() {
		PlayerCharacter archer = createPlayer(950003, "PhantomArcher", 92, 1000, 1000, -3000);
		// Monstro a 100 unidades (distancia menor que minDistance = 250)
		NpcInstance closeMonster = createMonster(50003, 1100, 1000, -3000, 4000);

		PhantomAction action = phantomService.processTick(
				archer,
				PhantomCombatKit.ARCHER_KITE,
				PhantomBehaviorMode.HUNTING,
				List.of(closeMonster),
				List.of()
		);

		assertNotNull(action);
		assertEquals(PhantomAction.ActionType.KITE_RETREAT, action.type(), "Arqueiro deve recuar quando monstro estiver muito proximo");
		assertEquals(closeMonster.objectId(), action.targetId());
		assertTrue(action.x() < 1000, "Coordenada de recuo deve se afastar do monstro (sentido oposto ao alvo)");
	}

	@Test
	@DisplayName("Criterio Onda C8: Gestao automatica de consumiveis (Pocao de HP quando < 60%)")
	void testAutoPotionsConsumption() {
		PlayerCharacter phantom = createPlayer(950004, "PhantomPots", 88, 1000, 1000, -3000);
		// HP em 50% (2500 / 5000)
		phantom.currentHp(2500.0);

		PhantomAction action = phantomService.processTick(
				phantom,
				PhantomCombatKit.FIGHTER_DPS,
				PhantomBehaviorMode.HUNTING,
				List.of(),
				List.of()
		);

		assertNotNull(action);
		assertEquals(PhantomAction.ActionType.USE_POTION, action.type());
		assertEquals(PhantomConsumableManager.GREATER_HEALING_POTION, action.skillId());
	}

	@Test
	@DisplayName("Criterio Onda C8: Modo Loja Privada em Vila (TOWN_STORE) sentando e registrando titulo")
	void testTownStoreBehavior() {
		PlayerCharacter merchant = createPlayer(950005, "PhantomVendor", 54, 83400, 148000, -3400);
		merchant.storeTitle("Buying materials / adena");

		PhantomAction action = phantomService.processTick(
				merchant,
				PhantomCombatKit.FIGHTER_DPS,
				PhantomBehaviorMode.TOWN_STORE,
				List.of(),
				List.of()
		);

		assertNotNull(action);
		assertEquals(PhantomAction.ActionType.SIT_STORE, action.type());
		assertTrue(merchant.sitting(), "O personagem deve sentar ao abrir loja de compra/venda");
		assertEquals(1, merchant.privateStoreType(), "Tipo da loja deve ser 1 (STORE_PRIVATE_SELL)");
	}

	@Test
	@DisplayName("Criterio Onda C8: Seguranca e Isolamento de Phantoms de rankings publicos")
	void testPhantomIsolationAndLifecycle() {
		PhantomProfile profile = new PhantomProfile(
				"PhantomGhost", 88, 78, PhantomCombatKit.FIGHTER_BURST,
				1000, 1000, -3000, PhantomBehaviorMode.HUNTING
		);

		PlayerCharacter phantom = phantomService.spawnPhantom(profile);
		assertNotNull(phantom);
		assertTrue(phantom.isPhantom(), "PlayerCharacter gerado por PhantomService DEVE ter isPhantom=true");
		assertEquals(1, phantomService.count());

		// Despawn limpo
		boolean removed = phantomService.despawnPhantom(phantom.objectId());
		assertTrue(removed);
		assertEquals(0, phantomService.count());
	}
}
