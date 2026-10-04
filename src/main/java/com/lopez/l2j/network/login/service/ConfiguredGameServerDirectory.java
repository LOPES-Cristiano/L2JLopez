package com.lopez.l2j.network.login.service;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import java.util.List;
import org.springframework.stereotype.Component;

/** Um unico game server (id 1) descrito em l2.login / l2.network. */
@Component
class ConfiguredGameServerDirectory implements GameServerDirectory {

	private final List<ServerEntry> servers;

	ConfiguredGameServerDirectory(ServerProperties p) {
		servers = List.of(new ServerEntry(1, p.login().gameServerHost(), p.network().gamePort(), true, 0,
				p.login().maxPlayers(), true, false, false, false));
	}

	@Override
	public List<ServerEntry> servers() {
		return servers;
	}
}