package com.lopez.l2j.network.login.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Um unico game server descrito em l2.login / l2.network e sincronizado com Config. */
@Component
class ConfiguredGameServerDirectory implements GameServerDirectory {

	private final List<ServerEntry> servers;

	ConfiguredGameServerDirectory(ServerProperties p) {
		int serverId = Config.REQUEST_SERVER_ID > 0 ? Config.REQUEST_SERVER_ID : 1;
		if (serverId != 1 && !Config.ACCEPT_ALTERNATE_ID && !Config.ACCEPT_NEW_GAME_SERVER) {
			serverId = 1;
		}
		String host = p.login().gameServerHost();
		if (host == null || host.isBlank() || "*".equals(host.trim()) || "0.0.0.0".equals(host.trim())) {
			host = (Config.INTERNAL_HOSTNAME != null && !Config.INTERNAL_HOSTNAME.isBlank() && !"*".equals(Config.INTERNAL_HOSTNAME.trim()))
					? Config.INTERNAL_HOSTNAME.trim()
					: ((Config.LOGIN_HOST != null && !Config.LOGIN_HOST.isBlank() && !"*".equals(Config.LOGIN_HOST.trim()))
							? Config.LOGIN_HOST.trim()
							: "127.0.0.1");
		}
		servers = List.of(new ServerEntry(serverId, host, p.network().gamePort(), true, 0,
				p.login().maxPlayers(), true, Config.SERVER_GM_ONLY, Config.SERVER_LIST_CLOCK, Config.SERVER_LIST_BRACKETS));
	}

	@Override
	public List<ServerEntry> servers() {
		return servers;
	}

	@Override
	public Optional<ServerEntry> find(int id) {
		Optional<ServerEntry> exact = servers.stream().filter(s -> s.id() == id).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		if ((Config.ACCEPT_ALTERNATE_ID || Config.ACCEPT_NEW_GAME_SERVER) && !servers.isEmpty()) {
			return Optional.of(servers.get(0));
		}
		return Optional.empty();
	}
}