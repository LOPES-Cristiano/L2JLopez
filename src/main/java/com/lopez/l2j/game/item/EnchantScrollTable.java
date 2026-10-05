package com.lopez.l2j.game.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tabela de pergaminhos de encantamento (Enchant Scrolls) com definicao de tipo, grade e blessed.
 */
public final class EnchantScrollTable {

	public record ScrollInfo(int itemId, boolean isWeapon, String grade, boolean isBlessed, boolean isCrystal) {
	}

	private static final Map<Integer, ScrollInfo> SCROLLS = new HashMap<>();

	static {
		// Weapon Scrolls - Normal
		add(955, true, "d", false, false);
		add(951, true, "c", false, false);
		add(947, true, "b", false, false);
		add(729, true, "a", false, false);
		add(959, true, "s", false, false);

		// Weapon Scrolls - Blessed
		add(6575, true, "d", true, false);
		add(6573, true, "c", true, false);
		add(6571, true, "b", true, false);
		add(6569, true, "a", true, false);
		add(6577, true, "s", true, false);

		// Weapon Scrolls - Crystal
		add(957, true, "d", false, true);
		add(953, true, "c", false, true);
		add(949, true, "b", false, true);
		add(731, true, "a", false, true);
		add(961, true, "s", false, true);

		// Armor Scrolls - Normal
		add(956, false, "d", false, false);
		add(952, false, "c", false, false);
		add(948, false, "b", false, false);
		add(730, false, "a", false, false);
		add(960, false, "s", false, false);

		// Armor Scrolls - Blessed
		add(6576, false, "d", true, false);
		add(6574, false, "c", true, false);
		add(6572, false, "b", true, false);
		add(6570, false, "a", true, false);
		add(6578, false, "s", true, false);

		// Armor Scrolls - Crystal
		add(958, false, "d", false, true);
		add(954, false, "c", false, true);
		add(950, false, "b", false, true);
		add(732, false, "a", false, true);
		add(962, false, "s", false, true);
	}

	private static void add(int itemId, boolean isWeapon, String grade, boolean isBlessed, boolean isCrystal) {
		SCROLLS.put(itemId, new ScrollInfo(itemId, isWeapon, grade.toLowerCase(), isBlessed, isCrystal));
	}

	public static Optional<ScrollInfo> get(int itemId) {
		return Optional.ofNullable(SCROLLS.get(itemId));
	}

	public static boolean isEnchantScroll(int itemId) {
		return SCROLLS.containsKey(itemId);
	}
}
