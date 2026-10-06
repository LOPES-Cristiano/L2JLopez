package com.lopez.l2j.game.zone;

import java.awt.Polygon;

/**
 * Forma geometrica tridimensional de uma zona delimitada por poligono 2D e limites Z min/max.
 */
public record ZoneShape(
		int zMin,
		int zMax,
		Polygon polygon
) {
	public boolean contains(int x, int y, int z) {
		if (z < zMin || z > zMax) {
			return false;
		}
		return polygon.contains(x, y);
	}
}
