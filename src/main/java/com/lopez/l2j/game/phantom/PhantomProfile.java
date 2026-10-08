package com.lopez.l2j.game.phantom;

/**
 * Perfil descritor para inicializacao e configuracao de um Phantom (Fake Player) - Onda C8.
 */
public record PhantomProfile(
		String name,
		int classId,
		int level,
		PhantomCombatKit kit,
		int spawnX,
		int spawnY,
		int spawnZ,
		PhantomBehaviorMode mode,
		String storeTitle
) {
	public PhantomProfile(String name, int classId, int level, PhantomCombatKit kit, int spawnX, int spawnY, int spawnZ, PhantomBehaviorMode mode) {
		this(name, classId, level, kit, spawnX, spawnY, spawnZ, mode, null);
	}
}
