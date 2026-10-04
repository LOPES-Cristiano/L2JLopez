package com.lopez.l2j.network.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.crypt.LegacyPasswordHasher;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAccountServiceTest {

	static class MemoryStore implements AccountStore {
		final Map<String, Account> accounts = new HashMap<>();
		int touches;

		@Override
		public Optional<Account> find(String login) {
			return Optional.ofNullable(accounts.get(login));
		}

		@Override
		public void create(String login, String passwordHash, String ip) {
			accounts.put(login, new Account(login, passwordHash, 0, 0));
		}

		@Override
		public void touch(String login, String ip, int lastServerId) {
			touches++;
		}
	}

	MemoryStore store;

	LoginAccountService service(boolean autoCreate, int maxPerIp) {
		return new LoginAccountService(store,
				new ServerProperties.Login(autoCreate, maxPerIp, "127.0.0.1", 100, true));
	}

	@BeforeEach
	void setUp() {
		store = new MemoryStore();
		store.accounts.put("alice", new Account("alice", LegacyPasswordHasher.hash("pw"), 0, 1));
		store.accounts.put("mallory", new Account("mallory", LegacyPasswordHasher.hash("pw"), -1, 0));
	}

	@Test
	void validLoginSucceedsAndCarriesLastServer() {
		var r = service(false, 1).authenticate("alice", "pw", "1.1.1.1");
		assertEquals(AuthResult.SUCCESS, r.status());
		assertEquals(1, r.lastServerId());
		assertEquals(1, store.touches);
	}

	@Test
	void wrongPasswordAndUnknownUserLookIdentical() {
		var s = service(false, 1);
		assertEquals(AuthResult.INVALID_CREDENTIALS, s.authenticate("alice", "nope", "1.1.1.1").status());
		assertEquals(AuthResult.INVALID_CREDENTIALS, s.authenticate("ghost", "pw", "1.1.1.1").status());
		assertFalse(s.isOnline("alice"));
	}

	@Test
	void bannedAccountIsRefusedEvenWithCorrectPassword() {
		assertEquals(AuthResult.BANNED, service(false, 1).authenticate("mallory", "pw", "1.1.1.1").status());
	}

	@Test
	void secondLoginOfSameAccountIsRejectedUntilReleased() {
		var s = service(false, 1);
		assertEquals(AuthResult.SUCCESS, s.authenticate("alice", "pw", "1.1.1.1").status());
		assertEquals(AuthResult.ALREADY_LOGGED_IN, s.authenticate("alice", "pw", "2.2.2.2").status());
		s.release("alice");
		assertEquals(AuthResult.SUCCESS, s.authenticate("alice", "pw", "2.2.2.2").status());
	}

	@Test
	void rejectsMalformedLoginNames() {
		var s = service(true, 5);
		for (String bad : new String[] { "a", "x".repeat(15), "UPPER", "bad name", "semi;colon", "", null }) {
			assertEquals(AuthResult.INVALID_CREDENTIALS, s.authenticate(bad, "pw", "1.1.1.1").status(), String.valueOf(bad));
		}
		assertEquals(2, store.accounts.size());
	}

	@Test
	void autoCreateOffRefusesUnknownUsers() {
		assertEquals(AuthResult.INVALID_CREDENTIALS, service(false, 5).authenticate("newbie", "pw", "1.1.1.1").status());
		assertFalse(store.accounts.containsKey("newbie"));
	}

	@Test
	void autoCreateStoresHashedPasswordAndLogsIn() {
		var r = service(true, 5).authenticate("newbie", "secret", "1.1.1.1");
		assertEquals(AuthResult.SUCCESS, r.status());
		String stored = store.accounts.get("newbie").passwordHash();
		assertFalse(stored.contains("secret"));
		assertTrue(LegacyPasswordHasher.matches("secret", stored));
	}

	@Test
	void autoCreateIsLimitedPerIp() {
		var s = service(true, 2);
		assertEquals(AuthResult.SUCCESS, s.authenticate("aa", "pw", "9.9.9.9").status());
		assertEquals(AuthResult.SUCCESS, s.authenticate("bb", "pw", "9.9.9.9").status());
		assertEquals(AuthResult.INVALID_CREDENTIALS, s.authenticate("cc", "pw", "9.9.9.9").status());
		assertFalse(store.accounts.containsKey("cc"));
		assertEquals(AuthResult.SUCCESS, s.authenticate("cc", "pw", "8.8.8.8").status());
	}

	@Test
	void storeFailureBecomesSystemErrorNotAnException() {
		AccountStore broken = new MemoryStore() {
			@Override
			public Optional<Account> find(String login) {
				throw new IllegalStateException("db down");
			}
		};
		var s = new LoginAccountService(broken, new ServerProperties.Login(false, 1, "h", 1, true));
		assertEquals(AuthResult.SYSTEM_ERROR, s.authenticate("alice", "pw", "1.1.1.1").status());
		assertFalse(s.isOnline("alice"));
	}
}
