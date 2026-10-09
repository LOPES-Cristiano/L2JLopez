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
 * Quest 004: Long Live the Pa'agrio Lord
 * Nakusin na Orc Village. Obtenção de 6 tributos dos chefes das tribos orc.
 */
@Component
public class Quest004LongLivethePaagrioLord extends Quest {

	public static final int QUEST_ID = 4;
	public static final String QUEST_NAME = "004_LongLivethePaagrioLord";

	public static final int NAKUSIN = 30578;
	public static final int[] CHIEFS = {30559, 30560, 30562, 30566, 30585, 30587};
	public static final int[] TRIBUTES = {1541, 1542, 1543, 1544, 1545, 1546};
	public static final int CLUB = 11;

	@Autowired
	public Quest004LongLivethePaagrioLord(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Long Live the Pa'agrio Lord");
		addStartNpc(NAKUSIN);
		addTalkId(NAKUSIN);
		for (int c : CHIEFS) addTalkId(c);
		registerQuestItems(TRIBUTES);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "centurion_nakusin_q0004_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == NAKUSIN) {
			if (cond == 0) {
				if (pc.level() >= 2 && pc.race() == 3) return "centurion_nakusin_q0004_02.htm";
				qs.exitQuest(true);
				return "centurion_nakusin_q0004_01.htm";
			} else if (cond == 1) {
				return "centurion_nakusin_q0004_04.htm";
			} else if (cond == 2) {
				for (int t : TRIBUTES) {
					qs.takeItems(t, -1);
				}
				qs.giveItems(CLUB, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "centurion_nakusin_q0004_06.htm";
			}
		} else {
			for (int i = 0; i < CHIEFS.length; i++) {
				if (npcId == CHIEFS[i]) {
					if (cond == 1 && !qs.hasQuestItems(TRIBUTES[i])) {
						qs.giveItems(TRIBUTES[i], 1);
						boolean all = true;
						for (int t : TRIBUTES) {
							if (!qs.hasQuestItems(t)) {
								all = false;
								break;
							}
						}
						if (all) {
							qs.setCond(2);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
						return npcId + "-01.htm";
					}
					return npcId + "-02.htm";
				}
			}
		}
		return "noquest";
	}
}
