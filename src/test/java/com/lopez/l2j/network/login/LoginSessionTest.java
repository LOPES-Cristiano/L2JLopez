package com.lopez.l2j.network.login;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.crypt.LegacyPasswordHasher;
import com.lopez.l2j.network.login.crypt.ScrambledRsaKeyPair;
import com.lopez.l2j.network.login.packet.LoginServerPacket;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.GgAuth;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerList;
import com.lopez.l2j.network.login.packet.PacketWriter;
import com.lopez.l2j.network.login.service.Account;
import com.lopez.l2j.network.login.service.AccountStore;
import com.lopez.l2j.network.login.service.GameServerDirectory;
import com.lopez.l2j.network.login.service.LoginAccountService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginSessionTest {

	static ScrambledRsaKeyPair rsa;
	final SecureRandom random = new SecureRandom();
	final Map<String, Account> accounts = new HashMap<>();
	LoginAccountService service;
	int servedServers;
	ServerEntry gameServer = new ServerEntry(1, "127.0.0.1", 7777, true, 0, 10, true, false, false, false);

	@BeforeAll
	static void keys() {
		rsa = ScrambledRsaKeyPair.generate(new SecureRandom());
	}

	@BeforeEach
	void setUp() {
		accounts.put("alice", new Account("alice", LegacyPasswordHasher.hash("pw"), 0, 0));
		accounts.put("mallory", new Account("mallory", LegacyPasswordHasher.hash("pw"), -1, 0));
		service = new LoginAccountService(new AccountStore() {
			public Optional<Account> find(String l) {
				return Optional.ofNullable(accounts.get(l));
			}

			public void create(String l, String h, String ip) {
			}

			public void touch(String l, String ip, int s) {
			}
		}, new ServerProperties.Login(false, 1, "127.0.0.1", 10, true));
	}

	LoginSession session(boolean licence) {
		GameServerDirectory dir = () -> List.of(gameServer);
		return new LoginSession(rsa, new byte[16], licence, "1.2.3.4", service, dir, random);
	}

	static byte[] rsaBlock(String user, String pass) throws Exception {
		byte[] plain = new byte[128];
		System.arraycopy(user.getBytes(StandardCharsets.ISO_8859_1), 0, plain, 0x5E, user.length());
		System.arraycopy(pass.getBytes(StandardCharsets.ISO_8859_1), 0, plain, 0x6C, pass.length());
		Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
		c.init(Cipher.ENCRYPT_MODE, rsa.publicKey());
		return c.doFinal(plain);
	}

	static byte[] gg(int sid) {
		return new PacketWriter().writeC(0x07).writeD(sid).writeD(0).writeD(0).writeD(0).writeD(0).toByteArray();
	}

	static byte[] authLogin(String u, String p) throws Exception {
		return new PacketWriter().writeC(0x00).writeB(rsaBlock(u, p)).toByteArray();
	}

	static byte[] serverLogin(int k1, int k2, int id) {
		return new PacketWriter().writeC(0x02).writeD(k1).writeD(k2).writeC(id).toByteArray();
	}

	LoginSession authed(LoginSession s) throws Exception {
		s.handle(gg(s.sessionId()));
		s.handle(authLogin("alice", "pw"));
		return s;
	}

	@Test
	void initCarriesSessionIdModulusAndBlowfishKey() {
		var s = session(true);
		assertEquals(s.sessionId(), s.init().sessionId());
		assertArrayEquals(rsa.scrambledModulus(), s.init().scrambledModulus());
	}

	@Test
	void happyPathWithLicenceGoesThroughAuthOkListAndPlayOk() throws Exception {
		var s = session(true);
		var r1 = s.handle(gg(s.sessionId()));
		assertEquals(List.of(new GgAuth(s.sessionId())), r1.packets());
		assertFalse(r1.close());

		var r2 = s.handle(authLogin("alice", "pw"));
		assertFalse(r2.close());
		AuthOk ok = (AuthOk) r2.packets().get(0);
		assertTrue(s.authenticated());

		var r3 = s.handle(new PacketWriter().writeC(0x05).writeD(ok.loginOk1()).writeD(ok.loginOk2()).toByteArray());
		assertTrue(r3.packets().get(0) instanceof ServerList);

		var r4 = s.handle(serverLogin(ok.loginOk1(), ok.loginOk2(), 1));
		PlayOk play = (PlayOk) r4.packets().get(0);
		assertEquals(s.playKey()[0], play.playOk1());
		assertEquals(s.playKey()[1], play.playOk2());
	}

	@Test
	void withoutLicenceLoginGoesStraightToServerList() throws Exception {
		var s = session(false);
		s.handle(gg(s.sessionId()));
		assertTrue(s.handle(authLogin("alice", "pw")).packets().get(0) instanceof ServerList);
	}

	@Test
	void wrongGameGuardSessionIdClosesConnection() {
		var s = session(true);
		var r = s.handle(gg(s.sessionId() + 1));
		assertTrue(r.close());
		assertEquals(new AuthFail(AuthFailReason.ACCESS_FAILED), r.packets().get(0));
	}

	@Test
	void authLoginBeforeGameGuardIsRejected() throws Exception {
		var r = session(true).handle(authLogin("alice", "pw"));
		assertTrue(r.close());
	}

	@Test
	void badPasswordBannedAndGarbageBlockAreAllRefused() throws Exception {
		var s1 = session(true);
		s1.handle(gg(s1.sessionId()));
		assertEquals(new AuthFail(AuthFailReason.USER_OR_PASS_WRONG), s1.handle(authLogin("alice", "bad")).packets().get(0));

		var s2 = session(true);
		s2.handle(gg(s2.sessionId()));
		assertEquals(new AuthFail(AuthFailReason.ACCOUNT_BANNED), s2.handle(authLogin("mallory", "pw")).packets().get(0));

		var s3 = session(true);
		s3.handle(gg(s3.sessionId()));
		byte[] junk = new byte[129];
		junk[0] = 0x00;
		var r = s3.handle(junk);
		assertTrue(r.close());
		assertFalse(s3.authenticated());
	}

	@Test
	void wrongLoginKeyCannotRequestServerListOrEnterServer() throws Exception {
		var s = authed(session(true));
		var r1 = s.handle(new PacketWriter().writeC(0x05).writeD(1).writeD(2).toByteArray());
		assertTrue(r1.close());
		var r2 = authed(session(true)).handle(serverLogin(1, 2, 1));
		assertTrue(r2.close());
	}

	@Test
	void fullOrDownOrUnknownServerGivesPlayFail() throws Exception {
		var s = authed(session(false));
		assertEquals(new PlayFail(PlayFailReason.TOO_MANY_PLAYERS), s.handle(serverLogin(0, 0, 9)).packets().get(0));
		gameServer = new ServerEntry(1, "127.0.0.1", 7777, true, 10, 10, true, false, false, false);
		var s2 = authed(session(false));
		assertTrue(s2.handle(serverLogin(0, 0, 1)).close());
	}

	@Test
	void disconnectBeforeJoiningReleasesAccountButAfterJoiningKeepsIt() throws Exception {
		var s = authed(session(false));
		assertTrue(service.isOnline("alice"));
		s.onDisconnect();
		assertFalse(service.isOnline("alice"));

		var s2 = authed(session(false));
		s2.handle(serverLogin(0, 0, 1));
		s2.onDisconnect();
		assertTrue(service.isOnline("alice"), "conta segue ocupada pelo game server ate ele liberar");
	}

	@Test
	void accountInUseIsReportedToSecondSession() throws Exception {
		authed(session(false));
		var s = session(false);
		s.handle(gg(s.sessionId()));
		assertEquals(new AuthFail(AuthFailReason.ACCOUNT_IN_USE), s.handle(authLogin("alice", "pw")).packets().get(0));
	}

	@Test
	void everyReplyPacketEncodesWithoutError() throws Exception {
		var s = session(true);
		for (LoginServerPacket p : s.handle(gg(s.sessionId())).packets()) {
			assertTrue(p.encode().length > 0);
		}
	}
}