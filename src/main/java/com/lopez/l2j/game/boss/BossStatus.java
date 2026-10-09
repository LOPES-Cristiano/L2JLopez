package com.lopez.l2j.game.boss;

/**
 * Estados do ciclo de vida de um Grand Boss no Lineage II Interlude.
 */
public enum BossStatus {
	NOTSPAWN(0),
	ALIVE(1),
	DEAD(2),
	INTERVAL(3),
	WAITING(4),
	FIGHTING(5);

	private final int id;

	BossStatus(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	public static BossStatus fromId(int id) {
		for (BossStatus s : values()) {
			if (s.id == id) {
				return s;
			}
		}
		return NOTSPAWN;
	}
}
