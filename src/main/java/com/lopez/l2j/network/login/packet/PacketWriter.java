package com.lopez.l2j.network.login.packet;

import java.io.ByteArrayOutputStream;

/** Escritor little-endian do protocolo L2 (C=byte, H=short, D=int, B=bytes). */
public final class PacketWriter {

	private final ByteArrayOutputStream out = new ByteArrayOutputStream(64);

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

	public PacketWriter writeB(byte[] bytes) {
		out.write(bytes, 0, bytes.length);
		return this;
	}

	public byte[] toByteArray() {
		return out.toByteArray();
	}
}
