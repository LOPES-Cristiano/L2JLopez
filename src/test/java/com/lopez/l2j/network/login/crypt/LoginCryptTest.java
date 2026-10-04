package com.lopez.l2j.network.login.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LoginCryptTest {

	private static final int OFFSET = 2; // cabecalho de tamanho do MMO

	private final SecureRandom rnd = new SecureRandom();

	@Test
	void checksumRoundTripAndTamperDetection() {
		for (int offset : new int[] { 0, 2 }) {
			byte[] raw = new byte[offset + 24];
			rnd.nextBytes(raw);
			LoginChecksum.append(raw, offset, 24);
			assertTrue(LoginChecksum.verify(raw, offset, 24));
			raw[offset + 5] ^= 0x01;
			assertFalse(LoginChecksum.verify(raw, offset, 24));
		}
	}

	@Test
	void checksumRejectsMalformedSizes() {
		assertFalse(LoginChecksum.verify(new byte[8], 0, 4));
		assertFalse(LoginChecksum.verify(new byte[8], 0, 7));
	}

	@Test
	void xorPassIsReversible() {
		for (int offset : new int[] { 0, 2 }) {
			byte[] original = new byte[offset + 32];
			rnd.nextBytes(original);
			byte[] data = original.clone();
			LoginChecksum.encXorPass(data, offset, 32, 0x1234567);
			assertFalse(Arrays.equals(original, data));
			LoginChecksum.decXorPass(data, offset, 32);
			// os 4 primeiros bytes do corpo e os 8 finais sao reservados; o miolo volta ao original
			assertArrayEquals(Arrays.copyOfRange(original, offset + 4, offset + 24),
					Arrays.copyOfRange(data, offset + 4, offset + 24));
		}
	}

	@Test
	void firstPacketIsStaticallyEncryptedAndClientCanRecoverTheBody() {
		byte[] sessionKey = LoginCrypt.newSessionKey(rnd);
		LoginCrypt server = new LoginCrypt(sessionKey);

		byte[] body = new byte[17]; // opcode + dados (ex.: Init)
		rnd.nextBytes(body);
		byte[] buf = new byte[OFFSET + 64];
		System.arraycopy(body, 0, buf, OFFSET, body.length);

		int size = server.encrypt(buf, OFFSET, body.length);
		assertEquals(0, size % 8);

		// cliente: Blowfish estatico e depois desfaz o XOR pass
		new L2Blowfish(LoginCrypt.STATIC_BLOWFISH_KEY).decrypt(buf, OFFSET, size);
		LoginChecksum.decXorPass(buf, OFFSET, size);
		assertArrayEquals(Arrays.copyOfRange(body, 4, 17), Arrays.copyOfRange(buf, OFFSET + 4, OFFSET + 17));
	}

	@Test
	void laterPacketsUseSessionKeyAndChecksum() {
		byte[] sessionKey = LoginCrypt.newSessionKey(rnd);
		LoginCrypt server = new LoginCrypt(sessionKey);
		LoginCrypt client = new LoginCrypt(sessionKey);

		server.encrypt(new byte[OFFSET + 64], OFFSET, 8); // consome o modo estatico

		byte[] body = new byte[11];
		rnd.nextBytes(body);
		byte[] buf = new byte[OFFSET + 64];
		System.arraycopy(body, 0, buf, OFFSET, body.length);
		int size = server.encrypt(buf, OFFSET, body.length);

		assertTrue(client.decrypt(buf, OFFSET, size));
		assertArrayEquals(body, Arrays.copyOfRange(buf, OFFSET, OFFSET + body.length));
	}

	@Test
	void tamperedPacketFailsChecksumAfterDecrypt() {
		byte[] sessionKey = LoginCrypt.newSessionKey(rnd);
		LoginCrypt server = new LoginCrypt(sessionKey);
		LoginCrypt client = new LoginCrypt(sessionKey);
		server.encrypt(new byte[OFFSET + 64], OFFSET, 8);

		byte[] buf = new byte[OFFSET + 64];
		int size = server.encrypt(buf, OFFSET, 11);
		buf[OFFSET + 3] ^= 0x40;
		assertFalse(client.decrypt(buf, OFFSET, size));
	}

	@Test
	void wrongSessionKeyCannotDecrypt() {
		LoginCrypt server = new LoginCrypt(LoginCrypt.newSessionKey(rnd));
		LoginCrypt attacker = new LoginCrypt(LoginCrypt.newSessionKey(rnd));
		server.encrypt(new byte[OFFSET + 64], OFFSET, 8);
		byte[] buf = new byte[OFFSET + 64];
		byte[] body = new byte[11];
		rnd.nextBytes(body);
		System.arraycopy(body, 0, buf, OFFSET, body.length);
		int size = server.encrypt(buf, OFFSET, body.length);
		assertFalse(attacker.decrypt(buf, OFFSET, size));
	}

	@Test
	void sessionKeysAreRandom() {
		assertNotEquals(Arrays.toString(LoginCrypt.newSessionKey(rnd)), Arrays.toString(LoginCrypt.newSessionKey(rnd)));
	}
}
