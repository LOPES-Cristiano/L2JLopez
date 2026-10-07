package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Oficial das Quests de Subclasse, Nobless e as 31 Sagas de 3ª Classe (Quests 70 a 100).
 */
public final class NoblessAndSagaCatalog {

	public record SagaInfo(
			int questId,
			String questName,
			String title,
			int secondClassId,
			int thirdClassId,
			String thirdClassName
	) {}

	public record NoblessQuestInfo(
			int questId,
			String questName,
			String title,
			int minLevel,
			int starterNpcId
	) {}

	private static final Map<Integer, SagaInfo> SAGAS_BY_QUEST_ID = new LinkedHashMap<>();
	private static final Map<Integer, SagaInfo> SAGAS_BY_THIRD_CLASS = new LinkedHashMap<>();
	private static final Map<Integer, NoblessQuestInfo> NOBLESS_QUESTS = new LinkedHashMap<>();

	static {
		// ==================== NOBLESS (Quests 241, 242, 246, 247) ====================
		NOBLESS_QUESTS.put(241, new NoblessQuestInfo(241, "241_PossessorOfAPreciousSoul_1", "Possessor of a Precious Soul - Part 1", 75, 31739)); // Talien
		NOBLESS_QUESTS.put(242, new NoblessQuestInfo(242, "242_PossessorOfAPreciousSoul_2", "Possessor of a Precious Soul - Part 2", 75, 31742)); // Virgilio
		NOBLESS_QUESTS.put(246, new NoblessQuestInfo(246, "246_PossessorOfAPreciousSoul_3", "Possessor of a Precious Soul - Part 3", 75, 31741)); // Caradine
		NOBLESS_QUESTS.put(247, new NoblessQuestInfo(247, "247_PossessorOfAPreciousSoul_4", "Possessor of a Precious Soul - Part 4", 75, 31741)); // Caradine / Lady of the Lake

		// ==================== 31 SAGAS DE 3ª CLASSE (Quests 70 a 100) ====================
		registerSaga(70, "70_SagaOfThePhoenixKnight", "Saga of the Phoenix Knight", 5, 90, "Phoenix Knight");
		registerSaga(71, "71_SagaOfEvasTemplar", "Saga of Eva's Templar", 20, 99, "Eva's Templar");
		registerSaga(72, "72_SagaOfTheSwordMuse", "Saga of the Sword Muse", 21, 100, "Sword Muse");
		registerSaga(73, "73_SagaOfTheDuelist", "Saga of the Duelist", 2, 88, "Duelist");
		registerSaga(74, "74_SagaOfTheDreadnoughts", "Saga of the Dreadnought", 3, 89, "Dreadnought");
		registerSaga(75, "75_SagaOfTheTitan", "Saga of the Titan", 46, 113, "Titan");
		registerSaga(76, "76_SagaOfTheGrandKhavatari", "Saga of the Grand Khavatari", 48, 114, "Grand Khavatari");
		registerSaga(77, "77_SagaOfTheDominator", "Saga of the Dominator", 51, 115, "Dominator");
		registerSaga(78, "78_SagaOfTheDoomcryer", "Saga of the Doomcryer", 52, 116, "Doomcryer");
		registerSaga(79, "79_SagaOfTheAdventurer", "Saga of the Adventurer", 8, 93, "Adventurer");
		registerSaga(80, "80_SagaOfTheWindRider", "Saga of the Wind Rider", 23, 101, "Wind Rider");
		registerSaga(81, "81_SagaOfTheGhostHunter", "Saga of the Ghost Hunter", 36, 108, "Ghost Hunter");
		registerSaga(82, "82_SagaOfTheSagittarius", "Saga of the Sagittarius", 9, 92, "Sagittarius");
		registerSaga(83, "83_SagaOfTheMoonlightSentinel", "Saga of the Moonlight Sentinel", 24, 102, "Moonlight Sentinel");
		registerSaga(84, "84_SagaOfTheGhostSentinel", "Saga of the Ghost Sentinel", 37, 109, "Ghost Sentinel");
		registerSaga(85, "85_SagaOfTheCardinal", "Saga of the Cardinal", 16, 97, "Cardinal");
		registerSaga(86, "86_SagaOfTheHierophant", "Saga of the Hierophant", 17, 98, "Hierophant");
		registerSaga(87, "87_SagaOfEvasSaint", "Saga of Eva's Saint", 30, 105, "Eva's Saint");
		registerSaga(88, "88_SagaOfTheArchmage", "Saga of the Archmage", 12, 94, "Archmage");
		registerSaga(89, "89_SagaOfTheMysticMuse", "Saga of the Mystic Muse", 27, 103, "Mystic Muse");
		registerSaga(90, "90_SagaOfTheStormScreamer", "Saga of the Storm Screamer", 40, 110, "Storm Screamer");
		registerSaga(91, "91_SagaOfTheArcanaLord", "Saga of the Arcana Lord", 14, 96, "Arcana Lord");
		registerSaga(92, "92_SagaOfTheElementalMaster", "Saga of the Elemental Master", 28, 104, "Elemental Master");
		registerSaga(93, "93_SagaOfTheSpectralMaster", "Saga of the Spectral Master", 41, 111, "Spectral Master");
		registerSaga(94, "94_SagaOfTheSoultaker", "Saga of the Soultaker", 13, 95, "Soultaker");
		registerSaga(95, "95_SagaOfTheHellKnight", "Saga of the Hell Knight", 6, 91, "Hell Knight");
		registerSaga(96, "96_SagaOfTheSpectralDancer", "Saga of the Spectral Dancer", 34, 107, "Spectral Dancer");
		registerSaga(97, "97_SagaOfTheShillienTemplar", "Saga of the Shillien Templar", 33, 106, "Shillien Templar");
		registerSaga(98, "98_SagaOfTheShillienSaint", "Saga of the Shillien Saint", 43, 112, "Shillien Saint");
		registerSaga(99, "99_SagaOfTheFortuneSeeker", "Saga of the Fortune Seeker", 55, 117, "Fortune Seeker");
		registerSaga(100, "100_SagaOfTheMaestro", "Saga of the Maestro", 57, 118, "Maestro");
	}

	private static void registerSaga(int qId, String name, String title, int second, int third, String thirdName) {
		SagaInfo saga = new SagaInfo(qId, name, title, second, third, thirdName);
		SAGAS_BY_QUEST_ID.put(qId, saga);
		SAGAS_BY_THIRD_CLASS.put(third, saga);
	}

	public static Map<Integer, SagaInfo> getAllSagas() {
		return Collections.unmodifiableMap(SAGAS_BY_QUEST_ID);
	}

	public static Map<Integer, NoblessQuestInfo> getAllNoblessQuests() {
		return Collections.unmodifiableMap(NOBLESS_QUESTS);
	}

	public static Optional<SagaInfo> getSagaByQuestId(int questId) {
		return Optional.ofNullable(SAGAS_BY_QUEST_ID.get(questId));
	}

	public static Optional<SagaInfo> getSagaByThirdClass(int thirdClassId) {
		return Optional.ofNullable(SAGAS_BY_THIRD_CLASS.get(thirdClassId));
	}
}
