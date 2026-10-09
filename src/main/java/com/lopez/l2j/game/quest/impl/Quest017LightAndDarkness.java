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
 * Quest 017: Light and Darkness
 * Hierarch em Forest of the Dead e os 4 Altares Abençoados.
 */
@Component
public class Quest017LightAndDarkness extends Quest {

	public static final int QUEST_ID = 17;
	public static final String QUEST_NAME = "017_LightAndDarkness";

	public static final int HIERARCH = 31517;
	public static final int[] ALTARS = {31508, 31509, 31510, 31511};
	public static final int BLOOD_OF_SAINT = 7168;

	@Autowired
	public Quest017LightAndDarkness(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Light and Darkness");
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH);
		for (int a : ALTARS) addTalkId(a);
		registerQuestItems(BLOOD_OF_SAINT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BLOOD_OF_SAINT, 4);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "dark_presbyter_q0017_04.htm";
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
				if (pc.level() >= 61) return "dark_presbyter_q0017_01.htm";
				qs.exitQuest(true);
				return "dark_presbyter_q0017_03.htm";
			} else if (cond >= 1 && cond <= 4) {
				return "dark_presbyter_q0017_05.htm";
			} else if (cond == 5) {
				qs.takeItems(BLOOD_OF_SAINT, -1);
				qs.giveItems(57, 69140);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "dark_presbyter_q0017_06.htm";
			}
		} else {
			for (int i = 0; i < ALTARS.length; i++) {
				if (npcId == ALTARS[i]) {
					if (cond == (i + 1) && qs.hasQuestItems(BLOOD_OF_SAINT)) {
						qs.takeItems(BLOOD_OF_SAINT, 1);
						qs.setCond(cond + 1);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "blessed_altar" + (i + 1) + "_q0017_02.htm";
					}
					return "blessed_altar" + (i + 1) + "_q0017_03.htm";
				}
			}
		}
		return "noquest";
	}
}
