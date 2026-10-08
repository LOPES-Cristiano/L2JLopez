package com.lopez.l2j.game.phantom;

/**
 * Representa uma acao decidida pelo loop de IA de um Phantom ou do AutoFarm - Onda C8.
 */
public record PhantomAction(
		ActionType type,
		int targetId,
		int skillId,
		int x,
		int y,
		int z,
		String message
) {
	public enum ActionType {
		ATTACK,
		CAST_SKILL,
		USE_POTION,
		KITE_RETREAT,
		MOVE_TO,
		SIT_STORE,
		STAND_UP,
		IDLE
	}

	public static PhantomAction attack(int targetId) {
		return new PhantomAction(ActionType.ATTACK, targetId, 0, 0, 0, 0, null);
	}

	public static PhantomAction cast(int targetId, int skillId) {
		return new PhantomAction(ActionType.CAST_SKILL, targetId, skillId, 0, 0, 0, null);
	}

	public static PhantomAction usePotion(int itemId) {
		return new PhantomAction(ActionType.USE_POTION, 0, itemId, 0, 0, 0, null);
	}

	public static PhantomAction kite(int targetId, int retreatX, int retreatY, int z) {
		return new PhantomAction(ActionType.KITE_RETREAT, targetId, 0, retreatX, retreatY, z, "Kiting away from target");
	}

	public static PhantomAction moveTo(int x, int y, int z) {
		return new PhantomAction(ActionType.MOVE_TO, 0, 0, x, y, z, null);
	}

	public static PhantomAction sitStore(String storeTitle) {
		return new PhantomAction(ActionType.SIT_STORE, 0, 0, 0, 0, 0, storeTitle);
	}

	public static PhantomAction standUp() {
		return new PhantomAction(ActionType.STAND_UP, 0, 0, 0, 0, 0, null);
	}

	public static PhantomAction idle() {
		return new PhantomAction(ActionType.IDLE, 0, 0, 0, 0, 0, null);
	}
}
