package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico Unificado de Eventos PvP com Rotacao de Arenas e Isolamento por Instancia.
 * Coordena os 3 eventos retail/custom de destaque:
 * 1. TVT (Team vs Team - Mata-Mata em Equipes)
 * 2. CTF (Capture The Flag - Captura da Bandeira Inimiga)
 * 3. DEATHMATCH (Todos contra Todos)
 *
 * Politica estrita: Zero Coins de Doacao (Sem 9300).
 * Recompensas concedidas exclusivamente em Adena Oficial (57) e Festival Adena (6673).
 */
@Service
public class PvPEventService {

	private static final Logger log = LoggerFactory.getLogger(PvPEventService.class);

	// Itens Retail Oficiais
	public static final int REWARD_ADENA_ID = 57;
	public static final int REWARD_FESTIVAL_ADENA_ID = 6673;

	public enum EventType {
		TVT("Team vs Team"),
		CTF("Capture the Flag"),
		DEATHMATCH("Deathmatch");

		private final String displayName;

		EventType(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
	}

	public enum EventState {
		IDLE,
		REGISTRATION,
		STARTING,
		RUNNING,
		FINISHED
	}

	public enum Team {
		BLUE("Blue Team", 0x0000FF),
		RED("Red Team", 0xFF0000),
		NONE("Individual", 0xFFFFFF);

		private final String teamName;
		private final int color;

		Team(String teamName, int color) {
			this.teamName = teamName;
			this.color = color;
		}

		public String teamName() {
			return teamName;
		}

		public int color() {
			return color;
		}
	}

	public record ArenaLocation(int x, int y, int z) {}

	public enum Arena {
		COLISEUM("Coliseum Arena",
				new ArenaLocation(149457, 46700, -3413),
				new ArenaLocation(147457, 46700, -3413)),
		GLUDIO("Gludio Combat Arena",
				new ArenaLocation(-14228, 123963, -3118),
				new ArenaLocation(-15000, 123963, -3118)),
		GIRAN("Giran Town Arena",
				new ArenaLocation(73815, 142645, -3778),
				new ArenaLocation(72815, 142645, -3778)),
		FANTASY_ISLE("Fantasy Isle Arena",
				new ArenaLocation(-58752, -56898, -2033),
				new ArenaLocation(-57752, -56898, -2033));

		private final String arenaName;
		private final ArenaLocation teamASpawn;
		private final ArenaLocation teamBSpawn;

		Arena(String arenaName, ArenaLocation teamASpawn, ArenaLocation teamBSpawn) {
			this.arenaName = arenaName;
			this.teamASpawn = teamASpawn;
			this.teamBSpawn = teamBSpawn;
		}

		public String arenaName() {
			return arenaName;
		}

		public ArenaLocation teamASpawn() {
			return teamASpawn;
		}

		public ArenaLocation teamBSpawn() {
			return teamBSpawn;
		}
	}

	public static class Participant {
		private final int charId;
		private final String name;
		private final int classId;
		private final String ipAddress;
		private final GameSession session;

		private Team team = Team.NONE;
		private int kills = 0;
		private int flagsCaptured = 0;
		private int origX;
		private int origY;
		private int origZ;
		private int origInstanceId;

		public Participant(GameSession session) {
			this.session = session;
			var ch = session != null ? session.activeChar() : null;
			this.charId = ch != null ? ch.objectId() : 0;
			this.name = ch != null ? ch.name() : "Player";
			this.classId = ch != null ? ch.classId() : 0;
			this.ipAddress = session != null && session.clientIp() != null ? session.clientIp() : "127.0.0.1";
			if (ch != null) {
				this.origX = ch.x();
				this.origY = ch.y();
				this.origZ = ch.z();
				this.origInstanceId = ch.instanceId();
			}
		}

		public int charId() { return charId; }
		public String name() { return name; }
		public int classId() { return classId; }
		public String ipAddress() { return ipAddress; }
		public GameSession session() { return session; }
		public Team team() { return team; }
		public void team(Team team) { this.team = team; }
		public int kills() { return kills; }
		public void addKill() { this.kills++; }
		public int flagsCaptured() { return flagsCaptured; }
		public void addFlagCapture() { this.flagsCaptured++; }
		public int origX() { return origX; }
		public int origY() { return origY; }
		public int origZ() { return origZ; }
		public int origInstanceId() { return origInstanceId; }
	}

	private final InventoryService inventoryService;
	private final AtomicInteger instanceSequence = new AtomicInteger(5000);

	private EventState state = EventState.IDLE;
	private EventType currentType = EventType.TVT;
	private Arena currentArena = Arena.COLISEUM;
	private int activeInstanceId = 0;

	private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
	private final List<Arena> arenaRotation = List.of(Arena.COLISEUM, Arena.GLUDIO, Arena.GIRAN, Arena.FANTASY_ISLE);
	private int arenaIndex = 0;

	// Placar de Equipes
	private int blueScore = 0;
	private int redScore = 0;

	@Autowired
	public PvPEventService(@Autowired(required = false) InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public EventState state() {
		return state;
	}

	public EventType currentType() {
		return currentType;
	}

	public Arena currentArena() {
		return currentArena;
	}

	public int activeInstanceId() {
		return activeInstanceId;
	}

	public int blueScore() {
		return blueScore;
	}

	public int redScore() {
		return redScore;
	}

	public int getParticipantCount() {
		return participants.size();
	}

	public Map<Integer, Participant> getParticipants() {
		return Collections.unmodifiableMap(participants);
	}

	/**
	 * Abre o periodo de inscricoes com selecao automatica do proximo tipo de evento e arena rotacionada.
	 */
	public synchronized boolean openRegistration(EventType type) {
		if (state != EventState.IDLE) {
			log.warn("Nao e possivel abrir inscricoes: evento em estado {}", state);
			return false;
		}

		this.currentType = type != null ? type : EventType.TVT;
		this.currentArena = arenaRotation.get(arenaIndex % arenaRotation.size());
		this.arenaIndex++;
		this.participants.clear();
		this.blueScore = 0;
		this.redScore = 0;
		this.state = EventState.REGISTRATION;

		log.info("Evento PvP [{}] aberto para inscricoes na arena [{}]",
				currentType.displayName(), currentArena.arenaName());
		return true;
	}

	public synchronized boolean register(GameSession session) {
		if (state != EventState.REGISTRATION) {
			return false;
		}
		if (session == null || session.activeChar() == null) {
			return false;
		}

		PlayerCharacter player = session.activeChar();
		if (player.level() < 70) {
			log.info("Jogador {} recusado: nivel {} < 70", player.name(), player.level());
			return false;
		}
		if (player.karma() > 0) {
			log.info("Jogador {} recusado por possuir karma PK ({})", player.name(), player.karma());
			return false;
		}
		if (participants.containsKey(player.objectId())) {
			return false;
		}

		// Anti-feed: checa se outro participante ja tem o mesmo IP (exceto localhost)
		String ip = session.clientIp() != null ? session.clientIp() : "127.0.0.1";
		if (!"127.0.0.1".equals(ip)) {
			boolean ipExists = participants.values().stream().anyMatch(p -> ip.equals(p.ipAddress()));
			if (ipExists) {
				log.warn("Anti-feed: jogador {} recusado por IP duplicado {}", player.name(), ip);
				return false;
			}
		}

		participants.put(player.objectId(), new Participant(session));
		log.info("Jogador {} registrado no evento {}", player.name(), currentType.displayName());
		return true;
	}

	public synchronized boolean unregister(int charId) {
		if (state != EventState.REGISTRATION) {
			return false;
		}
		return participants.remove(charId) != null;
	}

	/**
	 * Inicia o evento: cria instancia isolada, distribui equipes, limpa buffs e teleporta.
	 */
	public synchronized boolean startEvent() {
		if (state != EventState.REGISTRATION) {
			return false;
		}
		if (participants.size() < 2) {
			log.warn("Participantes insuficientes ({}) para iniciar evento", participants.size());
			cancelEvent();
			return false;
		}

		this.state = EventState.STARTING;
		this.activeInstanceId = instanceSequence.incrementAndGet();

		// Divide equipes se for TVT ou CTF
		List<Participant> list = new ArrayList<>(participants.values());
		if (currentType == EventType.TVT || currentType == EventType.CTF) {
			for (int i = 0; i < list.size(); i++) {
				Team team = (i % 2 == 0) ? Team.BLUE : Team.RED;
				list.get(i).team(team);
			}
		} else {
			list.forEach(p -> p.team(Team.NONE));
		}

		// Teleporta para arena na instancia dedicada e limpa buffs
		for (Participant p : list) {
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
				ch.instanceId(activeInstanceId);
				ch.effects().clear();
				ch.currentHp(ch.maxHp());
				ch.currentMp(ch.maxMp());
				ch.currentCp(ch.maxCp());

				ArenaLocation loc = (p.team() == Team.RED) ? currentArena.teamBSpawn() : currentArena.teamASpawn();
				ch.x(loc.x());
				ch.y(loc.y());
				ch.z(loc.z());
			}
		}

		this.state = EventState.RUNNING;
		log.info("Evento PvP [{}] INICIADO com {} participantes na Instancia #{}",
				currentType.displayName(), participants.size(), activeInstanceId);
		return true;
	}

	public synchronized void onKill(int killerId, int victimId) {
		if (state != EventState.RUNNING) {
			return;
		}

		Participant killer = participants.get(killerId);
		Participant victim = participants.get(victimId);
		if (killer == null || victim == null) {
			return;
		}

		killer.addKill();

		if (currentType == EventType.TVT) {
			if (killer.team() == Team.BLUE) {
				blueScore++;
			} else if (killer.team() == Team.RED) {
				redScore++;
			}
		}

		log.info("PvPEvent: {} abateu {} (Score: Blue={}, Red={})",
				killer.name(), victim.name(), blueScore, redScore);
	}

	public synchronized void onFlagCapture(int carrierId) {
		if (state != EventState.RUNNING || currentType != EventType.CTF) {
			return;
		}

		Participant carrier = participants.get(carrierId);
		if (carrier == null) {
			return;
		}

		carrier.addFlagCapture();
		if (carrier.team() == Team.BLUE) {
			blueScore += 5;
		} else if (carrier.team() == Team.RED) {
			redScore += 5;
		}

		log.info("PvPEvent CTF: Bandeira capturada por {} para a equipe {}! (Score: Blue={}, Red={})",
				carrier.name(), carrier.team().teamName(), blueScore, redScore);
	}

	/**
	 * Conclui o evento, premia os vencedores exclusivamente com itens retail
	 * (Adena 57 e Festival Adena 6673) e teleporta os jogadores de volta ao overworld.
	 */
	public synchronized Team finishEvent() {
		if (state != EventState.RUNNING) {
			return Team.NONE;
		}

		this.state = EventState.FINISHED;
		Team winningTeam = Team.NONE;

		if (currentType == EventType.TVT || currentType == EventType.CTF) {
			if (blueScore > redScore) {
				winningTeam = Team.BLUE;
			} else if (redScore > blueScore) {
				winningTeam = Team.RED;
			}
		}

		// Distribuicao de Recompensas Retail (Zero doacao!)
		for (Participant p : participants.values()) {
			boolean isWinner = false;
			if (currentType == EventType.DEATHMATCH) {
				int maxKills = participants.values().stream().mapToInt(Participant::kills).max().orElse(0);
				isWinner = p.kills() == maxKills && maxKills > 0;
			} else {
				isWinner = p.team() == winningTeam && winningTeam != Team.NONE;
			}

			if (isWinner && p.session() != null && p.session().activeChar() != null) {
				giveRetailReward(p.session().activeChar());
			}

			// Restaura coordenadas e instancia original
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
				ch.instanceId(p.origInstanceId());
				ch.x(p.origX());
				ch.y(p.origY());
				ch.z(p.origZ());
				ch.currentHp(ch.maxHp());
				ch.currentCp(ch.maxCp());
			}
		}

		log.info("Evento PvP [{}] FINALIZADO! Vencedor: {}", currentType.displayName(), winningTeam);
		this.state = EventState.IDLE;
		return winningTeam;
	}

	private void giveRetailReward(PlayerCharacter player) {
		int adenaAmount = 5_000_000;
		int festivalAdenaAmount = 10;

		Inventory inv = player.inventory();
		if (inv != null) {
			if (inventoryService != null) {
				inventoryService.addItem(inv, REWARD_ADENA_ID, adenaAmount, "PvPEventReward");
				inventoryService.addItem(inv, REWARD_FESTIVAL_ADENA_ID, festivalAdenaAmount, "PvPEventReward");
			} else {
				// Adiciona diretamente se rodando em testes sem banco
				inv.addAdena(adenaAmount);
			}
			log.info("Recompensa retail entregue ao vencedor {}: {} Adena e {} Festival Adena",
					player.name(), adenaAmount, festivalAdenaAmount);
		}
	}

	public synchronized void cancelEvent() {
		for (Participant p : participants.values()) {
			if (p.session() != null && p.session().activeChar() != null) {
				PlayerCharacter ch = p.session().activeChar();
				ch.instanceId(p.origInstanceId());
				ch.x(p.origX());
				ch.y(p.origY());
				ch.z(p.origZ());
			}
		}
		participants.clear();
		this.state = EventState.IDLE;
		this.activeInstanceId = 0;
		log.info("Evento PvP cancelado e participantes restaurados");
	}

	/**
	 * Avanca a rotacao para o proximo tipo de evento (TVT -> CTF -> DM).
	 */
	public EventType nextEventType() {
		return switch (currentType) {
			case TVT -> EventType.CTF;
			case CTF -> EventType.DEATHMATCH;
			case DEATHMATCH -> EventType.TVT;
		};
	}
}
