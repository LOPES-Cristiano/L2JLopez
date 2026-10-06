package com.lopez.l2j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

	@BeforeAll
	static void setup() {
		Config.load();
	}

	@Test
	void loadsAllPropertiesFromConfigFolder() {
		Path configDir = ConfigLoader.getConfigRootDir();
		assertNotNull(configDir, "Diretorio de config deve ser resolvido");
		assertTrue(Files.exists(configDir), "Diretorio config deve existir");

		Map<String, String> all = ConfigLoader.getAllRawProperties();
		assertFalse(all.isEmpty(), "Propriedades devem ser carregadas");
		assertTrue(all.size() > 50, "Deve carregar mais de 50 propriedades dos arquivos .properties");

		Map<String, Map<String, String>> byFile = ConfigLoader.getAllFileProperties();
		assertTrue(byFile.containsKey("rates.properties"), "Deve carregar rates.properties");
		assertTrue(byFile.containsKey("custom.properties"), "Deve carregar custom.properties");
		assertTrue(byFile.containsKey("mods.properties"), "Deve carregar mods.properties");
		assertTrue(byFile.containsKey("gameserver.properties"), "Deve carregar gameserver.properties");
		assertTrue(byFile.containsKey("altgame.properties"), "Deve carregar altgame.properties");
		assertTrue(byFile.containsKey("player.properties"), "Deve carregar player.properties");
		assertTrue(byFile.containsKey("options.properties"), "Deve carregar options.properties");
		assertTrue(byFile.containsKey("authserver.properties"), "Deve carregar authserver.properties");
	}

	@Test
	void testRatesPropertiesLoaded() {
		assertEquals(100.0f, Config.RATE_XP, 0.001);
		assertEquals(100.0f, Config.RATE_SP, 0.001);
		assertEquals(1.0f, Config.RATE_PARTY_XP, 0.001);
		assertEquals(100.0f, Config.RATE_DROP_ADENA, 0.001);
		assertEquals(100.0f, Config.RATE_DROP_ITEMS, 0.001);
		assertEquals(1.0f, Config.RATE_DROP_SPOIL, 0.001);
	}

	@Test
	void testCustomPropertiesLoaded() {
		assertEquals(10_000_000, Config.STARTING_ADENA);
		assertEquals(0, Config.STARTING_AA);
		assertFalse(Config.ENABLE_STARTUP_LVL);
		assertEquals(1, Config.STARTUP_LVL);
		assertTrue(Config.ALLOW_MANA_POTIONS);
		assertEquals(200, Config.MANA_POTION_POWER);
	}

	@Test
	void testModsPropertiesLoaded() {
		assertFalse(Config.ALLOW_OFFLINE_TRADE);
		assertFalse(Config.ALLOW_OFFLINE_TRADE_CRAFT);
		assertTrue(Config.ALLOW_OFFLINE_TRADE_COLOR_NAME);
		assertEquals("999999", Config.OFFLINE_TRADE_COLOR_NAME);
		assertTrue(Config.RESTORE_OFFLINE_TRADERS);
	}

	@Test
	void testDynamicModificationTakesEffect() {
		float originalXp = Config.RATE_XP;
		try {
			Config.setProperty("RateXp", "250.0");
			assertEquals(250.0f, Config.RATE_XP, 0.001);
			assertEquals("250.0", Config.getProperty("RateXp"));

			Config.setProperty("StartingAdena", "50000000");
			assertEquals(50_000_000, Config.STARTING_ADENA);

			Config.setProperty("AllowOfflineTrade", "True");
			assertTrue(Config.ALLOW_OFFLINE_TRADE);
		} finally {
			Config.setProperty("RateXp", String.valueOf(originalXp));
			Config.reload();
		}
	}

	@Test
	void testNormalizedKeyLookup() {
		// Busca independente de case ou pontuacao
		String rateXp = ConfigLoader.getProperty("ratexp");
		assertNotNull(rateXp, "ratexp normalizado deve ser encontrado");

		String offlineTrade = ConfigLoader.getProperty("allow_offline_trade");
		assertNotNull(offlineTrade, "allow_offline_trade normalizado deve ser encontrado");
	}
}
