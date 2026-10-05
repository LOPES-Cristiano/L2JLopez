package com.lopez.l2j.network.game.packet;

import com.lopez.l2j.network.login.packet.PacketReader;
import java.util.List;
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

	record MoveBackwardToLocation(int targetX, int targetY, int targetZ, int originX, int originY, int originZ,
			int moveMovement) implements GameClientPacket {
		public MoveBackwardToLocation(int targetX, int targetY, int targetZ, int originX, int originY, int originZ) {
			this(targetX, targetY, targetZ, originX, originY, originZ, 1);
		}
	}

	record AttackRequest(int objectId, int originX, int originY, int originZ, int attackId)
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

	record RequestSocialAction(int actionId) implements GameClientPacket {
	}

	record RequestWithDrawalParty() implements GameClientPacket {
	}

	record RequestOustPartyMember(String name) implements GameClientPacket {
	}

	record RequestShowMiniMap() implements GameClientPacket {
	}

	record RequestRestartPoint(int requestedPointType) implements GameClientPacket {
	}

	record RequestUserCommand(int commandId) implements GameClientPacket {
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

	record RequestBypassToServer(String command) implements GameClientPacket {
	}

	/** 0x5b - comando digitado na caixa de chat com prefixo // (ex.: //admin). */
	record SendBypassBuildCmd(String command) implements GameClientPacket {
	}

	record ItemRequest(int itemId, int count) {
	}

	record RequestBuyItem(int listId, java.util.List<ItemRequest> items) implements GameClientPacket {
		public RequestBuyItem {
			items = java.util.List.copyOf(items);
		}
	}

	record SellItemRequest(int objectId, int itemId, int count) {
	}

	record RequestSellItem(int listId, java.util.List<SellItemRequest> items) implements GameClientPacket {
		public RequestSellItem {
			items = java.util.List.copyOf(items);
		}
	}

	record RequestShortCutReg(int type, int slot, int id, int characterType) implements GameClientPacket {
	}

	record RequestShortCutDel(int id) implements GameClientPacket {
	}

	record RequestMagicSkillUse(int magicId, boolean ctrlPressed, boolean shiftPressed) implements GameClientPacket {
	}

	/** 0xD0:0x05 - liga (type 1) ou desliga (type 0) o uso automatico de um soulshot/spiritshot. */
	record RequestAutoSoulShot(int itemId, int type) implements GameClientPacket {
	}

	/** 0x6b - detalhes (custo/livros) de um skill da janela do treinador. */
	record RequestAcquireSkillInfo(int skillId, int level, int skillType) implements GameClientPacket {
	}

	/** 0x6c - aprender o skill selecionado na janela do treinador. */
	record RequestAcquireSkill(int skillId, int level, int skillType) implements GameClientPacket {
	}

	/** 0xD0:0x06 - detalhes de encantamento do skill. */
	record RequestExEnchantSkillInfo(int skillId, int level) implements GameClientPacket {
	}

	/** 0xD0:0x07 - executa o encantamento do skill. */
	record RequestExEnchantSkill(int skillId, int level) implements GameClientPacket {
	}

	/** 0x29 - convite para grupo (party). */
	record RequestJoinParty(String name, int itemDistribution) implements GameClientPacket {
	}

	/** 0x2a - resposta ao convite de grupo (1 = aceitar, 0 = recusar). */
	record RequestAnswerJoinParty(int response) implements GameClientPacket {
	}

	/** 0xa7 - selecao e compra de item em janela MultiSell. */
	record MultiSellChoose(int listId, int entryId, int amount) implements GameClientPacket {
	}

	/** 0x58 - aplicacao de enchant scroll no item selecionado. */
	record RequestEnchantItem(int objectId) implements GameClientPacket {
	}

	record WareHouseItemRequest(int objectId, int count) {
	}

	/** 0x31 - lista de itens a depositar no armazem. */
	record SendWareHouseDepositList(List<WareHouseItemRequest> items) implements GameClientPacket {
	}

	/** 0x32 - lista de itens a retirar do armazem. */
	record SendWareHouseWithDrawList(List<WareHouseItemRequest> items) implements GameClientPacket {
	}

	/** 0x59 - destruicao de item arrastado para a lixeira. */
	record RequestDestroyItem(int objectId, int count) implements GameClientPacket {
	}

	/** 0x6e - comando de GM (Alt+G, etc). */
	record RequestGMCommand(String targetName, int command) implements GameClientPacket {
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
					case 0x01 -> {
						int tx = r.readD(), ty = r.readD(), tz = r.readD();
						int ox = r.readD(), oy = r.readD(), oz = r.readD();
						int mm = r.remaining() >= 4 ? r.readD() : 1;
						yield new MoveBackwardToLocation(tx, ty, tz, ox, oy, oz, mm);
					}
					case 0x0a -> new AttackRequest(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x48 -> new ValidatePosition(r.readD(), r.readD(), r.readD(), r.readD());
					case 0x38 -> say2(r);
					case 0x04 -> new Action(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x21 -> new RequestBypassToServer(r.readS());
					case 0x1b -> new RequestSocialAction(r.readD());
					case 0x1e -> readSellItem(r);
					case 0x1f -> readBuyItem(r);
					case 0x29 -> new RequestJoinParty(r.readS(), r.readD());
					case 0x2a -> new RequestAnswerJoinParty(r.readD());
					case 0x2b -> new RequestWithDrawalParty();
					case 0x2c -> new RequestOustPartyMember(r.readS());
					case 0x2f -> new RequestMagicSkillUse(r.readD(), r.readD() != 0, r.readC() != 0);
					case 0x33 -> new RequestShortCutReg(r.readD(), r.readD(), r.readD(), r.readD());
					case 0x35 -> new RequestShortCutDel(r.readD());
					case 0x37 -> new RequestTargetCancel();
					case 0x45 -> new RequestActionUse(r.readD(), r.readD() == 1, r.readC() == 1);
					case 0x0f -> new RequestItemList();
					case 0x14 -> new UseItem(r.readD());
					case 0x11 -> new RequestUnEquipItem(r.readD());
					case 0x31 -> readWareHouseList(r, true);
					case 0x32 -> readWareHouseList(r, false);
					case 0x3f -> new RequestSkillList();
					case 0x58 -> r.remaining() >= 4 ? new RequestEnchantItem(r.readD()) : new Unknown(op, -1);
					case 0x59 -> r.remaining() >= 8 ? new RequestDestroyItem(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x5b -> new SendBypassBuildCmd(r.readS());
					case 0x6b -> new RequestAcquireSkillInfo(r.readD(), r.readD(), r.readD());
					case 0x6c -> new RequestAcquireSkill(r.readD(), r.readD(), r.readD());
					case 0x6d -> new RequestRestartPoint(r.readD());
					case 0x6e -> {
						String target = r.readS();
						int cmd = r.remaining() >= 4 ? r.readD() : 1;
						yield new RequestGMCommand(target, cmd);
					}
					case 0x63 -> new RequestQuestList();
					case 0x09 -> new Logout();
					case 0x46 -> new RequestRestart();
					case 0xaa -> new RequestUserCommand(r.readD());
					case 0xa7 -> readMultiSellChoose(r);
					case 0xcd -> new RequestShowMiniMap();
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
			});
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static MultiSellChoose readMultiSellChoose(PacketReader r) {
		if (r.remaining() < 12) {
			return new MultiSellChoose(0, 0, 0);
		}
		int listId = r.readD();
		int entryId = r.readD();
		int amount = r.readD();
		int realEntryId = entryId >= 100000 ? entryId / 100000 : entryId;
		return new MultiSellChoose(listId, realEntryId, Math.max(1, amount));
	}

	private static RequestBuyItem readBuyItem(PacketReader r) {
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return new RequestBuyItem(listId, java.util.List.of());
		}
		java.util.List<ItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new ItemRequest(r.readD(), r.readD()));
		}
		return new RequestBuyItem(listId, items);
	}

	private static RequestSellItem readSellItem(PacketReader r) {
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new RequestSellItem(listId, java.util.List.of());
		}
		java.util.List<SellItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new SellItemRequest(r.readD(), r.readD(), r.readD()));
		}
		return new RequestSellItem(listId, items);
	}

	private static GameClientPacket readWareHouseList(PacketReader r, boolean deposit) {
		if (r.remaining() < 4) {
			return deposit ? new SendWareHouseDepositList(java.util.List.of()) : new SendWareHouseWithDrawList(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return deposit ? new SendWareHouseDepositList(java.util.List.of()) : new SendWareHouseWithDrawList(java.util.List.of());
		}
		java.util.List<WareHouseItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new WareHouseItemRequest(r.readD(), r.readD()));
		}
		return deposit ? new SendWareHouseDepositList(items) : new SendWareHouseWithDrawList(items);
	}

	private static GameClientPacket extended(PacketReader r) {
		if (r.remaining() < 2) {
			return new Unknown(0xd0, -1);
		}
		int sub = r.readH();
		return switch (sub) {
			case 0x08 -> new RequestManorList();
			case 0x05 -> r.remaining() >= 8 ? new RequestAutoSoulShot(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x06 -> r.remaining() >= 8 ? new RequestExEnchantSkillInfo(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x07 -> r.remaining() >= 8 ? new RequestExEnchantSkill(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			default -> new Unknown(0xd0, sub);
		};
	}

	private static Say2 say2(PacketReader r) {
		String text = r.readS();
		int channel = r.readD();
		String target = channel == 2 && r.remaining() >= 2 ? r.readS() : null;
		return new Say2(text, channel, target);
	}
}
