package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BotsAndPasskeyServiceTest {

	private BotsPreventionService botsService;
	private WarehousePasskeyService passkeyService;
	private CharacterVariablesService charVars;

	@BeforeEach
	void setUp() {
		botsService = new BotsPreventionService();
		botsService.setEnabled(true);
		botsService.setBaseKillsCounter(60);
		botsService.setKillsRandomization(0); // Fixo em 60 para teste deterministico
		botsService.setValidationTimeoutSeconds(60);

		charVars = mock(CharacterVariablesService.class);
		passkeyService = new WarehousePasskeyService(charVars);
	}

	@Test
	void testBotsPreventionTriggerAfter60Kills() {
		int playerId = 2001;

		// 59 kills nao devem disparar o desafio
		for (int i = 1; i <= 59; i++) {
			Optional<BotsPreventionService.CaptchaChallenge> challengeOpt = botsService.onMobKill(playerId);
			assertTrue(challengeOpt.isEmpty(), "Kill " + i + " nao deveria disparar desafio");
		}

		// 60º kill dispara o desafio
		Optional<BotsPreventionService.CaptchaChallenge> challengeOpt = botsService.onMobKill(playerId);
		assertTrue(challengeOpt.isPresent(), "60º kill deve gerar desafio");

		var challenge = challengeOpt.get();
		assertTrue(challenge.getCorrectNumber() >= 100 && challenge.getCorrectNumber() <= 999);
		assertTrue(challenge.getOptions().contains(challenge.getCorrectNumber()));
		assertEquals(4, challenge.getOptions().size());
		assertTrue(botsService.hasPendingChallenge(playerId));
	}

	@Test
	void testBotsPreventionValidateCorrectAnswer() {
		int playerId = 2002;
		for (int i = 1; i <= 59; i++) {
			botsService.onMobKill(playerId);
		}
		var challenge = botsService.onMobKill(playerId).orElseThrow();

		// Resposta correta
		boolean validated = botsService.validateAnswer(playerId, challenge.getCorrectNumber());
		assertTrue(validated);
		assertFalse(botsService.hasPendingChallenge(playerId));
	}

	@Test
	void testBotsPreventionValidateIncorrectAnswer() {
		int playerId = 2003;
		for (int i = 1; i <= 59; i++) {
			botsService.onMobKill(playerId);
		}
		var challenge = botsService.onMobKill(playerId).orElseThrow();

		// Resposta propositalmente incorreta
		int wrong = challenge.getCorrectNumber() + 1;
		boolean validated = botsService.validateAnswer(playerId, wrong);
		assertFalse(validated);
		assertTrue(botsService.hasPendingChallenge(playerId));
	}

	@Test
	void testWarehousePasskeySetupAndValidation() {
		int playerId = 3001;
		when(charVars.get(eq(playerId), eq(WarehousePasskeyService.VAR_NAME))).thenReturn(null);

		assertFalse(passkeyService.hasPasskey(playerId));
		assertTrue(passkeyService.isUnlocked(playerId)); // Sem senha = livre

		// Configuracao de senha valida (6 digitos)
		boolean setupOk = passkeyService.setupPasskey(playerId, "123456");
		assertTrue(setupOk);
		verify(charVars).set(eq(playerId), eq(WarehousePasskeyService.VAR_NAME), anyString());

		// Rejeicao de senha invalida (< 4 digitos ou letras)
		assertFalse(passkeyService.setupPasskey(playerId, "12"));
		assertFalse(passkeyService.setupPasskey(playerId, "abcd"));
	}

	@Test
	void testWarehousePasskeyUnlockCorrectAndIncorrect() {
		int playerId = 3002;
		String validPin = "9876";
		String hashed = passkeyService.hashPin(playerId, validPin);
		when(charVars.get(eq(playerId), eq(WarehousePasskeyService.VAR_NAME))).thenReturn(hashed);

		assertTrue(passkeyService.hasPasskey(playerId));

		// Inicialmente trancado
		passkeyService.lock(playerId);
		assertFalse(passkeyService.isUnlocked(playerId));

		// Senha incorreta
		var resultFail = passkeyService.unlock(playerId, "0000");
		assertEquals(WarehousePasskeyService.ValidationResult.INVALID, resultFail);
		assertFalse(passkeyService.isUnlocked(playerId));

		// Senha correta
		var resultOk = passkeyService.unlock(playerId, validPin);
		assertEquals(WarehousePasskeyService.ValidationResult.SUCCESS, resultOk);
		assertTrue(passkeyService.isUnlocked(playerId));
		assertTrue(passkeyService.canAccessWarehouse(playerId));
	}

	@Test
	void testWarehousePasskeyLockoutAfter3Failures() {
		int playerId = 3003;
		String validPin = "5555";
		String hashed = passkeyService.hashPin(playerId, validPin);
		when(charVars.get(eq(playerId), eq(WarehousePasskeyService.VAR_NAME))).thenReturn(hashed);

		passkeyService.lock(playerId);

		// 1ª tentativa incorreta
		assertEquals(WarehousePasskeyService.ValidationResult.INVALID, passkeyService.unlock(playerId, "1111"));
		// 2ª tentativa incorreta
		assertEquals(WarehousePasskeyService.ValidationResult.INVALID, passkeyService.unlock(playerId, "2222"));
		// 3ª tentativa incorreta -> LOCKED_OUT
		assertEquals(WarehousePasskeyService.ValidationResult.LOCKED_OUT, passkeyService.unlock(playerId, "3333"));

		// 4ª tentativa mesmo com a senha correta deve continuar bloqueada durante o período de lockout
		assertEquals(WarehousePasskeyService.ValidationResult.LOCKED_OUT, passkeyService.unlock(playerId, validPin));
	}

	@Test
	void testWarehousePasskeyChange() {
		int playerId = 3004;
		String oldPin = "1111";
		String newPin = "2222";
		String oldHash = passkeyService.hashPin(playerId, oldPin);
		when(charVars.get(eq(playerId), eq(WarehousePasskeyService.VAR_NAME))).thenReturn(oldHash);

		// Mudanca com senha atual errada
		assertFalse(passkeyService.changePasskey(playerId, "9999", newPin));

		// Mudanca com senha atual correta
		assertTrue(passkeyService.changePasskey(playerId, oldPin, newPin));
		verify(charVars).set(eq(playerId), eq(WarehousePasskeyService.VAR_NAME), eq(passkeyService.hashPin(playerId, newPin)));
	}

	@Test
	void testWarehousePasskeyHtmlGeneration() {
		int playerId = 3005;
		when(charVars.get(eq(playerId), eq(WarehousePasskeyService.VAR_NAME))).thenReturn(null);

		String html = passkeyService.buildPasskeyHtml(playerId, "setup", "12", "Mensagem de Teste");
		assertTrue(html.contains("passkey_key"));
		assertTrue(html.contains("passkey_submit"));
		assertTrue(html.contains("Mensagem de Teste"));
		assertTrue(html.contains("**")); // 2 digitos mascarados
	}
}
