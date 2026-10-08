package com.lopez.l2j.game.item;

import static com.lopez.l2j.game.item.ItemSlots.SLOT_FACE;
import static com.lopez.l2j.game.item.ItemSlots.SLOT_HAIR;
import static com.lopez.l2j.game.item.ItemSlots.SLOT_L_EAR;
import static com.lopez.l2j.game.item.ItemSlots.SLOT_L_FINGER;
import static com.lopez.l2j.game.item.ItemSlots.SLOT_L_HAND;
import static com.lopez.l2j.game.item.ItemSlots.SLOT_NECK;

import java.util.List;
import java.util.Locale;

/**
 * Template imutavel de item, unificando L2EtcItem/L2Armor/L2Weapon do legado. As regras de type1/type2/bodyPart
 * sao as de ItemTable.readArmor/readItem/readWeapon (esses valores vao direto para o cliente).
 *
 * @param subType weaponType (sword, bow, none=escudo...), armor_type (light, heavy, magic, pet...) ou item_type
 *                (potion, arrow, quest...)
 */
public record ItemTemplate(
		int id,
		int displayId,
		String name,
		Kind kind,
		String subType,
		int type1,
		int type2,
		int bodyPart,
		int weight,
		boolean stackable,
		String crystalType,
		int price,
		int pAtk,
		int mAtk,
		int pDef,
		int mDef,
		int atkSpeed,
		int critical,
		int hitModify,
		int avoidModify,
		int shieldDef,
		int mpBonus,
		int soulshots,
		int spiritshots,
		int rndDam,
		List<ItemSkillHolder> itemSkills,
		ItemSkillHolder enchant4Skill,
		ItemSkillHolder onCritSkill,
		ItemSkillHolder onCastSkill,
		boolean sellable,
		boolean dropable,
		boolean destroyable,
		boolean tradeable
) {

	public enum Kind {
		WEAPON, ARMOR, ETC
	}

	public String itemType() {
		return subType;
	}

	public static final int ADENA_ID = 57;

	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	public static final int TYPE2_PET_WOLF = 6;
	public static final int TYPE2_PET_HATCHLING = 7;
	public static final int TYPE2_PET_STRIDER = 8;
	public static final int TYPE2_PET_BABY = 9;

	public ItemTemplate(int id, int displayId, String name, Kind kind, String subType, int type1, int type2,
			int bodyPart, int weight, boolean stackable, String crystalType, int price, int pAtk, int mAtk, int pDef,
			int mDef, int atkSpeed, int critical, int hitModify, int avoidModify, int shieldDef, boolean sellable,
			boolean dropable, boolean destroyable, boolean tradeable) {
		this(id, displayId, name, kind, subType, type1, type2, bodyPart, weight, stackable, crystalType, price, pAtk,
				mAtk, pDef, mDef, atkSpeed, critical, hitModify, avoidModify, shieldDef, 0, 1, 1, 0, List.of(), null,
				null, null, sellable, dropable, destroyable, tradeable);
	}

	public ItemTemplate(int id, int displayId, String name, Kind kind, String subType, int type1, int type2,
			int bodyPart, int weight, boolean stackable, String crystalType, int price, int pAtk, int mAtk, int pDef,
			int mDef, int atkSpeed, int critical, int shieldDef, boolean sellable, boolean dropable, boolean destroyable,
			boolean tradeable) {
		this(id, displayId, name, kind, subType, type1, type2, bodyPart, weight, stackable, crystalType, price, pAtk,
				mAtk, pDef, mDef, atkSpeed, critical, 0, 0, shieldDef, 0, 1, 1, 0, List.of(), null, null, null, sellable,
				dropable, destroyable, tradeable);
	}

	public ItemTemplate {
		name = name == null ? "" : name;
		subType = subType == null ? "none" : subType.toLowerCase(Locale.ROOT);
		crystalType = crystalType == null ? "none" : crystalType.toLowerCase(Locale.ROOT);
		itemSkills = itemSkills == null ? List.of() : List.copyOf(itemSkills);
	}

	/** Item que vai para o paperdoll de um jogador (itens de pet e etc comuns ficam de fora). */
	public boolean isEquipable() {
		return bodyPart > 0 && type2 < TYPE2_PET_WOLF;
	}

	public boolean isPetItem() {
		return type2 >= TYPE2_PET_WOLF;
	}

	public boolean isQuestItem() {
		return type2 == TYPE2_QUEST;
	}

	public boolean isTradeable() {
		return tradeable && !isQuestItem();
	}

	public boolean isArrow() {
		return kind == Kind.ETC && "arrow".equals(subType);
	}

	public boolean isStackable() {
		return stackable;
	}

	public int crystalGrade() {
		if (crystalType == null) {
			return 0;
		}
		return switch (crystalType.toLowerCase(Locale.ROOT)) {
			case "d" -> 1;
			case "c" -> 2;
			case "b" -> 3;
			case "a" -> 4;
			case "s" -> 5;
			default -> 0;
		};
	}

	public static ItemTemplate weapon(int id, int displayId, String name, String bodyPartName, String weaponType,
			int weight, String crystal, int pAtk, int mAtk, int atkSpeed, int critical, int hitModify, int avoidModify,
			int shieldDef, int soulshots, int spiritshots, int rndDam, int price, List<ItemSkillHolder> itemSkills,
			ItemSkillHolder enchant4Skill, ItemSkillHolder onCritSkill, ItemSkillHolder onCastSkill,
			boolean sellable, boolean dropable, boolean destroyable, boolean tradeable) {
		String type = weaponType == null ? "none" : weaponType.toLowerCase(Locale.ROOT);
		int bodyPart = ItemSlots.parseBodyPart(bodyPartName);
		int type1;
		int type2;
		if (type.equals("pet")) {
			type1 = TYPE1_WEAPON_RING_EARRING_NECKLACE;
			type2 = petType2(bodyPart);
			bodyPart = ItemSlots.SLOT_R_HAND;
		} else if (type.equals("none")) { // escudos
			type1 = TYPE1_SHIELD_ARMOR;
			type2 = TYPE2_SHIELD_ARMOR;
		} else {
			type1 = TYPE1_WEAPON_RING_EARRING_NECKLACE;
			type2 = TYPE2_WEAPON;
		}
		return new ItemTemplate(id, displayId, name, Kind.WEAPON, type, type1, type2, bodyPart, weight, false,
				crystal, price, pAtk, mAtk, 0, 0, atkSpeed, critical, hitModify, avoidModify, shieldDef, 0, soulshots,
				spiritshots, rndDam, itemSkills, enchant4Skill, onCritSkill, onCastSkill, sellable, dropable,
				destroyable, tradeable);
	}

	public static ItemTemplate weapon(int id, int displayId, String name, String bodyPartName, String weaponType,
			int weight, String crystal, int pAtk, int mAtk, int atkSpeed, int critical, int hitModify, int avoidModify,
			int shieldDef, int price, boolean sellable, boolean dropable, boolean destroyable, boolean tradeable) {
		return weapon(id, displayId, name, bodyPartName, weaponType, weight, crystal, pAtk, mAtk, atkSpeed, critical,
				hitModify, avoidModify, shieldDef, 1, 1, 0, price, List.of(), null, null, null, sellable, dropable,
				destroyable, tradeable);
	}

	public static ItemTemplate weapon(int id, int displayId, String name, String bodyPartName, String weaponType,
			int weight, String crystal, int pAtk, int mAtk, int atkSpeed, int critical, int shieldDef, int price,
			boolean sellable, boolean dropable, boolean destroyable, boolean tradeable) {
		return weapon(id, displayId, name, bodyPartName, weaponType, weight, crystal, pAtk, mAtk, atkSpeed, critical,
				0, 0, shieldDef, 1, 1, 0, price, List.of(), null, null, null, sellable, dropable, destroyable, tradeable);
	}

	public static ItemTemplate armor(int id, int displayId, String name, String bodyPartName, String armorType,
			int weight, String crystal, int pDef, int mDef, int avoidModify, int mpBonus, int price,
			List<ItemSkillHolder> itemSkills, boolean sellable, boolean dropable, boolean destroyable, boolean tradeable) {
		String type = armorType == null ? "none" : armorType.toLowerCase(Locale.ROOT);
		int bodyPart = ItemSlots.parseBodyPart(bodyPartName);
		int type1;
		int type2;
		if (bodyPart == SLOT_NECK || bodyPart == SLOT_HAIR || bodyPart == SLOT_FACE || (bodyPart & SLOT_L_EAR) != 0
				&& bodyPart > 0 || (bodyPart & SLOT_L_FINGER) != 0 && bodyPart > 0) {
			type1 = TYPE1_WEAPON_RING_EARRING_NECKLACE;
			type2 = TYPE2_ACCESSORY;
		} else {
			type1 = TYPE1_SHIELD_ARMOR;
			type2 = TYPE2_SHIELD_ARMOR;
		}
		if (type.equals("pet") && bodyPart != SLOT_NECK) {
			type1 = TYPE1_SHIELD_ARMOR;
			type2 = petType2(bodyPart);
			bodyPart = ItemSlots.SLOT_CHEST;
		}
		return new ItemTemplate(id, displayId, name, Kind.ARMOR, type, type1, type2, bodyPart, weight, false, crystal,
				price, 0, 0, pDef, mDef, 0, 0, 0, avoidModify, 0, mpBonus, 0, 0, 0, itemSkills, null, null, null,
				sellable, dropable, destroyable, tradeable);
	}

	public static ItemTemplate armor(int id, int displayId, String name, String bodyPartName, String armorType,
			int weight, String crystal, int pDef, int mDef, int avoidModify, int price, boolean sellable, boolean dropable,
			boolean destroyable, boolean tradeable) {
		return armor(id, displayId, name, bodyPartName, armorType, weight, crystal, pDef, mDef, avoidModify, 0, price,
				List.of(), sellable, dropable, destroyable, tradeable);
	}

	public static ItemTemplate armor(int id, int displayId, String name, String bodyPartName, String armorType,
			int weight, String crystal, int pDef, int mDef, int price, boolean sellable, boolean dropable,
			boolean destroyable, boolean tradeable) {
		return armor(id, displayId, name, bodyPartName, armorType, weight, crystal, pDef, mDef, 0, 0, price,
				List.of(), sellable, dropable, destroyable, tradeable);
	}

	public static ItemTemplate etc(int id, int displayId, String name, String itemType, String consumeType,
			int weight, String crystal, int price, List<ItemSkillHolder> itemSkills, boolean sellable, boolean dropable,
			boolean destroyable, boolean tradeable) {
		String type = itemType == null ? "none" : itemType.toLowerCase(Locale.ROOT);
		String consume = consumeType == null ? "normal" : consumeType.toLowerCase(Locale.ROOT);
		int type2 = type.equals("quest") ? TYPE2_QUEST : TYPE2_OTHER;
		int bodyPart = type.equals("arrow") || type.equals("lure") ? SLOT_L_HAND : 0;
		boolean stackable = consume.equals("stackable") || consume.equals("asset");
		if (consume.equals("asset")) {
			type2 = TYPE2_MONEY;
		}
		return new ItemTemplate(id, displayId, name, Kind.ETC, type, TYPE1_ITEM_QUESTITEM_ADENA, type2, bodyPart,
				weight, stackable, crystal, price, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemSkills, null, null, null,
				sellable, dropable, destroyable, tradeable);
	}

	public static ItemTemplate etc(int id, int displayId, String name, String itemType, String consumeType,
			int weight, String crystal, int price, boolean sellable, boolean dropable, boolean destroyable,
			boolean tradeable) {
		return etc(id, displayId, name, itemType, consumeType, weight, crystal, price, List.of(), sellable, dropable,
				destroyable, tradeable);
	}

	private static int petType2(int bodyPart) {
		return switch (bodyPart) {
			case ItemSlots.SLOT_WOLF -> TYPE2_PET_WOLF;
			case ItemSlots.SLOT_HATCHLING -> TYPE2_PET_HATCHLING;
			case ItemSlots.SLOT_BABYPET -> TYPE2_PET_BABY;
			default -> TYPE2_PET_STRIDER;
		};
	}
}
