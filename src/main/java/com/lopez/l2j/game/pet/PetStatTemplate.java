package com.lopez.l2j.game.pet;

/**
 * Estatisticas de um tipo de mascote em um nivel especifico.
 * Carregado de data/xml/player/pet_stats.xml.
 */
public record PetStatTemplate(
		String type,
		int typeId,
		int level,
		long expMax,
		int hpMax,
		int mpMax,
		int pAtk,
		int pDef,
		int mAtk,
		int mDef,
		int accuracy,
		int evasion,
		int critical,
		int speed,
		int atkSpeed,
		int castSpeed,
		int feedMax,
		int feedBattle,
		int feedNormal,
		int loadMax,
		int hpRegen,
		int mpRegen,
		int ownerExpTaken
) {
}
