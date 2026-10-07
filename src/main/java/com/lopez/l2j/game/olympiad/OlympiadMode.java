package com.lopez.l2j.game.olympiad;

/**
 * Modalidades de competicao oficial das Grandes Olimpiadas de Lineage II Interlude:
 * 1. CLASS_FREE: 1v1 sem restricao de classe (minimo 4 participantes na fila).
 * 2. CLASS_BASED: 1v1 restrito a mesma classe (minimo 4 participantes da mesma classe na fila).
 * 3. TEAM_BASED: 3v3 entre equipes de nobres (minimo 2 equipes de 3 jogadores / 6 participantes).
 */
public enum OlympiadMode {
	CLASS_FREE("Class-Free 1v1", 4, 1, 10),
	CLASS_BASED("Class-Based 1v1", 4, 1, 10),
	TEAM_BASED("Team-Based 3v3", 6, 3, 20);

	private final String displayName;
	private final int minQueueSize;
	private final int teamSize;
	private final int maxPointTransfer;

	OlympiadMode(String displayName, int minQueueSize, int teamSize, int maxPointTransfer) {
		this.displayName = displayName;
		this.minQueueSize = minQueueSize;
		this.teamSize = teamSize;
		this.maxPointTransfer = maxPointTransfer;
	}

	public String displayName() {
		return displayName;
	}

	public int minQueueSize() {
		return minQueueSize;
	}

	public int teamSize() {
		return teamSize;
	}

	public int maxPointTransfer() {
		return maxPointTransfer;
	}
}
