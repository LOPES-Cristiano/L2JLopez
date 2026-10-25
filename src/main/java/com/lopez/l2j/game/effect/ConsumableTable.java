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
		REMEDY,
		/** Pedra de energia para Gladiator/Tyrant: aumenta cargas ate o nivel 2. */
		ENERGY_STONE,
		/** Scrolls de buff (ex.: L2Day 3926-3935). */
		SKILL_SCROLL,
		/** Elixires de HP, MP e CP por grade. */
		ELIXIR_HP,
		ELIXIR_MP,
		ELIXIR_CP,
		/** Scrolls of Resurrection (737, 3936). */
		RESURRECT_SCROLL
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
	private static final int ELIXIR_REUSE_MS = 300_000;

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
			instant(5589, Type.ENERGY_STONE, 2165, 1, 1, 500), // Energy Stone
			Map.entry(726, new Consumable(726, Type.HOT_MP, 2003, 1, 20, 5, 4000, 1000, null, null)), // Mana Drug
			// Buffs (stackType igual ao do L2J: o novo substitui o antigo)
			buff(733, 2010, 1, new BuffDef("hit_up", 300_000, 0, 1.0, 1.0, 4)), // Endeavor Potion
			buff(734, 2011, 1, new BuffDef("speed_up", BUFF_20_MIN, 20, 1.0, 1.0, 0)), // Haste Potion
			buff(1374, 2034, 1, new BuffDef("speed_up", BUFF_20_MIN, 33, 1.0, 1.0, 0)), // Greater Haste Potion
			buff(735, 2012, 1, new BuffDef("attack_time_down", BUFF_20_MIN, 0, 1.15, 1.0, 0)), // Potion of Alacrity
			buff(1375, 2035, 1, new BuffDef("attack_time_down", BUFF_20_MIN, 0, 1.33, 1.0, 0)), // Greater Swift Attack
			buff(6035, 2169, 1, new BuffDef("casting_time_down", BUFF_20_MIN, 0, 1.0, 1.23, 0)), // Magic Haste
			buff(6036, 2169, 2, new BuffDef("casting_time_down", BUFF_20_MIN, 0, 1.0, 1.3, 0)), // Greater Magic Haste
			// L2Day Buff Scrolls (3926-3935 -> Skills 2050-2059)
			skillScroll(3926, 2050, 1), // Scroll of Guidance
			skillScroll(3927, 2051, 1), // Scroll of Death Whisper
			skillScroll(3928, 2052, 1), // Scroll of Focus
			skillScroll(3929, 2053, 1), // Scroll of Greater Acumen
			skillScroll(3930, 2054, 1), // Scroll of Haste
			skillScroll(3931, 2055, 1), // Scroll of Agility
			skillScroll(3932, 2056, 1), // Scroll of Mystic Empower
			skillScroll(3933, 2057, 1), // Scroll of Might
			skillScroll(3934, 2058, 1), // Scroll of Wind Walk
			skillScroll(3935, 2059, 1), // Scroll of Shield
			// Event/Clan Hall Buff Scrolls (9146-9155 -> Skills 2050-2059)
			skillScroll(9146, 2050, 1), // Scroll of Guidance (Event)
			skillScroll(9147, 2051, 1), // Scroll of Death Whisper (Event)
			skillScroll(9148, 2052, 1), // Scroll of Focus (Event)
			skillScroll(9149, 2053, 1), // Scroll of Acumen (Event)
			skillScroll(9150, 2054, 1), // Scroll of Haste (Event)
			skillScroll(9151, 2055, 1), // Scroll of Agility (Event)
			skillScroll(9152, 2056, 1), // Scroll of Empower (Event)
			skillScroll(9153, 2057, 1), // Scroll of Might (Event)
			skillScroll(9154, 2058, 1), // Scroll of Wind Walk (Event)
			skillScroll(9155, 2059, 1), // Scroll of Shield (Event)
			// L2Day Blessed Scroll of Escape & SP Scrolls (5593-5595, 9898)
			skillScroll(3958, 2036, 1), // L2Day - Blessed Scroll of Escape
			Map.entry(5593, new Consumable(5593, Type.SKILL_SCROLL, 2167, 1, 500, 0, 0, 3000, null, null)), // SP Scroll: Low Grade
			Map.entry(5594, new Consumable(5594, Type.SKILL_SCROLL, 2167, 2, 5000, 0, 0, 3000, null, null)), // SP Scroll: Medium Grade
			Map.entry(5595, new Consumable(5595, Type.SKILL_SCROLL, 2167, 3, 100000, 0, 0, 3000, null, null)), // SP Scroll: High Grade
			Map.entry(9898, new Consumable(9898, Type.SKILL_SCROLL, 2167, 1, 500, 0, 0, 3000, null, null)), // SP Scroll (Event)
			// Elemental Resistance Potions (9997-10002 -> Skills 2335-2340)
			skillScroll(9997, 2335, 1), // Fire Potion
			skillScroll(9998, 2336, 1), // Water Potion
			skillScroll(9999, 2337, 1), // Wind Potion
			skillScroll(10000, 2338, 1), // Earth Potion
			skillScroll(10001, 2339, 1), // Holy Potion
			skillScroll(10002, 2340, 1), // Dark Potion
			// Fisherman's Potions (8193-8201 -> Skill 2274 lvls 1..9) & Recovery Potion (8202 -> Skill 2275)
			skillScroll(8193, 2274, 1), // Fisherman's Potion - Green
			skillScroll(8194, 2274, 2), // Fisherman's Potion - Jade
			skillScroll(8195, 2274, 3), // Fisherman's Potion - Blue
			skillScroll(8196, 2274, 4), // Fisherman's Potion - Yellow
			skillScroll(8197, 2274, 5), // Fisherman's Potion - Orange
			skillScroll(8198, 2274, 6), // Fisherman's Potion - Purple
			skillScroll(8199, 2274, 7), // Fisherman's Potion - Red
			skillScroll(8200, 2274, 8), // Fisherman's Potion - White
			skillScroll(8201, 2274, 9), // Fisherman's Potion - Black
			skillScroll(8202, 2275, 1), // Fisherman's Recovery Potion / Green Colored Fish Scale
			// Elixir of Life (ELIXIR_HP, skill 2287 lvls 1..6)
			elixir(8622, Type.ELIXIR_HP, 2287, 1, 250, "none"),
			elixir(8623, Type.ELIXIR_HP, 2287, 2, 500, "d"),
			elixir(8624, Type.ELIXIR_HP, 2287, 3, 750, "c"),
			elixir(8625, Type.ELIXIR_HP, 2287, 4, 1000, "b"),
			elixir(8626, Type.ELIXIR_HP, 2287, 5, 1300, "a"),
			elixir(8627, Type.ELIXIR_HP, 2287, 6, 1600, "s"),
			// Elixir of Mental Strength (ELIXIR_MP, skill 2288 lvls 1..6)
			elixir(8628, Type.ELIXIR_MP, 2288, 1, 60, "none"),
			elixir(8629, Type.ELIXIR_MP, 2288, 2, 100, "d"),
			elixir(8630, Type.ELIXIR_MP, 2288, 3, 150, "c"),
			elixir(8631, Type.ELIXIR_MP, 2288, 4, 200, "b"),
			elixir(8632, Type.ELIXIR_MP, 2288, 5, 250, "a"),
			elixir(8633, Type.ELIXIR_MP, 2288, 6, 330, "s"),
			// Elixir of CP (ELIXIR_CP, skill 2289 lvls 1..6)
			elixir(8634, Type.ELIXIR_CP, 2289, 1, 150, "none"),
			elixir(8635, Type.ELIXIR_CP, 2289, 2, 300, "d"),
			elixir(8636, Type.ELIXIR_CP, 2289, 3, 450, "c"),
			elixir(8637, Type.ELIXIR_CP, 2289, 4, 650, "b"),
			elixir(8638, Type.ELIXIR_CP, 2289, 5, 850, "a"),
			elixir(8639, Type.ELIXIR_CP, 2289, 6, 1100, "s"),
			// Scrolls of Resurrection
			resurrectScroll(737, 2014, 1, 20, 5000), // Scroll of Resurrection
			resurrectScroll(3936, 2049, 1, 100, 5000), // Blessed Scroll of Resurrection
			resurrectScroll(3959, 2062, 1, 100, 5000), // L2Day - Blessed Scroll of Resurrection
			resurrectScroll(6387, 2179, 1, 100, 5000), // Blessed Scroll of Resurrection: Pets
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
			spiritShot(2509, 2047, "none"), // Spiritshot: No Grade
			spiritShot(2510, 2155, "d"),
			spiritShot(2511, 2156, "c"),
			spiritShot(2512, 2157, "b"),
			spiritShot(2513, 2158, "a"),
			spiritShot(2514, 2159, "s"),
			// Blessed Spiritshots
			blessedSpiritShot(3947, 2061, "none"), // Blessed Spiritshot: No Grade
			blessedSpiritShot(3948, 2160, "d"),
			blessedSpiritShot(3949, 2161, "c"),
			blessedSpiritShot(3950, 2162, "b"),
			blessedSpiritShot(3951, 2163, "a"),
			blessedSpiritShot(3952, 2164, "s"),
			// Mystery Potion (CharChangePotions/MysteryPotion do L2JDream)
			Map.entry(5234, new Consumable(5234, Type.MYSTERY, 2103, 1, 0, 1, BUFF_20_MIN, 0, null, null)),
			// Facelifting Potion A-C (skills 2122-2124)
			look(5235, Type.FACE, 2122, 0), look(5236, Type.FACE, 2123, 1), look(5237, Type.FACE, 2124, 2),
			// Dye Potion A-D (skills 2125-2128)
			look(5238, Type.HAIR_COLOR, 2125, 0), look(5239, Type.HAIR_COLOR, 2126, 1), look(5240, Type.HAIR_COLOR, 2127, 2),
			look(5241, Type.HAIR_COLOR, 2128, 3),
			// Hair Style Change Potion A-G (skills 2129-2135)
			look(5242, Type.HAIR_STYLE, 2129, 0), look(5243, Type.HAIR_STYLE, 2130, 1), look(5244, Type.HAIR_STYLE, 2131, 2),
			look(5245, Type.HAIR_STYLE, 2132, 3), look(5246, Type.HAIR_STYLE, 2133, 4), look(5247, Type.HAIR_STYLE, 2134, 5),
			look(5248, Type.HAIR_STYLE, 2135, 6),
			// Remedios
			remedy(1831, 2042), // Antidote
			remedy(1832, 2043), // Advanced Antidote
			remedy(1833, 2044), // Bandage
			remedy(1834, 2045), // Emergency Dressing
			remedy(3889, 2060), // Potion of Recovery
			remedy(6037, 2170)); // Scroll of Waking

	private ConsumableTable() {
	}

	public static Optional<Consumable> get(int itemId) {
		return Optional.ofNullable(BY_ITEM.get(itemId));
	}

	public static Optional<Consumable> bySkillId(int skillId) {
		for (Consumable c : BY_ITEM.values()) {
			if (c.skillId() == skillId && c.buff() != null) {
				return Optional.of(c);
			}
		}
		return Optional.empty();
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

	private static Map.Entry<Integer, Consumable> skillScroll(int itemId, int skillId, int level) {
		return Map.entry(itemId, new Consumable(itemId, Type.SKILL_SCROLL, skillId, level, 0, 0, 0, 0, null, null));
	}

	private static Map.Entry<Integer, Consumable> elixir(int itemId, Type type, int skillId, int level, int amount,
			String grade) {
		return Map.entry(itemId, new Consumable(itemId, type, skillId, level, amount, 0, 0, ELIXIR_REUSE_MS, null, grade));
	}

	private static Map.Entry<Integer, Consumable> resurrectScroll(int itemId, int skillId, int level, int amount,
			int reuseMs) {
		return Map.entry(itemId, new Consumable(itemId, Type.RESURRECT_SCROLL, skillId, level, amount, 0, 0, reuseMs, null, null));
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

	/** Pocoes de aparencia (skills 2122-2135 do data/xml/stats/skills/2100-2199.xml). */
	private static Map.Entry<Integer, Consumable> look(int itemId, Type type, int skillId, int value) {
		return Map.entry(itemId, new Consumable(itemId, type, skillId, 1, value, 0, 0, 0, null, null));
	}

	private static Map.Entry<Integer, Consumable> remedy(int itemId, int skillId) {
		return Map.entry(itemId, new Consumable(itemId, Type.REMEDY, skillId, 1, 0, 0, 0, 0, null, null));
	}
}
