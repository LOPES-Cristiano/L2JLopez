package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico central de partidas e filas das Grandes Olimpiadas.
 * Suporta as 3 modalidades retail:
 * 1. CLASS_FREE (1v1 sem classe)
 * 2. CLASS_BASED (1v1 mesma classe)
 * 3. TEAM_BASED (3v3 equipes de nobres)
 * Com verificacao anti-feed por IP, remocao previa de buffs externos e distribuicao retail de pontos.
 */
@Service
public class OlympiadGameService {

	private static final Logger log = LoggerFactory.getLogger(OlympiadGameService.class);

	public enum RegisterResult {
		SUCCESS,
		DISABLED,
		NOT_NOBLE,
		LEVEL_TOO_LOW,
		HAS_KARMA,
		IN_CURSED_WEAPON,
		ALREADY_REGISTERED,
		SAME_IP_BLOCKED,
		INSUFFICIENT_POINTS,
		ENCHANT_LIMIT_EXCEEDED
	}

	private final OlympiadManager olympiadManager;
	private OlyClassDamageManager damageManager;

	private final Map<OlympiadMode, List<OlympiadParticipant>> queues = new ConcurrentHashMap<>();
	private final Map<Integer, OlympiadMatch> activeMatches = new ConcurrentHashMap<>();
	private final AtomicInteger matchIdCounter = new AtomicInteger(1000);

	@Autowired
	public OlympiadGameService(OlympiadManager olympiadManager) {
		this.olympiadManager = olympiadManager;
		for (OlympiadMode mode : OlympiadMode.values()) {
			queues.put(mode, Collections.synchronizedList(new ArrayList<>()));
		}
	}

	@Autowired(required = false)
	public void setDamageManager(OlyClassDamageManager damageManager) {
		this.damageManager = damageManager;
	}

	public OlyClassDamageManager damageManager() {
		return damageManager;
	}

	public Set<Integer> getRestrictedItemIds() {
		Set<Integer> ids = new HashSet<>();
		String raw = Config.ALT_OLY_RESTRICTED_ITEMS;
		if (raw != null && !raw.isBlank()) {
			for (String token : raw.split(",")) {
				String trimmed = token.trim();
				if (!trimmed.isEmpty()) {
					try {
						int id = Integer.parseInt(trimmed);
						if (id > 0) {
							ids.add(id);
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		return ids;
	}

	public Set<Integer> getRestrictedSkillIds() {
		Set<Integer> ids = new HashSet<>();
		String raw = Config.ALT_OLY_RESTRICTED_SKILLS;
		if (raw != null && !raw.isBlank()) {
			for (String token : raw.split(",")) {
				String trimmed = token.trim();
				if (!trimmed.isEmpty()) {
					try {
						int id = Integer.parseInt(trimmed);
						if (id > 0) {
							ids.add(id);
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		return ids;
	}

	public boolean isItemRestrictedInOlympiad(int itemId) {
		return itemId > 0 && getRestrictedItemIds().contains(itemId);
	}

	public boolean isSkillRestrictedInOlympiad(int skillId) {
		return skillId > 0 && getRestrictedSkillIds().contains(skillId);
	}

	public int getConfiguredMinParticipants(OlympiadMode mode) {
		if (mode == OlympiadMode.CLASS_BASED) {
			return Config.ALT_OLY_CLASSED_PARTICIPANTS > 0 ? Config.ALT_OLY_CLASSED_PARTICIPANTS : mode.minQueueSize();
		}
		if (mode == OlympiadMode.CLASS_FREE) {
			return Config.ALT_OLY_NON_CLASSED_PARTICIPANTS > 0 ? Config.ALT_OLY_NON_CLASSED_PARTICIPANTS : mode.minQueueSize();
		}
		return mode != null ? mode.minQueueSize() : 4;
	}

	public boolean hasConfiguredParticipantsForScheduledMatch(OlympiadMode mode) {
		return getQueueCount(mode) >= getConfiguredMinParticipants(mode);
	}

	public long getBattleDurationMs() {
		return Config.ALT_OLY_BATTLE > 0 ? Config.ALT_OLY_BATTLE : 360000L;
	}

	public long getBattleWaitMs() {
		return Config.ALT_OLY_BWAIT > 0 ? Config.ALT_OLY_BWAIT : 600000L;
	}

	public long getInitialWaitMs() {
		return Config.ALT_OLY_IWAIT > 0 ? Config.ALT_OLY_IWAIT : 300000L;
	}

	public int getBattleLoadWaitSeconds() {
		return Config.OLYMPIAD_COUNT_BATTLE_LOAD_WAIT > 0 ? Config.OLYMPIAD_COUNT_BATTLE_LOAD_WAIT : 45;
	}

	public RegisterResult register(GameSession session, OlympiadMode mode) {
		if (!Config.OLYMPIAD_ENABLED) {
			return RegisterResult.DISABLED;
		}

		if (session == null || session.activeChar() == null) {
			return RegisterResult.NOT_NOBLE;
		}

		PlayerCharacter player = session.activeChar();

		if (olympiadManager != null && !olympiadManager.isNoble(player.objectId())) {
			return RegisterResult.NOT_NOBLE;
		}

		if (player.level() < 75) {
			return RegisterResult.LEVEL_TOO_LOW;
		}

		if (player.karma() > 0) {
			return RegisterResult.HAS_KARMA;
		}

		if (isAlreadyRegistered(player.objectId())) {
			return RegisterResult.ALREADY_REGISTERED;
		}

		// Checa pontuacao minima
		if (olympiadManager != null) {
			var nobleOpt = olympiadManager.getNoble(player.objectId());
			if (nobleOpt.isPresent() && nobleOpt.get().points() <= 0) {
				return RegisterResult.INSUFFICIENT_POINTS;
			}
		}

		// Checa limite de enchant e itens restritos se configurado
		Set<Integer> restrictedItems = getRestrictedItemIds();
		if (player.inventory() != null && player.inventory().items() != null) {
			for (var item : player.inventory().items()) {
				if (item == null || !item.isEquipped()) {
					continue;
				}
				if (Config.ALT_OLY_ENCHANT_LIMIT >= 0 && item.enchant() > Config.ALT_OLY_ENCHANT_LIMIT) {
					String itemName = item.template() != null ? item.template().name() : "Item";
					log.warn("Olympiad: Jogador {} tentou registrar com item {} enchant +{} (limite: +{})",
							player.name(), itemName, item.enchant(), Config.ALT_OLY_ENCHANT_LIMIT);
					return RegisterResult.ENCHANT_LIMIT_EXCEEDED;
				}
				if (!restrictedItems.isEmpty() && restrictedItems.contains(item.itemId())) {
					log.warn("Olympiad: Jogador {} possui item restrito equipado (ID {})", player.name(), item.itemId());
				}
			}
		}

		// Verificacao anti-feed por IP e HWID/Account na mesma fila
		String clientIp = session.clientIp() != null ? session.clientIp() : "127.0.0.1";
		String account = player.account() != null ? player.account() : "";
		List<OlympiadParticipant> queue = queues.get(mode);
		synchronized (queue) {
			if (Config.ALT_OLY_SAME_IP) {
				boolean sameIpFound = queue.stream()
						.anyMatch(p -> clientIp.equals(p.ipAddress()) && !"127.0.0.1".equals(clientIp));
				if (sameIpFound) {
					log.warn("Olympiad anti-feed: Jogador {} bloqueado por mesmo IP {} na fila {}",
							player.name(), clientIp, mode);
					return RegisterResult.SAME_IP_BLOCKED;
				}
			}
			if (Config.OLYMPIAD_PROTECTION_HWID && !account.isBlank()) {
				boolean sameHwidFound = queue.stream()
						.anyMatch(p -> account.equalsIgnoreCase(p.account()));
				if (sameHwidFound) {
					log.warn("Olympiad HWID protection: Jogador {} bloqueado por mesma conta/HWID {} na fila {}",
							player.name(), account, mode);
					return RegisterResult.SAME_IP_BLOCKED;
				}
			}

			OlympiadParticipant participant = new OlympiadParticipant(session, player.x(), player.y(), player.z());
			queue.add(participant);
		}

		log.info("Jogador {} (Classe {}) registrado com sucesso na fila {}", player.name(), player.classId(), mode);
		return RegisterResult.SUCCESS;
	}

	/**
	 * Registra uma equipe de 3 jogadores na modalidade TEAM_BASED (3x3),
	 * aplicando validacao individual e anti-feed por IP entre os membros e a fila.
	 */
	public RegisterResult registerTeam(List<GameSession> teamSessions) {
		if (!Config.OLYMPIAD_ENABLED) {
			return RegisterResult.DISABLED;
		}
		if (teamSessions == null || teamSessions.size() != 3) {
			return RegisterResult.NOT_NOBLE;
		}
		List<String> teamIps = new ArrayList<>();
		List<String> teamAccounts = new ArrayList<>();
		for (GameSession s : teamSessions) {
			if (s == null || s.activeChar() == null) {
				return RegisterResult.NOT_NOBLE;
			}
			PlayerCharacter p = s.activeChar();
			if (olympiadManager != null && !olympiadManager.isNoble(p.objectId())) {
				return RegisterResult.NOT_NOBLE;
			}
			if (p.level() < 75) {
				return RegisterResult.LEVEL_TOO_LOW;
			}
			if (p.karma() > 0) {
				return RegisterResult.HAS_KARMA;
			}
			if (isAlreadyRegistered(p.objectId())) {
				return RegisterResult.ALREADY_REGISTERED;
			}
			if (olympiadManager != null) {
				var nobleOpt = olympiadManager.getNoble(p.objectId());
				if (nobleOpt.isPresent() && nobleOpt.get().points() <= 0) {
					return RegisterResult.INSUFFICIENT_POINTS;
				}
			}
			String ip = s.clientIp() != null ? s.clientIp() : "127.0.0.1";
			if (Config.ALT_OLY_SAME_IP && !"127.0.0.1".equals(ip)) {
				if (teamIps.contains(ip)) {
					return RegisterResult.SAME_IP_BLOCKED;
				}
				teamIps.add(ip);
			}
			String acc = p.account() != null ? p.account() : "";
			if (Config.OLYMPIAD_PROTECTION_HWID && !acc.isBlank()) {
				if (teamAccounts.contains(acc.toLowerCase())) {
					return RegisterResult.SAME_IP_BLOCKED;
				}
				teamAccounts.add(acc.toLowerCase());
			}
		}

		List<OlympiadParticipant> queue = queues.get(OlympiadMode.TEAM_BASED);
		synchronized (queue) {
			if (Config.ALT_OLY_SAME_IP) {
				for (String ip : teamIps) {
					boolean sameIpInQueue = queue.stream()
							.anyMatch(p -> ip.equals(p.ipAddress()) && !"127.0.0.1".equals(ip));
					if (sameIpInQueue) {
						return RegisterResult.SAME_IP_BLOCKED;
					}
				}
			}
			if (Config.OLYMPIAD_PROTECTION_HWID) {
				for (String acc : teamAccounts) {
					boolean sameAccInQueue = queue.stream()
							.anyMatch(p -> acc.equalsIgnoreCase(p.account()));
					if (sameAccInQueue) {
						return RegisterResult.SAME_IP_BLOCKED;
					}
				}
			}
			for (GameSession s : teamSessions) {
				PlayerCharacter p = s.activeChar();
				queue.add(new OlympiadParticipant(s, p.x(), p.y(), p.z()));
			}
		}
		return RegisterResult.SUCCESS;
	}

	public boolean unregister(int charId) {
		for (List<OlympiadParticipant> queue : queues.values()) {
			synchronized (queue) {
				boolean removed = queue.removeIf(p -> p.charId() == charId);
				if (removed) {
					log.info("Jogador {} removido da fila de Olympiad", charId);
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAlreadyRegistered(int charId) {
		for (List<OlympiadParticipant> queue : queues.values()) {
			synchronized (queue) {
				if (queue.stream().anyMatch(p -> p.charId() == charId)) {
					return true;
				}
			}
		}
		return activeMatches.values().stream()
				.anyMatch(m -> m.teamA().stream().anyMatch(p -> p.charId() == charId)
						|| m.teamB().stream().anyMatch(p -> p.charId() == charId));
	}

	public int getQueueCount(OlympiadMode mode) {
		List<OlympiadParticipant> queue = queues.get(mode);
		return queue != null ? queue.size() : 0;
	}

	/**
	 * Processa o matchmaking para o modo especificado.
	 * Cria e retorna uma nova partida se os requisitos de fila e anti-feed forem satisfeitos.
	 */
	public OlympiadMatch createMatchIfReady(OlympiadMode mode, int stadiumId) {
		List<OlympiadParticipant> queue = queues.get(mode);
		if (queue == null) {
			return null;
		}

		synchronized (queue) {
			if (queue.size() < mode.minQueueSize()) {
				return null;
			}

			if (mode == OlympiadMode.CLASS_FREE) {
				return formClassFreeMatch(queue, stadiumId);
			} else if (mode == OlympiadMode.CLASS_BASED) {
				return formClassBasedMatch(queue, stadiumId);
			} else if (mode == OlympiadMode.TEAM_BASED) {
				return formTeamBasedMatch(queue, stadiumId);
			}
		}
		return null;
	}

	private OlympiadMatch formClassFreeMatch(List<OlympiadParticipant> queue, int stadiumId) {
		for (int i = 0; i < queue.size(); i++) {
			OlympiadParticipant p1 = queue.get(i);
			for (int j = i + 1; j < queue.size(); j++) {
				OlympiadParticipant p2 = queue.get(j);
				// Anti-feed check: IPs devem ser diferentes (exceto localhost em testes)
				if ("127.0.0.1".equals(p1.ipAddress()) || !p1.ipAddress().equals(p2.ipAddress())) {
					queue.remove(j);
					queue.remove(i);
					int matchId = matchIdCounter.incrementAndGet();
					OlympiadMatch match = new OlympiadMatch(matchId, stadiumId, OlympiadMode.CLASS_FREE,
							List.of(p1), List.of(p2));
					activeMatches.put(matchId, match);
					prepareCombatants(match);
					return match;
				}
			}
		}
		return null;
	}

	private OlympiadMatch formClassBasedMatch(List<OlympiadParticipant> queue, int stadiumId) {
		// Agrupa por classe
		Map<Integer, List<OlympiadParticipant>> byClass = new HashMap<>();
		for (OlympiadParticipant p : queue) {
			byClass.computeIfAbsent(p.classId(), k -> new ArrayList<>()).add(p);
		}

		for (var entry : byClass.entrySet()) {
			List<OlympiadParticipant> candidates = entry.getValue();
			if (candidates.size() >= 2) {
				for (int i = 0; i < candidates.size(); i++) {
					OlympiadParticipant p1 = candidates.get(i);
					for (int j = i + 1; j < candidates.size(); j++) {
						OlympiadParticipant p2 = candidates.get(j);
						if ("127.0.0.1".equals(p1.ipAddress()) || !p1.ipAddress().equals(p2.ipAddress())) {
							queue.remove(p1);
							queue.remove(p2);
							int matchId = matchIdCounter.incrementAndGet();
							OlympiadMatch match = new OlympiadMatch(matchId, stadiumId, OlympiadMode.CLASS_BASED,
									List.of(p1), List.of(p2));
							activeMatches.put(matchId, match);
							prepareCombatants(match);
							return match;
						}
					}
				}
			}
		}
		return null;
	}

	private OlympiadMatch formTeamBasedMatch(List<OlympiadParticipant> queue, int stadiumId) {
		if (queue.size() < 6) {
			return null;
		}

		List<OlympiadParticipant> sideA = new ArrayList<>();
		List<OlympiadParticipant> sideB = new ArrayList<>();

		Iterator<OlympiadParticipant> it = queue.iterator();
		while (it.hasNext() && sideA.size() < 3) {
			sideA.add(it.next());
			it.remove();
		}
		while (it.hasNext() && sideB.size() < 3) {
			OlympiadParticipant candidate = it.next();
			boolean ipConflict = sideA.stream().anyMatch(a ->
					a.ipAddress() != null && !"127.0.0.1".equals(a.ipAddress()) && a.ipAddress().equals(candidate.ipAddress()));
			if (!ipConflict) {
				sideB.add(candidate);
				it.remove();
			}
		}

		if (sideA.size() == 3 && sideB.size() == 3) {
			int matchId = matchIdCounter.incrementAndGet();
			OlympiadMatch match = new OlympiadMatch(matchId, stadiumId, OlympiadMode.TEAM_BASED, sideA, sideB);
			activeMatches.put(matchId, match);
			prepareCombatants(match);
			return match;
		}

		// Se nao conseguiu formar, reinjeta
		queue.addAll(0, sideB);
		queue.addAll(0, sideA);
		return null;
	}

	/**
	 * Prepara os combatentes: ativa modo Olimpiada, remove buffs externos, restaura CP/HP/MP a 100%.
	 */
	public void prepareCombatants(OlympiadMatch match) {
		List<OlympiadParticipant> all = new ArrayList<>();
		all.addAll(match.teamA());
		all.addAll(match.teamB());

		if (Config.ALT_OLY_ANNOUNCE_GAMES) {
			String msg = "Olympiad Stadium #" + match.stadiumId() + " (" + match.mode() + ") match is starting!";
			log.info(msg);
			GameWorld world = GameWorld.getInstance();
			if (world != null) {
				CreatureSay announce = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Olympiad", msg);
				for (var online : world.onlinePlayers()) {
					online.send(announce);
				}
			}
		}

		for (OlympiadParticipant p : all) {
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
				ch.olympiadMode(true);
				// Retail Interlude: strip outside buffs
				ch.effects().clear();
				// Full heal if enabled
				if (Config.ALT_OLY_HEAL_ON_TELEPORT || Config.ALT_OLY_HEAL_ON_FIGHT_START) {
					ch.currentHp(ch.maxHp());
					ch.currentMp(ch.maxMp());
					ch.currentCp(ch.maxCp());
				}
			}
		}
	}

	/**
	 * Finaliza a partida, avalia vencedor, transfere pontos no OlympiadManager
	 * e restaura os participantes as coordenadas originais.
	 */
	public OlympiadMatch.MatchResult concludeMatch(int matchId) {
		OlympiadMatch match = activeMatches.remove(matchId);
		if (match == null) {
			return null;
		}

		OlympiadMatch.MatchResult result = match.evaluateOutcome();

		List<OlympiadParticipant> all = new ArrayList<>();
		all.addAll(match.teamA());
		all.addAll(match.teamB());
		for (OlympiadParticipant p : all) {
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
				ch.olympiadMode(false);
			}
		}

		int rewardItemId = match.battleRewardItemId();
		int rewardItemCount = match.battleRewardItemCount();

		if (olympiadManager != null) {
			if (match.mode() == OlympiadMode.CLASS_FREE || match.mode() == OlympiadMode.CLASS_BASED) {
				if (!match.teamA().isEmpty() && !match.teamB().isEmpty()) {
					int p1Id = match.teamA().get(0).charId();
					int p2Id = match.teamB().get(0).charId();

					if (result == OlympiadMatch.MatchResult.TEAM_A_WIN) {
						olympiadManager.recordMatch(p1Id, p2Id, false);
					} else if (result == OlympiadMatch.MatchResult.TEAM_B_WIN) {
						olympiadManager.recordMatch(p2Id, p1Id, false);
					} else {
						olympiadManager.recordMatch(p1Id, p2Id, true);
					}
				}
			} else if (match.mode() == OlympiadMode.TEAM_BASED) {
				// Equipe 3v3: pontos distribuidos entre capitães/membros
				for (int i = 0; i < Math.min(match.teamA().size(), match.teamB().size()); i++) {
					int pA = match.teamA().get(i).charId();
					int pB = match.teamB().get(i).charId();
					if (result == OlympiadMatch.MatchResult.TEAM_A_WIN) {
						olympiadManager.recordMatch(pA, pB, false);
					} else if (result == OlympiadMatch.MatchResult.TEAM_B_WIN) {
						olympiadManager.recordMatch(pB, pA, false);
					} else {
						olympiadManager.recordMatch(pA, pB, true);
					}
				}
			}
		}

		log.info("Match {} ({}) concluida com resultado: {} (Recompensa configurada: {}x item {})",
				matchId, match.mode(), result, rewardItemCount, rewardItemId);
		return result;
	}

	public OlympiadMatch getActiveMatch(int matchId) {
		return activeMatches.get(matchId);
	}
}
