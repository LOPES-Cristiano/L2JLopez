package com.lopez.l2j.game.world;

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

	/** Visao minima de um jogador online, implementada pela sessao. */
	public interface OnlinePlayer {
		int objectId();

		String name();

		int x();

		int y();

		int z();

		void send(GameServerPacket packet);

		default GameServerPacket charInfo() {
			return null;
		}

		default com.lopez.l2j.game.model.PlayerCharacter character() {
			return null;
		}
	}

	public static final int LOCAL_CHAT_RANGE = 1250;
	public static final int VISIBILITY_RADIUS = 3500;
	public static final int GRID_SIZE = 4096;

	private final Map<Integer, OnlinePlayer> playersById = new ConcurrentHashMap<>();
	private final Map<String, OnlinePlayer> playersByName = new ConcurrentHashMap<>();

	private final Map<Integer, NpcInstance> npcsById = new ConcurrentHashMap<>();
	private final Map<Long, Set<NpcInstance>> npcsByCell = new ConcurrentHashMap<>();

	// ---- PLAYERS ----

	public void add(OnlinePlayer p) {
		playersById.put(p.objectId(), p);
		playersByName.put(key(p.name()), p);
	}

	public void remove(OnlinePlayer p) {
		playersById.remove(p.objectId(), p);
		playersByName.remove(key(p.name()), p);
	}

	public int online() {
		return playersById.size();
	}

	public Collection<OnlinePlayer> players() {
		return playersById.values();
	}

	public Optional<OnlinePlayer> byName(String name) {
		return name == null ? Optional.empty() : Optional.ofNullable(playersByName.get(key(name)));
	}

	public Optional<OnlinePlayer> player(int objectId) {
		return Optional.ofNullable(playersById.get(objectId));
	}

	public List<OnlinePlayer> findPlayersAround(int x, int y, int range) {
		long r2 = (long) range * range;
		List<OnlinePlayer> list = new ArrayList<>();
		for (OnlinePlayer p : playersById.values()) {
			long dx = p.x() - x;
			long dy = p.y() - y;
			if (dx * dx + dy * dy <= r2) {
				list.add(p);
			}
		}
		return list;
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
		broadcast(packet, p -> {
			if (!includeSelf && p.objectId() == origin.objectId()) {
				return false;
			}
			long dx = p.x() - origin.x();
			long dy = p.y() - origin.y();
			return dx * dx + dy * dy <= r2;
		});
	}

	public void broadcastAround(int x, int y, int range, GameServerPacket packet) {
		long r2 = (long) range * range;
		broadcast(packet, p -> {
			long dx = p.x() - x;
			long dy = p.y() - y;
			return dx * dx + dy * dy <= r2;
		});
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

	private static String key(String name) {
		return name.toLowerCase(java.util.Locale.ROOT);
	}
}

