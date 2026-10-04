package com.lopez.l2j.network.game.packet;

import com.lopez.l2j.network.login.packet.PacketReader;
import java.util.Optional;

/**
 * Pacotes do cliente Interlude para o game server, decodificados por estado (porta do L2GamePacketHandler).
 * Opcodes desconhecidos/fora de estado viram {@link Unknown} (o legado apenas os ignora).
 */
public sealed interface GameClientPacket {

	enum State {
		CONNECTED, AUTHED, IN_GAME
	}

	record ProtocolVersion(int version) implements GameClientPacket {
	}

	record AuthLogin(String account, int playOk2, int playOk1, int loginOk1, int loginOk2) implements GameClientPacket {
	}

	record NewCharacter() implements GameClientPacket {
	}

	record CharacterCreate(String name, int race, int sex, int classId, int intel, int str, int con, int men,
			int dex, int wit, int hairStyle, int hairColor, int face) implements GameClientPacket {
	}

	record CharacterDelete(int slot) implements GameClientPacket {
	}

	record CharacterRestore(int slot) implements GameClientPacket {
	}

	record CharacterSelect(int slot) implements GameClientPacket {
	}

	record RequestManorList() implements GameClientPacket {
	}

	record EnterWorld() implements GameClientPacket {
	}

	record MoveBackwardToLocation(int targetX, int targetY, int targetZ, int originX, int originY, int originZ)
			implements GameClientPacket {
	}

	record ValidatePosition(int x, int y, int z, int heading) implements GameClientPacket {
	}

	record Say2(String text, int channel, String target) implements GameClientPacket {
	}

	record Action(int objectId, int originX, int originY, int originZ, int shift) implements GameClientPacket {
	}

	record RequestTargetCancel() implements GameClientPacket {
	}

	record RequestActionUse(int actionId, boolean ctrl, boolean shift) implements GameClientPacket {
	}

	record RequestItemList() implements GameClientPacket {
	}

	record UseItem(int objectId) implements GameClientPacket {
	}

	/** {@code bodyPart} = mascara L2Item.SLOT_* do slot clicado. */
	record RequestUnEquipItem(int bodyPart) implements GameClientPacket {
	}

	record RequestSkillList() implements GameClientPacket {
	}

	record RequestQuestList() implements GameClientPacket {
	}

	record Logout() implements GameClientPacket {
	}

	record RequestRestart() implements GameClientPacket {
	}

	record Unknown(int opcode, int subOpcode) implements GameClientPacket {
	}

	/** Pacote com tamanho/campos invalidos devolve vazio (a sessao deve fechar). */
	static Optional<GameClientPacket> decode(State state, byte[] body) {
		try {
			PacketReader r = new PacketReader(body);
			int op = r.readC();
			return Optional.of(switch (state) {
				case CONNECTED -> switch (op) {
					case 0x00 -> new ProtocolVersion(r.readD());
					case 0x08 -> new AuthLogin(r.readS().toLowerCase(java.util.Locale.ROOT), r.readD(), r.readD(),
							r.readD(), r.readD());
					default -> new Unknown(op, -1);
				};
				case AUTHED -> switch (op) {
					case 0x0e -> new NewCharacter();
					case 0x0b -> new CharacterCreate(r.readS(), r.readD(), r.readD(), r.readD(), r.readD(),
							r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD());
					case 0x0c -> new CharacterDelete(r.readD());
					case 0x62 -> new CharacterRestore(r.readD());
					case 0x0d -> new CharacterSelect(r.readD());
					case 0x09 -> new Logout();
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
				case IN_GAME -> switch (op) {
					case 0x03 -> new EnterWorld();
					case 0x01 -> new MoveBackwardToLocation(r.readD(), r.readD(), r.readD(), r.readD(), r.readD(),
							r.readD());
					case 0x48 -> new ValidatePosition(r.readD(), r.readD(), r.readD(), r.readD());
					case 0x38 -> say2(r);
					case 0x04 -> new Action(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x37 -> new RequestTargetCancel();
					case 0x45 -> new RequestActionUse(r.readD(), r.readD() == 1, r.readC() == 1);
					case 0x0f -> new RequestItemList();
					case 0x14 -> new UseItem(r.readD());
					case 0x11 -> new RequestUnEquipItem(r.readD());
					case 0x3f -> new RequestSkillList();
					case 0x63 -> new RequestQuestList();
					case 0x09 -> new Logout();
					case 0x46 -> new RequestRestart();
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
			});
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static GameClientPacket extended(PacketReader r) {
		if (r.remaining() < 2) {
			return new Unknown(0xd0, -1);
		}
		int sub = r.readH();
		return sub == 0x08 ? new RequestManorList() : new Unknown(0xd0, sub);
	}

	private static Say2 say2(PacketReader r) {
		String text = r.readS();
		int channel = r.readD();
		String target = channel == 2 && r.remaining() >= 2 ? r.readS() : null;
		return new Say2(text, channel, target);
	}
}
