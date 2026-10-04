package com.lopez.l2j.network.login.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class L2BlowfishTest {

	private static byte[] hex(String s) {
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
		}
		return b;
	}

	@Test
	void matchesStandardBlowfishVectorWithLittleEndianWords() {
		// Vetor classico de Eric Young: chave 0, texto 0 -> 4EF997456198DD78 (big-endian).
		// Em L2 cada palavra de 4 bytes sai invertida: 4597F94E 78DD9861.
		byte[] data = new byte[8];
		new L2Blowfish(new byte[8]).encrypt(data, 0, 8);
		assertArrayEquals(hex("4597F94E78DD9861"), data);
	}

	@Test
	void matchesSecondStandardVector() {
		// Chave 0123456789ABCDEF, texto 1111111111111111 -> 61F9C3802281B096 (big-endian).
		byte[] data = hex("1111111111111111");
		new L2Blowfish(hex("0123456789ABCDEF")).encrypt(data, 0, 8);
		// Em L2 cada palavra sai invertida: 80C3F961 96B08122.
		assertArrayEquals(hex("80C3F96196B08122"), data);
	}

	@Test
	void roundTripsManyBlocksAtAnOffsetWithoutTouchingNeighbours() {
		byte[] key = new byte[16];
		new SecureRandom().nextBytes(key);
		byte[] original = new byte[8 * 5 + 6];
		new SecureRandom().nextBytes(original);
		byte[] data = original.clone();

		L2Blowfish bf = new L2Blowfish(key);
		bf.encrypt(data, 2, 32);
		assertFalse(Arrays.equals(original, data));
		assertEquals(original[0], data[0]);
		assertEquals(original[1], data[1]);
		assertArrayEquals(Arrays.copyOfRange(original, 34, 46), Arrays.copyOfRange(data, 34, 46));

		bf.decrypt(data, 2, 32);
		assertArrayEquals(original, data);
	}

	@Test
	void rejectsSizesThatAreNotWholeBlocks() {
		L2Blowfish bf = new L2Blowfish(new byte[16]);
		assertThrows(IllegalArgumentException.class, () -> bf.encrypt(new byte[16], 0, 12));
		assertThrows(IllegalArgumentException.class, () -> bf.decrypt(new byte[16], 8, 16));
	}

	@Test
	void differentKeysProduceDifferentCiphertext() {
		byte[] a = new byte[8];
		byte[] b = new byte[8];
		new L2Blowfish(hex("00000000000000000000000000000001")).encrypt(a, 0, 8);
		new L2Blowfish(hex("00000000000000000000000000000002")).encrypt(b, 0, 8);
		assertFalse(Arrays.equals(a, b));
	}
}
