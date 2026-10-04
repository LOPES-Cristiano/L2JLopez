package com.lopez.l2j.network.login.crypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Compatibilidade com a coluna accounts.password do L2JDream: SHA-1 sem sal, em Base64.
 * Serve apenas para VERIFICAR contas existentes; e fraco e deve ser trocado por hash com sal
 * (rehash no proximo login bem-sucedido) quando o fluxo de login for implementado.
 */
public final class LegacyPasswordHasher {

	private LegacyPasswordHasher() {
	}

	public static String hash(String password) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			return Base64.getEncoder().encodeToString(sha1.digest(password.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 indisponivel", e);
		}
	}

	/** Comparacao em tempo constante para nao vazar quantos caracteres do hash batem. */
	public static boolean matches(String password, String storedHash) {
		if (password == null || storedHash == null) {
			return false;
		}
		return MessageDigest.isEqual(
				hash(password).getBytes(StandardCharsets.UTF_8),
				storedHash.getBytes(StandardCharsets.UTF_8));
	}
}
