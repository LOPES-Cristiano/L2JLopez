package com.lopez.l2j.network.login.packet;

import java.util.Optional;

/** Pacotes recebidos do cliente; o opcode valido depende do estado da sessao. */
public sealed interface LoginClientPacket {

	enum State {
		CONNECTED, AUTHED_GG, AUTHED_LOGIN
	}

	record AuthGameGuard(int sessionId) implements LoginClientPacket {
	}

	/** Bloco RSA de 128 bytes com usuario e senha; decifrado pela sessao. */
	record RequestAuthLogin(byte[] rsaBlock) implements LoginClientPacket {
	}

	record RequestServerList(int loginOk1, int loginOk2) implements LoginClientPacket {
	}

	record RequestServerLogin(int loginOk1, int loginOk2, int serverId) implements LoginClientPacket {
	}

	/**
	 * @return vazio para opcode invalido no estado atual ou pacote truncado (o chamador fecha a conexao).
	 */
	static Optional<LoginClientPacket> decode(State state, byte[] body) {
		if (body == null || body.length == 0) {
			return Optional.empty();
		}
		PacketReader r = new PacketReader(body);
		int opcode = r.readC();
		try {
			return switch (state) {
				case CONNECTED -> opcode == 0x07 ? Optional.of(readGameGuard(r)) : Optional.empty();
				case AUTHED_GG -> opcode == 0x00 && r.remaining() >= 128
						? Optional.of(new RequestAuthLogin(r.readB(128)))
						: Optional.empty();
				case AUTHED_LOGIN -> switch (opcode) {
					case 0x05, 0x06 -> Optional.of(new RequestServerList(r.readD(), r.readD()));
					case 0x02 -> Optional.of(new RequestServerLogin(r.readD(), r.readD(), r.readC()));
					default -> Optional.empty();
				};
			};
		} catch (IllegalArgumentException truncated) {
			return Optional.empty();
		}
	}

	private static AuthGameGuard readGameGuard(PacketReader r) {
		int sessionId = r.readD();
		r.readD();
		r.readD();
		r.readD();
		r.readD(); // data1..4: ignorados
		return new AuthGameGuard(sessionId);
	}
}
