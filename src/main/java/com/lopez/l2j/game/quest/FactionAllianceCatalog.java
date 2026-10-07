package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico das Quests de Alianças Épicas de Facções (Ketra Orcs vs Varka Silenos).
 *
 * Mapeia os 5 estágios de aliança, insígnias de soldados/capitães/generais e selos de fidelidade.
 */
public final class FactionAllianceCatalog {

	public record AllianceFaction(
			int questId,
			String factionName,
			int hierarchNpcId,
			String hierarchName,
			String region,
			List<Integer> markOfAllianceItemIds,
			int soldierBadgeId,
			int captainBadgeId,
			int generalBadgeId) {
	}

	private static final Map<Integer, AllianceFaction> BY_QUEST_ID = new LinkedHashMap<>();

	static {
		// Quest 605: Alliance with Ketra Orcs
		BY_QUEST_ID.put(605, new AllianceFaction(
				605,
				"Ketra Orcs",
				31371,
				"Hierarch Wahkan",
				"Ketra Orc Outpost",
				List.of(7211, 7212, 7213, 7214, 7215), // Mark of Ketra's Alliance Stage 1 to 5
				7216, // Varka's Badge - Soldier
				7217, // Varka's Badge - Captain
				7218  // Varka's Badge - General
		));

		// Quest 611: Alliance with Varka Silenos
		BY_QUEST_ID.put(611, new AllianceFaction(
				611,
				"Varka Silenos",
				31378,
				"Hierarch Naran Ashanuk",
				"Varka Silenos Stronghold",
				List.of(7221, 7222, 7223, 7224, 7225), // Mark of Varka's Alliance Stage 1 to 5
				7226, // Ketra's Badge - Soldier
				7227, // Ketra's Badge - Captain
				7228  // Ketra's Badge - General
		));
	}

	private FactionAllianceCatalog() {
	}

	public static Map<Integer, AllianceFaction> allFactions() {
		return Collections.unmodifiableMap(BY_QUEST_ID);
	}

	public static Optional<AllianceFaction> findByQuestId(int questId) {
		return Optional.ofNullable(BY_QUEST_ID.get(questId));
	}
}
