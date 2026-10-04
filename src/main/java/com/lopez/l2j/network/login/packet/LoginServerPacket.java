package com.lopez.l2j.network.login.packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Pacotes enviados pelo login server ao cliente (opcodes do protocolo Interlude). */
public sealed interface LoginServerPacket {

	byte[] encode();

	enum AuthFailReason {
		SYSTEM_ERROR(0x01), PASS_WRONG(0x02), USER_OR_PASS_WRONG(0x03), ACCESS_FAILED(0x04), ACCOUNT_IN_USE(0x07),
		ACCOUNT_BANNED(0x09), SERVER_OVERLOADED(0x0f), IGNORE(0x17), INVALID_SECURITY_CARD_NO(0x1f), DUAL_BOX(0x23);

		public final int code;

		AuthFailReason(int code) {
			this.code = code;
		}
	}

	enum PlayFailReason {
		SYSTEM_ERROR(0x01), USER_OR_PASS_WRONG(0x02), REASON3(0x03), REASON4(0x04), TOO_MANY_PLAYERS(0x0f);

		public final int code;

		PlayFailReason(int code) {
			this.code = code;
		}
	}

	/** Opcode 0x00: abre a sessao e entrega o modulo RSA embaralhado e a chave Blowfish. */
	record Init(int sessionId, byte[] scrambledModulus, byte[] blowfishKey) implements LoginServerPacket {
		public Init {
			if (scrambledModulus.length != 128 || blowfishKey.length != 16) {
				throw new IllegalArgumentException("Init exige modulo de 128 bytes e chave de 16 bytes");
			}
			scrambledModulus = scrambledModulus.clone();
			blowfishKey = blowfishKey.clone();
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x00).writeD(sessionId).writeD(0x0000c621).writeB(scrambledModulus)
					.writeD(0x29DD954E).writeD(0x77C39CFC).writeD(0x97ADB620).writeD(0x07BDE0F7)
					.writeB(blowfishKey).writeC(0x00).toByteArray();
		}
	}

	/** Opcode 0x0b: resposta ao GameGuard (aceita sempre, como no legado). */
	record GgAuth(int sessionId) implements LoginServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x0b).writeD(sessionId).writeD(0).writeD(0).writeD(0).writeD(0)
					.toByteArray();
		}
	}

	/** Opcode 0x03: login aceito; carrega a primeira metade da chave de sessao. */
	record AuthOk(int loginOk1, int loginOk2) implements LoginServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x03).writeD(loginOk1).writeD(loginOk2).writeD(0).writeD(0)
					.writeD(0x000003ea).writeD(0).writeD(0).writeD(0).writeB(new byte[16]).toByteArray();
		}
	}

	/** Opcode 0x01. */
	record AuthFail(AuthFailReason reason) implements LoginServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x01).writeC(reason.code).toByteArray();
		}
	}

	/** Opcode 0x07: segunda metade da chave, usada pelo game server para validar o jogador. */
	record PlayOk(int playOk1, int playOk2) implements LoginServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x07).writeD(playOk1).writeD(playOk2).toByteArray();
		}
	}

	/** Opcode 0x06. */
	record PlayFail(PlayFailReason reason) implements LoginServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x06).writeC(reason.code).toByteArray();
		}
	}

	/** Entrada da lista de servidores. */
	record ServerEntry(int id, String host, int port, boolean pvp, int currentPlayers, int maxPlayers, boolean up,
			boolean testServer, boolean clock, boolean brackets) {
	}

	/** Opcode 0x04. */
	record ServerList(List<ServerEntry> servers, int lastServerId) implements LoginServerPacket {
		public ServerList {
			servers = List.copyOf(servers);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x04).writeC(servers.size());
			boolean lastIsUp = servers.stream().anyMatch(s -> s.id() == lastServerId && s.up());
			w.writeC(lastIsUp ? lastServerId : 0);
			for (ServerEntry s : servers) {
				w.writeC(s.id());
				w.writeB(ipv4(s.host()));
				w.writeD(s.port());
				w.writeC(0x00); // limite de idade
				w.writeC(s.pvp() ? 1 : 0);
				w.writeH(s.currentPlayers());
				w.writeH(s.maxPlayers());
				w.writeC(s.up() ? 1 : 0);
				int bits = (s.testServer() ? 0x04 : 0) | (s.clock() ? 0x02 : 0);
				w.writeD(bits);
				w.writeC(s.brackets() ? 1 : 0);
			}
			return w.toByteArray();
		}

		private static byte[] ipv4(String host) {
			try {
				byte[] raw = InetAddress.getByName(host).getAddress();
				if (raw.length == 4) {
					return raw;
				}
			} catch (UnknownHostException e) {
				// cai no loopback, como o legado
			}
			return new byte[] { 127, 0, 0, 1 };
		}
	}
}
