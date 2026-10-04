package com.lopez.l2j.network.login;

import com.lopez.l2j.network.login.crypt.LoginCredentials;
import com.lopez.l2j.network.login.crypt.LoginCredentials.Credentials;
import com.lopez.l2j.network.login.crypt.ScrambledRsaKeyPair;
import com.lopez.l2j.network.login.packet.LoginClientPacket;
import com.lopez.l2j.network.login.packet.LoginClientPacket.AuthGameGuard;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestAuthLogin;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestServerList;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestServerLogin;
import com.lopez.l2j.network.login.packet.LoginServerPacket;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.GgAuth;
import com.lopez.l2j.network.login.packet.LoginServerPacket.Init;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerList;
import com.lopez.l2j.network.login.service.GameServerDirectory;
import com.lopez.l2j.network.login.service.LoginAccountService;
import com.lopez.l2j.network.session.SessionKey;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Maquina de estados de UMA conexao de login (porta de L2AuthClient + L2AuthPacketHandler + os
 * clientpackets). Nao conhece sockets: recebe o corpo ja decifrado de um pacote e devolve os pacotes de
 * resposta, o que a torna testavel sem rede. Nao e thread-safe; use uma thread por conexao.
 */
public final class LoginSession {

	/** Resposta a um pacote: o que enviar e se a conexao deve fechar depois. */
	public record Reply(List<LoginServerPacket> packets, boolean close) {
		static Reply send(LoginServerPacket... p) {
			return new Reply(List.of(p), false);
		}

		static Reply fail(LoginServerPacket p) {
			return new Reply(List.of(p), true);
		}

		static Reply failAuth(AuthFailReason reason) {
			return fail(new AuthFail(reason));
		}
	}

	private final int sessionId;
	private final byte[] blowfishKey;
	private final ScrambledRsaKeyPair rsa;
	private final boolean showLicence;
	private final String ip;
	private final LoginAccountService accounts;
	private final GameServerDirectory servers;
	private final int loginOk1;
	private final int loginOk2;
	private final int playOk1;
	private final int playOk2;

	private final BiConsumer<String, SessionKey> onPlayOk;

	private LoginClientPacket.State state = LoginClientPacket.State.CONNECTED;
	private String account;
	private int lastServerId;
	private boolean joinedGameServer;

	public LoginSession(ScrambledRsaKeyPair rsa, byte[] blowfishKey, boolean showLicence, String ip,
			LoginAccountService accounts, GameServerDirectory servers, SecureRandom random) {
		this(rsa, blowfishKey, showLicence, ip, accounts, servers, random, (a, k) -> {
		});
	}

	/** @param onPlayOk recebe conta e chave completa quando o cliente e liberado para o game server */
	public LoginSession(ScrambledRsaKeyPair rsa, byte[] blowfishKey, boolean showLicence, String ip,
			LoginAccountService accounts, GameServerDirectory servers, SecureRandom random,
			BiConsumer<String, SessionKey> onPlayOk) {
		this.rsa = rsa;
		this.blowfishKey = blowfishKey.clone();
		this.showLicence = showLicence;
		this.ip = ip;
		this.accounts = accounts;
		this.servers = servers;
		this.onPlayOk = onPlayOk;
		this.sessionId = random.nextInt();
		this.loginOk1 = random.nextInt();
		this.loginOk2 = random.nextInt();
		this.playOk1 = random.nextInt();
		this.playOk2 = random.nextInt();
	}

	public int sessionId() {
		return sessionId;
	}

	/** Primeiro pacote da conexao; deve ser enviado antes de qualquer leitura. */
	public Init init() {
		return new Init(sessionId, rsa.scrambledModulus(), blowfishKey);
	}

	/** Chave Blowfish desta sessao; o LoginCrypt da conexao deve ser criado com ela. */
	public byte[] blowfishKey() {
		return blowfishKey.clone();
	}

	public boolean authenticated() {
		return account != null;
	}

	public String account() {
		return account;
	}

	/** Processa o corpo (opcode + dados) de um pacote do cliente. */
	public Reply handle(byte[] body) {
		var decoded = LoginClientPacket.decode(state, body);
		if (decoded.isEmpty()) {
			return Reply.failAuth(AuthFailReason.ACCESS_FAILED);
		}
		return switch (decoded.get()) {
			case AuthGameGuard p -> onGameGuard(p);
			case RequestAuthLogin p -> onAuthLogin(p);
			case RequestServerList p -> onServerList(p);
			case RequestServerLogin p -> onServerLogin(p);
		};
	}

	/** Chame quando a conexao cair: libera a conta se o jogador nao chegou a entrar num game server. */
	public void onDisconnect() {
		if (account != null && !joinedGameServer) {
			accounts.release(account);
		}
	}

	private Reply onGameGuard(AuthGameGuard p) {
		if (p.sessionId() != sessionId) {
			return Reply.failAuth(AuthFailReason.ACCESS_FAILED);
		}
		state = LoginClientPacket.State.AUTHED_GG;
		return Reply.send(new GgAuth(sessionId));
	}

	private Reply onAuthLogin(RequestAuthLogin p) {
		var credentials = LoginCredentials.decode(p.rsaBlock(), rsa.privateKey());
		if (credentials.isEmpty()) {
			return Reply.failAuth(AuthFailReason.ACCESS_FAILED);
		}
		Credentials c = credentials.get();
		var result = accounts.authenticate(c.user(), c.password(), ip);
		return switch (result.status()) {
			case SUCCESS -> {
				account = c.user();
				lastServerId = result.lastServerId();
				state = LoginClientPacket.State.AUTHED_LOGIN;
				yield showLicence ? Reply.send(new AuthOk(loginOk1, loginOk2)) : Reply.send(serverList());
			}
			case INVALID_CREDENTIALS -> Reply.failAuth(AuthFailReason.USER_OR_PASS_WRONG);
			case BANNED -> Reply.failAuth(AuthFailReason.ACCOUNT_BANNED);
			case ALREADY_LOGGED_IN -> Reply.failAuth(AuthFailReason.ACCOUNT_IN_USE);
			case SYSTEM_ERROR -> Reply.failAuth(AuthFailReason.SYSTEM_ERROR);
		};
	}

	private Reply onServerList(RequestServerList p) {
		if (!keyMatches(p.loginOk1(), p.loginOk2())) {
			return Reply.failAuth(AuthFailReason.ACCESS_FAILED);
		}
		return Reply.send(serverList());
	}

	private Reply onServerLogin(RequestServerLogin p) {
		if (showLicence && !keyMatches(p.loginOk1(), p.loginOk2())) {
			return Reply.failAuth(AuthFailReason.ACCESS_FAILED);
		}
		var server = servers.find(p.serverId());
		if (server.isEmpty() || !server.get().up()
				|| server.get().currentPlayers() >= server.get().maxPlayers()) {
			return Reply.fail(new PlayFail(PlayFailReason.TOO_MANY_PLAYERS));
		}
		lastServerId = p.serverId();
		accounts.recordLastServer(account, ip, lastServerId);
		joinedGameServer = true;
		onPlayOk.accept(account, new SessionKey(loginOk1, loginOk2, playOk1, playOk2));
		return Reply.send(new PlayOk(playOk1, playOk2));
	}

	private boolean keyMatches(int ok1, int ok2) {
		return ok1 == loginOk1 && ok2 == loginOk2;
	}

	private ServerList serverList() {
		return new ServerList(servers.servers(), lastServerId);
	}

	/** Chave de sessao que o game server validara (playOk1/playOk2); exposta para o handshake login->game. */
	public int[] playKey() {
		return new int[] { playOk1, playOk2 };
	}

	/** Usado apenas pelos testes e pelo handshake: par loginOk enviado no AuthOk. */
	int[] loginKey() {
		return new int[] { loginOk1, loginOk2 };
	}
}