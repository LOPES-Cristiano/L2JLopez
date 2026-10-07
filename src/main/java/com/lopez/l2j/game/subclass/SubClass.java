package com.lopez.l2j.game.subclass;

/**
 * Representa uma subclasse de um personagem no Lineage II Interlude.
 *
 * @param classId    ID da classe da subclasse.
 * @param exp        Experiência acumulada nesta subclasse.
 * @param sp         Skill Points acumulados nesta subclasse.
 * @param level      Nível atual na subclasse (inicia no nível 40).
 * @param classIndex Índice da subclasse (1, 2 ou 3). A classe base tem índice 0.
 */
public record SubClass(int classId, long exp, int sp, int level, int classIndex) {

	public static final int BASE_INDEX = 0;
	public static final int INITIAL_SUB_LEVEL = 40;
	public static final long INITIAL_SUB_EXP = 15_422_851L; // EXP canônica para nível 40 no Interlude

	public SubClass withClassId(int newClassId) {
		return new SubClass(newClassId, exp, sp, level, classIndex);
	}

	public SubClass withProgress(int newLevel, long newExp, int newSp) {
		return new SubClass(classId, newExp, newSp, newLevel, classIndex);
	}
}
