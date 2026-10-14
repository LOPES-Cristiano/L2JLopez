package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de cores e titulos progressivos por PvP (PvP Color System de mods.properties)
 * e formatacao de anuncios de PvP/PK (ANNOUNCE_PK_PVP_NORMAL_MESSAGE de add-on.properties).
 */
@Service
public class QuakeSystemService {

	private static final Logger log = LoggerFactory.getLogger(QuakeSystemService.class);

	public record PvpTierReward(int requiredPvp, int colorRgb, String title) {}

	/**
	 * Aplica a cor de nome/titulo e o titulo correspondente a quantidade de PvPs do jogador
	 * conforme configurado em mods.properties (PVP_COLOR_SYSTEM, PVP_COLOR_MODE, PVP_AMMOUNT_1..5).
	 */
	public boolean applyPvpColorAndTitle(PlayerCharacter player) {
		if (player == null || !Config.PVP_COLOR_SYSTEM) {
			return false;
		}
		int pvps = player.pvpKills();
		int matchedColor = -1;
		String matchedTitle = null;

		if (Config.PVP_AMMOUNT_5 > 0 && pvps >= Config.PVP_AMMOUNT_5) {
			matchedColor = parseHexColor(Config.COLOR_FOR_AMMOUNT_5);
			matchedTitle = Config.TITLE_FOR_AMMOUNT_5;
		} else if (Config.PVP_AMMOUNT_4 > 0 && pvps >= Config.PVP_AMMOUNT_4) {
			matchedColor = parseHexColor(Config.COLOR_FOR_AMMOUNT_4);
			matchedTitle = Config.TITLE_FOR_AMMOUNT_4;
		} else if (Config.PVP_AMMOUNT_3 > 0 && pvps >= Config.PVP_AMMOUNT_3) {
			matchedColor = parseHexColor(Config.COLOR_FOR_AMMOUNT_3);
			matchedTitle = Config.TITLE_FOR_AMMOUNT_3;
		} else if (Config.PVP_AMMOUNT_2 > 0 && pvps >= Config.PVP_AMMOUNT_2) {
			matchedColor = parseHexColor(Config.COLOR_FOR_AMMOUNT_2);
			matchedTitle = Config.TITLE_FOR_AMMOUNT_2;
		} else if (Config.PVP_AMMOUNT_1 > 0 && pvps >= Config.PVP_AMMOUNT_1) {
			matchedColor = parseHexColor(Config.COLOR_FOR_AMMOUNT_1);
			matchedTitle = Config.TITLE_FOR_AMMOUNT_1;
		}

		if (matchedColor < 0 && (matchedTitle == null || matchedTitle.isBlank())) {
			return false;
		}

		String mode = Config.PVP_COLOR_MODE != null ? Config.PVP_COLOR_MODE.trim() : "Both";
		if (matchedColor >= 0) {
			if ("Name".equalsIgnoreCase(mode) || "Both".equalsIgnoreCase(mode)) {
				player.nameColor(matchedColor);
			}
			if ("Title".equalsIgnoreCase(mode) || "Both".equalsIgnoreCase(mode)) {
				player.titleColor(matchedColor);
			}
		}
		if (matchedTitle != null && !matchedTitle.isBlank()
				&& ("Title".equalsIgnoreCase(mode) || "Both".equalsIgnoreCase(mode))) {
			player.title(matchedTitle.trim());
		}
		log.debug("PvPColorSystem aplicado para {} ({} PvPs, mode={})", player.name(), pvps, mode);
		return true;
	}

	/**
	 * Retorna se os anuncios de PvP/PK devem ser enviados como mensagem normal em vez de Critical Announce.
	 */
	public boolean useNormalMessageForPvpPkAnnounce() {
		return Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE;
	}

	public String formatKillAnnouncement(String killerName, String victimName, boolean isPvp) {
		String prefix = Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE ? "[Normal]" : "[Announce]";
		if (isPvp) {
			return prefix + " " + killerName + " derrotou " + victimName + " em combate PvP!";
		}
		return prefix + " " + killerName + " assassinou " + victimName + " (PK)!";
	}

	public static int parseHexColor(String hex) {
		if (hex == null || hex.isBlank()) {
			return -1;
		}
		String clean = hex.trim();
		if (clean.startsWith("0x") || clean.startsWith("0X")) {
			clean = clean.substring(2);
		} else if (clean.startsWith("#")) {
			clean = clean.substring(1);
		}
		try {
			return Integer.parseInt(clean, 16);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
