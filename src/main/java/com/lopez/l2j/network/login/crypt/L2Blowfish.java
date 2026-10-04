package com.lopez.l2j.network.login.crypt;

import java.security.GeneralSecurityException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Blowfish do protocolo L2: Blowfish padrao (ECB, sem padding), mas cada palavra de 32 bits do bloco
 * trafega em little-endian. Em vez de manter o BlowfishEngine proprio de 1300 linhas do L2JDream,
 * usamos o provider JCE e invertemos os bytes de cada palavra na entrada e na saida.
 *
 * <p>Instancias nao sao thread-safe (um Cipher por direcao); crie uma por conexao.
 */
public final class L2Blowfish {

	private static final int BLOCK = 8;

	private final Cipher encrypt;
	private final Cipher decrypt;

	public L2Blowfish(byte[] key) {
		try {
			Key k = new SecretKeySpec(key, "Blowfish");
			encrypt = Cipher.getInstance("Blowfish/ECB/NoPadding");
			encrypt.init(Cipher.ENCRYPT_MODE, k);
			decrypt = Cipher.getInstance("Blowfish/ECB/NoPadding");
			decrypt.init(Cipher.DECRYPT_MODE, k);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Blowfish indisponivel nesta JVM", e);
		}
	}

	/** Cifra {@code size} bytes (multiplo de 8) in-place a partir de {@code offset}. */
	public void encrypt(byte[] data, int offset, int size) {
		process(encrypt, data, offset, size);
	}

	/** Decifra {@code size} bytes (multiplo de 8) in-place a partir de {@code offset}. */
	public void decrypt(byte[] data, int offset, int size) {
		process(decrypt, data, offset, size);
	}

	private static void process(Cipher cipher, byte[] data, int offset, int size) {
		if (size < 0 || (size % BLOCK) != 0) {
			throw new IllegalArgumentException("Tamanho deve ser multiplo de 8: " + size);
		}
		if (offset < 0 || offset + size > data.length) {
			throw new IllegalArgumentException("Intervalo fora do buffer");
		}
		byte[] block = new byte[BLOCK];
		try {
			for (int pos = offset; pos < offset + size; pos += BLOCK) {
				swapWords(data, pos, block);
				cipher.update(block, 0, BLOCK, block, 0);
				swapWords(block, 0, data, pos);
			}
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Copia 8 bytes de src para dst invertendo a ordem dos bytes de cada palavra de 4 bytes. */
	private static void swapWords(byte[] src, int srcPos, byte[] dst) {
		swapWords(src, srcPos, dst, 0);
	}

	private static void swapWords(byte[] src, int srcPos, byte[] dst, int dstPos) {
		byte a0 = src[srcPos], a1 = src[srcPos + 1], a2 = src[srcPos + 2], a3 = src[srcPos + 3];
		byte b0 = src[srcPos + 4], b1 = src[srcPos + 5], b2 = src[srcPos + 6], b3 = src[srcPos + 7];
		dst[dstPos] = a3;
		dst[dstPos + 1] = a2;
		dst[dstPos + 2] = a1;
		dst[dstPos + 3] = a0;
		dst[dstPos + 4] = b3;
		dst[dstPos + 5] = b2;
		dst[dstPos + 6] = b1;
		dst[dstPos + 7] = b0;
	}
}
