package com.lopez.l2j.network.login.service;

import java.util.Optional;

/** Porta de persistencia das contas. */
public interface AccountStore {

	Optional<Account> find(String login);

	void create(String login, String passwordHash, String ip);

	/** Registra ultimo acesso (login bem-sucedido ou entrada num servidor). */
	void touch(String login, String ip, int lastServerId);
}