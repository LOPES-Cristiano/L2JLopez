package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 214: Trial of the Scholar
 * 1º Passo de 2ª Classe para Sorcerer, Spellsinger, Spellhowler, Necromancer, Warlock, Elemental Summoner, Phantom Summoner.
 */
@Component
public class Quest214TrialOfScholar extends Quest {

	public static final int QUEST_ID = 214;
	public static final String QUEST_NAME = "214_TrialOfScholar";

	// NPCs
	public static final int MIRIEN = 30461;
	public static final int SYLVAIN = 30070;
	public static final int LUCAS = 30071;
	public static final int VALKON = 30103;
	public static final int DIETER = 30111;
	public static final int JUREK = 30115;
	public static final int EDROC = 30230;
	public static final int RAUT = 30316;
	public static final int POITAN = 30458;
	public static final int MARIA = 30608;
	public static final int CRETA = 30609;
	public static final int CRONOS = 30610;
	public static final int TRIFF = 30611;
	public static final int CASIAN = 30612;

	// Monstros
	public static final int MEDUSA = 20158;
	public static final int GHOUL = 20201;
	public static final int SHACKLE = 20235;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int FETTERED_SOUL = 20552;
	public static final int GRANDIS = 20554;
	public static final int ENCHANTED_GARGOYLE = 20567;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;
	public static final int MONSTER_EYE_DESTROYER = 20068;

	// Itens
	public static final int MARK_OF_SCHOLAR = 2674;
	public static final int MIRIENS_SIGIL1 = 2675;
	public static final int MIRIENS_SIGIL2 = 2676;
	public static final int MIRIENS_SIGIL3 = 2677;
	public static final int MIRIENS_INSTRUCTION = 2678;
	public static final int MARIAS_LETTER1 = 2679;
	public static final int MARIAS_LETTER2 = 2680;
	public static final int LUKAS_LETTER = 2681;
	public static final int LUCILLAS_HANDBAG = 2682;
	public static final int CRETAS_LETTER1 = 2683;
	public static final int CRETAS_PAINTING1 = 2684;
	public static final int CRETAS_PAINTING2 = 2685;
	public static final int CRETAS_PAINTING3 = 2686;
	public static final int BROWN_SCROLL_SCRAP = 2687;
	public static final int CRYSTAL_OF_PURITY1 = 2688;
	public static final int HIGHPRIESTS_SIGIL = 2689;
	public static final int GMAGISTERS_SIGIL = 2690;
	public static final int CRONOS_SIGIL = 2691;
	public static final int SYLVAINS_LETTER = 2692;
	public static final int SYMBOL_OF_SYLVAIN = 2693;
	public static final int JUREKS_LIST = 2694;
	public static final int MEYEDESTROYERS_SKIN = 2695;
	public static final int SHAMANS_NECKLACE = 2696;
	public static final int SHACKLES_SCALP = 2697;
	public static final int SYMBOL_OF_JUREK = 2698;
	public static final int CRONOS_LETTER = 2699;
	public static final int DIETERS_KEY = 2700;
	public static final int CRETAS_LETTER2 = 2701;
	public static final int DIETERS_LETTER = 2702;
	public static final int DIETERS_DIARY = 2703;
	public static final int RAUTS_LETTER_ENVELOPE = 2704;
	public static final int TRIFFS_RING = 2705;
	public static final int SCRIPTURE_CHAPTER_1 = 2706;
	public static final int SCRIPTURE_CHAPTER_2 = 2707;
	public static final int SCRIPTURE_CHAPTER_3 = 2708;
	public static final int SCRIPTURE_CHAPTER_4 = 2709;
	public static final int VALKONS_REQUEST = 2710;
	public static final int POITANS_NOTES = 2711;
	public static final int STRONG_LIQUOR = 2713;
	public static final int CRYSTAL_OF_PURITY2 = 2714;
	public static final int CASIANS_LIST = 2715;
	public static final int GHOULS_SKIN = 2716;
	public static final int MEDUSAS_BLOOD = 2717;
	public static final int FETTEREDSOULS_ICHOR = 2718;
	public static final int ENCHT_GARGOYLES_NAIL = 2719;
	public static final int SYMBOL_OF_CRONOS = 2720;

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 26, 39); // Wizard, Elven Wizard, Dark Wizard

	@Autowired
	public Quest214TrialOfScholar(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Scholar");

		addStartNpc(MIRIEN);
		addTalkId(MIRIEN, SYLVAIN, LUCAS, VALKON, DIETER, JUREK, EDROC, RAUT, POITAN, MARIA, CRETA, CRONOS, TRIFF, CASIAN);

		addKillId(MEDUSA, GHOUL, SHACKLE, BREKA_ORC_SHAMAN, FETTERED_SOUL, GRANDIS,
				ENCHANTED_GARGOYLE, LETO_LIZARDMAN_WARRIOR, MONSTER_EYE_DESTROYER);

		registerQuestItems(MIRIENS_SIGIL1, MIRIENS_SIGIL2, MIRIENS_SIGIL3, MIRIENS_INSTRUCTION,
				MARIAS_LETTER1, MARIAS_LETTER2, LUKAS_LETTER, LUCILLAS_HANDBAG, CRETAS_LETTER1,
				CRETAS_PAINTING1, CRETAS_PAINTING2, CRETAS_PAINTING3, BROWN_SCROLL_SCRAP,
				CRYSTAL_OF_PURITY1, HIGHPRIESTS_SIGIL, GMAGISTERS_SIGIL, CRONOS_SIGIL,
				SYLVAINS_LETTER, SYMBOL_OF_SYLVAIN, JUREKS_LIST, MEYEDESTROYERS_SKIN,
				SHAMANS_NECKLACE, SHACKLES_SCALP, SYMBOL_OF_JUREK, CRONOS_LETTER, DIETERS_KEY,
				CRETAS_LETTER2, DIETERS_LETTER, DIETERS_DIARY, RAUTS_LETTER_ENVELOPE, TRIFFS_RING,
				SCRIPTURE_CHAPTER_1, SCRIPTURE_CHAPTER_2, SCRIPTURE_CHAPTER_3, SCRIPTURE_CHAPTER_4,
				VALKONS_REQUEST, POITANS_NOTES, STRONG_LIQUOR, CRYSTAL_OF_PURITY2, CASIANS_LIST,
				GHOULS_SKIN, MEDUSAS_BLOOD, FETTEREDSOULS_ICHOR, ENCHT_GARGOYLES_NAIL, SYMBOL_OF_CRONOS);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30461-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(MIRIENS_SIGIL1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30461-04.htm";
		} else if ("30070_1".equalsIgnoreCase(event)) {
			qs.giveItems(HIGHPRIESTS_SIGIL, 1);
			qs.giveItems(SYLVAINS_LETTER, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30070-02.htm";
		} else if ("30608_1".equalsIgnoreCase(event)) {
			qs.takeItems(SYLVAINS_LETTER, -1);
			qs.giveItems(MARIAS_LETTER1, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30608-02.htm";
		} else if ("30115_2".equalsIgnoreCase(event)) {
			qs.giveItems(JUREKS_LIST, 1);
			qs.giveItems(GMAGISTERS_SIGIL, 1);
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30115-03.htm";
		} else if ("30461_1".equalsIgnoreCase(event)) {
			qs.takeItems(SYMBOL_OF_JUREK, -1);
			qs.takeItems(MIRIENS_SIGIL2, -1);
			qs.giveItems(MIRIENS_SIGIL3, 1);
			qs.setCond(19);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30461-10.htm";
		} else if ("30610_1".equalsIgnoreCase(event)) {
			qs.giveItems(CRONOS_SIGIL, 1);
			qs.giveItems(CRONOS_LETTER, 1);
			qs.setCond(20);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30610-02.htm";
		} else if ("30461-08.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SYMBOL_OF_CRONOS, -1);
			qs.takeItems(MIRIENS_SIGIL3, -1);
			qs.giveItems(MARK_OF_SCHOLAR, 1);
			qs.addExpAndSp(80265, 12100);
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitCurrentQuest(false);
			return event;
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == MIRIEN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 35 && VALID_CLASSES.contains(player.classId())) {
					return "30461-01.htm";
				} else {
					return "30461-02.htm";
				}
			} else if (cond == 1) {
				return "30461-05.htm";
			} else if (cond == 15 && qs.hasQuestItems(SYMBOL_OF_SYLVAIN)) {
				qs.takeItems(SYMBOL_OF_SYLVAIN, -1);
				qs.takeItems(MIRIENS_SIGIL1, -1);
				qs.giveItems(MIRIENS_SIGIL2, 1);
				qs.setCond(15);
				return "30461-06.htm";
			} else if (cond == 18 && qs.hasQuestItems(SYMBOL_OF_JUREK)) {
				return "30461-07.htm";
			} else if (cond >= 19 && cond < 30) {
				return "30461-11.htm";
			} else if (cond == 30 && qs.hasQuestItems(SYMBOL_OF_CRONOS)) {
				return "30461-08.htm";
			}
		} else if (npcId == SYLVAIN) {
			if (cond == 1) {
				return "30070-01.htm";
			} else if (cond >= 2 && cond < 14) {
				return "30070-03.htm";
			} else if (cond == 14 && qs.hasQuestItems(CRYSTAL_OF_PURITY1)) {
				qs.takeItems(CRYSTAL_OF_PURITY1, -1);
				qs.takeItems(HIGHPRIESTS_SIGIL, -1);
				qs.giveItems(SYMBOL_OF_SYLVAIN, 1);
				qs.setCond(15);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30070-04.htm";
			}
		} else if (npcId == JUREK) {
			if (cond == 15 && qs.hasQuestItems(MIRIENS_SIGIL2)) {
				return "30115-01.htm";
			} else if (cond == 17 && qs.getQuestItemsCount(MEYEDESTROYERS_SKIN) >= 5
					&& qs.getQuestItemsCount(SHAMANS_NECKLACE) >= 5
					&& qs.getQuestItemsCount(SHACKLES_SCALP) >= 2) {
				qs.takeItems(MEYEDESTROYERS_SKIN, -1);
				qs.takeItems(SHAMANS_NECKLACE, -1);
				qs.takeItems(SHACKLES_SCALP, -1);
				qs.takeItems(JUREKS_LIST, -1);
				qs.takeItems(GMAGISTERS_SIGIL, -1);
				qs.giveItems(SYMBOL_OF_JUREK, 1);
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30115-04.htm";
			}
		} else if (npcId == CRONOS) {
			if (cond == 19 && qs.hasQuestItems(MIRIENS_SIGIL3)) {
				return "30610-01.htm";
			} else if (cond == 29) {
				qs.takeItems(CRONOS_SIGIL, -1);
				qs.giveItems(SYMBOL_OF_CRONOS, 1);
				qs.setCond(30);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30610-10.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 16) {
			if (npcId == MONSTER_EYE_DESTROYER && qs.getQuestItemsCount(MEYEDESTROYERS_SKIN) < 5) {
				qs.giveItems(MEYEDESTROYERS_SKIN, 1);
			} else if (npcId == BREKA_ORC_SHAMAN && qs.getQuestItemsCount(SHAMANS_NECKLACE) < 5) {
				qs.giveItems(SHAMANS_NECKLACE, 1);
			} else if (npcId == SHACKLE && qs.getQuestItemsCount(SHACKLES_SCALP) < 2) {
				qs.giveItems(SHACKLES_SCALP, 1);
			}

			if (qs.getQuestItemsCount(MEYEDESTROYERS_SKIN) >= 5
					&& qs.getQuestItemsCount(SHAMANS_NECKLACE) >= 5
					&& qs.getQuestItemsCount(SHACKLES_SCALP) >= 2) {
				qs.setCond(17);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
