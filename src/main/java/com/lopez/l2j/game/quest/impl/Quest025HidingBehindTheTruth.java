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
 * Quest 025: Hiding Behind the Truth
 * Benedict e Agripel em Rune, Maid of Lidia e Hardin. Recompensa: Jóias B-grade (Earring of Blessing, Necklace of Grace).
 */
@Component
public class Quest025HidingBehindTheTruth extends Quest {

	public static final int QUEST_ID = 25;
	public static final String QUEST_NAME = "025_HidingBehindTheTruth";

	public static final int BENEDICT = 31349;
	public static final int AGRIPEL = 31348;
	public static final int HARDIN = 31522;
	public static final int MAID = 31532;

	public static final int SUSPICIOUS_TOTEM = 7151;
	public static final int CONTRACT = 7158;
	public static final int EARRING_OF_BLESSING = 874;
	public static final int NECKLACE_OF_GRACE = 905;

	public static final int TRIOLS_PAWN = 27218;

	@Autowired
	public Quest025HidingBehindTheTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hiding Behind the Truth");
		addStartNpc(BENEDICT);
		addTalkId(BENEDICT, AGRIPEL, HARDIN, MAID);
		addKillId(TRIOLS_PAWN);
		registerQuestItems(CONTRACT);
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
			return "falsepriest_benedict_q0025_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BENEDICT) {
			if (cond == 0) {
				if (pc.level() >= 66) return "falsepriest_benedict_q0025_01.htm";
				qs.exitQuest(true);
				return "falsepriest_benedict_q0025_02.htm";
			} else if (cond == 1) {
				return "falsepriest_benedict_q0025_04.htm";
			}
		} else if (npcId == AGRIPEL) {
			if (cond == 1) {
				qs.giveItems(CONTRACT, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "falsepriest_agripel_q0025_02.htm";
			} else if (cond == 2) {
				qs.takeItems(CONTRACT, -1);
				qs.giveItems(EARRING_OF_BLESSING, 1);
				qs.giveItems(NECKLACE_OF_GRACE, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "falsepriest_agripel_q0025_08.htm";
			}
		}
		return "noquest";
	}
}
