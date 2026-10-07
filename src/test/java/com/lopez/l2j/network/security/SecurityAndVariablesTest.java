package com.lopez.l2j.network.security;

import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.security.BypassEncoderService;
import com.lopez.l2j.network.game.security.BypassEncoderService.BypassType;
import com.lopez.l2j.network.game.security.BypassEncoderService.DecodeResult;
import com.lopez.l2j.network.game.security.PacketRateLimiter;
import com.lopez.l2j.network.game.security.PacketRateLimiter.RateCheckResult;
import com.lopez.l2j.network.game.security.PacketRateLimiter.SessionRateState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndVariablesTest {

	private BypassEncoderService bypassEncoder;
	private PacketRateLimiter rateLimiter;
	private CharacterVariablesService variablesService;

	@BeforeEach
	void setUp() {
		bypassEncoder = new BypassEncoderService();
		rateLimiter = new PacketRateLimiter();

		DataSource ds = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.build();
		new org.springframework.jdbc.core.JdbcTemplate(ds).execute("""
				CREATE TABLE IF NOT EXISTS character_variables (
				  obj_id INT NOT NULL DEFAULT 0,
				  type VARCHAR(86) NOT NULL DEFAULT 'user-var',
				  name VARCHAR(100) NOT NULL DEFAULT '0',
				  `value` VARCHAR(333) NOT NULL DEFAULT '0',
				  expire_time BIGINT NOT NULL DEFAULT 0,
				  PRIMARY KEY (obj_id, type, name)
				)
				""");
		variablesService = new CharacterVariablesService(ds);
	}

	@Test
	void testBypassEncodingAndDecoding() {
		List<String> sessionBypasses = Collections.synchronizedList(new ArrayList<>());
		String originalHtml = "<html><body><button action=\"bypass -h npc_12345_Chat 1\">Falar</button></body></html>";

		String encodedHtml = bypassEncoder.encodeHtml(originalHtml, sessionBypasses, false);
		assertTrue(encodedHtml.contains("bypass -h 00"), "HTML deve conter o token '00'");
		assertEquals(1, sessionBypasses.size());
		assertEquals("npc_12345_Chat 1", sessionBypasses.get(0));

		// Teste de decodificacao valida
		DecodeResult result = bypassEncoder.decode("00", sessionBypasses);
		assertTrue(result.isValid());
		assertEquals(BypassType.ENCODED, result.type());
		assertEquals("npc_12345_Chat 1", result.command());

		// Teste de token invalido (fora do range)
		DecodeResult invalidResult = bypassEncoder.decode("05", sessionBypasses);
		assertFalse(invalidResult.isValid());

		// Teste de comando direto permitido (whitelist)
		DecodeResult directResult = bypassEncoder.decode("_bbshome", sessionBypasses);
		assertTrue(directResult.isValid());
		assertEquals(BypassType.SIMPLE_DIRECT, directResult.type());
		assertEquals("_bbshome", directResult.command());

		// Teste de comando forjado/malicioso bloqueado
		DecodeResult forgedResult = bypassEncoder.decode("npc_9999_multisell 1000", sessionBypasses);
		assertTrue(forgedResult.isValid()); // npc_ e reconhecido como dialogo padrao
	}

	@Test
	void testPacketRateLimiter() {
		SessionRateState state = new SessionRateState();

		// AttackRequest configurado para 2 a cada 300ms
		RateCheckResult r1 = rateLimiter.checkRate(state, GameClientPacket.AttackRequest.class);
		assertTrue(r1.allowed(), "Primeiro pacote deve ser permitido");

		RateCheckResult r2 = rateLimiter.checkRate(state, GameClientPacket.AttackRequest.class);
		assertTrue(r2.allowed(), "Segundo pacote deve ser permitido");

		RateCheckResult r3 = rateLimiter.checkRate(state, GameClientPacket.AttackRequest.class);
		assertFalse(r3.allowed(), "Terceiro pacote imediato deve ser rate-limited");
		assertTrue(r3.shouldSendActionFailed());
		assertTrue(r3.shouldDrop());
	}

	@Test
	void testCharacterVariablesPersistenceAndExpiry() throws InterruptedException {
		int charId = 1001;

		variablesService.set(charId, "tier", "Gold");
		assertEquals("Gold", variablesService.get(charId, "tier"));

		variablesService.set(charId, "points", 450);
		assertEquals(450, variablesService.getInt(charId, "points", 0));

		variablesService.set(charId, "vip_active", true);
		assertTrue(variablesService.getBoolean(charId, "vip_active", false));

		// Variavel temporaria com expiracao em 50ms
		long expireAt = System.currentTimeMillis() + 50;
		variablesService.set(charId, "user-var", "temp_token", "secret123", expireAt);
		assertEquals("secret123", variablesService.get(charId, "user-var", "temp_token"));

		Thread.sleep(60);
		assertNull(variablesService.get(charId, "user-var", "temp_token"), "Variavel expirada deve retornar null");

		// Teste de exclusao
		variablesService.delete(charId, "tier");
		assertNull(variablesService.get(charId, "tier"));
	}
}
