package com.lopez.l2j.network.login.crypt;

/**
 * Checksum XOR de 32 bits e "XOR pass" inicial do protocolo de login L2. Todas as palavras sao
 * little-endian; os intervalos sao relativos a {@code offset} (o MMO costuma passar offset 2,
 * depois do cabecalho de tamanho).
 */
public final class LoginChecksum {

	private LoginChecksum() {
	}

	private static int readInt(byte[] raw, int i) {
		return (raw[i] & 0xff) | (raw[i + 1] & 0xff) << 8 | (raw[i + 2] & 0xff) << 16 | (raw[i + 3] & 0xff) << 24;
	}

	private static void writeInt(byte[] raw, int i, int v) {
		raw[i] = (byte) v;
		raw[i + 1] = (byte) (v >> 8);
		raw[i + 2] = (byte) (v >> 16);
		raw[i + 3] = (byte) (v >> 24);
	}

	/** Os ultimos 4 bytes do intervalo devem ser o XOR de todas as palavras anteriores. */
	public static boolean verify(byte[] raw, int offset, int size) {
		if ((size & 3) != 0 || size <= 4) {
			return false;
		}
		int end = offset + size - 4;
		int checksum = 0;
		for (int i = offset; i < end; i += 4) {
			checksum ^= readInt(raw, i);
		}
		return readInt(raw, end) == checksum;
	}

	/** Grava nos ultimos 4 bytes do intervalo o XOR de todas as palavras anteriores. */
	public static void append(byte[] raw, int offset, int size) {
		if ((size & 3) != 0 || size < 8) {
			throw new IllegalArgumentException("Tamanho invalido para checksum: " + size);
		}
		int end = offset + size - 4;
		int checksum = 0;
		for (int i = offset; i < end; i += 4) {
			checksum ^= readInt(raw, i);
		}
		writeInt(raw, end, checksum);
	}

	/**
	 * Primeiro pacote (Init): embaralha o corpo com uma chave XOR acumulativa e grava a chave nos
	 * ultimos 4 bytes. Os 4 primeiros bytes (offset) e os 8 finais ficam reservados.
	 */
	public static void encXorPass(byte[] raw, int offset, int size, int key) {
		int stop = offset + size - 8;
		int pos = offset + 4;
		int ecx = key;
		while (pos < stop) {
			int edx = readInt(raw, pos);
			ecx += edx;
			edx ^= ecx;
			writeInt(raw, pos, edx);
			pos += 4;
		}
		writeInt(raw, pos, ecx);
	}

	/** Inverso de {@link #encXorPass}; o cliente real faz isto ao receber o Init. */
	public static void decXorPass(byte[] raw, int offset, int size) {
		int stop = offset + size - 8;
		int ecx = readInt(raw, stop);
		// encXorPass avanca ate pos==stop e grava ecx ali; desfazemos de tras para frente.
		for (int p = stop - 4; p >= offset + 4; p -= 4) {
			int edx = readInt(raw, p);
			edx ^= ecx;
			ecx -= edx;
			writeInt(raw, p, edx);
		}
	}
}
