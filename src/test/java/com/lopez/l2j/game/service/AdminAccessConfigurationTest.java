package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminAccessConfigurationTest {

	private PlayerCharacter createPlayer(int objId, String name, int accessLevel) {
		PlayerCharacter p = new PlayerCharacter(objId, "acc", name, 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		p.accessLevel(accessLevel);
		return p;
	}

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@Test
	void testAccessPropertiesLoading() {
		assertFalse(Config.GM_STARTUP_INVISIBLE);
		assertFalse(Config.GM_STARTUP_INVULNERABLE);
		assertFalse(Config.GM_STARTUP_SILENCE);
		assertFalse(Config.GM_STARTUP_AUTO_LIST);
		assertFalse(Config.SHOW_GM_LOGIN);
		assertFalse(Config.EVERYONE_HAS_ADMIN_RIGHTS);
		assertFalse(Config.GM_ITEM_RESTRICTION);
		assertEquals(65535, Config.GM_MAX_ENCHANT);
		assertEquals(60, Config.STANDARD_RESPAWN_DELAY);
		assertTrue(Config.GM_AUDIT);
		assertTrue(Config.SHOW_HTML_CHAT);
		assertEquals("FF9900", Config.GM_NAME_COLOR);
		assertEquals("0099FF", Config.GM_TITLE_COLOR);
	}

	@Test
	void testGmColorsAppliedOnCharacter() {
		PlayerCharacter gm = createPlayer(7001, "AdminMaster", 100);

		if (gm.accessLevel() > 0 && Config.GM_NAME_COLOR != null && !Config.GM_NAME_COLOR.isBlank()) {
			gm.nameColor(Integer.decode("0x" + Config.GM_NAME_COLOR));
		}
		if (gm.accessLevel() > 0 && Config.GM_TITLE_COLOR != null && !Config.GM_TITLE_COLOR.isBlank()) {
			gm.titleColor(Integer.decode("0x" + Config.GM_TITLE_COLOR));
		}

		assertEquals(Integer.decode("0xFF9900"), gm.nameColor());
		assertEquals(Integer.decode("0x0099FF"), gm.titleColor());
	}
}
