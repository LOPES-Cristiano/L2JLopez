package com.lopez.l2j.network.login.service;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acesso JDBC a tabela legada accounts (colunas: login, password, lastactive, accessLevel, lastIP, lastServerId). */
@Repository
class JdbcAccountStore implements AccountStore {

	private final JdbcClient jdbc;

	JdbcAccountStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<Account> find(String login) {
		return jdbc.sql("SELECT login, password, accessLevel, lastServerId FROM accounts WHERE login = :login")
				.param("login", login)
				.query((rs, n) -> new Account(rs.getString("login"), rs.getString("password"),
						rs.getInt("accessLevel"), rs.getInt("lastServerId")))
				.optional();
	}

	@Override
	public void create(String login, String passwordHash, String ip) {
		jdbc.sql("""
				INSERT INTO accounts (login, password, lastactive, accessLevel, lastIP)
				VALUES (:login, :pwd, :now, 0, :ip)
				""")
				.param("login", login)
				.param("pwd", passwordHash)
				.param("now", System.currentTimeMillis() / 1000)
				.param("ip", ip)
				.update();
	}

	@Override
	public void touch(String login, String ip, int lastServerId) {
		jdbc.sql("UPDATE accounts SET lastactive = :now, lastIP = :ip, lastServerId = :srv WHERE login = :login")
				.param("now", System.currentTimeMillis() / 1000)
				.param("ip", ip)
				.param("srv", lastServerId)
				.param("login", login)
				.update();
	}
}