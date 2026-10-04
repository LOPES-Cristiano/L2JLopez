package com.lopez.l2j.game.effect;

import java.util.Map;
import java.util.Optional;

/**
 * Consumiveis suportados (porta dos item handlers Potions/SoulShots do L2JDream, com os valores dos skills
 * 2001-2169 de data/xml/stats/skills). Enquanto nao existe motor de skills, cada skill vira uma definicao fixa.
 */
public final class ConsumableTable {

	public enum Type {
		HEAL_HP, HEAL_MP, HEAL_CP, HOT_HP, HOT_MP, BUFF, SOULSHOT, SPIRITSHOT, BLESSED_SPIRITSHOT,
		/** Muda o rosto (amount = novo valor). */
		FACE,
		/** Muda a cor do cabelo (amount = novo valor). */
		HAIR_COLOR,
		/** Muda o estilo do cabelo (amount = novo valor). */
		HAIR_STYLE,
		/** Mystery Potion: cabecao (AbnormalEffect BIG_HEAD) por ticks*intervalMs. */
		MYSTERY,
		/** Antidotos/bandagens: removem veneno/sangramento. */
		REMEDY
	}

	/** Mascara AbnormalEffect.BIG_HEAD do cliente Interlude. */
	public static final int ABNORMAL_BIG_HEAD = 0x2000;

	/**
	 * @param amount     cura instantanea, ou cura por tick (HOT)
	 * @param ticks      numero de ticks (HOT)
	 * @param intervalMs intervalo entre ticks (HOT)
	 * @param reuseMs    reuse do skill
	 * @param buff       definicao do buff (BUFF)
	 * @param grade      grade do soulshot/spiritshot: none, d, c, b, a, s
	 */
	public record Consumable(int itemId, Type type, int skillId, int level, double amount, int ticks,
			int intervalMs, int reuseMs, BuffDef buff, String grade) {
	}

	public record BuffDef(String stackType, int durationMs, int runSpdAdd, double pAtkSpdMul, double mAtkSpdMul,
			int accuracyAdd) {
	}

	private static final int BUFF_20_MIN = 1_200_000;

	private static final Map<Integer, Consumable> BY_ITEM = Map.ofEntries(
			// Curas de HP ao longo do tempo (stackType HpRecover)
			hot(65, 2001, 2, 3, 5000, 1000), // Red Potion
			hot(725, 2002, 1.5, 4, 5000, 1000), // Healing Drug
			hot(1060, 2031, 10, 15, 1000, 1000), // Lesser Healing Potion
			hot(1073, 2031, 10, 15, 1000, 1000), // Beginner's Potion
			hot(727, 2032, 24, 15, 1000, 1000), // Healing Potion
			hot(1061, 2032, 24, 15, 1000, 1000), // Healing Potion
			hot(1539, 2037, 50, 15, 1000, 10000), // Greater Healing Potion
			// Curas instantaneas
			instant(1540, Type.HEAL_HP, 2038, 1, 440, 500), // Quick Healing Potion
			instant(728, Type.HEAL_MP, 2005, 1, 200, 1000), // Mana Potion
			instant(5591, Type.HEAL_CP, 2166, 1, 50, 1000), // CP Potion
			instant(5592, Type.HEAL_CP, 2166, 2, 200, 1000), // Greater CP Potion
			Map.entry(726, new Consumable(726, Type.HOT_MP, 2003, 1, 20, 5, 4000, 1000, null, null)), // Mana Drug
			// Buffs (stackType igual ao do L2J: o novo substitui o antigo)
			buff(733, 2010, 1, new BuffDef("hit_up", 300_000, 0, 1.0, 1.0, 4)), // Endeavor Potion
			buff(734, 2011, 1, new BuffDef("speed_up", BUFF_20_MIN, 20, 1.0, 1.0, 0)), // Haste Potion
			buff(1374, 2034, 1, new BuffDef("speed_up", BUFF_20_MIN, 33, 1.0, 1.0, 0)), // Greater Haste Potion
			buff(735, 2012, 1, new BuffDef("attack_time_down", BUFF_20_MIN, 0, 1.15, 1.0, 0)), // Potion of Alacrity
			buff(1375, 2035, 1, new BuffDef("attack_time_down", BUFF_20_MIN, 0, 1.33, 1.0, 0)), // Greater Swift Attack
			buff(6035, 2169, 1, new BuffDef("casting_time_down", BUFF_20_MIN, 0, 1.0, 1.23, 0)), // Magic Haste
			buff(6036, 2169, 2, new BuffDef("casting_time_down", BUFF_20_MIN, 0, 1.0, 1.3, 0)), // Greater Magic Haste
			// Soulshots (skill = animacao da carga por grade)
			shot(5789, 2039, "none"), // Beginner's Soulshot
			shot(1835, 2039, "none"), // Soulshot: No Grade
			shot(1463, 2150, "d"),
			shot(1464, 2151, "c"),
			shot(1465, 2152, "b"),
			shot(1466, 2153, "a"),
			shot(1467, 2154, "s"),
			// Spiritshots
			spiritShot(5790, 2061, "none"), // Beginner's Spiritshot
			spiritShot(2509, 2155, "none"), // Spiritshot: No Grade
			spiritShot(2510, 2156, "d"),
			spiritShot(2511, 2157, "c"),
			spiritShot(2512, 2158, "b"),
			spiritShot(2513, 2159, "a"),
			spiritShot(2514, 2159, "s"),
			// Blessed Spiritshots
			blessedSpiritShot(3947, 2160, "none"), // Blessed Spiritshot: No Grade
			blessedSpiritShot(3948, 2161, "d"),
			blessedSpiritShot(3949, 2162, "c"),
			blessedSpiritShot(3950, 2163, "b"),
			blessedSpiritShot(3951, 2164, "a"),
			blessedSpiritShot(3952, 2164, "s"),
			// Mystery Potion (CharChangePotions/MysteryPotion do L2JDream)
			Map.entry(5234, new Consumable(5234, Type.MYSTERY, 2103, 1, 0, 1, BUFF_20_MIN, 0, null, null)),
			// Facelifting Potion A-C
			look(5235, Type.FACE, 0), look(5236, Type.FACE, 1), look(5237, Type.FACE, 2),
			// Dye Potion A-D
			look(5238, Type.HAIR_COLOR, 0), look(5239, Type.HAIR_COLOR, 1), look(5240, Type.HAIR_COLOR, 2),
			look(5241, Type.HAIR_COLOR, 3),
			// Hair Style Change Potion A-G
			look(5242, Type.HAIR_STYLE, 0), look(5243, Type.HAIR_STYLE, 1), look(5244, Type.HAIR_STYLE, 2),
			look(5245, Type.HAIR_STYLE, 3), look(5246, Type.HAIR_STYLE, 4), look(5247, Type.HAIR_STYLE, 5),
			look(5248, Type.HAIR_STYLE, 6),
			// Remedios
			remedy(1831, 2042), // Antidote
			remedy(1832, 2043), // Advanced Antidote
			remedy(1833, 34), // Bandage
			remedy(1834, 2045), // Emergency Dressing
			remedy(3889, 2042)); // Potion of Recovery

	private ConsumableTable() {
	}

	public static Optional<Consumable> get(int itemId) {
		return Optional.ofNullable(BY_ITEM.get(itemId));
	}

	public static boolean isSoulshot(int itemId) {
		var c = BY_ITEM.get(itemId);
		return c != null && c.type() == Type.SOULSHOT;
	}

	public static boolean isSpiritshot(int itemId) {
		var c = BY_ITEM.get(itemId);
		return c != null && (c.type() == Type.SPIRITSHOT || c.type() == Type.BLESSED_SPIRITSHOT);
	}

	public static boolean isBlessedSpiritshot(int itemId) {
		var c = BY_ITEM.get(itemId);
		return c != null && c.type() == Type.BLESSED_SPIRITSHOT;
	}

	public static boolean isShot(int itemId) {
		var c = BY_ITEM.get(itemId);
		return c != null && (c.type() == Type.SOULSHOT || c.type() == Type.SPIRITSHOT || c.type() == Type.BLESSED_SPIRITSHOT);
	}

	/** Indice de grade usado no flag de soulshot do pacote Attack e na regra de compatibilidade. */
	public static int gradeIndex(String crystalType) {
		return switch (crystalType == null ? "none" : crystalType) {
			case "d" -> 1;
			case "c" -> 2;
			case "b" -> 3;
			case "a" -> 4;
			case "s", "s80", "s84" -> 5;
			default -> 0;
		};
	}

	private static Map.Entry<Integer, Consumable> hot(int itemId, int skillId, double perTick, int ticks,
			int intervalMs, int reuseMs) {
		return Map.entry(itemId, new Consumable(itemId, Type.HOT_HP, skillId, 1, perTick, ticks, intervalMs, reuseMs,
				null, null));
	}

	private static Map.Entry<Integer, Consumable> instant(int itemId, Type type, int skillId, int level, int amount,
			int reuseMs) {
		return Map.entry(itemId, new Consumable(itemId, type, skillId, level, amount, 0, 0, reuseMs, null, null));
	}

	private static Map.Entry<Integer, Consumable> buff(int itemId, int skillId, int level, BuffDef def) {
		return Map.entry(itemId, new Consumable(itemId, Type.BUFF, skillId, level, 0, 0, 0, 0, def, null));
	}

	private static Map.Entry<Integer, Consumable> shot(int itemId, int skillId, String grade) {
		return Map.entry(itemId, new Consumable(itemId, Type.SOULSHOT, skillId, 1, 0, 0, 0, 0, null, grade));
	}

	private static Map.Entry<Integer, Consumable> spiritShot(int itemId, int skillId, String grade) {
		return Map.entry(itemId, new Consumable(itemId, Type.SPIRITSHOT, skillId, 1, 0, 0, 0, 0, null, grade));
	}

	private static Map.Entry<Integer, Consumable> blessedSpiritShot(int itemId, int skillId, String grade) {
		return Map.entry(itemId, new Consumable(itemId, Type.BLESSED_SPIRITSHOT, skillId, 1, 0, 0, 0, 0, null, grade));
	}

	/** Pocoes de aparencia: todas usam a animacao do skill 2003 (como no L2JDream). */
	private static Map.Entry<Integer, Consumable> look(int itemId, Type type, int value) {
		return Map.entry(itemId, new Consumable(itemId, type, 2003, 1, value, 0, 0, 0, null, null));
	}

	private static Map.Entry<Integer, Consumable> remedy(int itemId, int skillId) {
		return Map.entry(itemId, new Consumable(itemId, Type.REMEDY, skillId, 1, 0, 0, 0, 0, null, null));
	}
}
