package com.lopez.l2j.network.login.packet;

import java.util.Arrays;

/** Leitor little-endian com checagem de limites (nunca le alem do pacote). */
public final class PacketReader {

	private final byte[] data;
	private int pos;

	public PacketReader(byte[] data) {
		this.data = data;
	}

	public int remaining() {
		return data.length - pos;
	}

	private void require(int n) {
		if (remaining() < n) {
			throw new IllegalArgumentException("Pacote truncado: faltam " + (n - remaining()) + " bytes");
		}
	}

	public int readC() {
		require(1);
		return data[pos++] & 0xff;
	}

	public int readD() {
		require(4);
		int v = (data[pos] & 0xff) | (data[pos + 1] & 0xff) << 8 | (data[pos + 2] & 0xff) << 16
				| (data[pos + 3] & 0xff) << 24;
		pos += 4;
		return v;
	}

	public int readH() {
		require(2);
		int v = (data[pos] & 0xff) | (data[pos + 1] & 0xff) << 8;
		pos += 2;
		return v;
	}

	/** String UTF-16LE terminada por 0x0000; se o terminador faltar, le ate o fim do pacote. */
	public String readS() {
		StringBuilder sb = new StringBuilder();
		while (remaining() >= 2) {
			int c = readH();
			if (c == 0) {
				break;
			}
			sb.append((char) c);
		}
		return sb.toString();
	}

	public byte[] readB(int n) {
		require(n);
		byte[] b = Arrays.copyOfRange(data, pos, pos + n);
		pos += n;
		return b;
	}
}
