package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de recompensa por votos em rankings (Onda 14: vote.properties).
 * Controla os parametros de API (TopZone, HopZone, L2Network), intervalo de 12 horas e entrega de itens.
 */
@Service
public class VoteRewardService {

	private static final Logger log = LoggerFactory.getLogger(VoteRewardService.class);

	public static final long VOTE_COOLDOWN_MILLIS = 12 * 3600 * 1000L; // 12 horas
	public static final int DEFAULT_REWARD_ITEM_ID = 57;
	public static final int DEFAULT_REWARD_COUNT = 100_000;

	private InventoryService inventoryService;
	private CharacterVariablesService characterVariablesService;

	private final Map<Integer, Long> lastVoteTimes = new ConcurrentHashMap<>();
	private final Map<String, Long> siteVoteTimes = new ConcurrentHashMap<>();

	public VoteRewardService() {
	}

	@Autowired
	public VoteRewardService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) CharacterVariablesService characterVariablesService) {
		this.inventoryService = inventoryService;
		this.characterVariablesService = characterVariablesService;
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public void setCharacterVariablesService(CharacterVariablesService characterVariablesService) {
		this.characterVariablesService = characterVariablesService;
	}

	/**
	 * Verifica o cooldown do voto (em memória e via CharacterVariablesService "VOTE_LAST_" + siteName),
	 * registra o voto e entrega a recompensa (DEFAULT_REWARD_ITEM_ID = 57, DEFAULT_REWARD_COUNT = 100_000)
	 * na mochila e no banco do jogador via InventoryService.
	 */
	public synchronized boolean claimVoteReward(PlayerCharacter player, String siteName) {
		if (player == null) {
			return false;
		}
		String resolvedSite = (siteName == null || siteName.isBlank()) ? "DEFAULT" : siteName.trim();
		String varName = "VOTE_LAST_" + resolvedSite;
		String siteKey = player.objectId() + ":" + resolvedSite.toUpperCase();
		long now = System.currentTimeMillis();

		Long memLast = siteVoteTimes.get(siteKey);
		if (memLast != null && (now - memLast) < VOTE_COOLDOWN_MILLIS) {
			return false;
		}

		if (characterVariablesService != null) {
			String persisted = characterVariablesService.get(player.objectId(), varName);
			if (persisted != null && !persisted.isBlank()) {
				try {
					long lastTime = Long.parseLong(persisted.trim());
					if ((now - lastTime) < VOTE_COOLDOWN_MILLIS) {
						return false;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}

		siteVoteTimes.put(siteKey, now);
		lastVoteTimes.put(player.objectId(), now);
		if (characterVariablesService != null) {
			characterVariablesService.set(player.objectId(), varName, String.valueOf(now));
		}

		var inv = player.inventory();
		if (inv != null) {
			if (inventoryService != null) {
				var added = inventoryService.addItem(inv, DEFAULT_REWARD_ITEM_ID, DEFAULT_REWARD_COUNT, "VoteReward");
				if (added == null) {
					inv.addItem(DEFAULT_REWARD_ITEM_ID, DEFAULT_REWARD_COUNT);
				}
			} else {
				inv.addItem(DEFAULT_REWARD_ITEM_ID, DEFAULT_REWARD_COUNT);
			}
		}

		log.info("VoteReward: Jogador {} recebeu {}x item #{} por votar em {}",
				player.name(), DEFAULT_REWARD_COUNT, DEFAULT_REWARD_ITEM_ID, resolvedSite);
		return true;
	}

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
		siteVoteTimes.keySet().removeIf(k -> k.startsWith(objectId + ":"));
	}
}

