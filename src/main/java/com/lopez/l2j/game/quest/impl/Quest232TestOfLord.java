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
 * Quest 232: Test of the Lord
 * 3º Passo de 2ª Classe para Overlord.
 */
@Component
public class Quest232TestOfLord extends Quest {

	public static final int QUEST_ID = 232;
	public static final String QUEST_NAME = "232_TestOfLord";

	// NPCs
	public static final int KAKAI = 30565;
	public static final int MARTANKUS = 30649;

	// Itens
	public static final int MARK_OF_LORD = 3390;
	public static final int ORDEAL_NECKLACE = 3391;
	public static final int BEAR_FANG_NECKLACE = 3412;
	public static final int MARTANKUS_CHARM = 3413;
	public static final int IMMORTAL_FLAME = 3416;

	private static final Set<Integer> VALID_CLASSES = Set.of(50); // Orc Shaman

	@Autowired
	public Quest232TestOfLord(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Lord");

		addStartNpc(KAKAI);
		addTalkId(KAKAI, MARTANKUS);

		registerQuestItems(ORDEAL_NECKLACE, BEAR_FANG_NECKLACE, MARTANKUS_CHARM, IMMORTAL_FLAME);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30565-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ORDEAL_NECKLACE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30565-05.htm";
		} else if ("30565_2".equalsIgnoreCase(event) || "30565-11.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(IMMORTAL_FLAME)) {
				qs.takeItems(IMMORTAL_FLAME, -1);
				qs.giveItems(MARK_OF_LORD, 1);
				qs.addExpAndSp(115000, 12000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30565-11.htm";
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

		if (npcId == KAKAI) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30565-01.htm";
				} else {
					return "30565-02.htm";
				}
			} else if (cond == 1) {
				return "30565-06.htm";
			} else if (cond >= 2 && qs.hasQuestItems(IMMORTAL_FLAME)) {
				return "30565-10.htm";
			}
		} else if (npcId == MARTANKUS) {
			if (cond == 1) {
				qs.giveItems(IMMORTAL_FLAME, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30649-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
