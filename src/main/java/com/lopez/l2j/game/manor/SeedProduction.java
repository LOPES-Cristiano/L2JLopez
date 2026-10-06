package com.lopez.l2j.game.manor;

/**
 * Oferta de producao de sementes pelo castelo no sistema Manor.
 */
public class SeedProduction {

	private final int seedId;
	private int amount;
	private final int startAmount;
	private final int price;

	public SeedProduction(int seedId, int amount, int startAmount, int price) {
		this.seedId = seedId;
		this.amount = amount;
		this.startAmount = startAmount;
		this.price = price;
	}

	public int seedId() {
		return seedId;
	}

	public int amount() {
		return amount;
	}

	public void amount(int amount) {
		this.amount = Math.max(0, amount);
	}

	public int startAmount() {
		return startAmount;
	}

	public int price() {
		return price;
	}
}
