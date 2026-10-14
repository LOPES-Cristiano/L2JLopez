package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de verificacao Anti-Bot por Captcha e aplicacao de punicao configuravel
 * (mods.properties: CAPTCHA_PUNISHMENT_TIME).
 */
@Service
public class AntiBotCaptchaService {

	private static final Logger log = LoggerFactory.getLogger(AntiBotCaptchaService.class);

	public record CaptchaChallenge(int playerId, String code, long expiresAt) {}

	private final Map<Integer, CaptchaChallenge> activeChallenges = new ConcurrentHashMap<>();
	private final Map<Integer, Long> punishmentEndTimes = new ConcurrentHashMap<>();

	public int getPunishmentMinutes() {
		return Math.max(1, Config.CAPTCHA_PUNISHMENT_TIME);
	}

	public long getPunishmentMillis() {
		return getPunishmentMinutes() * 60_000L;
	}

	public CaptchaChallenge issueChallenge(PlayerCharacter player) {
		if (player == null) {
			return null;
		}
		int codeNum = ThreadLocalRandom.current().nextInt(1000, 10000);
		String code = String.valueOf(codeNum);
		CaptchaChallenge challenge = new CaptchaChallenge(
				player.objectId(), code, System.currentTimeMillis() + 60_000L);
		activeChallenges.put(player.objectId(), challenge);
		return challenge;
	}

	public boolean verifyResponse(PlayerCharacter player, String answer) {
		if (player == null) {
			return false;
		}
		CaptchaChallenge challenge = activeChallenges.remove(player.objectId());
		if (challenge == null) {
			return true;
		}
		if (System.currentTimeMillis() > challenge.expiresAt()
				|| answer == null
				|| !challenge.code().equalsIgnoreCase(answer.trim())) {
			applyPunishment(player);
			return false;
		}
		return true;
	}

	public long applyPunishment(PlayerCharacter player) {
		if (player == null) {
			return 0L;
		}
		long durationMs = getPunishmentMillis();
		long until = System.currentTimeMillis() + durationMs;
		player.setInJail(true);
		player.disable(until, false);
		punishmentEndTimes.put(player.objectId(), until);
		log.info("AntiBotCaptcha: jogador {} punido por {} minutos (CAPTCHA_PUNISHMENT_TIME)",
				player.name(), Config.CAPTCHA_PUNISHMENT_TIME);
		return until;
	}

	public boolean isPunished(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		Long until = punishmentEndTimes.get(player.objectId());
		if (until == null) {
			return false;
		}
		if (System.currentTimeMillis() >= until) {
			punishmentEndTimes.remove(player.objectId());
			player.setInJail(false);
			return false;
		}
		return true;
	}
}
