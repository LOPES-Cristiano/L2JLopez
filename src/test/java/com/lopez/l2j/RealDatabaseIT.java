package com.lopez.l2j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.features.achievements.AchievementProgressStore;
import com.lopez.l2j.network.login.crypt.LegacyPasswordHasher;
import com.lopez.l2j.network.login.service.AccountStore;
import com.lopez.l2j.network.login.service.AuthResult;
import com.lopez.l2j.network.login.service.LoginAccountService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Integracao contra o MySQL/MariaDB REAL (perfil padrao: Flyway ligado, variaveis L2_DB_URL/USER/PASSWORD).
 * Desligado por padrao para o build nao depender de banco; rode com {@code L2_IT=true ./mvnw test -Dtest=RealDatabaseIT} (o surefire nao pega *IT por padrao).
 * Usa o schema configurado: cria somente linhas com prefixo "it_" / ids altos e remove no fim.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "L2_IT", matches = "true")
class RealDatabaseIT {

	private static final int OWNER = 2_000_000_001;

	@Autowired
	JdbcClient jdbc;
	@Autowired
	AccountStore accounts;
	@Autowired
	AchievementProgressStore achievements;
	@Autowired
	LoginAccountService login;

	String user = "it_" + UUID.randomUUID().toString().substring(0, 8);

	@AfterEach
	void cleanup() {
		jdbc.sql("DELETE FROM accounts WHERE login = :l").param("l", user).update();
		jdbc.sql("DELETE FROM player_achievement WHERE owner_id = :o").param("o", OWNER).update();
	}

	@Test
	void allMigrationsAreApplied() {
		Integer failed = jdbc.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0")
				.query(Integer.class).single();
		Integer applied = jdbc.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1")
				.query(Integer.class).single();
		assertEquals(0, failed);
		assertTrue(applied >= 123, "migracoes aplicadas: " + applied);
	}

	@Test
	void accountStoreRoundTripOnLegacyTable() {
		accounts.create(user, LegacyPasswordHasher.hash("pw"), "127.0.0.1");
		var found = accounts.find(user).orElseThrow();
		assertEquals(user, found.login());
		assertEquals(0, found.accessLevel());
		assertTrue(LegacyPasswordHasher.matches("pw", found.passwordHash()));

		accounts.touch(user, "10.0.0.9", 3);
		assertEquals(3, accounts.find(user).orElseThrow().lastServerId());
	}

	@Test
	void loginServiceAuthenticatesAgainstRealAccount() {
		accounts.create(user, LegacyPasswordHasher.hash("pw"), "127.0.0.1");
		assertEquals(AuthResult.SUCCESS, login.authenticate(user, "pw", "127.0.0.1").status());
		login.release(user);
		assertEquals(AuthResult.INVALID_CREDENTIALS, login.authenticate(user, "bad", "127.0.0.1").status());
	}

	@Test
	void achievementProgressPersistsAndCountsRepeats() {
		achievements.recordCompletion(OWNER, 1);
		achievements.recordCompletion(OWNER, 1);
		achievements.recordCompletion(OWNER, 5);
		assertEquals(Set.of(1, 5), achievements.completedIds(OWNER));
		int times = jdbc.sql("SELECT times_completed FROM player_achievement WHERE owner_id=:o AND achievement_id=1")
				.param("o", OWNER).query(Integer.class).single();
		assertEquals(2, times);
	}
}
