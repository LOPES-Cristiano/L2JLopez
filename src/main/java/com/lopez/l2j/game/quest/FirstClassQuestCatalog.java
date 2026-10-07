package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico e Registro Mestre das 18 Quests de 1ª Mudança de Classe (401–418) no Lineage II Interlude.
 * Mapeia rigorosamente cada ocupação inicial para seu teste de transição, NPC mestre e item de prova.
 */
public final class FirstClassQuestCatalog {

	public record FirstClassInfo(
			int questId,
			String questName,
			String title,
			int startingClassId,
			int targetClassId,
			int startNpcId,
			int proofItemId,
			int minLevel
	) {}

	private static final Map<Integer, FirstClassInfo> QUESTS_BY_ID = new LinkedHashMap<>();
	private static final Map<Integer, FirstClassInfo> QUESTS_BY_TARGET_CLASS = new LinkedHashMap<>();

	static {
		// Humanos
		register(401, "401_PathToWarrior", "Path of the Warrior", 0, 1, 30010, 1145, 18);
		register(402, "402_PathToKnight", "Path of the Human Knight", 0, 4, 30417, 1161, 18);
		register(403, "403_PathToRogue", "Path of the Rogue", 0, 7, 30379, 1190, 18);
		register(404, "404_PathToWizard", "Path of the Human Wizard", 10, 11, 30391, 1210, 18);
		register(405, "405_PathToCleric", "Path of the Cleric", 10, 15, 30022, 1201, 18);

		// Elfos
		register(406, "406_PathToElvenKnight", "Path of the Elven Knight", 18, 19, 30327, 1220, 18);
		register(407, "407_PathToElvenScout", "Path of the Elven Scout", 18, 22, 30328, 1207, 18);
		register(408, "408_PathToElvenWizard", "Path of the Elven Wizard", 25, 26, 30414, 1218, 18);
		register(409, "409_PathToOracle", "Path of the Elven Oracle", 25, 29, 30293, 1235, 18);

		// Dark Elves
		register(410, "410_PathToPalusKnight", "Path of the Palus Knight", 31, 32, 30329, 1246, 18);
		register(411, "411_PathToAssassin", "Path of the Assassin", 31, 35, 30416, 1258, 18);
		register(412, "412_PathToDarkWizard", "Path of the Dark Wizard", 38, 39, 30415, 1254, 18);
		register(413, "413_PathToShillienOracle", "Path of the Shillien Oracle", 38, 42, 30330, 1262, 18);

		// Orcs
		register(414, "414_PathToOrcRaider", "Path of the Orc Raider", 44, 45, 30505, 1592, 18);
		register(415, "415_PathToOrcMonk", "Path of the Orc Monk", 44, 47, 30587, 1615, 18);
		register(416, "416_PathToOrcShaman", "Path of the Orc Shaman", 49, 50, 30585, 1630, 18);

		// Anões
		register(417, "417_PathToScavenger", "Path of the Scavenger", 53, 54, 30517, 1642, 18);
		register(418, "418_PathToArtisan", "Path of the Artisan", 53, 56, 30527, 1631, 18);
	}

	private static void register(int id, String name, String title, int startClass, int targetClass, int npc, int proofItem, int minLvl) {
		FirstClassInfo info = new FirstClassInfo(id, name, title, startClass, targetClass, npc, proofItem, minLvl);
		QUESTS_BY_ID.put(id, info);
		QUESTS_BY_TARGET_CLASS.put(targetClass, info);
	}

	public static Map<Integer, FirstClassInfo> getAllQuests() {
		return Collections.unmodifiableMap(QUESTS_BY_ID);
	}

	public static Optional<FirstClassInfo> getById(int questId) {
		return Optional.ofNullable(QUESTS_BY_ID.get(questId));
	}

	public static Optional<FirstClassInfo> getByTargetClass(int targetClassId) {
		return Optional.ofNullable(QUESTS_BY_TARGET_CLASS.get(targetClassId));
	}

	public static boolean isFirstClassProofItem(int itemId) {
		return QUESTS_BY_ID.values().stream().anyMatch(q -> q.proofItemId() == itemId);
	}
}
