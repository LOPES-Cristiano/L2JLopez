package com.lopez.l2j.game.craft;

import com.lopez.l2j.game.model.PlayerCharacter;

import java.util.Set;

/**
 * Utilitario de identificacao de classes de Anao (Dwarf) e bonificacao de craft e encantamento.
 * Classes de Anao: 53 (Dwarven Fighter), 54 (Scavenger), 55 (Bounty Hunter), 56 (Artisan),
 * 57 (Warsmith), 117 (Fortune Seeker), 118 (Maestro).
 */
public final class DwarfCraftBonus {

	private static final Set<Integer> DWARF_CLASS_IDS = Set.of(53, 54, 55, 56, 57, 117, 118);

	public static final int DEFAULT_CRAFT_BONUS_PERCENT = 5;
	public static final int DEFAULT_ENCHANT_BONUS_PERCENT = 5;

	private DwarfCraftBonus() {}

	public static boolean isDwarfClass(int classId) {
		return DWARF_CLASS_IDS.contains(classId);
	}

	public static boolean isDwarf(PlayerCharacter player) {
		if (player == null) return false;
		if (player.race() == 4) return true;
		if (isDwarfClass(player.classId())) return true;
		return isDwarfClass(player.baseClassId());
	}

	public static int getCraftBonusPercent() {
		return DEFAULT_CRAFT_BONUS_PERCENT;
	}

	public static int getEnchantBonusPercent() {
		return DEFAULT_ENCHANT_BONUS_PERCENT;
	}
}
