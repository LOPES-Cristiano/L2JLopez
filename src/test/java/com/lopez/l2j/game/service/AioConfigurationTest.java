package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AioConfigurationTest {

	private AioService aioService;

	private PlayerCharacter createPlayer(int objId, String name, int classId, int level) {
		PlayerCharacter p = new PlayerCharacter(objId, "acc", name, level, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		p.classId(classId);
		return p;
	}

	@BeforeEach
	void setUp() {
		Config.load();
		aioService = new AioService();
	}

	@Test
	void testAioSystemConfigLoading() {
		assertTrue(Config.ENABLE_AIO_SYSTEM);
		assertEquals(1, Config.AIO_SET_DELEVEL);
		assertFalse(Config.ENABLE_AIO_DELEVEL);
		assertFalse(Config.ALLOW_AIO_LEAVE_TOWN);
		assertFalse(Config.ALLOW_AIO_SPEAK_NPC);
		assertFalse(Config.ALLOW_AIO_TELEPORT);
		assertTrue(Config.ALLOW_AIO_NAME_COLOR);
		assertEquals("88AA88", Config.AIO_NAME_COLOR);
		assertTrue(Config.ALLOW_AIO_DUAL);
		assertEquals(9225, Config.AIO_ITEM_ID);
		assertTrue(Config.BUFF_SHOP_ENABLE);
		assertEquals(14, Config.BUFF_SHOP_MAX_DAYS);
		assertEquals(24, Config.DEFAULT_BUFF_SHOP_SLOTS);
	}

	@Test
	void testAioStatusGrantAndColors() {
		PlayerCharacter player = createPlayer(1001, "AioBuffer", 10, 80);
		aioService.setAioStatus(player, true);

		assertTrue(player.isAio());
		assertTrue(aioService.isAio(player.getObjectId()));
		assertEquals(Integer.decode("0x88AA88"), player.nameColor());
		assertEquals(Integer.decode("0x88AA88"), player.titleColor());

		aioService.setAioStatus(player, false);
		assertFalse(player.isAio());
		assertFalse(aioService.isAio(player.getObjectId()));
	}

	@Test
	void testAioDelevelOnExpire() {
		PlayerCharacter player = createPlayer(1002, "AioDelevel", 10, 80);
		aioService.setAioStatus(player, true);

		Config.ENABLE_AIO_DELEVEL = true;
		Config.AIO_SET_DELEVEL = 1;

		aioService.setAioStatus(player, false);
		assertEquals(1, player.getLevel());

		Config.ENABLE_AIO_DELEVEL = false;
	}

	@Test
	void testAioRestrictions() {
		PlayerCharacter aioPlayer = createPlayer(1003, "AioChar", 10, 80);
		PlayerCharacter normalPlayer = createPlayer(1004, "NormalChar", 0, 80);

		aioService.setAioStatus(aioPlayer, true);

		// Leave town
		assertFalse(aioService.canLeaveTown(aioPlayer));
		assertTrue(aioService.canLeaveTown(normalPlayer));

		// Speak NPC
		assertFalse(aioService.canSpeakNpc(aioPlayer));
		assertTrue(aioService.canSpeakNpc(normalPlayer));

		// Teleport
		assertFalse(aioService.canTeleport(aioPlayer));
		assertTrue(aioService.canTeleport(normalPlayer));

		// Peace Zone Cast
		assertTrue(aioService.canCastBuffs(aioPlayer, true));
		assertFalse(aioService.canCastBuffs(aioPlayer, false));
		assertTrue(aioService.canCastBuffs(normalPlayer, false));

		// AIO Dual
		assertTrue(aioService.canEquipWeapon(aioPlayer, 9209));
		Config.ALLOW_AIO_DUAL = false;
		assertFalse(aioService.canEquipWeapon(aioPlayer, 9209));
		Config.ALLOW_AIO_DUAL = true;
	}

	@Test
	void testAioAllowedClasses() {
		assertTrue(aioService.isClassAllowed(10)); // Human Mystic
		assertTrue(aioService.isClassAllowed(25)); // Elf Mystic
		assertTrue(aioService.isClassAllowed(38)); // Dark Elf Mystic
		assertFalse(aioService.isClassAllowed(0)); // Human Fighter
		assertFalse(aioService.isClassAllowed(44)); // Orc Fighter
	}

	@Test
	void testBuffShopConfigIntegration() {
		assertTrue(aioService.isBuffShopEnabled());
		assertEquals(14, aioService.getBuffShopMaxDays());
		assertEquals(24, aioService.getDefaultBuffShopSlots());
	}
}
