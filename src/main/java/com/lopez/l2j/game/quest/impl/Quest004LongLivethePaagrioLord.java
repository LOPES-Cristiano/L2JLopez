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
	public static final int KUNAI = 30559;
	public static final int USKA = 30560;
	public static final int GROOKIN = 30562;
	public static final int VARKEES = 30566;
	public static final int TATARU = 30585;
	public static final int GANTAKI = 30587;

	public static final int[] CHIEFS = {30559, 30560, 30562, 30566, 30585, 30587};
	public static final int[] TRIBUTES = {1541, 1542, 1543, 1544, 1545, 1546};
	public static final int CLUB = 4;

	@Autowired
	public Quest004LongLivethePaagrioLord(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Long Live the Pa'agrio Lord");
		addStartNpc(NAKUSIN);
		addTalkId(NAKUSIN, KUNAI, USKA, GROOKIN, VARKEES, TATARU, GANTAKI);
		registerQuestItems(1541, 1542, 1543, 1544, 1545, 1546);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30578-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30578-03.htm".equalsIgnoreCase(event) ? "30578-03.htm" : "centurion_nakusin_q0004_03.htm";
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
				if (pc.race() != 3) {
					qs.exitQuest(true);
					return "centurion_nakusin_q0004_01.htm";
				}
				if (pc.level() >= 2) {
					return "centurion_nakusin_q0004_02.htm";
				}
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
		} else if (npcId == TATARU) {
			return handleGift(qs, 1542, "30585-01.htm", "30585-02.htm");
		} else if (npcId == VARKEES) {
			return handleGift(qs, 1541, "30566-01.htm", "30566-02.htm");
		} else if (npcId == GROOKIN) {
			return handleGift(qs, 1543, "30562-01.htm", "30562-02.htm");
		} else if (npcId == USKA) {
			return handleGift(qs, 1544, "30560-01.htm", "30560-02.htm");
		} else if (npcId == KUNAI) {
			return handleGift(qs, 1545, "30559-01.htm", "30559-02.htm");
		} else if (npcId == GANTAKI) {
			return handleGift(qs, 1546, "30587-01.htm", "30587-02.htm");
		}
		return "noquest";
	}

	private String handleGift(QuestState qs, int itemId, String firstHtml, String repeatHtml) {
		if (qs.getCond() == 1 && !qs.hasQuestItems(itemId)) {
			qs.giveItems(itemId, 1);
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
			return firstHtml;
		}
		return repeatHtml;
	}
}
