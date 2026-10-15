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
 * Quest 126: In the Name of Evil - Part 2
 */
@Component
public class Quest126IntheNameofEvilPart2 extends Quest {

	public static final int QUEST_ID = 126;
	public static final String QUEST_NAME = "126_IntheNameofEvilPart2";
	public static final int START_NPC = 32115;

	public static final int MUSHIKA = 32114;
	public static final int ASAMAH = 32115;
	public static final int ULU_KAIMU = 32119;
	public static final int BALU_KAIMU = 32120;
	public static final int CHUTA_KAIMU = 32121;
	public static final int WARRIOR_GRAVE = 32122;
	public static final int SHILEN_STONE_STATUE = 32109;

	public static final int EPITAPH_OF_WISDOM = 8781;
	public static final int BONEPOWDER = 8783;
	public static final int EWA = 729;

	@Autowired
	public Quest126IntheNameofEvilPart2(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In the Name of Evil - Part 2");
		addStartNpc(START_NPC);
		addStartNpc(32116);
		addTalkId(32115, 32114, 32116, 32117, 32119, 32120, 32121, 32122, 32109);
		registerQuestItems(8781, 8783);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "32115-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) ? "asamah_q0126_0104.htm" : event;
		} else if ("32115-10.htm".equalsIgnoreCase(event) || "reply_4".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32119-02.htm".equalsIgnoreCase(event) || "reply_5".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32119-09.htm".equalsIgnoreCase(event) || "reply_8".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32119-11.htm".equalsIgnoreCase(event) || "reply_11".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32120-02.htm".equalsIgnoreCase(event) || "32120-07.htm".equalsIgnoreCase(event) || "reply_13".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32120-09.htm".equalsIgnoreCase(event) || "reply_14".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32120-11.htm".equalsIgnoreCase(event) || "reply_17".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32121-02.htm".equalsIgnoreCase(event) || "32121-07.htm".equalsIgnoreCase(event) || "reply_18a".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32121-10.htm".equalsIgnoreCase(event) || "reply_23".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32121-15.htm".equalsIgnoreCase(event) || "reply_28".equalsIgnoreCase(event)) {
			qs.setCond(11);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32122-02.htm".equalsIgnoreCase(event) || "32122-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32122-15.htm".equalsIgnoreCase(event) || "reply_40".equalsIgnoreCase(event)) {
			qs.setCond(13);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32122-18.htm".equalsIgnoreCase(event) || "reply_42".equalsIgnoreCase(event)) {
			qs.setCond(14);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("DOOne".equalsIgnoreCase(event)) {
			return "32122-26.htm";
		} else if ("MIOne".equalsIgnoreCase(event)) {
			return "32122-30.htm";
		} else if ("FAOne".equalsIgnoreCase(event)) {
			return "32122-34.htm";
		} else if ("SOLOne".equalsIgnoreCase(event)) {
			return "32122-38.htm";
		} else if ("FA_2One".equalsIgnoreCase(event) || "reply_108".equalsIgnoreCase(event)) {
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32122-42.htm";
		} else if ("FATwo".equalsIgnoreCase(event)) {
			return "32122-47.htm";
		} else if ("SOLTwo".equalsIgnoreCase(event)) {
			return "32122-51.htm";
		} else if ("TITwo".equalsIgnoreCase(event)) {
			return "32122-55.htm";
		} else if ("SOL_2Two".equalsIgnoreCase(event)) {
			return "32122-59.htm";
		} else if ("FA_2Two".equalsIgnoreCase(event) || "reply_118".equalsIgnoreCase(event)) {
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32122-63.htm";
		} else if ("SOLTri".equalsIgnoreCase(event)) {
			return "32122-68.htm";
		} else if ("FATri".equalsIgnoreCase(event)) {
			return "32122-72.htm";
		} else if ("MITri".equalsIgnoreCase(event)) {
			return "32122-76.htm";
		} else if ("FA_2Tri".equalsIgnoreCase(event)) {
			return "32122-80.htm";
		} else if ("MI_2Tri".equalsIgnoreCase(event) || "reply_128".equalsIgnoreCase(event)) {
			qs.setCond(17);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32122-84.htm";
		} else if ("32122-87.htm".equalsIgnoreCase(event) || "reply_130".equalsIgnoreCase(event)) {
			qs.giveItems(BONEPOWDER, 1);
			return event;
		} else if ("32122-90.htm".equalsIgnoreCase(event) || "reply_132".equalsIgnoreCase(event)) {
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32109-02.htm".equalsIgnoreCase(event) || "reply_1a".equalsIgnoreCase(event)) {
			qs.setCond(19);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32109-19.htm".equalsIgnoreCase(event) || "reply_12b".equalsIgnoreCase(event)) {
			qs.setCond(20);
			qs.takeItems(BONEPOWDER, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32115-21.htm".equalsIgnoreCase(event) || "reply_62".equalsIgnoreCase(event)) {
			qs.setCond(21);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32115-28.htm".equalsIgnoreCase(event) || "reply_72".equalsIgnoreCase(event)) {
			qs.setCond(22);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32114-08.htm".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			qs.setCond(23);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32114-02.htm".equalsIgnoreCase(event) || "32114-09.htm".equalsIgnoreCase(event) || "reply_3".equalsIgnoreCase(event)) {
			qs.giveItems(EWA, 1);
			qs.giveItems(8704, 1);
			qs.giveItems(57, 461512);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ASAMAH || npcId == 32116) {
			if (cond == 0) {
				if (pc.level() >= 77) return "asamah_q0126_0101.htm";
				qs.exitQuest(true);
				return "asamah_q0126_0103.htm";
			} else if (cond == 1) {
				return "asamah_q0126_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(EWA, 1);
				qs.giveItems(8704, 1);
				qs.giveItems(57, 461512);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "asamah_q0126_0202.htm";
			} else if (cond > 2 && cond < 20) {
				return "32115-12.htm";
			} else if (cond == 20) {
				return "32115-13.htm";
			} else if (cond == 21) {
				return "32115-22.htm";
			} else if (cond >= 22) {
				return "32115-29.htm";
			}
		} else if (npcId == 32117 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "shaman_caracawe_q0126_0201.htm";
		} else if (npcId == ULU_KAIMU) {
			if (cond == 1) return "32119-01a.htm";
			if (cond == 2) return "32119-02.htm";
			if (cond == 3) return "32119-08.htm";
			if (cond == 4) return "32119-09.htm";
			if (cond >= 5) return "32119-12.htm";
		} else if (npcId == BALU_KAIMU) {
			if (cond < 5) return "32120-02.htm";
			if (cond == 5) return "32120-01.htm";
			if (cond == 6) return "32120-03.htm";
			if (cond == 7) return "32120-08.htm";
			if (cond >= 8) return "32120-12.htm";
		} else if (npcId == CHUTA_KAIMU) {
			if (cond < 8) return "32121-02.htm";
			if (cond == 8) return "32121-01.htm";
			if (cond == 9) return "32121-03.htm";
			if (cond == 10) return "32121-10.htm";
			if (cond >= 11) return "32121-16.htm";
		} else if (npcId == WARRIOR_GRAVE) {
			if (cond < 11) return "32122-02.htm";
			if (cond == 11) return "32122-01.htm";
			if (cond == 12) return "32122-15.htm";
			if (cond == 13) return "32122-18.htm";
			if (cond == 14) return "32122-24.htm";
			if (cond == 15) return "32122-45.htm";
			if (cond == 16) return "32122-66.htm";
			if (cond == 17) return "32122-84.htm";
			if (cond >= 18) return "32122-91.htm";
		} else if (npcId == SHILEN_STONE_STATUE) {
			if (cond < 18) return "32109-03.htm";
			if (cond == 18) return "32109-02.htm";
			if (cond == 19) return "32109-05.htm";
			if (cond >= 20) return "32109-04.htm";
		} else if (npcId == MUSHIKA) {
			if (cond < 22) return "32114-02.htm";
			if (cond == 22) return "32114-01.htm";
			if (cond == 23) return "32114-04.htm";
		}
		return "noquest";
	}
}
