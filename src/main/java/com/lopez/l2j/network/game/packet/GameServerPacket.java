package com.lopez.l2j.network.game.packet;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.Paperdoll;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.network.login.packet.PacketWriter;
import java.util.Collection;
import java.util.List;

/**
 * Pacotes enviados pelo game server ao cliente Interlude. Layouts portados 1:1 de
 * com.dream.game.network.serverpackets; campos de sistemas ainda nao migrados (inventario, clan, skills,
 * cubics...) saem zerados, como o legado faria para um personagem novo sem esses dados.
 */
public sealed interface GameServerPacket {

	byte[] encode();

	/** 0x00 KeyPacket: resposta ao ProtocolVersion; entrega a chave e liga a cifra. */
	record KeyPacket(byte[] key, boolean protocolOk) implements GameServerPacket {
		public KeyPacket {
			key = key.clone();
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x00).writeC(protocolOk ? 0x01 : 0x00).writeB(key).writeD(0x01)
					.writeC(0x01).toByteArray();
		}
	}

	/** 0x13 CharSelectionInfo: lista de personagens da conta (paperdolls paralelos a characters). */
	record CharSelectionInfo(String account, int sessionId, List<PlayerCharacter> characters, List<Paperdoll> paperdolls,
			int activeIndex) implements GameServerPacket {
		public CharSelectionInfo {
			characters = List.copyOf(characters);
			paperdolls = List.copyOf(paperdolls);
			if (paperdolls.size() != characters.size()) {
				throw new IllegalArgumentException("um paperdoll por personagem");
			}
		}

		/** Ativo = o de lastAccess mais recente, como no legado. */
		public static CharSelectionInfo of(String account, int sessionId, List<PlayerCharacter> characters,
				List<Paperdoll> paperdolls) {
			int active = -1;
			long last = 0;
			for (int i = 0; i < characters.size(); i++) {
				if (characters.get(i).lastAccess() > last) {
					last = characters.get(i).lastAccess();
					active = i;
				}
			}
			return new CharSelectionInfo(account, sessionId, characters, paperdolls, active);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x13).writeD(characters.size());
			long now = System.currentTimeMillis();
			for (int i = 0; i < characters.size(); i++) {
				PlayerCharacter c = characters.get(i);
				w.writeS(c.name()).writeD(c.objectId()).writeS(account).writeD(sessionId).writeD(c.clanId())
						.writeD(0x00);
				w.writeD(c.female() ? 1 : 0).writeD(c.race()).writeD(c.baseClassId()).writeD(0x01);
				w.writeD(0x00).writeD(0x00).writeD(0x00);
				w.writeF(c.currentHp()).writeF(c.currentMp());
				w.writeD(c.sp()).writeQ(c.exp()).writeD(c.level()).writeD(c.karma());
				for (int k = 0; k < 9; k++) {
					w.writeD(0x00);
				}
				writePaperdoll(w, paperdolls.get(i), ItemSlots.LRHAND);
				w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face());
				w.writeF(c.maxHp()).writeF(c.maxMp());
				int deleteSeconds = c.deleteTime() > 0 ? (int) Math.max(0, (c.deleteTime() - now) / 1000) : 0;
				w.writeD(deleteSeconds).writeD(c.classId()).writeD(i == activeIndex ? 0x01 : 0x00);
				w.writeC(0x00).writeD(0x00); // enchant effect, augmentation
			}
			return w.toByteArray();
		}
	}

	/**
	 * 17 object ids + 17 item ids na ordem {@link ItemSlots#VISIBLE_ORDER}; o 15o slot usa {@code twoHandSlot}
	 * (UserInfo manda RHAND, CharSelectionInfo manda LRHAND).
	 */
	private static void writePaperdoll(PacketWriter w, Paperdoll p, int twoHandSlot) {
		for (int slot : ItemSlots.VISIBLE_ORDER) {
			w.writeD(p.objectId(slot == ItemSlots.LRHAND ? twoHandSlot : slot));
		}
		for (int slot : ItemSlots.VISIBLE_ORDER) {
			w.writeD(p.itemId(slot == ItemSlots.LRHAND ? twoHandSlot : slot));
		}
	}

	/** 0x17 NewCharacterSuccess (CharTemplates): classes oferecidas na criacao. */
	record NewCharacterSuccess(List<CharTemplate> templates) implements GameServerPacket {
		public NewCharacterSuccess {
			templates = List.copyOf(templates);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x17).writeD(templates.size());
			for (CharTemplate t : templates) {
				w.writeD(t.raceId()).writeD(t.classId());
				for (int stat : new int[] { t.str(), t.dex(), t.con(), t.intel(), t.wit(), t.men() }) {
					w.writeD(0x46).writeD(stat).writeD(0x0a);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x19 CharCreateOk. */
	record CharCreateOk() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x19).writeD(0x01).toByteArray();
		}
	}

	enum CharCreateFailReason {
		CREATION_FAILED(0x00), TOO_MANY_CHARACTERS(0x01), NAME_ALREADY_EXISTS(0x02), ENG_CHARS_16(0x03),
		INCORRECT_NAME(0x04), CREATE_NOT_ALLOWED(0x05), CHOOSE_ANOTHER_SERVER(0x06);

		public final int code;

		CharCreateFailReason(int code) {
			this.code = code;
		}
	}

	/** 0x1a CharCreateFail. */
	record CharCreateFail(CharCreateFailReason reason) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x1a).writeD(reason.code).toByteArray();
		}
	}

	/** 0x23 CharDeleteSuccess. */
	record CharDeleteSuccess() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x23).toByteArray();
		}
	}

	/** 0x24 CharDeleteFail (1 = membro de clan, 2 = lider de clan). */
	record CharDeleteFail(int reason) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x24).writeD(reason).toByteArray();
		}
	}

	/** 0xF8 SSQInfo (ceu dos Seven Signs): 256 = neutro. */
	record SsqInfo(int state) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf8).writeH(256 + state).toByteArray();
		}
	}

	/** 0x15 CharSelected: personagem escolhido; o cliente carrega o mapa e responde EnterWorld. */
	record CharSelected(PlayerCharacter c, CharTemplate t, int sessionId, int gameTime) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x15);
			w.writeS(c.name()).writeD(c.objectId()).writeS(c.title()).writeD(sessionId).writeD(c.clanId())
					.writeD(0x00);
			w.writeD(c.female() ? 1 : 0).writeD(c.race()).writeD(c.classId()).writeD(0x01);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z());
			w.writeF(c.currentHp()).writeF(c.currentMp()).writeD(c.sp()).writeQ(c.exp()).writeD(c.level());
			w.writeD(c.karma()).writeD(c.pkKills());
			w.writeD(t.intel()).writeD(t.str()).writeD(t.con()).writeD(t.men()).writeD(t.dex()).writeD(t.wit());
			for (int i = 0; i < 30; i++) {
				w.writeD(0x00);
			}
			w.writeD(0x00).writeD(0x00);
			w.writeD(gameTime);
			w.writeD(0x00);
			w.writeD(c.classId());
			w.writeD(0x00).writeD(0x00).writeD(0x00).writeD(0x00);
			return w.toByteArray();
		}
	}

	/** 0x04 UserInfo: estado completo do proprio personagem (o cliente so "entra" no mundo apos recebe-lo). */
	record UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad)
			implements GameServerPacket {
		static final int WALK_SPEED = 80;
		static final int INVENTORY_LIMIT = 80;
		static final int NAME_COLOR = 0xFFFFFF;
		static final int TITLE_COLOR = 0xFFFF77;

		public UserInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory().paperdollView(), c.inventory().currentLoad());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x04);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z()).writeD(c.heading()).writeD(c.objectId());
			w.writeS(c.name()).writeD(c.race()).writeD(c.female() ? 1 : 0).writeD(c.classId());
			w.writeD(c.level()).writeQ(c.exp());
			w.writeD(t.str()).writeD(t.dex()).writeD(t.con()).writeD(t.intel()).writeD(t.wit()).writeD(t.men());
			w.writeD(c.maxHp()).writeD((int) c.currentHp()).writeD(c.maxMp()).writeD((int) c.currentMp());
			w.writeD(c.sp()).writeD(currentLoad).writeD(t.maxLoad());
			w.writeD(0x28); // valor fixo do legado (posicao 0x28 do paperdoll)
			writePaperdoll(w, paperdoll, ItemSlots.RHAND);
			for (int i = 0; i < 14; i++) {
				w.writeH(0x00);
			}
			w.writeD(0x00); // augmentation RHAND
			for (int i = 0; i < 12; i++) {
				w.writeH(0x00);
			}
			w.writeD(0x00); // augmentation LRHAND
			for (int i = 0; i < 4; i++) {
				w.writeH(0x00);
			}
			w.writeD(t.pAtk()).writeD(t.pAtkSpd()).writeD(t.pDef()).writeD(t.evasion()).writeD(t.accuracy())
					.writeD(t.critical()).writeD(t.mAtk());
			w.writeD(t.mAtkSpd()).writeD(t.pAtkSpd());
			w.writeD(t.mDef());
			w.writeD(0x00).writeD(c.karma()); // pvp flag, karma
			int run = t.runSpeed();
			w.writeD(run).writeD(WALK_SPEED).writeD(run).writeD(WALK_SPEED).writeD(run).writeD(WALK_SPEED);
			w.writeD(0).writeD(0); // fly speeds
			w.writeF(1.0).writeF(1.0); // move / attack speed multipliers
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));
			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face()).writeD(c.isGm() ? 1 : 0);
			w.writeS(c.title());
			w.writeD(c.clanId()).writeD(0).writeD(0).writeD(0); // clan crest, ally, ally crest
			w.writeD(0); // relation
			w.writeC(0).writeC(0).writeC(t.canCraft() ? 1 : 0); // mount, private store, dwarven craft
			w.writeD(c.pkKills()).writeD(c.pvpKills());
			w.writeH(0); // cubics
			w.writeC(0);
			w.writeD(0); // abnormal effect
			w.writeC(0);
			w.writeD(0); // clan privileges
			w.writeH(0).writeH(0); // recom left/have
			w.writeD(0);
			w.writeH(INVENTORY_LIMIT);
			w.writeD(c.classId()).writeD(0);
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			w.writeC(0); // enchant effect
			w.writeC(0); // team
			w.writeD(0); // large clan crest
			w.writeC(0).writeC(0); // noble, hero
			w.writeC(0).writeD(0).writeD(0).writeD(0); // fishing
			w.writeD(NAME_COLOR);
			w.writeC(c.running() ? 1 : 0);
			w.writeD(0).writeD(0); // pledge class (x2)
			w.writeD(TITLE_COLOR);
			w.writeD(0); // cursed weapon level
			return w.toByteArray();
		}
	}

	/** Linha de item dos pacotes ItemList/InventoryUpdate (foto imutavel do ItemInstance). */
	record ItemInfo(int change, int type1, int objectId, int displayId, int count, int type2, int customType1,
			boolean equipped, int bodyPart, int enchant, int customType2, int augmentation, int mana) {
		public static final int ADDED = 1;
		public static final int MODIFIED = 2;
		public static final int REMOVED = 3;

		public static ItemInfo of(ItemInstance i, int change) {
			var t = i.template();
			return new ItemInfo(change, t.type1(), i.objectId(), t.displayId(), i.count(), t.type2(),
					i.customType1(), i.isEquipped(), t.bodyPart(), i.enchant(), i.customType2(), 0, i.mana());
		}

		void write(PacketWriter w) {
			w.writeH(type1).writeD(objectId).writeD(displayId).writeD(count).writeH(type2).writeH(customType1)
					.writeH(equipped ? 1 : 0).writeD(bodyPart).writeH(enchant).writeH(customType2)
					.writeD(augmentation).writeD(mana);
		}
	}

	/** 0x1b ItemList: inventario completo (o legado limita a 400 linhas). */
	record ItemList(List<ItemInfo> items, boolean showWindow) implements GameServerPacket {
		static final int MAX_ITEMS = 400;

		public ItemList {
			items = List.copyOf(items.size() > MAX_ITEMS ? items.subList(0, MAX_ITEMS) : items);
		}

		public static ItemList of(Collection<ItemInstance> items, boolean showWindow) {
			return new ItemList(items.stream().map(i -> ItemInfo.of(i, 0)).toList(), showWindow);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x1b).writeH(showWindow ? 1 : 0).writeH(items.size());
			items.forEach(i -> i.write(w));
			return w.toByteArray();
		}
	}

	/** 0x27 InventoryUpdate: so os itens que mudaram (1 = novo, 2 = modificado, 3 = removido). */
	record InventoryUpdate(List<ItemInfo> items) implements GameServerPacket {
		public InventoryUpdate {
			items = List.copyOf(items);
		}

		public static InventoryUpdate modified(Collection<ItemInstance> items) {
			return new InventoryUpdate(items.stream().map(i -> ItemInfo.of(i, ItemInfo.MODIFIED)).toList());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x27).writeH(items.size());
			for (ItemInfo i : items) {
				w.writeH(i.change());
				i.write(w);
			}
			return w.toByteArray();
		}
	}

	/** 0x64 SystemMessage: mensagem do systemmsg.dat do cliente com parametros tipados. */
	record SystemMessage(int id, List<Param> params) implements GameServerPacket {
		public static final int YOU_PICKED_UP_S1_S2 = 29;
		public static final int S1_EQUIPPED = 49;
		public static final int EARNED_S2_S1_S = 53;
		public static final int S1_CANNOT_BE_USED = 113;
		public static final int S1_S2_EQUIPPED = 368;
		public static final int S1_DISARMED = 417;
		public static final int EQUIPMENT_S1_S2_REMOVED = 1064;

		public sealed interface Param {
		}

		public record Text(String value) implements Param {
		}

		public record Number(int value) implements Param {
		}

		public record ItemName(int itemId) implements Param {
		}

		public record NpcName(int npcId) implements Param {
		}

		public SystemMessage {
			params = List.copyOf(params);
		}

		public static SystemMessage of(int id, Param... params) {
			return new SystemMessage(id, List.of(params));
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x64).writeD(id).writeD(params.size());
			for (Param p : params) {
				switch (p) {
					case Text t -> w.writeD(0).writeS(t.value());
					case Number n -> w.writeD(1).writeD(n.value());
					case NpcName n -> w.writeD(2).writeD(1_000_000 + n.npcId());
					case ItemName i -> w.writeD(3).writeD(i.itemId());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x58 SkillList (vazia). */
	record SkillList() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x58).writeD(0).toByteArray();
		}
	}

	/** 0x80 QuestList (vazia). */
	record QuestList() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x80).writeH(0).toByteArray();
		}
	}

	/** 0x45 ShortCutInit (vazio). */
	record ShortCutInit() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x45).writeD(0).toByteArray();
		}
	}

	/** 0xe4 HennaInfo (sem tatuagens). */
	record HennaInfo() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xe4).writeC(0).writeC(0).writeC(0).writeC(0).writeC(0).writeC(0)
					.writeD(3).writeD(0).toByteArray();
		}
	}

	/** 0xF3 EtcStatusUpdate (sem penalidades). */
	record EtcStatusUpdate() implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xf3);
			for (int i = 0; i < 7; i++) {
				w.writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** 0xFE:0x2E ExStorageMaxCount. */
	record ExStorageMaxCount(int inventory, int warehouse, int freight, int privateSell, int privateBuy,
			int dwarfRecipe, int commonRecipe) implements GameServerPacket {
		public static ExStorageMaxCount defaults() {
			return new ExStorageMaxCount(80, 100, 20, 4, 4, 100, 100);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x2e).writeD(inventory).writeD(warehouse)
					.writeD(freight).writeD(privateSell).writeD(privateBuy).writeD(dwarfRecipe)
					.writeD(commonRecipe).toByteArray();
		}
	}

	/** 0xfa FriendList (vazia). */
	record FriendList() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfa).writeD(0).toByteArray();
		}
	}

	/** 0xEC ClientSetTime: hora do mundo em minutos do dia de jogo e velocidade (6). */
	record ClientSetTime(int gameTime) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xec).writeD(gameTime).writeD(6).toByteArray();
		}
	}

	/** 0xFE:0x1B ExSendManorList: castelos com manor (o cliente pede logo apos CharSelected). */
	record ExSendManorList(List<String> manors) implements GameServerPacket {
		public static final List<String> CASTLES = List.of("gludio", "dion", "giran", "oren", "aden",
				"innadril", "goddard", "rune", "schuttgart");

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x1b).writeD(manors.size());
			for (int i = 0; i < manors.size(); i++) {
				w.writeD(i + 1).writeS(manors.get(i));
			}
			return w.toByteArray();
		}
	}

	/** 0x01 MoveToLocation. */
	record MoveToLocation(int objectId, int toX, int toY, int toZ, int fromX, int fromY, int fromZ)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x01).writeD(objectId).writeD(toX).writeD(toY).writeD(toZ)
					.writeD(fromX).writeD(fromY).writeD(fromZ).toByteArray();
		}
	}

	/** 0x47 StopMove. */
	record StopMove(int objectId, int x, int y, int z, int heading) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x47).writeD(objectId).writeD(x).writeD(y).writeD(z).writeD(heading)
					.toByteArray();
		}
	}

	/** 0x61 ValidateLocation. */
	record ValidateLocation(int objectId, int x, int y, int z, int heading) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x61).writeD(objectId).writeD(x).writeD(y).writeD(z).writeD(heading)
					.toByteArray();
		}
	}

	/** 0x4a CreatureSay: mensagem de chat (tipo = canal do SystemChatChannelId). */
	record CreatureSay(int objectId, int channel, String name, String text) implements GameServerPacket {
		public static final int ALL = 0;
		public static final int SHOUT = 1;
		public static final int TELL = 2;
		public static final int ANNOUNCEMENT = 10;

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x4a).writeD(objectId).writeD(channel).writeS(name).writeS(text)
					.toByteArray();
		}
	}

	/** 0x25 ActionFailed: destrava o cliente apos uma acao recusada/ignorada. */
	record ActionFailed() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new byte[] { 0x25 };
		}
	}

	/** 0x2f ChangeWaitType: 0 = sentar, 1 = levantar. */
	record ChangeWaitType(int objectId, int type, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2f).writeD(objectId).writeD(type).writeD(x).writeD(y).writeD(z)
					.toByteArray();
		}
	}

	/** 0x2e ChangeMoveType: 1 = correr, 0 = andar. */
	record ChangeMoveType(int objectId, boolean running) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2e).writeD(objectId).writeD(running ? 1 : 0).writeD(0)
					.toByteArray();
		}
	}

	/** 0xa6 MyTargetSelected. */
	record MyTargetSelected(int objectId, int color) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xa6).writeD(objectId).writeH(color).toByteArray();
		}
	}

	/** 0x2a TargetUnselected. */
	record TargetUnselected(int objectId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2a).writeD(objectId).writeD(x).writeD(y).writeD(z).toByteArray();
		}
	}

	/** 0x5f RestartResponse. */
	record RestartResponse(boolean ok) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5f).writeD(ok ? 1 : 0).toByteArray();
		}
	}

	/** 0x7e LeaveWorld: o cliente volta a tela de login. */
	record LeaveWorld() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new byte[] { 0x7e };
		}
	}

	/** 0x16 NpcInfo: visualizacao de NPC/monstro no mundo. */
	record NpcInfo(NpcInstance npc) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var t = npc.template();
			PacketWriter w = new PacketWriter().writeC(0x16);
			w.writeD(npc.objectId());
			w.writeD(t.idTemplate() + 1000000);
			w.writeD(t.isAttackable() ? 1 : 0);
			w.writeD(npc.x()).writeD(npc.y()).writeD(npc.z()).writeD(npc.heading());
			w.writeD(0x00);
			w.writeD(t.mAtkSpd()).writeD(t.pAtkSpd());
			w.writeD(t.runSpd()).writeD(t.walkSpd());
			w.writeD(t.runSpd()).writeD(t.walkSpd()); // swim run/walk
			w.writeD(t.runSpd()).writeD(t.walkSpd()); // fl run/walk
			w.writeD(t.runSpd()).writeD(t.walkSpd()); // fly run/walk
			w.writeF(1.1);
			w.writeF(t.pAtkSpd() / 277.478340719);
			w.writeF(t.collisionRadius()).writeF(t.collisionHeight());
			w.writeD(t.rhand()).writeD(t.armor()).writeD(t.lhand());
			w.writeC(1); // name above char
			w.writeC(npc.isRunning() ? 1 : 0);
			w.writeC(npc.isInCombat() ? 1 : 0);
			w.writeC(npc.isDead() ? 1 : 0);
			w.writeC(0); // isSummoned
			w.writeS(t.serverSideName() ? t.name() : "");
			w.writeS(t.serverSideTitle() ? t.title() : "");
			w.writeD(0x00).writeD(0x00).writeD(0x00);
			w.writeD(0); // abnormal effect
			w.writeD(0).writeD(0).writeD(0).writeD(0); // clan / ally
			w.writeC(0); // fly / water
			w.writeC(0); // team
			w.writeF(t.collisionRadius()).writeF(t.collisionHeight());
			w.writeD(0x00).writeD(0x00);
			return w.toByteArray();
		}
	}

	/** 0x12 DeleteObject: remove um objeto visivel (NPC ou player que saiu do alcance). */
	record DeleteObject(int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x12).writeD(objectId).writeD(0x00).toByteArray();
		}
	}

	/** 0x03 CharInfo: visualizacao de outro jogador no mundo. */
	record CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll) implements GameServerPacket {
		public CharInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory().paperdollView());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x03);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z()).writeD(c.heading());
			w.writeD(c.objectId());
			w.writeS(c.name());
			w.writeD(c.race());
			w.writeD(c.female() ? 1 : 0);
			w.writeD(c.classId());

			// 12 slots visiveis de paperdoll (item IDs)
			w.writeD(paperdoll.itemId(ItemSlots.HAIRALL));
			w.writeD(paperdoll.itemId(ItemSlots.HEAD));
			w.writeD(paperdoll.itemId(ItemSlots.RHAND));
			w.writeD(paperdoll.itemId(ItemSlots.LHAND));
			w.writeD(paperdoll.itemId(ItemSlots.GLOVES));
			w.writeD(paperdoll.itemId(ItemSlots.CHEST));
			w.writeD(paperdoll.itemId(ItemSlots.LEGS));
			w.writeD(paperdoll.itemId(ItemSlots.FEET));
			w.writeD(paperdoll.itemId(ItemSlots.BACK));
			w.writeD(paperdoll.itemId(ItemSlots.RHAND));
			w.writeD(paperdoll.itemId(ItemSlots.HAIR));
			w.writeD(paperdoll.itemId(ItemSlots.FACE));

			// Augmentation e enchant info (20 shorts + 2 ints = 48 bytes)
			for (int i = 0; i < 4; i++) w.writeH(0x00);
			w.writeD(0x00);
			for (int i = 0; i < 12; i++) w.writeH(0x00);
			w.writeD(0x00);
			for (int i = 0; i < 4; i++) w.writeH(0x00);

			w.writeD(0x00); // pvp flag
			w.writeD(c.karma());
			w.writeD(t.mAtkSpd()).writeD(t.pAtkSpd());
			w.writeD(0x00).writeD(c.karma());

			int run = t.runSpeed();
			int walk = 80;
			w.writeD(run).writeD(walk).writeD(run).writeD(walk).writeD(run).writeD(walk).writeD(run).writeD(walk);
			w.writeF(1.0).writeF(1.0);
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));

			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face());
			w.writeS(c.title());
			w.writeD(c.clanId()).writeD(0).writeD(0).writeD(0);

			w.writeD(0);
			w.writeC(c.sitting() ? 0 : 1);
			w.writeC(c.running() ? 1 : 0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(0);

			w.writeH(0);
			w.writeC(0);
			w.writeD(0);
			w.writeC(0);
			w.writeH(0);
			w.writeD(c.classId());
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			w.writeC(0);
			w.writeC(0);
			w.writeD(0);
			w.writeC(0).writeC(0);
			w.writeC(0).writeD(0).writeD(0).writeD(0);
			w.writeD(0xFFFFFF);
			w.writeD(0x00);
			w.writeD(0).writeD(0);
			w.writeD(0xFFFF77);
			w.writeD(0x00);
			return w.toByteArray();
		}
	}
}
