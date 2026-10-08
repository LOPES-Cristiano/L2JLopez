package com.lopez.l2j.game.war;

/**
 * Tipos de faccoes no sistema Good vs Evil (GvE) - Onda C10.
 */
public enum FactionType {
	NONE(0xFFFFFF, "Neutral"),
	GOOD(0x3399FF, "Good"),   // Azul
	EVIL(0xFF3333, "Evil");   // Vermelho

	private final int nameColor;
	private final String displayName;

	FactionType(int nameColor, String displayName) {
		this.nameColor = nameColor;
		this.displayName = displayName;
	}

	public int nameColor() {
		return nameColor;
	}

	public String displayName() {
		return displayName;
	}

	public boolean isOpponent(FactionType other) {
		if (this == NONE || other == null || other == NONE) {
			return false;
		}
		return this != other;
	}
}
