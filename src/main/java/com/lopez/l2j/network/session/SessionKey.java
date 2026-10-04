package com.lopez.l2j.network.session;

/**
 * Chave de sessao emitida pelo login (AuthOk = loginOk1/2, PlayOk = playOk1/2) e reapresentada pelo
 * cliente ao game server no pacote AuthLogin.
 */
public record SessionKey(int loginOk1, int loginOk2, int playOk1, int playOk2) {

	/** Com ShowLicence o cliente conhece as quatro partes; sem ela, so as do PlayOk sao confiaveis. */
	public boolean matches(SessionKey other, boolean checkLoginKeys) {
		if (other == null || playOk1 != other.playOk1 || playOk2 != other.playOk2) {
			return false;
		}
		return !checkLoginKeys || (loginOk1 == other.loginOk1 && loginOk2 == other.loginOk2);
	}
}
