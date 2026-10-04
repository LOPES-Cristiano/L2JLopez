package com.lopez.l2j.network.login.crypt;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;

/**
 * Par RSA de 1024 bits do login server + o modulo "embaralhado" que o Init envia ao cliente
 * (o cliente desfaz o embaralhamento antes de usar a chave publica).
 */
public final class ScrambledRsaKeyPair {

	private static final int MODULUS_BYTES = 0x80;

	private final KeyPair pair;
	private final byte[] scrambledModulus;

	public ScrambledRsaKeyPair(KeyPair pair) {
		this.pair = pair;
		this.scrambledModulus = scramble(((RSAPublicKey) pair.getPublic()).getModulus());
	}

	public static ScrambledRsaKeyPair generate(SecureRandom random) {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4), random);
			return new ScrambledRsaKeyPair(gen.generateKeyPair());
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("RSA indisponivel nesta JVM", e);
		}
	}

	public RSAPrivateKey privateKey() {
		return (RSAPrivateKey) pair.getPrivate();
	}

	public RSAPublicKey publicKey() {
		return (RSAPublicKey) pair.getPublic();
	}

	/** Copia defensiva: o chamador nao pode alterar o modulo que sera enviado a todos os clientes. */
	public byte[] scrambledModulus() {
		return scrambledModulus.clone();
	}

	static byte[] scramble(BigInteger modulus) {
		byte[] m = modulus.toByteArray();
		if (m.length == MODULUS_BYTES + 1 && m[0] == 0) {
			byte[] trimmed = new byte[MODULUS_BYTES];
			System.arraycopy(m, 1, trimmed, 0, MODULUS_BYTES);
			m = trimmed;
		}
		if (m.length != MODULUS_BYTES) {
			throw new IllegalArgumentException("Modulo RSA deve ter 128 bytes, tem " + m.length);
		}
		for (int i = 0; i < 4; i++) {
			byte t = m[i];
			m[i] = m[0x4d + i];
			m[0x4d + i] = t;
		}
		for (int i = 0; i < 0x40; i++) {
			m[i] = (byte) (m[i] ^ m[0x40 + i]);
		}
		for (int i = 0; i < 4; i++) {
			m[0x0d + i] = (byte) (m[0x0d + i] ^ m[0x34 + i]);
		}
		for (int i = 0; i < 0x40; i++) {
			m[0x40 + i] = (byte) (m[0x40 + i] ^ m[i]);
		}
		return m;
	}

	/** Inverso de {@link #scramble}: o que o cliente faz. Usado nos testes. */
	static byte[] unscramble(byte[] scrambled) {
		byte[] m = scrambled.clone();
		for (int i = 0; i < 0x40; i++) {
			m[0x40 + i] = (byte) (m[0x40 + i] ^ m[i]);
		}
		for (int i = 0; i < 4; i++) {
			m[0x0d + i] = (byte) (m[0x0d + i] ^ m[0x34 + i]);
		}
		for (int i = 0; i < 0x40; i++) {
			m[i] = (byte) (m[i] ^ m[0x40 + i]);
		}
		for (int i = 0; i < 4; i++) {
			byte t = m[i];
			m[i] = m[0x4d + i];
			m[0x4d + i] = t;
		}
		return m;
	}
}
