package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de premiacoes e notificacoes para abates PvP e PK.
 * Configuracoes mapeadas de config/game/custom/add-on.properties:
 * AllowPvpRewardSystem, PvpRewardItem, AllowPkRewardSystem, PkRewardItem,
 * AnnouncePkPvP, AnnouncePkMsg, AnnouncePvpMsg, PvPCongratulationsMsg.
 * Inclui protecao anti-feed (mesmo IP, mesmo cla e tempo minimo entre abates do mesmo alvo).
 */
@Service
public class PvpRewardService {

	private static final Logger log = LoggerFactory.getLogger(PvpRewardService.class);

	public record RewardItem(int itemId, int count) {}

	private final PvPRankService pvpRankService;
	private final PvPColorService pvpColorService;

	private final Map<String, Long> lastKillTimes = new ConcurrentHashMap<>();
	private int cooldownMinutes = 5;

	@Autowired
	public PvpRewardService(@Autowired(required = false) PvPRankService pvpRankService,
			@Autowired(required = false) PvPColorService pvpColorService) {
		this.pvpRankService = pvpRankService;
		this.pvpColorService = pvpColorService;
		com.lopez.l2j.network.game.GameSession.Context.setGlobalPvpRewardService(this);
	}

	@jakarta.annotation.PostConstruct
	public void initGlobal() {
		com.lopez.l2j.network.game.GameSession.Context.setGlobalPvpRewardService(this);
	}

	public List<RewardItem> parseRewardItems(String raw) {
		List<RewardItem> list = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return list;
		}
		String[] entries = raw.split(";");
		for (String entry : entries) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			String[] parts = trimmed.split(",");
			if (parts.length == 2) {
				try {
					int itemId = Integer.parseInt(parts[0].trim());
					int count = Integer.parseInt(parts[1].trim());
					if (itemId > 0 && count > 0) {
						list.add(new RewardItem(itemId, count));
					}
				} catch (NumberFormatException ignored) {}
			}
		}
		return list;
	}

	public boolean checkAntiFarm(GameSession killerSession, GameSession victimSession) {
		if (killerSession == null || victimSession == null) {
			return false;
		}
		PlayerCharacter killer = killerSession.activeCharacter();
		PlayerCharacter victim = victimSession.activeCharacter();
		if (killer == null || victim == null || killer.objectId() == victim.objectId()) {
			return false;
		}

		String kIp = killerSession.ip();
		String vIp = victimSession.ip();
		if (kIp != null && vIp != null && !kIp.isEmpty() && kIp.equals(vIp) && !kIp.equals("127.0.0.1")) {
			log.warn("PvpRewardService: Anti-farm acionado! Mesmo IP entre {} e {}: {}", killer.name(), victim.name(), kIp);
			return false;
		}

		if (killer.clanId() > 0 && killer.clanId() == victim.clanId()) {
			log.info("PvpRewardService: Anti-farm acionado! Mesmo cla entre {} e {}", killer.name(), victim.name());
			return false;
		}

		String key = killer.objectId() + "_" + victim.objectId();
		long now = System.currentTimeMillis();
		Long lastKill = lastKillTimes.get(key);
		if (lastKill != null) {
			long diffMin = (now - lastKill) / (60 * 1000);
			if (diffMin < cooldownMinutes) {
				log.info("PvpRewardService: Anti-farm! Intervalo minimo de {} min nao decorrido entre {} e {}",
						cooldownMinutes, killer.name(), victim.name());
				return false;
			}
		}
		lastKillTimes.put(key, now);
		return true;
	}

	public void handleKill(GameSession killerSession, GameSession victimSession) {
		if (killerSession == null || victimSession == null) {
			return;
		}
		PlayerCharacter killer = killerSession.activeCharacter();
		PlayerCharacter victim = victimSession.activeCharacter();
		if (killer == null || victim == null || killer.objectId() == victim.objectId()) {
			return;
		}

		boolean isPvp = victim.pvpFlag() > 0 || victim.karma() > 0;

		if (isPvp) {
			killer.pvpKills(killer.pvpKills() + 1);

			if (Config.PVP_CONGRATULATIONS_MSG) {
				killerSession.send(new CreatureSay(0, CreatureSay.ALL, "PvP",
						"Voce ganhou 1 ponto de PvP! Total: " + killer.pvpKills()));
			}

			if (Config.ANNOUNCE_PK_PVP) {
				String msg = Config.ANNOUNCE_PVP_MSG
						.replace("$killer", killer.name())
						.replace("$target", victim.name());
				broadcastAnnounce(killerSession, msg);
			}

			if (Config.ALLOW_PVP_REWARD_SYSTEM && checkAntiFarm(killerSession, victimSession)) {
				List<RewardItem> rewards = parseRewardItems(Config.PVP_REWARD_ITEM);
				giveRewards(killerSession, rewards, "PvPReward");
			}

			if (pvpRankService != null) {
				pvpRankService.registerPvPKill(killer.objectId(), killerSession.ip(), victim.objectId(), victimSession.ip(), false);
			}
		} else {
			killer.pkKills(killer.pkKills() + 1);
			int karmaToAdd = Math.max(1000, 2000 * killer.pkKills());
			killer.karma(killer.karma() + karmaToAdd);

			if (Config.ANNOUNCE_PK_PVP) {
				String msg = Config.ANNOUNCE_PK_MSG
						.replace("$killer", killer.name())
						.replace("$target", victim.name());
				broadcastAnnounce(killerSession, msg);
			}

			if (Config.ALLOW_PK_REWARD_SYSTEM && checkAntiFarm(killerSession, victimSession)) {
				List<RewardItem> rewards = parseRewardItems(Config.PK_REWARD_ITEM);
				giveRewards(killerSession, rewards, "PkReward");
			}

			if (pvpRankService != null) {
				pvpRankService.registerPvPKill(killer.objectId(), killerSession.ip(), victim.objectId(), victimSession.ip(), true);
			}
		}

		if (pvpColorService != null) {
			int newNameColor = pvpColorService.getNameColor(killer.pvpKills(), killer.nameColor());
			if (newNameColor != killer.nameColor()) {
				killer.nameColor(newNameColor);
			}
			int newTitleColor = pvpColorService.getTitleColor(killer.pvpKills(), killer.titleColor());
			if (newTitleColor != killer.titleColor()) {
				killer.titleColor(newTitleColor);
			}
		}

		killerSession.sendUserInfoAndBroadcastCharInfo();
		if (killerSession.context() != null && killerSession.context().characters() != null) {
			killerSession.context().characters().save(killer, true);
		}
	}

	private void giveRewards(GameSession session, List<RewardItem> rewards, String process) {
		if (session == null || session.context() == null || session.context().inventories() == null) {
			return;
		}
		PlayerCharacter c = session.activeCharacter();
		if (c == null || c.inventory() == null) {
			return;
		}

		for (RewardItem item : rewards) {
			session.context().inventories().addItem(c.inventory(), item.itemId(), item.count(), process);
			session.send(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
					new SystemMessage.Number(item.count()),
					new SystemMessage.ItemName(item.itemId())));
		}
		session.send(ItemList.of(c.inventory().items(), false));
	}

	private void broadcastAnnounce(GameSession session, String message) {
		if (session == null || message == null || message.isBlank()) {
			return;
		}
		var say = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "", message);
		if (session.context() != null && session.context().world() != null) {
			session.context().world().broadcast(say, p -> true);
		} else {
			session.send(say);
		}
	}

	public int getCooldownMinutes() {
		return cooldownMinutes;
	}

	public void setCooldownMinutes(int cooldownMinutes) {
		this.cooldownMinutes = cooldownMinutes;
	}

	public void clearCooldowns() {
		lastKillTimes.clear();
	}
}
