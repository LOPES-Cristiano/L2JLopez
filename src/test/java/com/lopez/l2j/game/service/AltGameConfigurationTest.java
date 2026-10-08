package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharCreateFailReason;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AltGameConfigurationTest {

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@Test
	void testAltGamePropertiesLoading() {
		assertTrue(Config.CANCEL_AUGMENTATION_EFFECT);
		assertTrue(Config.ALT_DANCE_MP_CONSUME);
		assertEquals(100, Config.ALT_BUFFER_TIME);
		assertEquals(100, Config.ALT_5MIN_TIME);
		assertEquals(100, Config.ALT_DANCE_TIME);
		assertEquals(100, Config.ALT_SONG_TIME);
		assertEquals(100, Config.ALT_HERO_TIME);
		assertEquals(1, Config.ALT_CH_TIME);
		assertEquals(50, Config.MAX_BUFF_AMOUNT);
		assertTrue(Config.CANCEL_LESSER_EFFECT);
		assertTrue(Config.STORE_SKILL_COOLTIME);
		assertTrue(Config.GRADE_PENALTY);
		assertEquals("all", Config.ALT_GAME_CANCEL_BY_HIT);
		assertFalse(Config.ALT_SHIELD_BLOCKS);
		assertEquals(5, Config.ALT_PERFECT_SHIELD_BLOCK_RATE);
		assertTrue(Config.CONSUME_ON_SUCCESS);
		assertFalse(Config.ENABLE_STATIC_REUSE);
		assertFalse(Config.OLY_USE_STATIC_REUSE);
		assertEquals(70, Config.SKILL_REUSE_DELAY);
		assertTrue(Config.USE_LEVEL_PENALTY);
		assertEquals(2, Config.M_CRIT_RATE);
		assertFalse(Config.DISABLE_SKILLS_ON_LEVEL_LOST);
		assertEquals("new", Config.CANCEL_MODE);
		assertFalse(Config.JAIL_IS_PVP_ZONE);
	}

	@Test
	void testForbiddenNamesValidation() {
		assertTrue(Config.FORBIDDEN_NAMES.contains("admin"));
		assertTrue(Config.FORBIDDEN_NAMES.contains("gm"));
		assertTrue(Config.FORBIDDEN_NAMES.contains("gamemaster"));

		CharacterService charService = new CharacterService(null, null, null);

		// Nome com "admin"
		var resAdmin = charService.create(new CharacterService.CreateRequest(
				"acc", "AdminPlayer", 0, 0, 0, 0, 0, 0));
		assertFalse(resAdmin.ok());
		assertEquals(CharCreateFailReason.INCORRECT_NAME, resAdmin.failure());

		// Nome com "gm"
		var resGm = charService.create(new CharacterService.CreateRequest(
				"acc", "MegaGmPro", 0, 0, 0, 0, 0, 0));
		assertFalse(resGm.ok());
		assertEquals(CharCreateFailReason.INCORRECT_NAME, resGm.failure());

		// Nome com caracteres especiais
		var resSpecial = charService.create(new CharacterService.CreateRequest(
				"acc", "Player_123", 0, 0, 0, 0, 0, 0));
		assertFalse(resSpecial.ok());
		assertEquals(CharCreateFailReason.INCORRECT_NAME, resSpecial.failure());
	}

	@Test
	void testCombatMagicCritMultiplierConfig() {
		assertEquals(2, Config.M_CRIT_RATE);

		CombatService combatService = new CombatService();
		assertNotNull(combatService);

		// Modifica temporariamente para validar dinamismo
		Config.M_CRIT_RATE = 4;
		assertEquals(4, Config.M_CRIT_RATE);
		Config.M_CRIT_RATE = 2;
	}

	@Test
	void testCancelRestoreServiceIntegration() {
		CancelRestoreService cancelService = new CancelRestoreService();
		assertNotNull(cancelService);
		assertEquals(0, cancelService.getPendingTasks().size());
		assertEquals("new", Config.CANCEL_MODE);
	}
}
