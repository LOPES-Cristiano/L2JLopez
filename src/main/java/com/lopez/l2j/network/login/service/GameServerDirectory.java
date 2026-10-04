package com.lopez.l2j.network.login.service;

import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import java.util.List;
import java.util.Optional;

/** Fonte da lista de game servers mostrada ao cliente (hoje configurada; depois, registro dinamico). */
public interface GameServerDirectory {

	List<ServerEntry> servers();

	default Optional<ServerEntry> find(int id) {
		return servers().stream().filter(s -> s.id() == id).findFirst();
	}
}