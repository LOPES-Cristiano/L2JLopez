package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

		// Checa limite de enchant se configurado
		if (Config.ALT_OLY_ENCHANT_LIMIT >= 0 && player.inventory() != null) {
			for (var item : player.inventory().items()) {
				if (item != null && item.isEquipped() && item.enchant() > Config.ALT_OLY_ENCHANT_LIMIT) {
					String itemName = item.template() != null ? item.template().name() : "Item";
					log.warn("Olympiad: Jogador {} tentou registrar com item {} enchant +{} (limite: +{})",
							player.name(), itemName, item.enchant(), Config.ALT_OLY_ENCHANT_LIMIT);
					return RegisterResult.ENCHANT_LIMIT_EXCEEDED;
				}
			}
		}

		// Verificacao anti-feed por IP na mesma fila
		if (Config.ALT_OLY_SAME_IP) {
			String clientIp = session.clientIp() != null ? session.clientIp() : "127.0.0.1";
			List<OlympiadParticipant> queue = queues.get(mode);
			synchronized (queue) {
				boolean sameIpFound = queue.stream()
						.anyMatch(p -> clientIp.equals(p.ipAddress()) && !"127.0.0.1".equals(clientIp));
				if (sameIpFound) {
					log.warn("Olympiad anti-feed: Jogador {} bloqueado por mesmo IP {} na fila {}",
							player.name(), clientIp, mode);
					return RegisterResult.SAME_IP_BLOCKED;
				}

				OlympiadParticipant participant = new OlympiadParticipant(session, player.x(), player.y(), player.z());
				queue.add(participant);
			}
		} else {
			List<OlympiadParticipant> queue = queues.get(mode);
			synchronized (queue) {
				OlympiadParticipant participant = new OlympiadParticipant(session, player.x(), player.y(), player.z());
				queue.add(participant);
			}
		}

		log.info("Jogador {} (Classe {}) registrado com sucesso na fila {}", player.name(), player.classId(), mode);
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
			sideB.add(it.next());
			it.remove();
		}

		if (sideA.size() == 3 && sideB.size() == 3) {
			int matchId = matchIdCounter.incrementAndGet();
			OlympiadMatch match = new OlympiadMatch(matchId, stadiumId, OlympiadMode.TEAM_BASED, sideA, sideB);
			activeMatches.put(matchId, match);
			prepareCombatants(match);
			return match;
		}

		// Se nao conseguiu formar, reinjeta
		queue.addAll(sideA);
		queue.addAll(sideB);
		return null;
	}

	/**
	 * Prepara os combatentes: remove buffs externos, restaura CP/HP/MP a 100%.
	 */
	public void prepareCombatants(OlympiadMatch match) {
		List<OlympiadParticipant> all = new ArrayList<>();
		all.addAll(match.teamA());
		all.addAll(match.teamB());

		for (OlympiadParticipant p : all) {
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
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

		log.info("Match {} ({}) concluida com resultado: {}", matchId, match.mode(), result);
		return result;
	}

	public OlympiadMatch getActiveMatch(int matchId) {
		return activeMatches.get(matchId);
	}
}
