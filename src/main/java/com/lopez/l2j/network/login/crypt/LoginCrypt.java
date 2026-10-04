package com.lopez.l2j.network.login.crypt;

import java.security.SecureRandom;

/**
 * Cripto de transporte do login server (um por conexao). O primeiro pacote enviado (Init) usa a
 * chave Blowfish estatica conhecida do cliente + XOR pass; os seguintes usam a chave da sessao e
 * checksum.
 */
public final class LoginCrypt {

	/** Chave fixa do cliente Interlude para decifrar o Init. Nao e segredo: esta no cliente. */
	static final byte[] STATIC_BLOWFISH_KEY = {
			(byte) 0x6b, (byte) 0x60, (byte) 0xcb, (byte) 0x5b, (byte) 0x82, (byte) 0xce, (byte) 0x90, (byte) 0xb1,
			(byte) 0xcc, (byte) 0x2b, (byte) 0x6c, (byte) 0x55, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c };

	private final SecureRandom random;
	private final L2Blowfish staticCipher = new L2Blowfish(STATIC_BLOWFISH_KEY);
	private final L2Blowfish sessionCipher;
	private boolean first = true;

	public LoginCrypt(byte[] sessionBlowfishKey) {
		this(sessionBlowfishKey, new SecureRandom());
	}

	LoginCrypt(byte[] sessionBlowfishKey, SecureRandom random) {
		this.sessionCipher = new L2Blowfish(sessionBlowfishKey);
		this.random = random;
	}

	/** Gera a chave Blowfish de sessao (16 bytes aleatorios; o protocolo envia um 0x00 depois). */
	public static byte[] newSessionKey(SecureRandom random) {
		byte[] key = new byte[16];
		random.nextBytes(key);
		return key;
	}

	/**
	 * Decifra um pacote recebido do cliente e valida o checksum.
	 *
	 * @return false se o checksum nao bate (pacote adulterado/corrompido: feche a conexao)
	 */
	public boolean decrypt(byte[] raw, int offset, int size) {
		sessionCipher.decrypt(raw, offset, size);
		return LoginChecksum.verify(raw, offset, size);
	}

	/**
	 * Cifra um pacote a enviar. O buffer precisa ter espaco para o preenchimento (ate +16 bytes na
	 * primeira chamada, +12 nas demais).
	 *
	 * @param size tamanho do corpo sem checksum/padding
	 * @return tamanho final cifrado, a ser enviado
	 */
	public int encrypt(byte[] raw, int offset, int size) {
		size += 4; // checksum
		if (first) {
			size += 4; // chave do XOR pass
			size += 8 - size % 8;
			LoginChecksum.encXorPass(raw, offset, size, random.nextInt(Integer.MAX_VALUE));
			staticCipher.encrypt(raw, offset, size);
			first = false;
		} else {
			size += 8 - size % 8;
			LoginChecksum.append(raw, offset, size);
			sessionCipher.encrypt(raw, offset, size);
		}
		return size;
	}
}
