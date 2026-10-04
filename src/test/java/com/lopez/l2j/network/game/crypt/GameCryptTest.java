package com.lopez.l2j.network.game.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameCryptTest {

	final SecureRandom random = new SecureRandom();

	@Test
	void keyHasLegacyFixedSuffix() {
		byte[] key = GameCrypt.newKey(random);
		assertEquals(16, key.length);
		assertArrayEquals(new byte[] { (byte) 0xc8, 0x27, (byte) 0x93, 0x01, (byte) 0xa1, 0x6c, 0x31, (byte) 0x97 },
				java.util.Arrays.copyOfRange(key, 8, 16));
	}

	@Test
	void firstEncryptIsPlainAndEnablesCipher() {
		var crypt = new GameCrypt(GameCrypt.newKey(random));
		byte[] keyPacket = { 0x00, 0x01, 1, 2, 3 };
		byte[] copy = keyPacket.clone();
		assertFalse(crypt.enabled());
		crypt.encrypt(keyPacket, 0, keyPacket.length);
		assertArrayEquals(copy, keyPacket);
		assertTrue(crypt.enabled());

		byte[] next = { 0x13, 5, 6, 7 };
		byte[] nextCopy = next.clone();
		crypt.encrypt(next, 0, next.length);
		assertFalse(java.util.Arrays.equals(nextCopy, next));
	}

	@Test
	void decryptBeforeEnableIsNoOp() {
		var crypt = new GameCrypt(GameCrypt.newKey(random));
		byte[] protocolVersion = { 0x00, (byte) 0xe2, 0x02, 0, 0 };
		byte[] copy = protocolVersion.clone();
		crypt.decrypt(protocolVersion, 0, protocolVersion.length);
		assertArrayEquals(copy, protocolVersion);
	}

	/** Servidor e cliente usam o mesmo algoritmo em sentidos opostos; chaves evoluem em sincronia. */
	@Test
	void serverAndClientStayInSyncAcrossManyPackets() {
		byte[] key = GameCrypt.newKey(random);
		var server = new GameCrypt(key);
		var client = new GameCrypt(key);
		server.encrypt(new byte[3], 0, 3); // KeyPacket em claro liga a cifra
		client.encrypt(new byte[0], 0, 0); // cliente liga ao receber a chave
		Random r = new Random(42);
		for (int i = 0; i < 200; i++) {
			byte[] plain = new byte[1 + r.nextInt(300)];
			r.nextBytes(plain);
			byte[] wire = plain.clone();
			server.encrypt(wire, 0, wire.length);
			client.decrypt(wire, 0, wire.length);
			assertArrayEquals(plain, wire, "servidor->cliente pacote " + i);

			byte[] up = new byte[1 + r.nextInt(300)];
			r.nextBytes(up);
			byte[] upWire = up.clone();
			client.encrypt(upWire, 0, upWire.length);
			server.decrypt(upWire, 0, upWire.length);
			assertArrayEquals(up, upWire, "cliente->servidor pacote " + i);
		}
	}

	/** Vetor fixo calculado com o algoritmo legado (XOR encadeado + soma do tamanho nos bytes 8..11). */
	@Test
	void matchesLegacyAlgorithmOnKnownVector() {
		byte[] key = new byte[16];
		for (int i = 0; i < 16; i++) {
			key[i] = (byte) (i * 17 + 3);
		}
		var crypt = new GameCrypt(key);
		crypt.encrypt(new byte[1], 0, 1);
		byte[] data = { 1, 2, 3, 4 };
		crypt.encrypt(data, 0, 4);
		// legado: temp = raw ^ key[i] ^ temp
		int t0 = 1 ^ 3;
		int t1 = 2 ^ 20 ^ t0;
		int t2 = 3 ^ 37 ^ t1;
		int t3 = 4 ^ 54 ^ t2;
		assertArrayEquals(new byte[] { (byte) t0, (byte) t1, (byte) t2, (byte) t3 }, data);
		// segundo pacote usa a chave com bytes 8..11 somados de 4
		byte[] second = { 0, 0, 0, 0, 0, 0, 0, 0, 9 };
		crypt.encrypt(second, 0, second.length);
		int expectedKey8 = (key[8] & 0xff) + 4;
		int prev = second[7] & 0xff;
		assertEquals((byte) (9 ^ expectedKey8 ^ prev), second[8]);
	}

	@Test
	void rejectsWrongKeySize() {
		assertThrows(IllegalArgumentException.class, () -> new GameCrypt(new byte[8]));
	}
}
