package com.lopez.l2j.game.world;

import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Jogadores dentro do mundo. Versao minima do L2World: sem regioes/knownlist ainda, so o necessario para
 * chat (local por distancia, shout global, tell por nome).
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
	}

	public static final int LOCAL_CHAT_RANGE = 1250;

	private final Map<Integer, OnlinePlayer> byId = new ConcurrentHashMap<>();
	private final Map<String, OnlinePlayer> byName = new ConcurrentHashMap<>();

	public void add(OnlinePlayer p) {
		byId.put(p.objectId(), p);
		byName.put(key(p.name()), p);
	}

	public void remove(OnlinePlayer p) {
		byId.remove(p.objectId(), p);
		byName.remove(key(p.name()), p);
	}

	public int online() {
		return byId.size();
	}

	public Collection<OnlinePlayer> players() {
		return byId.values();
	}

	public Optional<OnlinePlayer> byName(String name) {
		return name == null ? Optional.empty() : Optional.ofNullable(byName.get(key(name)));
	}

	public void broadcast(GameServerPacket packet, Predicate<OnlinePlayer> filter) {
		for (OnlinePlayer p : byId.values()) {
			if (filter.test(p)) {
				p.send(packet);
			}
		}
	}

	public void broadcastAround(OnlinePlayer origin, int range, GameServerPacket packet) {
		long r2 = (long) range * range;
		broadcast(packet, p -> {
			long dx = p.x() - origin.x();
			long dy = p.y() - origin.y();
			return dx * dx + dy * dy <= r2;
		});
	}

	private static String key(String name) {
		return name.toLowerCase(java.util.Locale.ROOT);
	}
}
