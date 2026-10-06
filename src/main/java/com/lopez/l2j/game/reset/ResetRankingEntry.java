package com.lopez.l2j.game.reset;

public record ResetRankingEntry(
		int playerId,
		String playerName,
		int dailyCount,
		int monthlyCount,
		int totalCount
) {}
