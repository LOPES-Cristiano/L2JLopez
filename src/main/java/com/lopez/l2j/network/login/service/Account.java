package com.lopez.l2j.network.login.service;

/** Linha da tabela accounts relevante para o login. */
public record Account(String login, String passwordHash, int accessLevel, int lastServerId) {

	public boolean banned() {
		return accessLevel < 0;
	}

	@Override
	public String toString() {
		return "Account[login=" + login + ", accessLevel=" + accessLevel + "]";
	}
}