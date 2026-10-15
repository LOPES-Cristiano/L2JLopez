package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 125: In the Name of Evil - Part 1
 */
@Component
public class Quest125InTheNameOfEvilPart1 extends Quest {

	public static final int QUEST_ID = 125;
	public static final String QUEST_NAME = "125_InTheNameOfEvilPart1";
	public static final int START_NPC = 32114;

	public static final int MUSHIKA = 32114;
	public static final int KARAKAWEI = 32117;
	public static final int ULU_KAIMU = 32119;
	public static final int BALU_KAIMU = 32120;
	public static final int CHUTA_KAIMU = 32121;

	public static final int ORNITHOMIMUS_CLAW = 8779;
	public static final int DEINONYCHUS_BONE = 8780;
	public static final int EPITAPH_OF_WISDOM = 8781;
	public static final int GAZKH_FRAGMENT = 8782;

	@Autowired
	public Quest125InTheNameOfEvilPart1(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In the Name of Evil - Part 1");
		addStartNpc(START_NPC);
		addStartNpc(32115);
		addTalkId(32114, 32115, 32117, 32119, 32120, 32121);
		addKillId(22200, 22201, 22202, 22203, 22204, 22205, 22219, 22220, 22224, 22225);
		registerQuestItems(8779, 8780, 8781, 8782);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "32114-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) ? "mushika_q0125_0104.htm" : event;
		} else if ("32114-07.htm".equalsIgnoreCase(event) || "32114-09.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(GAZKH_FRAGMENT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32117-08.htm".equalsIgnoreCase(event) || "reply_13".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32117-14.htm".equalsIgnoreCase(event) || "reply_17".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32119-14.htm".equalsIgnoreCase(event) || "reply_26".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32120-15.htm".equalsIgnoreCase(event) || "reply_34".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32121-16.htm".equalsIgnoreCase(event) || "reply_43".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.takeItems(GAZKH_FRAGMENT, -1);
			qs.giveItems(EPITAPH_OF_WISDOM, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
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

		if (npcId == START_NPC || npcId == 32115) {
			if (cond == 0) {
				if (pc.level() >= 76) return "mushika_q0125_0101.htm";
				qs.exitQuest(true);
				return "mushika_q0125_0103.htm";
			} else if (cond == 1) {
				return "mushika_q0125_0105.htm";
			} else if (cond == 2) {
				if (!qs.hasQuestItems(GAZKH_FRAGMENT)) {
					qs.giveItems(57, 230756);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "mushika_q0125_0202.htm";
				}
				return "32114-10.htm";
			} else if (cond >= 3 && cond <= 7) {
				return "32114-11.htm";
			} else if (cond == 8) {
				qs.takeItems(EPITAPH_OF_WISDOM, -1);
				qs.giveItems(57, 230756);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "32114-12.htm";
			}
		} else if (npcId == KARAKAWEI) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "shaman_caracawe_q0125_0201.htm";
			} else if (cond == 2) {
				return "32117-01.htm";
			} else if (cond == 3) {
				if (qs.getQuestItemsCount(ORNITHOMIMUS_CLAW) >= 2 && qs.getQuestItemsCount(DEINONYCHUS_BONE) >= 2) {
					qs.takeItems(ORNITHOMIMUS_CLAW, -1);
					qs.takeItems(DEINONYCHUS_BONE, -1);
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "32117-10.htm";
				}
				return "32117-09.htm";
			} else if (cond == 4) {
				if (qs.getQuestItemsCount(ORNITHOMIMUS_CLAW) >= 2 && qs.getQuestItemsCount(DEINONYCHUS_BONE) >= 2) {
					qs.takeItems(ORNITHOMIMUS_CLAW, -1);
					qs.takeItems(DEINONYCHUS_BONE, -1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
				return "32117-10.htm";
			} else if (cond == 5) {
				return "32117-15.htm";
			} else if (cond == 6 || cond == 7) {
				return "32117-16.htm";
			} else if (cond == 8) {
				return "32117-17.htm";
			}
		} else if (npcId == ULU_KAIMU) {
			if (cond == 5) return "32119-01.htm";
			if (cond >= 6) return "32119-14.htm";
		} else if (npcId == BALU_KAIMU) {
			if (cond == 6) return "32120-01.htm";
			if (cond >= 7) return "32120-16.htm";
		} else if (npcId == CHUTA_KAIMU) {
			if (cond == 7) return "32121-01.htm";
			if (cond >= 8) return "32121-17.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs.getCond() != 3) return null;

		int npcId = npc.getNpcId();
		if (npcId == 22200 || npcId == 22201 || npcId == 22202 || npcId == 22219 || npcId == 22224) {
			if (qs.getQuestItemsCount(ORNITHOMIMUS_CLAW) < 2 && ThreadLocalRandom.current().nextInt(100) < 50) {
				qs.giveItems(ORNITHOMIMUS_CLAW, 1);
				if (qs.getQuestItemsCount(ORNITHOMIMUS_CLAW) >= 2 && qs.getQuestItemsCount(DEINONYCHUS_BONE) >= 2) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (npcId == 22203 || npcId == 22204 || npcId == 22205 || npcId == 22220 || npcId == 22225) {
			if (qs.getQuestItemsCount(DEINONYCHUS_BONE) < 2 && ThreadLocalRandom.current().nextInt(100) < 50) {
				qs.giveItems(DEINONYCHUS_BONE, 1);
				if (qs.getQuestItemsCount(ORNITHOMIMUS_CLAW) >= 2 && qs.getQuestItemsCount(DEINONYCHUS_BONE) >= 2) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
