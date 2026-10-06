package com.lopez.l2j.game.achievements;

import java.time.Instant;

public record PlayerAchievement(
		int ownerId,
		int achievementId,
		int timesCompleted,
		Instant lastCompletedAt
) {}
