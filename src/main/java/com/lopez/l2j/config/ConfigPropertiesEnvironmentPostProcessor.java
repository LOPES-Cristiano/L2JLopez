package com.lopez.l2j.config;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Conecta os arquivos .properties legados do diretorio config/ diretamente
 * ao Environment do Spring Boot.
 * Garante que qualquer alteracao feita pelo usuario em config/**\/*.properties
 * alimente ServerProperties e @Value, eliminando a necessidade de duplicar
 * configuracoes no application.yml.
 */
public class ConfigPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final Logger log = LoggerFactory.getLogger(ConfigPropertiesEnvironmentPostProcessor.class);
	public static final String PROPERTY_SOURCE_NAME = "l2jConfigProperties";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		try {
			Config.load();
			Map<String, String> raw = ConfigLoader.getAllRawProperties();
			if (raw.isEmpty()) {
				log.warn("Nenhuma propriedade carregada pelo ConfigLoader em config/.");
				return;
			}

			Map<String, Object> springProps = new HashMap<>();

			// 1. Todas as propriedades brutas ficam acessíveis no Environment (ex: ${RateXp}, ${StartingAdena})
			springProps.putAll(raw);

			// 2. Mapeamento explicito para a arvore l2.* do ServerProperties
			// Rates (rates.properties)
			mapIfPresent(raw, "RateXp", springProps, "l2.rates.xp", "100.0");
			mapIfPresent(raw, "RateSp", springProps, "l2.rates.sp", "100.0");
			mapIfPresent(raw, "RatePartyXp", springProps, "l2.rates.party-xp", "1.0");
			mapIfPresent(raw, "RatePartySp", springProps, "l2.rates.party-sp", "1.0");
			mapIfPresent(raw, "RateDropAdena", springProps, "l2.rates.adena", "100.0");
			mapIfPresent(raw, "RateDropItems", springProps, "l2.rates.drop", "100.0");
			mapIfPresent(raw, "RateDropSpoil", springProps, "l2.rates.spoil", "1.0");

			// Server info & Game (gameserver.properties / custom.properties)
			mapIfPresent(raw, "ServerName", springProps, "l2.server-name", "L2JLopez");
			mapIfPresent(raw, "StartingAdena", springProps, "l2.game.starting-adena", "10000000");

			// Datapack paths (gameserver.properties)
			String dpRoot = raw.getOrDefault("DatapackRoot", ".");
			String baseDp = ".".equals(dpRoot.trim()) ? "data" : (dpRoot.trim().replaceAll("[/\\\\]+$", "") + "/data");
			springProps.putIfAbsent("l2.datapack.root-dir", baseDp);
			springProps.putIfAbsent("l2.datapack.html-dir", baseDp + "/html");
			springProps.putIfAbsent("l2.datapack.xml-dir", baseDp + "/xml");
			springProps.putIfAbsent("l2.datapack.scripts-dir", baseDp + "/scripts");
			springProps.putIfAbsent("l2.datapack.teleports-file", baseDp + "/xml/world/teleports.xml");
			springProps.putIfAbsent("l2.datapack.buylists-file", baseDp + "/xml/world/buylists.xml");

			// Network (network.properties)
			mapIfPresent(raw, "GameServerPort", springProps, "l2.network.game-port", "7777");
			mapIfPresent(raw, "GameserverPort", springProps, "l2.network.game-port", "7777");
			mapIfPresent(raw, "AuthServerPort", springProps, "l2.network.login-port", "2106");
			mapIfPresent(raw, "LoginPort", springProps, "l2.network.login-internal-port", "9014");
			mapIfPresent(raw, "AuthPort", springProps, "l2.network.login-internal-port", "9014");
			mapIfPresent(raw, "MinProtocolVersion", springProps, "l2.network.protocol-min", "730");
			mapIfPresent(raw, "MaxProtocolVersion", springProps, "l2.network.protocol-max", "746");

			// Login (authserver.properties / gameserver.properties / network.properties)
			mapIfPresent(raw, "AutoCreateAccounts", springProps, "l2.login.auto-create-accounts", "true");
			mapIfPresent(raw, "MaxAccountCreationsPerIP", springProps, "l2.login.max-account-creations-per-ip", "2");
			mapIfPresent(raw, "MaximumOnlineUsers", springProps, "l2.login.max-players", "1000");
			mapIfPresent(raw, "ShowLicence", springProps, "l2.login.show-licence", "true");
			mapIfPresent(raw, "ExternalHostname", springProps, "l2.login.game-server-host", "127.0.0.1");
			mapIfPresent(raw, "GameserverHostname", springProps, "l2.login.game-server-host", "127.0.0.1");

			// Listeners (respeita se o ambiente ja tiver desativado, ex: profile de teste)
			String envGameListen = environment.getProperty("l2.game.listen");
			if ("false".equalsIgnoreCase(envGameListen)) {
				springProps.put("l2.game.listen", "false");
			} else if (raw.containsKey("GameServerListen")) {
				springProps.put("l2.game.listen", raw.get("GameServerListen").trim().toLowerCase());
			} else {
				springProps.put("l2.game.listen", "true");
			}

			String envLoginListen = environment.getProperty("l2.login.listen");
			if ("false".equalsIgnoreCase(envLoginListen)) {
				springProps.put("l2.login.listen", "false");
			} else if (raw.containsKey("LoginServerListen")) {
				springProps.put("l2.login.listen", raw.get("LoginServerListen").trim().toLowerCase());
			} else {
				springProps.put("l2.login.listen", "true");
			}

			// Features (custom.properties / add-on.properties / mods.properties)
			mapIfPresent(raw, "AllowAutoFarm", springProps, "l2.features.autofarm.enabled", "true");
			mapIfPresent(raw, "AllowDressMe", springProps, "l2.features.dressme.enabled", "true");
			mapIfPresent(raw, "AllowPvpRank", springProps, "l2.features.pvp-rank.enabled", "true");
			mapIfPresent(raw, "AllowReset", springProps, "l2.features.reset.enabled", "true");
			mapIfPresent(raw, "AllowRoulette", springProps, "l2.features.roulette.enabled", "true");
			mapIfPresent(raw, "AllowAio", springProps, "l2.features.aio.enabled", "true");
			mapIfPresent(raw, "AllowVip", springProps, "l2.features.vip.enabled", "true");
			mapIfPresent(raw, "AllowAchievements", springProps, "l2.features.achievements.enabled", "true");
			mapIfPresent(raw, "AllowVote", springProps, "l2.features.vote.enabled", "false");
			mapIfPresent(raw, "AllowQuake", springProps, "l2.features.quake.enabled", "true");

			// Admin Superusers (access.properties)
			String superusers = raw.getOrDefault("SuperUsers", "Cristiano,cristiano,admin,Admin,gm,GM,root").trim();
			springProps.put("l2.admin.superusers", superusers);
			String[] suArray = superusers.split(",");
			for (int i = 0; i < suArray.length; i++) {
				springProps.put("l2.admin.superusers[" + i + "]", suArray[i].trim());
			}

			// Skills
			mapIfPresent(raw, "AutoLearnSkills", springProps, "l2.skills.auto-learn", "true");
			mapIfPresent(raw, "AutoLearnMaxLevel", springProps, "l2.skills.auto-learn-max-level", "0");
			mapIfPresent(raw, "SpBookNeeded", springProps, "l2.skills.sp-book-needed", "true");
			mapIfPresent(raw, "CharMaxNumber", springProps, "l2.game.char-max-number", "7");
			mapIfPresent(raw, "DeleteCharAfterDays", springProps, "l2.game.delete-char-after-days", "0");

			// Geodata (options.properties)
			mapIfPresent(raw, "EnableGeoData", springProps, "l2.geodata.enabled", "false");
			if (raw.containsKey("GeoDataRoot")) {
				String gRoot = raw.get("GeoDataRoot").trim();
				String engine = raw.getOrDefault("GeoEngine", "geodata").trim();
				if (!gRoot.endsWith("geodata") && !gRoot.endsWith("geodata/")) {
					springProps.put("l2.geodata.dir", gRoot.replaceAll("[/\\\\]+$", "") + "/" + engine);
				} else {
					springProps.put("l2.geodata.dir", gRoot);
				}
			}

			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, springProps));
			log.info("ConfigPropertiesEnvironmentPostProcessor: {} propriedades inseridas com alta prioridade no Spring Environment.",
					springProps.size());
		} catch (Exception e) {
			log.error("Erro ao processar propriedades no ConfigPropertiesEnvironmentPostProcessor", e);
		}
	}

	private static void mapIfPresent(Map<String, String> source, String sourceKey, Map<String, Object> target,
			String targetKey) {
		mapIfPresent(source, sourceKey, target, targetKey, null);
	}

	private static void mapIfPresent(Map<String, String> source, String sourceKey, Map<String, Object> target,
			String targetKey, String defaultValue) {
		String val = source.get(sourceKey);
		if (val == null) {
			val = ConfigLoader.getProperty(sourceKey);
		}
		if (val != null) {
			target.put(targetKey, val.trim());
		} else if (defaultValue != null && !target.containsKey(targetKey)) {
			target.put(targetKey, defaultValue);
		}
	}

	@Override
	public int getOrder() {
		// Executa bem cedo, antes da vinculacao de @ConfigurationProperties
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}
}
