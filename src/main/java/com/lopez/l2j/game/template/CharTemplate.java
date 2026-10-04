package com.lopez.l2j.game.template;

/**
 * Template de classe de personagem (uma entrada de data/player/char_template.xml). Somente os campos usados
 * hoje pelo game server; os demais (unk1/unk2, itens iniciais) entram conforme forem migrados.
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
		int classLevel) {

	/** Classes iniciais (nivel 0 de profissao) que o cliente oferece na criacao de personagem. */
	public boolean isStartingClass() {
		return classLevel == 1;
	}

	public double collisionRadius(boolean female) {
		return female ? femaleCollisionRadius : maleCollisionRadius;
	}

	public double collisionHeight(boolean female) {
		return female ? femaleCollisionHeight : maleCollisionHeight;
	}
}
