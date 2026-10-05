package com.lopez.l2j.network.login.packet;

import java.io.ByteArrayOutputStream;

/** Escritor little-endian do protocolo L2 (C=byte, H=short, D=int, B=bytes). */
public final class PacketWriter {

	private final ByteArrayOutputStream out;

	public PacketWriter() {
		this(64);
	}

	public PacketWriter(int initialCapacity) {
		this.out = new ByteArrayOutputStream(initialCapacity);
	}

	public PacketWriter writeC(int v) {
		out.write(v & 0xff);
		return this;
	}

	public PacketWriter writeH(int v) {
		out.write(v & 0xff);
		out.write((v >> 8) & 0xff);
		return this;
	}

	public PacketWriter writeD(int v) {
		out.write(v & 0xff);
		out.write((v >> 8) & 0xff);
		out.write((v >> 16) & 0xff);
		out.write((v >> 24) & 0xff);
		return this;
	}

	public PacketWriter writeQ(long v) {
		writeD((int) v);
		writeD((int) (v >>> 32));
		return this;
	}

	/** Double IEEE-754 little-endian ("F" no protocolo). */
	public PacketWriter writeF(double v) {
		return writeQ(Double.doubleToRawLongBits(v));
	}

	/** String UTF-16LE terminada por 0x0000 ("S" no protocolo); null vira string vazia. */
	public PacketWriter writeS(String s) {
		if (s != null) {
			for (int i = 0; i < s.length(); i++) {
				writeH(s.charAt(i));
			}
		}
		return writeH(0);
	}

	public PacketWriter writeB(byte[] bytes) {
		out.write(bytes, 0, bytes.length);
		return this;
	}

	public byte[] toByteArray() {
		return out.toByteArray();
	}
}
