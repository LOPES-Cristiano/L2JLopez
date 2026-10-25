package com.lopez.l2j.game.zone;

import java.util.List;

/**
 * Entidade de zona no mapa com tipos, regras de PvP e formas poligonais.
 */
public record Zone(
		int id,
		String name,
		ZoneType type,
		boolean isPeace,
		boolean isArena,
		int castleId,
		int townId,
		List<ZoneShape> shapes,
		int skillId,
		int skillLvl,
		int damagePerSec,
		double moveBonus
) {
	public Zone(
			int id,
			String name,
			ZoneType type,
			boolean isPeace,
			boolean isArena,
			int castleId,
			int townId,
			List<ZoneShape> shapes) {
		this(id, name, type, isPeace, isArena, castleId, townId, shapes, 0, 0, 100, -0.5);
	}

	public boolean contains(int x, int y, int z) {
		for (ZoneShape shape : shapes) {
			if (shape.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}
}
