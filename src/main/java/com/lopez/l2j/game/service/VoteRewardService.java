package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de recompensa por votos em rankings (Onda 14: vote.properties).
 * Controla os parametros de API (TopZone, HopZone, L2Network), intervalo de 12 horas e entrega de itens.
 */
@Service
public class VoteRewardService {

	private static final Logger log = LoggerFactory.getLogger(VoteRewardService.class);

	public static final long VOTE_COOLDOWN_MILLIS = 12 * 3600 * 1000L; // 12 horas

	private final Map<Integer, Long> lastVoteTimes = new ConcurrentHashMap<>();

	public boolean canVote(int objectId) {
		Long last = lastVoteTimes.get(objectId);
		if (last == null) {
			return true;
		}
		return (System.currentTimeMillis() - last) >= VOTE_COOLDOWN_MILLIS;
	}

	public long getCooldownRemainingMillis(int objectId) {
		Long last = lastVoteTimes.get(objectId);
		if (last == null) {
			return 0L;
		}
		long diff = System.currentTimeMillis() - last;
		return diff >= VOTE_COOLDOWN_MILLIS ? 0L : (VOTE_COOLDOWN_MILLIS - diff);
	}

	public boolean recordVote(int objectId) {
		if (!canVote(objectId)) {
			return false;
		}
		lastVoteTimes.put(objectId, System.currentTimeMillis());
		return true;
	}

	public int getRewardItemId() {
		return Config.VOTE_SYSTEM_REWARD_ID;
	}

	public int getRewardItemCount() {
		return Config.VOTE_SYSTEM_REWARD_COUNT;
	}

	public String getTopzoneApiKey() {
		return Config.API_KEY_TOPZONE;
	}

	public String getTopzoneServerId() {
		return Config.SERVER_ID_KEY_TOPZONE;
	}

	public String getHopzoneApiKey() {
		return Config.API_KEY_HOPZONE;
	}

	public String getNetworkServerId() {
		return Config.SERVER_ID_NETWORK;
	}

	public void clearCooldown(int objectId) {
		lastVoteTimes.remove(objectId);
	}
}
