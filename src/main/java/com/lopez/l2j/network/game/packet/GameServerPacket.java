package com.lopez.l2j.network.game.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.config.ConfigLoader;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.Paperdoll;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.network.login.packet.PacketWriter;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	record UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad, PlayerStats stats,
			int clanCrestId, int allyId, int allyCrestId)
			implements GameServerPacket {
		static final int WALK_SPEED = 80;
		static final int INVENTORY_LIMIT = 80;
		static final int NAME_COLOR = 0xFFFFFF;
		static final int TITLE_COLOR = 0xFFFF77;

		public UserInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory().paperdollView(), c.inventory().currentLoad(), PlayerStats.calculate(c, t), 0, 0, 0);
		}

		public UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad) {
			this(c, t, paperdoll, currentLoad, PlayerStats.calculate(c, t), 0, 0, 0);
		}

		public UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad, PlayerStats stats) {
			this(c, t, paperdoll, currentLoad, stats, 0, 0, 0);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x04);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z()).writeD(c.heading()).writeD(c.objectId());
			w.writeS(c.name()).writeD(c.race()).writeD(c.female() ? 1 : 0).writeD(c.classId());
			w.writeD(c.level()).writeQ(c.exp());
			w.writeD(t.str() + c.hennaSTR() + c.augSTR()).writeD(t.dex() + c.hennaDEX()).writeD(t.con() + c.hennaCON() + c.augCON())
					.writeD(t.intel() + c.hennaINT() + c.augINT()).writeD(t.wit() + c.hennaWIT()).writeD(t.men() + c.hennaMEN() + c.augMEN());
			w.writeD(c.maxHp()).writeD((int) c.currentHp()).writeD(c.maxMp()).writeD((int) c.currentMp());
			int maxLoad = (int) Math.min(Integer.MAX_VALUE, (long) (t.maxLoad() * (Config.ALT_WEIGHT_LIMIT > 0 ? Config.ALT_WEIGHT_LIMIT : 1.0f)));
			w.writeD(c.sp()).writeD(currentLoad).writeD(maxLoad);
			w.writeD(0x28); // valor fixo do legado (posicao 0x28 do paperdoll)
			writePaperdoll(w, paperdoll, ItemSlots.RHAND);
			for (int i = 0; i < 14; i++) {
				w.writeH(0x00);
			}
			int rhandAug = 0;
			int lrhandAug = 0;
			var inv = c.inventory();
			if (inv != null) {
				var rw = inv.paperdoll(ItemSlots.RHAND);
				if (rw != null && rw.augmentation() != null) rhandAug = rw.augmentation().attributes();
				var lrw = inv.paperdoll(ItemSlots.LRHAND);
				if (lrw != null && lrw.augmentation() != null) lrhandAug = lrw.augmentation().attributes();
			}
			w.writeD(rhandAug); // augmentation RHAND
			for (int i = 0; i < 12; i++) {
				w.writeH(0x00);
			}
			w.writeD(lrhandAug); // augmentation LRHAND
			for (int i = 0; i < 4; i++) {
				w.writeH(0x00);
			}
			w.writeD(stats.pAtk()).writeD(stats.pAtkSpd()).writeD(stats.pDef()).writeD(stats.evasion()).writeD(stats.accuracy())
					.writeD(stats.critical()).writeD(stats.mAtk());
			w.writeD(stats.mAtkSpd()).writeD(stats.pAtkSpd());
			w.writeD(stats.mDef());
			w.writeD(c.pvpFlag()).writeD(c.karma()); // pvp flag, karma
			int run = stats.runSpeed();
			int baseRun = t != null ? Math.max(1, t.runSpeed()) : 120;
			double moveMultiplier = (double) run / baseRun;
			double atkSpeedMultiplier = stats.pAtkSpd() / 277.478340719;
			int walk = Math.max(1, (int) Math.round(80.0 * moveMultiplier));
			int swimRun = Math.max(1, (int) Math.round(run * 0.7));
			int swimWalk = Math.max(1, (int) Math.round(walk * 0.7));

			w.writeD(run).writeD(walk).writeD(swimRun).writeD(swimWalk).writeD(run).writeD(walk);
			w.writeD(0).writeD(0); // fly speeds
			w.writeF(moveMultiplier).writeF(atkSpeedMultiplier); // move / attack speed multipliers
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));
			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face()).writeD(c.isGm() ? 1 : 0);
			w.writeS(c.title());
			w.writeD(c.clanId()).writeD(clanCrestId).writeD(allyId).writeD(allyCrestId);
			w.writeD(0); // relation
			w.writeC(0).writeC(c.privateStoreType()).writeC(t.canCraft() ? 1 : 0); // mount, private store, dwarven craft
			w.writeD(c.pkKills()).writeD(c.pvpKills());
			w.writeH(0); // cubics
			w.writeC(0);
			w.writeD(c.abnormalEffect()); // abnormal effect
			w.writeC(0);
			w.writeD(0); // clan privileges
			w.writeH(c.recomLeft()).writeH(c.recomHave()); // recom left/have
			w.writeD(0);
			w.writeH(INVENTORY_LIMIT);
			w.writeD(c.classId()).writeD(0);
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			w.writeC(0); // enchant effect
			w.writeC(0); // team
			w.writeD(0); // large clan crest
			w.writeC(0).writeC(0); // noble, hero
			w.writeC(0).writeD(0).writeD(0).writeD(0); // fishing
			w.writeD(c.isGm() ? 0x00FFFF : (c.nameColor() != 0 ? c.nameColor() : NAME_COLOR));
			w.writeC(c.running() ? 1 : 0);
			w.writeD(0).writeD(0); // pledge class (x2)
			w.writeD(c.isGm() ? 0x00FF77 : (c.titleColor() != 0 ? c.titleColor() : TITLE_COLOR));
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
					i.customType1(), i.isEquipped(), t.bodyPart(), i.enchant(), i.customType2(),
					i.augmentation() != null ? i.augmentation().attributes() : 0, i.mana());
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
		public static final int YOU_PICKED_UP_S1_ADENA = 28;
		public static final int YOU_PICKED_UP_S1_S2 = 29;
		public static final int YOU_PICKED_UP_S1 = 30;
		public static final int YOU_DID_S1_DMG = 35;
		public static final int S1_GAVE_YOU_S2_DMG = 36;
		public static final int EARNED_S1_EXPERIENCE = 45;
		public static final int USE_S1 = 46;
		public static final int S1_PREPARED_FOR_REUSE = 48;
		public static final int S1_HAS_WORN_OFF = 92;
		public static final int YOU_FEEL_S1_EFFECT = 110;
		public static final int SOULSHOTS_GRADE_MISMATCH = 337;
		public static final int NOT_ENOUGH_SOULSHOTS = 338;
		public static final int CANNOT_USE_SOULSHOTS = 339;
		public static final int ENABLED_SOULSHOT = 342;
		public static final int S1_HP_RESTORED = 1066;
		public static final int S1_MP_RESTORED = 1068;
		public static final int S1_CP_WILL_BE_RESTORED = 1405;
		public static final int USE_OF_S1_WILL_BE_AUTO = 1433;
		public static final int AUTO_USE_OF_S1_CANCELLED = 1434;
		public static final int S1_EQUIPPED = 49;
		public static final int EARNED_S1_ADENA = 52;
		public static final int EARNED_S2_S1_S = 53;
		public static final int EARNED_S1 = 54;
		public static final int YOU_EARNED_S1_EXP_AND_S2_SP = 95;
		public static final int YOU_INCREASED_YOUR_LEVEL = 96;
		public static final int S1_CANNOT_BE_USED = 113;
		public static final int SLOTS_FULL = 129;
		public static final int YOU_NOT_ENOUGH_ADENA = 279;
		public static final int ACQUIRED_S1_SP = 331;
		public static final int S1_S2_EQUIPPED = 368;
		public static final int S1_DISARMED = 417;
		public static final int WEIGHT_LIMIT_EXCEEDED = 422;
		public static final int EQUIPMENT_S1_S2_REMOVED = 1064;
		public static final int TARGET_TOO_FAR = 22;
		public static final int NOT_ENOUGH_MP = 24;
		public static final int NOT_ENOUGH_HP = 23;
		public static final int CASTING_INTERRUPTED = 27;
		public static final int TARGET_IS_INCORRECT = 144;
		public static final int ITEM_MISSING_TO_LEARN_SKILL = 276;
		public static final int LEARNED_SKILL_S1 = 277;
		public static final int NOT_ENOUGH_SP_TO_LEARN_SKILL = 278;
		public static final int S1_DISAPPEARED = 302;
		public static final int DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN = 607;
		public static final int EFFECT_S1_DISAPPEARED = 749;
		public static final int NO_MORE_SKILLS_TO_LEARN = 750;
		public static final int S2_HP_RESTORED_BY_S1 = 1067;
		public static final int CRITICAL_HIT_MAGIC = 1280;
		public static final int CRITICAL_HIT = 44;
		public static final int S1_WAS_UNAFFECTED_BY_S2 = 139;
		public static final int SPIRITSHOTS_GRADE_MISMATCH = 530;
		public static final int NOT_ENOUGH_SPIRITSHOTS = 531;
		public static final int CANNOT_USE_SPIRITSHOTS = 532;
		public static final int ENABLED_SPIRITSHOT = 533;
		public static final int DISABLED_SPIRITSHOT = 534;
		public static final int SKILL_REMOVED_DUE_LACK_MP = 140;
		public static final int YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL = 1439;
		public static final int YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1 = 1440;
		public static final int YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL = 1441;
		public static final int YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL = 1443;
		public static final int YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL = 1444;
		public static final int ADD_NEW_SUBCLASS = 1269;
		public static final int SUBCLASS_TRANSFER_COMPLETED = 1270;
		public static final int YOU_INVITED_S1_TO_PARTY = 105;
		public static final int YOU_JOINED_PARTY = 106;
		public static final int S1_JOINED_PARTY = 107;
		public static final int S1_LEFT_PARTY = 108;
		public static final int CANT_INVITE_YOURSELF = 159;
		public static final int PLAYER_ALREADY_IN_PARTY = 160;
		public static final int ONLY_LEADER_CAN_INVITE = 161;
		public static final int PARTY_FULL = 162;
		public static final int S1_REFUSED_PARTY = 163;
		public static final int WAITING_FOR_REPLY = 164;
		public static final int TARGET_CANT_FOUND = 165;
		public static final int PARTY_DISPERSED = 203;
		public static final int NOT_ENOUGH_ARROWS = 726;
		public static final int S1_SUCCESSFULLY_ENCHANTED = 62;
		public static final int S1_S2_SUCCESSFULLY_ENCHANTED = 63;
		public static final int ENCHANTMENT_FAILED_S1_EVAPORATED = 64;
		public static final int ENCHANTMENT_FAILED_S1_S2_EVAPORATED = 65;
		public static final int WAREHOUSE_FULL = 130;
		public static final int INAPPROPRIATE_ENCHANT_CONDITION = 355;
		public static final int BLESSED_ENCHANT_FAILED = 1517;
		public static final int SELECT_THE_CATALYST_FOR_AUGMENTATION = 1958;
		public static final int REQUIRES_S1_S2 = 1959;
		public static final int THIS_IS_NOT_A_SUITABLE_ITEM = 1960;
		public static final int GEMSTONE_QUANTITY_IS_INCORRECT = 1961;
		public static final int THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED = 1962;
		public static final int SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION = 1963;
		public static final int AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM = 1964;
		public static final int AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1 = 1965;
		public static final int FISHING_POLE_NOT_EQUIPPED = 1453;
		public static final int BAIT_ON_HOOK_BEFORE_FISHING = 1454;
		public static final int CANNOT_FISH_UNDER_WATER = 1455;
		public static final int CANNOT_FISH_ON_BOAT = 1456;
		public static final int CANNOT_FISH_HERE = 1457;
		public static final int FISHING_ATTEMPT_CANCELLED = 1458;
		public static final int NOT_ENOUGH_BAIT = 1459;
		public static final int REEL_LINE_AND_STOP_FISHING = 1460;
		public static final int CAST_LINE_AND_START_FISHING = 1461;
		public static final int CAN_USE_PUMPING_ONLY_WHILE_FISHING = 1462;
		public static final int CAN_USE_REELING_ONLY_WHILE_FISHING = 1463;
		public static final int FISH_RESISTED_ATTEMPT_TO_BRING_IT_IN = 1464;
		public static final int PUMPING_SUCCESFUL_S1_DAMAGE = 1465;
		public static final int FISH_RESISTED_PUMPING_S1_HP_REGAINED = 1466;
		public static final int REELING_SUCCESFUL_S1_DAMAGE = 1467;
		public static final int FISH_RESISTED_REELING_S1_HP_REGAINED = 1468;
		public static final int YOU_CAUGHT_SOMETHING = 1469;
		public static final int GOT_A_BITE = 1449;
		public static final int BAIT_STOLEN_BY_FISH = 1450;
		public static final int FISH_SPIT_THE_HOOK = 1451;
		public static final int YOU_CAUGHT_SOMETHING_SMELLY_THROW_IT_BACK = 1452;
		public static final int REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY = 1480;
		public static final int PUMPING_SUCCESSFUL_PENALTY_S1 = 1481;
		public static final int REELING_SUCCESSFUL_PENALTY_S1 = 1482;
		public static final int YOU_ALREADY_HAVE_A_PET = 543;
		public static final int SUMMON_A_PET = 547;
		public static final int YOU_CANNOT_SUMMON_IN_COMBAT = 578;
		public static final int NOTHING_INSIDE_THAT = 1669;
		public static final int S1_NIGHT_EFFECT_APPLIES = 1131;
		public static final int S1_NIGHT_EFFECT_DISAPPEARS = 1132;

		public static SystemMessage id(int id) {
			return new SystemMessage(id, List.of());
		}

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

		public record SkillName(int skillId, int level) implements Param {
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
					case SkillName s -> w.writeD(4).writeD(s.skillId()).writeD(s.level());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x58 SkillList: lista de habilidades aprendidas pelo jogador. */
	record SkillList(List<com.lopez.l2j.game.skill.Skill> skills) implements GameServerPacket {
		public SkillList() {
			this(List.of());
		}

		public SkillList {
			skills = List.copyOf(skills);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x58).writeD(skills.size());
			for (var s : skills) {
				w.writeD(s.passive() ? 1 : 0);
				w.writeD(s.level());
				w.writeD(s.id());
				w.writeC(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x80 QuestList (vazia). */
	record QuestList() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x80).writeH(0).toByteArray();
		}
	}

	/** 0x45 ShortCutInit: atalhos da barra rapida do jogador. */
	record ShortCutInit(List<com.lopez.l2j.game.shortcut.ShortCut> shortcuts) implements GameServerPacket {
		public ShortCutInit() {
			this(List.of());
		}

		public ShortCutInit {
			shortcuts = List.copyOf(shortcuts);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x45).writeD(shortcuts.size());
			for (var sc : shortcuts) {
				w.writeD(sc.type());
				w.writeD(sc.globalSlot());
				switch (sc.type()) {
					case 1 -> {
						w.writeD(sc.id());
						w.writeD(0x01);
						w.writeD(-1);
						w.writeD(0x00);
						w.writeD(0x00);
						w.writeH(0x00);
						w.writeH(0x00);
					}
					case 2 -> {
						w.writeD(sc.id());
						w.writeD(sc.level());
						w.writeC(0x00);
						w.writeD(0x01);
					}
					default -> {
						w.writeD(sc.id());
						w.writeD(0x01);
					}
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x44 ShortCutRegister: confirmacao de registro de atalho. */
	record ShortCutRegister(com.lopez.l2j.game.shortcut.ShortCut shortcut) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x44);
			w.writeD(shortcut.type());
			w.writeD(shortcut.globalSlot());
			switch (shortcut.type()) {
				case 1 -> {
					w.writeD(shortcut.id());
					w.writeD(shortcut.characterType());
					w.writeD(-1);
					w.writeD(0x00);
					w.writeD(0x00);
					w.writeD(0x00);
				}
				case 2 -> {
					w.writeD(shortcut.id());
					w.writeD(shortcut.level());
					w.writeC(0x00);
					w.writeD(shortcut.characterType());
				}
				default -> {
					w.writeD(shortcut.id());
					w.writeD(shortcut.characterType());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x48 MagicSkillUse: animacao e efeito de conjuracao de habilidade. */
	record MagicSkillUse(int charObjId, int targetObjId, int skillId, int skillLevel, int hitTime, int reuseDelay,
			int x, int y, int z, int targetX, int targetY, int targetZ) implements GameServerPacket {
		public MagicSkillUse(int charObjId, int targetObjId, int skillId, int skillLevel, int hitTime, int reuseDelay) {
			this(charObjId, targetObjId, skillId, skillLevel, hitTime, reuseDelay, 0, 0, 0, 0, 0, 0);
		}
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0x48)
					.writeD(charObjId)
					.writeD(targetObjId)
					.writeD(skillId)
					.writeD(skillLevel)
					.writeD(hitTime)
					.writeD(reuseDelay)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(0)
					.writeD(targetX)
					.writeD(targetY)
					.writeD(targetZ)
					.toByteArray();
		}
	}

	/** 0x49 MagicSkillCanceld: cancela a animacao/conjuracao de habilidade no cliente. */
	record MagicSkillCanceld(int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x49).writeD(objectId).toByteArray();
		}
	}

	/** 0xFE:0x12 ExAutoSoulShot: acende/apaga o icone de uso automatico do shot (type 1 = on, 0 = off). */
	record ExAutoSoulShot(int itemId, int type) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x12).writeD(itemId).writeD(type).toByteArray();
		}
	}

	/** 0x7f MagicEffectIcons: icones de buffs na barra (duracao restante em segundos). */
	record MagicEffectIcons(List<Icon> icons) implements GameServerPacket {
		public record Icon(int skillId, int level, int durationSeconds) {
		}

		public MagicEffectIcons {
			icons = List.copyOf(icons);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x7f).writeH(icons.size());
			for (Icon i : icons) {
				w.writeD(i.skillId()).writeH(i.level()).writeD(i.durationSeconds());
			}
			return w.toByteArray();
		}
	}

	/** 0x8a AcquireSkillList: janela de aprendizado do treinador (type 0 = skills de classe). */
	record AcquireSkillList(int type, List<Entry> skills) implements GameServerPacket {
		public record Entry(int id, int nextLevel, int maxLevel, int spCost, int requirements) {
		}

		public AcquireSkillList {
			skills = List.copyOf(skills);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x8a).writeD(type).writeD(skills.size());
			for (Entry e : skills) {
				w.writeD(e.id()).writeD(e.nextLevel()).writeD(e.maxLevel()).writeD(e.spCost()).writeD(e.requirements());
			}
			return w.toByteArray();
		}
	}

	/** 0x8b AcquireSkillInfo: custo e requisitos (livros) do skill selecionado na janela do treinador. */
	record AcquireSkillInfo(int id, int level, int spCost, int mode, List<Requirement> requirements)
			implements GameServerPacket {
		/** type 99 = item (livro), unk 50 como no L2J. */
		public record Requirement(int type, int itemId, int count, int unk) {
		}

		public AcquireSkillInfo {
			requirements = List.copyOf(requirements);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x8b).writeD(id).writeD(level).writeD(spCost).writeD(mode)
					.writeD(requirements.size());
			for (Requirement r : requirements) {
				w.writeD(r.type()).writeD(r.itemId()).writeD(r.count()).writeD(r.unk());
			}
			return w.toByteArray();
		}
	}

	/** 0x25 AcquireSkillDone: fecha/atualiza a janela de aprendizado. */
	record AcquireSkillDone() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x25).toByteArray();
		}
	}

	/** 0x76 MagicSkillLaunched: momento em que o skill e disparado sobre os alvos. */
	record MagicSkillLaunched(int charObjId, int skillId, int skillLevel, List<Integer> targets)
			implements GameServerPacket {
		public MagicSkillLaunched {
			targets = List.copyOf(targets);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x76).writeD(charObjId).writeD(skillId).writeD(skillLevel)
					.writeD(targets.size());
			if (targets.isEmpty()) {
				w.writeD(0);
			}
			for (int t : targets) {
				w.writeD(t);
			}
			return w.toByteArray();
		}
	}

	/** 0x6d SetupGauge: barra de conjuracao (0 = azul). */
	record SetupGauge(int color, int time) implements GameServerPacket {
		public static final int BLUE = 0;

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x6d).writeD(color).writeD(time).writeD(time).toByteArray();
		}
	}

	/** 0xe4 HennaInfo: informacoes de tatuagens e modificadores de status do jogador */
	record HennaInfo(int intAdd, int strAdd, int conAdd, int menAdd, int dexAdd, int witAdd,
			List<Integer> symbols) implements GameServerPacket {

		public HennaInfo() {
			this(0, 0, 0, 0, 0, 0, List.of());
		}

		public HennaInfo(PlayerCharacter player) {
			this(player.hennaINT(), player.hennaSTR(), player.hennaCON(), player.hennaMEN(),
					player.hennaDEX(), player.hennaWIT(),
					java.util.Arrays.stream(player.hennas()).filter(id -> id > 0).boxed().toList());
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xe4)
					.writeC(intAdd)
					.writeC(strAdd)
					.writeC(conAdd)
					.writeC(menAdd)
					.writeC(dexAdd)
					.writeC(witAdd)
					.writeD(3) // 3 slots max
					.writeD(symbols.size());
			for (int id : symbols) {
				w.writeD(id).writeD(id);
			}
			return w.toByteArray();
		}
	}

	/** 0xe2 HennaEquipList: lista de tatuagens disponiveis no Symbol Maker */
	record HennaEquipList(int adena, int maxSlots, List<com.lopez.l2j.game.henna.Henna> hennas) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xe2)
					.writeD(adena)
					.writeD(maxSlots)
					.writeD(hennas.size());
			for (var h : hennas) {
				w.writeD(h.symbolId())
						.writeD(h.dyeId())
						.writeD(h.dyeAmount())
						.writeD(h.price())
						.writeD(1);
			}
			return w.toByteArray();
		}
	}

	/** 0xe3 HennaItemInfo: detalhes e alteracao de atributos de uma tatuagem */
	record HennaItemInfo(com.lopez.l2j.game.henna.Henna henna, PlayerCharacter player, CharTemplate tpl) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int curInt = tpl.intel() + player.hennaINT();
			int curStr = tpl.str() + player.hennaSTR();
			int curCon = tpl.con() + player.hennaCON();
			int curMen = tpl.men() + player.hennaMEN();
			int curDex = tpl.dex() + player.hennaDEX();
			int curWit = tpl.wit() + player.hennaWIT();

			return new PacketWriter().writeC(0xe3)
					.writeD(henna.symbolId())
					.writeD(henna.dyeId())
					.writeD(henna.dyeAmount())
					.writeD(henna.price())
					.writeD(1)
					.writeD((int) player.inventory().adena())
					.writeD(curInt)
					.writeC(curInt + henna.statInt())
					.writeD(curStr)
					.writeC(curStr + henna.statStr())
					.writeD(curCon)
					.writeC(curCon + henna.statCon())
					.writeD(curMen)
					.writeC(curMen + henna.statMen())
					.writeD(curDex)
					.writeC(curDex + henna.statDex())
					.writeD(curWit)
					.writeC(curWit + henna.statWit())
					.toByteArray();
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
			return of(null);
		}

		public static ExStorageMaxCount of(PlayerCharacter c) {
			boolean isDwarf = c != null && c.race() == 4;
			boolean isGM = c != null && c.accessLevel() > 0;
			int inv = isGM ? ConfigLoader.getInt("MaxInventorySlotsForGameMaster", 250)
					: (isDwarf ? Config.INVENTORY_MAXIMUM_DWARF : Config.INVENTORY_MAXIMUM_NO_DWARF);
			int wh = isDwarf ? Config.WAREHOUSE_SLOT_DWARF : Config.WAREHOUSE_SLOT_NO_DWARF;
			int freight = ConfigLoader.getInt("MaxWarehouseFreightSlots", 100);
			int pSell = isDwarf ? ConfigLoader.getInt("MaxPvtStoreSellSlotsDwarf", 6)
					: ConfigLoader.getInt("MaxPvtStoreSellSlotsOther", 4);
			int pBuy = isDwarf ? ConfigLoader.getInt("MaxPvtStoreBuySlotsDwarf", 6)
					: ConfigLoader.getInt("MaxPvtStoreBuySlotsOther", 4);
			int dwarfRec = ConfigLoader.getInt("DwarfRecipeLimit", 50);
			int commonRec = ConfigLoader.getInt("CommonRecipeLimit", 50);
			return new ExStorageMaxCount(inv, wh, freight, pSell, pBuy, dwarfRec, commonRec);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x2e).writeD(inventory).writeD(warehouse)
					.writeD(freight).writeD(privateSell).writeD(privateBuy).writeD(dwarfRecipe)
					.writeD(commonRecipe).toByteArray();
		}
	}

	/** Registro de amigo para exibicao na lista de contatos. */
	record FriendItem(int charId, String name, boolean online, int friendId) {}

	/** 0xfa FriendList: lista de amigos do jogador. */
	record FriendList(java.util.List<FriendItem> friends) implements GameServerPacket {
		public FriendList() {
			this(java.util.List.of());
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfa).writeD(friends == null ? 0 : friends.size());
			if (friends != null) {
				for (var f : friends) {
					w.writeD(f.charId());
					w.writeS(f.name());
					w.writeD(f.online() ? 1 : 0);
					w.writeD(f.online() ? f.friendId() : 0);
				}
			}
			return w.toByteArray();
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
		public static final int PARTY = 3;
		public static final int CLAN = 4;
		public static final int TRADE = 8;
		public static final int ALLIANCE = 9;
		public static final int ANNOUNCEMENT = 10;
		public static final int HERO = 17;

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
			String name = (t.serverSideName() || t.id() >= 50000 || t.id() != t.idTemplate()) ? t.name() : "";
			String title = t.serverSideTitle() ? t.title() : "";
			if (t.isMonster()) {
				String lvlTitle = "Lv " + t.level() + (t.aggroRange() > 0 ? "*" : "");
				title = title.isEmpty() ? lvlTitle : lvlTitle + " " + title;
			}
			w.writeS(name);
			w.writeS(title);
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
	record CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int clanCrestId, int allyId, int allyCrestId) implements GameServerPacket {
		public CharInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory().paperdollView(), 0, 0, 0);
		}

		public CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll) {
			this(c, t, paperdoll, 0, 0, 0);
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
			int hairall = paperdoll.itemId(ItemSlots.HAIRALL);
			int head = paperdoll.itemId(ItemSlots.HEAD);
			int rhand = paperdoll.itemId(ItemSlots.RHAND);
			int lhand = paperdoll.itemId(ItemSlots.LHAND);
			int gloves = paperdoll.itemId(ItemSlots.GLOVES);
			int chest = paperdoll.itemId(ItemSlots.CHEST);
			int legs = paperdoll.itemId(ItemSlots.LEGS);
			int feet = paperdoll.itemId(ItemSlots.FEET);
			int back = paperdoll.itemId(ItemSlots.BACK);
			int hair = paperdoll.itemId(ItemSlots.HAIR);
			int face = paperdoll.itemId(ItemSlots.FACE);

			if (c.isDressMe()) {
				if (c.dressMeArmor() != null && c.dressMeArmor().armor() != null) {
					var a = c.dressMeArmor().armor();
					if (a.chest() > 0) chest = a.chest();
					if (a.legs() > 0) legs = a.legs();
					if (a.gloves() > 0) gloves = a.gloves();
					if (a.feet() > 0) feet = a.feet();
					if (a.helmet() > 0) hairall = a.helmet();
				}
				if (c.dressMeWeapon() != null && c.dressMeWeapon().weapon() != null) {
					var wVisual = c.dressMeWeapon().weapon();
					if (wVisual.rhand() > 0) rhand = wVisual.rhand();
					if (wVisual.lhand() > 0) lhand = wVisual.lhand();
				}
			}

			w.writeD(hairall);
			w.writeD(head);
			w.writeD(rhand);
			w.writeD(lhand);
			w.writeD(gloves);
			w.writeD(chest);
			w.writeD(legs);
			w.writeD(feet);
			w.writeD(back);
			w.writeD(rhand);
			w.writeD(hair);
			w.writeD(face);

			// Augmentation e enchant info (20 shorts + 2 ints = 48 bytes)
			int rhandAug = 0;
			int lrhandAug = 0;
			if (c.inventory() != null) {
				var rw = c.inventory().paperdoll(ItemSlots.RHAND);
				if (rw != null && rw.augmentation() != null) rhandAug = rw.augmentation().attributes();
				var lrw = c.inventory().paperdoll(ItemSlots.LRHAND);
				if (lrw != null && lrw.augmentation() != null) lrhandAug = lrw.augmentation().attributes();
			}
			for (int i = 0; i < 4; i++) w.writeH(0x00);
			w.writeD(rhandAug);
			for (int i = 0; i < 12; i++) w.writeH(0x00);
			w.writeD(lrhandAug);
			for (int i = 0; i < 4; i++) w.writeH(0x00);

			PlayerStats stats = PlayerStats.calculate(c, t);
			w.writeD(c.pvpFlag()); // pvp flag
			w.writeD(c.karma());
			w.writeD(stats.mAtkSpd()).writeD(stats.pAtkSpd());
			w.writeD(c.pvpFlag()).writeD(c.karma());

			int run = stats.runSpeed();
			int baseRun = t != null ? Math.max(1, t.runSpeed()) : 120;
			double moveMultiplier = (double) run / baseRun;
			double atkSpeedMultiplier = stats.pAtkSpd() / 277.478340719;
			int walk = Math.max(1, (int) Math.round(80.0 * moveMultiplier));
			int swimRun = Math.max(1, (int) Math.round(run * 0.7));
			int swimWalk = Math.max(1, (int) Math.round(walk * 0.7));

			w.writeD(run).writeD(walk).writeD(swimRun).writeD(swimWalk).writeD(run).writeD(walk).writeD(0).writeD(0);
			w.writeF(moveMultiplier).writeF(atkSpeedMultiplier);
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));

			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face());
			w.writeS(c.title());
			w.writeD(c.clanId()).writeD(clanCrestId).writeD(allyId).writeD(allyCrestId);

			w.writeD(0);
			w.writeC(c.sitting() ? 0 : 1);
			w.writeC(c.running() ? 1 : 0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(0);
			w.writeC(c.privateStoreType());

			w.writeH(0);
			w.writeC(0);
			w.writeD(c.abnormalEffect()); // abnormal effect
			w.writeC(0);
			w.writeH(0);
			w.writeD(c.classId());
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			w.writeC(0);
			w.writeC(0);
			w.writeD(0);
			w.writeC(0).writeC(0);
			w.writeC(0).writeD(0).writeD(0).writeD(0);
			w.writeD(c.isGm() ? 0x00FFFF : (c.nameColor() != 0 ? c.nameColor() : 0xFFFFFF));
			w.writeD(0x00);
			w.writeD(0).writeD(0);
			w.writeD(c.isGm() ? 0x00FF77 : (c.titleColor() != 0 ? c.titleColor() : 0xFFFF77));
			w.writeD(0x00);
			return w.toByteArray();
		}
	}

	/** 0x0f NpcHtmlMessage: janela de dialogo de NPC com HTML do L2 e bypasses. */
	record NpcHtmlMessage(int npcObjectId, String html, int itemId) implements GameServerPacket {
		public NpcHtmlMessage(int npcObjectId, String html) {
			this(npcObjectId, html, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x0f).writeD(npcObjectId).writeS(html).writeD(itemId).toByteArray();
		}
	}

	/** 0x28 TeleportToLocation: atualiza posicao instantanea do objeto no cliente. */
	record TeleportToLocation(int objectId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x28).writeD(objectId).writeD(x).writeD(y).writeD(z).toByteArray();
		}
	}

	/** 0x11 BuyList: lista de produtos a venda no NPC mercador. */
	record BuyList(int money, int listId, List<BuyProductView> products) implements GameServerPacket {
		public record BuyProductView(int itemId, int price, int count, int type1, int type2, int bodyPart) {
		}

		public BuyList {
			products = List.copyOf(products);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x11).writeD(money).writeD(listId).writeH(products.size());
			for (BuyProductView p : products) {
				w.writeH(p.type1());
				w.writeD(p.itemId());
				w.writeD(p.itemId());
				w.writeD(p.count() < 0 ? 0 : p.count());
				w.writeH(p.type2());
				w.writeH(0);
				w.writeD(p.bodyPart());
				w.writeH(0);
				w.writeH(0);
				w.writeH(0);
				w.writeD(p.price());
			}
			return w.toByteArray();
		}
	}

	/** 0x10 SellList: lista de itens vendiveis para o NPC mercador. */
	record SellList(int money, int lease, List<SellItemView> items) implements GameServerPacket {
		public record SellItemView(int objectId, int itemId, int count, int type1, int type2, int bodyPart, int enchant, int price) {
		}

		public SellList {
			items = List.copyOf(items);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x10).writeD(money).writeD(lease).writeH(items.size());
			for (SellItemView item : items) {
				w.writeH(item.type1());
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(item.type2());
				w.writeH(0);
				w.writeD(item.bodyPart());
				w.writeH(item.enchant());
				w.writeH(0);
				w.writeH(0);
				w.writeD(item.price());
			}
			return w.toByteArray();
		}
	}

	/** 0x05 Attack: animacao de ataque e dano aplicado ao alvo. */
	record Attack(int attackerObjId, int targetObjId, int damage, int flags, int x, int y, int z)
			implements GameServerPacket {
		public static final int HITFLAG_USESS = 0x10;
		public static final int HITFLAG_CRIT = 0x20;
		public static final int HITFLAG_SHLD = 0x40;
		public static final int HITFLAG_MISS = 0x80;

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x05);
			w.writeD(attackerObjId);
			w.writeD(targetObjId);
			w.writeD(damage);
			w.writeC(flags);
			w.writeD(x);
			w.writeD(y);
			w.writeD(z);
			w.writeH(0); // hits extras = 0
			return w.toByteArray();
		}
	}

	/** 0x0e StatusUpdate: atualiza atributos e HP/MP/CP em tempo real. */
	record StatusUpdate(int objectId, List<Attribute> attributes) implements GameServerPacket {
		public record Attribute(int id, int value) {
		}

		public static final int LEVEL = 0x01;
		public static final int EXP = 0x02;
		public static final int STR = 0x03;
		public static final int DEX = 0x04;
		public static final int CON = 0x05;
		public static final int INT = 0x06;
		public static final int WIT = 0x07;
		public static final int MEN = 0x08;
		public static final int CUR_HP = 0x09;
		public static final int MAX_HP = 0x0a;
		public static final int CUR_MP = 0x0b;
		public static final int MAX_MP = 0x0c;
		public static final int SP = 0x0d;
		public static final int CUR_LOAD = 0x0e;
		public static final int CUR_CP = 0x21;
		public static final int MAX_CP = 0x22;

		public static StatusUpdate hp(int objectId, int curHp, int maxHp) {
			return new StatusUpdate(objectId, List.of(new Attribute(CUR_HP, curHp), new Attribute(MAX_HP, maxHp)));
		}

		public static StatusUpdate mp(int objectId, int curMp, int maxMp) {
			return new StatusUpdate(objectId, List.of(new Attribute(CUR_MP, curMp), new Attribute(MAX_MP, maxMp)));
		}

		public StatusUpdate {
			attributes = List.copyOf(attributes);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x0e).writeD(objectId).writeD(attributes.size());
			for (Attribute a : attributes) {
				w.writeD(a.id());
				w.writeD(a.value());
			}
			return w.toByteArray();
		}
	}

	/** 0x06 Die: objeto morreu. */
	record Die(int charObjId, boolean toVillage, boolean toClanHall, boolean toCastle) implements GameServerPacket {
		public Die(int charObjId, boolean toVillage) {
			this(charObjId, toVillage, false, false);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x06);
			w.writeD(charObjId);
			w.writeD(toVillage ? 1 : 0);
			w.writeD(toClanHall ? 1 : 0); // clanhall
			w.writeD(toCastle ? 1 : 0); // castle
			w.writeD(0); // flag
			w.writeD(0); // sweepable
			w.writeD(0); // fixedres
			return w.toByteArray();
		}
	}

	/** 0x07 Revive: objeto renasce. */
	record Revive(int charObjId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x07).writeD(charObjId).toByteArray();
		}
	}

	/** 0x60 MoveToPawn: move o personagem ate alcancar o alvo com offset. */
	record MoveToPawn(int charObjId, int targetId, int distance, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x60).writeD(charObjId).writeD(targetId).writeD(distance).writeD(x)
					.writeD(y).writeD(z).toByteArray();
		}
	}

	/** 0x2b AutoAttackStart: inicia a postura de combate/auto-attack. */
	record AutoAttackStart(int targetObjId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2b).writeD(targetObjId).toByteArray();
		}
	}

	/** 0x2c AutoAttackStop: finaliza a postura de combate. */
	record AutoAttackStop(int targetObjId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2c).writeD(targetObjId).toByteArray();
		}
	}

	/** 0x2d SocialAction: animacao de acao social (15 = Level Up). */
	record SocialAction(int charObjId, int actionId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2d).writeD(charObjId).writeD(actionId).toByteArray();
		}
	}

	/** 0x9d ShowMiniMap: abre a janela do mapa mundi (Alt+M / radar). */
	record ShowMiniMap(int mapId, int sevenSignsPeriod) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x9d).writeD(mapId).writeD(sevenSignsPeriod).toByteArray();
		}
	}

	/** 0x99 StaticObject: renderiza objeto estatico do cenario (mapa, sinalizacao, estatua). */
	record StaticObject(int staticObjectId, int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x99).writeD(staticObjectId).writeD(objectId).toByteArray();
		}
	}

	/** 0xde ShowTownMap: abre o mapa detalhado da vila (ex: ao clicar nas placas de vilas). */
	record ShowTownMap(String texture, int x, int y) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xde).writeS(texture).writeD(x).writeD(y).toByteArray();
		}
	}

	/** 0x0c DropItem: renderiza item caido no chao do mundo. */
	record DropItem(int dropperObjId, int itemObjId, int itemId, int x, int y, int z, boolean isStackable, int count) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0x0c)
					.writeD(dropperObjId)
					.writeD(itemObjId)
					.writeD(itemId)
					.writeD(x).writeD(y).writeD(z)
					.writeD(isStackable ? 1 : 0)
					.writeD(count)
					.writeD(1)
					.toByteArray();
		}
	}

	/** 0x0d GetItem: animacao visual de recolher item do chao. */
	record GetItem(int playerId, int itemObjId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0x0d)
					.writeD(playerId)
					.writeD(itemObjId)
					.writeD(x).writeD(y).writeD(z)
					.toByteArray();
		}
	}

	/** 0xfe:0x13 ExFishingStart: arremesso da linha de pesca pelo personagem. */
	record ExFishingStart(int charObjId, int fishType, int x, int y, int z, boolean isNightLure) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xfe).writeH(0x13)
					.writeD(charObjId)
					.writeD(fishType)
					.writeD(x).writeD(y).writeD(z)
					.writeC(0x00)
					.writeC(0x00)
					.writeC(fishType >= 7 && fishType <= 9 ? 0x01 : 0x00)
					.writeC(0x00)
					.toByteArray();
		}
	}

	/** 0xfe:0x14 ExFishingEnd: encerra a sessao de pesca com vitoria ou derrota. */
	record ExFishingEnd(int charObjId, boolean win) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xfe).writeH(0x14)
					.writeD(charObjId)
					.writeC(win ? 1 : 0)
					.toByteArray();
		}
	}

	/** 0xfe:0x15 ExFishingStartCombat: inicia o minigame de combate com o peixe fisgado. */
	record ExFishingStartCombat(int charObjId, int time, int hp, int mode, int lureType, int deceptiveMode) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xfe).writeH(0x15)
					.writeD(charObjId)
					.writeD(time)
					.writeD(hp)
					.writeC(mode)
					.writeC(lureType)
					.writeC(deceptiveMode)
					.toByteArray();
		}
	}

	/** 0xfe:0x16 ExFishingHpRegen: atualiza a barra de HP do peixe e a resposta a Pumping/Reeling. */
	record ExFishingHpRegen(int charObjId, int time, int fishHp, int hpMode, int goodUse, int anim, int penalty, int hpBarColor) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xfe).writeH(0x16)
					.writeD(charObjId)
					.writeD(time)
					.writeD(fishHp)
					.writeC(hpMode)
					.writeC(goodUse)
					.writeC(anim)
					.writeD(penalty)
					.writeC(hpBarColor)
					.toByteArray();
		}
	}

	/** 0xfe:0x63 ExAutoFishing: alterna o estado de pesca automatica. */
	record ExAutoFishing(int enabled) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x63).writeC(enabled).toByteArray();
		}
	}

	/** 0xfe:0x17 ExEnchantSkillList: lista de skills que podem ser encantados no trainer. */
	record ExEnchantSkillList(List<SkillEntry> skills) implements GameServerPacket {
		public record SkillEntry(int id, int nextLevel, int sp, int exp) {
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x17).writeD(skills.size());
			for (var s : skills) {
				w.writeD(s.id()).writeD(s.nextLevel()).writeD(s.sp()).writeD(s.exp());
			}
			return w.toByteArray();
		}
	}

	/** 0xfe:0x18 ExEnchantSkillInfo: detalhes de custo em SP/EXP e taxa de sucesso do proximo nivel. */
	record ExEnchantSkillInfo(int id, int level, int spCost, long expCost, int rate, List<Req> reqs)
			implements GameServerPacket {
		public record Req(int type, int id, int count, int unk) {
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x18).writeD(id).writeD(level).writeD(spCost)
					.writeQ(expCost).writeD(rate).writeD(reqs.size());
			for (var r : reqs) {
				w.writeD(r.type()).writeD(r.id()).writeD(r.count()).writeD(r.unk());
			}
			return w.toByteArray();
		}
	}

	/** 0x39 AskJoinParty: convite de grupo enviado ao jogador convidado. */
	record AskJoinParty(String requestorName, int itemDistribution) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x39).writeS(requestorName).writeD(itemDistribution).toByteArray();
		}
	}

	/** 0x3a JoinParty: resposta do convite de grupo (1 = aceito, 0 = recusado). */
	record JoinParty(int response) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x3a).writeD(response).toByteArray();
		}
	}

	/** 0x4e PartySmallWindowAll: inicializa a janela de grupo com todos os membros (exceto o próprio jogador). */
	record PartySmallWindowAll(int leaderObjectId, int lootDistribution, List<PlayerCharacter> members,
			int receiverObjectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0x4e).writeD(leaderObjectId).writeD(lootDistribution);
			long count = members.stream().filter(m -> m.objectId() != receiverObjectId).count();
			w.writeD((int) count);
			for (var m : members) {
				if (m.objectId() == receiverObjectId) {
					continue;
				}
				w.writeD(m.objectId()).writeS(m.name())
						.writeD((int) m.currentCp()).writeD(m.maxCp())
						.writeD((int) m.currentHp()).writeD(m.maxHp())
						.writeD((int) m.currentMp()).writeD(m.maxMp())
						.writeD(m.level()).writeD(m.classId())
						.writeD(0).writeD(m.race());
			}
			return w.toByteArray();
		}
	}

	/** 0x4f PartySmallWindowAdd: adiciona um novo membro na janela de grupo existente. */
	record PartySmallWindowAdd(int leaderObjectId, int lootDistribution, PlayerCharacter member)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x4f).writeD(leaderObjectId).writeD(lootDistribution)
					.writeD(member.objectId()).writeS(member.name())
					.writeD((int) member.currentCp()).writeD(member.maxCp())
					.writeD((int) member.currentHp()).writeD(member.maxHp())
					.writeD((int) member.currentMp()).writeD(member.maxMp())
					.writeD(member.level()).writeD(member.classId())
					.writeD(0).writeD(member.race()).toByteArray();
		}
	}

	/** 0x50 PartySmallWindowDeleteAll: fecha a janela de grupo quando o grupo se desfaz ou o jogador sai. */
	record PartySmallWindowDeleteAll() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x50).toByteArray();
		}
	}

	/** 0x51 PartySmallWindowDelete: remove um membro da janela de grupo. */
	record PartySmallWindowDelete(int memberObjectId, String memberName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x51).writeD(memberObjectId).writeS(memberName).toByteArray();
		}
	}

	/** 0x52 PartySmallWindowUpdate: atualiza HP/MP/CP de um membro do grupo. */
	record PartySmallWindowUpdate(PlayerCharacter member) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x52).writeD(member.objectId()).writeS(member.name())
					.writeD((int) member.currentCp()).writeD(member.maxCp())
					.writeD((int) member.currentHp()).writeD(member.maxHp())
					.writeD((int) member.currentMp()).writeD(member.maxMp())
					.writeD(member.level()).writeD(member.classId()).toByteArray();
		}
	}

	/** 0xd0 MultiSellList: janela de troca/compra de itens combinados (MultiSell). */
	record MultiSellList(int listId, int page, int finished, int pageSize, List<MultiSellEntryView> entries)
			implements GameServerPacket {

		public record MultiSellEntryView(int entryId, List<ItemView> ingredients, List<ItemView> products) {
		}

		public record ItemView(int itemId, int bodyPart, int type2, long count, int enchant) {
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter(512).writeC(0xd0);
			w.writeD(listId).writeD(page).writeD(finished).writeD(pageSize).writeD(entries.size());
			for (var ent : entries) {
				w.writeD(ent.entryId());
				w.writeD(0);
				w.writeD(0);
				w.writeC(1);
				w.writeH(ent.products().size());
				w.writeH(ent.ingredients().size());
				for (var p : ent.products()) {
					w.writeH(p.itemId());
					w.writeD(p.bodyPart());
					w.writeH(p.type2());
					w.writeD((int) p.count());
					w.writeH(p.enchant());
					w.writeD(0);
					w.writeD(0);
				}
				for (var i : ent.ingredients()) {
					w.writeH(i.itemId());
					w.writeH(i.type2());
					w.writeD((int) i.count());
					w.writeH(i.enchant());
					w.writeD(0);
					w.writeD(0);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x6f ChooseInventoryItem: abre a janela de selecao/encantamento de itens. */
	record ChooseInventoryItem(int itemId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x6f).writeD(itemId).toByteArray();
		}
	}

	/** 0x81 EnchantResult: resultado do encantamento (0=sucesso, 1=falha normal, 2=cancelado, 3=falha blessed). */
	record EnchantResult(int result) implements GameServerPacket {
		public static final int RES_SUCCESS = 0;
		public static final int RES_FAIL = 1;
		public static final int RES_CANCEL = 2;
		public static final int RES_BLESSED_FAIL = 3;

		public static final EnchantResult SUCCESS = new EnchantResult(RES_SUCCESS);
		public static final EnchantResult FAIL = new EnchantResult(RES_FAIL);
		public static final EnchantResult CANCEL = new EnchantResult(RES_CANCEL);
		public static final EnchantResult BLESSED_FAIL = new EnchantResult(RES_BLESSED_FAIL);

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x81).writeD(result).toByteArray();
		}
	}

	/** 0x41 WareHouseDepositList: lista de itens disponiveis para deposito no armazem. */
	record WareHouseDepositList(int whType, int adena, List<ItemInstance> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter(256).writeC(0x41).writeH(whType).writeD(adena).writeH(items.size());
			for (var item : items) {
				w.writeH(item.template().type1());
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(item.template().type2());
				w.writeH(item.customType1());
				w.writeD(item.template().bodyPart());
				w.writeH(item.enchant());
				w.writeH(item.customType2());
				w.writeH(0);
				w.writeD(item.objectId());
				w.writeQ(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x42 WareHouseWithdrawalList: lista de itens guardados no armazem para retirada. */
	record WareHouseWithdrawalList(int whType, int adena, List<ItemInstance> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter(256).writeC(0x42).writeH(whType).writeD(adena).writeH(items.size());
			for (var item : items) {
				w.writeH(item.template().type1());
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(item.template().type2());
				w.writeH(item.customType1());
				w.writeD(item.template().bodyPart());
				w.writeH(item.enchant());
				w.writeH(item.customType2());
				w.writeH(0);
				w.writeD(item.objectId());
				w.writeQ(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x8f GMViewCharacterInfo: status e aparencia de personagem para o painel Alt+G do GM. */
	record GMViewCharacterInfo(PlayerCharacter c, CharTemplate t, PlayerStats stats, Paperdoll paperdoll, int currentLoad)
			implements GameServerPacket {

		public GMViewCharacterInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, PlayerStats.calculate(c, t), c.inventory().paperdollView(), c.inventory().currentLoad());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter(512).writeC(0x8f);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z()).writeD(c.heading());
			w.writeD(c.objectId());
			w.writeS(c.name());
			w.writeD(c.race());
			w.writeD(c.female() ? 1 : 0);
			w.writeD(c.classId());
			w.writeD(c.level());
			w.writeQ(c.exp());
			w.writeD(t.str()).writeD(t.dex()).writeD(t.con()).writeD(t.intel()).writeD(t.wit()).writeD(t.men());
			w.writeD(c.maxHp()).writeD((int) c.currentHp());
			w.writeD(c.maxMp()).writeD((int) c.currentMp());
			w.writeD(c.sp());
			int maxLoad = (int) Math.min(Integer.MAX_VALUE, (long) (t.maxLoad() * (Config.ALT_WEIGHT_LIMIT > 0 ? Config.ALT_WEIGHT_LIMIT : 1.0f)));
			w.writeD(currentLoad).writeD(maxLoad);
			w.writeD(0x28); // paperdoll start offset
			writePaperdoll(w, paperdoll, ItemSlots.RHAND);
			w.writeD(stats.pAtk());
			w.writeD(stats.pAtkSpd());
			w.writeD(stats.pDef());
			w.writeD(stats.evasion());
			w.writeD(stats.accuracy());
			w.writeD(stats.critical());
			w.writeD(stats.mAtk());
			w.writeD(stats.mAtkSpd());
			w.writeD(stats.pAtkSpd());
			w.writeD(stats.mDef());
			w.writeD(c.pvpFlag()); // pvp flag
			w.writeD(c.karma());
			int run = stats.runSpeed();
			int walk = 80;
			w.writeD(run).writeD(walk).writeD(run).writeD(walk).writeD(run).writeD(walk).writeD(run).writeD(walk);
			w.writeF(1.0).writeF(1.0); // multipliers
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));
			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face()).writeD(c.isGm() ? 1 : 0);
			w.writeS(c.title() != null ? c.title() : "");
			w.writeD(c.clanId()).writeD(0).writeD(0); // clanId, crest, ally
			w.writeC(0).writeC(0).writeC(t.canCraft() ? 1 : 0); // mount, store, dwarven craft
			w.writeD(c.pkKills()).writeD(c.pvpKills());
			w.writeH(0).writeH(c.recomHave()); // recom left/have
			w.writeD(c.classId()).writeD(0);
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			w.writeC(c.running() ? 1 : 0);
			w.writeD(0).writeD(0).writeD(0).writeD(0);
			w.writeD(c.isGm() ? 0x00FFFF : 0xFFFFFF); // name color
			w.writeD(0);
			return w.toByteArray();
		}
	}

	/** 0x90 GMViewPledgeInfo: informacoes de clan para o GM. */
	record GMViewPledgeInfo(String charName, int clanId, int level, int classId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x90);
			w.writeS(charName);
			w.writeD(clanId);
			w.writeS(clanId != 0 ? "Clan " + clanId : "");
			w.writeS(charName);
			w.writeD(0).writeD(0).writeD(0).writeD(0).writeD(0);
			w.writeD(level);
			w.writeD(0).writeD(0).writeD(0);
			w.writeS("").writeD(0).writeD(0);
			w.writeD(1); // 1 member
			w.writeS(charName).writeD(level).writeD(classId).writeD(0).writeD(0).writeD(1);
			return w.toByteArray();
		}
	}

	/** 0x91 GMViewSkillInfo: lista de skills do jogador para o GM. */
	record GMViewSkillInfo(String charName, Map<Integer, Integer> skills, SkillService skillService)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x91);
			w.writeS(charName);
			w.writeD(skills.size());
			for (var entry : skills.entrySet()) {
				int skillId = entry.getKey();
				int level = entry.getValue();
				boolean isPassive = skillService != null && skillService.table() != null
						&& skillService.table().get(skillId, level).map(SkillTemplate::isPassive).orElse(false);
				w.writeD(isPassive ? 1 : 0);
				w.writeD(level);
				w.writeD(skillId);
				w.writeC(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x93 GMViewQuestInfo: lista de quests para o GM. */
	record GMViewQuestInfo(String charName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x93).writeS(charName).writeH(0).toByteArray();
		}
	}

	/** 0x94 GMViewItemList: inventario completo para o GM. */
	record GMViewItemList(String charName, Collection<ItemInstance> items, int inventoryLimit)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x94);
			w.writeS(charName);
			w.writeD(inventoryLimit);
			w.writeH(1);
			w.writeH(items.size());
			for (ItemInstance item : items) {
				var t = item.template();
				w.writeH(t.type1());
				w.writeD(item.objectId());
				w.writeD(t.displayId());
				w.writeD(item.count());
				w.writeH(t.type2());
				w.writeH(item.customType1());
				w.writeH(item.isEquipped() ? 1 : 0);
				w.writeD(t.bodyPart());
				w.writeH(item.enchant());
				w.writeH(item.customType2());
			}
			return w.toByteArray();
		}
	}

	/** 0xea GMHennaInfo: dyes/simbolos para a tela de inventario do GM. */
	record GMHennaInfo(int intel, int str, int con, int men, int dex, int wit) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xea)
					.writeC(intel).writeC(str).writeC(con).writeC(men).writeC(dex).writeC(wit)
					.writeD(0)
					.toByteArray();
		}
	}

	/** 0x95 GMViewWarehouseWithdrawList: armazem para o GM. */
	record GMViewWarehouseWithdrawList(String charName, int adena, List<ItemInstance> items)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x95);
			w.writeS(charName);
			w.writeD(adena);
			w.writeH(items.size());
			for (var item : items) {
				var t = item.template();
				w.writeH(t.type1());
				w.writeD(item.objectId());
				w.writeD(t.displayId());
				w.writeD(item.count());
				w.writeH(t.type2());
				w.writeH(0);
				w.writeD(t.bodyPart());
				w.writeH(item.enchant());
				w.writeH(0);
				w.writeH(0);
				w.writeD(item.objectId());
			}
			return w.toByteArray();
		}
	}

	/** 0x4C DoorInfo: informacoes e estado de porta no mundo. */
	record DoorInfo(int objectId, int doorId, int viewHp, int isTargetable, int isEnemy, int currentHp, int maxHp, boolean showHp, int damage)
			implements GameServerPacket {
		public DoorInfo(int objectId, int doorId, int currentHp, int maxHp, boolean showHp) {
			this(objectId, doorId, 1, 1, 0, currentHp, maxHp, showHp, 0);
		}

		public DoorInfo(com.lopez.l2j.game.door.DoorInstance door) {
			this(door.objectId(), door.doorId(), 1, 1, 0, door.currentHp(), door.maxHp(), true, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x4c)
					.writeD(objectId)
					.writeD(doorId)
					.writeD(viewHp)
					.writeD(isTargetable)
					.writeD(isEnemy)
					.writeD(currentHp)
					.writeD(maxHp)
					.writeD(showHp ? 1 : 0)
					.writeD(damage)
					.toByteArray();
		}
	}

	/** 0x4D DoorStatusUpdate: atualiza abertura/fechamento e HP da porta. */
	record DoorStatusUpdate(int objectId, boolean open, int damage, int isEnemy, int doorId, int maxHp, int currentHp)
			implements GameServerPacket {
		public DoorStatusUpdate(int objectId, int doorId, boolean open, int currentHp, int maxHp) {
			this(objectId, open, 0, 0, doorId, maxHp, currentHp);
		}

		public DoorStatusUpdate(com.lopez.l2j.game.door.DoorInstance door) {
			this(door.objectId(), door.isOpen(), 0, 0, door.doorId(), door.maxHp(), door.currentHp());
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x4d)
					.writeD(objectId)
					.writeD(open ? 0 : 1) // 0 = open, 1 = closed no cliente Interlude
					.writeD(damage)
					.writeD(isEnemy)
					.writeD(doorId)
					.writeD(maxHp)
					.writeD(currentHp)
					.toByteArray();
		}
	}

	/** 0x6A PledgeCrest: envia os bytes do brasao do cla (16x12 BMP) */
	record PledgeCrest(int crestId, byte[] data) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0x6a)
					.writeD(crestId)
					.writeD(data != null ? data.length : 0);
			if (data != null && data.length > 0) {
				w.writeB(data);
			}
			return w.toByteArray();
		}
	}

	/** 0x88 PledgeShowInfoUpdate: atualizacao da janela de cla */
	record PledgeShowInfoUpdate(com.lopez.l2j.game.clan.Clan clan) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x88)
					.writeD(clan.clanId())
					.writeD(clan.crestId())
					.writeD(clan.level())
					.writeD(clan.castleId())
					.writeD(clan.fortId())
					.writeD(clan.rank())
					.writeD(clan.reputationScore())
					.writeD(0)
					.writeD(0)
					.writeD(clan.allyId())
					.writeS(clan.allyName() != null ? clan.allyName() : "")
					.writeD(clan.allyCrestId())
					.writeD(0)
					.toByteArray();
		}
	}

	/** 0x53 PledgeShowMemberListAll: lista completa de membros do cla */
	record PledgeShowMemberListAll(com.lopez.l2j.game.clan.Clan clan, int subPledge) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0x53)
					.writeD(subPledge)
					.writeD(clan.clanId())
					.writeS(clan.name())
					.writeS(clan.leaderName() != null ? clan.leaderName() : "")
					.writeD(clan.crestId())
					.writeD(clan.level())
					.writeD(clan.castleId())
					.writeD(clan.fortId())
					.writeD(clan.rank())
					.writeD(clan.reputationScore())
					.writeD(0)
					.writeD(0)
					.writeD(clan.allyId())
					.writeS(clan.allyName() != null ? clan.allyName() : "")
					.writeD(clan.allyCrestId())
					.writeD(0)
					.writeD(clan.membersCount());

			for (var m : clan.members()) {
				w.writeS(m.name())
				 .writeD(m.level())
				 .writeD(m.classId())
				 .writeD(0)
				 .writeD(0)
				 .writeD(m.isOnline() ? 1 : 0)
				 .writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x54 PledgeShowMemberListUpdate: atualiza um unico membro na lista */
	record PledgeShowMemberListUpdate(String charName, int level, int classId, boolean online) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x54)
					.writeS(charName)
					.writeD(level)
					.writeD(classId)
					.writeD(0)
					.writeD(0)
					.writeD(online ? 1 : 0)
					.writeD(0)
					.toByteArray();
		}
	}

	/** 0xfe:0x45 ExCursedWeaponList: IDs de todas as armas amaldicoadas existentes */
	record ExCursedWeaponList(java.util.List<Integer> weaponIds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x45);
			if (weaponIds != null) {
				w.writeD(weaponIds.size());
				for (int id : weaponIds) {
					w.writeD(id);
				}
			} else {
				w.writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** Informacao de localizacao da arma amaldicoada para ExCursedWeaponLocation */
	record CursedWeaponLocationInfo(int weaponId, int activated, int x, int y, int z) {
	}

	/** 0xfe:0x46 ExCursedWeaponLocation: localizacao no mapa das armas amaldicoadas ativas/no chao */
	record ExCursedWeaponLocation(java.util.List<CursedWeaponLocationInfo> weapons) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x46);
			if (weapons != null && !weapons.isEmpty()) {
				w.writeD(weapons.size());
				for (var cwi : weapons) {
					w.writeD(cwi.weaponId());
					w.writeD(cwi.activated());
					w.writeD(cwi.x());
					w.writeD(cwi.y());
					w.writeD(cwi.z());
				}
			} else {
				w.writeD(0);
				w.writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** 0xf5 SSQStatus: exibe a pagina solicitada do Registro dos Sete Selos */
	record SSQStatus(int page, int period, int cycle, int playerCabal, int playerSeal,
			int stoneContrib, int adenaCollect, long dawnScore, long duskScore) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter()
					.writeC(0xf5)
					.writeC(page)
					.writeC(period);

			switch (page) {
				case 1 -> {
					w.writeD(cycle);
					w.writeD(257); // msgId periodo
					w.writeD(258); // msgId tempo
					w.writeC(playerCabal);
					w.writeC(playerSeal);
					w.writeD(stoneContrib);
					w.writeD(adenaCollect);
					w.writeD((int) Math.min(Integer.MAX_VALUE, duskScore));
					w.writeD(0);
					w.writeD((int) Math.min(Integer.MAX_VALUE, duskScore));
					w.writeC(50);
					w.writeD((int) Math.min(Integer.MAX_VALUE, dawnScore));
					w.writeD(0);
					w.writeD((int) Math.min(Integer.MAX_VALUE, dawnScore));
					w.writeC(50);
				}
				default -> {
					w.writeH(1);
					w.writeC(0);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x7d FriendAddRequest: convite de amizade enviado ao alvo. */
	record FriendAddRequest(String requestorName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x7d).writeS(requestorName).writeD(0).toByteArray();
		}
	}

	/** 0xfd L2FriendSay: mensagem de chat entre amigos / PM. */
	record L2FriendSay(String sender, String receiver, String message) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfd).writeD(0).writeS(receiver).writeS(sender).writeS(message).toByteArray();
		}
	}

	/** Item listado a venda na loja de um jogador. */
	record PrivateStoreItem(int objectId, int itemId, int count, int price, int bodyPart, int enchant) {}

	/** 0x9c PrivateStoreMsgSell: titulo da loja pessoal de venda sobre a cabeca do personagem. */
	record PrivateStoreMsgSell(int objectId, String storeMsg) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x9c).writeD(objectId).writeS(storeMsg != null ? storeMsg : "").toByteArray();
		}
	}

	/** 0x9b PrivateStoreListSell: visualizacao da lista de itens/buffs a venda para o cliente comprador. */
	record PrivateStoreListSell(int sellerId, boolean packageSale, int buyerAdena, List<PrivateStoreItem> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x9b).writeD(sellerId).writeD(packageSale ? 1 : 0).writeD(buyerAdena).writeD(items.size());
			for (var item : items) {
				w.writeD(0); // type2
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.bodyPart());
				w.writeH(item.enchant());
				w.writeD(0); // customType1
				w.writeD(0); // augment
				w.writeD(item.price()); // price
				w.writeD(0); // storePrice
			}
			return w.toByteArray();
		}
	}

	/** 0x9a PrivateStoreManageListSell: janela de configuracao da loja pessoal (itens/buffs disponiveis e precos). */
	record PrivateStoreManageListSell(int sellerId, boolean packageSale, int sellerAdena, List<PrivateStoreItem> available, List<PrivateStoreItem> current) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x9a).writeD(sellerId).writeD(packageSale ? 1 : 0).writeD(sellerAdena);
			w.writeD(available.size());
			for (var item : available) {
				w.writeD(0); // type2
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.bodyPart());
				w.writeH(item.enchant());
				w.writeD(0);
				w.writeD(0);
				w.writeD(item.price());
			}
			w.writeD(current.size());
			for (var item : current) {
				w.writeD(0);
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.bodyPart());
				w.writeH(item.enchant());
				w.writeD(0);
				w.writeD(0);
				w.writeD(item.price());
				w.writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** 0xfe:0x50 ExShowVariationMakeWindow: abre a janela de augmentacao de armas. */
	record ExShowVariationMakeWindow() implements GameServerPacket {
		public static final ExShowVariationMakeWindow STATIC_PACKET = new ExShowVariationMakeWindow();
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x50).toByteArray();
		}
	}

	/** 0xfe:0x51 ExShowVariationCancelWindow: abre a janela de cancelamento de augmentacao. */
	record ExShowVariationCancelWindow() implements GameServerPacket {
		public static final ExShowVariationCancelWindow STATIC_PACKET = new ExShowVariationCancelWindow();
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x51).toByteArray();
		}
	}

	/** 0xfe:0x52 ExPutItemResultForVariationMake: confirma o item alvo para augmentacao. */
	record ExPutItemResultForVariationMake(int itemObjId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x52).writeD(itemObjId).writeD(1).toByteArray();
		}
	}

	/** 0xfe:0x53 ExPutIntensiveResultForVariationMake: confirma a Life Stone e requisitos de Gemstones. */
	record ExPutIntensiveResultForVariationMake(int refinerItemObjId, int lifeStoneId, int gemstoneItemId, int gemstoneCount) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x53)
					.writeD(refinerItemObjId).writeD(lifeStoneId).writeD(gemstoneItemId).writeD(gemstoneCount).writeD(1)
					.toByteArray();
		}
	}

	/** 0xfe:0x54 ExPutCommissionResultForVariationMake: confirma o deposito de Gemstones. */
	record ExPutCommissionResultForVariationMake(int gemstoneItemObjId, int gemstoneCount, int gemstoneItemId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x54)
					.writeD(gemstoneItemObjId).writeD(gemstoneCount).writeD(1)
					.toByteArray();
		}
	}

	/** 0xfe:0x55 ExVariationResult: resultado da augmentacao (stat12, stat34, 1=sucesso, 0=falha). */
	record ExVariationResult(int stat12, int stat34, int success) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x55)
					.writeD(stat12).writeD(stat34).writeD(success)
					.toByteArray();
		}
	}

	/** 0xfe:0x56 ExPutItemResultForVariationCancel: confirma o item para remocao de augmentacao e custo. */
	record ExPutItemResultForVariationCancel(int itemObjId, long price) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x56)
					.writeD(0x40A97712).writeD(itemObjId).writeD(0x27).writeD(0x2006).writeQ(price).writeD(0x01)
					.toByteArray();
		}
	}

	/** 0xfe:0x57 ExVariationCancelResult: resultado da remocao de augmentacao (1=sucesso). */
	record ExVariationCancelResult(int result) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x57)
					.writeD(1).writeD(result)
					.toByteArray();
		}
	}

	/** 0x6e ShowBoard: exibe ou atualiza a tela da Comunidade BBS (Alt + B). */
	record ShowBoard(String htmlCode, String id) implements GameServerPacket {
		public ShowBoard(String htmlCode) {
			this(htmlCode, "101");
		}

		@Override
		public byte[] encode() {
			String fullContent = (id != null ? id : "101") + "\u0008" + (htmlCode != null ? htmlCode : "");
			return new PacketWriter()
					.writeC(0x6e)
					.writeC(0x01)
					.writeS("bypass _bbshome")
					.writeS("bypass _bbsgetfav")
					.writeS("bypass _bbsloc")
					.writeS("bypass _bbsclan")
					.writeS("bypass _bbsmemo")
					.writeS("bypass _maillist_0_1_0_")
					.writeS("bypass _friendlist_0_")
					.writeS("bypass bbs_add_fav")
					.writeS(fullContent)
					.toByteArray();
		}
	}

	/** 0x98 PlaySound: reproduz efeito sonoro ou trilha musical no cliente Interlude. */
	record PlaySound(int soundType, String soundFile, int hasCenterObject, int objectId, int x, int y, int z) implements GameServerPacket {
		public PlaySound(String soundFile) {
			this(0, soundFile, 0, 0, 0, 0, 0);
		}

		public PlaySound(int soundType, String soundFile) {
			this(soundType, soundFile, 0, 0, 0, 0, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0x98)
					.writeD(soundType)
					.writeS(soundFile != null ? soundFile : "")
					.writeD(hasCenterObject)
					.writeD(objectId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}
}
