package com.lopez.l2j.game.augmentation;

/**
 * Representa os dados de augmentacao de uma arma (Life Stone).
 * Os atributos codificam os bonus de status em 32 bits:
 * - stat12 (16 bits inferiores): primeiro bonus de status
 * - stat34 (16 bits superiores): segundo bonus de status ou id de skill de augmentacao
 */
public record Augmentation(int attributes, int skillId, int skillLevel) {

	public int stat12() {
		return 0x0000FFFF & attributes;
	}

	public int stat34() {
		return attributes >> 16;
	}

	public boolean hasSkill() {
		return skillId > 0 && skillLevel > 0;
	}
}
