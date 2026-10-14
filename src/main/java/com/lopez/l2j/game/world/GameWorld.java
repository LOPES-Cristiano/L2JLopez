package com.lopez.l2j.game.world;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Gerencia os jogadores e NPCs no mundo, com indexacao espacial em grade 2D para consultas O(1) de proximidade.
 */
@Component
public class GameWorld {

	private static volatile GameWorld instance;

	public GameWorld() {
		instance = this;
	}

	public static GameWorld getInstance() {
		return instance;
	}

	public static void setInstance(GameWorld world) {
		instance = world;
	}

	/** Visao minima de um jogador online, implementada pela sessao. */
	public interface OnlinePlayer {
		int objectId();

		String name();

		int x();

		int y();

		int z();

		void send(GameServerPacket packet);
		
		default void sendMessage(String text) {
			if (text != null && !text.isBlank()) {
				send(GameServerPacket.SystemMessage.sendString(text));
			}
		}

		default void close(GameServerPacket packet) {
		}

		default String account() {
			return null;
		}

		default GameServerPacket charInfo() {
			return null;
		}

		default com.lopez.l2j.game.model.PlayerCharacter character() {
			return null;
		}

		default void onAttacked(int attackerObjectId) {
			onAttacked(attackerObjectId, 0);
		}

		default void onAttacked(int attackerObjectId, int damage) {
		}

		default void onDeath() {
		}

		default void onDeath(int killerObjectId) {
			onDeath();
		}

		default boolean isTeleporting() {
			return false;
		}

		default void sendSkillList() {
		}

		default void refreshWeightAndPenalties() {
		}

		default void sendUserInfoAndBroadcastCharInfo() {
		}

		default void addKnownObject(int objectId) {
		}

		default void removeKnownObject(int objectId) {
		}

		default int instanceId() {
			PlayerCharacter ch = character();
			return ch != null ? ch.instanceId() : 0;
		}

		default void teleportToLocation(int x, int y, int z, int instanceId) {
			PlayerCharacter ch = character();
			if (ch != null) {
				ch.instanceId(instanceId);
				ch.x(x);
				ch.y(y);
				ch.z(z);
			}
			send(new GameServerPacket.TeleportToLocation(objectId(), x, y, z, 0));
		}

		default void teleportToLocation(int x, int y, int z) {
			teleportToLocation(x, y, z, instanceId());
		}
	}

	public static final int LOCAL_CHAT_RANGE = 1250;
	public static final int VISIBILITY_RADIUS = 3500;
	public static final int GRID_SIZE = 4096;

	private final Map<Integer, OnlinePlayer> playersById = new ConcurrentHashMap<>();
	private final Map<String, OnlinePlayer> playersByName = new ConcurrentHashMap<>();

	private final Map<Integer, NpcInstance> npcsById = new ConcurrentHashMap<>();
	private final Map<Long, Set<NpcInstance>> npcsByCell = new ConcurrentHashMap<>();

	private final Map<Integer, com.lopez.l2j.game.summon.SummonInstance> summonsById = new ConcurrentHashMap<>();

	// ---- PLAYERS ----

	public void add(OnlinePlayer p) {
		playersById.put(p.objectId(), p);
		if (p.name() != null) {
			playersByName.put(key(p.name()), p);
		}
	}

	public void remove(OnlinePlayer p) {
		playersById.remove(p.objectId(), p);
		if (p.name() != null) {
			playersByName.remove(key(p.name()), p);
		}
	}

	public void removeById(int objectId) {
		OnlinePlayer p = playersById.remove(objectId);
		if (p != null && p.name() != null) {
			playersByName.remove(key(p.name()), p);
		}
	}

	public record PhantomOnlinePlayer(PlayerCharacter character) implements OnlinePlayer {
		@Override public int objectId() { return character.objectId(); }
		@Override public String name() { return character.name(); }
		@Override public int x() { return character.x(); }
		@Override public int y() { return character.y(); }
		@Override public int z() { return character.z(); }
		@Override public void send(GameServerPacket packet) {}
	}

	public int online() {
		return playersById.size();
	}

	public int onlineCount() {
		return online();
	}

	public Collection<OnlinePlayer> players() {
		return playersById.values();
	}

	public Collection<OnlinePlayer> allPlayers() {
		return players();
	}

	public Optional<OnlinePlayer> byName(String name) {
		return name == null ? Optional.empty() : Optional.ofNullable(playersByName.get(key(name)));
	}

	public Optional<OnlinePlayer> player(int objectId) {
		return Optional.ofNullable(playersById.get(objectId));
	}

	public PlayerCharacter getPlayer(int objectId) {
		OnlinePlayer p = playersById.get(objectId);
		return p != null ? p.character() : null;
	}

	public PlayerCharacter getPlayer(String name) {
		if (name == null) {
			return null;
		}
		OnlinePlayer p = playersByName.get(key(name));
		return p != null ? p.character() : null;
	}

	public void teleportPlayer(PlayerCharacter player, int x, int y, int z) {
		teleportPlayer(player, x, y, z, player != null ? player.instanceId() : 0);
	}

	public void teleportPlayer(PlayerCharacter player, int x, int y, int z, int instanceId) {
		if (player == null) {
			return;
		}
		player.instanceId(instanceId);
		player.x(x);
		player.y(y);
		player.z(z);
		OnlinePlayer op = playersById.get(player.objectId());
		if (op != null) {
			op.teleportToLocation(x, y, z, instanceId);
		}
	}

	public void teleportPlayer(int playerObjectId, int x, int y, int z, int instanceId) {
		OnlinePlayer op = playersById.get(playerObjectId);
		if (op != null) {
			op.teleportToLocation(x, y, z, instanceId);
		}
	}

	public void sendPacket(int playerObjectId, GameServerPacket packet) {
		if (packet == null) {
			return;
		}
		OnlinePlayer op = playersById.get(playerObjectId);
		if (op != null) {
			op.send(packet);
		}
	}

	public void sendMessage(int playerObjectId, String message) {
		OnlinePlayer op = playersById.get(playerObjectId);
		if (op != null) {
			op.sendMessage(message);
		}
	}

	public List<OnlinePlayer> findPlayersAround(int x, int y, int range) {
		return findPlayersAround(x, y, range, 0);
	}

	public List<OnlinePlayer> findPlayersAround(int x, int y, int range, int instanceId) {
		long r2 = (long) range * range;
		List<OnlinePlayer> list = new ArrayList<>();
		for (OnlinePlayer p : playersById.values()) {
			if (p.instanceId() != instanceId) {
				continue;
			}
			long dx = p.x() - x;
			long dy = p.y() - y;
			if (dx * dx + dy * dy <= r2) {
				list.add(p);
			}
		}
		return list;
	}

	public void broadcast(GameServerPacket packet) {
		broadcast(packet, p -> true);
	}

	public void broadcast(GameServerPacket packet, Predicate<OnlinePlayer> filter) {
		for (OnlinePlayer p : playersById.values()) {
			if (filter.test(p)) {
				p.send(packet);
			}
		}
	}

	public void broadcastAround(OnlinePlayer origin, int range, GameServerPacket packet) {
		broadcastAround(origin, range, packet, true);
	}

	public void broadcastAround(OnlinePlayer origin, int range, GameServerPacket packet, boolean includeSelf) {
		long r2 = (long) range * range;
		int origInstance = origin.instanceId();
		broadcast(packet, p -> {
			if (!includeSelf && p.objectId() == origin.objectId()) {
				return false;
			}
			if (p.instanceId() != origInstance) {
				return false;
			}
			long dx = p.x() - origin.x();
			long dy = p.y() - origin.y();
			return dx * dx + dy * dy <= r2;
		});
	}

	public void broadcastAround(int x, int y, int range, GameServerPacket packet) {
		broadcastAround(x, y, range, 0, packet);
	}

	public void broadcastAround(int x, int y, int range, int instanceId, GameServerPacket packet) {
		long r2 = (long) range * range;
		broadcast(packet, p -> {
			if (p.instanceId() != instanceId) {
				return false;
			}
			long dx = p.x() - x;
			long dy = p.y() - y;
			return dx * dx + dy * dy <= r2;
		});
	}

	// ---- SUMMONS / PETS ----

	public void addSummon(com.lopez.l2j.game.summon.SummonInstance summon) {
		if (summon != null) {
			summonsById.put(summon.objectId(), summon);
		}
	}

	public void removeSummon(com.lopez.l2j.game.summon.SummonInstance summon) {
		if (summon != null) {
			summonsById.remove(summon.objectId());
		}
	}

	public void removeSummon(int objectId) {
		summonsById.remove(objectId);
	}

	public void removeSummonById(int objectId) {
		summonsById.remove(objectId);
	}

	public Optional<com.lopez.l2j.game.summon.SummonInstance> summon(int objectId) {
		return Optional.ofNullable(summonsById.get(objectId));
	}

	public Collection<com.lopez.l2j.game.summon.SummonInstance> summons() {
		return summonsById.values();
	}

	public List<com.lopez.l2j.game.summon.SummonInstance> findSummonsAround(int x, int y, int range, int instanceId) {
		long r2 = (long) range * range;
		List<com.lopez.l2j.game.summon.SummonInstance> list = new ArrayList<>();
		for (var s : summonsById.values()) {
			if (s.instanceId() != instanceId) {
				continue;
			}
			long dx = s.x() - x;
			long dy = s.y() - y;
			if (dx * dx + dy * dy <= r2) {
				list.add(s);
			}
		}
		return list;
	}

	// ---- NPCS E SPATIAL GRID ----

	public void addNpc(NpcInstance npc) {
		npcsById.put(npc.objectId(), npc);
		long key = cellKey(npc.x(), npc.y());
		npcsByCell.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(npc);
	}

	public void removeNpc(NpcInstance npc) {
		npcsById.remove(npc.objectId());
		long key = cellKey(npc.x(), npc.y());
		Set<NpcInstance> cell = npcsByCell.get(key);
		if (cell != null) {
			cell.remove(npc);
			if (cell.isEmpty()) {
				npcsByCell.remove(key, Collections.emptySet());
			}
		}
	}

	public void removeNpcsByInstance(int instanceId) {
		if (instanceId <= 0) {
			return;
		}
		List<NpcInstance> toRemove = new ArrayList<>();
		for (NpcInstance npc : npcsById.values()) {
			if (npc.instanceId() == instanceId) {
				toRemove.add(npc);
			}
		}
		for (NpcInstance npc : toRemove) {
			removeNpc(npc);
		}
	}

	public void updateNpcPosition(NpcInstance npc, int oldX, int oldY) {
		long oldKey = cellKey(oldX, oldY);
		long newKey = cellKey(npc.x(), npc.y());
		if (oldKey != newKey) {
			Set<NpcInstance> oldCell = npcsByCell.get(oldKey);
			if (oldCell != null) {
				oldCell.remove(npc);
				if (oldCell.isEmpty()) {
					npcsByCell.remove(oldKey, Collections.emptySet());
				}
			}
			npcsByCell.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet()).add(npc);
		}
	}

	public Optional<NpcInstance> npc(int objectId) {
		return Optional.ofNullable(npcsById.get(objectId));
	}

	public Collection<NpcInstance> npcs() {
		return npcsById.values();
	}

	public int totalNpcs() {
		return npcsById.size();
	}

	public List<NpcInstance> findNpcsAround(int x, int y, int range) {
		return findNpcsAround(x, y, range, 0);
	}

	public List<NpcInstance> findNpcsAround(int x, int y, int range, int instanceId) {
		long r2 = (long) range * range;
		int minCellX = (x - range) >> 12;
		int maxCellX = (x + range) >> 12;
		int minCellY = (y - range) >> 12;
		int maxCellY = (y + range) >> 12;

		List<NpcInstance> list = new ArrayList<>();
		for (int cx = minCellX; cx <= maxCellX; cx++) {
			for (int cy = minCellY; cy <= maxCellY; cy++) {
				long key = (((long) cx) << 32) | (cy & 0xFFFFFFFFL);
				Set<NpcInstance> cell = npcsByCell.get(key);
				if (cell != null) {
					for (NpcInstance npc : cell) {
						if (npc.instanceId() != instanceId) {
							continue;
						}
						long dx = npc.x() - x;
						long dy = npc.y() - y;
						if (dx * dx + dy * dy <= r2) {
							list.add(npc);
						}
					}
				}
			}
		}
		return list;
	}

	public static long cellKey(int x, int y) {
		int cx = x >> 12;
		int cy = y >> 12;
		return (((long) cx) << 32) | (cy & 0xFFFFFFFFL);
	}

	public boolean isGridAlwaysOn() {
		return com.lopez.l2j.config.Config.GRIDS_ALWAYS_ON;
	}

	public int getGridNeighborTurnOnTimeSeconds() {
		return com.lopez.l2j.config.Config.GRID_NEIGHBOR_TURN_ON_TIME;
	}

	public int getGridNeighborTurnOffTimeSeconds() {
		return com.lopez.l2j.config.Config.GRID_NEIGHBOR_TURN_OFF_TIME;
	}

	public boolean isCellActive(int x, int y) {
		if (com.lopez.l2j.config.Config.GRIDS_ALWAYS_ON) {
			return true;
		}
		return !findPlayersAround(x, y, GRID_SIZE).isEmpty();
	}

	public int getNoRestartKickTimeSeconds() {
		return com.lopez.l2j.config.Config.NO_RESTART_KICK_TIME;
	}

	public boolean isBoatAllowed() {
		return com.lopez.l2j.config.Config.ALLOW_BOAT;
	}

	public boolean isWaterAllowed() {
		return com.lopez.l2j.config.Config.ALLOW_WATER;
	}

	private static String key(String name) {
		return name.toLowerCase(java.util.Locale.ROOT);
	}
}

