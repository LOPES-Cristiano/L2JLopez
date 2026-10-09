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
 * Quest 233: Test of the Warspirit
 * 3º Passo de 2ª Classe para Warcryer.
 */
@Component
public class Quest233TestOfWarspirit extends Quest {

	public static final int QUEST_ID = 233;
	public static final String QUEST_NAME = "233_TestOfWarspirit";

	// NPCs
	public static final int SOMAK = 30510;
	public static final int ORIM = 30630;
	public static final int MANAKIA = 30515;
	public static final int PEKIRON = 30682;
	public static final int RACOY = 30507;

	// Itens
	public static final int MARK_OF_WARSPIRIT = 2879;
	public static final int VENDETTA_TOTEM = 2880;
	public static final int WARSPIRIT_TOTEM = 2882;
	public static final int BRAKIS_REMAINS1 = 2887;
	public static final int HERMODTS_REMAINS1 = 2901;
	public static final int KIRUNAS_REMAINS1 = 2910;
	public static final int TONARS_REMAINS1 = 2894;

	private static final Set<Integer> VALID_CLASSES = Set.of(50); // Orc Shaman

	@Autowired
	public Quest233TestOfWarspirit(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Warspirit");

		addStartNpc(SOMAK);
		addTalkId(SOMAK, ORIM, MANAKIA, PEKIRON, RACOY);

		registerQuestItems(VENDETTA_TOTEM, WARSPIRIT_TOTEM, BRAKIS_REMAINS1, HERMODTS_REMAINS1,
				KIRUNAS_REMAINS1, TONARS_REMAINS1);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30510-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30510-05.htm";
		} else if ("30510-09.htm".equalsIgnoreCase(event)) {
			if (hasAllFourRemains(qs)) {
				qs.takeItems(BRAKIS_REMAINS1, -1);
				qs.takeItems(HERMODTS_REMAINS1, -1);
				qs.takeItems(KIRUNAS_REMAINS1, -1);
				qs.takeItems(TONARS_REMAINS1, -1);
				qs.giveItems(MARK_OF_WARSPIRIT, 1);
				qs.addExpAndSp(118304, 26250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private boolean hasAllFourRemains(QuestState qs) {
		return qs.hasQuestItems(BRAKIS_REMAINS1) && qs.hasQuestItems(HERMODTS_REMAINS1)
				&& qs.hasQuestItems(KIRUNAS_REMAINS1) && qs.hasQuestItems(TONARS_REMAINS1);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == SOMAK) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30510-03.htm";
				} else {
					return "30510-01.htm";
				}
			} else if (cond == 1) {
				if (hasAllFourRemains(qs)) {
					return "30510-08.htm";
				}
				return "30510-06.htm";
			}
		} else if (npcId == ORIM) {
			if (cond == 1 && !qs.hasQuestItems(BRAKIS_REMAINS1)) {
				qs.giveItems(BRAKIS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30630-05.htm";
			}
		} else if (npcId == MANAKIA) {
			if (cond == 1 && !qs.hasQuestItems(HERMODTS_REMAINS1)) {
				qs.giveItems(HERMODTS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30515-05.htm";
			}
		} else if (npcId == PEKIRON) {
			if (cond == 1 && !qs.hasQuestItems(TONARS_REMAINS1)) {
				qs.giveItems(TONARS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30682-05.htm";
			}
		} else if (npcId == RACOY) {
			if (cond == 1 && !qs.hasQuestItems(KIRUNAS_REMAINS1)) {
				qs.giveItems(KIRUNAS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30507-05.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
