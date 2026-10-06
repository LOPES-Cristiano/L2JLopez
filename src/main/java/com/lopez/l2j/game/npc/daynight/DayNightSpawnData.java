package com.lopez.l2j.game.npc.daynight;

/**
 * Representa a definicao de um ponto de spawn com restricao de ciclo (dia ou noite).
 */
public record DayNightSpawnData(int npcTemplateId, int x, int y, int z, int heading, int count) {

	public DayNightSpawnData(int npcTemplateId, int x, int y, int z, int heading) {
		this(npcTemplateId, x, y, z, heading, 1);
	}
}
