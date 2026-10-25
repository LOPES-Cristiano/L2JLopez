package com.lopez.l2j.game.stats;

/**
 * Estatisticas suportadas pelo motor de Calculators do Lineage II Interlude.
 */
public enum Stat {
	// Vital Stats
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	MAX_RECOVERABLE_HP("maxRecoverableHp"),
	MAX_RECOVERABLE_MP("maxRecoverableMp"),
	MAX_RECOVERABLE_CP("maxRecoverableCp"),
	REG_HP("regHp"),
	REG_MP("regMp"),
	REG_CP("regCp"),
	MANA_CHARGE("manaCharge"),
	RECHARGE_MP_RATE("gainMp"),
	HEAL_EFFECTIVNESS("gainHp"),
	HEAL_PROFICIENCY("giveHp"),
	HEAL_STATIC_BONUS("bonusHp"),
	HEAL_PERCENT("healPercent"),

	// Combat Core Stats
	P_ATK("pAtk"),
	M_ATK("mAtk"),
	P_DEF("pDef"),
	M_DEF("mDef"),
	P_ATK_SPD("pAtkSpd"),
	M_ATK_SPD("mAtkSpd"),
	CRITICAL_RATE("rCrit"),
	CRITICAL_RATE_POS("rCritPos"),
	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"),
	BOW_CRIT_RATE("bowCritRate"),
	M_CRITICAL_RATE("mCritRate"),
	BLOW_RATE("blowRate"),
	LETHAL_RATE("lethalRate"),
	ACCURACY("accCombat"),
	EVASION("rEvas"),
	P_SKILL_EVASION("pSkillEvas"),
	ATTACK_CANCEL("cancel"),
	RUN_SPEED("runSpd"),
	WALK_SPEED("walkSpd"),

	// Shield & Range Stats
	SHIELD_DEFENCE("sDef"),
	SHIELD_RATE("rShld"),
	SHIELD_DEFENCE_ANGLE("shldAngle"),
	POWER_ATTACK_RANGE("pAtkRange"),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	POWER_ATTACK_ANGLE("pAtkAngle"),
	POLE_TARG_COUNT("atkCountMax"),

	// Base Attributes
	STAT_STR("STR"),
	STAT_CON("CON"),
	STAT_DEX("DEX"),
	STAT_INT("INT"),
	STAT_WIT("WIT"),
	STAT_MEN("MEN"),

	// Absorption, Reflection & Transfer
	ABSORB_DAM("absorbDam"),
	ABSORB_CP_PERCENT("absorbCpPercent"),
	TRANSFER_DAMAGE_PERCENT("transDam"),
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"),
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"),
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam"),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam"),

	// PvP Bonus & Defense
	PVP_PHYSICAL_DMG("pvpPhysDmg"),
	PVP_MAGICAL_DMG("pvpMagicalDmg"),
	PVP_PHYS_SKILL_DMG("pvpPhysSkillsDmg"),
	PVP_PHYSICAL_DEF("pvpPhysDef"),
	PVP_MAGICAL_DEF("pvpMagicalDef"),
	PVP_PHYS_SKILL_DEF("pvpPhysSkillsDef"),

	// Skill Reuse & Resource Consumption Rates
	ATK_REUSE("atkReuse"),
	P_REUSE("pReuse"),
	M_REUSE("mReuse"),
	MUSIC_REUSE_RATE("musicReuse"),
	PHYSICAL_MP_CONSUME_RATE("PhysicalMpConsumeRate"),
	MAGICAL_MP_CONSUME_RATE("MagicalMpConsumeRate"),
	DANCE_MP_CONSUME_RATE("DanceMpConsumeRate"),
	BOW_MP_CONSUME_RATE("BowMpConsumeRate"),
	HP_CONSUMPTION_RATE("HpConsumeRate"),
	MP_CONSUMPTION_RATE("MpConsumeRate"),

	// Elemental Attack Power & Defense Vulnerability
	FIRE_POWER("firePower"),
	WATER_POWER("waterPower"),
	WIND_POWER("windPower"),
	EARTH_POWER("earthPower"),
	HOLY_POWER("holyPower"),
	DARK_POWER("darkPower"),
	FIRE_VULN("fireVuln"),
	WATER_VULN("waterVuln"),
	WIND_VULN("windVuln"),
	EARTH_VULN("earthVuln"),
	HOLY_VULN("holyVuln"),
	DARK_VULN("darkVuln"),

	// Weapon Type Vulnerabilities
	SWORD_WPN_VULN("swordWpnVuln"),
	BLUNT_WPN_VULN("bluntWpnVuln"),
	DAGGER_WPN_VULN("daggerWpnVuln"),
	BOW_WPN_VULN("bowWpnVuln"),
	CROSSBOW_WPN_VULN("crossbowWpnVuln"),
	POLE_WPN_VULN("poleWpnVuln"),
	ETC_WPN_VULN("etcWpnVuln"),
	FIST_WPN_VULN("fistWpnVuln"),
	DUAL_WPN_VULN("dualWpnVuln"),
	DUAL_FIST_WPN_VULN("dualFistWpnVuln"),
	BIG_SWORD_WPN_VULN("bigSwordWpnVuln"),
	BIG_BLUNT_WPN_VULN("bigBluntWpnVuln"),
	CRIT_VULN("critVuln"),
	MAGIC_DAM_VULN("magicDamVuln"),
	FALL_VULN("fallVuln"),

	// Status Vulnerabilities & Proficiencies
	AGGRESSION_VULN("aggressionVuln"),
	BLEED_VULN("bleedVuln"),
	POISON_VULN("poisonVuln"),
	STUN_VULN("stunVuln"),
	PARALYZE_VULN("paralyzeVuln"),
	ROOT_VULN("rootVuln"),
	SLEEP_VULN("sleepVuln"),
	CONFUSION_VULN("confusionVuln"),
	MOVEMENT_VULN("movementVuln"),
	DERANGEMENT_VULN("derangementVuln"),
	DEBUFF_VULN("debuffVuln"),
	CANCEL_VULN("cancelVuln"),
	AGGRESSION_PROF("aggressionProf"),
	BLEED_PROF("bleedProf"),
	POISON_PROF("poisonProf"),
	STUN_PROF("stunProf"),
	PARALYZE_PROF("paralyzeProf"),
	ROOT_PROF("rootProf"),
	SLEEP_PROF("sleepProf"),
	CONFUSION_PROF("confusionProf"),
	MOVEMENT_PROF("movementProf"),
	DERANGEMENT_PROF("derangementProf"),
	DEBUFF_PROF("debuffProf"),
	CANCEL_PROF("cancelProf"),

	// NPC Race P.Atk & P.Def Modifiers
	PATK_ANIMALS("pAtk-animals"),
	PATK_MONSTERS("pAtk-monsters"),
	PATK_PLANTS("pAtk-plants"),
	PATK_INSECTS("pAtk-insects"),
	PATK_REPTILES("pAtk-reptiles"),
	PATK_GIANTS("pAtk-giants"),
	PATK_UNDEAD("pAtk-undead"),
	PATK_DRAGONS("pAtk-dragons"),
	PATK_MAGIC_CREATURES("pAtk-magicCreature"),
	PDEF_ANIMALS("pDef-animals"),
	PDEF_MONSTERS("pDef-monsters"),
	PDEF_PLANTS("pDef-plants"),
	PDEF_INSECTS("pDef-insects"),
	PDEF_REPTILES("pDef-reptiles"),
	PDEF_GIANTS("pDef-giants"),
	PDEF_UNDEAD("pDef-undead"),
	PDEF_DRAGONS("pDef-dragons"),
	PDEF_MAGIC_CREATURES("pDef-magicCreature"),
	VALAKAS("valakas"),
	VALAKAS_RES("valakasRes"),

	// Limits, Multipliers & Utility
	MAX_LOAD("maxLoad"),
	WEIGHT_PENALTY("weightPenalty"),
	INV_LIM("inventoryLimit"),
	WH_LIM("whLimit"),
	FREIGHT_LIM("FreightLimit"),
	P_SELL_LIM("PrivateSellLimit"),
	P_BUY_LIM("PrivateBuyLimit"),
	REC_D_LIM("DwarfRecipeLimit"),
	REC_C_LIM("CommonRecipeLimit"),
	BUFF_LIMIT("buffLimit"),
	DANCE_LIMIT("danceLimit"),
	BREATH("breath"),
	SKILL_MASTERY("skillMastery"),
	EXP_MULTIPLIER("rExp"),
	SP_MULTIPLIER("rSp");

	private final String value;

	Stat(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Stat fromString(String val) {
		if (val == null || val.isBlank()) return null;
		String trimmed = val.trim();
		for (Stat s : values()) {
			if (s.value.equalsIgnoreCase(trimmed) || s.name().equalsIgnoreCase(trimmed)) {
				return s;
			}
		}
		return switch (trimmed.toLowerCase(java.util.Locale.ROOT)) {
			case "patk-magiccreatures", "patk_magic_creatures" -> PATK_MAGIC_CREATURES;
			case "pdef-magiccreatures", "pdef_magic_creatures" -> PDEF_MAGIC_CREATURES;
			case "valakasvuln", "valakas_vuln" -> VALAKAS_RES;
			case "patk-valakas", "valakasatk" -> VALAKAS;
			case "fatalblowrate" -> BLOW_RATE;
			default -> null;
		};
	}
}
