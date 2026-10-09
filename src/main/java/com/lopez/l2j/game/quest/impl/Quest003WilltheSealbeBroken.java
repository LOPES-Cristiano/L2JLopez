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
 * Quest 003: Will the Seal be Broken?
 * Talloth na Dark Elven Village. Mobs em Swampland. Recompensa: Scroll of Enchant Armor D-Grade (956).
 */
@Component
public class Quest003WilltheSealbeBroken extends Quest {

	public static final int QUEST_ID = 3;
	public static final String QUEST_NAME = "003_WilltheSealbeBroken";

	public static final int TALLOTH = 30141;

	public static final int ONYX_BEAST_EYE = 1081;
	public static final int TAINT_STONE = 1082;
	public static final int SUCCUBUS_BLOOD = 1083;
	public static final int ENCHANT_ARMOR_D = 956;

	public static final int[] MOBS = {20031, 20041, 20046, 20048, 20052, 20057};

	@Autowired
	public Quest003WilltheSealbeBroken(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Will the Seal be Broken?");
		addStartNpc(TALLOTH);
		addTalkId(TALLOTH);
		addKillId(MOBS);
		registerQuestItems(ONYX_BEAST_EYE, TAINT_STONE, SUCCUBUS_BLOOD);
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
			return "tewndrowell_q0003_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int cond = qs.getCond();
		if (cond == 0) {
			if (pc.level() >= 16 && pc.race() == 2) return "tewndrowell_q0003_02.htm";
			qs.exitQuest(true);
			return "tewndrowell_q0003_01.htm";
		} else if (cond == 1) {
			return "tewndrowell_q0003_04.htm";
		} else if (cond == 2) {
			if (qs.hasQuestItems(ONYX_BEAST_EYE) && qs.hasQuestItems(TAINT_STONE) && qs.hasQuestItems(SUCCUBUS_BLOOD)) {
				qs.takeItems(ONYX_BEAST_EYE, -1);
				qs.takeItems(TAINT_STONE, -1);
				qs.takeItems(SUCCUBUS_BLOOD, -1);
				qs.giveItems(ENCHANT_ARMOR_D, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "tewndrowell_q0003_06.htm";
			}
			return "tewndrowell_q0003_05.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int cond = qs.getCond();
		if (cond == 1) {
			int npcId = npc.getNpcId();
			if (npcId == 20031 && !qs.hasQuestItems(ONYX_BEAST_EYE)) {
				qs.giveItems(ONYX_BEAST_EYE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if ((npcId == 20041 || npcId == 20046) && !qs.hasQuestItems(TAINT_STONE)) {
				qs.giveItems(TAINT_STONE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if ((npcId == 20048 || npcId == 20052 || npcId == 20057) && !qs.hasQuestItems(SUCCUBUS_BLOOD)) {
				qs.giveItems(SUCCUBUS_BLOOD, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			if (qs.hasQuestItems(ONYX_BEAST_EYE) && qs.hasQuestItems(TAINT_STONE) && qs.hasQuestItems(SUCCUBUS_BLOOD)) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
