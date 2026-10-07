package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico das Quests de Progressão e Elevação de Nível de Clã do Lineage II Interlude.
 *
 * Mapeia as lendárias missões de Nível 4 (Proof of Clan Alliance) e Nível 5 (Pursuit of Clan Ambition).
 */
public final class ClanQuestCatalog {

	public record ClanQuestEntry(
			int questId,
			String questName,
			int targetClanLevel,
			int minPlayerLevel,
			int startNpcId,
			String startNpcName,
			String startCity,
			int rewardItemId,
			String rewardItemName,
			int rewardSp) {
	}

	private static final Map<Integer, ClanQuestEntry> BY_ID = new LinkedHashMap<>();

	static {
		// Quest 501: Proof of Clan Alliance (Elevação para Clã Nível 4)
		BY_ID.put(501, new ClanQuestEntry(
				501,
				"Proof of Clan Alliance",
				4,
				1,
				30756,
				"Sir Kristof Rodemai",
				"Giran Castle Town",
				3874,
				"Alliance Manifesto",
				120_000));

		// Quest 503: Pursuit of Clan Ambition (Elevação para Clã Nível 5)
		BY_ID.put(503, new ClanQuestEntry(
				503,
				"Pursuit of Clan Ambition",
				5,
				1,
				30760,
				"Sir Gustaf Athebaldt",
				"Town of Oren",
				3870,
				"Seal of Aspiration",
				250_000));
	}

	private ClanQuestCatalog() {
	}

	public static Map<Integer, ClanQuestEntry> allEntries() {
		return Collections.unmodifiableMap(BY_ID);
	}

	public static Optional<ClanQuestEntry> findById(int questId) {
		return Optional.ofNullable(BY_ID.get(questId));
	}

	public static Optional<ClanQuestEntry> findByTargetClanLevel(int level) {
		return BY_ID.values().stream()
				.filter(e -> e.targetClanLevel() == level)
				.findFirst();
	}
}
