package com.lopez.l2j.network.game.crypt;

import java.security.SecureRandom;

/**
 * Cifra XOR encadeada do game server Interlude (porta de tools/security/GameCrypt + BlowFishKeygen).
 *
 * <p>Chave de 16 bytes: 8 aleatorios + sufixo fixo {@code C8 27 93 01 A1 6C 31 97}. Apos cada pacote, os
 * bytes 8..11 da chave (int LE) sao somados ao tamanho do pacote, independentemente em cada direcao.
 * A cifra comeca desligada: o primeiro {@link #encrypt} (o KeyPacket) sai em claro e liga a cifra nos dois
 * sentidos - exatamente como o legado, pois o cliente so cifra depois de receber a chave.
 */
public final class GameCrypt {

	private static final byte[] FIXED_SUFFIX = { (byte) 0xc8, 0x27, (byte) 0x93, 0x01, (byte) 0xa1, 0x6c, 0x31,
			(byte) 0x97 };

	private final byte[] inKey = new byte[16];
	private final byte[] outKey = new byte[16];
	private volatile boolean enabled;

	public GameCrypt(byte[] key) {
		if (key.length != 16) {
			throw new IllegalArgumentException("chave do game deve ter 16 bytes");
		}
		System.arraycopy(key, 0, inKey, 0, 16);
		System.arraycopy(key, 0, outKey, 0, 16);
	}

	public static byte[] newKey(SecureRandom random) {
		byte[] key = new byte[16];
		byte[] head = new byte[8];
		random.nextBytes(head);
		System.arraycopy(head, 0, key, 0, 8);
		System.arraycopy(FIXED_SUFFIX, 0, key, 8, 8);
		return key;
	}

	public boolean enabled() {
		return enabled;
	}

	public void decrypt(byte[] raw, int offset, int size) {
		if (!enabled) {
			return;
		}
		int prev = 0;
		for (int i = 0; i < size; i++) {
			int cipher = raw[offset + i] & 0xff;
			raw[offset + i] = (byte) (cipher ^ inKey[i & 15] ^ prev);
			prev = cipher;
		}
		advance(inKey, size);
	}

	public void encrypt(byte[] raw, int offset, int size) {
		if (!enabled) {
			enabled = true;
			return;
		}
		int prev = 0;
		for (int i = 0; i < size; i++) {
			prev = (raw[offset + i] & 0xff) ^ outKey[i & 15] ^ prev;
			raw[offset + i] = (byte) prev;
		}
		advance(outKey, size);
	}

	private static void advance(byte[] key, int size) {
		int old = (key[8] & 0xff) | (key[9] & 0xff) << 8 | (key[10] & 0xff) << 16 | (key[11] & 0xff) << 24;
		old += size;
		key[8] = (byte) old;
		key[9] = (byte) (old >> 8);
		key[10] = (byte) (old >> 16);
		key[11] = (byte) (old >> 24);
	}
}
