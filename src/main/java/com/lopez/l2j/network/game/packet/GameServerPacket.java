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
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Pacotes enviados pelo game server ao cliente Interlude. Layouts portados 1:1
 * de
 * com.dream.game.network.serverpackets; campos de sistemas ainda nao migrados
 * (inventario, clan, skills,
 * cubics...) saem zerados, como o legado faria para um personagem novo sem
 * esses dados.
 */
public sealed interface GameServerPacket {

	byte[] encode();

	/**
	 * 0x00 KeyPacket: resposta ao ProtocolVersion; entrega a chave e liga a cifra.
	 */
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

	/**
	 * 0x13 CharSelectionInfo: lista de personagens da conta (paperdolls paralelos a
	 * characters).
	 */
	record CharSelectionInfo(String account, int sessionId, List<PlayerCharacter> characters,
			List<Paperdoll> paperdolls,
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
				int enchant = 0;
				int augId = 0;
				if (i < paperdolls.size() && paperdolls.get(i) != null) {
					enchant = Math.min(127, Math.max(0, paperdolls.get(i).enchant(ItemSlots.RHAND)));
				}
				if (c.inventory() != null) {
					var rw = c.inventory().paperdoll(ItemSlots.RHAND);
					if (rw != null && rw.augmentation() != null) {
						augId = rw.augmentation().attributes();
					} else {
						var lrw = c.inventory().paperdoll(ItemSlots.LRHAND);
						if (lrw != null && lrw.augmentation() != null) {
							augId = lrw.augmentation().attributes();
						}
					}
				}
				w.writeC(enchant).writeD(augId); // enchant effect, augmentation
			}
			return w.toByteArray();
		}
	}

	/**
	 * 17 object ids + 17 item ids na ordem {@link ItemSlots#VISIBLE_ORDER}; o 15o
	 * slot usa {@code twoHandSlot}
	 * (UserInfo manda RHAND, CharSelectionInfo manda LRHAND).
	 */
	private static void writePaperdoll(PacketWriter w, Paperdoll p, int twoHandSlot) {
		for (int slot : ItemSlots.VISIBLE_ORDER) {
			w.writeD(p != null ? p.objectId(slot == ItemSlots.LRHAND ? twoHandSlot : slot) : 0);
		}
		for (int slot : ItemSlots.VISIBLE_ORDER) {
			w.writeD(p != null ? p.itemId(slot == ItemSlots.LRHAND ? twoHandSlot : slot) : 0);
		}
	}

	private static void writePaperdollAugmentations(PacketWriter w, PlayerCharacter c) {
		for (int i = 0; i < 14; i++) {
			w.writeH(0x00);
		}
		int rhandAug = 0;
		int lrhandAug = 0;
		var inv = c != null ? c.inventory() : null;
		if (inv != null) {
			var rw = inv.paperdoll(ItemSlots.RHAND);
			if (rw != null && rw.augmentation() != null) {
				rhandAug = rw.augmentation().attributes();
			}
			var lrw = inv.paperdoll(ItemSlots.LRHAND);
			if (lrw != null && lrw.augmentation() != null) {
				lrhandAug = lrw.augmentation().attributes();
			} else if (rhandAug != 0 && rw != null && rw.template() != null
					&& rw.template().bodyPart() == ItemSlots.LRHAND) {
				lrhandAug = rhandAug;
			}
		}
		w.writeD(rhandAug); // augmentation RHAND
		for (int i = 0; i < 12; i++) {
			w.writeH(0x00);
		}
		w.writeD(lrhandAug); // augmentation LRHAND
		for (int i = 0; i < 4; i++) {
			w.writeH(0x00);
		}
	}

	private static List<Integer> resolvePlayerCubics(PlayerCharacter c) {
		if (c == null) {
			return List.of();
		}
		for (String methodName : new String[] { "cubicIds", "getCubicIds", "cubics", "getCubics" }) {
			try {
				var m = c.getClass().getMethod(methodName);
				Object val = m.invoke(c);
				if (val instanceof Collection<?> col) {
					List<Integer> out = new ArrayList<>(col.size());
					for (Object elem : col) {
						if (elem instanceof Number n) {
							out.add(n.intValue());
						} else if (elem != null) {
							int id = invokeInt(elem, "id", invokeInt(elem, "cubicId", 0));
							if (id > 0) {
								out.add(id);
							}
						}
					}
					return out;
				} else if (val instanceof Map<?, ?> map) {
					List<Integer> out = new ArrayList<>(map.size());
					for (Object key : map.keySet()) {
						if (key instanceof Number n) {
							out.add(n.intValue());
						}
					}
					return out;
				}
			} catch (Exception ignored) {
			}
		}
		return List.of();
	}

	private static int invokeInt(Object target, String methodName, int defaultValue) {
		if (target == null) {
			return defaultValue;
		}
		try {
			var m = target.getClass().getMethod(methodName);
			Object v = m.invoke(target);
			if (v instanceof Number n) {
				return n.intValue();
			}
			if (v instanceof Boolean b) {
				return b ? 1 : 0;
			}
		} catch (Exception ignored) {
		}
		return defaultValue;
	}

	private static long invokeLong(Object target, String methodName, long defaultValue) {
		if (target == null) {
			return defaultValue;
		}
		try {
			var m = target.getClass().getMethod(methodName);
			Object v = m.invoke(target);
			if (v instanceof Number n) {
				return n.longValue();
			}
		} catch (Exception ignored) {
		}
		return defaultValue;
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

	/**
	 * 0x24 CharDeleteFail (1 = falha geral, 2 = membro de clan, 3 = lider de clan).
	 */
	record CharDeleteFail(int reason) implements GameServerPacket {
		public static final int REASON_DELETION_FAILED = 0x01;
		public static final int REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER = 0x02;
		public static final int REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED = 0x03;

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x24).writeD(reason).toByteArray();
		}
	}

	/**
	 * 0xF8 SSQInfo (ceu dos Seven Signs): 256 = neutro, 257 = dusk/red, 258 =
	 * dawn/blue.
	 */
	record SsqInfo(int state) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int sky = state >= 256 ? state : (state == 2 ? 258 : (state == 1 ? 257 : 256));
			return new PacketWriter().writeC(0xf8).writeH(sky).toByteArray();
		}
	}

	/**
	 * 0x15 CharSelected: personagem escolhido; o cliente carrega o mapa e responde
	 * EnterWorld.
	 */
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

	/**
	 * 0x04 UserInfo: estado completo do proprio personagem (o cliente so "entra" no
	 * mundo apos recebe-lo).
	 */
	record UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad, PlayerStats stats,
			int clanCrestId, int allyId, int allyCrestId, List<Integer> cubics)
			implements GameServerPacket {
		static final int WALK_SPEED = 80;
		static final int INVENTORY_LIMIT = 80;
		static final int NAME_COLOR = 0xFFFFFF;
		static final int TITLE_COLOR = 0xFFFF77;

		public UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad, PlayerStats stats,
				int clanCrestId, int allyId, int allyCrestId) {
			this(c, t, paperdoll, currentLoad, stats, clanCrestId, allyId, allyCrestId, resolvePlayerCubics(c));
		}

		public UserInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory().paperdollView(), c.inventory().currentLoad(), PlayerStats.calculate(c, t),
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
		}

		public UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad) {
			this(c, t, paperdoll, currentLoad, PlayerStats.calculate(c, t),
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
		}

		public UserInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int currentLoad, PlayerStats stats) {
			this(c, t, paperdoll, currentLoad, stats,
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x04);
			w.writeD(c.x()).writeD(c.y()).writeD(c.z()).writeD(c.heading()).writeD(c.objectId());
			w.writeS(c.name()).writeD(c.race()).writeD(c.female() ? 1 : 0).writeD(c.classId());
			w.writeD(c.level()).writeQ(c.exp());
			int str = stats != null ? stats.str() : (t.str() + c.hennaSTR() + c.augSTR());
			int dex = stats != null ? stats.dex() : (t.dex() + c.hennaDEX() + c.augDEX());
			int con = stats != null ? stats.con() : (t.con() + c.hennaCON() + c.augCON());
			int intel = stats != null ? stats.intel() : (t.intel() + c.hennaINT() + c.augINT());
			int wit = stats != null ? stats.wit() : (t.wit() + c.hennaWIT() + c.augWIT());
			int men = stats != null ? stats.men() : (t.men() + c.hennaMEN() + c.augMEN());
			w.writeD(str).writeD(dex).writeD(con).writeD(intel).writeD(wit).writeD(men);
			w.writeD(c.maxHp()).writeD((int) c.currentHp()).writeD(c.maxMp()).writeD((int) c.currentMp());
			long fallbackLoad = (long) ((double) t.maxLoad()
					* (Config.ALT_WEIGHT_LIMIT > 0 ? Config.ALT_WEIGHT_LIMIT : 1.0));
			if (Config.ALT_WEIGHT_LIMIT > 1.0 && (fallbackLoad >= Integer.MAX_VALUE || fallbackLoad <= 0)) {
				fallbackLoad = Integer.MAX_VALUE;
			}
			int maxLoad = (stats != null && stats.maxLoad() > 0)
					? stats.maxLoad()
					: (int) Math.min(Integer.MAX_VALUE, Math.max(1L, fallbackLoad));
			w.writeD(c.sp()).writeD(currentLoad).writeD(maxLoad);
			w.writeD(0x28); // valor fixo do legado (posicao 0x28 do paperdoll)
			writePaperdoll(w, paperdoll, ItemSlots.RHAND);
			writePaperdollAugmentations(w, c);
			w.writeD(stats.pAtk()).writeD(stats.pAtkSpd()).writeD(stats.pDef()).writeD(stats.evasion())
					.writeD(stats.accuracy())
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
			boolean isFlying = c.mountNpcId() == 12621;
			int flyRun = isFlying ? run : 0;
			int flyWalk = isFlying ? walk : 0;

			w.writeD(run).writeD(walk).writeD(swimRun).writeD(swimWalk).writeD(0).writeD(0);
			w.writeD(flyRun).writeD(flyWalk); // fly speeds
			w.writeF(moveMultiplier).writeF(atkSpeedMultiplier); // move / attack speed multipliers
			w.writeF(t.collisionRadius(c.female())).writeF(t.collisionHeight(c.female()));
			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face()).writeD(c.isGm() ? 1 : 0);
			w.writeS(c.title());
			var clan = c.getClan();
			int effClanCrest = clanCrestId != 0 ? clanCrestId : (clan != null ? clan.crestId() : 0);
			int effAllyId = allyId != 0 ? allyId : (clan != null ? clan.allyId() : 0);
			int effAllyCrest = allyCrestId != 0 ? allyCrestId : (clan != null ? clan.allyCrestId() : 0);
			w.writeD(c.clanId()).writeD(effClanCrest).writeD(effAllyId).writeD(effAllyCrest);
			int relation = c.isClanLeader() ? 0x40 : 0;
			int siegeState = invokeInt(c, "siegeState", 0);
			if (siegeState == 1) {
				relation |= 0x180;
			} else if (siegeState == 2) {
				relation |= 0x80;
			}
			w.writeD(relation); // relation (0x40 = clan leader, siege flags)
			int mountType = c.mountNpcId() == 12621 ? 2 : (c.mountNpcId() > 0 ? 1 : 0);
			boolean hasDwarvenCraft = (t != null && t.canCraft()) || c.canCraft() || c.skillLevel(172) > 0
					|| com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(c);
			w.writeC(mountType).writeC(c.privateStoreType()).writeC(hasDwarvenCraft ? 1 : 0); // mount, private store,
																								// dwarven craft
			w.writeD(c.pkKills()).writeD(c.pvpKills());
			List<Integer> activeCubics = (cubics != null && !cubics.isEmpty()) ? cubics : resolvePlayerCubics(c);
			w.writeH(activeCubics.size()); // cubics
			for (int cubicId : activeCubics) {
				w.writeH(cubicId);
			}
			w.writeC(invokeInt(c, "partyMatchRoom", 0) > 0 ? 1 : 0);
			w.writeD(c.abnormalEffect()); // abnormal effect
			w.writeC(0);
			int privs = c.isClanLeader() ? 0x7ffffe : c.clanPrivs();
			w.writeD(privs); // clan privileges
			w.writeH(c.recomLeft()).writeH(c.recomHave()); // recom left/have
			w.writeD(c.mountNpcId() > 0 ? c.mountNpcId() + 1000000 : 0); // mount npc id
			w.writeH(c != null ? c.maxInventorySlots() : INVENTORY_LIMIT);
			w.writeD(c.classId()).writeD(0);
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			int enchantEffect = 0;
			if (paperdoll != null && paperdoll.enchant(ItemSlots.RHAND) > 0) {
				enchantEffect = Math.min(127, paperdoll.enchant(ItemSlots.RHAND));
			} else if (c.inventory() != null) {
				var rw = c.inventory().paperdoll(ItemSlots.RHAND);
				if (rw != null) {
					enchantEffect = Math.min(127, Math.max(0, rw.enchant()));
				}
			}
			w.writeC(c.isMounted() ? 0 : enchantEffect); // enchant effect (weapon glow: 0=none, 4-15=blue, 16+=red)
			w.writeC(c.team()); // team
			int largeCrestId = (clan != null) ? clan.largeCrestId() : 0;
			w.writeD(largeCrestId); // large clan crest
			w.writeC(c.isNoble() ? 1 : 0).writeC(c.isHero() ? 1 : 0); // noble, hero
			w.writeC(c.isFishing() ? 1 : 0).writeD(c.fishX()).writeD(c.fishY()).writeD(c.fishZ()); // fishing
			w.writeD(c.isGm() ? 0x00FFFF : (c.nameColor() != 0 ? c.nameColor() : NAME_COLOR));
			w.writeC(c.running() ? 1 : 0);
			w.writeD(c.pledgeRank()).writeD(c.subPledge()); // pledge class, pledge type
			w.writeD(c.isGm() ? 0x00FF77 : (c.titleColor() != 0 ? c.titleColor() : TITLE_COLOR));
			w.writeD(invokeInt(c, "cursedWeaponLevel", 0)); // cursed weapon level
			return w.toByteArray();
		}
	}

	/**
	 * Linha de item dos pacotes ItemList/InventoryUpdate (foto imutavel do
	 * ItemInstance).
	 */
	record ItemInfo(int change, int type1, int objectId, int displayId, int count, int type2, int customType1,
			boolean equipped, int bodyPart, int enchant, int customType2, int augmentation, int mana) {
		public static final int ADDED = 1;
		public static final int MODIFIED = 2;
		public static final int REMOVED = 3;

		public static ItemInfo of(ItemInstance i, int change) {
			var t = i.template();
			int count = (change == REMOVED) ? 0 : i.count();
			boolean equipped = (change == REMOVED) ? false : i.isEquipped();
			return new ItemInfo(change, t.type1(), i.objectId(), t.displayId(), count, t.type2(),
					i.customType1(), equipped, t.bodyPart(), i.enchant(), i.customType2(),
					i.augmentation() != null ? i.augmentation().attributes() : 0, i.mana());
		}

		void write(PacketWriter w) {
			w.writeH(type1).writeD(objectId).writeD(displayId).writeD(count).writeH(type2).writeH(customType1)
					.writeH(equipped ? 1 : 0).writeD(bodyPart).writeH(enchant).writeH(customType2)
					.writeD(augmentation).writeD(mana);
		}
	}

	/** 0x1b ItemList: inventario completo. */
	record ItemList(List<ItemInfo> items, boolean showWindow) implements GameServerPacket {
		static final int MAX_ITEMS = 1000;

		public ItemList {
			items = List.copyOf(items.size() > MAX_ITEMS ? items.subList(0, MAX_ITEMS) : items);
		}

		public ItemList(PlayerCharacter player, boolean showWindow) {
			this(player != null && player.inventory() != null
					? player.inventory().items().stream().map(i -> ItemInfo.of(i, 0)).toList()
					: List.of(), showWindow);
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

	/**
	 * 0x27 InventoryUpdate: so os itens que mudaram (1 = novo, 2 = modificado, 3 =
	 * removido).
	 */
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

	/**
	 * 0x64 SystemMessage: mensagem do systemmsg.dat do cliente com parametros
	 * tipados.
	 */
	record SystemMessage(int id, List<Param> params) implements GameServerPacket {
		public static final int YOU_HAVE_BEEN_DISCONNECTED = 0;
		public static final int DISCONNECTED_FROM_SERVER = 127;
		public static final int YOU_PICKED_UP_S1_ADENA = 28;
		public static final int YOU_PICKED_UP_S1_S2 = 29;
		public static final int YOU_PICKED_UP_S1 = 30;
		public static final int YOU_DID_S1_DMG = 35;
		public static final int S1_GAVE_YOU_S2_DMG = 36;
		public static final int EARNED_S1_EXPERIENCE = 45;
		public static final int USE_S1 = 46;
		public static final int S1_PREPARED_FOR_REUSE = 48;
		public static final int CANNOT_DISCARD_THIS_ITEM = 98;
		public static final int S1_HAS_WORN_OFF = 92;
		public static final int CANT_LOGOUT_WHILE_IN_COMBAT = 101;
		public static final int CANT_RESTART_WHILE_IN_COMBAT = 102;
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
		public static final int TOO_FAR_FROM_NPC = 548;
		public static final int NOT_ENOUGH_MP = 24;
		public static final int NOT_ENOUGH_HP = 23;
		public static final int NOT_ENOUGH_ITEMS = 351;
		public static final int CASTING_INTERRUPTED = 27;
		public static final int TARGET_IS_INCORRECT = 144;
		public static final int CANT_SEE_TARGET = 181;
		public static final int ITEM_MISSING_TO_LEARN_SKILL = 276;
		public static final int LEARNED_SKILL_S1 = 277;
		public static final int NOT_ENOUGH_SP_TO_LEARN_SKILL = 278;
		public static final int S1_DISAPPEARED = 302;
		public static final int UNABLE_TO_UNLOCK_DOOR = 319;
		public static final int FAILED_TO_UNLOCK_DOOR = 320;
		public static final int DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN = 607;
		public static final int EFFECT_S1_DISAPPEARED = 749;
		public static final int NO_MORE_SKILLS_TO_LEARN = 750;
		public static final int S2_HP_RESTORED_BY_S1 = 1067;
		public static final int CRITICAL_HIT_MAGIC = 1280;
		public static final int AVOIDED_S1_ATTACK = 43;
		public static final int CRITICAL_HIT = 44;
		public static final int S1_WAS_UNAFFECTED_BY_S2 = 139;
		public static final int DRAIN_HALF_SUCCESFUL = 156;
		public static final int RESISTED_S1_DRAIN = 157;
		public static final int ATTACK_FAILED = 158;
		public static final int RESISTED_S1_MAGIC = 159;
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
		public static final int THE_SYMBOL_HAS_BEEN_ADDED = 877;
		public static final int THE_SYMBOL_HAS_BEEN_DELETED = 878;
		public static final int THE_SYMBOL_CANNOT_BE_DRAWN = 899;
		public static final int NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL = 900;
		public static final int YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL = 1444;
		public static final int ADD_NEW_SUBCLASS = 1269;
		public static final int SUBCLASS_TRANSFER_COMPLETED = 1270;
		public static final int FORCE_INCREASED_TO_S1 = 323;
		public static final int FORCE_MAXLEVEL_REACHED = 324;
		public static final int YOU_MAY_CREATE_UP_TO_48_MACROS = 797;
		public static final int INVALID_MACRO = 810;
		public static final int MACRO_DESCRIPTION_MAX_32_CHARS = 837;
		public static final int ENTER_THE_MACRO_NAME = 838;
		public static final int MACRO_NAME_ALREADY_USED = 839;
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
		public static final int NOT_AUTHORIZED_TO_BESTOW_RIGHTS = 794;
		public static final int YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT = 794;
		public static final int RECIPES_CANT_BE_REGISTERED = 840;
		public static final int RECIPE_ALREADY_REGISTERED = 840;
		public static final int NO_FUTHER_RECIPES_CAN_BE_ADDED = 841;
		public static final int S1_ADDED = 851;
		public static final int RECIPE_INCORRECT = 852;
		public static final int CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING = 853;
		public static final int MISSING_S2_S1_TO_CREATE = 854;
		public static final int UP_TO_S1_RECIPES_CAN_REGISTER = 894;
		public static final int NO_RECIPES_REGISTERED = 895;
		public static final int CANT_REGISTER_NO_ABILITY_TO_CRAFT = 1061;
		public static final int CREATE_LVL_TOO_LOW_TO_REGISTER = 404;
		public static final int ITEM_MIXING_FAILED = 719;
		public static final int S2_S1_DISAPPEARED = 301;
		public static final int S2_CREATED_FOR_S1_FOR_S3_ADENA = 1145;
		public static final int S1_CREATED_S2_FOR_S3_ADENA = 1146;
		public static final int S2_S3_S_CREATED_FOR_S1_FOR_S4_ADENA = 1147;
		public static final int S1_CREATED_S2_S3_S_FOR_S4_ADENA = 1148;
		public static final int CREATION_OF_S2_FOR_S1_AT_S3_ADENA_FAILED = 1149;
		public static final int S1_FAILED_TO_CREATE_S2_FOR_S3_ADENA = 1150;
		public static final int NO_PRIVATE_WORKSHOP_HERE = 1297;
		public static final int CANT_CRAFT_DURING_COMBAT = 1126;
		public static final int EARNED_ITEM_S1 = 54;
		public static final int YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED = 1036;
		public static final int S1_S2_SUCCESSFULLY_ENCHANTED = 63;
		public static final int ENCHANTMENT_FAILED_S1_EVAPORATED = 64;
		public static final int ENCHANTMENT_FAILED_S1_S2_EVAPORATED = 65;
		public static final int CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE = 1065;
		public static final int CRYSTALLIZE_LEVEL_TOO_LOW = 562;
		public static final int S1_CRYSTALLIZED = 1258;
		public static final int WAREHOUSE_FULL = 130;
		public static final int INAPPROPRIATE_ENCHANT_CONDITION = 355;
		public static final int BLESSED_ENCHANT_FAILED = 1517;
		public static final int SELECT_THE_ITEM_TO_BE_AUGMENTED = 1957;
		public static final int SELECT_THE_CATALYST_FOR_AUGMENTATION = 1958;
		public static final int REQUIRES_S1_S2 = 1959;
		public static final int THIS_IS_NOT_A_SUITABLE_ITEM = 1960;
		public static final int GEMSTONE_QUANTITY_IS_INCORRECT = 1961;
		public static final int THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED = 1962;
		public static final int SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION = 1963;
		public static final int YOU_HAVE_BECOME_A_NOBLESSE = 1665;
		public static final int AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM = 1964;
		public static final int AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1 = 1965;
		public static final int ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN = 1970;
		public static final int PRESS_THE_AUGMENT_BUTTON_TO_BEGIN = 1984;
		public static final int AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS = 2001;
		public static final int SPOIL_SUCCESS = 612;
		public static final int ALREADY_SPOILED = 357;
		public static final int SWEEPER_FAILED_TARGET_NOT_SPOILED = 343;
		public static final int S1_SWEEPED_UP_S3_S2 = 608;
		public static final int S1_SWEEPED_UP_S2 = 609;
		public static final int SWEEP_NOT_ALLOWED = 683;
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
		public static final int S1_IS_BUSY_TRY_LATER = 153;
		public static final int ALREADY_TRADING = 118;
		public static final int TRADE_CANCELLED = 120;
		public static final int TRADE_SUCCESSFUL = 121;
		public static final int EARNED_S1_RAID_POINTS = 1725;
		public static final int YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE = 84;
		public static final int YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE = 85;
		public static final int S2_MP_HAS_BEEN_DRAINED_BY_S1 = 970;
		public static final int YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1 = 1867;
		public static final int SKILL_READY_TO_USE_AGAIN = 2015;
		public static final int S1 = 1987;
		public static final int S1_ADENA_DISAPPEARED = 672;
		public static final int CONTRIB_SCORE_INCREASED_BY_S1 = 1267;
		public static final int SEVENSIGNS_PARTECIPATION_DAWN = 1273;
		public static final int SEVENSIGNS_PARTECIPATION_DUSK = 1274;
		public static final int FIGHT_FOR_AVARICE = 1275;
		public static final int FIGHT_FOR_GNOSIS = 1276;
		public static final int FIGHT_FOR_STRIFE = 1277;
		public static final int CONTRIB_SCORE_EXCEEDED = 1279;

		public static SystemMessage sendString(String text) {
			return of(S1, new Text(text != null ? text : ""));
		}

		public String text() {
			for (Param p : params) {
				if (p instanceof Text t) {
					return t.value();
				}
			}
			return "";
		}

		public SystemMessage(int id) {
			this(id, List.of());
		}

		public static SystemMessage id(int id) {
			return new SystemMessage(id, List.of());
		}

		public sealed interface Param {
		}

		public record Text(String value) implements Param {
		}

		public record Number(int value) implements Param {
		}

		public record ItemNumber(int value) implements Param {
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
					case ItemNumber n -> w.writeD(6).writeD(n.value());
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

	/**
	 * 0x80 QuestList: envia lista de quests ativas do personagem (formato Interlude
	 * C6).
	 */
	record QuestList(List<QuestEntry> quests) implements GameServerPacket {
		public record QuestEntry(int questId, int cond) {
		}

		public QuestList() {
			this(List.of());
		}

		@Override
		public byte[] encode() {
			PacketWriter pw = new PacketWriter().writeC(0x80);
			if (quests == null || quests.isEmpty()) {
				pw.writeH(0);
			} else {
				pw.writeH(quests.size());
				for (QuestEntry q : quests) {
					pw.writeD(q.questId());
					pw.writeD(q.cond());
				}
			}
			return pw.toByteArray();
		}
	}

	/** 0x45 ShortCutInit: atalhos da barra rapida do jogador. */
	record ShortCutInit(List<com.lopez.l2j.game.shortcut.ShortCut> shortcuts, PlayerCharacter player)
			implements GameServerPacket {
		public ShortCutInit() {
			this(List.of(), null);
		}

		public ShortCutInit(List<com.lopez.l2j.game.shortcut.ShortCut> shortcuts) {
			this(shortcuts, null);
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
						int sharedGroup = invokeInt(sc, "sharedReuseGroup", -1);
						int curReuse = invokeInt(sc, "remainingReuse", 0);
						int maxReuse = invokeInt(sc, "reuseDelay", 0);
						int augId = invokeInt(sc, "augmentationId", 0);
						if (augId == 0 && player != null && player.inventory() != null) {
							var item = player.inventory().getByObjectId(sc.id());
							if (item != null && item.augmentation() != null) {
								augId = item.augmentation().attributes();
							}
						}
						w.writeD(sc.id());
						w.writeD(sc.characterType() > 0 ? sc.characterType() : 0x01);
						w.writeD(sharedGroup);
						w.writeD(curReuse);
						w.writeD(maxReuse);
						w.writeH(augId & 0xFFFF);
						w.writeH(augId >>> 16);
					}
					case 2 -> {
						w.writeD(sc.id());
						w.writeD(sc.level());
						w.writeC(0x00);
						w.writeD(sc.characterType() > 0 ? sc.characterType() : 0x01);
					}
					default -> {
						w.writeD(sc.id());
						w.writeD(sc.characterType() > 0 ? sc.characterType() : 0x01);
					}
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x44 ShortCutRegister: confirmacao de registro de atalho. */
	record ShortCutRegister(com.lopez.l2j.game.shortcut.ShortCut shortcut, PlayerCharacter player)
			implements GameServerPacket {
		public ShortCutRegister(com.lopez.l2j.game.shortcut.ShortCut shortcut) {
			this(shortcut, null);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x44);
			w.writeD(shortcut.type());
			w.writeD(shortcut.globalSlot());
			switch (shortcut.type()) {
				case 1 -> {
					int sharedGroup = invokeInt(shortcut, "sharedReuseGroup", -1);
					int curReuse = invokeInt(shortcut, "remainingReuse", 0);
					int maxReuse = invokeInt(shortcut, "reuseDelay", 0);
					int augId = invokeInt(shortcut, "augmentationId", 0);
					if (augId == 0 && player != null && player.inventory() != null) {
						var item = player.inventory().getByObjectId(shortcut.id());
						if (item != null && item.augmentation() != null) {
							augId = item.augmentation().attributes();
						}
					}
					w.writeD(shortcut.id());
					w.writeD(shortcut.characterType());
					w.writeD(sharedGroup);
					w.writeD(curReuse);
					w.writeD(maxReuse);
					w.writeD(augId);
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

		public MagicSkillUse(int charObjId, int targetObjId, int skillId, int skillLevel, int hitTime, int reuseDelay,
				int x, int y, int z) {
			this(charObjId, targetObjId, skillId, skillLevel, hitTime, reuseDelay, x, y, z, x, y, z);
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

	/**
	 * 0x49 MagicSkillCanceld: cancela a animacao/conjuracao de habilidade no
	 * cliente.
	 */
	record MagicSkillCanceld(int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x49).writeD(objectId).toByteArray();
		}
	}

	/**
	 * 0xFE:0x12 ExAutoSoulShot: acende/apaga o icone de uso automatico do shot
	 * (type 1 = on, 0 = off).
	 */
	record ExAutoSoulShot(int itemId, int type) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x12).writeD(itemId).writeD(type).toByteArray();
		}
	}

	/**
	 * 0x7f MagicEffectIcons: icones de buffs e debuffs na barra (duracao restante
	 * em segundos).
	 */
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

	/** Alias de AbnormalStatusUpdate (MagicEffectIcons opcode 0x7f). */
	record AbnormalStatusUpdate(List<MagicEffectIcons.Icon> icons) implements GameServerPacket {
		public AbnormalStatusUpdate {
			icons = List.copyOf(icons);
		}

		@Override
		public byte[] encode() {
			return new MagicEffectIcons(icons).encode();
		}
	}

	/**
	 * 0x8a AcquireSkillList: janela de aprendizado do treinador (type 0 = skills de
	 * classe).
	 */
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

	/**
	 * 0x8b AcquireSkillInfo: custo e requisitos (livros) do skill selecionado na
	 * janela do treinador.
	 */
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

	/**
	 * 0x76 MagicSkillLaunched: momento em que o skill e disparado sobre os alvos.
	 */
	record MagicSkillLaunched(int charObjId, int skillId, int skillLevel, List<Integer> targets)
			implements GameServerPacket {
		public MagicSkillLaunched {
			targets = List.copyOf(targets);
		}

		public MagicSkillLaunched(int charObjId, int skillId, int skillLevel, int[] targetIds) {
			this(charObjId, skillId, skillLevel,
					targetIds != null ? java.util.Arrays.stream(targetIds).boxed().toList() : List.of());
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

	/**
	 * 0x6d SetupGauge: barra de progresso/conjuracao (0 = azul, 1 = vermelho, 2 =
	 * ciano/agua, 3 = verde).
	 */
	record SetupGauge(int color, int time) implements GameServerPacket {
		public static final int BLUE = 0;
		public static final int RED = 1;
		public static final int CYAN = 2;
		public static final int GREEN = 3;

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x6d).writeD(color).writeD(time).writeD(time).toByteArray();
		}
	}

	/**
	 * 0xe4 HennaInfo: informacoes de tatuagens e modificadores de status do jogador
	 */
	record HennaInfo(int intAdd, int strAdd, int conAdd, int menAdd, int dexAdd, int witAdd,
			int maxSlots, List<EquippedSlot> slots) implements GameServerPacket {

		public record EquippedSlot(int symbolId, boolean activeForClass) {
		}

		public HennaInfo() {
			this(0, 0, 0, 0, 0, 0, 0, List.of());
		}

		public HennaInfo(PlayerCharacter player) {
			this(player, 2, null);
		}

		public HennaInfo(PlayerCharacter player, int classLevel,
				com.lopez.l2j.game.henna.HennaTreeTable treeTable) {
			this(player.hennaINT(), player.hennaSTR(), player.hennaCON(), player.hennaMEN(),
					player.hennaDEX(), player.hennaWIT(),
					classLevel < 1 ? 0 : (classLevel == 1 ? 2 : 3),
					buildSlots(player, treeTable));
		}

		private static List<EquippedSlot> buildSlots(PlayerCharacter player,
				com.lopez.l2j.game.henna.HennaTreeTable treeTable) {
			List<EquippedSlot> list = new java.util.ArrayList<>();
			for (int id : player.hennas()) {
				if (id > 0) {
					boolean allowed = treeTable == null || treeTable.isAllowed(player.classId(), id);
					list.add(new EquippedSlot(id, allowed));
				}
			}
			return list;
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
					.writeD(maxSlots)
					.writeD(slots.size());
			for (var s : slots) {
				w.writeD(s.symbolId())
						.writeD(s.activeForClass() ? s.symbolId() : 0);
			}
			return w.toByteArray();
		}
	}

	/** 0xe2 HennaEquipList: lista de tatuagens disponiveis no Symbol Maker */
	record HennaEquipList(int adena, int emptySlots, List<com.lopez.l2j.game.henna.Henna> hennas)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xe2)
					.writeD(adena)
					.writeD(emptySlots);
			if (hennas != null && !hennas.isEmpty()) {
				w.writeD(hennas.size());
				for (var h : hennas) {
					w.writeD(h.symbolId())
							.writeD(h.dyeId())
							.writeD(h.dyeAmount())
							.writeD(h.price())
							.writeD(1);
				}
			} else {
				w.writeD(0);
			}
			return w.toByteArray();
		}
	}

	/** 0xe3 HennaItemInfo: detalhes e alteracao de atributos de uma tatuagem */
	record HennaItemInfo(com.lopez.l2j.game.henna.Henna henna, PlayerCharacter player, CharTemplate tpl)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			int curInt = tpl != null ? tpl.intel() + player.hennaINT() : player.hennaINT();
			int curStr = tpl != null ? tpl.str() + player.hennaSTR() : player.hennaSTR();
			int curCon = tpl != null ? tpl.con() + player.hennaCON() : player.hennaCON();
			int curMen = tpl != null ? tpl.men() + player.hennaMEN() : player.hennaMEN();
			int curDex = tpl != null ? tpl.dex() + player.hennaDEX() : player.hennaDEX();
			int curWit = tpl != null ? tpl.wit() + player.hennaWIT() : player.hennaWIT();
			int adena = (int) (player.inventory() != null ? player.inventory().adena() : 0);

			return new PacketWriter().writeC(0xe3)
					.writeD(henna.symbolId())
					.writeD(henna.dyeId())
					.writeD(henna.dyeAmount())
					.writeD(henna.price())
					.writeD(1)
					.writeD(adena)
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

	/**
	 * 0xe5 HennaUnequipList: lista de tatuagens atualmente equipadas para remocao
	 * no Symbol Maker
	 */
	record HennaUnequipList(int adena, int emptySlots, List<com.lopez.l2j.game.henna.Henna> hennas)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xe5)
					.writeD(adena)
					.writeD(emptySlots)
					.writeD(hennas != null ? hennas.size() : 0);
			if (hennas != null) {
				for (var h : hennas) {
					w.writeD(h.symbolId())
							.writeD(h.dyeId())
							.writeD(h.dyeAmount() / 2)
							.writeD(h.price() / 5)
							.writeD(1);
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xe6 HennaUnequipInfo: detalhes e alteracao de atributos apos remover uma
	 * tatuagem
	 */
	record HennaUnequipInfo(com.lopez.l2j.game.henna.Henna henna, PlayerCharacter player, CharTemplate tpl)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			int curInt = tpl != null ? tpl.intel() + player.hennaINT() : player.hennaINT();
			int curStr = tpl != null ? tpl.str() + player.hennaSTR() : player.hennaSTR();
			int curCon = tpl != null ? tpl.con() + player.hennaCON() : player.hennaCON();
			int curMen = tpl != null ? tpl.men() + player.hennaMEN() : player.hennaMEN();
			int curDex = tpl != null ? tpl.dex() + player.hennaDEX() : player.hennaDEX();
			int curWit = tpl != null ? tpl.wit() + player.hennaWIT() : player.hennaWIT();
			int adena = (int) (player.inventory() != null ? player.inventory().adena() : 0);

			return new PacketWriter().writeC(0xe6)
					.writeD(henna.symbolId())
					.writeD(henna.dyeId())
					.writeD(henna.dyeAmount() / 2)
					.writeD(henna.price() / 5)
					.writeD(1)
					.writeD(adena)
					.writeD(curInt)
					.writeC(curInt - henna.statInt())
					.writeD(curStr)
					.writeC(curStr - henna.statStr())
					.writeD(curCon)
					.writeC(curCon - henna.statCon())
					.writeD(curMen)
					.writeC(curMen - henna.statMen())
					.writeD(curDex)
					.writeC(curDex - henna.statDex())
					.writeD(curWit)
					.writeC(curWit - henna.statWit())
					.toByteArray();
		}
	}

	/** 0xF3 EtcStatusUpdate: penalidades de peso (0-4), grau (0-4), charges etc. */
	record EtcStatusUpdate(int charges, int weightPenalty, int messageRefusal, int dangerZone, int expertisePenalty,
			int charmOfCourage, int deathPenalty) implements GameServerPacket {
		public EtcStatusUpdate() {
			this(0, 0, 0, 0, 0, 0, 0);
		}

		public EtcStatusUpdate(int weightPenalty, int expertisePenalty) {
			this(0, weightPenalty, 0, 0, expertisePenalty, 0, 0);
		}

		public EtcStatusUpdate(com.lopez.l2j.game.model.PlayerCharacter c, com.lopez.l2j.game.model.PlayerStats stats) {
			this(c != null ? c.charges() : 0,
					stats != null ? stats.weightPenalty() : 0,
					(c != null && (c.isBlockingAll() || invokeInt(c, "messageRefusal", 0) != 0)) ? 1 : 0,
					invokeInt(c, "inDangerArea", invokeInt(c, "dangerZone", 0)),
					stats != null ? stats.gradePenalty() : 0,
					invokeInt(c, "charmOfCourage", 0),
					invokeInt(c, "deathPenaltyBuffLevel", invokeInt(c, "deathPenalty", 0)));
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf3).writeD(charges).writeD(weightPenalty).writeD(messageRefusal)
					.writeD(dangerZone).writeD(expertisePenalty).writeD(charmOfCourage).writeD(deathPenalty)
					.toByteArray();
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
			int inv = c != null ? c.maxInventorySlots() : Config.MAX_INVENTORY_SLOTS_FOR_OTHER;
			int wh = isDwarf ? Config.MAX_WAREHOUSE_SLOTS_FOR_DWARF : Config.MAX_WAREHOUSE_SLOTS_FOR_OTHER;
			int freight = Config.MAX_WAREHOUSE_FREIGHT_SLOTS;
			int pSell = isDwarf ? Config.MAX_PVT_STORE_SELL_SLOTS_DWARF : Config.MAX_PVT_STORE_SELL_SLOTS_OTHER;
			int pBuy = isDwarf ? Config.MAX_PVT_STORE_BUY_SLOTS_DWARF : Config.MAX_PVT_STORE_BUY_SLOTS_OTHER;
			int dwarfRec = Config.DWARF_RECIPE_LIMIT;
			int commonRec = Config.COMMON_RECIPE_LIMIT;
			if (c != null) {
				wh = (int) Math.round(PlayerStats.applyStat(c, "whLimit", wh));
				freight = (int) Math.round(PlayerStats.applyStat(c, "FreightLimit", freight));
				pSell = (int) Math.round(PlayerStats.applyStat(c, "PrivateSellLimit", pSell));
				pBuy = (int) Math.round(PlayerStats.applyStat(c, "PrivateBuyLimit", pBuy));
				dwarfRec = (int) Math.round(PlayerStats.applyStat(c, "DwarfRecipeLimit", dwarfRec));
				commonRec = (int) Math.round(PlayerStats.applyStat(c, "CommonRecipeLimit", commonRec));
			}
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
	record FriendItem(int charId, String name, boolean online, int friendId) {
		public String friendName() {
			return name;
		}
	}

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

	/**
	 * 0xEC ClientSetTime: hora do mundo em minutos do dia de jogo e velocidade (6).
	 */
	record ClientSetTime(int gameTime) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xec).writeD(gameTime).writeD(6).toByteArray();
		}
	}

	/**
	 * 0xFE:0x1B ExSendManorList: castelos com manor (o cliente pede logo apos
	 * CharSelected).
	 */
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
		public static final int HERO_VOICE = 17;
		public static final int CRITICAL_ANNOUNCE = 18;

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

	/**
	 * 0x26 ServerClose: fecha a conexao e faz o cliente exibir a caixa de dialogo
	 * "You have been disconnected from the server."
	 */
	record ServerClose() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new byte[] { 0x26 };
		}
	}

	/** 0x7e LeaveWorld: o cliente sai do mundo / fecha para o desktop. */
	record LeaveWorld() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new byte[] { 0x7e };
		}
	}

	/** 0x16 NpcInfo: visualizacao de NPC/monstro no mundo. */
	record NpcInfo(NpcInstance npc) implements GameServerPacket {
		public static NpcInfo of(NpcInstance npc) {
			return new NpcInfo(npc);
		}

		public static GameServerPacket forSummon(com.lopez.l2j.game.summon.SummonInstance summon) {
			return new SummonNpcInfo(summon);
		}

		@Override
		public byte[] encode() {
			var t = npc.template();
			PacketWriter w = new PacketWriter().writeC(0x16);
			w.writeD(npc.objectId());
			w.writeD(t.idTemplate() + 1000000);
			w.writeD(t.isAttackable() ? 1 : 0);
			w.writeD(npc.x()).writeD(npc.y()).writeD(npc.z()).writeD(npc.heading());
			w.writeD(0x00);
			w.writeD(npc.mAtkSpd()).writeD(npc.pAtkSpd());
			w.writeD(npc.runSpd()).writeD(npc.walkSpd());
			w.writeD(npc.runSpd()).writeD(npc.walkSpd()); // swim run/walk
			w.writeD(npc.runSpd()).writeD(npc.walkSpd()); // fl run/walk
			w.writeD(npc.runSpd()).writeD(npc.walkSpd()); // fly run/walk
			double moveMult = t.runSpd() > 0 ? (double) npc.runSpd() / t.runSpd() : 1.1;
			double atkMult = t.pAtkSpd() > 0 ? (double) npc.pAtkSpd() / t.pAtkSpd() : (npc.pAtkSpd() / 277.478340719);
			w.writeF(moveMult > 0 ? moveMult : 1.1);
			w.writeF(atkMult > 0 ? atkMult : 1.0);
			w.writeF(t.collisionRadius()).writeF(t.collisionHeight());
			w.writeD(t.rhand()).writeD(t.armor()).writeD(t.lhand());
			w.writeC(1); // name above char
			w.writeC(npc.isRunning() ? 1 : 0);
			w.writeC(npc.isInCombat() ? 1 : 0);
			w.writeC(npc.isDead() ? 1 : 0);
			w.writeC(npc.isSpoiled() ? 1 : 0); // isSummoned / isSpoiled (ativa textura azulada no cliente)
			String name = (t.serverSideName() || t.id() >= 50000 || t.id() != t.idTemplate()) ? t.name() : "";
			String title = t.serverSideTitle() ? t.title() : "";
			if (npc.isChampion()) {
				String cTitle = npc.championTitle() != null ? npc.championTitle() : "Champion";
				boolean isAggressive = t.aggroRange() > 0 && !t.isFriendlyMob();
				String lvlTitle = "Lv " + t.level() + (isAggressive ? "*" : "");
				title = cTitle + " " + lvlTitle;
			} else if (t.isMonster()) {
				boolean isAggressive = t.aggroRange() > 0 && !t.isFriendlyMob();
				String lvlTitle = "Lv " + t.level() + (isAggressive ? "*" : "");
				title = title.isEmpty() ? lvlTitle : lvlTitle + " " + title;
			}
			w.writeS(name);
			w.writeS(title);
			w.writeD(0x00).writeD(0x00).writeD(0x00);
			w.writeD(npc.abnormalEffect()); // abnormal effect
			w.writeD(0).writeD(0).writeD(0).writeD(0); // clan / ally
			w.writeC(0); // fly / water
			w.writeC(npc.isChampion() ? 2 : 0); // team (2 = aura vermelha para Champion mob)
			w.writeF(t.collisionRadius()).writeF(t.collisionHeight());
			w.writeD(0x00).writeD(0x00);
			return w.toByteArray();
		}
	}

	/** 0x16 NpcInfo para Summon/Pet visivel por todos no mundo. */
	record SummonNpcInfo(com.lopez.l2j.game.summon.SummonInstance summon) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var t = summon.template();
			PacketWriter w = new PacketWriter().writeC(0x16);
			w.writeD(summon.objectId());
			w.writeD(summon.idTemplate() + 1000000);
			w.writeD(1); // attackable / selectable
			w.writeD(summon.x()).writeD(summon.y()).writeD(summon.z()).writeD(summon.heading());
			w.writeD(0x00);
			w.writeD(summon.mAtkSpd()).writeD(summon.pAtkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeF(1.1);
			w.writeF(summon.pAtkSpd() / 277.478340719);
			w.writeF(t.collisionRadius() > 0 ? t.collisionRadius() : 9.0);
			w.writeF(t.collisionHeight() > 0 ? t.collisionHeight() : 22.0);
			w.writeD(t.rhand()).writeD(t.armor()).writeD(t.lhand());
			w.writeC(1); // name above char
			w.writeC(summon.running() ? 1 : 0);
			w.writeC(summon.isInCombat() ? 1 : 0);
			w.writeC(summon.isDead() ? 1 : 0);
			w.writeC(0); // 0=normal, 2=summoned animation
			w.writeS(summon.name());
			w.writeS(summon.title());
			w.writeD(0x01); // title color
			w.writeD(summon.owner() != null ? summon.owner().pvpFlag() : 0);
			w.writeD(summon.owner() != null ? summon.owner().karma() : 0);
			w.writeD(summon.abnormalEffect());
			w.writeD(0).writeD(0).writeD(0).writeD(0);
			w.writeC(0);
			w.writeC(summon.owner() != null ? summon.owner().team() : 0);
			w.writeF(t.collisionRadius() > 0 ? t.collisionRadius() : 9.0);
			w.writeF(t.collisionHeight() > 0 ? t.collisionHeight() : 22.0);
			w.writeD(0x00).writeD(0x00);
			return w.toByteArray();
		}
	}

	/** 0xB0 PetStatusShow: abre a janela de controle do Pet/Servitor no cliente. */
	record PetStatusShow(int summonType) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xb0).writeD(summonType).toByteArray();
		}
	}

	/** 0xB1 PetInfo: status completo do Pet/Servitor para o dono. */
	record PetInfo(com.lopez.l2j.game.summon.SummonInstance summon, int val) implements GameServerPacket {
		public PetInfo(com.lopez.l2j.game.summon.SummonInstance summon) {
			this(summon, 0);
		}

		@Override
		public byte[] encode() {
			var t = summon.template();
			PacketWriter w = new PacketWriter().writeC(0xb1);
			w.writeD(summon.summonType());
			w.writeD(summon.objectId());
			w.writeD(summon.idTemplate() + 1000000);
			w.writeD(0);
			w.writeD(summon.x()).writeD(summon.y()).writeD(summon.z()).writeD(summon.heading());
			w.writeD(0);
			w.writeD(summon.mAtkSpd()).writeD(summon.pAtkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			w.writeD(summon.runSpd()).writeD(summon.walkSpd());
			double moveMult = (t != null && t.runSpd() > 0) ? (double) summon.runSpd() / t.runSpd() : 1.1;
			w.writeF(moveMult > 0 ? moveMult : 1.1);
			w.writeF(summon.pAtkSpd() / 277.478340719);
			w.writeF(t.collisionRadius() > 0 ? t.collisionRadius() : 9.0);
			w.writeF(t.collisionHeight() > 0 ? t.collisionHeight() : 22.0);
			w.writeD(t.rhand()).writeD(t.armor()).writeD(t.lhand());
			w.writeC(1); // owner online
			w.writeC(summon.running() ? 1 : 0);
			w.writeC(summon.isInCombat() ? 1 : 0);
			w.writeC(summon.isDead() ? 1 : 0);
			w.writeC(val); // 0=teleported, 1=default, 2=summoned
			w.writeS(summon.name());
			w.writeS(summon.title());
			w.writeD(1);
			w.writeD(summon.owner() != null ? summon.owner().pvpFlag() : 0);
			w.writeD(summon.owner() != null ? summon.owner().karma() : 0);
			w.writeD(summon.currentFed());
			w.writeD(summon.maxFed());
			w.writeD((int) summon.currentHp());
			w.writeD(summon.maxHp());
			w.writeD((int) summon.currentMp());
			w.writeD(summon.maxMp());
			w.writeD(summon.sp());
			w.writeD(summon.level());
			w.writeQ(summon.exp());
			long expThisLvl = invokeLong(summon, "expForThisLevel", invokeLong(summon, "getExpForThisLevel", 0L));
			long expNextLvl = invokeLong(summon, "expForNextLevel",
					invokeLong(summon, "getExpForNextLevel", Math.max(1000L, summon.exp() + 1000L)));
			w.writeQ(expThisLvl); // exp this level
			w.writeQ(expNextLvl); // exp next level
			w.writeD(invokeInt(summon, "currentLoad", 0)); // curLoad
			w.writeD(invokeInt(summon, "maxLoad", summon.isPet() ? 54500 : 0)); // maxLoad
			w.writeD(summon.pAtk());
			w.writeD(summon.pDef());
			w.writeD(summon.mAtk());
			w.writeD(summon.mDef());
			w.writeD(summon.accuracy());
			w.writeD(summon.evasion());
			w.writeD(summon.critical());
			w.writeD(summon.runSpd());
			w.writeD(summon.pAtkSpd());
			w.writeD(summon.mAtkSpd());
			w.writeD(summon.abnormalEffect());
			int npcId = summon.idTemplate();
			int mountable = invokeInt(summon, "isMountable",
					(npcId == 12526 || npcId == 12527 || npcId == 12528 || npcId == 12621) ? 1 : 0);
			w.writeH(mountable); // mountable
			w.writeC(0);
			w.writeH(0);
			w.writeC(summon.owner() != null ? summon.owner().team() : 0);
			w.writeD(invokeInt(summon, "soulShotsPerHit", 1)); // soulshots per hit
			w.writeD(invokeInt(summon, "spiritShotsPerHit", 1)); // spiritshots per hit
			return w.toByteArray();
		}
	}

	/**
	 * 0xB5 PetStatusUpdate: atualiza barras de HP/MP/Fome do Pet/Servitor para o
	 * dono.
	 */
	record PetStatusUpdate(com.lopez.l2j.game.summon.SummonInstance summon) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xb5);
			w.writeD(summon.summonType());
			w.writeD(summon.objectId());
			w.writeD(summon.x()).writeD(summon.y()).writeD(summon.z());
			w.writeS(summon.title());
			w.writeD(summon.currentFed());
			w.writeD(summon.maxFed());
			w.writeD((int) summon.currentHp());
			w.writeD(summon.maxHp());
			w.writeD((int) summon.currentMp());
			w.writeD(summon.maxMp());
			w.writeD(summon.level());
			w.writeQ(summon.exp());
			long expThisLvl = invokeLong(summon, "expForThisLevel", invokeLong(summon, "getExpForThisLevel", 0L));
			long expNextLvl = invokeLong(summon, "expForNextLevel",
					invokeLong(summon, "getExpForNextLevel", Math.max(1000L, summon.exp() + 1000L)));
			w.writeQ(expThisLvl);
			w.writeQ(expNextLvl);
			return w.toByteArray();
		}
	}

	/** 0xB6 PetDelete: fecha a janela do Pet/Servitor no cliente. */
	record PetDelete(int summonType, int objId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xb6).writeD(summonType).writeD(objId).toByteArray();
		}
	}

	/**
	 * 0x12 DeleteObject: remove um objeto visivel (NPC ou player que saiu do
	 * alcance).
	 */
	record DeleteObject(int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x12).writeD(objectId).writeD(0x00).toByteArray();
		}
	}

	/** 0x03 CharInfo: visualizacao de outro jogador no mundo. */
	record CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int clanCrestId, int allyId,
			int allyCrestId, List<Integer> cubics) implements GameServerPacket {
		public CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll, int clanCrestId, int allyId,
				int allyCrestId) {
			this(c, t, paperdoll, clanCrestId, allyId, allyCrestId, resolvePlayerCubics(c));
		}

		public CharInfo(PlayerCharacter c) {
			this(c, null, c.inventory() != null ? c.inventory().paperdollView() : null,
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
		}

		public CharInfo(PlayerCharacter c, CharTemplate t) {
			this(c, t, c.inventory() != null ? c.inventory().paperdollView() : null,
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
		}

		public CharInfo(PlayerCharacter c, CharTemplate t, Paperdoll paperdoll) {
			this(c, t, paperdoll,
					c.getClan() != null ? c.getClan().crestId() : 0,
					c.getClan() != null ? c.getClan().allyId() : 0,
					c.getClan() != null ? c.getClan().allyCrestId() : 0,
					resolvePlayerCubics(c));
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
			int hairall = paperdoll != null ? paperdoll.itemId(ItemSlots.HAIRALL) : 0;
			int head = paperdoll != null ? paperdoll.itemId(ItemSlots.HEAD) : 0;
			int rhand = paperdoll != null ? paperdoll.itemId(ItemSlots.RHAND) : 0;
			int lrhand = paperdoll != null ? paperdoll.itemId(ItemSlots.LRHAND) : 0;
			if (rhand == 0 && lrhand > 0) {
				rhand = lrhand;
			}
			if (lrhand == 0 && rhand > 0) {
				lrhand = rhand;
			}
			int lhand = paperdoll != null ? paperdoll.itemId(ItemSlots.LHAND) : 0;
			int gloves = paperdoll != null ? paperdoll.itemId(ItemSlots.GLOVES) : 0;
			int chest = paperdoll != null ? paperdoll.itemId(ItemSlots.CHEST) : 0;
			int legs = paperdoll != null ? paperdoll.itemId(ItemSlots.LEGS) : 0;
			int feet = paperdoll != null ? paperdoll.itemId(ItemSlots.FEET) : 0;
			int back = paperdoll != null ? paperdoll.itemId(ItemSlots.BACK) : 0;
			int hair = paperdoll != null ? paperdoll.itemId(ItemSlots.HAIR) : 0;
			int face = paperdoll != null ? paperdoll.itemId(ItemSlots.FACE) : 0;

			if (c.isDressMe()) {
				if (c.dressMeArmor() != null && c.dressMeArmor().armor() != null) {
					var a = c.dressMeArmor().armor();
					if (a.chest() > 0)
						chest = a.chest();
					if (a.legs() > 0)
						legs = a.legs();
					if (a.gloves() > 0)
						gloves = a.gloves();
					if (a.feet() > 0)
						feet = a.feet();
					if (a.helmet() > 0)
						hairall = a.helmet();
				}
				if (c.dressMeWeapon() != null && c.dressMeWeapon().weapon() != null) {
					var wVisual = c.dressMeWeapon().weapon();
					if (wVisual.rhand() > 0) {
						rhand = wVisual.rhand();
						lrhand = wVisual.rhand();
					}
					if (wVisual.lhand() > 0)
						lhand = wVisual.lhand();
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
			w.writeD(lrhand);
			w.writeD(hair);
			w.writeD(face);

			// Augmentation e enchant info (20 shorts + 2 ints = 48 bytes)
			int rhandAug = 0;
			int lrhandAug = 0;
			if (c.inventory() != null) {
				var rw = c.inventory().paperdoll(ItemSlots.RHAND);
				if (rw != null && rw.augmentation() != null)
					rhandAug = rw.augmentation().attributes();
				var lrw = c.inventory().paperdoll(ItemSlots.LRHAND);
				if (lrw != null && lrw.augmentation() != null) {
					lrhandAug = lrw.augmentation().attributes();
				} else if (rhandAug != 0) {
					lrhandAug = rhandAug;
				}
			}
			for (int i = 0; i < 4; i++)
				w.writeH(0x00);
			w.writeD(rhandAug);
			for (int i = 0; i < 12; i++)
				w.writeH(0x00);
			w.writeD(lrhandAug);
			for (int i = 0; i < 4; i++)
				w.writeH(0x00);

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
			boolean isFlying = c.mountNpcId() == 12621;
			int flyRun = isFlying ? run : 0;
			int flyWalk = isFlying ? walk : 0;

			w.writeD(run).writeD(walk).writeD(swimRun).writeD(swimWalk).writeD(run).writeD(walk).writeD(flyRun)
					.writeD(flyWalk);
			w.writeF(moveMultiplier).writeF(atkSpeedMultiplier);
			w.writeF(t != null ? t.collisionRadius(c.female()) : 9.0)
					.writeF(t != null ? t.collisionHeight(c.female()) : 23.0);

			w.writeD(c.hairStyle()).writeD(c.hairColor()).writeD(c.face());
			w.writeS(c.title());
			var clan = c.getClan();
			int effClanCrest = clanCrestId != 0 ? clanCrestId : (clan != null ? clan.crestId() : 0);
			int effAllyId = allyId != 0 ? allyId : (clan != null ? clan.allyId() : 0);
			int effAllyCrest = allyCrestId != 0 ? allyCrestId : (clan != null ? clan.allyCrestId() : 0);
			w.writeD(c.clanId()).writeD(effClanCrest).writeD(effAllyId).writeD(effAllyCrest);

			w.writeD(0);
			w.writeC(c.sitting() ? 0 : 1);
			w.writeC(c.running() ? 1 : 0);
			w.writeC(c.isInCombat() ? 1 : 0); // isInCombat: 1 combate / 0 normal
			w.writeC(c.isDead() ? 1 : 0); // isAlikeDead
			w.writeC(c.invis() ? 1 : 0); // invisible
			int mountType = c.mountNpcId() == 12621 ? 2 : (c.mountNpcId() > 0 ? 1 : 0);
			w.writeC(mountType);
			w.writeC(c.privateStoreType());

			List<Integer> activeCubics = (cubics != null && !cubics.isEmpty()) ? cubics : resolvePlayerCubics(c);
			w.writeH(activeCubics.size()); // cubics
			for (int cubicId : activeCubics) {
				w.writeH(cubicId);
			}
			w.writeC(invokeInt(c, "partyMatchRoom", 0) > 0 ? 1 : 0);
			w.writeD(c.abnormalEffect()); // abnormal effect
			w.writeC(c.recomLeft());
			w.writeH(c.recomHave());
			w.writeD(c.classId());
			w.writeD(c.maxCp()).writeD((int) c.currentCp());
			int charEnchantEffect = 0;
			if (paperdoll != null && paperdoll.enchant(ItemSlots.RHAND) > 0) {
				charEnchantEffect = Math.min(127, paperdoll.enchant(ItemSlots.RHAND));
			} else if (c.inventory() != null) {
				var rw = c.inventory().paperdoll(ItemSlots.RHAND);
				if (rw != null) {
					charEnchantEffect = Math.min(127, Math.max(0, rw.enchant()));
				}
			}
			w.writeC(c.isMounted() ? 0 : charEnchantEffect); // enchant effect (weapon glow: 0=none, 4-15=blue, 16+=red)
			w.writeC(c.team()); // team
			int largeCrestId = (clan != null) ? clan.largeCrestId() : 0;
			w.writeD(largeCrestId);
			w.writeC(c.isNoble() ? 1 : 0).writeC(c.isHero() ? 1 : 0);
			w.writeC(c.isFishing() ? 1 : 0).writeD(c.fishX()).writeD(c.fishY()).writeD(c.fishZ());
			w.writeD(c.isGm() ? 0x00FFFF : (c.nameColor() != 0 ? c.nameColor() : 0xFFFFFF));
			w.writeD(c.heading());
			w.writeD(c.pledgeRank()).writeD(c.subPledge());
			w.writeD(c.isGm() ? 0x00FF77 : (c.titleColor() != 0 ? c.titleColor() : 0xFFFF77));
			w.writeD(invokeInt(c, "cursedWeaponLevel", 0));
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

	/**
	 * 0x28 TeleportToLocation: atualiza posicao instantanea do objeto no cliente.
	 */
	record TeleportToLocation(int objectId, int x, int y, int z) implements GameServerPacket {
		public TeleportToLocation(int objectId, int x, int y, int z, int heading) {
			this(objectId, x, y, z);
		}

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
		public record SellItemView(int objectId, int itemId, int count, int type1, int type2, int bodyPart, int enchant,
				int price) {
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
	record Attack(int attackerObjId, int targetObjId, int damage, int flags, int x, int y, int z, List<Hit> extraHits)
			implements GameServerPacket {
		public record Hit(int targetObjId, int damage, int flags) {
		}

		public static final int HITFLAG_USESS = 0x10;
		public static final int HITFLAG_CRIT = 0x20;
		public static final int HITFLAG_SHLD = 0x40;
		public static final int HITFLAG_MISS = 0x80;

		public Attack(int attackerObjId, int targetObjId, int damage, int flags, int x, int y, int z) {
			this(attackerObjId, targetObjId, damage, flags, x, y, z, List.of());
		}

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
			int extraCount = extraHits != null ? extraHits.size() : 0;
			w.writeH(extraCount);
			if (extraHits != null) {
				for (Hit h : extraHits) {
					w.writeD(h.targetObjId());
					w.writeD(h.damage());
					w.writeC(h.flags());
				}
			}
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
		public static final int MAX_LOAD = 0x0f;
		public static final int CUR_CP = 0x21;
		public static final int MAX_CP = 0x22;

		public StatusUpdate(int objectId, int id1, int val1) {
			this(objectId, List.of(new Attribute(id1, val1)));
		}

		public StatusUpdate(int objectId, int id1, int val1, int id2, int val2) {
			this(objectId, List.of(new Attribute(id1, val1), new Attribute(id2, val2)));
		}

		public static StatusUpdate hp(int objectId, int curHp, int maxHp) {
			return new StatusUpdate(objectId, List.of(new Attribute(CUR_HP, curHp), new Attribute(MAX_HP, maxHp)));
		}

		public static StatusUpdate mp(int objectId, int curMp, int maxMp) {
			return new StatusUpdate(objectId, List.of(new Attribute(CUR_MP, curMp), new Attribute(MAX_MP, maxMp)));
		}

		public static StatusUpdate cp(int objectId, int curCp, int maxCp) {
			return new StatusUpdate(objectId, List.of(new Attribute(CUR_CP, curCp), new Attribute(MAX_CP, maxCp)));
		}

		public static StatusUpdate forPlayer(PlayerCharacter p) {
			if (p == null) {
				return null;
			}
			return new StatusUpdate(p.objectId(), List.of(
					new Attribute(CUR_HP, (int) p.currentHp()),
					new Attribute(MAX_HP, p.maxHp()),
					new Attribute(CUR_MP, (int) p.currentMp()),
					new Attribute(MAX_MP, p.maxMp()),
					new Attribute(CUR_CP, (int) p.currentCp()),
					new Attribute(MAX_CP, p.maxCp())));
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
	record Die(int charObjId, boolean toVillage, boolean toClanHall, boolean toCastle, boolean toSiegeHQ,
			boolean sweepable, boolean fixedRes) implements GameServerPacket {
		public Die(int charObjId, boolean toVillage) {
			this(charObjId, toVillage, false, false, false, false, false);
		}

		public Die(int charObjId, boolean toVillage, boolean toClanHall, boolean toCastle) {
			this(charObjId, toVillage, toClanHall, toCastle, false, false, false);
		}

		public Die(int charObjId, boolean toVillage, boolean toClanHall, boolean toCastle, boolean sweepable) {
			this(charObjId, toVillage, toClanHall, toCastle, false, sweepable, false);
		}

		public static Die forPlayer(PlayerCharacter p) {
			if (p == null) {
				return new Die(0, true);
			}
			var clan = p.getClan();
			boolean hasHideout = clan != null && clan.hideoutId() > 0;
			boolean hasCastle = clan != null && (clan.castleId() > 0 || clan.fortId() > 0);
			boolean fixed = p.isGm();
			return new Die(p.objectId(), true, hasHideout, hasCastle, false, false, fixed);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x06);
			w.writeD(charObjId);
			w.writeD(toVillage ? 1 : 0);
			w.writeD(toClanHall ? 1 : 0); // clanhall
			w.writeD(toCastle ? 1 : 0); // castle
			w.writeD(toSiegeHQ ? 1 : 0); // flag
			w.writeD(sweepable ? 1 : 0); // sweepable (blue glow)
			w.writeD(fixedRes ? 1 : 0); // fixedres
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

	/**
	 * 0x99 StaticObject: renderiza objeto estatico do cenario (mapa, sinalizacao,
	 * estatua).
	 */
	record StaticObject(int staticObjectId, int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x99).writeD(staticObjectId).writeD(objectId).toByteArray();
		}
	}

	/**
	 * 0xde ShowTownMap: abre o mapa detalhado da vila (ex: ao clicar nas placas de
	 * vilas).
	 */
	record ShowTownMap(String texture, int x, int y) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xde).writeS(texture).writeD(x).writeD(y).toByteArray();
		}
	}

	/** 0x0c DropItem: renderiza item caido no chao do mundo. */
	record DropItem(int dropperObjId, int itemObjId, int itemId, int x, int y, int z, boolean isStackable, int count)
			implements GameServerPacket {
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
	record ExFishingStart(int charObjId, int fishType, int x, int y, int z, boolean isNightLure)
			implements GameServerPacket {
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

	/**
	 * 0xfe:0x15 ExFishingStartCombat: inicia o minigame de combate com o peixe
	 * fisgado.
	 */
	record ExFishingStartCombat(int charObjId, int time, int hp, int mode, int lureType, int deceptiveMode)
			implements GameServerPacket {
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

	/**
	 * 0xfe:0x16 ExFishingHpRegen: atualiza a barra de HP do peixe e a resposta a
	 * Pumping/Reeling.
	 */
	record ExFishingHpRegen(int charObjId, int time, int fishHp, int hpMode, int goodUse, int anim, int penalty,
			int hpBarColor) implements GameServerPacket {
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

	/**
	 * 0xfe:0x17 ExEnchantSkillList: lista de skills que podem ser encantados no
	 * trainer.
	 */
	record ExEnchantSkillList(List<SkillEntry> skills) implements GameServerPacket {
		public record SkillEntry(int id, int nextLevel, int sp, long exp) {
			public SkillEntry(int id, int nextLevel, int sp, int exp) {
				this(id, nextLevel, sp, (long) exp);
			}
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x17).writeD(skills.size());
			for (var s : skills) {
				w.writeD(s.id()).writeD(s.nextLevel()).writeD(s.sp()).writeQ(s.exp());
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xfe:0x18 ExEnchantSkillInfo: detalhes de custo em SP/EXP e taxa de sucesso
	 * do proximo nivel.
	 */
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

	/**
	 * 0x4e PartySmallWindowAll: inicializa a janela de grupo com todos os membros
	 * (exceto o próprio jogador).
	 */
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

	/**
	 * 0x4f PartySmallWindowAdd: adiciona um novo membro na janela de grupo
	 * existente.
	 */
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

	/**
	 * 0x50 PartySmallWindowDeleteAll: fecha a janela de grupo quando o grupo se
	 * desfaz ou o jogador sai.
	 */
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

	/**
	 * 0xd0 MultiSellList: janela de troca/compra de itens combinados (MultiSell).
	 */
	record MultiSellList(int listId, int page, int finished, int pageSize, List<MultiSellEntryView> entries)
			implements GameServerPacket {

		public record MultiSellEntryView(int entryId, boolean stackable, List<ItemView> ingredients,
				List<ItemView> products) {
			public MultiSellEntryView(int entryId, List<ItemView> ingredients, List<ItemView> products) {
				this(entryId, true, ingredients, products);
			}
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
				w.writeC(ent.stackable() ? 1 : 0);
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

	/**
	 * 0x81 EnchantResult: resultado do encantamento (0=sucesso, 1=falha normal,
	 * 2=cancelado, 3=falha blessed).
	 */
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

	/**
	 * 0x41 WareHouseDepositList: lista de itens disponiveis para deposito no
	 * armazem.
	 */
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
				int aug = item.augmentation() != null ? item.augmentation().attributes() : 0;
				w.writeD(aug & 0x0000FFFF);
				w.writeD(aug >> 16);
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0x42 WareHouseWithdrawalList: lista de itens guardados no armazem para
	 * retirada.
	 */
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
				int aug = item.augmentation() != null ? item.augmentation().attributes() : 0;
				w.writeD(aug & 0x0000FFFF);
				w.writeD(aug >> 16);
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0x8f GMViewCharacterInfo: status e aparencia de personagem para o painel
	 * Alt+G do GM.
	 */
	record GMViewCharacterInfo(PlayerCharacter c, CharTemplate t, PlayerStats stats, Paperdoll paperdoll,
			int currentLoad)
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
			int gmStr = stats != null ? stats.str() : (t.str() + c.hennaSTR() + c.augSTR());
			int gmDex = stats != null ? stats.dex() : (t.dex() + c.hennaDEX() + c.augDEX());
			int gmCon = stats != null ? stats.con() : (t.con() + c.hennaCON() + c.augCON());
			int gmIntel = stats != null ? stats.intel() : (t.intel() + c.hennaINT() + c.augINT());
			int gmWit = stats != null ? stats.wit() : (t.wit() + c.hennaWIT() + c.augWIT());
			int gmMen = stats != null ? stats.men() : (t.men() + c.hennaMEN() + c.augMEN());
			w.writeD(gmStr).writeD(gmDex).writeD(gmCon).writeD(gmIntel).writeD(gmWit).writeD(gmMen);
			w.writeD(c.maxHp()).writeD((int) c.currentHp());
			w.writeD(c.maxMp()).writeD((int) c.currentMp());
			w.writeD(c.sp());
			long fallbackLoad = (long) ((double) t.maxLoad()
					* (Config.ALT_WEIGHT_LIMIT > 0 ? Config.ALT_WEIGHT_LIMIT : 1.0));
			if (Config.ALT_WEIGHT_LIMIT > 1.0 && (fallbackLoad >= Integer.MAX_VALUE || fallbackLoad <= 0)) {
				fallbackLoad = Integer.MAX_VALUE;
			}
			int maxLoad = (stats != null && stats.maxLoad() > 0)
					? stats.maxLoad()
					: (int) Math.min(Integer.MAX_VALUE, Math.max(1L, fallbackLoad));
			w.writeD(currentLoad).writeD(maxLoad);
			w.writeD(0x28); // paperdoll start offset
			writePaperdoll(w, paperdoll, ItemSlots.RHAND);
			for (int slot : ItemSlots.VISIBLE_ORDER) {
				int effSlot = (slot == ItemSlots.LRHAND) ? ItemSlots.RHAND : slot;
				int augId = 0;
				if (c.inventory() != null) {
					var it = c.inventory().paperdoll(effSlot);
					if (it != null && it.augmentation() != null) {
						augId = it.augmentation().attributes();
					}
				}
				w.writeD(augId);
			}
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
			boolean hasDwarvenCraft = (t != null && t.canCraft()) || c.canCraft() || c.skillLevel(172) > 0
					|| com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(c);
			w.writeC(0).writeC(0).writeC(hasDwarvenCraft ? 1 : 0); // mount, store, dwarven craft
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
	record GMViewPledgeInfo(String charName, int clanId, int level, int classId, com.lopez.l2j.game.clan.Clan clan)
			implements GameServerPacket {
		public GMViewPledgeInfo(String charName, int clanId, int level, int classId) {
			this(charName, clanId, level, classId, null);
		}

		public GMViewPledgeInfo(String charName, com.lopez.l2j.game.clan.Clan clan) {
			this(charName, clan != null ? clan.clanId() : 0, clan != null ? clan.level() : 0, 0, clan);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x90);
			w.writeS(charName != null ? charName : "");
			if (clan != null) {
				int hideoutOrFort = clan.hideoutId() > 0 ? clan.hideoutId() : clan.fortId();
				int atWar = invokeInt(clan, "isAtWar", invokeInt(clan, "atWar", 0));
				w.writeD(clan.clanId());
				w.writeD(0x00);
				w.writeS(clan.name() != null ? clan.name() : "");
				w.writeS(clan.leaderName() != null ? clan.leaderName() : "");
				w.writeD(clan.crestId());
				w.writeD(clan.level());
				w.writeD(clan.castleId());
				w.writeD(hideoutOrFort);
				w.writeD(clan.rank());
				w.writeD(clan.reputationScore());
				w.writeD(0);
				w.writeD(0);
				w.writeD(clan.allyId());
				w.writeS(clan.allyName() != null ? clan.allyName() : "");
				w.writeD(clan.allyCrestId());
				w.writeD(atWar);
				var members = clan.members();
				w.writeD(members != null ? members.size() : 0);
				if (members != null) {
					for (var m : members) {
						w.writeS(m.name() != null ? m.name() : "");
						w.writeD(m.level());
						w.writeD(m.classId());
						w.writeD(invokeInt(m, "sex", 0));
						w.writeD(invokeInt(m, "race", 0));
						w.writeD(m.isOnline() ? m.objectId() : 0);
						w.writeD(invokeInt(m, "hasSponsor", 0));
					}
				}
				return w.toByteArray();
			}
			w.writeD(clanId);
			w.writeD(0x00);
			w.writeS(clanId != 0 ? "Clan " + clanId : "");
			w.writeS(charName != null ? charName : "");
			w.writeD(0).writeD(level).writeD(0).writeD(0).writeD(0);
			w.writeD(0).writeD(0).writeD(0).writeD(0);
			w.writeS("").writeD(0).writeD(0);
			w.writeD(1); // 1 member
			w.writeS(charName != null ? charName : "").writeD(level).writeD(classId).writeD(0).writeD(0).writeD(1)
					.writeD(0);
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
	record GMViewQuestInfo(String charName, List<QuestList.QuestEntry> quests) implements GameServerPacket {
		public GMViewQuestInfo(String charName) {
			this(charName, List.of());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x93).writeS(charName != null ? charName : "");
			int size = quests != null ? quests.size() : 0;
			w.writeH(size);
			if (quests != null) {
				for (var q : quests) {
					w.writeD(q.questId()).writeD(q.cond());
				}
			}
			w.writeH(0); // quest items count
			return w.toByteArray();
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
				int augId = item.augmentation() != null ? item.augmentation().attributes() : 0;
				w.writeD(augId);
				w.writeD(item.mana());
			}
			return w.toByteArray();
		}
	}

	/** 0xea GMHennaInfo: dyes/simbolos para a tela de inventario do GM. */
	record GMHennaInfo(int intel, int str, int con, int men, int dex, int wit, int maxSlots,
			List<HennaInfo.EquippedSlot> slots)
			implements GameServerPacket {
		public GMHennaInfo(int intel, int str, int con, int men, int dex, int wit) {
			this(intel, str, con, men, dex, wit, 3, List.of());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xea)
					.writeC(intel).writeC(str).writeC(con).writeC(men).writeC(dex).writeC(wit)
					.writeD(maxSlots);
			int size = slots != null ? slots.size() : 0;
			w.writeD(size);
			if (slots != null) {
				for (var s : slots) {
					w.writeD(s.symbolId()).writeD(s.activeForClass() ? 1 : 0);
				}
			}
			return w.toByteArray();
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
				w.writeH(item.customType1());
				if (t.bodyPart() != 0) {
					w.writeD(t.bodyPart());
					w.writeH(item.enchant());
					w.writeH(0);
					w.writeH(0);
					w.writeD(item.objectId());
					int aug = item.augmentation() != null ? item.augmentation().attributes() : 0;
					w.writeD(aug & 0x0000FFFF);
					w.writeD(aug >>> 16);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x4C DoorInfo: informacoes e estado de porta no mundo. */
	record DoorInfo(int objectId, int doorId, int viewHp, int isTargetable, int isEnemy, int currentHp, int maxHp,
			boolean showHp, int damage)
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

	/** 0x6C PledgeCrest: envia os bytes do brasao do cla (16x12 BMP) */
	record PledgeCrest(int crestId, byte[] data) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0x6c)
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
			int hideoutOrFort = clan.hideoutId() > 0 ? clan.hideoutId() : clan.fortId();
			int atWar = invokeInt(clan, "isAtWar", invokeInt(clan, "atWar", 0));
			return new PacketWriter().writeC(0x88)
					.writeD(clan.clanId())
					.writeD(clan.crestId())
					.writeD(clan.level())
					.writeD(clan.castleId())
					.writeD(hideoutOrFort)
					.writeD(clan.rank())
					.writeD(clan.reputationScore())
					.writeD(0)
					.writeD(0)
					.writeD(clan.allyId())
					.writeS(clan.allyName() != null ? clan.allyName() : "")
					.writeD(clan.allyCrestId())
					.writeD(atWar)
					.toByteArray();
		}
	}

	/** 0x53 PledgeShowMemberListAll: lista completa de membros do cla */
	record PledgeShowMemberListAll(com.lopez.l2j.game.clan.Clan clan, int subPledge) implements GameServerPacket {
		public PledgeShowMemberListAll(com.lopez.l2j.game.clan.Clan clan) {
			this(clan, 0);
		}

		@Override
		public byte[] encode() {
			int hideoutOrFort = clan.hideoutId() > 0 ? clan.hideoutId() : clan.fortId();
			int atWar = invokeInt(clan, "isAtWar", invokeInt(clan, "atWar", 0));
			var w = new PacketWriter().writeC(0x53)
					.writeD(subPledge == 0 ? 0 : 1)
					.writeD(clan.clanId())
					.writeD(subPledge)
					.writeS(clan.name())
					.writeS(clan.leaderName() != null ? clan.leaderName() : "")
					.writeD(clan.crestId())
					.writeD(clan.level())
					.writeD(clan.castleId())
					.writeD(hideoutOrFort)
					.writeD(clan.rank())
					.writeD(clan.reputationScore())
					.writeD(0)
					.writeD(0)
					.writeD(clan.allyId())
					.writeS(clan.allyName() != null ? clan.allyName() : "")
					.writeD(clan.allyCrestId())
					.writeD(atWar)
					.writeD(clan.membersCount());

			for (var m : clan.members()) {
				int memberPledgeType = invokeInt(m, "pledgeType", subPledge);
				int hasSponsor = invokeInt(m, "hasSponsor", invokeInt(m, "sponsor", 0) != 0 ? 1 : 0);
				w.writeS(m.name())
						.writeD(m.level())
						.writeD(m.classId())
						.writeD(invokeInt(m, "sex", 0))
						.writeD(invokeInt(m, "race", 0))
						.writeD(m.isOnline() ? (m.objectId() != 0 ? m.objectId() : 1) : 0)
						.writeD(hasSponsor != 0 ? hasSponsor : memberPledgeType);
			}
			return w.toByteArray();
		}
	}

	/** 0x54 PledgeShowMemberListUpdate: atualiza um unico membro na lista */
	record PledgeShowMemberListUpdate(String charName, int level, int classId, boolean online, int objectId,
			int pledgeType, int hasSponsor)
			implements GameServerPacket {
		public PledgeShowMemberListUpdate(String charName, int level, int classId, boolean online) {
			this(charName, level, classId, online, 0, 0, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x54)
					.writeS(charName)
					.writeD(level)
					.writeD(classId)
					.writeD(0)
					.writeD(objectId)
					.writeD(online ? (objectId != 0 ? objectId : 1) : 0)
					.writeD(pledgeType)
					.writeD(hasSponsor)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x45 ExCursedWeaponList: IDs de todas as armas amaldicoadas existentes
	 */
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

	/**
	 * 0xfe:0x46 ExCursedWeaponLocation: localizacao no mapa das armas amaldicoadas
	 * ativas/no chao
	 */
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
	record SSQStatus(
			int page,
			int period,
			int cycle,
			int periodMsgId,
			int timeMsgId,
			int playerCabal,
			int playerSeal,
			int stoneContrib,
			int adenaCollect,
			int duskStoneScoreProp,
			int duskFestivalScore,
			int duskTotalScore,
			int duskPercent,
			int dawnStoneScoreProp,
			int dawnFestivalScore,
			int dawnTotalScore,
			int dawnPercent,
			int winningCabal,
			int totalDawnMembers,
			int totalDuskMembers,
			int[] sealOwners,
			int[] duskSealProportions,
			int[] dawnSealProportions,
			int[] festivalLevelScores,
			int[] duskFestivalHighScores,
			int[] dawnFestivalHighScores,
			List<List<String>> duskFestivalMembers,
			List<List<String>> dawnFestivalMembers) implements GameServerPacket {

		public SSQStatus(int page, int period, int cycle, int playerCabal, int playerSeal,
				int stoneContrib, int adenaCollect, long dawnScore, long duskScore) {
			this(page, period, cycle, 1176, 1180, playerCabal, playerSeal, stoneContrib, adenaCollect,
					(int) Math.min(500, duskScore), 0, (int) Math.min(1000, duskScore), 50,
					(int) Math.min(500, dawnScore), 0, (int) Math.min(1000, dawnScore), 50,
					0, 0, 0,
					new int[4], new int[4], new int[4],
					new int[] { 60, 70, 100, 120, 150 },
					new int[5], new int[5],
					List.of(List.of(), List.of(), List.of(), List.of(), List.of()),
					List.of(List.of(), List.of(), List.of(), List.of(), List.of()));
		}

		public static SSQStatus of(int page, com.lopez.l2j.game.sevensigns.SevenSignsManager ss, int playerObjectId) {
			if (ss == null) {
				return new SSQStatus(page, 0, 1, 0, 0, 0, 0, 0, 0);
			}

			int period = ss.activePeriod();
			int cycle = ss.currentCycle();

			int periodMsgId = switch (period) {
				case com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RECRUITING -> 1183;
				case com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMPETITION -> 1176;
				case com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS -> 1184;
				case com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION -> 1185;
				default -> 1176;
			};

			int timeMsgId = switch (period) {
				case com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RECRUITING,
						com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS ->
					1181;
				default -> 1180;
			};

			var pData = ss.getPlayerData(playerObjectId).orElse(null);
			int playerCabal = pData != null ? pData.cabal() : 0;
			int playerSeal = pData != null ? pData.seal() : 0;
			int stoneContrib = pData != null ? (pData.redStones() + pData.greenStones() + pData.blueStones()) : 0;
			int adenaCollect = pData != null ? pData.ancientAdena() : 0;

			int duskStoneProp = ss.getStoneScoreProp(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK);
			int dawnStoneProp = ss.getStoneScoreProp(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN);
			int duskFest = ss.getCurrentFestivalScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK);
			int dawnFest = ss.getCurrentFestivalScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN);
			int duskTotal = ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK);
			int dawnTotal = ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN);
			int totalOverall = duskTotal + dawnTotal;
			int duskPercent = totalOverall > 0 ? Math.round((float) duskTotal / totalOverall * 100) : 0;
			int dawnPercent = totalOverall > 0 ? Math.round((float) dawnTotal / totalOverall * 100) : 0;

			int winningCabal = ss.getCabalHighestScore();
			int totalDawnMembers = ss.getTotalMembers(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN);
			int totalDuskMembers = ss.getTotalMembers(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK);

			int[] sealOwners = new int[] { 0, ss.avariceOwner(), ss.gnosisOwner(), ss.strifeOwner() };
			int[] duskSealProportions = new int[] { 0,
					ss.getSealProportion(1, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK),
					ss.getSealProportion(2, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK),
					ss.getSealProportion(3, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) };
			int[] dawnSealProportions = new int[] { 0,
					ss.getSealProportion(1, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN),
					ss.getSealProportion(2, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN),
					ss.getSealProportion(3, com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) };

			int[] duskFestivalHighScores = new int[5];
			int[] dawnFestivalHighScores = new int[5];
			List<List<String>> duskFestivalMembers = new java.util.ArrayList<>(5);
			List<List<String>> dawnFestivalMembers = new java.util.ArrayList<>(5);
			for (int i = 0; i < 5; i++) {
				duskFestivalHighScores[i] = ss
						.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, i);
				dawnFestivalHighScores[i] = ss
						.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, i);
				duskFestivalMembers
						.add(ss.getHighestScoreMembers(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, i));
				dawnFestivalMembers
						.add(ss.getHighestScoreMembers(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, i));
			}

			return new SSQStatus(
					page, period, cycle, periodMsgId, timeMsgId, playerCabal, playerSeal,
					stoneContrib, adenaCollect, duskStoneProp, duskFest, duskTotal, duskPercent,
					dawnStoneProp, dawnFest, dawnTotal, dawnPercent,
					winningCabal, totalDawnMembers, totalDuskMembers,
					sealOwners, duskSealProportions, dawnSealProportions,
					com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_LEVEL_SCORES,
					duskFestivalHighScores, dawnFestivalHighScores,
					duskFestivalMembers, dawnFestivalMembers);
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter()
					.writeC(0xf5)
					.writeC(page)
					.writeC(period);

			switch (page) {
				case 1 -> {
					w.writeD(cycle);
					w.writeD(periodMsgId);
					w.writeD(timeMsgId);
					w.writeC(playerCabal);
					w.writeC(playerSeal);
					w.writeD(stoneContrib);
					w.writeD(adenaCollect);
					w.writeD(duskStoneScoreProp);
					w.writeD(duskFestivalScore);
					w.writeD(duskTotalScore);
					w.writeC(duskPercent);
					w.writeD(dawnStoneScoreProp);
					w.writeD(dawnFestivalScore);
					w.writeD(dawnTotalScore);
					w.writeC(dawnPercent);
				}
				case 2 -> {
					w.writeH(1);
					w.writeC(5);
					for (int i = 0; i < 5; i++) {
						w.writeC(i + 1);
						w.writeD(festivalLevelScores != null && i < festivalLevelScores.length ? festivalLevelScores[i]
								: 60);

						int duskScore = duskFestivalHighScores != null && i < duskFestivalHighScores.length
								? duskFestivalHighScores[i]
								: 0;
						w.writeD(duskScore);
						var duskMems = (duskFestivalMembers != null && i < duskFestivalMembers.size())
								? duskFestivalMembers.get(i)
								: List.<String>of();
						w.writeC(duskMems.size());
						for (String m : duskMems) {
							w.writeS(m);
						}

						int dawnScore = dawnFestivalHighScores != null && i < dawnFestivalHighScores.length
								? dawnFestivalHighScores[i]
								: 0;
						w.writeD(dawnScore);
						var dawnMems = (dawnFestivalMembers != null && i < dawnFestivalMembers.size())
								? dawnFestivalMembers.get(i)
								: List.<String>of();
						w.writeC(dawnMems.size());
						for (String m : dawnMems) {
							w.writeS(m);
						}
					}
				}
				case 3 -> {
					w.writeC(10);
					w.writeC(35);
					w.writeC(3);
					for (int i = 1; i <= 3; i++) {
						int dawnProp = dawnSealProportions != null && i < dawnSealProportions.length
								? dawnSealProportions[i]
								: 0;
						int duskProp = duskSealProportions != null && i < duskSealProportions.length
								? duskSealProportions[i]
								: 0;
						int owner = sealOwners != null && i < sealOwners.length ? sealOwners[i] : 0;

						w.writeC(i);
						w.writeC(owner);

						int duskPct = totalDuskMembers > 0 ? Math.round((float) duskProp / totalDuskMembers * 100) : 0;
						int dawnPct = totalDawnMembers > 0 ? Math.round((float) dawnProp / totalDawnMembers * 100) : 0;
						w.writeC(duskPct);
						w.writeC(dawnPct);
					}
				}
				case 4 -> {
					w.writeC(winningCabal);
					w.writeC(3);
					for (int i = 1; i <= 3; i++) {
						int dawnProp = dawnSealProportions != null && i < dawnSealProportions.length
								? dawnSealProportions[i]
								: 0;
						int duskProp = duskSealProportions != null && i < duskSealProportions.length
								? duskSealProportions[i]
								: 0;
						int dawnPct = totalDawnMembers > 0 ? Math.round((float) dawnProp / totalDawnMembers * 100) : 0;
						int duskPct = totalDuskMembers > 0 ? Math.round((float) duskProp / totalDuskMembers * 100) : 0;
						int owner = sealOwners != null && i < sealOwners.length ? sealOwners[i] : 0;

						w.writeC(i);
						w.writeC(owner);

						int sysMsgId;
						if (owner == 0) { // CABAL_NULL
							if (winningCabal == 0) {
								sysMsgId = 1266; // COMPETITION_TIE_SEAL_NOT_AWARDED
							} else if (winningCabal == 2) { // DAWN
								sysMsgId = dawnPct >= 35 ? 1267 : 1268;
							} else { // DUSK
								sysMsgId = duskPct >= 35 ? 1267 : 1268;
							}
						} else if (owner == 2) { // DAWN
							if (winningCabal == 0) {
								sysMsgId = dawnPct >= 10 ? 1269 : 1266;
							} else if (winningCabal == 2) {
								sysMsgId = dawnPct >= 10 ? 1269 : 1270;
							} else {
								sysMsgId = duskPct >= 35 ? 1267 : (dawnPct >= 10 ? 1269 : 1270);
							}
						} else { // DUSK
							if (winningCabal == 0) {
								sysMsgId = duskPct >= 10 ? 1269 : 1266;
							} else if (winningCabal == 2) {
								sysMsgId = dawnPct >= 35 ? 1267 : (duskPct >= 10 ? 1269 : 1270);
							} else {
								sysMsgId = duskPct >= 10 ? 1269 : 1270;
							}
						}

						w.writeH(sysMsgId);
						w.writeH(0);
					}
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
			return new PacketWriter().writeC(0xfd).writeD(0).writeS(receiver).writeS(sender).writeS(message)
					.toByteArray();
		}
	}

	/** Item listado a venda na loja de um jogador. */
	record PrivateStoreItem(int objectId, int itemId, int count, int price, int bodyPart, int enchant,
			int type2, int customType1, int augment, int storePrice) {
		public PrivateStoreItem(int objectId, int itemId, int count, int price, int bodyPart, int enchant) {
			this(objectId, itemId, count, price, bodyPart, enchant, 0, 0, 0, price);
		}
	}

	/**
	 * 0x9c PrivateStoreMsgSell: titulo da loja pessoal de venda sobre a cabeca do
	 * personagem.
	 */
	record PrivateStoreMsgSell(int objectId, String storeMsg) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x9c).writeD(objectId).writeS(storeMsg != null ? storeMsg : "")
					.toByteArray();
		}
	}

	/**
	 * 0x9b PrivateStoreListSell: visualizacao da lista de itens/buffs a venda para
	 * o cliente comprador.
	 */
	record PrivateStoreListSell(int sellerId, boolean packageSale, int buyerAdena, List<PrivateStoreItem> items)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x9b).writeD(sellerId).writeD(packageSale ? 1 : 0)
					.writeD(buyerAdena).writeD(items.size());
			for (var item : items) {
				w.writeD(item.type2()); // type2
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.enchant());
				w.writeH(item.customType1());
				w.writeD(item.bodyPart());
				w.writeD(item.price()); // price
				w.writeD(item.storePrice() > 0 ? item.storePrice() : item.price()); // storePrice
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0x9a PrivateStoreManageListSell: janela de configuracao da loja pessoal
	 * (itens/buffs disponiveis e precos).
	 */
	record PrivateStoreManageListSell(int sellerId, boolean packageSale, int sellerAdena,
			List<PrivateStoreItem> available, List<PrivateStoreItem> current) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x9a).writeD(sellerId).writeD(packageSale ? 1 : 0)
					.writeD(sellerAdena);
			w.writeD(available.size());
			for (var item : available) {
				w.writeD(item.type2()); // type2
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.enchant());
				w.writeH(item.customType1());
				w.writeD(item.bodyPart());
				w.writeD(item.price());
			}
			w.writeD(current.size());
			for (var item : current) {
				w.writeD(item.type2());
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(0);
				w.writeH(item.enchant());
				w.writeH(item.customType1());
				w.writeD(item.bodyPart());
				w.writeD(item.price());
				w.writeD(item.storePrice() > 0 ? item.storePrice() : item.price());
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xfe:0x50 ExShowVariationMakeWindow: abre a janela de augmentacao de armas.
	 */
	record ExShowVariationMakeWindow() implements GameServerPacket {
		public static final ExShowVariationMakeWindow STATIC_PACKET = new ExShowVariationMakeWindow();

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x50).toByteArray();
		}
	}

	/**
	 * 0xfe:0x51 ExShowVariationCancelWindow: abre a janela de cancelamento de
	 * augmentacao.
	 */
	record ExShowVariationCancelWindow() implements GameServerPacket {
		public static final ExShowVariationCancelWindow STATIC_PACKET = new ExShowVariationCancelWindow();

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x51).toByteArray();
		}
	}

	/**
	 * 0xfe:0x52 ExPutItemResultForVariationMake: confirma o item alvo para
	 * augmentacao.
	 */
	record ExPutItemResultForVariationMake(int itemObjId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x52).writeD(itemObjId).writeD(1).writeD(1).toByteArray();
		}
	}

	/**
	 * 0xfe:0x53 ExPutIntensiveResultForVariationMake: confirma a Life Stone e
	 * requisitos de Gemstones.
	 */
	record ExPutIntensiveResultForVariationMake(int refinerItemObjId, int lifeStoneId, int gemstoneItemId,
			int gemstoneCount) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x53)
					.writeD(refinerItemObjId).writeD(lifeStoneId).writeD(gemstoneItemId).writeD(gemstoneCount).writeD(1)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x54 ExPutCommissionResultForVariationMake: confirma o deposito de
	 * Gemstones.
	 */
	record ExPutCommissionResultForVariationMake(int gemstoneItemObjId, int gemstoneCount) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x54)
					.writeD(gemstoneItemObjId).writeD(1).writeD(gemstoneCount).writeD(1).writeD(1)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x55 ExVariationResult: resultado da augmentacao (stat12, stat34,
	 * 1=sucesso, 0=falha).
	 */
	record ExVariationResult(int stat12, int stat34, int success) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x55)
					.writeD(stat12).writeD(stat34).writeD(success)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x56 ExPutItemResultForVariationCancel: confirma o item para remocao de
	 * augmentacao e custo.
	 */
	record ExPutItemResultForVariationCancel(int itemObjId, int itemId, int stat12, int stat34, long price)
			implements GameServerPacket {
		public ExPutItemResultForVariationCancel(int itemObjId, int itemId, long price) {
			this(itemObjId, itemId, 0, 0, price);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x56)
					.writeD(itemObjId).writeD(itemId).writeD(stat12).writeD(stat34).writeQ(price).writeD(0x01)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x57 ExVariationCancelResult: resultado da remocao de augmentacao
	 * (1=sucesso).
	 */
	record ExVariationCancelResult(int result) implements GameServerPacket {
		public ExVariationCancelResult(int unk, int result) {
			this(result);
		}

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

	/**
	 * 0x98 PlaySound: reproduz efeito sonoro ou trilha musical no cliente
	 * Interlude.
	 */
	record PlaySound(int soundType, String soundFile, int hasCenterObject, int objectId, int x, int y, int z)
			implements GameServerPacket {
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

	/**
	 * 0xa0 TutorialShowHtml: exibe pagina HTML de tutorial.
	 * <p>
	 * ATENCAO: no Interlude (C6) o opcode 0xa6 pertence a MyTargetSelected. Enviar
	 * o tutorial com 0xa6 faz o
	 * cliente interpretar o HTML como objectId de alvo e gera "General protection
	 * fault" (MyTargetSelected) no
	 * primeiro login de personagens novos.
	 */
	record TutorialShowHtml(String html) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xa0)
					.writeS(html != null ? html : "")
					.toByteArray();
		}
	}

	/** 0xa1 TutorialShowQuestionMark: exibe icone de interrogacao de tutorial. */
	record TutorialShowQuestionMark(int markId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xa1)
					.writeD(markId)
					.toByteArray();
		}
	}

	/**
	 * 0xa2 TutorialEnableClientEvent: habilita gatilho de evento no cliente para o
	 * tutorial.
	 */
	record TutorialEnableClientEvent(int eventId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xa2)
					.writeD(eventId)
					.toByteArray();
		}
	}

	/** 0xa3 TutorialCloseHtml: fecha janela de tutorial. */
	record TutorialCloseHtml() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xa3)
					.toByteArray();
		}
	}

	/**
	 * 0xeb RadarControl: adiciona ou remove marcador no radar do minimapa
	 * (Interlude C6).
	 */
	record RadarControl(int show, int type, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xeb)
					.writeD(show)
					.writeD(type)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x1a ExShowQuestMark: notifica o cliente da atualizacao da quest e pisca
	 * o marcador/pino no mapa.
	 */
	record ExShowQuestMark(int questId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter()
					.writeC(0xfe)
					.writeH(0x1a)
					.writeD(questId)
					.toByteArray();
		}
	}

	/** 0xe8 SendMacroList: lista de macros ou atualizacao de macro individual. */
	record SendMacroList(int revision, int count, com.lopez.l2j.game.macro.Macro macro) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xe7);
			w.writeD(revision);
			w.writeC(0);
			w.writeC(count);
			w.writeC(macro != null ? 1 : 0);
			if (macro != null) {
				w.writeD(macro.id());
				w.writeS(macro.name());
				w.writeS(macro.descr());
				w.writeS(macro.acronym());
				w.writeC(macro.icon());
				var cmds = macro.commands();
				w.writeC(cmds.size());
				for (int i = 0; i < cmds.size(); i++) {
					var cmd = cmds.get(i);
					w.writeC(i + 1);
					w.writeC(cmd.type());
					w.writeD(cmd.d1());
					w.writeC(cmd.d2());
					w.writeS(cmd.cmd() != null ? cmd.cmd() : "");
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x5E SendTradeRequest: convite de negociacao enviado ao jogador alvo. */
	record SendTradeRequest(int senderId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5e).writeD(senderId).toByteArray();
		}
	}

	/** 0xd6 RecipeBookItemList: envia lista de receitas do livro de receitas. */
	record RecipeBookItemList(boolean isDwarvenCraft, int maxCost, List<Integer> recipes) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = recipes != null ? recipes.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xd6);
			w.writeD(isDwarvenCraft ? 0 : 1);
			w.writeD(maxCost);
			w.writeD(size);
			if (recipes != null) {
				int idx = 1;
				for (int id : recipes) {
					w.writeD(id);
					w.writeD(idx++);
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xfe:0x33 ExGetBossRecord: envia pontuacao e historico de raid bosses para o
	 * mapa mundi / janela de raids (Alt+M).
	 */
	record ExGetBossRecord(int ranking, int totalPoints, Map<Integer, Integer> bossPoints) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = bossPoints != null ? bossPoints.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x33);
			w.writeD(ranking);
			w.writeD(totalPoints);
			w.writeD(size);
			if (bossPoints != null && !bossPoints.isEmpty()) {
				for (var entry : bossPoints.entrySet()) {
					w.writeD(entry.getKey());
					w.writeD(entry.getValue());
					w.writeD(0);
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0x1e TradeStart: abre a janela de troca com a lista de itens negociaveis do
	 * jogador.
	 */
	record TradeStart(int partnerObjId, List<ItemInstance> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0x1e).writeD(partnerObjId).writeH(items.size());
			for (var item : items) {
				var t = item.template();
				w.writeH(t != null ? t.type1() : 0);
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeD(item.count());
				w.writeH(t != null ? t.type2() : 0);
				w.writeH(item.customType1());
				w.writeD(t != null ? t.bodyPart() : 0);
				w.writeH(item.enchant());
				w.writeH(item.customType2());
				w.writeH(0);
			}
			return w.toByteArray();
		}
	}

	/** 0x20 TradeOwnAdd: adiciona item na propria metade da janela de troca. */
	record TradeOwnAdd(ItemInstance item, int count) implements GameServerPacket {
		public TradeOwnAdd(ItemInstance item) {
			this(item, item != null ? item.count() : 0);
		}

		@Override
		public byte[] encode() {
			var t = item.template();
			return new PacketWriter().writeC(0x20).writeH(1)
					.writeH(t != null ? t.type1() : 0)
					.writeD(item.objectId())
					.writeD(item.itemId())
					.writeD(count)
					.writeH(t != null ? t.type2() : 0)
					.writeH(item.customType1())
					.writeD(t != null ? t.bodyPart() : 0)
					.writeH(item.enchant())
					.writeH(item.customType2())
					.writeH(0)
					.toByteArray();
		}
	}

	/**
	 * 0x21 TradeOtherAdd: adiciona item na metade do parceiro da janela de troca.
	 */
	record TradeOtherAdd(ItemInstance item, int count) implements GameServerPacket {
		public TradeOtherAdd(ItemInstance item) {
			this(item, item != null ? item.count() : 0);
		}

		@Override
		public byte[] encode() {
			var t = item.template();
			return new PacketWriter().writeC(0x21).writeH(1)
					.writeH(t != null ? t.type1() : 0)
					.writeD(item.objectId())
					.writeD(item.itemId())
					.writeD(count)
					.writeH(t != null ? t.type2() : 0)
					.writeH(item.customType1())
					.writeD(t != null ? t.bodyPart() : 0)
					.writeH(item.enchant())
					.writeH(item.customType2())
					.writeH(0)
					.toByteArray();
		}
	}

	/** 0x22 SendTradeDone: fecha a janela de troca (1 = sucesso, 0 = cancelado). */
	record SendTradeDone(int num) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x22).writeD(num).toByteArray();
		}
	}

	/** 0x75 TradePressOwnOk: trava o botao OK na propria janela de troca. */
	record TradePressOwnOk() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x75).toByteArray();
		}
	}

	/** 0x7c TradePressOtherOk: trava o botao OK do parceiro na janela de troca. */
	record TradePressOtherOk() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x7c).toByteArray();
		}
	}

	/**
	 * 0xb7 PrivateStoreManageListBuy: abre a configuracao de loja pessoal de
	 * compra.
	 */
	record PrivateStoreManageListBuy(int objId, int adena, List<PrivateStoreItem> current,
			List<PrivateStoreItem> available) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xb7).writeD(objId).writeD(adena);
			w.writeD(available.size());
			for (var item : available) {
				w.writeD(item.itemId());
				w.writeH(item.enchant());
				w.writeD(item.count());
				w.writeD(item.price());
				w.writeH(0);
				w.writeD(item.bodyPart());
				w.writeH(0);
			}
			w.writeD(current.size());
			for (var item : current) {
				w.writeD(item.itemId());
				w.writeH(item.enchant());
				w.writeD(item.count());
				w.writeD(item.price());
				w.writeH(0);
				w.writeD(item.bodyPart());
				w.writeH(0);
				w.writeD(item.price());
				w.writeD(item.price());
			}
			return w.toByteArray();
		}
	}

	/** 0xb8 PrivateStoreListBuy: exibe a lista de compra de outro jogador. */
	record PrivateStoreListBuy(int buyerObjId, int sellerAdena, List<PrivateStoreItem> items)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xb8).writeD(buyerObjId).writeD(sellerAdena)
					.writeD(items.size());
			for (var item : items) {
				w.writeD(item.objectId());
				w.writeD(item.itemId());
				w.writeH(item.enchant());
				w.writeD(item.count());
				w.writeD(item.price());
				w.writeH(0);
				w.writeD(item.bodyPart());
				w.writeH(0);
				w.writeD(item.price());
				w.writeD(item.count());
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xb9 PrivateStoreMsgBuy: exibe a placa da loja de compra sobre a cabeca do
	 * jogador.
	 */
	record PrivateStoreMsgBuy(int objId, String storeMsg) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xb9).writeD(objId).writeS(storeMsg != null ? storeMsg : "").toByteArray();
		}
	}

	/**
	 * 0xd7 RecipeItemMakeInfo: abre a janela de criacao de item (Dwarven/Common
	 * Craft).
	 */
	record RecipeItemMakeInfo(int id, boolean isDwarven, int currentMp, int maxMp, int status)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xd7)
					.writeD(id)
					.writeD(isDwarven ? 0 : 1)
					.writeD(currentMp)
					.writeD(maxMp)
					.writeD(status)
					.toByteArray();
		}
	}

	/** 0xc9 SiegeInfo: exibe informacoes da Siege do castelo. */
	record SiegeInfo(int castleId, int ownerClanId, String castleName, String ownerName, String leaderName,
			int allyId, String allyName, String allyLeaderName, int siegeTimeSeconds, boolean isOwnerLeader)
			implements GameServerPacket {
		public SiegeInfo(int castleId, int ownerClanId, String castleName, String ownerName, String leaderName,
				int siegeTimeSeconds) {
			this(castleId, ownerClanId, castleName, ownerName, leaderName, 0, "", "", siegeTimeSeconds, false);
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xc9);
			w.writeD(castleId);
			w.writeD(isOwnerLeader ? 1 : (ownerClanId > 0 ? 1 : 0));
			w.writeD(ownerClanId);
			w.writeS(castleName != null ? castleName : "Castle");
			if (ownerClanId > 0) {
				w.writeS(ownerName != null ? ownerName : "");
				w.writeS(leaderName != null ? leaderName : "");
				w.writeD(allyId);
				w.writeS(allyName != null ? allyName : "");
				w.writeS(allyLeaderName != null ? allyLeaderName : "");
			}
			w.writeD((int) (System.currentTimeMillis() / 1000L));
			w.writeD(siegeTimeSeconds);
			w.writeD(0);
			return w.toByteArray();
		}
	}

	/** 0xca SiegeAttackerList: lista de clas atacantes registrados na Siege. */
	record SiegeAttackerList(int castleId, List<Integer> clanIds, List<String> clanNames, List<String> leaderNames,
			List<Integer> crestIds, List<Integer> allyIds, List<String> allyNames, List<String> allyLeaderNames,
			List<Integer> allyCrestIds)
			implements GameServerPacket {
		public SiegeAttackerList(int castleId, List<Integer> clanIds, List<String> clanNames,
				List<String> leaderNames) {
			this(castleId, clanIds, clanNames, leaderNames, List.of(), List.of(), List.of(), List.of(), List.of());
		}

		@Override
		public byte[] encode() {
			int size = clanIds != null ? clanIds.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xca).writeD(castleId).writeD(0).writeD(1).writeD(0).writeD(size)
					.writeD(size);
			for (int i = 0; i < size; i++) {
				w.writeD(clanIds.get(i));
				w.writeS(clanNames != null && i < clanNames.size() ? clanNames.get(i) : "");
				w.writeS(leaderNames != null && i < leaderNames.size() ? leaderNames.get(i) : "");
				w.writeD(crestIds != null && i < crestIds.size() ? crestIds.get(i) : 0);
				w.writeD(0);
				w.writeD(allyIds != null && i < allyIds.size() ? allyIds.get(i) : 0);
				w.writeS(allyNames != null && i < allyNames.size() ? allyNames.get(i) : "");
				w.writeS(allyLeaderNames != null && i < allyLeaderNames.size() ? allyLeaderNames.get(i) : "");
				w.writeD(allyCrestIds != null && i < allyCrestIds.size() ? allyCrestIds.get(i) : 0);
			}
			return w.toByteArray();
		}
	}

	/** 0xcb SiegeDefenderList: lista de clas defensores registrados na Siege. */
	record SiegeDefenderList(int castleId, List<Integer> clanIds, List<String> clanNames, List<String> leaderNames,
			List<Integer> crestIds, List<Integer> types, List<Integer> allyIds, List<String> allyNames,
			List<String> allyLeaderNames, List<Integer> allyCrestIds)
			implements GameServerPacket {
		public SiegeDefenderList(int castleId, List<Integer> clanIds, List<String> clanNames,
				List<String> leaderNames) {
			this(castleId, clanIds, clanNames, leaderNames, List.of(), List.of(), List.of(), List.of(), List.of(),
					List.of());
		}

		@Override
		public byte[] encode() {
			int size = clanIds != null ? clanIds.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xcb).writeD(castleId).writeD(0).writeD(1).writeD(0).writeD(size)
					.writeD(size);
			for (int i = 0; i < size; i++) {
				w.writeD(clanIds.get(i));
				w.writeS(clanNames != null && i < clanNames.size() ? clanNames.get(i) : "");
				w.writeS(leaderNames != null && i < leaderNames.size() ? leaderNames.get(i) : "");
				w.writeD(crestIds != null && i < crestIds.size() ? crestIds.get(i) : 0);
				w.writeD(0);
				w.writeD(types != null && i < types.size() ? types.get(i) : 1); // 1=owner, 2=waiting, 3=approved,
																				// 4=rejected
				w.writeD(allyIds != null && i < allyIds.size() ? allyIds.get(i) : 0);
				w.writeS(allyNames != null && i < allyNames.size() ? allyNames.get(i) : "");
				w.writeS(allyLeaderNames != null && i < allyLeaderNames.size() ? allyLeaderNames.get(i) : "");
				w.writeD(allyCrestIds != null && i < allyCrestIds.size() ? allyCrestIds.get(i) : 0);
			}
			return w.toByteArray();
		}
	}

	/** 0xc4 Earthquake: efeito de terremoto de camera no cliente (Grand Bosses). */
	record Earthquake(int x, int y, int z, int intensity, int duration, int isNpc) implements GameServerPacket {
		public Earthquake(int x, int y, int z, int intensity, int duration) {
			this(x, y, z, intensity, duration, 0);
		}

		public Earthquake(int x, int y, int z, int intensity) {
			this(x, y, z, intensity, 10, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xc4)
					.writeD(x).writeD(y).writeD(z)
					.writeD(intensity).writeD(duration).writeD(isNpc)
					.toByteArray();
		}
	}

	/**
	 * 0xc7 SpecialCamera: cutscene cinematografica de Grand Boss (Antharas,
	 * Valakas, Baium, Frintezza).
	 */
	record SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int turn, int rise,
			int widescreen, int unk)
			implements GameServerPacket {
		public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration) {
			this(id, dist, yaw, pitch, time, duration, 0, 0, 1, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xc7)
					.writeD(id).writeD(dist).writeD(yaw).writeD(pitch).writeD(time).writeD(duration)
					.writeD(turn).writeD(rise).writeD(widescreen).writeD(unk)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x2b ExOlympiadMode: alterna modo de camera/UI da Olimpiada (0=normal,
	 * 1=lutador1, 2=lutador2, 3=espectador).
	 */
	record ExOlympiadMode(int mode) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x2b).writeC(mode).toByteArray();
		}
	}

	/**
	 * 0xfe:0x29 ExOlympiadUserInfo: exibe barra de HP/CP do oponente nas
	 * Olimpiadas.
	 */
	record ExOlympiadUserInfo(int side, int objectId, String name, int classId, int curHp, int maxHp, int curCp,
			int maxCp) implements GameServerPacket {
		public static ExOlympiadUserInfo of(PlayerCharacter p, int side) {
			return new ExOlympiadUserInfo(side, p.objectId(), p.name(), p.classId(),
					(int) p.currentHp(), p.maxHp(), (int) p.currentCp(), p.maxCp());
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x29)
					.writeC(side)
					.writeD(objectId)
					.writeS(name != null ? name : "")
					.writeD(classId)
					.writeD(curHp).writeD(maxHp)
					.writeD(curCp).writeD(maxCp)
					.toByteArray();
		}
	}

	/** 0xfe:0x38 ExShowScreenMessage: exibe mensagem grande na tela do cliente. */
	record ExShowScreenMessage(int type, int sysMessageId, int position, int unk1, int size, int unk2, int unk3,
			boolean showEffect, int time, boolean fade, String text) implements GameServerPacket {
		public static final int TOP_LEFT = 1;
		public static final int TOP_CENTER = 2;
		public static final int TOP_RIGHT = 3;
		public static final int MIDDLE_LEFT = 4;
		public static final int MIDDLE_CENTER = 5;
		public static final int MIDDLE_RIGHT = 6;
		public static final int BOTTOM_CENTER = 7;
		public static final int BOTTOM_RIGHT = 8;

		public ExShowScreenMessage(String text, int time) {
			this(1, -1, TOP_CENTER, 0, 0, 0, 0, false, time, false, text);
		}

		public ExShowScreenMessage(String text, int time, int position, boolean showEffect) {
			this(1, -1, position, 0, 0, 0, 0, showEffect, time, false, text);
		}

		public ExShowScreenMessage(int type, int sysMessageId, int position, boolean showEffect, int time, boolean fade,
				String text) {
			this(type, sysMessageId, position, 0, 0, 0, 0, showEffect, time, fade, text);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x38)
					.writeD(type).writeD(sysMessageId).writeD(position).writeD(unk1).writeD(size)
					.writeD(unk2).writeD(unk3).writeD(showEffect ? 1 : 0).writeD(time).writeD(fade ? 1 : 0)
					.writeS(text != null ? text : "")
					.toByteArray();
		}
	}

	/**
	 * 0xed ConfirmDlg: caixa de confirmacao retail (ressurreicao=1510, summon
	 * friend=1842, casamento=1983).
	 */
	record ConfirmDlg(int messageId, String requestorName, int time, int requesterId, List<SystemMessage.Param> params)
			implements GameServerPacket {
		public ConfirmDlg(int messageId, String requestorName, int time, int requesterId) {
			this(messageId, requestorName, time, requesterId,
					List.of(new SystemMessage.Text(requestorName != null ? requestorName : "")));
		}

		public ConfirmDlg(int messageId, String requestorName) {
			this(messageId, requestorName, 15000, 0);
		}

		public ConfirmDlg(int messageId, int time, int requesterId, List<SystemMessage.Param> params) {
			this(messageId, "", time, requesterId, params != null ? List.copyOf(params) : List.of());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xed).writeD(messageId);
			List<SystemMessage.Param> effParams = (params != null && !params.isEmpty())
					? params
					: List.of(new SystemMessage.Text(requestorName != null ? requestorName : ""));
			w.writeD(effParams.size());
			for (SystemMessage.Param p : effParams) {
				switch (p) {
					case SystemMessage.Text t -> w.writeD(0).writeS(t.value() != null ? t.value() : "");
					case SystemMessage.Number n -> w.writeD(1).writeD(n.value());
					case SystemMessage.NpcName n -> w.writeD(2).writeD(1_000_000 + n.npcId());
					case SystemMessage.ItemName i -> w.writeD(3).writeD(i.itemId());
					case SystemMessage.SkillName s -> w.writeD(4).writeD(s.skillId()).writeD(s.level());
					case SystemMessage.ItemNumber n -> w.writeD(6).writeD(n.value());
				}
			}
			w.writeD(time);
			w.writeD(requesterId);
			return w.toByteArray();
		}
	}

	/** 0x86 Ride: animacao de montar/desmontar em Strider ou Wyvern. */
	record Ride(int objectId, int mounted, int rideType, int rideClassId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x86)
					.writeD(objectId)
					.writeD(mounted)
					.writeD(rideType)
					.writeD(rideClassId > 0 ? rideClassId + 1000000 : 0)
					.toByteArray();
		}
	}

	/**
	 * 0xc1 SkillCoolTime: envia tempos de recarga restantes das skills para o
	 * cliente.
	 */
	record SkillCoolTime(List<CoolTimeEntry> entries) implements GameServerPacket {
		public record CoolTimeEntry(int skillId, int level, int reuseSeconds, int remainingSeconds) {
		}

		public SkillCoolTime() {
			this(List.of());
		}

		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xc1).writeD(entries != null ? entries.size() : 0);
			if (entries != null) {
				for (var e : entries) {
					w.writeD(e.skillId()).writeD(e.level()).writeD(e.reuseSeconds()).writeD(e.remainingSeconds());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x32 AskJoinPledge: convite para entrar em cla. */
	record AskJoinPledge(int requestorObjId, String pledgeName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x32).writeD(requestorObjId).writeS(pledgeName != null ? pledgeName : "")
					.toByteArray();
		}
	}

	/** 0x33 JoinPledge: confirmacao de entrada no cla. */
	record JoinPledge(int clanId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x33).writeD(clanId).toByteArray();
		}
	}

	/**
	 * 0x30 ManagePledgePower: exibe a mascara de privilegios de um rank/membro do
	 * cla.
	 */
	record ManagePledgePower(int clanId, int action, int rank, int privs) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x30).writeD(rank).writeD(action).writeD(privs).toByteArray();
		}
	}

	/**
	 * 0xfe:0x28 ExPledgeCrestLarge: envia os bytes do brasao grande (large crest)
	 * do cla.
	 */
	record ExPledgeCrestLarge(int crestId, byte[] data) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x28)
					.writeD(0x00)
					.writeD(crestId)
					.writeD(data != null ? data.length : 0);
			if (data != null && data.length > 0) {
				w.writeB(data);
			}
			return w.toByteArray();
		}
	}

	/** 0xfe:0x3b PledgePowerGradeList: lista de ranks de privilegio do cla. */
	record PledgePowerGradeList(Map<Integer, Integer> rankCounts) implements GameServerPacket {
		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x3b);
			w.writeD(9);
			for (int i = 1; i <= 9; i++) {
				w.writeD(i);
				w.writeD(rankCounts != null ? rankCounts.getOrDefault(i, 0) : 0);
			}
			return w.toByteArray();
		}
	}

	/** 0xfe:0x3c PledgeReceivePowerInfo: privilegios de um membro do cla. */
	record PledgeReceivePowerInfo(int powerGrade, String memberName, int privs) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x3c)
					.writeD(powerGrade)
					.writeS(memberName != null ? memberName : "")
					.writeD(privs)
					.toByteArray();
		}
	}

	/**
	 * 0xfe:0x3d PledgeReceiveMemberInfo: detalhes de um membro do cla na janela
	 * Alt+N.
	 */
	record PledgeReceiveMemberInfo(int pledgeType, String name, String title, int powerGrade, String clanOrSubName,
			String apprenticeOrSponsorName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x3d)
					.writeD(pledgeType)
					.writeS(name != null ? name : "")
					.writeS(title != null ? title : "")
					.writeD(powerGrade)
					.writeS(clanOrSubName != null ? clanOrSubName : "")
					.writeS(apprenticeOrSponsorName != null ? apprenticeOrSponsorName : "")
					.toByteArray();
		}
	}

	/** 0xfe:0x3e PledgeReceiveWarList: lista de guerras do cla na janela Alt+N. */
	record PledgeReceiveWarList(int tab, List<WarEntry> wars) implements GameServerPacket {
		public record WarEntry(String clanName, int state, int memberCount) {
		}

		@Override
		public byte[] encode() {
			var w = new PacketWriter().writeC(0xfe).writeH(0x3e)
					.writeD(tab)
					.writeD(0x00)
					.writeD(wars != null ? wars.size() : 0);
			if (wars != null) {
				for (var war : wars) {
					w.writeS(war.clanName());
					w.writeD(war.state());
					w.writeD(war.memberCount());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xce RelationChanged: atualiza relacao de PvP/Guerra/Cerco sobre outro
	 * personagem.
	 */
	record RelationChanged(int objId, int relation, boolean autoAttackable, int karma, int pvpFlag)
			implements GameServerPacket {
		public static final int RELATION_PVP_FLAG = 0x00002;
		public static final int RELATION_HAS_KARMA = 0x00004;
		public static final int RELATION_LEADER = 0x00080;
		public static final int RELATION_INSIEGE = 0x00200;
		public static final int RELATION_ATTACKER = 0x00400;
		public static final int RELATION_ALLY = 0x00800;
		public static final int RELATION_ENEMY = 0x01000;
		public static final int RELATION_MUTUAL_WAR = 0x08000;
		public static final int RELATION_1SIDED_WAR = 0x10000;
		public static final int RELATION_HAS_PARTY = 0x00008;
		public static final int RELATION_PARTYLEADER = 0x00010;
		public static final int RELATION_CLAN_MEMBER = 0x00040;

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xce)
					.writeD(objId)
					.writeD(relation)
					.writeD(autoAttackable ? 1 : 0)
					.writeD(karma)
					.writeD(pvpFlag)
					.toByteArray();
		}
	}

	/** 0xfe:0x4b ExDuelAskStart: pergunta ao alvo se aceita o duelo. */
	record ExDuelAskStart(String requestorName, int partyDuel) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x4b).writeS(requestorName != null ? requestorName : "")
					.writeD(partyDuel).toByteArray();
		}
	}

	/** 0xfe:0x4c ExDuelReady: prepara os jogadores para inicio do duelo. */
	record ExDuelReady(int partyDuel) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x4c).writeD(partyDuel).toByteArray();
		}
	}

	/** 0xfe:0x4d ExDuelStart: inicia a luta do duelo no cliente. */
	record ExDuelStart(int partyDuel) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x4d).writeD(partyDuel).toByteArray();
		}
	}

	/** 0xfe:0x4e ExDuelEnd: encerra o duelo no cliente. */
	record ExDuelEnd(int partyDuel) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x4e).writeD(partyDuel).toByteArray();
		}
	}

	/** 0xdc ShowCalc: abre a calculadora in-game do cliente. */
	record ShowCalc(int calculatorId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xdc).writeD(calculatorId).toByteArray();
		}
	}

	/** 0xd4 Dice: animacao de rolar dados no chao. */
	record Dice(int charObjId, int itemId, int number, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xd4)
					.writeD(charObjId)
					.writeD(itemId)
					.writeD(number)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0x02 NpcSay: mensagem falada por um NPC no chat local. */
	record NpcSay(int objectId, int textType, int npcId, String text) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x02)
					.writeD(objectId)
					.writeD(textType)
					.writeD(npcId > 1000000 ? npcId : npcId + 1000000)
					.writeS(text != null ? text : "")
					.toByteArray();
		}
	}

	/**
	 * 0x0B SpawnItem: exibe um item ja existente no chao ao entrar no raio de
	 * visao.
	 */
	record SpawnItem(int objectId, int itemId, int x, int y, int z, boolean stackable, int count)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x0b)
					.writeD(objectId)
					.writeD(itemId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(stackable ? 1 : 0)
					.writeD(count)
					.writeD(0x00)
					.toByteArray();
		}
	}

	/** 0x14 AuthLoginFail: falha na autenticacao da sessao no GameServer. */
	record AuthLoginFail(int reason) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x14).writeD(reason).toByteArray();
		}
	}

	/** 0x1C SunRise: transicao de iluminacao para amanhecer no cliente. */
	record SunRise() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x1c).toByteArray();
		}
	}

	/** 0x1D SunSet: transicao de iluminacao para anoitecer no cliente. */
	record SunSet() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x1d).toByteArray();
		}
	}

	/**
	 * 0x29 TargetSelected: confirma selecao visual de alvo para outros
	 * jogadores/cliente.
	 */
	record TargetSelected(int objectId, int targetObjId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x29)
					.writeD(objectId)
					.writeD(targetObjId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0x4B EquipUpdate: atualiza slot de equipamento especifico. */
	record EquipUpdate(int change, int objectId, int bodyPart) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x4b)
					.writeD(change)
					.writeD(objectId)
					.writeD(bodyPart)
					.toByteArray();
		}
	}

	/**
	 * 0x55 PledgeShowMemberListAdd: adiciona um novo membro na lista da janela do
	 * cla.
	 */
	record PledgeShowMemberListAdd(String name, int level, int classId, int sex, int race, int isOnline, int pledgeType)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x55)
					.writeS(name != null ? name : "")
					.writeD(level)
					.writeD(classId)
					.writeD(sex)
					.writeD(race)
					.writeD(isOnline)
					.writeD(pledgeType)
					.toByteArray();
		}
	}

	/** 0x56 PledgeShowMemberListDelete: remove um membro da janela do cla. */
	record PledgeShowMemberListDelete(String playerName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x56).writeS(playerName != null ? playerName : "").toByteArray();
		}
	}

	/** 0x59 VehicleInfo: envia informacoes estaticas de um barco/navio. */
	record VehicleInfo(int objectId, int x, int y, int z, int heading) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x59)
					.writeD(objectId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(heading)
					.toByteArray();
		}
	}

	/** 0x5A VehicleDeparture: movimentacao de partida de um barco/navio. */
	record VehicleDeparture(int objectId, int moveSpeed, int rotationSpeed, int x, int y, int z)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5a)
					.writeD(objectId)
					.writeD(moveSpeed)
					.writeD(rotationSpeed)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0x5B OnVehicleCheckLocation: sincroniza posicao do barco/navio. */
	record OnVehicleCheckLocation(int boatObjId, int x, int y, int z, int heading) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5b)
					.writeD(boatObjId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(heading)
					.toByteArray();
		}
	}

	/** 0x5C GetOnVehicle: confirma embarque do jogador no barco. */
	record GetOnVehicle(int charObjId, int boatObjId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5c)
					.writeD(charObjId)
					.writeD(boatObjId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0x5D GetOffVehicle: confirma desembarque do jogador do barco. */
	record GetOffVehicle(int charObjId, int boatObjId, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x5d)
					.writeD(charObjId)
					.writeD(boatObjId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0x62 StartRotation: inicia rotacao do personagem pelo teclado. */
	record StartRotation(int charObjId, int degree, int side, int speed) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x62)
					.writeD(charObjId)
					.writeD(degree)
					.writeD(side)
					.writeD(speed)
					.toByteArray();
		}
	}

	/** 0x63 StopRotation: para rotacao do personagem pelo teclado. */
	record StopRotation(int charObjId, int degree, int speed) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x63)
					.writeD(charObjId)
					.writeD(degree)
					.writeD(speed)
					.writeC(degree)
					.toByteArray();
		}
	}

	/** 0x65 StartPledgeWar: notificacao de inicio de guerra de cla. */
	record StartPledgeWar(String playerName, String pledgeName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x65)
					.writeS(playerName != null ? playerName : "")
					.writeS(pledgeName != null ? pledgeName : "")
					.toByteArray();
		}
	}

	/** 0x67 StopPledgeWar: notificacao de fim de guerra de cla. */
	record StopPledgeWar(String pledgeName, String playerName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x67)
					.writeS(pledgeName != null ? pledgeName : "")
					.writeS(playerName != null ? playerName : "")
					.toByteArray();
		}
	}

	/** 0x69 SurrenderPledgeWar: notificacao de rendicao de guerra de cla. */
	record SurrenderPledgeWar(String pledgeName, String playerName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x69)
					.writeS(pledgeName != null ? pledgeName : "")
					.writeS(playerName != null ? playerName : "")
					.toByteArray();
		}
	}

	/** 0x70 OpenUrl: solicita ao cliente abrir uma URL no navegador. */
	record OpenUrl(String url) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x70).writeS(url != null ? url : "").toByteArray();
		}
	}

	/** 0x71 MoveToLocationInVehicle: movimento do jogador dentro do barco. */
	record MoveToLocationInVehicle(int charObjId, int boatId, int targetX, int targetY, int targetZ, int originX,
			int originY, int originZ) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x71)
					.writeD(charObjId)
					.writeD(boatId)
					.writeD(targetX)
					.writeD(targetY)
					.writeD(targetZ)
					.writeD(originX)
					.writeD(originY)
					.writeD(originZ)
					.toByteArray();
		}
	}

	/** 0x72 StopMoveInVehicle: parada de movimento dentro do barco. */
	record StopMoveInVehicle(int charObjId, int boatId, int x, int y, int z, int heading) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x72)
					.writeD(charObjId)
					.writeD(boatId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(heading)
					.toByteArray();
		}
	}

	/** 0x73 ValidateLocationInVehicle: valida posicao dentro do barco. */
	record ValidateLocationInVehicle(int charObjId, int boatId, int x, int y, int z, int heading)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x73)
					.writeD(charObjId)
					.writeD(boatId)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(heading)
					.toByteArray();
		}
	}

	record TradeOwnAddItem(int type1, int objectId, int itemId, int count, int type2, int bodyPart, int enchant) {
	}

	/**
	 * 0x74 TradeItemUpdate: atualiza inventario disponivel durante a janela de
	 * trade.
	 */
	record TradeItemUpdate(List<TradeOwnAddItem> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = items != null ? items.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0x74).writeH(size);
			if (items != null) {
				for (TradeOwnAddItem it : items) {
					w.writeH(it.count() > 1 ? 3 : 2)
							.writeH(it.type1())
							.writeD(it.objectId())
							.writeD(it.itemId())
							.writeD(it.count())
							.writeH(it.type2())
							.writeH(0x00)
							.writeD(it.bodyPart())
							.writeH(it.enchant())
							.writeH(0x00)
							.writeH(0x00);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x7B FriendStatus: notifica login/logout de um amigo. */
	record FriendStatus(boolean online, String name, int objectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x7b)
					.writeD(online ? 1 : 0)
					.writeS(name != null ? name : "")
					.writeD(objectId)
					.toByteArray();
		}
	}

	/**
	 * 0x82 PledgeShowMemberListDeleteAll: limpa a lista de membros da janela do
	 * cla.
	 */
	record PledgeShowMemberListDeleteAll() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x82).toByteArray();
		}
	}

	/**
	 * 0x83 PledgeInfo: envia o nome do cla e da alianca para renderizar acima dos
	 * personagens.
	 */
	record PledgeInfo(int clanId, String clanName, String allyName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x83)
					.writeD(clanId)
					.writeS(clanName != null ? clanName : "")
					.writeS(allyName != null ? allyName : "")
					.toByteArray();
		}
	}

	/** 0x8C ServerObjectInfo: informacoes de objeto estatico do servidor. */
	record ServerObjectInfo(int objectId, int idTemplate, String name, boolean attackable, int x, int y, int z,
			int heading, double collisionRadius, double collisionHeight, int currentHp, int maxHp)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x8c)
					.writeD(objectId)
					.writeD(idTemplate + 1000000)
					.writeS(name != null ? name : "")
					.writeD(attackable ? 1 : 0)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(heading)
					.writeF(1.0)
					.writeF(1.0)
					.writeF(collisionRadius)
					.writeF(collisionHeight)
					.writeD(attackable ? currentHp : 0)
					.writeD(attackable ? maxHp : 0)
					.writeD(0x01)
					.writeD(0x00)
					.toByteArray();
		}
	}

	/** Entrada de sala de Party Matching. */
	record PartyMatchRoomEntry(int id, String title, int location, int minLvl, int maxLvl, int members, int maxMembers,
			String leaderName) {
	}

	/** 0x96 PartyMatchList: lista de salas de Party Matching abertas. */
	record PartyMatchList(List<PartyMatchRoomEntry> rooms) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = rooms != null ? rooms.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0x96)
					.writeD(size > 0 ? 1 : 0)
					.writeD(size);
			if (rooms != null) {
				for (PartyMatchRoomEntry r : rooms) {
					w.writeD(r.id())
							.writeS(r.title() != null ? r.title() : "")
							.writeD(r.location())
							.writeD(r.minLvl())
							.writeD(r.maxLvl())
							.writeD(r.members())
							.writeD(r.maxMembers())
							.writeS(r.leaderName() != null ? r.leaderName() : "");
				}
			}
			return w.toByteArray();
		}
	}

	/** 0x97 PartyMatchDetail: detalhes de uma sala de Party Matching. */
	record PartyMatchDetail(int roomId, int maxMembers, int minLvl, int maxLvl, int lootType, int location,
			String title) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x97)
					.writeD(roomId)
					.writeD(maxMembers)
					.writeD(minLvl)
					.writeD(maxLvl)
					.writeD(lootType)
					.writeD(location)
					.writeS(title != null ? title : "")
					.toByteArray();
		}
	}

	/** Coordenada de membro da party para o radar/mapa. */
	record PartyMemberPosEntry(int objectId, int x, int y, int z) {
	}

	/**
	 * 0xA7 PartyMemberPosition: atualiza posicoes dos membros da party no
	 * mapa/radar.
	 */
	record PartyMemberPosition(List<PartyMemberPosEntry> locations) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = locations != null ? locations.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xa7).writeD(size);
			if (locations != null) {
				for (PartyMemberPosEntry loc : locations) {
					w.writeD(loc.objectId())
							.writeD(loc.x())
							.writeD(loc.y())
							.writeD(loc.z());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xA8 AskJoinAlly: popup de convite para entrar em uma Alianca. */
	record AskJoinAlly(int requestorObjId, String requestorName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xa8)
					.writeD(requestorObjId)
					.writeS(requestorName != null ? requestorName : "")
					.toByteArray();
		}
	}

	/** 0xAE AllyCrest: envia os bytes do brasao da alianca (8x12 BMP). */
	record AllyCrest(int crestId, byte[] data) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xae)
					.writeD(crestId)
					.writeD(data != null ? data.length : 0);
			if (data != null && data.length > 0) {
				w.writeB(data);
			}
			return w.toByteArray();
		}
	}

	/** Item de inventario de Pet. */
	record PetInventoryItem(int change, int type1, int objectId, int itemId, int count, int type2, int customType1,
			boolean equipped, int bodyPart, int enchantLevel, int customType2) {
	}

	/** 0xB2 PetItemList: lista de itens no inventario do Pet. */
	record PetItemList(List<PetInventoryItem> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = items != null ? items.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xb2).writeH(size);
			if (items != null) {
				for (PetInventoryItem it : items) {
					w.writeH(it.type1())
							.writeD(it.objectId())
							.writeD(it.itemId())
							.writeD(it.count())
							.writeH(it.type2())
							.writeH(it.customType1())
							.writeH(it.equipped() ? 1 : 0)
							.writeD(it.bodyPart())
							.writeH(it.enchantLevel())
							.writeH(it.customType2());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xB3 PetInventoryUpdate: atualizacao incremental do inventario do Pet. */
	record PetInventoryUpdate(List<PetInventoryItem> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = items != null ? items.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xb3).writeH(size);
			if (items != null) {
				for (PetInventoryItem it : items) {
					w.writeH(it.change())
							.writeH(it.type1())
							.writeD(it.objectId())
							.writeD(it.itemId())
							.writeD(it.count())
							.writeH(it.type2())
							.writeH(it.customType1())
							.writeH(it.equipped() ? 1 : 0)
							.writeD(it.bodyPart())
							.writeH(it.enchantLevel())
							.writeH(it.customType2());
				}
			}
			return w.toByteArray();
		}
	}

	/** Informacao de cla membro de uma alianca. */
	record AllyClanInfo(String clanName, int clanLevel, String leaderName, int totalMembers, int onlineMembers) {
	}

	/** 0xB4 AllyInfo: informacoes detalhadas da alianca (/allyinfo). */
	record AllyInfo(String allyName, int totalMembers, int onlineMembers, String leaderClanName,
			String leaderPlayerName, List<AllyClanInfo> allies) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = allies != null ? allies.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xb4)
					.writeS(allyName != null ? allyName : "")
					.writeD(totalMembers)
					.writeD(onlineMembers)
					.writeS(leaderClanName != null ? leaderClanName : "")
					.writeS(leaderPlayerName != null ? leaderPlayerName : "")
					.writeD(size);
			if (allies != null) {
				for (AllyClanInfo aci : allies) {
					w.writeS(aci.clanName() != null ? aci.clanName() : "")
							.writeD(0x00)
							.writeD(aci.clanLevel())
							.writeS(aci.leaderName() != null ? aci.leaderName() : "")
							.writeD(aci.totalMembers())
							.writeD(aci.onlineMembers());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xBA VehicleStarted: estado de movimento do barco. */
	record VehicleStarted(int objectId, int state) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xba).writeD(objectId).writeD(state).toByteArray();
		}
	}

	/**
	 * 0xC2 PackageToList: lista de personagens da mesma conta para envio de
	 * Freight.
	 */
	record PackageToList(Map<Integer, String> players) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = players != null ? players.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xc2).writeD(size);
			if (players != null) {
				for (Map.Entry<Integer, String> e : players.entrySet()) {
					w.writeD(e.getKey()).writeS(e.getValue() != null ? e.getValue() : "");
				}
			}
			return w.toByteArray();
		}
	}

	record WarehouseItem(int type1, int objectId, int itemId, int count, int type2, int bodyPart, int enchantLevel,
			int customType1, int customType2, int augmentationId) {
		public WarehouseItem(int type1, int objectId, int itemId, int count, int type2, int bodyPart,
				int enchantLevel) {
			this(type1, objectId, itemId, count, type2, bodyPart, enchantLevel, 0, 0, 0);
		}
	}

	/**
	 * 0xC3 PackageSendableList: lista de itens enviaveis via Freight para outro
	 * personagem da conta.
	 */
	record PackageSendableList(int playerObjId, int adena, List<WarehouseItem> items) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = items != null ? items.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xc3)
					.writeD(playerObjId)
					.writeD(adena)
					.writeD(size);
			if (items != null) {
				for (WarehouseItem it : items) {
					w.writeH(it.type1())
							.writeD(it.objectId())
							.writeD(it.itemId())
							.writeD(it.count())
							.writeH(it.type2())
							.writeH(it.customType1())
							.writeD(it.bodyPart())
							.writeH(it.enchantLevel())
							.writeH(it.customType2())
							.writeH(0x00)
							.writeD(it.objectId())
							.writeD(it.augmentationId() & 0x0000FFFF)
							.writeD(it.augmentationId() >>> 16);
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xC5 FlyToLocation: deslocamento aereo / salto / empurrao de skill. */
	record FlyToLocation(int charObjId, int destX, int destY, int destZ, int origX, int origY, int origZ, int flyType)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xc5)
					.writeD(charObjId)
					.writeD(destX)
					.writeD(destY)
					.writeD(destZ)
					.writeD(origX)
					.writeD(origY)
					.writeD(origZ)
					.writeD(flyType)
					.toByteArray();
		}
	}

	/** 0xC8 NormalCamera: restaura a camera normal apos cutscene/SpecialCamera. */
	record NormalCamera() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xc8).toByteArray();
		}
	}

	/** 0xCC TitleUpdate: atualiza o titulo de um personagem em tempo real. */
	record TitleUpdate(int objectId, String title) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xcc)
					.writeD(objectId)
					.writeS(title != null ? title : "")
					.toByteArray();
		}
	}

	/**
	 * 0xCD PledgeStatusChanged: atualiza status de lider, clanId, crestId e allyId.
	 */
	record PledgeStatusChanged(int leaderId, int clanId, int crestId, int allyId, int allyCrestId)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xcd)
					.writeD(leaderId)
					.writeD(clanId)
					.writeD(crestId)
					.writeD(allyId)
					.writeD(allyCrestId)
					.writeD(0)
					.writeD(0)
					.toByteArray();
		}
	}

	/**
	 * 0xD1 SetSummonRemainTime: atualiza a barra de tempo restante de invocacao
	 * (Summon/Pet).
	 */
	record SetSummonRemainTime(int maxTime, int remainingTime) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xd1)
					.writeD(maxTime)
					.writeD(remainingTime)
					.toByteArray();
		}
	}

	/** 0xD5 Snoop: monitora mensagens de chat de um jogador (ferramenta de GM). */
	record Snoop(int convoId, String name, int type, String speaker, String msg) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xd5)
					.writeD(convoId)
					.writeS(name != null ? name : "")
					.writeD(0x00)
					.writeD(type)
					.writeS(speaker != null ? speaker : "")
					.writeS(msg != null ? msg : "")
					.toByteArray();
		}
	}

	/** Entrada de receita com preco na loja de criacao (Dwarven Manufacture). */
	record ManufactureRecipeEntry(int recipeId, int cost) {
	}

	/**
	 * 0xD8 RecipeShopManageList: abre gerenciamento da oficina de criacao (Dwarven
	 * Manufacture).
	 */
	record RecipeShopManageList(int sellerObjId, int adena, boolean isDwarven, List<Integer> recipes,
			List<ManufactureRecipeEntry> createList) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xd8)
					.writeD(sellerObjId)
					.writeD(adena)
					.writeD(isDwarven ? 0x00 : 0x01);
			int rSize = recipes != null ? recipes.size() : 0;
			w.writeD(rSize);
			if (recipes != null) {
				int idx = 1;
				for (int recipeId : recipes) {
					w.writeD(recipeId).writeD(idx++);
				}
			}
			int cSize = createList != null ? createList.size() : 0;
			w.writeD(cSize);
			if (createList != null) {
				for (ManufactureRecipeEntry e : createList) {
					w.writeD(e.recipeId()).writeD(0x00).writeD(e.cost());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xD9 RecipeShopSellList: abre vitrine de criacao de um anao para um
	 * comprador.
	 */
	record RecipeShopSellList(int manufacturerObjId, int currentMp, int maxMp, int buyerAdena,
			List<ManufactureRecipeEntry> createList) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = createList != null ? createList.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xd9)
					.writeD(manufacturerObjId)
					.writeD(currentMp)
					.writeD(maxMp)
					.writeD(buyerAdena)
					.writeD(size);
			if (createList != null) {
				for (ManufactureRecipeEntry e : createList) {
					w.writeD(e.recipeId()).writeD(0x00).writeD(e.cost());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xDA RecipeShopItemInfo: exibe detalhes e chance de uma receita na loja de
	 * criacao.
	 */
	record RecipeShopItemInfo(int manufacturerObjId, int recipeId, int currentMp, int maxMp, int status)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xda)
					.writeD(manufacturerObjId)
					.writeD(recipeId)
					.writeD(currentMp)
					.writeD(maxMp)
					.writeD(status)
					.toByteArray();
		}
	}

	/**
	 * 0xDB RecipeShopMsg: exibe a placa acima da cabeca do artesao em loja de
	 * criacao.
	 */
	record RecipeShopMsg(int charObjId, String storeName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xdb)
					.writeD(charObjId)
					.writeS(storeName != null ? storeName : "")
					.toByteArray();
		}
	}

	/** Corredor da Monster Race Track. */
	record MonRaceRunner(int objectId, int npcId, String name, int x, int y, int z, int destX, int destY, int destZ,
			double collisionHeight, double collisionRadius, int[] speeds) {
	}

	/** 0xDD MonRaceInfo: informacoes dos corredores da Monster Race Track. */
	record MonRaceInfo(int unknown1, int unknown2, List<MonRaceRunner> runners) implements GameServerPacket {
		public MonRaceInfo(int unknown1, int unknown2) {
			this(unknown1, unknown2, List.of());
		}

		@Override
		public byte[] encode() {
			int size = runners != null ? runners.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xdd)
					.writeD(unknown1)
					.writeD(unknown2)
					.writeD(size);
			if (runners != null) {
				for (int i = 0; i < size; i++) {
					MonRaceRunner r = runners.get(i);
					w.writeD(r.objectId())
							.writeD(r.npcId() + 1000000)
							.writeD(r.x()).writeD(r.y()).writeD(r.z())
							.writeD(r.destX()).writeD(r.destY()).writeD(r.destZ())
							.writeF(r.collisionHeight()).writeF(r.collisionRadius())
							.writeD(120);
					for (int j = 0; j < 20; j++) {
						if (unknown1 == 0) {
							w.writeC(r.speeds() != null && j < r.speeds().length ? r.speeds()[j] : 0);
						} else {
							w.writeC(0);
						}
					}
					w.writeD(0);
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xDF ObservationMode: entra no modo de observacao (Broadcasting Tower /
	 * Olimpiadas).
	 */
	record ObservationMode(int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xdf)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeC(0x00)
					.writeC(0xc0)
					.writeC(0x00)
					.writeC(0x00)
					.toByteArray();
		}
	}

	/**
	 * 0xE0 ObservationReturn: sai do modo de observacao e retorna ao personagem.
	 */
	record ObservationReturn(int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xe0)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0xE1 ChairSit: animacao de sentar em trono/cadeira estatica. */
	record ChairSit(int playerId, int staticObjectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xe1)
					.writeD(playerId)
					.writeD(staticObjectId)
					.toByteArray();
		}
	}

	/** Semente disponivel para compra no Manor. */
	record SeedBuyEntry(int itemId, int count, int price) {
	}

	/** 0xE8 BuyListSeed: lista de sementes a venda pelo Manor Manager. */
	record BuyListSeed(int money, int manorId, List<SeedBuyEntry> seeds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = seeds != null ? seeds.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xe8)
					.writeD(money)
					.writeD(manorId)
					.writeH(size);
			if (seeds != null) {
				for (SeedBuyEntry s : seeds) {
					w.writeH(0x04)
							.writeD(s.itemId())
							.writeD(s.itemId())
							.writeD(s.count())
							.writeH(0x04)
							.writeH(0x00)
							.writeD(s.price());
				}
			}
			return w.toByteArray();
		}
	}

	/** Colheita disponivel para venda no Manor. */
	record ProcureSellEntry(int type1, int objectId, int itemId, int count, int type2, int price) {
		public ProcureSellEntry(int type1, int objectId, int itemId, int count, int type2) {
			this(type1, objectId, itemId, count, type2, 0);
		}
	}

	/** 0xE9 SellListProcure: lista de colheitas para entrega no Manor. */
	record SellListProcure(int money, int castleId, List<ProcureSellEntry> sellList) implements GameServerPacket {
		public SellListProcure(int money, List<ProcureSellEntry> sellList) {
			this(money, 0, sellList);
		}

		@Override
		public byte[] encode() {
			int size = sellList != null ? sellList.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xe9)
					.writeD(money)
					.writeD(castleId)
					.writeH(size);
			if (sellList != null) {
				for (ProcureSellEntry it : sellList) {
					w.writeH(it.type1())
							.writeD(it.objectId())
							.writeD(it.itemId())
							.writeD(it.count())
							.writeH(it.type2())
							.writeH(0x00)
							.writeD(it.price());
				}
			}
			return w.toByteArray();
		}
	}

	record EffectInfo(int skillId, int level, int durationSeconds) {
	}

	/**
	 * 0xEE PartySpelled: exibe buffs/debuffs ativos em um membro da party ou
	 * summon.
	 */
	record PartySpelled(int charType, int objectId, List<EffectInfo> effects) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = effects != null ? effects.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xee)
					.writeD(charType)
					.writeD(objectId)
					.writeD(size);
			if (effects != null) {
				for (EffectInfo ef : effects) {
					w.writeD(ef.skillId())
							.writeH(ef.level())
							.writeD(ef.durationSeconds());
				}
			}
			return w.toByteArray();
		}
	}

	/** Item disponivel para provar (Wear/Try-On). */
	record ShopPreviewProduct(int itemId, int type2, int bodyPart, int wearPrice) {
	}

	/** 0xEF ShopPreviewList: abre lista de equipamentos para provar na loja. */
	record ShopPreviewList(int listId, int money, List<ShopPreviewProduct> products) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = products != null ? products.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xef)
					.writeC(0xc0).writeC(0x13).writeC(0x00).writeC(0x00)
					.writeD(money)
					.writeD(listId)
					.writeH(size);
			if (products != null) {
				for (ShopPreviewProduct p : products) {
					w.writeD(p.itemId())
							.writeH(p.type2())
							.writeH(p.bodyPart())
							.writeD(p.wearPrice());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xF0 ShopPreviewInfo: aplica visual temporario dos itens provados nos 17
	 * slots.
	 */
	record ShopPreviewInfo(int[] paperdollItemIds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			PacketWriter w = new PacketWriter().writeC(0xf0).writeD(17);
			for (int i = 0; i < 17; i++) {
				int val = (paperdollItemIds != null && i < paperdollItemIds.length) ? paperdollItemIds[i] : 0;
				w.writeD(val);
			}
			return w.toByteArray();
		}
	}

	/** 0xF1 CameraMode: alterna modo de camera em primeira/terceira pessoa. */
	record CameraMode(int mode) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf1).writeD(mode).toByteArray();
		}
	}

	/** 0xF2 ShowXMasSeal: exibe cartao/selo de evento natalino na tela. */
	record ShowXMasSeal(int itemId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf2).writeD(itemId).toByteArray();
		}
	}

	/**
	 * 0xF4 ShortBuffStatusUpdate: atualiza slot de buff de curta duracao (ex:
	 * Healing Potion / Regen).
	 */
	record ShortBuffStatusUpdate(int skillId, int skillLvl, int duration) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf4)
					.writeD(skillId)
					.writeD(skillLvl)
					.writeD(duration)
					.toByteArray();
		}
	}

	/**
	 * 0xF7 ClanHallDecoration: envia estado das decoracoes e funcoes de um Clan
	 * Hall.
	 */
	record ClanHallDecoration(int clanHallId, int hpRegen, int mpRegen, int expRestore, int teleport, int curtains,
			int itemCreate, int support, int platform) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf7)
					.writeD(clanHallId)
					.writeC(hpRegen)
					.writeC(mpRegen)
					.writeC(mpRegen)
					.writeC(expRestore)
					.writeC(teleport)
					.writeC(0)
					.writeC(curtains)
					.writeC(itemCreate)
					.writeC(support)
					.writeC(support)
					.writeC(platform)
					.writeC(itemCreate)
					.writeD(0)
					.writeD(0)
					.toByteArray();
		}
	}

	/** 0xF9 GameGuardQuery: solicita validacao de GameGuard ao cliente. */
	record GameGuardQuery() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf9).toByteArray();
		}
	}

	/** 0xFE:0x01 ExRegMax: define limite de registro em eventos. */
	record ExRegMax(int count, int time, double max) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x01)
					.writeD(1)
					.writeD(count)
					.writeD(time)
					.writeF(max)
					.toByteArray();
		}
	}

	/** 0xFE:0x09 ExColosseumFenceInfo: renderiza cercas de arena/coliseu. */
	record ExColosseumFenceInfo(int objectId, int type, int x, int y, int z, int sizeX, int sizeY)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x09)
					.writeD(objectId)
					.writeD(type)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(sizeX)
					.writeD(sizeY)
					.toByteArray();
		}
	}

	/** Membro de sala de Party Matching. */
	record PartyRoomMemberEntry(int objectId, String name, int classId, int level, int region, int memberType) {
	}

	/**
	 * 0xFE:0x0E ExPartyRoomMember: envia lista de membros na sala de Party
	 * Matching.
	 */
	record ExPartyRoomMember(int mode, List<PartyRoomMemberEntry> members) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = members != null ? members.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x0e)
					.writeD(mode)
					.writeD(size);
			if (members != null) {
				for (PartyRoomMemberEntry m : members) {
					w.writeD(m.objectId())
							.writeS(m.name() != null ? m.name() : "")
							.writeD(m.classId())
							.writeD(m.level())
							.writeD(m.region())
							.writeD(m.memberType());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xFE:0x0F ExClosePartyRoom: fecha a janela da sala de Party Matching. */
	record ExClosePartyRoom() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x0f).toByteArray();
		}
	}

	/**
	 * 0xFE:0x10 ExManagePartyRoomMember: adiciona/atualiza/remove membro da sala de
	 * Party Matching.
	 */
	record ExManagePartyRoomMember(int mode, PartyRoomMemberEntry member) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x10)
					.writeD(mode)
					.writeD(member != null ? member.objectId() : 0)
					.writeS(member != null && member.name() != null ? member.name() : "")
					.writeD(member != null ? member.classId() : 0)
					.writeD(member != null ? member.level() : 1)
					.writeD(member != null ? member.region() : 0)
					.writeD(member != null ? member.memberType() : 0)
					.toByteArray();
		}
	}

	/** 0xFE:0x19 ExQuestInfo: abre/atualiza a janela de Quests no cliente. */
	record ExQuestInfo() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x19).toByteArray();
		}
	}

	/** Entrada de informacao de semente do Manor. */
	record SeedInfoEntry(int seedId, int canProduce, int startProduce, int price, int seedLevel, int reward1Id,
			int reward2Id) {
	}

	/** 0xFE:0x1C ExShowSeedInfo: exibe informacoes de sementes de um Manor. */
	record ExShowSeedInfo(int manorId, List<SeedInfoEntry> seeds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = seeds != null ? seeds.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x1c)
					.writeC(0)
					.writeD(manorId)
					.writeD(0)
					.writeD(size);
			if (seeds != null) {
				for (SeedInfoEntry s : seeds) {
					w.writeD(s.seedId())
							.writeD(s.canProduce())
							.writeD(s.startProduce())
							.writeD(s.price())
							.writeD(s.seedLevel())
							.writeC(1).writeD(s.reward1Id())
							.writeC(1).writeD(s.reward2Id());
				}
			}
			return w.toByteArray();
		}
	}

	/** Entrada de informacao de colheita (Crop) do Manor. */
	record CropInfoEntry(int cropId, int amount, int startAmount, int price, int rewardType, int seedLevel,
			int reward1Id, int reward2Id) {
	}

	/** 0xFE:0x1D ExShowCropInfo: exibe informacoes de colheitas de um Manor. */
	record ExShowCropInfo(int manorId, List<CropInfoEntry> crops) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = crops != null ? crops.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x1d)
					.writeC(0)
					.writeD(manorId)
					.writeD(0)
					.writeD(size);
			if (crops != null) {
				for (CropInfoEntry c : crops) {
					w.writeD(c.cropId())
							.writeD(c.amount())
							.writeD(c.startAmount())
							.writeD(c.price())
							.writeC(c.rewardType())
							.writeD(c.seedLevel())
							.writeC(1).writeD(c.reward1Id())
							.writeC(1).writeD(c.reward2Id());
				}
			}
			return w.toByteArray();
		}
	}

	/** Entrada padrao de Manor. */
	record ManorDefaultEntry(int cropId, int seedLevel, int seedPrice, int cropPrice, int reward1Id, int reward2Id) {
	}

	/** 0xFE:0x1E ExShowManorDefaultInfo: exibe tabela de valores base do Manor. */
	record ExShowManorDefaultInfo(List<ManorDefaultEntry> crops) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = crops != null ? crops.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x1e)
					.writeC(0)
					.writeD(size);
			if (crops != null) {
				for (ManorDefaultEntry c : crops) {
					w.writeD(c.cropId())
							.writeD(c.seedLevel())
							.writeD(c.seedPrice())
							.writeD(c.cropPrice())
							.writeC(1).writeD(c.reward1Id())
							.writeC(1).writeD(c.reward2Id());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x1F ExShowSeedSetting: janela de configuracao de sementes do castelo.
	 */
	record ExShowSeedSetting(int manorId, List<SeedInfoEntry> seeds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = seeds != null ? seeds.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x1f)
					.writeD(manorId)
					.writeD(size);
			if (seeds != null) {
				for (SeedInfoEntry s : seeds) {
					w.writeD(s.seedId())
							.writeD(s.seedLevel())
							.writeC(1).writeD(s.reward1Id())
							.writeC(1).writeD(s.reward2Id())
							.writeD(s.startProduce())
							.writeD(s.price())
							.writeD(Math.max(1, s.price() / 2))
							.writeD(s.price() * 10)
							.writeD(s.canProduce())
							.writeD(s.price())
							.writeD(s.startProduce())
							.writeD(s.price());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x20 ExShowCropSetting: janela de configuracao de colheitas do castelo.
	 */
	record ExShowCropSetting(int manorId, List<CropInfoEntry> crops) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = crops != null ? crops.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x20)
					.writeD(manorId)
					.writeD(size);
			if (crops != null) {
				for (CropInfoEntry c : crops) {
					w.writeD(c.cropId())
							.writeD(c.seedLevel())
							.writeC(1).writeD(c.reward1Id())
							.writeC(1).writeD(c.reward2Id())
							.writeD(c.startAmount())
							.writeD(0)
							.writeD(Math.max(1, c.price() / 2))
							.writeD(c.price() * 10)
							.writeD(c.amount())
							.writeD(c.price())
							.writeC(c.rewardType())
							.writeD(c.startAmount())
							.writeD(c.price())
							.writeC(c.rewardType());
				}
			}
			return w.toByteArray();
		}
	}

	/** Item de colheita do jogador para venda no Manor. */
	record SellCropEntry(int objectId, int cropId, int seedLevel, int reward1Id, int reward2Id, int manorId,
			int buyResidual, int buyPrice, int rewardType, int myCount) {
	}

	/**
	 * 0xFE:0x21 ExShowSellCropList: janela de venda de colheitas do jogador no
	 * Manor.
	 */
	record ExShowSellCropList(int manorId, List<SellCropEntry> crops) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = crops != null ? crops.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x21)
					.writeD(manorId)
					.writeD(size);
			if (crops != null) {
				for (SellCropEntry c : crops) {
					w.writeD(c.objectId())
							.writeD(c.cropId())
							.writeD(c.seedLevel())
							.writeC(1).writeD(c.reward1Id())
							.writeC(1).writeD(c.reward2Id())
							.writeD(c.manorId())
							.writeD(c.buyResidual())
							.writeD(c.buyPrice())
							.writeC(c.rewardType())
							.writeD(c.myCount());
				}
			}
			return w.toByteArray();
		}
	}

	/** Cotacao de compra de colheita por castelo. */
	record CropProcureDetailEntry(int castleId, int amount, int price, int rewardType) {
	}

	/**
	 * 0xFE:0x22 ExShowProcureCropDetail: detalhes de cotacao de uma colheita em
	 * todos os castelos.
	 */
	record ExShowProcureCropDetail(int cropId, List<CropProcureDetailEntry> castleCrops) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = castleCrops != null ? castleCrops.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x22)
					.writeD(cropId)
					.writeD(size);
			if (castleCrops != null) {
				for (CropProcureDetailEntry c : castleCrops) {
					w.writeD(c.castleId())
							.writeD(c.amount())
							.writeD(c.price())
							.writeC(c.rewardType());
				}
			}
			return w.toByteArray();
		}
	}

	/** Entrada de Heroi no Monument of Heroes. */
	record HeroListEntry(String charName, int classId, String clanName, int clanCrestId, String allyName,
			int allyCrestId, int count) {
	}

	/** 0xFE:0x23 ExHeroList: lista de herois das Olimpiadas. */
	record ExHeroList(List<HeroListEntry> heroes) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = heroes != null ? heroes.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x23).writeD(size);
			if (heroes != null) {
				for (HeroListEntry h : heroes) {
					w.writeS(h.charName() != null ? h.charName() : "")
							.writeD(h.classId())
							.writeS(h.clanName() != null ? h.clanName() : "")
							.writeD(h.clanCrestId())
							.writeS(h.allyName() != null ? h.allyName() : "")
							.writeD(h.allyCrestId())
							.writeD(h.count());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x24 ExServerPrimitive: renderiza linhas e pontos 3D de debug no
	 * cliente.
	 */
	record ExServerPrimitive(String name, int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x24)
					.writeS(name != null ? name : "Debug")
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.writeD(Integer.MAX_VALUE)
					.writeD(Integer.MAX_VALUE)
					.writeD(0)
					.toByteArray();
		}
	}

	/** 0xFE:0x25 ExOpenMPCC: abre a interface de Command Channel (MPCC). */
	record ExOpenMPCC() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x25).toByteArray();
		}
	}

	/** 0xFE:0x26 ExCloseMPCC: fecha a interface de Command Channel (MPCC). */
	record ExCloseMPCC() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x26).toByteArray();
		}
	}

	/**
	 * 0xFE:0x27 ExAskJoinMPCC: popup de convite para entrar em Command Channel
	 * (MPCC).
	 */
	record ExAskJoinMPCC(String requestorName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x27)
					.writeS(requestorName != null ? requestorName : "")
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x2A ExOlympiadSpelledInfo: buffs de competidor para espectadores das
	 * Olimpiadas.
	 */
	record ExOlympiadSpelledInfo(int playerObjId, List<EffectInfo> effects) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = effects != null ? effects.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x2a)
					.writeD(playerObjId)
					.writeD(size);
			if (effects != null) {
				for (EffectInfo ef : effects) {
					w.writeD(ef.skillId())
							.writeH(ef.level())
							.writeD(ef.durationSeconds());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xFE:0x2C ExOlympiadMatchEnd: notifica fim da partida das Olimpiadas. */
	record ExOlympiadMatchEnd() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x2c).toByteArray();
		}
	}

	/**
	 * 0xFE:0x2D ExMailArrived: notifica chegada de novo e-mail/mensagem na caixa
	 * postal.
	 */
	record ExMailArrived() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x2d).toByteArray();
		}
	}

	/** Party participante de um Command Channel (MPCC). */
	record MpccPartyEntry(String leaderName, int leaderObjId, int memberCount) {
	}

	/**
	 * 0xFE:0x30 ExMultiPartyCommandChannelInfo: informacoes das parties no Command
	 * Channel.
	 */
	record ExMultiPartyCommandChannelInfo(String channelLeaderName, int lootType, int totalMembers,
			List<MpccPartyEntry> parties) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = parties != null ? parties.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x30)
					.writeS(channelLeaderName != null ? channelLeaderName : "")
					.writeD(lootType)
					.writeD(totalMembers)
					.writeD(size);
			if (parties != null) {
				for (MpccPartyEntry p : parties) {
					w.writeS(p.leaderName() != null ? p.leaderName() : "")
							.writeD(p.leaderObjId())
							.writeD(p.memberCount());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x31 ExPCCafePointInfo: atualiza pontuacao de PC Bang Points na
	 * interface.
	 */
	record ExPCCafePointInfo(int score, int modify, int periodType, int remainingTime, int pointType)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x31)
					.writeD(score)
					.writeD(modify)
					.writeC(periodType)
					.writeD(remainingTime)
					.writeC(pointType)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x32 ExSetCompassZoneCode: atualiza o indicador de zona
	 * (Peace/PvP/Siege/SevenSigns) na bussola.
	 */
	record ExSetCompassZoneCode(int zoneType) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x32).writeD(zoneType).toByteArray();
		}
	}

	/**
	 * 0xFE:0x34 ExAskJoinPartyRoom: popup de convite para entrar em uma sala de
	 * Party Matching.
	 */
	record ExAskJoinPartyRoom(String requestorName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x34)
					.writeS(requestorName != null ? requestorName : "")
					.toByteArray();
		}
	}

	/** Jogador na fila de espera de Party Matching. */
	record PartyMatchingWaiterEntry(String name, int classId, int level) {
	}

	/**
	 * 0xFE:0x35 ExListPartyMatchingWaitingRoom: lista de jogadores aguardando grupo
	 * no Party Matching.
	 */
	record ExListPartyMatchingWaitingRoom(int totalCount, List<PartyMatchingWaiterEntry> waiters)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = waiters != null ? waiters.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x35)
					.writeD(totalCount)
					.writeD(size);
			if (waiters != null) {
				for (PartyMatchingWaiterEntry m : waiters) {
					w.writeS(m.name() != null ? m.name() : "")
							.writeD(m.classId())
							.writeD(m.level());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x37 ExShowAdventurerGuideBook: abre o Guia do Aventureiro no cliente.
	 */
	record ExShowAdventurerGuideBook() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x37).toByteArray();
		}
	}

	/** Habilidade de cla. */
	record PledgeSkillEntry(int skillId, int skillLevel) {
	}

	/** 0xFE:0x39 PledgeSkillList: envia a lista completa de Clan Skills. */
	record PledgeSkillList(List<PledgeSkillEntry> skills) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = skills != null ? skills.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x39).writeD(size);
			if (skills != null) {
				for (PledgeSkillEntry sk : skills) {
					w.writeD(sk.skillId()).writeD(sk.skillLevel());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xFE:0x3A PledgeSkillListAdd: adiciona ou atualiza uma Clan Skill. */
	record PledgeSkillListAdd(int skillId, int skillLevel) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x3a)
					.writeD(skillId)
					.writeD(skillLevel)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x3F PledgeReceiveSubPledgeCreated: confirma criacao de Sub-Unidade
	 * (Academy/Royal Guard/Order of Knights).
	 */
	record PledgeReceiveSubPledgeCreated(int subPledgeId, String name, String leaderName) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x3f)
					.writeD(0x01)
					.writeD(subPledgeId)
					.writeS(name != null ? name : "")
					.writeS(leaderName != null ? leaderName : "")
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x40 ExRedSky: efeito visual de ceu vermelho por determinada duracao.
	 */
	record ExRedSky(int durationSeconds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x40).writeD(durationSeconds).toByteArray();
		}
	}

	/**
	 * 0xFE:0x43 ShowPCCafeCouponShowUI: abre interface de insercao de cupom PC
	 * Cafe.
	 */
	record ShowPCCafeCouponShowUI() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x43).toByteArray();
		}
	}

	/** 0xFE:0x44 ExOrcMove: pacote especial de animacao tutorial Orc. */
	record ExOrcMove() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x44).toByteArray();
		}
	}

	/** 0xFE:0x47 ExRestartClient: solicita reinicializacao do cliente. */
	record ExRestartClient() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x47).toByteArray();
		}
	}

	/** 0xFE:0x48 ExRequestHackShield: solicita verificacao do HackShield. */
	record ExRequestHackShield() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x48).toByteArray();
		}
	}

	/**
	 * 0xFE:0x49 ExUseSharedGroupItem: exibe cooldown compartilhado de item (ex:
	 * pocoes/elixires).
	 */
	record ExUseSharedGroupItem(int itemId, int groupId, int remainingTime, int totalTime) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x49)
					.writeD(itemId)
					.writeD(groupId)
					.writeD(remainingTime)
					.writeD(totalTime)
					.toByteArray();
		}
	}

	/** Membro de uma party dentro do Command Channel. */
	record MpccPartyMemberEntry(String name, int objectId, int classId) {
	}

	/**
	 * 0xFE:0x4A ExMPCCShowPartyMemberInfo: lista membros de uma party especifica do
	 * Command Channel.
	 */
	record ExMPCCShowPartyMemberInfo(List<MpccPartyMemberEntry> members) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = members != null ? members.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x4a).writeD(size);
			if (members != null) {
				for (MpccPartyMemberEntry m : members) {
					w.writeS(m.name() != null ? m.name() : "")
							.writeD(m.objectId())
							.writeD(m.classId());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x4F ExDuelUpdateUserInfo: atualiza HP/MP/CP do oponente durante um
	 * duelo.
	 */
	record ExDuelUpdateUserInfo(String name, int objectId, int classId, int level, int currentHp, int maxHp,
			int currentMp, int maxMp, int currentCp, int maxCp) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x4f)
					.writeS(name != null ? name : "")
					.writeD(objectId)
					.writeD(classId)
					.writeD(level)
					.writeD(currentHp)
					.writeD(maxHp)
					.writeD(currentMp)
					.writeD(maxMp)
					.writeD(currentCp)
					.writeD(maxCp)
					.toByteArray();
		}
	}

	/** 0xFE:0x5B ExShowSlideshowKamael: exibe slideshow promocional no cliente. */
	record ExShowSlideshowKamael() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x5b).toByteArray();
		}
	}

	/** 0xFE:0x5C ExPlayScene: reproduz cena cinematica no cliente. */
	record ExPlayScene() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x5c).toByteArray();
		}
	}

	/**
	 * 0xFE:0x5D ExSpawnEmitter: spawna emissor de particulas entre NPC e jogador.
	 */
	record ExSpawnEmitter(int npcObjectId, int playerObjectId) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x5d)
					.writeD(npcObjectId)
					.writeD(playerObjectId)
					.writeD(0x00)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x5E ExEnchantSkillInfoDetail: detalhes de custo de encantamento de
	 * skill.
	 */
	record ExEnchantSkillInfoDetail(int itemId, int itemCount) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x5e)
					.writeD(0).writeD(0).writeD(0).writeD(0).writeD(0).writeD(0)
					.writeD(itemCount)
					.writeD(0)
					.writeD(itemId)
					.writeD(0)
					.toByteArray();
		}
	}

	/** Ponto de rastro 3D. */
	record TracePoint(int x, int y, int z, int time) {
	}

	/** 0xFE:0x67 ExShowTrace: desenha rastro de pontos no espaco 3D. */
	record ExShowTrace(List<TracePoint> traces) implements GameServerPacket {
		@Override
		public byte[] encode() {
			int size = traces != null ? traces.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x67).writeH(size);
			if (traces != null) {
				for (TracePoint t : traces) {
					w.writeD(t.x()).writeD(t.y()).writeD(t.z()).writeH(t.time());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x7A ExOlympiadUserInfoSpectator: informacoes de competidor das
	 * Olimpiadas para espectadores.
	 */
	record ExOlympiadUserInfoSpectator(int side, int objectId, String name, int classId, int currentHp, int maxHp,
			int currentCp, int maxCp) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x7a)
					.writeC(side)
					.writeD(objectId)
					.writeS(name != null ? name : "")
					.writeD(classId)
					.writeD(currentHp)
					.writeD(maxHp)
					.writeD(currentCp)
					.writeD(maxCp)
					.toByteArray();
		}
	}

	/** 0xFE:0x7D ExShowFortressMapInfo: informacoes de mapa de Fortaleza. */
	record ExShowFortressMapInfo(int fortId, boolean siegeInProgress, int fortSize) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x7d)
					.writeD(fortId)
					.writeD(siegeInProgress ? 1 : 0)
					.writeD(fortSize)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x80 ExPrivateStoreSetWholeMsg: placa de venda de pacote completo
	 * (Package Sale).
	 */
	record ExPrivateStoreSetWholeMsg(int objectId, String msg) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x80)
					.writeD(objectId)
					.writeS(msg != null ? msg : "")
					.toByteArray();
		}
	}

	/** 0xFE:0x81 RequestEnchant: confirmacao estendida de encantamento. */
	record RequestEnchant(int result) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x81).writeD(result).toByteArray();
		}
	}

	/** 0xFE:0x8A ExShowCubTimer: exibe timer de cubo no cliente. */
	record ExShowCubTimer(int seconds) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x8a).writeD(seconds).toByteArray();
		}
	}

	/** 0xFE:0x8B ExCubTimerStop: interrompe timer de cubo no cliente. */
	record ExCubTimerStop() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x8b).toByteArray();
		}
	}

	/** 0xFB L2Friend: adiciona ou remove um amigo individualmente na interface. */
	record L2Friend(boolean add, int friendObjId, String friendName, boolean isOnline) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfb)
					.writeD(add ? 1 : 3)
					.writeD(friendObjId)
					.writeS(friendName != null ? friendName : "")
					.writeD(isOnline ? 1 : 0)
					.writeD(isOnline ? friendObjId : 0)
					.toByteArray();
		}
	}

	/**
	 * 0xFD FriendRecvMsg: mensagem privada na janela de amigos (alias de
	 * L2FriendSay).
	 */
	record FriendRecvMsg(String sender, String receiver, String message) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfd)
					.writeD(0)
					.writeS(receiver != null ? receiver : "")
					.writeS(sender != null ? sender : "")
					.writeS(message != null ? message : "")
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x01 ExRegenMax: taxa e duracao de regeneracao de pocao (alias de
	 * ExRegMax).
	 */
	record ExRegenMax(int count, int time, double max) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x01)
					.writeD(1)
					.writeD(count)
					.writeD(time)
					.writeF(max)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x44 ExCaptureOrc: pacote especial de captura Orc (alias de ExOrcMove).
	 */
	record ExCaptureOrc() implements GameServerPacket {
		private static final byte[] STATIC_DATA = new byte[] {
				(byte) 0xE4, (byte) 0xAB, (byte) 0x8E, (byte) 0xC5, (byte) 0xE9, (byte) 0xF9, (byte) 0x86, (byte) 0x7B,
				(byte) 0x9E, (byte) 0x5D, (byte) 0x83, (byte) 0x14, (byte) 0x05, (byte) 0xD4, (byte) 0x48, (byte) 0x01,
				(byte) 0xCD, (byte) 0xA2, (byte) 0x8D, (byte) 0x90, (byte) 0x62, (byte) 0x8C, (byte) 0xDA, (byte) 0x32,
				(byte) 0x7B, (byte) 0x1B, (byte) 0x87, (byte) 0x6D, (byte) 0x08, (byte) 0xC4, (byte) 0xE1, (byte) 0x56,
				(byte) 0x9B, (byte) 0x3B, (byte) 0xC3, (byte) 0x40, (byte) 0xDF, (byte) 0xE8, (byte) 0xD7, (byte) 0xE1,
				(byte) 0x98, (byte) 0x38, (byte) 0x1C, (byte) 0xA5, (byte) 0x8E, (byte) 0x45, (byte) 0x3F, (byte) 0xF2,
				(byte) 0x5E, (byte) 0x1C, (byte) 0x59, (byte) 0x8E, (byte) 0x74, (byte) 0x01, (byte) 0x9E, (byte) 0xC2,
				(byte) 0x00, (byte) 0x95, (byte) 0xB0, (byte) 0x1D, (byte) 0x87, (byte) 0xED, (byte) 0x9C, (byte) 0x8A
		};

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x44).writeB(STATIC_DATA).toByteArray();
		}
	}

	/** Entrada de castelo para ExShowCastleInfo (0xFE:0x14). */
	record CastleInfoEntry(int castleId, String ownerClanName, int taxPercent, int nextSiegeTimeSeconds) {
	}

	/**
	 * 0xFE:0x14 ExShowCastleInfo: lista os castelos, donos, taxas e horarios de
	 * cerco no mapa mundi.
	 */
	record ExShowCastleInfo(List<CastleInfoEntry> castles) implements GameServerPacket {
		public ExShowCastleInfo() {
			this(List.of());
		}

		@Override
		public byte[] encode() {
			int size = castles != null ? castles.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x14).writeD(size);
			if (castles != null) {
				for (CastleInfoEntry c : castles) {
					w.writeD(c.castleId())
							.writeS(c.ownerClanName() != null ? c.ownerClanName() : "")
							.writeD(c.taxPercent())
							.writeD(c.nextSiegeTimeSeconds());
				}
			}
			return w.toByteArray();
		}
	}

	/** Entrada de fortaleza para ExShowFortressInfo (0xFE:0x15). */
	record FortressInfoEntry(int fortId, String ownerClanName, boolean inSiege, int possessionTimeSeconds) {
	}

	/**
	 * 0xFE:0x15 ExShowFortressInfo: lista as fortalezas, donos e status de cerco no
	 * mapa mundi.
	 */
	record ExShowFortressInfo(List<FortressInfoEntry> fortresses) implements GameServerPacket {
		public ExShowFortressInfo() {
			this(List.of());
		}

		@Override
		public byte[] encode() {
			int size = fortresses != null ? fortresses.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x15).writeD(size);
			if (fortresses != null) {
				for (FortressInfoEntry f : fortresses) {
					w.writeD(f.fortId())
							.writeS(f.ownerClanName() != null ? f.ownerClanName() : "")
							.writeD(f.inSiege() ? 1 : 0)
							.writeD(f.possessionTimeSeconds());
				}
			}
			return w.toByteArray();
		}
	}

	/** Entrada de Clan Hall (Agit) para ExShowAgitInfo (0xFE:0x16). */
	record AgitInfoEntry(int hallId, String clanName, String leaderName, int grade) {
	}

	/**
	 * 0xFE:0x16 ExShowAgitInfo: lista os Clan Halls (Agits), clas proprietarios e
	 * lideres no mapa mundi.
	 */
	record ExShowAgitInfo(List<AgitInfoEntry> agits) implements GameServerPacket {
		public ExShowAgitInfo() {
			this(List.of());
		}

		@Override
		public byte[] encode() {
			int size = agits != null ? agits.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x16).writeD(size);
			if (agits != null) {
				for (AgitInfoEntry a : agits) {
					w.writeD(a.hallId())
							.writeS(a.clanName() != null ? a.clanName() : "")
							.writeS(a.leaderName() != null ? a.leaderName() : "")
							.writeD(a.grade());
				}
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0xFE:0x17 ExShowFortressSiegeInfo: exibe informacoes de quartel/comandantes
	 * durante cerco de fortaleza.
	 */
	record ExShowFortressSiegeInfo(int fortId, int size, int commandersRemaining) implements GameServerPacket {
		public ExShowFortressSiegeInfo(int fortId) {
			this(fortId, 3, 0);
		}

		@Override
		public byte[] encode() {
			int count = Math.max(0, size);
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x17)
					.writeD(fortId)
					.writeD(count)
					.writeD(commandersRemaining);
			for (int i = 0; i < count; i++) {
				w.writeD(i < commandersRemaining ? 0 : 1);
			}
			return w.toByteArray();
		}
	}

	/**
	 * 0x08 AttackOutOfRange: notifica que o ataque falhou por alvo fora de alcance.
	 */
	record AttackOutOfRange() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x08).toByteArray();
		}
	}

	/**
	 * 0x09 AttackinCoolTime: notifica que o ataque ainda esta em tempo de recarga.
	 */
	record AttackinCoolTime() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x09).toByteArray();
		}
	}

	/** 0x0A AttackDeadTarget: notifica tentativa de atacar um alvo ja morto. */
	record AttackDeadTarget() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x0a).toByteArray();
		}
	}

	/**
	 * 0x2E SendStatus: pacote de status online do servidor (L2Remote/RCON status).
	 */
	record SendStatus(int onlinePlayers, int maxOnline, int onlinePrivStore) implements GameServerPacket {
		public SendStatus(int onlinePlayers) {
			this(onlinePlayers, Math.max(onlinePlayers, 500), 0);
		}

		public SendStatus() {
			this(1, 500, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x2e)
					.writeD(0x01)
					.writeD(maxOnline)
					.writeD(onlinePlayers + 2)
					.writeD(onlinePlayers)
					.writeD(onlinePrivStore)
					.writeH(0x30).writeH(0x2c)
					.writeH(0x35).writeH(0x31)
					.writeH(0x30).writeH(0x2c)
					.writeH(0x37).writeH(0x37).writeH(0x37).writeH(0x35)
					.writeD(0).writeD(0).writeD(0)
					.writeC(0x00)
					.toByteArray();
		}
	}

	/**
	 * 0x43 WareHouseDone: confirma conclusao de operacao de armazem (Warehouse).
	 */
	record WareHouseDone(int status) implements GameServerPacket {
		public WareHouseDone() {
			this(0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x43).writeD(status).toByteArray();
		}
	}

	/** 0x6E HideBoard: fecha a janela do Community Board (Alt+B). */
	record HideBoard() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0x6e).toByteArray();
		}
	}

	/** 0x9A PrivateStoreManageListBuff: gerenciamento de Buff Shop privada. */
	record PrivateStoreManageListBuff(int playerObjId, int adena, List<ManufactureRecipeEntry> buffs)
			implements GameServerPacket {
		public PrivateStoreManageListBuff(int playerObjId, int adena) {
			this(playerObjId, adena, List.of());
		}

		public PrivateStoreManageListBuff() {
			this(0, 0, List.of());
		}

		@Override
		public byte[] encode() {
			int size = buffs != null ? buffs.size() : 0;
			PacketWriter w = new PacketWriter().writeC(0x9a)
					.writeD(playerObjId)
					.writeD(0)
					.writeD(adena)
					.writeD(size);
			if (buffs != null) {
				for (ManufactureRecipeEntry b : buffs) {
					w.writeD(b.recipeId()).writeD(0).writeD(b.cost());
				}
			}
			w.writeD(size);
			if (buffs != null) {
				for (ManufactureRecipeEntry b : buffs) {
					w.writeD(b.recipeId()).writeD(0).writeD(b.cost());
				}
			}
			return w.toByteArray();
		}
	}

	/** 0xAD DismissAlliance: notificacao de dissolucao de alianca. */
	record DismissAlliance() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xad).toByteArray();
		}
	}

	/**
	 * 0xCF EventTrigger: aciona um gatilho visual de ambiente/mapa (emitterId,
	 * enabled).
	 */
	record EventTrigger(int trapId, boolean active) implements GameServerPacket {
		public EventTrigger(int trapId, int active) {
			this(trapId, active != 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xcf)
					.writeD(trapId)
					.writeC(active ? 1 : 0)
					.toByteArray();
		}
	}

	/**
	 * 0xD3 RequestNetPing: solicita resposta de latencia (NetPing 0xA8) do cliente.
	 */
	record RequestNetPing(int kId) implements GameServerPacket {
		public RequestNetPing() {
			this((int) (System.currentTimeMillis() & 0x7FFFFFFF));
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xd3).writeD(kId).toByteArray();
		}
	}

	/** 0xAA ShowRadar: exibe marcador no radar do cliente. */
	record ShowRadar(int x, int y, int z) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xaa)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/** 0xEB DeleteRadar: remove marcadores do radar do cliente. */
	record DeleteRadar(int type, int x, int y, int z) implements GameServerPacket {
		public DeleteRadar() {
			this(1, 0, 0, 0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xeb)
					.writeD(type)
					.writeD(x)
					.writeD(y)
					.writeD(z)
					.toByteArray();
		}
	}

	/**
	 * 0xF6 PetitionVotePacket: solicita avaliacao ao jogador apos encerramento de
	 * peticao GM.
	 */
	record PetitionVotePacket() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xf6).toByteArray();
		}
	}

	/**
	 * 0xFE:0x42 PledgeReceiveUpdatePower: atualiza mascara de privilegios do membro
	 * do cla.
	 */
	record PledgeReceiveUpdatePower(int powerPrivilege) implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x42)
					.writeD(powerPrivilege)
					.toByteArray();
		}
	}

	/** 0xFE:0x45 ExSearchOrc: pacote auxiliar anti-bot / verificacao de cliente. */
	record ExSearchOrc() implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x45).toByteArray();
		}
	}

	/** 0xFE:0x58 ExDuelEnemyRelation: define relacao visual de inimigo em duelo. */
	record ExDuelEnemyRelation(int nonRemoved) implements GameServerPacket {
		public ExDuelEnemyRelation(boolean removed) {
			this(removed ? 1 : 0);
		}

		public ExDuelEnemyRelation() {
			this(0);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x58)
					.writeD(nonRemoved)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x59 ExPlayAnimation: reproduz animacao especifica em personagem/NPC.
	 */
	record ExPlayAnimation(int objectId, int actionType, int animationId, String animationName)
			implements GameServerPacket {
		public ExPlayAnimation(int objectId, int animationId) {
			this(objectId, 1, animationId, "");
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x59)
					.writeD(objectId)
					.writeC(actionType)
					.writeD(animationId)
					.writeS(animationName != null ? animationName : "")
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x5A ExMPCCPartyInfoUpdate: adiciona, atualiza ou remove uma party na
	 * janela do Command Channel.
	 */
	record ExMPCCPartyInfoUpdate(String leaderName, int leaderObjId, int memberCount, int mode)
			implements GameServerPacket {
		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x5a)
					.writeS(leaderName != null ? leaderName : "")
					.writeD(leaderObjId)
					.writeD(memberCount)
					.writeD(mode)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x5F ExBasicActionList: envia a lista de IDs de acoes basicas
	 * habilitadas na interface (Alt+C).
	 */
	record ExBasicActionList(int[] actionIds) implements GameServerPacket {
		public static final int[] DEFAULT_ACTIONS;
		static {
			DEFAULT_ACTIONS = new int[72];
			for (int i = 0; i < DEFAULT_ACTIONS.length; i++) {
				DEFAULT_ACTIONS[i] = i;
			}
		}

		public ExBasicActionList() {
			this(DEFAULT_ACTIONS);
		}

		@Override
		public byte[] encode() {
			int[] ids = actionIds != null ? actionIds : DEFAULT_ACTIONS;
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x5f).writeD(ids.length);
			for (int id : ids) {
				w.writeD(id);
			}
			return w.toByteArray();
		}
	}

	/** 0xFE:0x6C ExRpItemLink: exibe tooltip de item linkado no chat. */
	record ExRpItemLink(int objectId, int itemId, int count, int type2, int bodyPart, int enchantLevel, int customType2,
			int augmentationId, int mana) implements GameServerPacket {
		public ExRpItemLink(com.lopez.l2j.game.item.ItemInstance item) {
			this(
					item != null ? item.objectId() : 0,
					item != null ? item.itemId() : 0,
					item != null ? item.count() : 1,
					item != null && item.template() != null ? item.template().type2() : 0,
					item != null && item.template() != null ? item.template().bodyPart() : 0,
					item != null ? item.enchantLevel() : 0,
					item != null ? item.customType2() : 0,
					item != null ? item.augmentationId() : 0,
					item != null ? item.mana() : -1);
		}

		@Override
		public byte[] encode() {
			return new PacketWriter().writeC(0xfe).writeH(0x6c)
					.writeD(objectId)
					.writeD(itemId)
					.writeD(count)
					.writeH(type2)
					.writeD(bodyPart)
					.writeH(enchantLevel)
					.writeH(customType2)
					.writeH(0x00)
					.writeD(augmentationId)
					.writeD(mana)
					.toByteArray();
		}
	}

	/**
	 * 0xFE:0x70 ExUISetting: envia as configuracoes de interface e mapeamento de
	 * teclas (KeyMapping).
	 */
	record ExUISetting(byte[] uiData) implements GameServerPacket {
		public ExUISetting() {
			this(new byte[16]);
		}

		@Override
		public byte[] encode() {
			byte[] data = uiData != null ? uiData : new byte[16];
			PacketWriter w = new PacketWriter().writeC(0xfe).writeH(0x70)
					.writeD(data.length + 16)
					.writeD(0x00)
					.writeD(0x00)
					.writeD(0x00)
					.writeD(0x00);
			if (data.length > 0) {
				w.writeB(data);
			}
			return w.toByteArray();
		}
	}
}
