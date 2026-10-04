package com.lopez.l2j.network.login.crypt;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Cipher;

/**
 * Decifra o bloco RSA de 128 bytes do RequestAuthLogin e extrai usuario e senha nos offsets do
 * protocolo (usuario em 0x5E, 14 bytes; senha em 0x6C, 16 bytes).
 */
public final class LoginCredentials {

	public static final int RSA_BLOCK = 0x80;

	private LoginCredentials() {
	}

	/** Credenciais extraidas. {@code toString} nunca expoe a senha. */
	public record Credentials(String user, String password) {
		@Override
		public String toString() {
			return "Credentials[user=" + user + ", password=***]";
		}
	}

	/** @return vazio se o bloco nao puder ser decifrado ou nao tiver usuario. */
	public static Optional<Credentials> decode(byte[] block, RSAPrivateKey key) {
		if (block == null || block.length != RSA_BLOCK) {
			return Optional.empty();
		}
		byte[] decrypted;
		try {
			Cipher rsa = Cipher.getInstance("RSA/ECB/NoPadding");
			rsa.init(Cipher.DECRYPT_MODE, key);
			decrypted = rsa.doFinal(block, 0, RSA_BLOCK);
		} catch (GeneralSecurityException e) {
			return Optional.empty();
		}
		if (decrypted.length < RSA_BLOCK) {
			// BigInteger remove zeros a esquerda; recoloca-os para os offsets baterem.
			byte[] padded = new byte[RSA_BLOCK];
			System.arraycopy(decrypted, 0, padded, RSA_BLOCK - decrypted.length, decrypted.length);
			decrypted = padded;
		}
		String user = readString(decrypted, 0x5E, 14).toLowerCase(Locale.ROOT);
		String password = readString(decrypted, 0x6C, 16);
		if (user.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Credentials(user, password));
	}

	/** Le ate o primeiro NUL (o cliente preenche o campo com zeros), removendo espacos nas pontas. */
	private static String readString(byte[] data, int offset, int length) {
		int end = offset;
		while (end < offset + length && data[end] != 0) {
			end++;
		}
		return new String(data, offset, end - offset, StandardCharsets.ISO_8859_1).trim();
	}
}
