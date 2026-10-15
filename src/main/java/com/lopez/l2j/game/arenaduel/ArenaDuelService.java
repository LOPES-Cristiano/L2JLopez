package com.lopez.l2j.game.arenaduel;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sistema moderno de Arena de Duelo Automatizada 1x1.
 * Gerencia fila de inscricao, matchmaking automatico, teleporte para arenas de combate,
 * contagem regressiva, resolucao de vencedor/perdedor, retorno as posicoes originais
 * e premiacao.
 */
public class ArenaDuelService {

	private static final Logger log = LoggerFactory.getLogger(ArenaDuelService.class);

	private final Map<Integer, ArenaDefinition> arenas = new ConcurrentHashMap<>();
	private final Queue<QueuedPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
	private final Map<Integer, ArenaMatch> activeMatches = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> playerToMatch = new ConcurrentHashMap<>();
	private final AtomicInteger matchIdGenerator = new AtomicInteger(1);

	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;
	private ScheduledExecutorService scheduler;

	public record QueuedPlayer(PlayerCharacter player, ArenaLoc originalLoc) {}
	public record RewardEntry(int itemId, int count) {}

	public ArenaDuelService(ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this.itemRepository = itemRepository;
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x50000000);
		initializeDefaultArenas();
		if (Config.ARENA_DUEL_ENABLE || Config.ARENA_ENABLED) {
			startScheduledArenaTasks();
		}
	}

	public ArenaDuelService() {
		this(null, null, null);
	}

	private void initializeDefaultArenas() {
		// Colosseum 4 Arenas
		arenas.put(1, new ArenaDefinition(1, "Colosseum Arena Alpha",
				new ArenaLoc(-88000, 142000, -3600),
				new ArenaLoc(-87000, 142000, -3600),
				new ArenaLoc(-87500, 142500, -3400)));
		arenas.put(2, new ArenaDefinition(2, "Colosseum Arena Beta",
				new ArenaLoc(-88000, 145000, -3600),
				new ArenaLoc(-87000, 145000, -3600),
				new ArenaLoc(-87500, 145500, -3400)));
		arenas.put(3, new ArenaDefinition(3, "Colosseum Arena Gamma",
				new ArenaLoc(-85000, 142000, -3600),
				new ArenaLoc(-84000, 142000, -3600),
				new ArenaLoc(-84500, 142500, -3400)));
		arenas.put(4, new ArenaDefinition(4, "Colosseum Arena Delta",
				new ArenaLoc(-85000, 145000, -3600),
				new ArenaLoc(-84000, 145000, -3600),
				new ArenaLoc(-84500, 145500, -3400)));

		ArenaLoc configuredLoc = parseConfiguredArenaLoc();
		if (configuredLoc != null) {
			arenas.put(5, new ArenaDefinition(5, "Configured Arena Duel Stadium",
					new ArenaLoc(configuredLoc.x() - 350, configuredLoc.y(), configuredLoc.z()),
					new ArenaLoc(configuredLoc.x() + 350, configuredLoc.y(), configuredLoc.z()),
					new ArenaLoc(configuredLoc.x(), configuredLoc.y() + 450, configuredLoc.z() + 150)));
		}
	}

	public ArenaLoc parseConfiguredArenaLoc() {
		String raw = Config.ARENA_DUEL_LOC;
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String[] parts = raw.trim().split(",");
		if (parts.length >= 3) {
			try {
				return new ArenaLoc(
						Integer.parseInt(parts[0].trim()),
						Integer.parseInt(parts[1].trim()),
						Integer.parseInt(parts[2].trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	public synchronized void startScheduledArenaTasks() {
		if (scheduler != null && !scheduler.isShutdown()) {
			return;
		}
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "ArenaDuel-Scheduler");
			t.setDaemon(true);
			return t;
		});
		int checkSec = getCheckIntervalSeconds();
		scheduler.scheduleAtFixedRate(this::processQueue, checkSec, checkSec, TimeUnit.SECONDS);
		if (Config.ARENA_ENABLED && Config.ARENA_INTERVAL > 0) {
			long intervalMs = Config.ARENA_INTERVAL * 60_000L;
			scheduler.scheduleAtFixedRate(this::processQueue, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
		}
		log.info("ArenaDuelService: agendador iniciado (check={}s, call={}s, wait={}s, arenaInterval={}min)",
				getCheckIntervalSeconds(), getCallIntervalSeconds(), getWaitIntervalSeconds(), getArenaEventIntervalMinutes());
	}

	public synchronized void stopScheduledArenaTasks() {
		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
	}

	public boolean isArenaDuelEnabled() {
		return Config.ARENA_DUEL_ENABLE || Config.ARENA_ENABLED;
	}

	public int getCheckIntervalSeconds() {
		return Math.max(1, Config.ARENA_DUEL_CHECK_INTERVAL);
	}

	public int getCallIntervalSeconds() {
		return Math.max(1, Config.ARENA_DUEL_CALL_INTERVAL);
	}

	public int getWaitIntervalSeconds() {
		return Math.max(1, Config.ARENA_DUEL_WAIT_INTERVAL);
	}

	public int getArenaEventIntervalMinutes() {
		return Math.max(1, Config.ARENA_INTERVAL);
	}

	public Set<Integer> getRestrictedItemIds() {
		Set<Integer> restricted = new HashSet<>();
		List<Integer> raw = Config.ARENA_DUEL_ITEMS_RESTRICTION;
		if (raw != null) {
			for (Integer id : raw) {
				if (id != null && id > 0) {
					restricted.add(id);
				}
			}
		}
		return restricted;
	}

	public boolean hasRestrictedEquipment(PlayerCharacter player) {
		if (player == null || player.inventory() == null || player.inventory().items() == null) {
			return false;
		}
		Set<Integer> restrictedIds = getRestrictedItemIds();
		for (ItemInstance item : player.inventory().items()) {
			if (item == null || !item.isEquipped()) {
				continue;
			}
			if (restrictedIds.contains(item.itemId())) {
				return true;
			}
			if (!Config.ARENA_ALLOW_S && item.template() != null) {
				String crystal = item.template().crystalType();
				if ("s".equalsIgnoreCase(crystal) || "s80".equalsIgnoreCase(crystal)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean register(PlayerCharacter player) {
		if (player == null || player.isDead()) {
			return false;
		}
		if (isRegistered(player.objectId()) || isInMatch(player.objectId())) {
			return false;
		}
		if (hasRestrictedEquipment(player)) {
			log.info("Player {} blocked from Arena Duel due to restricted item or S-grade restriction.", player.name());
			return false;
		}

		ArenaLoc loc = new ArenaLoc(player.x(), player.y(), player.z());
		waitingQueue.add(new QueuedPlayer(player, loc));
		log.info("Player {} (ID: {}) registered for 1x1 Arena Duel. Queue size: {}",
				player.name(), player.objectId(), waitingQueue.size());

		processQueue();
		return true;
	}

	public synchronized boolean unregister(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		boolean removed = waitingQueue.removeIf(qp -> qp.player().objectId() == player.objectId());
		if (removed) {
			log.info("Player {} (ID: {}) unregistered from 1x1 Arena Duel.", player.name(), player.objectId());
		}
		return removed;
	}

	public boolean isRegistered(int playerId) {
		return waitingQueue.stream().anyMatch(qp -> qp.player().objectId() == playerId);
	}

	public boolean isInMatch(int playerId) {
		return playerToMatch.containsKey(playerId);
	}

	public int getQueueSize() {
		return waitingQueue.size();
	}

	public Map<Integer, ArenaMatch> getActiveMatches() {
		return Collections.unmodifiableMap(activeMatches);
	}

	public ArenaMatch getMatch(int matchId) {
		return activeMatches.get(matchId);
	}

	public ArenaMatch getPlayerMatch(int playerId) {
		Integer matchId = playerToMatch.get(playerId);
		return matchId != null ? activeMatches.get(matchId) : null;
	}

	public synchronized void processQueue() {
		while (waitingQueue.size() >= 2) {
			ArenaDefinition freeArena = findFreeArena();
			if (freeArena == null) {
				break; // All arenas occupied
			}

			QueuedPlayer qp1 = waitingQueue.poll();
			QueuedPlayer qp2 = waitingQueue.poll();
			if (qp1 == null || qp2 == null) {
				break;
			}

			// Validate both players are still valid
			if (qp1.player().isDead() || qp2.player().isDead()) {
				if (!qp1.player().isDead()) waitingQueue.add(qp1);
				if (!qp2.player().isDead()) waitingQueue.add(qp2);
				continue;
			}

			int matchId = matchIdGenerator.getAndIncrement();
			ArenaMatch match = new ArenaMatch(matchId, freeArena, qp1.player(), qp2.player(), qp1.originalLoc(), qp2.originalLoc());
			activeMatches.put(matchId, match);
			playerToMatch.put(qp1.player().objectId(), matchId);
			playerToMatch.put(qp2.player().objectId(), matchId);

			// Heal and teleport to arena spawn points
			int duelInstanceId = 9200 + (matchId % 100);
			var world = com.lopez.l2j.game.world.GameWorld.getInstance();

			qp1.player().team(1);
			qp1.player().currentHp(qp1.player().maxHp());
			qp1.player().currentMp(qp1.player().maxMp());
			qp1.player().currentCp(qp1.player().maxCp());
			if (world != null) {
				world.teleportPlayer(qp1.player(), freeArena.spawn1().x(), freeArena.spawn1().y(), freeArena.spawn1().z(), duelInstanceId);
				world.player(qp1.player().objectId()).ifPresent(op -> {
					if (op instanceof com.lopez.l2j.network.game.GameSession gs) gs.sendUserInfoAndBroadcastCharInfo();
				});
			} else {
				qp1.player().teleport(freeArena.spawn1().x(), freeArena.spawn1().y(), freeArena.spawn1().z());
			}

			qp2.player().team(2);
			qp2.player().currentHp(qp2.player().maxHp());
			qp2.player().currentMp(qp2.player().maxMp());
			qp2.player().currentCp(qp2.player().maxCp());
			if (world != null) {
				world.teleportPlayer(qp2.player(), freeArena.spawn2().x(), freeArena.spawn2().y(), freeArena.spawn2().z(), duelInstanceId);
				world.player(qp2.player().objectId()).ifPresent(op -> {
					if (op instanceof com.lopez.l2j.network.game.GameSession gs) gs.sendUserInfoAndBroadcastCharInfo();
				});
			} else {
				qp2.player().teleport(freeArena.spawn2().x(), freeArena.spawn2().y(), freeArena.spawn2().z());
			}

			log.info("Match #{} started in {}: {} vs {}", matchId, freeArena.name(), qp1.player().name(), qp2.player().name());
		}
	}

	public void startFight(int matchId) {
		ArenaMatch match = activeMatches.get(matchId);
		if (match != null && match.getState() == ArenaDuelState.COUNTDOWN) {
			match.setState(ArenaDuelState.FIGHTING);
			log.info("Match #{} is now FIGHTING!", matchId);
		}
	}

	public synchronized void finishMatch(int matchId, int winnerId) {
		ArenaMatch match = activeMatches.get(matchId);
		if (match == null || match.getState() == ArenaDuelState.FINISHED) {
			return;
		}

		match.setState(ArenaDuelState.FINISHED);
		match.setWinnerId(winnerId);

		PlayerCharacter winner = (match.getPlayer1().objectId() == winnerId) ? match.getPlayer1() :
				(match.getPlayer2().objectId() == winnerId ? match.getPlayer2() : null);

		if (winner != null) {
			awardWinner(winner);
			log.info("Match #{} won by {} (ID: {})", matchId, winner.name(), winner.objectId());
		}

		// Restore and return players
		PlayerCharacter p1 = match.getPlayer1();
		PlayerCharacter p2 = match.getPlayer2();
		var world = com.lopez.l2j.game.world.GameWorld.getInstance();

		p1.team(0);
		p1.currentHp(p1.maxHp());
		p1.currentMp(p1.maxMp());
		p1.currentCp(p1.maxCp());
		if (world != null) {
			world.sendPacket(p1.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(p1.objectId()));
			world.teleportPlayer(p1, match.getOriginalLoc1().x(), match.getOriginalLoc1().y(), match.getOriginalLoc1().z(), 0);
			world.player(p1.objectId()).ifPresent(op -> {
				if (op instanceof com.lopez.l2j.network.game.GameSession gs) gs.sendUserInfoAndBroadcastCharInfo();
			});
		} else {
			p1.teleport(match.getOriginalLoc1().x(), match.getOriginalLoc1().y(), match.getOriginalLoc1().z());
		}

		p2.team(0);
		p2.currentHp(p2.maxHp());
		p2.currentMp(p2.maxMp());
		p2.currentCp(p2.maxCp());
		if (world != null) {
			world.sendPacket(p2.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(p2.objectId()));
			world.teleportPlayer(p2, match.getOriginalLoc2().x(), match.getOriginalLoc2().y(), match.getOriginalLoc2().z(), 0);
			world.player(p2.objectId()).ifPresent(op -> {
				if (op instanceof com.lopez.l2j.network.game.GameSession gs) gs.sendUserInfoAndBroadcastCharInfo();
			});
		} else {
			p2.teleport(match.getOriginalLoc2().x(), match.getOriginalLoc2().y(), match.getOriginalLoc2().z());
		}

		// Return spectators
		for (int specId : match.getSpectators()) {
			// Spectator cleanup
		}

		playerToMatch.remove(p1.objectId());
		playerToMatch.remove(p2.objectId());
		activeMatches.remove(matchId);
	}

	public void onPlayerDeath(PlayerCharacter deadPlayer) {
		if (deadPlayer == null) return;
		ArenaMatch match = getPlayerMatch(deadPlayer.objectId());
		if (match != null && match.getState() == ArenaDuelState.FIGHTING) {
			PlayerCharacter opponent = match.getOpponent(deadPlayer.objectId());
			int winnerId = opponent != null ? opponent.objectId() : 0;
			finishMatch(match.getMatchId(), winnerId);
		}
	}

	public void onPlayerDisconnect(int playerId) {
		unregister(null); // safely cleanup queue
		waitingQueue.removeIf(qp -> qp.player().objectId() == playerId);

		ArenaMatch match = getPlayerMatch(playerId);
		if (match != null) {
			PlayerCharacter opponent = match.getOpponent(playerId);
			int winnerId = opponent != null ? opponent.objectId() : 0;
			finishMatch(match.getMatchId(), winnerId);
		}
	}

	public boolean addSpectator(PlayerCharacter spectator, int arenaId) {
		ArenaDefinition arena = arenas.get(arenaId);
		if (spectator == null || arena == null) return false;

		ArenaMatch match = findMatchByArena(arenaId);
		if (match == null) return false;

		match.addSpectator(spectator.objectId());
		int duelInstanceId = 9200 + (match.getMatchId() % 100);
		var world = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (world != null) {
			world.teleportPlayer(spectator, arena.spectatorLoc().x(), arena.spectatorLoc().y(), arena.spectatorLoc().z(), duelInstanceId);
		} else {
			spectator.teleport(arena.spectatorLoc().x(), arena.spectatorLoc().y(), arena.spectatorLoc().z());
		}
		return true;
	}

	public void removeSpectator(PlayerCharacter spectator, ArenaLoc returnLoc) {
		if (spectator == null) return;
		for (ArenaMatch match : activeMatches.values()) {
			match.removeSpectator(spectator.objectId());
		}
		if (returnLoc != null) {
			var world = com.lopez.l2j.game.world.GameWorld.getInstance();
			if (world != null) {
				world.teleportPlayer(spectator, returnLoc.x(), returnLoc.y(), returnLoc.z(), 0);
			} else {
				spectator.teleport(returnLoc.x(), returnLoc.y(), returnLoc.z());
			}
		}
	}

	public Map<Integer, String> getFightsSummary() {
		Map<Integer, String> map = new HashMap<>();
		for (ArenaMatch match : activeMatches.values()) {
			map.put(match.getArena().id(), String.format("%s vs %s (%s)",
					match.getPlayer1().name(), match.getPlayer2().name(), match.getState().name()));
		}
		return map;
	}

	private ArenaDefinition findFreeArena() {
		for (ArenaDefinition arena : arenas.values()) {
			boolean occupied = activeMatches.values().stream()
					.anyMatch(m -> m.getArena().id() == arena.id());
			if (!occupied) {
				return arena;
			}
		}
		return null;
	}

	private ArenaMatch findMatchByArena(int arenaId) {
		return activeMatches.values().stream()
				.filter(m -> m.getArena().id() == arenaId)
				.findFirst()
				.orElse(null);
	}

	public List<RewardEntry> getConfiguredDuelRewards() {
		List<RewardEntry> rewards = new ArrayList<>();
		String raw = Config.ARENA_DUEL_REWARD;
		if (raw != null && !raw.isBlank()) {
			for (String entry : raw.split(";")) {
				String trimmed = entry.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				String[] kv = trimmed.split(",");
				if (kv.length >= 2) {
					try {
						int itemId = Integer.parseInt(kv[0].trim());
						int count = Integer.parseInt(kv[1].trim());
						if (itemId > 0 && count > 0) {
							rewards.add(new RewardEntry(itemId, count));
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		if (Config.ARENA_ENABLED && Config.ARENA_REWARD_ID > 0 && Config.ARENA_REWARD_COUNT > 0) {
			rewards.add(new RewardEntry(Config.ARENA_REWARD_ID, Config.ARENA_REWARD_COUNT));
		}
		return rewards;
	}

	private void awardWinner(PlayerCharacter winner) {
		if (itemRepository != null && itemTable != null) {
			// 1,000,000 Adena (item 57) + 5 Festival Adena (item 6673)
			itemTable.get(57).ifPresent(tmpl -> {
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, winner.objectId(), 1_000_000);
				itemRepository.insert(item, "ArenaDuelReward");
				if (winner.inventory() != null) {
					winner.inventory().add(item);
				}
			});
			itemTable.get(6673).ifPresent(tmpl -> {
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, winner.objectId(), 5);
				itemRepository.insert(item, "ArenaDuelReward");
				if (winner.inventory() != null) {
					winner.inventory().add(item);
				}
			});
			if (Config.ARENA_DUEL_ENABLE || Config.ARENA_ENABLED) {
				for (RewardEntry reward : getConfiguredDuelRewards()) {
					itemTable.get(reward.itemId()).ifPresent(tmpl -> {
						ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, winner.objectId(), reward.count());
						itemRepository.insert(item, "ArenaConfiguredReward");
						if (winner.inventory() != null) {
							winner.inventory().add(item);
						}
					});
				}
			}
		}
	}
}
