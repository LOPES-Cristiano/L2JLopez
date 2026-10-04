package com.lopez.l2j.network.login.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RsaLoginTest {

	static ScrambledRsaKeyPair pair;

	@BeforeAll
	static void generate() {
		pair = ScrambledRsaKeyPair.generate(new SecureRandom());
	}

	/** O que o cliente faz: monta o bloco de 128 bytes e cifra com a chave publica (sem padding). */
	private static byte[] clientBlock(String user, String password) throws Exception {
		byte[] plain = new byte[128];
		plain[0] = 0; // garante valor menor que o modulo
		byte[] u = user.getBytes(StandardCharsets.ISO_8859_1);
		byte[] p = password.getBytes(StandardCharsets.ISO_8859_1);
		System.arraycopy(u, 0, plain, 0x5E, Math.min(14, u.length));
		System.arraycopy(p, 0, plain, 0x6C, Math.min(16, p.length));
		Cipher rsa = Cipher.getInstance("RSA/ECB/NoPadding");
		rsa.init(Cipher.ENCRYPT_MODE, pair.publicKey());
		return rsa.doFinal(plain);
	}

	@Test
	void keyIs1024BitWithStandardExponent() {
		assertEquals(1024, pair.publicKey().getModulus().bitLength());
		assertEquals(BigInteger.valueOf(65537), pair.publicKey().getPublicExponent());
	}

	@Test
	void scrambledModulusIsUnscrambledByTheClientToTheRealModulus() {
		byte[] scrambled = pair.scrambledModulus();
		assertEquals(128, scrambled.length);
		byte[] real = ScrambledRsaKeyPair.unscramble(scrambled);
		assertEquals(pair.publicKey().getModulus(), new BigInteger(1, real));
	}

	@Test
	void scrambledModulusDiffersFromRealOne() {
		byte[] real = pair.publicKey().getModulus().toByteArray();
		byte[] scrambled = pair.scrambledModulus();
		assertFalse(java.util.Arrays.equals(java.util.Arrays.copyOfRange(real, real.length - 128, real.length), scrambled));
	}

	@Test
	void scrambledModulusIsDefensivelyCopied() {
		byte[] a = pair.scrambledModulus();
		a[0] ^= 0x7f;
		assertFalse(java.util.Arrays.equals(a, pair.scrambledModulus()));
	}

	@Test
	void decodesUserAndPasswordFromClientBlock() throws Exception {
		LoginCredentials.Credentials c = LoginCredentials.decode(clientBlock("Admin", "s3cret"), pair.privateKey()).orElseThrow();
		assertEquals("admin", c.user()); // usuario vira minusculo, como no legado
		assertEquals("s3cret", c.password());
	}

	@Test
	void passwordIsNeverPrintedByToString() throws Exception {
		LoginCredentials.Credentials c = LoginCredentials.decode(clientBlock("bob", "hunter2"), pair.privateKey()).orElseThrow();
		assertFalse(c.toString().contains("hunter2"));
		assertTrue(c.toString().contains("bob"));
	}

	@Test
	void rejectsWrongLengthBlocks() {
		assertTrue(LoginCredentials.decode(new byte[127], pair.privateKey()).isEmpty());
		assertTrue(LoginCredentials.decode(null, pair.privateKey()).isEmpty());
	}

	@Test
	void rejectsBlockWithoutUser() throws Exception {
		assertTrue(LoginCredentials.decode(clientBlock("", "x"), pair.privateKey()).isEmpty());
	}

	@Test
	void truncatesOverlongFieldsInsteadOfFailing() throws Exception {
		LoginCredentials.Credentials c = LoginCredentials
				.decode(clientBlock("a-very-long-user-name", "p"), pair.privateKey()).orElseThrow();
		assertEquals("a-very-long-us", c.user());
	}

	@Test
	void legacyPasswordHashMatchesKnownSha1Base64() {
		assertEquals("0DPiKuNIrrVmD8IUCuw1hQxNqZc=", LegacyPasswordHasher.hash("admin"));
		assertTrue(LegacyPasswordHasher.matches("admin", "0DPiKuNIrrVmD8IUCuw1hQxNqZc="));
		assertFalse(LegacyPasswordHasher.matches("Admin", "0DPiKuNIrrVmD8IUCuw1hQxNqZc="));
		assertFalse(LegacyPasswordHasher.matches(null, "x"));
		assertFalse(LegacyPasswordHasher.matches("x", null));
	}
}
