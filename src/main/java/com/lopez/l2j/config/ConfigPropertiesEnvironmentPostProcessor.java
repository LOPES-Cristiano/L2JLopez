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
 * sobrescreva os valores de application.yml e alimente ServerProperties e @Value.
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
			mapIfPresent(raw, "RateXp", springProps, "l2.rates.xp");
			mapIfPresent(raw, "RateSp", springProps, "l2.rates.sp");
			mapIfPresent(raw, "RatePartyXp", springProps, "l2.rates.party-xp");
			mapIfPresent(raw, "RatePartySp", springProps, "l2.rates.party-sp");
			mapIfPresent(raw, "RateDropAdena", springProps, "l2.rates.adena");
			mapIfPresent(raw, "RateDropItems", springProps, "l2.rates.drop");
			mapIfPresent(raw, "RateDropSpoil", springProps, "l2.rates.spoil");

			mapIfPresent(raw, "StartingAdena", springProps, "l2.game.starting-adena");

			mapIfPresent(raw, "ServerName", springProps, "l2.server-name");

			mapIfPresent(raw, "GameServerPort", springProps, "l2.network.game-port");
			mapIfPresent(raw, "GameserverPort", springProps, "l2.network.game-port");
			mapIfPresent(raw, "AuthServerPort", springProps, "l2.network.login-port");
			mapIfPresent(raw, "LoginPort", springProps, "l2.network.login-internal-port");
			mapIfPresent(raw, "AuthPort", springProps, "l2.network.login-internal-port");
			mapIfPresent(raw, "MinProtocolVersion", springProps, "l2.network.protocol-min");
			mapIfPresent(raw, "MaxProtocolVersion", springProps, "l2.network.protocol-max");

			mapIfPresent(raw, "AutoCreateAccounts", springProps, "l2.login.auto-create-accounts");
			mapIfPresent(raw, "MaxAccountCreationsPerIP", springProps, "l2.login.max-account-creations-per-ip");
			mapIfPresent(raw, "MaximumOnlineUsers", springProps, "l2.login.max-players");
			mapIfPresent(raw, "ShowLicence", springProps, "l2.login.show-licence");
			mapIfPresent(raw, "ExternalHostname", springProps, "l2.login.game-server-host");
			mapIfPresent(raw, "GameserverHostname", springProps, "l2.login.game-server-host");

			mapIfPresent(raw, "AutoLearnSkills", springProps, "l2.skills.auto-learn");
			mapIfPresent(raw, "AutoLearnMaxLevel", springProps, "l2.skills.auto-learn-max-level");
			mapIfPresent(raw, "SpBookNeeded", springProps, "l2.skills.sp-book-needed");
			mapIfPresent(raw, "CharMaxNumber", springProps, "l2.game.char-max-number");
			mapIfPresent(raw, "DeleteCharAfterDays", springProps, "l2.game.delete-char-after-days");

			// Geodata (options.properties)
			mapIfPresent(raw, "EnableGeoData", springProps, "l2.geodata.enabled");
			if (raw.containsKey("GeoDataRoot")) {
				String root = raw.get("GeoDataRoot").trim();
				String engine = raw.getOrDefault("GeoEngine", "geodata").trim();
				if (!root.endsWith("geodata") && !root.endsWith("geodata/")) {
					springProps.put("l2.geodata.dir", root.replaceAll("[/\\\\]+$", "") + "/" + engine);
				} else {
					springProps.put("l2.geodata.dir", root);
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
		String val = source.get(sourceKey);
		if (val == null) {
			val = ConfigLoader.getProperty(sourceKey);
		}
		if (val != null) {
			target.put(targetKey, val.trim());
		}
	}

	@Override
	public int getOrder() {
		// Executa bem cedo, antes da vinculacao de @ConfigurationProperties
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}
}
