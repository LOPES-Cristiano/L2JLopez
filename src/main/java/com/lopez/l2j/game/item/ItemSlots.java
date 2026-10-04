package com.lopez.l2j.game.item;

import java.util.Map;

/**
 * Mascaras de body part (L2Item.SLOT_*) e indices do paperdoll (Inventory.PAPERDOLL_*) do legado. Os valores
 * sao protocolo: o cliente Interlude os recebe no ItemList/InventoryUpdate e os envia no RequestUnEquipItem.
 */
public final class ItemSlots {

	private ItemSlots() {
	}

	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = SLOT_R_EAR | SLOT_L_EAR;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = SLOT_R_FINGER | SLOT_L_FINGER;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_FACE = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;

	public static final int UNDER = 0;
	public static final int REAR = 1;
	public static final int LEAR = 2;
	public static final int NECK = 4;
	public static final int LFINGER = 5;
	public static final int RFINGER = 6;
	public static final int HEAD = 8;
	public static final int RHAND = 9;
	public static final int LHAND = 10;
	public static final int GLOVES = 11;
	public static final int CHEST = 12;
	public static final int LEGS = 13;
	public static final int FEET = 14;
	public static final int BACK = 15;
	public static final int LRHAND = 16;
	public static final int FULLARMOR = 17;
	public static final int HAIR = 18;
	public static final int ALLDRESS = 19;
	public static final int FACE = 20;
	public static final int HAIRALL = 21;
	public static final int PAPERDOLL_SLOTS = 22;

	/**
	 * Ordem dos 17 slots visiveis nos pacotes UserInfo/CharSelectionInfo/CharInfo (o 15o e a mao das armas de
	 * duas maos).
	 */
	public static final int[] VISIBLE_ORDER = { HAIRALL, REAR, LEAR, NECK, RFINGER, LFINGER, HEAD, RHAND, LHAND,
			GLOVES, CHEST, LEGS, FEET, BACK, LRHAND, HAIR, FACE };

	private static final Map<String, Integer> BY_NAME = Map.ofEntries(Map.entry("shirt", SLOT_UNDERWEAR),
			Map.entry("underwear", SLOT_UNDERWEAR), Map.entry("chest", SLOT_CHEST),
			Map.entry("fullarmor", SLOT_FULL_ARMOR), Map.entry("head", SLOT_HEAD), Map.entry("hair", SLOT_HAIR),
			Map.entry("face", SLOT_FACE), Map.entry("dhair", SLOT_HAIRALL), Map.entry("back", SLOT_BACK),
			Map.entry("neck", SLOT_NECK), Map.entry("legs", SLOT_LEGS), Map.entry("feet", SLOT_FEET),
			Map.entry("gloves", SLOT_GLOVES), Map.entry("chest,legs", SLOT_CHEST | SLOT_LEGS),
			Map.entry("rhand", SLOT_R_HAND), Map.entry("lhand", SLOT_L_HAND), Map.entry("lrhand", SLOT_LR_HAND),
			Map.entry("rear,lear", SLOT_LR_EAR), Map.entry("rfinger,lfinger", SLOT_LR_FINGER),
			Map.entry("wolf", SLOT_WOLF), Map.entry("hatchling", SLOT_HATCHLING), Map.entry("strider", SLOT_STRIDER),
			Map.entry("babypet", SLOT_BABYPET), Map.entry("none", SLOT_NONE));

	/** Converte a coluna {@code bodypart} das tabelas armor/weapon; desconhecido vira SLOT_NONE. */
	public static int parseBodyPart(String name) {
		if (name == null) {
			return SLOT_NONE;
		}
		return BY_NAME.getOrDefault(name.trim().toLowerCase(java.util.Locale.ROOT), SLOT_NONE);
	}

	/** Slot do paperdoll ocupado por uma mascara (porta de Inventory.getPaperdollIndex); -1 se nenhum. */
	public static int paperdollIndex(int bodyPart) {
		return switch (bodyPart) {
			case SLOT_UNDERWEAR -> UNDER;
			case SLOT_R_EAR -> REAR;
			case SLOT_L_EAR, SLOT_LR_EAR -> LEAR;
			case SLOT_NECK -> NECK;
			case SLOT_R_FINGER -> RFINGER;
			case SLOT_L_FINGER, SLOT_LR_FINGER -> LFINGER;
			case SLOT_HEAD -> HEAD;
			case SLOT_R_HAND, SLOT_LR_HAND -> RHAND;
			case SLOT_L_HAND -> LHAND;
			case SLOT_GLOVES -> GLOVES;
			case SLOT_CHEST, SLOT_FULL_ARMOR, SLOT_ALLDRESS -> CHEST;
			case SLOT_LEGS -> LEGS;
			case SLOT_FEET -> FEET;
			case SLOT_BACK -> BACK;
			case SLOT_FACE -> FACE;
			case SLOT_HAIRALL -> HAIRALL;
			case SLOT_HAIR -> HAIR;
			default -> -1;
		};
	}
}
