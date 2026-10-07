package com.lopez.l2j.game.template;

/**
 * Template de classe de personagem (uma entrada de data/player/char_template.xml).
 */
public record CharTemplate(
		int classId,
		String className,
		int raceId,
		int str, int con, int dex, int intel, int wit, int men,
		int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
		int accuracy, int critical, int evasion, int runSpeed, int maxLoad,
		int spawnX, int spawnY, int spawnZ,
		boolean canCraft,
		double maleCollisionRadius, double maleCollisionHeight,
		double femaleCollisionRadius, double femaleCollisionHeight,
		double hpBase, double mpBase, double cpBase,
		double hpAdd, double hpMod,
		double cpAdd, double cpMod,
		double mpAdd, double mpMod,
		int classLevel) {

	public CharTemplate(
			int classId, String className, int raceId,
			int str, int con, int dex, int intel, int wit, int men,
			int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int accuracy, int critical, int evasion, int runSpeed, int maxLoad,
			int spawnX, int spawnY, int spawnZ,
			boolean canCraft,
			double maleCollisionRadius, double maleCollisionHeight,
			double femaleCollisionRadius, double femaleCollisionHeight,
			double hpBase, double mpBase, double cpBase,
			int classLevel) {
		this(classId, className, raceId, str, con, dex, intel, wit, men,
				pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd,
				accuracy, critical, evasion, runSpeed, maxLoad,
				spawnX, spawnY, spawnZ,
				canCraft,
				maleCollisionRadius, maleCollisionHeight,
				femaleCollisionRadius, femaleCollisionHeight,
				hpBase, mpBase, cpBase,
				0, 0, 0, 0, 0, 0,
				classLevel);
	}

	public boolean isStartingClass() {
		return classLevel == 1;
	}

	/**
	 * Grau/Tier da classe:
	 * 0 = Classe inicial (Human Fighter, etc. - class_lvl 1)
	 * 1 = 1st Class Transfer (Warrior, Knight, etc. - class_lvl 20)
	 * 2 = 2nd Class Transfer (Gladiator, Paladin, etc. - class_lvl 40)
	 * 3 = 3rd Class Transfer (Duelist, Phoenix Knight, etc. - class_lvl 76)
	 */
	public int classTier() {
		if (classLevel >= 76) {
			return 3;
		}
		if (classLevel >= 40) {
			return 2;
		}
		if (classLevel >= 20) {
			return 1;
		}
		return 0;
	}

	public double collisionRadius(boolean female) {
		return female ? femaleCollisionRadius : maleCollisionRadius;
	}

	public double collisionHeight(boolean female) {
		return female ? femaleCollisionHeight : maleCollisionHeight;
	}

	public int calculateMaxHp(int level) {
		int lvl = level - classLevel;
		if (lvl <= 0 || hpAdd == 0) {
			return (int) Math.round(hpBase);
		}
		double hpmod = hpMod * lvl;
		double hpmax = (hpAdd + hpmod) * lvl;
		double hpmin = hpAdd * lvl + hpmod;
		return (int) Math.round(hpBase + (hpmax + hpmin) / 2.0);
	}

	public int calculateMaxMp(int level) {
		int lvl = level - classLevel;
		if (lvl <= 0 || mpAdd == 0) {
			return (int) Math.round(mpBase);
		}
		double mpmod = mpMod * lvl;
		double mpmax = (mpAdd + mpmod) * lvl;
		double mpmin = mpAdd * lvl + mpmod;
		return (int) Math.round(mpBase + (mpmax + mpmin) / 2.0);
	}

	public int calculateMaxCp(int level) {
		int lvl = level - classLevel;
		if (lvl <= 0 || cpAdd == 0) {
			return (int) Math.round(cpBase);
		}
		double cpmod = cpMod * lvl;
		double cpmax = (cpAdd + cpmod) * lvl;
		double cpmin = cpAdd * lvl + cpmod;
		return (int) Math.round(cpBase + (cpmax + cpmin) / 2.0);
	}
}
