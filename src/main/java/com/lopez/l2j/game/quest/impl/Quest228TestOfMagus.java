package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 228: Test of the Magus
 * 3º Passo de 2ª Classe para Sorcerer, Spellsinger, Spellhowler.
 */
@Component
public class Quest228TestOfMagus extends Quest {

	public static final int QUEST_ID = 228;
	public static final String QUEST_NAME = "228_TestOfMagus";

	// NPCs
	public static final int RUKAL = 30629;
	public static final int PARINA = 30391;
	public static final int CASIAN = 30612;
	public static final int SALAMANDER = 30411;
	public static final int SYLPH = 30412;
	public static final int UNDINE = 30413;
	public static final int SERPENT = 30409;

	// Itens
	public static final int MARK_OF_MAGUS = 2840;
	public static final int RUKALS_LETTER = 2841;
	public static final int PARINAS_LETTER = 2842;
	public static final int LILAC_CHARM = 2843;
	public static final int SCORE_OF_ELEMENTS = 2847;
	public static final int TONE_OF_WATER = 2856;
	public static final int TONE_OF_FIRE = 2857;
	public static final int TONE_OF_WIND = 2858;
	public static final int TONE_OF_EARTH = 2859;

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 26, 39);

	@Autowired
	public Quest228TestOfMagus(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Magus");

		addStartNpc(RUKAL);
		addTalkId(RUKAL, PARINA, CASIAN, SALAMANDER, SYLPH, UNDINE, SERPENT);

		registerQuestItems(RUKALS_LETTER, PARINAS_LETTER, LILAC_CHARM, SCORE_OF_ELEMENTS,
				TONE_OF_WATER, TONE_OF_FIRE, TONE_OF_WIND, TONE_OF_EARTH);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30629-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(RUKALS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30629-04.htm";
		} else if ("30391_1".equalsIgnoreCase(event)) {
			qs.takeItems(RUKALS_LETTER, -1);
			qs.giveItems(PARINAS_LETTER, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30391-02.htm";
		} else if ("30612_1".equalsIgnoreCase(event)) {
			qs.takeItems(PARINAS_LETTER, -1);
			qs.giveItems(LILAC_CHARM, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30612-02.htm";
		} else if ("30629_2".equalsIgnoreCase(event)) {
			qs.takeItems(LILAC_CHARM, -1);
			qs.giveItems(SCORE_OF_ELEMENTS, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30629-10.htm";
		} else if ("30629-12.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE)
					&& qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
				qs.takeItems(SCORE_OF_ELEMENTS, -1);
				qs.takeItems(TONE_OF_WATER, -1);
				qs.takeItems(TONE_OF_FIRE, -1);
				qs.takeItems(TONE_OF_WIND, -1);
				qs.takeItems(TONE_OF_EARTH, -1);
				qs.giveItems(MARK_OF_MAGUS, 1);
				qs.addExpAndSp(139039, 16000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
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

		if (npcId == RUKAL) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30629-03.htm";
				} else {
					return "30629-01.htm";
				}
			} else if (cond == 1) {
				return "30629-05.htm";
			} else if (cond == 4) {
				return "30629-09.htm";
			} else if (cond == 6 && qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE)
					&& qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
				return "30629-11.htm";
			}
		} else if (npcId == PARINA) {
			if (cond == 1) {
				return "30391-01.htm";
			}
		} else if (npcId == CASIAN) {
			if (cond == 2) {
				return "30612-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
