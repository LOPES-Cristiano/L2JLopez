package com.lopez.l2j.game.tournament;

import com.lopez.l2j.game.arenaduel.ArenaDefinition;
import com.lopez.l2j.game.arenaduel.ArenaLoc;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico de Torneios Agendados e Automatizados (1x1, 3x3, 5x5).
 * Repatriado do acervo L2JDream V2 (Tournament Manager).
 * Gerencia inscricoes solo e de equipe, filas dedicadas por modalidade,
 * teleporte para arenas de combate isoladas com auras distintas, contagem regressiva,
 * resolucao de vitoria e premiacao organica em Adena.
 */
@Service
public class TournamentService {

	private static final Logger log = LoggerFactory.getLogger(TournamentService.class);
	public static final int REWARD_ADENA_ID = 57;
	public static final int REWARD_ADENA_AMOUNT_1X1 = 1_000_000;
	public static final int REWARD_ADENA_AMOUNT_3X3 = 5_000_000;
	public static final int REWARD_ADENA_AMOUNT_5X5 = 10_000_000;

	public enum TournamentMode {
		SOLO_1X1(1, "1x1"),
		TRIO_3X3(3, "3x3"),
		PARTY_5X5(5, "5x5");

		private final int requiredMembers;
		private final String label;

		TournamentMode(int requiredMembers, String label) {
			this.requiredMembers = requiredMembers;
			this.label = label;
		}

		public int getRequiredMembers() { return requiredMembers; }
		public String getLabel() { return label; }
	}

	public enum TournamentState {
		WAITING,
		COUNTDOWN,
		FIGHTING,
		FINISHED
	}

	public static class TournamentTeam {
		private final int teamId;
		private final String name;
		private final List<PlayerCharacter> members;
		private final Map<Integer, ArenaLoc> originalLocations = new HashMap<>();
		private final Set<Integer> aliveMemberIds = ConcurrentHashMap.newKeySet();

		public TournamentTeam(int teamId, String name, List<PlayerCharacter> members) {
			this.teamId = teamId;
			this.name = name;
			this.members = Collections.unmodifiableList(new ArrayList<>(members));
			for (var m : members) {
				originalLocations.put(m.objectId(), new ArenaLoc(m.x(), m.y(), m.z()));
				aliveMemberIds.add(m.objectId());
			}
		}

		public int teamId() { return teamId; }
		public String name() { return name; }
		public List<PlayerCharacter> members() { return members; }
		public ArenaLoc originalLoc(int playerId) { return originalLocations.get(playerId); }
		public boolean isAlive(int playerId) { return aliveMemberIds.contains(playerId); }
		public void markDead(int playerId) { aliveMemberIds.remove(playerId); }
		public int aliveCount() { return aliveMemberIds.size(); }
		public boolean isEliminated() { return aliveMemberIds.isEmpty(); }
	}

	public static class TournamentMatch {
		private final int matchId;
		private final TournamentMode mode;
		private final ArenaDefinition arena;
		private final TournamentTeam teamRed;
		private final TournamentTeam teamBlue;
		private TournamentState state = TournamentState.COUNTDOWN;
		private TournamentTeam winner = null;

		public TournamentMatch(int matchId, TournamentMode mode, ArenaDefinition arena,
							   TournamentTeam teamRed, TournamentTeam teamBlue) {
			this.matchId = matchId;
			this.mode = mode;
			this.arena = arena;
			this.teamRed = teamRed;
			this.teamBlue = teamBlue;
		}

		public int matchId() { return matchId; }
		public TournamentMode mode() { return mode; }
		public ArenaDefinition arena() { return arena; }
		public TournamentTeam teamRed() { return teamRed; }
		public TournamentTeam teamBlue() { return teamBlue; }
		public TournamentState state() { return state; }
		public void setState(TournamentState state) { this.state = state; }
		public TournamentTeam winner() { return winner; }
		public void setWinner(TournamentTeam winner) { this.winner = winner; }

		public boolean hasPlayer(int playerId) {
			return teamRed.originalLocations.containsKey(playerId) || teamBlue.originalLocations.containsKey(playerId);
		}
	}

	private final Map<TournamentMode, Queue<TournamentTeam>> waitingQueues = new ConcurrentHashMap<>();
	private final Map<Integer, TournamentMatch> activeMatches = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> playerToMatch = new ConcurrentHashMap<>();
	private final Map<Integer, ArenaDefinition> arenas = new ConcurrentHashMap<>();
	private final AtomicInteger matchIdGenerator = new AtomicInteger(1);
	private final AtomicInteger teamIdGenerator = new AtomicInteger(1);

	private final InventoryService inventoryService;

	public TournamentService() {
		this(null);
	}

	@Autowired(required = false)
	public TournamentService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
		for (TournamentMode mode : TournamentMode.values()) {
			waitingQueues.put(mode, new ConcurrentLinkedQueue<>());
		}
		initDefaultArenas();
	}

	private void initDefaultArenas() {
		arenas.put(1, new ArenaDefinition(1, "Colosseum Tournament Red-Blue",
				new ArenaLoc(-88000, 142000, -3600),
				new ArenaLoc(-87000, 142000, -3600),
				new ArenaLoc(-87500, 142500, -3400)));
		arenas.put(2, new ArenaDefinition(2, "Fantasy Isle Arena 1",
				new ArenaLoc(-58700, -56800, -200),
				new ArenaLoc(-57700, -56800, -200),
				new ArenaLoc(-58200, -56800, -200)));
	}

	public synchronized boolean register(TournamentMode mode, List<PlayerCharacter> members) {
		if (members == null || members.isEmpty() || members.size() != mode.getRequiredMembers()) {
			return false;
		}
		for (var m : members) {
			if (m.isDead() || isRegistered(m.objectId()) || isInMatch(m.objectId())) {
				return false;
			}
		}

		int teamId = teamIdGenerator.getAndIncrement();
		String teamName = "Equipe-" + members.get(0).name();
		TournamentTeam team = new TournamentTeam(teamId, teamName, members);
		waitingQueues.get(mode).add(team);
		log.info("Tournament: Equipe {} registrada no modo {}. Fila agora possui {} equipes.",
				teamName, mode.getLabel(), waitingQueues.get(mode).size());

		processQueue(mode);
		return true;
	}

	public synchronized boolean unregister(int playerId) {
		for (var entry : waitingQueues.entrySet()) {
			Queue<TournamentTeam> queue = entry.getValue();
			var it = queue.iterator();
			while (it.hasNext()) {
				TournamentTeam team = it.next();
				if (team.originalLocations.containsKey(playerId)) {
					it.remove();
					log.info("Tournament: Equipe {} desregistrada do modo {}.", team.name(), entry.getKey().getLabel());
					return true;
				}
			}
		}
		return false;
	}

	public boolean isRegistered(int playerId) {
		for (Queue<TournamentTeam> queue : waitingQueues.values()) {
			for (TournamentTeam team : queue) {
				if (team.originalLocations.containsKey(playerId)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isInMatch(int playerId) {
		return playerToMatch.containsKey(playerId);
	}

	public TournamentMatch getPlayerMatch(int playerId) {
		Integer matchId = playerToMatch.get(playerId);
		return matchId != null ? activeMatches.get(matchId) : null;
	}

	public Map<Integer, TournamentMatch> getActiveMatches() {
		return Collections.unmodifiableMap(activeMatches);
	}

	public synchronized void processQueue(TournamentMode mode) {
		Queue<TournamentTeam> queue = waitingQueues.get(mode);
		while (queue.size() >= 2) {
			ArenaDefinition arena = findFreeArena();
			if (arena == null) {
				break;
			}

			TournamentTeam teamRed = queue.poll();
			TournamentTeam teamBlue = queue.poll();
			if (teamRed == null || teamBlue == null) {
				break;
			}

			int matchId = matchIdGenerator.getAndIncrement();
			TournamentMatch match = new TournamentMatch(matchId, mode, arena, teamRed, teamBlue);
			activeMatches.put(matchId, match);

			for (var m : teamRed.members()) {
				playerToMatch.put(m.objectId(), matchId);
				m.currentHp(m.maxHp());
				m.currentMp(m.maxMp());
				m.currentCp(m.maxCp());
				m.teleport(arena.spawn1().x(), arena.spawn1().y(), arena.spawn1().z());
			}

			for (var m : teamBlue.members()) {
				playerToMatch.put(m.objectId(), matchId);
				m.currentHp(m.maxHp());
				m.currentMp(m.maxMp());
				m.currentCp(m.maxCp());
				m.teleport(arena.spawn2().x(), arena.spawn2().y(), arena.spawn2().z());
			}

			log.info("Tournament: Partida #{} iniciada ({}) no {}: {} vs {}",
					matchId, mode.getLabel(), arena.name(), teamRed.name(), teamBlue.name());
		}
	}

	public void startFight(int matchId) {
		TournamentMatch match = activeMatches.get(matchId);
		if (match != null && match.state() == TournamentState.COUNTDOWN) {
			match.setState(TournamentState.FIGHTING);
			log.info("Tournament: Partida #{} comecou!", matchId);
		}
	}

	public synchronized void onPlayerDeath(int playerId) {
		TournamentMatch match = getPlayerMatch(playerId);
		if (match == null || match.state() == TournamentState.FINISHED) {
			return;
		}

		if (match.teamRed().originalLocations.containsKey(playerId)) {
			match.teamRed().markDead(playerId);
			if (match.teamRed().isEliminated()) {
				finishMatch(match.matchId(), match.teamBlue());
			}
		} else if (match.teamBlue().originalLocations.containsKey(playerId)) {
			match.teamBlue().markDead(playerId);
			if (match.teamBlue().isEliminated()) {
				finishMatch(match.matchId(), match.teamRed());
			}
		}
	}

	public synchronized void finishMatch(int matchId, TournamentTeam winner) {
		TournamentMatch match = activeMatches.remove(matchId);
		if (match == null) {
			return;
		}

		match.setState(TournamentState.FINISHED);
		match.setWinner(winner);

		// Distribuir recompensa para os integrantes do time vencedor
		int rewardAdena = switch (match.mode()) {
			case SOLO_1X1 -> REWARD_ADENA_AMOUNT_1X1;
			case TRIO_3X3 -> REWARD_ADENA_AMOUNT_3X3;
			case PARTY_5X5 -> REWARD_ADENA_AMOUNT_5X5;
		};

		if (winner != null && inventoryService != null) {
			for (var member : winner.members()) {
				if (member.inventory() != null) {
					inventoryService.addItem(member.inventory(), REWARD_ADENA_ID, rewardAdena, "TournamentReward");
				}
			}
		}

		// Retornar todos os combatentes aos pontos de origem e curá-los
		returnAndHeal(match.teamRed());
		returnAndHeal(match.teamBlue());

		for (var m : match.teamRed().members()) {
			playerToMatch.remove(m.objectId());
		}
		for (var m : match.teamBlue().members()) {
			playerToMatch.remove(m.objectId());
		}

		log.info("Tournament: Partida #{} finalizada. Vencedor: {}", matchId, winner != null ? winner.name() : "Empate");
	}

	private void returnAndHeal(TournamentTeam team) {
		for (var m : team.members()) {
			m.currentHp(m.maxHp());
			m.currentMp(m.maxMp());
			m.currentCp(m.maxCp());
			var loc = team.originalLoc(m.objectId());
			if (loc != null) {
				m.teleport(loc.x(), loc.y(), loc.z());
			}
		}
	}

	private ArenaDefinition findFreeArena() {
		for (ArenaDefinition arena : arenas.values()) {
			boolean busy = activeMatches.values().stream()
					.anyMatch(m -> m.arena().id() == arena.id() && m.state() != TournamentState.FINISHED);
			if (!busy) {
				return arena;
			}
		}
		return null;
	}

	public String buildTournamentHtml(PlayerCharacter player) {
		int playerId = player.objectId();
		boolean registered = isRegistered(playerId);
		boolean inMatch = isInMatch(playerId);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Gerenciador de Torneios (.tournament) ===</font><br><br>");

		if (inMatch) {
			TournamentMatch m = getPlayerMatch(playerId);
			sb.append("<font color=\"00FF00\">Voce esta em combate na partida #").append(m != null ? m.matchId() : "").append("</font><br>");
		} else if (registered) {
			sb.append("<font color=\"FFFF00\">Voce esta na fila de espera do Torneio!</font><br><br>");
			sb.append("<button value=\"Cancelar Inscricao\" action=\"bypass -h tournament_leave\" width=130 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
		} else {
			sb.append("Escolha a modalidade de torneio desejada:<br><br>");
			sb.append("<table width=240>");
			sb.append("<tr><td><font color=\"00FF00\">Torneio 1x1 (Individual)</font></td>");
			sb.append("<td><button value=\"Inscrever\" action=\"bypass -h tournament_join 1x1\" width=70 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");

			sb.append("<tr><td><font color=\"00FF00\">Torneio 3x3 (Trio)</font></td>");
			sb.append("<td><button value=\"Inscrever\" action=\"bypass -h tournament_join 3x3\" width=70 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");

			sb.append("<tr><td><font color=\"00FF00\">Torneio 5x5 (Party)</font></td>");
			sb.append("<td><button value=\"Inscrever\" action=\"bypass -h tournament_join 5x5\" width=70 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");
			sb.append("</table><br>");
		}

		sb.append("<font color=\"LEVEL\">Partidas Ativas: </font>").append(activeMatches.size()).append("<br>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
