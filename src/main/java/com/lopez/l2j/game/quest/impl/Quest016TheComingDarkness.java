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
 * Quest 016: The Coming Darkness
 * Hierarch em Forest of the Dead e os 5 Altares Malignos.
 */
@Component
public class Quest016TheComingDarkness extends Quest {

	public static final int QUEST_ID = 16;
	public static final String QUEST_NAME = "016_TheComingDarkness";

	public static final int HIERARCH = 31517;
	public static final int[] ALTARS = {31512, 31513, 31514, 31515, 31516};
	public static final int CRYSTAL_OF_SEAL = 7167;

	@Autowired
	public Quest016TheComingDarkness(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Coming Darkness");
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH);
		for (int a : ALTARS) addTalkId(a);
		registerQuestItems(CRYSTAL_OF_SEAL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(CRYSTAL_OF_SEAL, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "dark_presbyter_q0016_04.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == HIERARCH) {
			if (cond == 0) {
				if (pc.level() >= 61) return "dark_presbyter_q0016_01.htm";
				qs.exitQuest(true);
				return "dark_presbyter_q0016_02.htm";
			} else if (cond >= 1 && cond <= 5) {
				return "dark_presbyter_q0016_05.htm";
			} else if (cond == 6) {
				qs.takeItems(CRYSTAL_OF_SEAL, -1);
				qs.giveItems(57, 86515);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "dark_presbyter_q0016_06.htm";
			}
		} else {
			for (int i = 0; i < ALTARS.length; i++) {
				if (npcId == ALTARS[i]) {
					if (cond == (i + 1) && qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
						qs.takeItems(CRYSTAL_OF_SEAL, 1);
						qs.setCond(cond + 1);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "vicious_altar" + (i + 1) + "_q0016_02.htm";
					}
					return "vicious_altar" + (i + 1) + "_q0016_03.htm";
				}
			}
		}
		return "noquest";
	}
}
