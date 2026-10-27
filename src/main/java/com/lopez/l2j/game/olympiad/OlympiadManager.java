package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.HeroService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExOlympiadMode;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExOlympiadUserInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador das Grandes Olimpiadas de Lineage II Interlude.
 * Controla os Nobres registrados, pontuacao, historico de lutas,
 * determinacao de Herois e sincronizacao com olympiad_nobles.
 */
@Service
public class OlympiadManager {

	private static final Logger log = LoggerFactory.getLogger(OlympiadManager.class);

	public static final int PERIOD_COMPETITION = 0;
	public static final int PERIOD_VALIDATION = 1;
	public static final int MIN_MATCHES_FOR_HERO = 5;
	public static final int OFFICIAL_MIN_MATCHES_FOR_HERO = 9;

	public static final Set<Integer> HERO_WEAPON_IDS = Set.of(
			6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621
	);
	public static final int HERO_CIRCLET_ID = 6842;
	public static final List<Integer> HERO_SKILL_IDS = List.of(395, 396, 1374, 1375, 1376);

	public static final int STADIUM_X1 = -20814;
	public static final int STADIUM_Y1 = -21189;
	public static final int STADIUM_Z1 = -3030;

	public static final int STADIUM_X2 = -19614;
	public static final int STADIUM_Y2 = -21189;
	public static final int STADIUM_Z2 = -3030;

	private final Map<Integer, OlympiadNoble> nobles = new ConcurrentHashMap<>();
	private final Map<Integer, OlympiadNoble> previousCycleNoblesEom = new ConcurrentHashMap<>();
	private final Map<Integer, OlympiadNoble> activeHeroesByClass = new ConcurrentHashMap<>();
	private final Map<Integer, ActiveOlympiadMatch> activeMatches = new ConcurrentHashMap<>();
	private final Map<Integer, int[]> spectatorOriginalCoords = new ConcurrentHashMap<>();
	private final Map<OlympiadMode, List<PlayerCharacter>> modeQueues = new ConcurrentHashMap<>();
	private final Map<Integer, String> registeredIps = new ConcurrentHashMap<>();
	private final AtomicInteger stadiumInstanceCounter = new AtomicInteger(9300);
	private final Map<Integer, ScheduledFuture<?>> arenaTimers = new ConcurrentHashMap<>();
	private final ScheduledExecutorService olympiadScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Olympiad-Manager-Scheduler");
		t.setDaemon(true);
		return t;
	});
	private final JdbcClient jdbc;
	private OlyClassDamageManager olyClassDamageManager;
	private HeroService heroService;

	public record ActiveOlympiadMatch(
			int instanceId,
			PlayerCharacter playerOne,
			PlayerCharacter playerTwo,
			int[] origCoordsOne,
			int[] origCoordsTwo
	) {
		public String playerOneName() {
			return playerOne != null ? playerOne.name() : "";
		}

		public String playerTwoName() {
			return playerTwo != null ? playerTwo.name() : "";
		}
	}

	public Map<Integer, ActiveOlympiadMatch> getActiveMatches() {
		return Collections.unmodifiableMap(activeMatches);
	}

	private final Map<Integer, String> heroMessages = new ConcurrentHashMap<>();

	public void setHeroMessage(int charId, String msg) {
		if (msg != null) {
			heroMessages.put(charId, msg);
		}
	}

	public String getHeroMessage(int charId) {
		return heroMessages.getOrDefault(charId, "");
	}

	private int currentCycle = 1;
	private int period = PERIOD_COMPETITION;

	@Autowired
	public OlympiadManager(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
		for (OlympiadMode mode : OlympiadMode.values()) {
			modeQueues.put(mode, Collections.synchronizedList(new ArrayList<>()));
		}
	}

	@Autowired(required = false)
	public void setOlyClassDamageManager(OlyClassDamageManager olyClassDamageManager) {
		this.olyClassDamageManager = olyClassDamageManager;
	}

	@Autowired(required = false)
	public void setHeroService(HeroService heroService) {
		this.heroService = heroService;
	}

	public OlyClassDamageManager olyClassDamageManager() {
		return olyClassDamageManager;
	}

	public double getArenaDamageMultiplier(PlayerCharacter attacker, PlayerCharacter attacked) {
		if (olyClassDamageManager != null) {
			return olyClassDamageManager.getDamageMultiplier(attacker, attacked);
		}
		if (attacker == null || attacked == null || !attacker.isOlympiadMode() || !attacked.isOlympiadMode()) {
			return 1.0;
		}
		return 1.0;
	}

	public int getCompetitionStartHour() {
		return Math.max(0, Math.min(23, Config.ALT_OLY_START_TIME));
	}

	public int getCompetitionStartMinute() {
		return Math.max(0, Math.min(59, Config.ALT_OLY_MIN));
	}

	public long getCompetitionPeriodMs() {
		return Config.ALT_OLY_CPERIOD > 0 ? Config.ALT_OLY_CPERIOD : 21600000L;
	}

	public long getBattleDurationMs() {
		return Config.ALT_OLY_BATTLE > 0 ? Config.ALT_OLY_BATTLE : 360000L;
	}

	public long getBattleWaitMs() {
		return 600_000L;
	}

	public long getInitialWaitMs() {
		return 300_000L;
	}

	public long getWeeklyPeriodMs() {
		return Config.ALT_OLY_WPERIOD > 0 ? Config.ALT_OLY_WPERIOD : 604800000L;
	}

	public long getValidationPeriodMs() {
		return Config.ALT_OLY_VPERIOD > 0 ? Config.ALT_OLY_VPERIOD : 86400000L;
	}

	public long getCycleDurationMs() {
		String type = Config.OLYMPIAD_DURATION_TYPE != null ? Config.OLYMPIAD_DURATION_TYPE.trim().toUpperCase() : "MONTH";
		int mult = Math.max(1, Config.OLYMPIAD_DURATION);
		long baseMs = switch (type) {
			case "DAY" -> 86_400_000L;
			case "WEEK" -> 604_800_000L;
			default -> 2_592_000_000L; // 30 days
		};
		return baseMs * mult;
	}

	public boolean isWithinDailyCompetitionWindow(int hourOfDay, int minuteOfHour) {
		long startMsFromMidnight = (getCompetitionStartHour() * 3600_000L) + (getCompetitionStartMinute() * 60_000L);
		long currentMsFromMidnight = (hourOfDay * 3600_000L) + (minuteOfHour * 60_000L);
		long endMsFromMidnight = startMsFromMidnight + getCompetitionPeriodMs();
		if (endMsFromMidnight <= 86_400_000L) {
			return currentMsFromMidnight >= startMsFromMidnight && currentMsFromMidnight <= endMsFromMidnight;
		}
		return currentMsFromMidnight >= startMsFromMidnight || currentMsFromMidnight <= (endMsFromMidnight - 86_400_000L);
	}

	public int getRequiredParticipants(OlympiadMode mode) {
		if (mode == OlympiadMode.CLASS_BASED) {
			return Config.ALT_OLY_CLASSED_PARTICIPANTS > 0 ? Config.ALT_OLY_CLASSED_PARTICIPANTS : 5;
		}
		return Config.ALT_OLY_NON_CLASSED_PARTICIPANTS > 0 ? Config.ALT_OLY_NON_CLASSED_PARTICIPANTS : 9;
	}

	public int getBattleRewardItemId() {
		return Config.ALT_OLY_REWARD_ITEM > 0 ? Config.ALT_OLY_REWARD_ITEM : 6651;
	}

	public int getBattleRewardCount(OlympiadMode mode) {
		return mode == OlympiadMode.CLASS_BASED
				? Math.max(0, Config.ALT_OLY_CLASSED_REW_ITEM_COUNT)
				: Math.max(0, Config.ALT_OLY_NON_CLASSED_REW_ITEM_COUNT);
	}

	public int getCompetitionExchangeRewardItemId() {
		return Config.ALT_OLY_REWARD_ITEM > 0 ? Config.ALT_OLY_REWARD_ITEM : 6651;
	}

	public int getBattleLoadWaitSeconds() {
		return 45;
	}

	public boolean isHwidProtectionEnabled() {
		return Config.ALT_OLY_SAME_IP;
	}

	public Set<Integer> getRestrictedItems() {
		Set<Integer> ids = new HashSet<>();
		if (Config.ALT_OLY_RESTRICTED_ITEMS != null) {
			for (Integer id : Config.ALT_OLY_RESTRICTED_ITEMS) {
				if (id != null && id > 0) ids.add(id);
			}
		}
		return ids;
	}

	public Set<Integer> getRestrictedSkills() {
		Set<Integer> ids = new HashSet<>();
		if (Config.ALT_OLY_RESTRICTED_SKILLS != null) {
			for (Integer id : Config.ALT_OLY_RESTRICTED_SKILLS) {
				if (id != null && id > 0) ids.add(id);
			}
		}
		return ids;
	}

	public List<OlympiadNoble> getTopNoblesStatistic() {
		int limit = 100;
		return nobles.values().stream()
				.sorted(Comparator.<OlympiadNoble>comparingInt(OlympiadNoble::points).reversed())
				.limit(limit)
				.toList();
	}

	@PostConstruct
	public void init() {
		loadFromDb();
		olympiadScheduler.scheduleAtFixedRate(this::runScheduledMatchmaking, 30, 30, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void shutdown() {
		olympiadScheduler.shutdownNow();
	}

	private void runScheduledMatchmaking() {
		if (!Config.OLYMPIAD_ENABLED || period != PERIOD_COMPETITION) {
			return;
		}
		for (OlympiadMode mode : OlympiadMode.values()) {
			if (getQueueSize(mode) >= getRequiredParticipants(mode)) {
				processMatchmaking(mode);
			}
		}
	}

	public int currentCycle() {
		return currentCycle;
	}

	public void currentCycle(int currentCycle) {
		this.currentCycle = currentCycle;
	}

	public int period() {
		return period;
	}

	public void period(int period) {
		this.period = period;
	}

	public boolean isNoble(int charId) {
		return nobles.containsKey(charId);
	}

	public Optional<OlympiadNoble> getNoble(int charId) {
		return Optional.ofNullable(nobles.get(charId));
	}

	public Collection<OlympiadNoble> allNobles() {
		return Collections.unmodifiableCollection(nobles.values());
	}

	public synchronized OlympiadNoble registerNoble(int charId, String charName, int classId) {
		int startPts = Config.ALT_OLY_START_POINTS_COUNT > 0 ? Config.ALT_OLY_START_POINTS_COUNT : OlympiadNoble.DEFAULT_POINTS;
		OlympiadNoble noble = nobles.computeIfAbsent(charId, id -> new OlympiadNoble(id, charName, classId, startPts, 0, 0, 0, 0));
		if (charName != null && !charName.isBlank()) {
			noble.charName(charName);
		}
		persistNoble(noble);
		return noble;
	}

	/**
	 * Registra um jogador diretamente na fila de matchmaking da modalidade (CLASS_FREE, CLASS_BASED, TEAM_BASED)
	 * com protecao Anti-Feed por IP e HWID.
	 */
	public synchronized boolean registerForQueue(PlayerCharacter player, OlympiadMode mode, String clientIp) {
		if (player == null || mode == null || !Config.OLYMPIAD_ENABLED) {
			return false;
		}
		if (!isNoble(player.objectId())) {
			registerNoble(player.objectId(), player.name(), player.classId());
		}
		if (isRegisteredInQueue(player.objectId())) {
			return false;
		}
		String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
		String account = player.account() != null ? player.account() : "";
		List<PlayerCharacter> queue = modeQueues.get(mode);
		if (Config.ALT_OLY_SAME_IP && !"127.0.0.1".equals(ip)) {
			for (PlayerCharacter existing : queue) {
				String existingIp = registeredIps.getOrDefault(existing.objectId(), "127.0.0.1");
				if (ip.equals(existingIp)) {
					log.warn("OlympiadManager Anti-Feed: Jogador {} bloqueado por mesmo IP {} no modo {}", player.name(), ip, mode);
					return false;
				}
			}
		}
		if (Config.ALT_OLY_SAME_IP && !account.isBlank()) {
			for (PlayerCharacter existing : queue) {
				if (account.equalsIgnoreCase(existing.account())) {
					log.warn("OlympiadManager HWID Anti-Feed: Jogador {} bloqueado por mesma conta/HWID {} no modo {}", player.name(), account, mode);
					return false;
				}
			}
		}
		registeredIps.put(player.objectId(), ip);
		queue.add(player);
		return true;
	}

	public synchronized boolean unregisterFromQueue(int charId) {
		registeredIps.remove(charId);
		for (List<PlayerCharacter> q : modeQueues.values()) {
			if (q.removeIf(p -> p.objectId() == charId)) {
				return true;
			}
		}
		return false;
	}

	public boolean isRegisteredInQueue(int charId) {
		for (List<PlayerCharacter> q : modeQueues.values()) {
			synchronized (q) {
				if (q.stream().anyMatch(p -> p.objectId() == charId)) {
					return true;
				}
			}
		}
		return false;
	}

	public int getQueueSize(OlympiadMode mode) {
		List<PlayerCharacter> q = modeQueues.get(mode);
		return q != null ? q.size() : 0;
	}

	/**
	 * Executa matchmaking automatico para a modalidade solicitada caso haja participantes suficientes.
	 * Retorna o instanceId da arena criada ou -1 se nao houver par elegivel.
	 */
	public synchronized int processMatchmaking(OlympiadMode mode) {
		List<PlayerCharacter> queue = modeQueues.get(mode);
		if (queue == null || queue.size() < 2) {
			return -1;
		}
		for (int i = 0; i < queue.size(); i++) {
			PlayerCharacter p1 = queue.get(i);
			String ip1 = registeredIps.getOrDefault(p1.objectId(), "127.0.0.1");
			for (int j = i + 1; j < queue.size(); j++) {
				PlayerCharacter p2 = queue.get(j);
				String ip2 = registeredIps.getOrDefault(p2.objectId(), "127.0.0.1");
				if (mode == OlympiadMode.CLASS_BASED && p1.classId() != p2.classId()) {
					continue;
				}
				if (Config.ALT_OLY_SAME_IP && !"127.0.0.1".equals(ip1) && ip1.equals(ip2)) {
					continue;
				}
				if (Config.ALT_OLY_SAME_IP && p1.account() != null && !p1.account().isBlank()
						&& p1.account().equalsIgnoreCase(p2.account())) {
					continue;
				}
				queue.remove(j);
				queue.remove(i);
				registeredIps.remove(p1.objectId());
				registeredIps.remove(p2.objectId());
				return startArenaMatch(p1, p2);
			}
		}
		return -1;
	}

	/**
	 * Inicia uma partida das Grandes Olimpiadas em um estadio instanciado,
	 * teleportando ambos os nobres, limpando buffs externos, curando HP/MP/CP,
	 * ativando modo Olimpiada e enviando ExOlympiadMode / ExOlympiadUserInfo.
	 */
	public int startArenaMatch(PlayerCharacter p1, PlayerCharacter p2) {
		if (p1 == null || p2 == null) {
			return -1;
		}
		registerNoble(p1.objectId(), p1.name(), p1.classId());
		registerNoble(p2.objectId(), p2.name(), p2.classId());

		int instId = stadiumInstanceCounter.incrementAndGet();
		int[] orig1 = new int[]{p1.x(), p1.y(), p1.z()};
		int[] orig2 = new int[]{p2.x(), p2.y(), p2.z()};

		// Strip buffs externos e ativa modo Olimpiada para isolamento de balanceamento (OlyClassDamageManager)
		p1.effects().clear();
		p2.effects().clear();
		p1.olympiadMode(true);
		p2.olympiadMode(true);

		p1.team(1);
		p2.team(2);
		p1.currentHp(p1.maxHp());
		p1.currentMp(p1.maxMp());
		p1.currentCp(p1.maxCp());
		p2.currentHp(p2.maxHp());
		p2.currentMp(p2.maxMp());
		p2.currentCp(p2.maxCp());

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.teleportPlayer(p1, STADIUM_X1, STADIUM_Y1, STADIUM_Z1, instId);
			world.teleportPlayer(p2, STADIUM_X2, STADIUM_Y2, STADIUM_Z2, instId);

			ExOlympiadUserInfo ui1 = new ExOlympiadUserInfo(1, p1.objectId(), p1.name(), p1.classId(),
					(int) p1.currentHp(), (int) p1.maxHp(), (int) p1.currentCp(), (int) p1.maxCp());
			ExOlympiadUserInfo ui2 = new ExOlympiadUserInfo(2, p2.objectId(), p2.name(), p2.classId(),
					(int) p2.currentHp(), (int) p2.maxHp(), (int) p2.currentCp(), (int) p2.maxCp());

			world.player(p1.objectId()).ifPresent(op -> {
				op.send(new ExOlympiadMode(1));
				op.send(ui1);
				op.send(ui2);
				op.send(new CreatureSay(0, CreatureSay.HERO_VOICE, "Olympiad",
						"Welcome to the Grand Olympiad Stadium! The match against " + p2.name() + " begins!"));
			});
			world.player(p2.objectId()).ifPresent(op -> {
				op.send(new ExOlympiadMode(2));
				op.send(ui1);
				op.send(ui2);
				op.send(new CreatureSay(0, CreatureSay.HERO_VOICE, "Olympiad",
						"Welcome to the Grand Olympiad Stadium! The match against " + p1.name() + " begins!"));
			});
			if (Config.ALY_OLY_LOG_FIGHTS) {
				CreatureSay announce = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Olympiad",
						"Olympiad Arena #" + instId + ": " + p1.name() + " vs " + p2.name() + " is starting!");
				for (var online : world.onlinePlayers()) {
					online.send(announce);
				}
			}
		} else {
			p1.instanceId(instId);
			p1.x(STADIUM_X1); p1.y(STADIUM_Y1); p1.z(STADIUM_Z1);
			p2.instanceId(instId);
			p2.x(STADIUM_X2); p2.y(STADIUM_Y2); p2.z(STADIUM_Z2);
		}

		activeMatches.put(instId, new ActiveOlympiadMatch(instId, p1, p2, orig1, orig2));
		long durationMs = getBattleDurationMs();
		if (durationMs > 0) {
			ScheduledFuture<?> timer = olympiadScheduler.schedule(() -> {
				ActiveOlympiadMatch active = activeMatches.get(instId);
				if (active != null && active.playerOne() != null && active.playerTwo() != null) {
					double hp1 = active.playerOne().currentHp();
					double hp2 = active.playerTwo().currentHp();
					if (Math.abs(hp1 - hp2) < 1e-3) {
						finishArenaMatch(instId, active.playerOne().objectId(), active.playerTwo().objectId(), true);
					} else if (hp1 > hp2) {
						finishArenaMatch(instId, active.playerOne().objectId(), active.playerTwo().objectId(), false);
					} else {
						finishArenaMatch(instId, active.playerTwo().objectId(), active.playerOne().objectId(), false);
					}
				}
			}, durationMs, TimeUnit.MILLISECONDS);
			arenaTimers.put(instId, timer);
		}
		return instId;
	}

	/**
	 * Finaliza uma partida instanciada das Olimpiadas, computando os pontos e retornando os jogadores.
	 */
	public void finishArenaMatch(int instanceId, int winnerId, int loserId, boolean draw) {
		ScheduledFuture<?> timer = arenaTimers.remove(instanceId);
		if (timer != null) {
			timer.cancel(false);
		}
		recordMatch(winnerId, loserId, draw);
		ActiveOlympiadMatch match = activeMatches.remove(instanceId);
		if (match == null) {
			return;
		}
		GameWorld world = GameWorld.getInstance();
		restoreParticipant(match.playerOne(), match.origCoordsOne(), world);
		restoreParticipant(match.playerTwo(), match.origCoordsTwo(), world);
	}

	private void restoreParticipant(PlayerCharacter pc, int[] orig, GameWorld world) {
		if (pc == null) {
			return;
		}
		pc.team(0);
		pc.olympiadMode(false);
		pc.currentHp(pc.maxHp());
		pc.currentMp(pc.maxMp());
		pc.currentCp(pc.maxCp());
		if (world != null) {
			world.player(pc.objectId()).ifPresent(op -> {
				op.send(new ExOlympiadMode(0));
				op.send(new StatusUpdate(pc.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) pc.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) pc.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) pc.currentCp())
				)));
			});
			world.teleportPlayer(pc, orig[0], orig[1], orig[2], 0);
		} else {
			pc.instanceId(0);
			pc.x(orig[0]); pc.y(orig[1]); pc.z(orig[2]);
		}
	}

	public synchronized void recordMatch(int winnerId, int loserId, boolean draw) {
		recordMatch(winnerId, loserId, draw, OlympiadMode.CLASS_FREE);
	}

	/**
	 * Registra o resultado de uma luta das Olimpiadas aplicando o divisor de pontos por modalidade (#420)
	 * (1/5 para CLASS_FREE/TEAM_BASED, 1/3 para CLASS_BASED) e persistindo em olympiad_fights (#505).
	 */
	public synchronized void recordMatch(int winnerId, int loserId, boolean draw, OlympiadMode mode) {
		OlympiadNoble winner = nobles.get(winnerId);
		OlympiadNoble loser = nobles.get(loserId);
		if (winner == null || loser == null) {
			return;
		}

		int pointTransfer = 0;
		if (draw) {
			winner.recordDraw();
			loser.recordDraw();
			if (Config.OLYMPIAD_REMOVE_POINTS_ON_TIE) {
				int tiePenalty = 2;
				winner.points(Math.max(0, winner.points() - tiePenalty));
				loser.points(Math.max(0, loser.points() - tiePenalty));
			}
		} else {
			int divisor = (mode == OlympiadMode.CLASS_BASED) ? 3 : 5;
			pointTransfer = Math.max(1, Math.min(10, Math.min(winner.points(), loser.points()) / divisor));
			winner.recordWin(pointTransfer);
			loser.recordLoss(pointTransfer);
		}

		persistNoble(winner);
		persistNoble(loser);
		persistFightHistory(winnerId, loserId, mode != null ? mode.ordinal() : 0, draw ? 0 : 1, pointTransfer);

		log.info("Olympiad Match gravada: Winner={}, Loser={}, Draw={}, Mode={}, Points={}",
				winner.charName(), loser.charName(), draw, mode, pointTransfer);
	}

	private void persistFightHistory(int charOneId, int charTwoId, int classed, int winner, int pointTransfer) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed)
					VALUES (:c1, :c2, :cls1, :cls2, :win, :st, :tm, :cl)
					""")
					.param("c1", charOneId)
					.param("c2", charTwoId)
					.param("cls1", nobles.containsKey(charOneId) ? nobles.get(charOneId).classId() : 0)
					.param("cls2", nobles.containsKey(charTwoId) ? nobles.get(charTwoId).classId() : 0)
					.param("win", winner)
					.param("st", System.currentTimeMillis())
					.param("tm", pointTransfer)
					.param("cl", classed)
					.update();
		} catch (Exception ex) {
			log.debug("Aviso ao gravar olympiad_fights: {}", ex.getMessage());
		}
	}

	/**
	 * Computa os novos Herois da temporada por classId.
	 * Retorna mapa de classId -> Nobre com maior pontuacao (minimo de partidas e ao menos 1 vitoria - #423).
	 */
	public Map<Integer, OlympiadNoble> computeHeroes() {
		Map<Integer, OlympiadNoble> heroes = new HashMap<>();
		int minMatches = Config.ALT_OLY_MIN_MATCHES > 0 ? Config.ALT_OLY_MIN_MATCHES : MIN_MATCHES_FOR_HERO;

		for (OlympiadNoble noble : nobles.values()) {
			if (noble.competitionsDone() < minMatches || noble.competitionsWon() < 1) {
				continue;
			}
			OlympiadNoble existing = heroes.get(noble.classId());
			if (existing == null || noble.points() > existing.points()) {
				heroes.put(noble.classId(), noble);
			}
		}

		if (Config.ALT_OLY_SHOW_MONTHLY_WINNERS && !heroes.isEmpty()) {
			log.info("Olympiad Monthly Winners ({} heroes computed): {}", heroes.size(),
					heroes.values().stream().map(OlympiadNoble::charName).toList());
		}

		activeHeroesByClass.clear();
		activeHeroesByClass.putAll(heroes);
		persistHeroes(heroes);
		return Collections.unmodifiableMap(heroes);
	}

	private void persistHeroes(Map<Integer, OlympiadNoble> heroes) {
		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE heroes SET played = 0").update();
			} catch (Exception ignored) {
			}
		}
		for (OlympiadNoble h : heroes.values()) {
			if (jdbc != null) {
				try {
					int prevCount = 0;
					try {
						var rows = jdbc.sql("SELECT count FROM heroes WHERE charId = :charId")
								.param("charId", h.charId())
								.query().listOfRows();
						if (!rows.isEmpty() && rows.get(0).get("count") instanceof Number num) {
							prevCount = num.intValue();
						}
					} catch (Exception ignored) {
					}
					jdbc.sql("""
							REPLACE INTO heroes (charId, char_name, class_id, count, played)
							VALUES (:charId, :charName, :classId, :cnt, 1)
							""")
							.param("charId", h.charId())
							.param("charName", h.charName() != null ? h.charName() : "")
							.param("classId", h.classId())
							.param("cnt", prevCount + 1)
							.update();
				} catch (Exception ex) {
					log.debug("Aviso ao persistir heroi em heroes: {}", ex.getMessage());
				}
			}
			if (heroService != null) {
				try {
					heroService.setHero(h.charId(), 30);
				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * Encerra o ciclo mensal das Olimpiadas, copiando os pontos finais para olympiad_nobles_eom (#425, #505)
	 * e elegendo os novos Herois de cada classe.
	 */
	public synchronized Map<Integer, OlympiadNoble> finishCycleAndArchiveEom() {
		previousCycleNoblesEom.clear();
		for (OlympiadNoble n : nobles.values()) {
			OlympiadNoble copy = new OlympiadNoble(n.charId(), n.charName(), n.classId(),
					n.points(), n.competitionsDone(), n.competitionsWon(), n.competitionsLost(), n.competitionsDrawn());
			previousCycleNoblesEom.put(n.charId(), copy);
			if (jdbc != null) {
				try {
					jdbc.sql("""
							REPLACE INTO olympiad_nobles_eom (charId, class_id, olympiad_points, competitions_done,
							                                  competitions_won, competitions_lost, competitions_drawn)
							VALUES (:charId, :class_id, :olympiad_points, :competitions_done,
							        :competitions_won, :competitions_lost, :competitions_drawn)
							""")
							.param("charId", copy.charId())
							.param("class_id", copy.classId())
							.param("olympiad_points", copy.points())
							.param("competitions_done", copy.competitionsDone())
							.param("competitions_won", copy.competitionsWon())
							.param("competitions_lost", copy.competitionsLost())
							.param("competitions_drawn", copy.competitionsDrawn())
							.update();
				} catch (Exception ex) {
					log.debug("Aviso ao arquivar olympiad_nobles_eom: {}", ex.getMessage());
				}
			}
		}
		Map<Integer, OlympiadNoble> elected = computeHeroes();
		currentCycle++;
		return elected;
	}

	/**
	 * Troca pontos acumulados no ciclo anterior (olympiad_nobles_eom) por Noblesse Gate Passes (6651) (#425).
	 */
	public synchronized int exchangePreviousCyclePoints(int charId) {
		OlympiadNoble eom = previousCycleNoblesEom.get(charId);
		if (eom == null) {
			return 0;
		}
		int minMatches = Config.ALT_OLY_MIN_MATCHES > 0 ? Config.ALT_OLY_MIN_MATCHES : MIN_MATCHES_FOR_HERO;
		if (eom.competitionsDone() < minMatches || eom.points() < Config.ALT_OLY_MIN_POINT_FOR_EXCHANGE) {
			return 0;
		}
		int pts = eom.points();
		eom.points(0);
		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE olympiad_nobles_eom SET olympiad_points = 0 WHERE charId = :charId")
						.param("charId", charId)
						.update();
			} catch (Exception ex) {
				log.debug("Aviso ao atualizar olympiad_nobles_eom: {}", ex.getMessage());
			}
		}
		return pts * Config.ALT_OLY_GP_PER_POINT;
	}

	/**
	 * Concede as 5 habilidades exclusivas de Heroi (395, 396, 1374, 1375, 1376) apenas na classe principal (Base Class) (#424, #426).
	 */
	public void applyHeroStatusAndSkills(PlayerCharacter player, boolean isHero) {
		if (player == null) {
			return;
		}
		player.hero(isHero);
		boolean onMainClass = !player.isSubClassActive() || player.baseClassId() == 0;
		if (isHero && onMainClass) {
			for (int skillId : HERO_SKILL_IDS) {
				player.skills().put(skillId, 1);
			}
		} else {
			for (int skillId : HERO_SKILL_IDS) {
				player.skills().remove(skillId);
			}
			if (!isHero && player.inventory() != null) {
				for (var item : new ArrayList<>(player.inventory().items())) {
					if (HERO_WEAPON_IDS.contains(item.itemId()) || item.itemId() == HERO_CIRCLET_ID) {
						if (item.isEquipped()) {
							player.inventory().unequip(item);
						}
						player.inventory().remove(item);
					}
				}
			}
		}
	}

	/**
	 * Ingresso em Modo Espectador de uma arena das Olimpiadas (#422).
	 */
	public boolean enterSpectatorMode(PlayerCharacter spectator, int stadiumInstanceId) {
		if (spectator == null || spectator.isOlympiadMode() || isRegisteredInQueue(spectator.objectId())) {
			return false;
		}
		spectatorOriginalCoords.put(spectator.objectId(), new int[]{spectator.x(), spectator.y(), spectator.z()});
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.teleportPlayer(spectator, STADIUM_X1 + 600, STADIUM_Y1, STADIUM_Z1, stadiumInstanceId);
			world.player(spectator.objectId()).ifPresent(s -> s.send(new ExOlympiadMode(3)));
		} else {
			spectator.instanceId(stadiumInstanceId);
			spectator.x(STADIUM_X1 + 600);
			spectator.y(STADIUM_Y1);
			spectator.z(STADIUM_Z1);
		}
		return true;
	}

	/**
	 * Saida do Modo Espectador das Olimpiadas, restaurando posicao e visibilidade (#422).
	 */
	public boolean leaveSpectatorMode(PlayerCharacter spectator) {
		if (spectator == null) {
			return false;
		}
		int[] orig = spectatorOriginalCoords.remove(spectator.objectId());
		if (orig == null) {
			return false;
		}
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.player(spectator.objectId()).ifPresent(s -> s.send(new ExOlympiadMode(0)));
			world.teleportPlayer(spectator, orig[0], orig[1], orig[2], 0);
		} else {
			spectator.instanceId(0);
			spectator.x(orig[0]);
			spectator.y(orig[1]);
			spectator.z(orig[2]);
		}
		return true;
	}

	public void resetWeeklyPoints() {
		int addPoints = Config.ALT_OLY_WEEKLY_POINTS_COUNT > 0 ? Config.ALT_OLY_WEEKLY_POINTS_COUNT : 3;
		for (OlympiadNoble noble : nobles.values()) {
			noble.points(noble.points() + addPoints);
			persistNoble(noble);
		}
		log.info("Olympiad: +{} pontos semanais concedidos aos nobres", addPoints);
	}

	public synchronized int exchangePoints(int charId, int pointsToExchange) {
		OlympiadNoble noble = nobles.get(charId);
		if (noble == null) {
			return 0;
		}
		if (noble.points() < Config.ALT_OLY_MIN_POINT_FOR_EXCHANGE || pointsToExchange <= 0 || pointsToExchange > noble.points()) {
			return 0;
		}
		noble.points(noble.points() - pointsToExchange);
		persistNoble(noble);
		return pointsToExchange * Config.ALT_OLY_GP_PER_POINT;
	}

	public synchronized void rewardHeroPoints(int charId) {
		OlympiadNoble noble = nobles.get(charId);
		if (noble != null) {
			noble.points(noble.points() + Config.ALT_OLY_HERO_POINTS);
			persistNoble(noble);
		}
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		Map<Integer, String> charNames = new HashMap<>();
		try {
			var nameRows = jdbc.sql("SELECT obj_Id, char_name FROM characters").query().listOfRows();
			for (var r : nameRows) {
				Object idObj = r.get("obj_Id");
				Object nameObj = r.get("char_name");
				if (idObj instanceof Number n && nameObj != null) {
					charNames.put(n.intValue(), nameObj.toString());
				}
			}
		} catch (Exception ignored) {
		}

		try {
			var rows = jdbc.sql("""
					SELECT charId, class_id, olympiad_points, competitions_done,
					       competitions_won, competitions_lost, competitions_drawn
					FROM olympiad_nobles
					""").query().listOfRows();

			for (var row : rows) {
				int charId = ((Number) row.get("charId")).intValue();
				int classId = ((Number) row.get("class_id")).intValue();
				int points = ((Number) row.get("olympiad_points")).intValue();
				int done = ((Number) row.get("competitions_done")).intValue();
				int won = ((Number) row.get("competitions_won")).intValue();
				int lost = ((Number) row.get("competitions_lost")).intValue();
				int drawn = ((Number) row.get("competitions_drawn")).intValue();
				String charName = charNames.getOrDefault(charId, "");

				nobles.put(charId, new OlympiadNoble(charId, charName, classId, points, done, won, lost, drawn));
			}
			log.info("OlympiadManager carregou {} nobres do banco", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar olympiad_nobles do banco: {}", ex.getMessage());
		}

		try {
			var eomRows = jdbc.sql("""
					SELECT charId, class_id, olympiad_points, competitions_done,
					       competitions_won, competitions_lost, competitions_drawn
					FROM olympiad_nobles_eom
					""").query().listOfRows();
			for (var row : eomRows) {
				int charId = ((Number) row.get("charId")).intValue();
				int classId = ((Number) row.get("class_id")).intValue();
				int points = ((Number) row.get("olympiad_points")).intValue();
				int done = ((Number) row.get("competitions_done")).intValue();
				int won = ((Number) row.get("competitions_won")).intValue();
				int lost = ((Number) row.get("competitions_lost")).intValue();
				int drawn = ((Number) row.get("competitions_drawn")).intValue();
				String charName = charNames.getOrDefault(charId, "");

				previousCycleNoblesEom.put(charId, new OlympiadNoble(charId, charName, classId, points, done, won, lost, drawn));
			}
		} catch (Exception ignored) {
		}

		try {
			var heroRows = jdbc.sql("""
					SELECT charId, char_name, class_id, count, played
					FROM heroes
					WHERE played = 1
					""").query().listOfRows();
			for (var row : heroRows) {
				int charId = ((Number) row.get("charId")).intValue();
				int classId = ((Number) row.get("class_id")).intValue();
				String charName = row.get("char_name") != null ? row.get("char_name").toString() : charNames.getOrDefault(charId, "");
				OlympiadNoble noble = nobles.getOrDefault(charId,
						new OlympiadNoble(charId, charName, classId, OlympiadNoble.DEFAULT_POINTS, 0, 0, 0, 0));
				if (noble.charName() == null || noble.charName().isBlank()) {
					noble.charName(charName);
				}
				activeHeroesByClass.put(classId, noble);
			}
		} catch (Exception ignored) {
		}
	}

	private void persistNoble(OlympiadNoble noble) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO olympiad_nobles (charId, class_id, olympiad_points, competitions_done,
					                            competitions_won, competitions_lost, competitions_drawn)
					VALUES (:charId, :class_id, :olympiad_points, :competitions_done,
					        :competitions_won, :competitions_lost, :competitions_drawn)
					ON DUPLICATE KEY UPDATE
						class_id = :class_id,
						olympiad_points = :olympiad_points,
						competitions_done = :competitions_done,
						competitions_won = :competitions_won,
						competitions_lost = :competitions_lost,
						competitions_drawn = :competitions_drawn
					""")
					.param("charId", noble.charId())
					.param("class_id", noble.classId())
					.param("olympiad_points", noble.points())
					.param("competitions_done", noble.competitionsDone())
					.param("competitions_won", noble.competitionsWon())
					.param("competitions_lost", noble.competitionsLost())
					.param("competitions_drawn", noble.competitionsDrawn())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir nobre olympiad {}", noble.charId(), ex);
		}
	}

	public synchronized void save() {
		nobles.values().forEach(this::persistNoble);
		log.info("OlympiadManager: todos os {} nobres foram salvos.", nobles.size());
	}

	public synchronized Map<Integer, OlympiadNoble> electHeroes() {
		return computeHeroes();
	}
}
