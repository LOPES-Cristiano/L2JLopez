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
 * Quest 231: Test of the Maestro
 * 3º Passo de 2ª Classe para Warsmith.
 */
@Component
public class Quest231TestOfMaestro extends Quest {

	public static final int QUEST_ID = 231;
	public static final String QUEST_NAME = "231_TestOfMaestro";

	// NPCs
	public static final int LOCKIRIN = 30531;
	public static final int BALANKI = 30533;
	public static final int FILAUR = 30535;
	public static final int ARIN = 30536;

	// Itens
	public static final int MARK_OF_MAESTRO = 2867;
	public static final int RECOMMENDATION_OF_BALANKI = 2864;
	public static final int RECOMMENDATION_OF_FILAUR = 2865;
	public static final int RECOMMENDATION_OF_ARIN = 2866;

	private static final Set<Integer> VALID_CLASSES = Set.of(56); // Artisan

	@Autowired
	public Quest231TestOfMaestro(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Maestro");

		addStartNpc(LOCKIRIN);
		addTalkId(LOCKIRIN, BALANKI, FILAUR, ARIN);

		registerQuestItems(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR, RECOMMENDATION_OF_ARIN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30531-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30531-04.htm";
		} else if ("30531-06.htm".equalsIgnoreCase(event)) {
			if (hasAllThreeRecommendations(qs)) {
				qs.takeItems(RECOMMENDATION_OF_BALANKI, -1);
				qs.takeItems(RECOMMENDATION_OF_FILAUR, -1);
				qs.takeItems(RECOMMENDATION_OF_ARIN, -1);
				qs.giveItems(MARK_OF_MAESTRO, 1);
				qs.addExpAndSp(153053, 21500);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private boolean hasAllThreeRecommendations(QuestState qs) {
		return qs.hasQuestItems(RECOMMENDATION_OF_BALANKI)
				&& qs.hasQuestItems(RECOMMENDATION_OF_FILAUR)
				&& qs.hasQuestItems(RECOMMENDATION_OF_ARIN);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == LOCKIRIN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30531-03.htm";
				} else {
					return "30531-01.htm";
				}
			} else if (cond == 1) {
				if (hasAllThreeRecommendations(qs)) {
					return "30531-05.htm";
				}
				return "30531-04a.htm";
			}
		} else if (npcId == BALANKI) {
			if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_BALANKI)) {
				qs.giveItems(RECOMMENDATION_OF_BALANKI, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30533-03.htm";
			}
		} else if (npcId == FILAUR) {
			if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_FILAUR)) {
				qs.giveItems(RECOMMENDATION_OF_FILAUR, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30535-03.htm";
			}
		} else if (npcId == ARIN) {
			if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_ARIN)) {
				qs.giveItems(RECOMMENDATION_OF_ARIN, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30536-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
