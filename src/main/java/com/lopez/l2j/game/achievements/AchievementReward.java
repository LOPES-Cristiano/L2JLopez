package com.lopez.l2j.game.achievements;

public record AchievementReward(int itemId, long count, int enchantLevel) {
	public AchievementReward(int itemId, long count) {
		this(itemId, count, 0);
	}
}
