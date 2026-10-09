package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 020: Bring Up With Love
 * Domesticacao de bestas na Beast Farm com Beast Herder Tunatun.
 */
@Component
public class Quest020BringUpWithLove extends Quest {

	public static final int QUEST_ID = 20;
	public static final String QUEST_NAME = "20_BringUpWithLove";

	public static final int TUNATUN = 31537;
	public static final int JEWEL_OF_INNOCENCE = 7185;

	@Autowired
	public Quest020BringUpWithLove(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Bring Up With Love");
		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);
		registerQuestItems(JEWEL_OF_INNOCENCE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getInt("givemelove");
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.set("givemelove", "1");
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "beast_herder_tunatun_q0020_09.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			return "beast_herder_tunatun_q0020_03.htm";
		} else if ("reply_2".equalsIgnoreCase(event)) {
			return "beast_herder_tunatun_q0020_04.htm";
		} else if ("reply_4".equalsIgnoreCase(event)) {
			return "beast_herder_tunatun_q0020_05.htm";
		} else if ("reply_3".equalsIgnoreCase(event)) {
			return "beast_herder_tunatun_q0020_06.htm";
		} else if ("reply_5".equalsIgnoreCase(event)) {
			return "beast_herder_tunatun_q0020_08.htm";
		} else if ("reply_6".equalsIgnoreCase(event) && cond == 1 && qs.hasQuestItems(JEWEL_OF_INNOCENCE)) {
			qs.takeItems(JEWEL_OF_INNOCENCE, -1);
			qs.giveItems(57, 68500); // 68,500 adena
			qs.unset("givemelove");
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "beast_herder_tunatun_q0020_12.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int cond = qs.getInt("givemelove");
		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 65) {
				return "beast_herder_tunatun_q0020_01.htm";
			}
			qs.exitQuest(true);
			return "beast_herder_tunatun_q0020_02.htm";
		} else if (qs.isStarted()) {
			if (cond == 1 && !qs.hasQuestItems(JEWEL_OF_INNOCENCE)) {
				return "beast_herder_tunatun_q0020_10.htm";
			}
			if (cond == 1 && qs.hasQuestItems(JEWEL_OF_INNOCENCE)) {
				return "beast_herder_tunatun_q0020_11.htm";
			}
		}
		return "noquest";
	}
}
