package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico e Validador das 23 Quests de 2ª Mudança de Classe (211–233) no Lineage II Interlude.
 * Mapeia as 3 provas necessárias para cada uma das 31 classes de 2ª profissão:
 * 1. Trial (nível 35+)
 * 2. Testimony (nível 37+)
 * 3. Test (nível 39+)
 */
public final class SecondClassQuestCatalog {

	public enum QuestCategory {
		TRIAL,
		TESTIMONY,
		TEST
	}

	public record SecondClassQuestInfo(
			int questId,
			String questName,
			String title,
			QuestCategory category,
			int minLevel,
			int startNpcId,
			int proofItemId
	) {}

	public record SecondClassRequirement(
			int targetClassId,
			String targetClassName,
			int parentClassId,
			int trialQuestId,
			int testimonyQuestId,
			int testQuestId,
			int trialProofItem,
			int testimonyProofItem,
			int testProofItem
	) {
		public List<Integer> requiredProofItems() {
			return List.of(trialProofItem, testimonyProofItem, testProofItem);
		}
	}

	private static final Map<Integer, SecondClassQuestInfo> QUESTS_BY_ID = new LinkedHashMap<>();
	private static final Map<Integer, SecondClassRequirement> REQUIREMENTS_BY_CLASS = new LinkedHashMap<>();

	static {
		// ==================== TRIALS (35+) ====================
		registerQuest(211, "211_TrialOfChallenger", "Trial of the Challenger", QuestCategory.TRIAL, 35, 30644, 2627);
		registerQuest(212, "212_TrialOfDuty", "Trial of Duty", QuestCategory.TRIAL, 35, 30109, 2633);
		registerQuest(213, "213_TrialOfSeeker", "Trial of the Seeker", QuestCategory.TRIAL, 35, 30064, 2673);
		registerQuest(214, "214_TrialOfScholar", "Trial of the Scholar", QuestCategory.TRIAL, 35, 30607, 2674);
		registerQuest(215, "215_TrialOfPilgrim", "Trial of the Pilgrim", QuestCategory.TRIAL, 35, 30648, 2721);
		registerQuest(216, "216_TrialOfGuildsman", "Trial of the Guildsman", QuestCategory.TRIAL, 35, 30103, 3119);

		// ==================== TESTIMONIES (37+) ====================
		registerQuest(217, "217_TestimonyOfTrust", "Testimony of Trust", QuestCategory.TESTIMONY, 37, 30191, 2734);
		registerQuest(218, "218_TestimonyOfLife", "Testimony of Life", QuestCategory.TESTIMONY, 37, 30371, 3140);
		registerQuest(219, "219_TestimonyOfFate", "Testimony of Fate", QuestCategory.TESTIMONY, 37, 30419, 3172);
		registerQuest(220, "220_TestimonyOfGlory", "Testimony of Glory", QuestCategory.TESTIMONY, 37, 30514, 3203);
		registerQuest(221, "221_TestimonyOfProsperity", "Testimony of Prosperity", QuestCategory.TESTIMONY, 37, 30531, 3238);

		// ==================== TESTS (39+) ====================
		registerQuest(222, "222_TestOfDuelist", "Test of the Duelist", QuestCategory.TEST, 39, 30623, 2762);
		registerQuest(223, "223_TestOfChampion", "Test of the Champion", QuestCategory.TEST, 39, 30624, 3276);
		registerQuest(224, "224_TestOfSagittarius", "Test of the Sagittarius", QuestCategory.TEST, 39, 30702, 3293);
		registerQuest(225, "225_TestOfSearcher", "Test of the Searcher", QuestCategory.TEST, 39, 30690, 2809);
		registerQuest(226, "226_TestOfHealer", "Test of the Healer", QuestCategory.TEST, 39, 30473, 2820);
		registerQuest(227, "227_TestOfReformer", "Test of the Reformer", QuestCategory.TEST, 39, 30666, 2821);
		registerQuest(228, "228_TestOfMagus", "Test of the Magus", QuestCategory.TEST, 39, 30629, 2840);
		registerQuest(229, "229_TestOfWitchcraft", "Test of Witchcraft", QuestCategory.TEST, 39, 30630, 3307);
		registerQuest(230, "230_TestOfSummoner", "Test of the Summoner", QuestCategory.TEST, 39, 30634, 3334);
		registerQuest(231, "231_TestOfMaestro", "Test of the Maestro", QuestCategory.TEST, 39, 30531, 2867);
		registerQuest(232, "232_TestOfLord", "Test of the Lord", QuestCategory.TEST, 39, 30565, 3390);
		registerQuest(233, "233_TestOfWarspirit", "Test of the Warspirit", QuestCategory.TEST, 39, 30510, 2879);

		// ==================== CLASSES DE 2ª PROFISSÃO ====================
		// Humanos
		registerReq(2, "Gladiator", 1, 211, 217, 222);
		registerReq(3, "Warlord", 1, 211, 217, 223);
		registerReq(5, "Paladin", 4, 212, 217, 226);
		registerReq(6, "Dark Avenger", 4, 212, 217, 229);
		registerReq(8, "Treasure Hunter", 7, 213, 217, 225);
		registerReq(9, "Hawkeye", 7, 213, 217, 224);
		registerReq(12, "Sorcerer", 11, 214, 217, 228);
		registerReq(13, "Necromancer", 11, 214, 217, 229);
		registerReq(14, "Warlock", 11, 214, 217, 230);
		registerReq(16, "Bishop", 15, 215, 217, 226);
		registerReq(17, "Prophet", 15, 215, 217, 227);

		// Elfos
		registerReq(20, "Temple Knight", 19, 212, 218, 226);
		registerReq(21, "Swordsinger", 19, 211, 218, 222);
		registerReq(23, "Plains Walker", 22, 213, 218, 225);
		registerReq(24, "Silver Ranger", 22, 213, 218, 224);
		registerReq(27, "Spellsinger", 26, 214, 218, 228);
		registerReq(28, "Elemental Summoner", 26, 214, 218, 230);
		registerReq(30, "Elven Elder", 29, 215, 218, 227);

		// Dark Elves
		registerReq(33, "Shillien Knight", 32, 212, 219, 229);
		registerReq(34, "Blade Dancer", 32, 211, 219, 222);
		registerReq(36, "Abyss Walker", 35, 213, 219, 225);
		registerReq(37, "Phantom Ranger", 35, 213, 219, 224);
		registerReq(40, "Spellhowler", 39, 214, 219, 228);
		registerReq(41, "Phantom Summoner", 39, 214, 219, 230);
		registerReq(43, "Shillien Elder", 42, 215, 219, 227);

		// Orcs
		registerReq(46, "Destroyer", 45, 211, 220, 223);
		registerReq(48, "Tyrant", 47, 211, 220, 222);
		registerReq(51, "Overlord", 50, 215, 220, 232);
		registerReq(52, "Warcryer", 50, 215, 220, 233);

		// Anões
		registerReq(55, "Bounty Hunter", 54, 216, 221, 225);
		registerReq(57, "Warsmith", 56, 216, 221, 231);
	}

	private static void registerQuest(int id, String name, String title, QuestCategory cat, int minLvl, int npc, int proof) {
		QUESTS_BY_ID.put(id, new SecondClassQuestInfo(id, name, title, cat, minLvl, npc, proof));
	}

	private static void registerReq(int target, String name, int parent, int trial, int testi, int test) {
		int trialProof = QUESTS_BY_ID.get(trial).proofItemId();
		int testiProof = QUESTS_BY_ID.get(testi).proofItemId();
		int testProof = QUESTS_BY_ID.get(test).proofItemId();

		SecondClassRequirement req = new SecondClassRequirement(target, name, parent, trial, testi, test,
				trialProof, testiProof, testProof);
		REQUIREMENTS_BY_CLASS.put(target, req);
	}

	public static Map<Integer, SecondClassQuestInfo> getAllQuests() {
		return Collections.unmodifiableMap(QUESTS_BY_ID);
	}

	public static Map<Integer, SecondClassRequirement> getAllRequirements() {
		return Collections.unmodifiableMap(REQUIREMENTS_BY_CLASS);
	}

	public static Optional<SecondClassQuestInfo> getQuest(int questId) {
		return Optional.ofNullable(QUESTS_BY_ID.get(questId));
	}

	public static Optional<SecondClassRequirement> getRequirement(int targetClassId) {
		return Optional.ofNullable(REQUIREMENTS_BY_CLASS.get(targetClassId));
	}

	public static boolean isSecondClassProofItem(int itemId) {
		return QUESTS_BY_ID.values().stream().anyMatch(q -> q.proofItemId() == itemId);
	}
}
