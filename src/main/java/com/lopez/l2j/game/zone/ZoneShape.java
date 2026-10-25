package com.lopez.l2j.game.zone;

import java.awt.Polygon;

/**
 * Forma geometrica tridimensional de uma zona delimitada por poligono 2D e limites Z min/max.
 */
public record ZoneShape(
		int zMin,
		int zMax,
		Polygon polygon,
		int centerX,
		int centerY,
		int radius
) {
	public ZoneShape(int zMin, int zMax, Polygon polygon) {
		this(zMin, zMax, polygon, 0, 0, 0);
	}

	public static ZoneShape cylinder(int zMin, int zMax, int centerX, int centerY, int radius) {
		Polygon boundingPoly = new Polygon();
		boundingPoly.addPoint(centerX - radius, centerY - radius);
		boundingPoly.addPoint(centerX + radius, centerY - radius);
		boundingPoly.addPoint(centerX + radius, centerY + radius);
		boundingPoly.addPoint(centerX - radius, centerY + radius);
		return new ZoneShape(zMin, zMax, boundingPoly, centerX, centerY, Math.max(1, radius));
	}

	public boolean isCylinder() {
		return radius > 0;
	}

	public boolean contains(int x, int y, int z) {
		if (z < zMin || z > zMax) {
			return false;
		}
		if (radius > 0) {
			long dx = (long) x - centerX;
			long dy = (long) y - centerY;
			return (dx * dx + dy * dy) <= (long) radius * radius;
		}
		return polygon != null && polygon.contains(x, y);
	}
}
