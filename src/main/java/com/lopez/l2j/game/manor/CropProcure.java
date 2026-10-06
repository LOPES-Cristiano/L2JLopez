package com.lopez.l2j.game.manor;

/**
 * Ordem de compra de colheita pelo castelo no sistema Manor.
 */
public class CropProcure {

	private final int cropId;
	private int amount;
	private final int startAmount;
	private final int price;
	private final int rewardType;

	public CropProcure(int cropId, int amount, int startAmount, int price, int rewardType) {
		this.cropId = cropId;
		this.amount = amount;
		this.startAmount = startAmount;
		this.price = price;
		this.rewardType = rewardType;
	}

	public int cropId() {
		return cropId;
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

	public int rewardType() {
		return rewardType;
	}
}
