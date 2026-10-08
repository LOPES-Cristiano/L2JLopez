package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Senha Numerica / Passkey para Armazem (Protected Warehouse).
 * Repatriado do acervo L2JDream V2 / L2jFrozen.
 * Protege o armazem particular e de cla contra acesso indevido com hash seguro (SHA-256 + salt)
 * persistido em character_variables e bloqueio contra forca bruta (lockout apos tentativas falhas).
 */
@Service
public class WarehousePasskeyService {

	private static final Logger log = LoggerFactory.getLogger(WarehousePasskeyService.class);
	public static final String VAR_NAME = "wh_passkey";
	public static final int MAX_FAILED_ATTEMPTS = 3;
	public static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5 minutos

	public enum ValidationResult {
		SUCCESS,
		INVALID,
		LOCKED_OUT,
		NOT_CONFIGURED
	}

	public record FailedAttemptData(int attempts, long lastAttemptTime) {
		public boolean isLockedOut(long now) {
			return attempts >= MAX_FAILED_ATTEMPTS && (now - lastAttemptTime) < LOCKOUT_DURATION_MS;
		}
	}

	private final CharacterVariablesService characterVariables;
	private final Set<Integer> unlockedSessions = ConcurrentHashMap.newKeySet();
	private final Map<Integer, FailedAttemptData> failedAttempts = new ConcurrentHashMap<>();

	public WarehousePasskeyService() {
		this(null);
	}

	@Autowired(required = false)
	public WarehousePasskeyService(CharacterVariablesService characterVariables) {
		this.characterVariables = characterVariables;
	}

	/**
	 * Verifica se o jogador possui uma senha configurada.
	 */
	public boolean hasPasskey(int playerId) {
		if (characterVariables == null) {
			return false;
		}
		String hash = characterVariables.get(playerId, VAR_NAME);
		return hash != null && !hash.isBlank();
	}

	/**
	 * Verifica se o armazem esta liberado para a sessao ativa do jogador.
	 */
	public boolean isUnlocked(int playerId) {
		if (!hasPasskey(playerId)) {
			return true; // Sem senha configurada = liberado por padrao
		}
		return unlockedSessions.contains(playerId);
	}

	/**
	 * Tranca o armazem para a sessao do jogador.
	 */
	public void lock(int playerId) {
		unlockedSessions.remove(playerId);
	}

	/**
	 * Configura uma nova senha numerica (4 a 8 digitos).
	 */
	public boolean setupPasskey(int playerId, String pin) {
		if (pin == null || !pin.matches("\\d{4,8}")) {
			return false;
		}
		if (characterVariables == null) {
			return false;
		}
		String hashed = hashPin(playerId, pin.trim());
		characterVariables.set(playerId, VAR_NAME, hashed);
		unlockedSessions.add(playerId);
		failedAttempts.remove(playerId);
		log.info("Passkey configurada com sucesso para player {}", playerId);
		return true;
	}

	/**
	 * Altera a senha existente mediante confirmacao da senha atual.
	 */
	public boolean changePasskey(int playerId, String currentPin, String newPin) {
		if (unlock(playerId, currentPin) != ValidationResult.SUCCESS) {
			return false;
		}
		return setupPasskey(playerId, newPin);
	}

	/**
	 * Valida o PIN inserido pelo jogador.
	 */
	public ValidationResult unlock(int playerId, String pin) {
		if (!hasPasskey(playerId)) {
			return ValidationResult.NOT_CONFIGURED;
		}

		long now = System.currentTimeMillis();
		FailedAttemptData failed = failedAttempts.get(playerId);
		if (failed != null && failed.isLockedOut(now)) {
			log.warn("Player {} tentou acessar armazem sob bloqueio temporal (lockout)", playerId);
			return ValidationResult.LOCKED_OUT;
		}

		String expectedHash = characterVariables.get(playerId, VAR_NAME);
		String candidateHash = hashPin(playerId, pin != null ? pin.trim() : "");

		if (expectedHash.equalsIgnoreCase(candidateHash)) {
			failedAttempts.remove(playerId);
			unlockedSessions.add(playerId);
			log.info("Player {} desbloqueou o armazem com sucesso", playerId);
			return ValidationResult.SUCCESS;
		}

		int attempts = (failed != null ? failed.attempts() : 0) + 1;
		failedAttempts.put(playerId, new FailedAttemptData(attempts, now));
		log.warn("Tentativa de passkey incorreta para player {} (tentativa {}/{})", playerId, attempts, MAX_FAILED_ATTEMPTS);

		if (attempts >= MAX_FAILED_ATTEMPTS) {
			return ValidationResult.LOCKED_OUT;
		}
		return ValidationResult.INVALID;
	}

	/**
	 * Checa permissao de acesso ao armazem.
	 */
	public boolean canAccessWarehouse(int playerId) {
		return isUnlocked(playerId);
	}

	/**
	 * Gera o hash criptografico SHA-256 com salt individual do jogador.
	 */
	public String hashPin(int playerId, String pin) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String salted = "l2jlopez:wh:" + playerId + ":" + pin;
			byte[] digest = md.digest(salted.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Algoritmo SHA-256 nao encontrado na JVM", e);
		}
	}

	/**
	 * Interface HTML interativa com teclado numerico virtual para insercao de passkey.
	 */
	public String buildPasskeyHtml(int playerId, String mode, String currentInput, String feedbackMessage) {
		boolean configured = hasPasskey(playerId);
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Seguranca do Armazem (Passkey) ===</font><br><br>");

		if (feedbackMessage != null && !feedbackMessage.isBlank()) {
			sb.append("<font color=\"FF0000\">").append(feedbackMessage).append("</font><br><br>");
		}

		if (!configured) {
			sb.append("Seu armazem ainda nao possui senha de protecao.<br>");
			sb.append("Defina um PIN de 4 a 8 digitos:<br><br>");
		} else {
			sb.append("Insira sua senha de 4 a 8 digitos:<br><br>");
		}

		String display = (currentInput == null || currentInput.isEmpty()) ? "____" : "*".repeat(currentInput.length());
		sb.append("<font color=\"00FF00\"><b><h2>").append(display).append("</h2></b></font><br>");

		// Teclado Numerico 3x4
		sb.append("<table width=160>");
		int[][] pad = {
				{1, 2, 3},
				{4, 5, 6},
				{7, 8, 9}
		};
		String baseInput = currentInput != null ? currentInput : "";

		for (int[] row : pad) {
			sb.append("<tr>");
			for (int num : row) {
				sb.append("<td><button value=\"").append(num)
						.append("\" action=\"bypass -h passkey_key ").append(mode).append(" ").append(baseInput).append(num)
						.append("\" width=45 height=24 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			}
			sb.append("</tr>");
		}
		// Ultima linha: Limpar, 0, Confirmar
		sb.append("<tr>");
		sb.append("<td><button value=\"C\" action=\"bypass -h passkey_key ").append(mode).append(" \" width=45 height=24 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td><button value=\"0\" action=\"bypass -h passkey_key ").append(mode).append(" ").append(baseInput).append("0\" width=45 height=24 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td><button value=\"OK\" action=\"bypass -h passkey_submit ").append(mode).append(" ").append(baseInput).append("\" width=45 height=24 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr>");
		sb.append("</table>");

		sb.append("</center></body></html>");
		return sb.toString();
	}
}
