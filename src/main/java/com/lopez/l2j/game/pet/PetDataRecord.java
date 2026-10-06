package com.lopez.l2j.game.pet;

/**
 * Registro de estado e persistencia de mascote (tabela `pets`).
 */
public record PetDataRecord(
		int itemObjectId,
		String name,
		int level,
		int curHp,
		int curMp,
		long exp,
		int sp,
		int fed,
		int weapon,
		int armor,
		int jewel
) {

	public PetDataRecord withExp(long newExp, int newLevel, int newHp, int newMp) {
		return new PetDataRecord(itemObjectId, name, newLevel, newHp, newMp, newExp, sp, fed, weapon, armor, jewel);
	}

	public PetDataRecord withFed(int newFed) {
		return new PetDataRecord(itemObjectId, name, level, curHp, curMp, exp, sp, newFed, weapon, armor, jewel);
	}

	public PetDataRecord withHpMp(int newHp, int newMp) {
		return new PetDataRecord(itemObjectId, name, level, newHp, newMp, exp, sp, fed, weapon, armor, jewel);
	}

	public PetDataRecord withEquipment(int newWeapon, int newArmor, int newJewel) {
		return new PetDataRecord(itemObjectId, name, level, curHp, curMp, exp, sp, fed, newWeapon, newArmor, newJewel);
	}
}
