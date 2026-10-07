package com.lopez.l2j.game.stats;

/**
 * Estatisticas suportadas pelo motor de Calculators do Lineage II Interlude.
 */
public enum Stat {
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	P_ATK("pAtk"),
	M_ATK("mAtk"),
	P_DEF("pDef"),
	M_DEF("mDef"),
	P_ATK_SPD("pAtkSpd"),
	M_ATK_SPD("mAtkSpd"),
	CRITICAL_RATE("rCrit"),
	M_CRITICAL_RATE("mCritRate"),
	ACCURACY("accCombat"),
	EVASION("rEvas"),
	RUN_SPEED("runSpd"),
	WALK_SPEED("walkSpd");

	private final String value;

	Stat(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Stat fromString(String val) {
		if (val == null) return null;
		for (Stat s : values()) {
			if (s.value.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
				return s;
			}
		}
		return null;
	}
}
