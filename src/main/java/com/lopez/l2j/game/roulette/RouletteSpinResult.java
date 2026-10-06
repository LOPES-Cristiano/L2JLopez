package com.lopez.l2j.game.roulette;

public record RouletteSpinResult(
		boolean success,
		RouletteItem item,
		String message
) {
	public static RouletteSpinResult success(RouletteItem item) {
		return new RouletteSpinResult(true, item, "Congratulations! You won " + item.name() + " x" + item.count() + "!");
	}

	public static RouletteSpinResult failure(String message) {
		return new RouletteSpinResult(false, null, message);
	}
}
