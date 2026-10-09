package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.combat.CombatService.RelativePosition;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.trade.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerMechanicsAndCombatConfigurationTest {

	private PlayerCharacter createPlayer(int objId, String name) {
		return new PlayerCharacter(objId, "acc", name, 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@Test
	@DisplayName("Validar cálculo de posição relativa (Frente, Lado, Costas) e chances de Blow")
	void testBlowRelativePositionAndChances() {
		// Target at (0, 0) facing North (heading = 16384 -> 90 degrees)
		// Attacker at (0, 100) -> North -> Target is looking directly at attacker -> FRONT
		RelativePosition front = CombatService.getRelativePosition(0, 100, 0, 0, 16384);
		assertEquals(RelativePosition.FRONT, front);
		assertEquals(Config.BLOW_FRONT, CombatService.getBlowChance(front));

		// Attacker at (0, -100) -> South -> Behind target -> BEHIND
		RelativePosition behind = CombatService.getRelativePosition(0, -100, 0, 0, 16384);
		assertEquals(RelativePosition.BEHIND, behind);
		assertEquals(Config.BLOW_BEHIND, CombatService.getBlowChance(behind));

		// Attacker at (100, 0) -> East -> Side of target -> SIDE
		RelativePosition side = CombatService.getRelativePosition(100, 0, 0, 0, 16384);
		assertEquals(RelativePosition.SIDE, side);
		assertEquals(Config.BLOW_SIDE, CombatService.getBlowChance(side));
	}

	@Test
	@DisplayName("Validar multiplicadores configuráveis de Lethal Strike")
	void testLethalStrikeRateMultipliers() {
		assertEquals(Config.ALT_LETHAL_RATE_DAGGER, CombatService.getLethalRateMultiplier(true, false));
		assertEquals(Config.ALT_LETHAL_RATE_ARCHERY, CombatService.getLethalRateMultiplier(false, true));
		assertEquals(Config.ALT_LETHAL_RATE_OTHER, CombatService.getLethalRateMultiplier(false, false));

		assertTrue(CombatService.calcLethalSuccess(100.0, true, false));
		assertFalse(CombatService.calcLethalSuccess(0.0, false, false));
	}

	@Test
	@DisplayName("Validar restrições de Karma & PK no sistema de trocas (TradeService)")
	void testTradeKarmaRestrictions() {
		TradeService tradeService = new TradeService();
		PlayerCharacter normal1 = createPlayer(1001, "Normal1");
		PlayerCharacter normal2 = createPlayer(1002, "Normal2");
		PlayerCharacter pkPlayer = createPlayer(1003, "PK1");
		pkPlayer.karma(500);

		// With ALT_KARMA_PLAYER_CAN_TRADE = true (default)
		Config.ALT_KARMA_PLAYER_CAN_TRADE = true;
		assertTrue(tradeService.requestTrade(normal1, pkPlayer, msg -> {}));

		// With ALT_KARMA_PLAYER_CAN_TRADE = false
		Config.ALT_KARMA_PLAYER_CAN_TRADE = false;
		assertFalse(tradeService.requestTrade(normal2, pkPlayer, msg -> {}));
		assertFalse(tradeService.requestTrade(pkPlayer, normal2, msg -> {}));

		// Restaura padrao
		Config.ALT_KARMA_PLAYER_CAN_TRADE = true;
	}

	@Test
	@DisplayName("Validar configuracoes de consumo e tempos de PvP Flag")
	void testPvPFlagTimesAndConsumables() {
		assertEquals(10000, Config.PVP_VS_NORMAL_TIME);
		assertEquals(40000, Config.PVP_VS_PVP_TIME);
		assertTrue(Config.CONSUME_SOUL_SHOT);
		assertTrue(Config.CONSUME_ARROWS);
		assertFalse(Config.BLOCK_PARTY_INVITE_ON_COMBAT);
		assertFalse(Config.BLOCK_CHANGE_WEAPON_WHILE_ATTACKING);
		assertFalse(Config.ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE);
		assertFalse(Config.ALT_KARMA_PLAYER_CAN_SHOP);
		assertTrue(Config.ALT_KARMA_PLAYER_CAN_TELEPORT);
		assertFalse(Config.ALT_KARMA_PLAYER_CAN_USE_GK);
		assertTrue(Config.ALT_KARMA_PLAYER_CAN_USE_WAREHOUSE);
		assertNotNull(Config.LIST_OF_NON_DROPPABLE_ITEMS);
		assertTrue(Config.LIST_OF_NON_DROPPABLE_ITEMS.contains(57)); // Adena
	}
}
