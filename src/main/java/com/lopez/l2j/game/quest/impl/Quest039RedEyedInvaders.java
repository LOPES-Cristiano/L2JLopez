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
 * Quest 39: Red-Eyed Invaders
 * Guard Babenco e Captain Bathis em Gludio.
 */
@Component
public class Quest039RedEyedInvaders extends Quest {

	public static final int QUEST_ID = 39;
	public static final String QUEST_NAME = "039_RedEyedInvaders";

	public static final int START_NPC = 30334;
	public static final int BABENCO = 30334;
	public static final int BATHIS = 30332;

	public static final int MAILLE_LIZARDMAN = 20919;
	public static final int MAILLE_LIZARDMAN_SCOUT = 20920;
	public static final int MAILLE_LIZARDMAN_GUARD = 20921;
	public static final int GIANT_ARANEID = 20925;

	public static final int BLACK_BONE_NECKLACE = 7178;
	public static final int RED_BONE_NECKLACE = 7179;
	public static final int INCENSE_POUCH = 7180;
	public static final int GEM_OF_MAILLE = 7181;

	public static final int GREEN_COLORED_LURE_HG = 6521;
	public static final int BABY_DUCK_RODE = 6529;
	public static final int FISHING_SHOT_NG = 6535;

	@Autowired
	public Quest039RedEyedInvaders(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Red-Eyed Invaders");
		addStartNpc(BABENCO);
		addTalkId(BABENCO, BATHIS);
		addKillId(MAILLE_LIZARDMAN, MAILLE_LIZARDMAN_SCOUT, MAILLE_LIZARDMAN_GUARD, GIANT_ARANEID);
		registerQuestItems(BLACK_BONE_NECKLACE, RED_BONE_NECKLACE, INCENSE_POUCH, GEM_OF_MAILLE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30334-1.htm".equalsIgnoreCase(event) || "guard_babenco_q0039_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "guard_babenco_q0039_0104.htm";
		} else if ("30332-1.htm".equalsIgnoreCase(event) || "captain_bathia_q0039_0201.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30332-3.htm".equalsIgnoreCase(event) || "captain_bathia_q0039_0301.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BLACK_BONE_NECKLACE) >= 100 && qs.getQuestItemsCount(RED_BONE_NECKLACE) >= 100) {
				qs.setCond(4);
				qs.takeItems(BLACK_BONE_NECKLACE, -1);
				qs.takeItems(RED_BONE_NECKLACE, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "captain_bathia_q0039_0203.htm";
		} else if ("30332-5.htm".equalsIgnoreCase(event) || "captain_bathia_q0039_0401.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(INCENSE_POUCH) >= 30 && qs.getQuestItemsCount(GEM_OF_MAILLE) >= 30) {
				qs.takeItems(INCENSE_POUCH, -1);
				qs.takeItems(GEM_OF_MAILLE, -1);
				qs.giveItems(GREEN_COLORED_LURE_HG, 60);
				qs.giveItems(BABY_DUCK_RODE, 1);
				qs.giveItems(FISHING_SHOT_NG, 500);
				qs.addExpAndSp(62366, 2783);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "captain_bathia_q0039_0304.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case BABENCO:
				if (cond == 0) {
					if (pc.level() >= 20) return "guard_babenco_q0039_0101.htm";
					qs.exitQuest(true);
					return "guard_babenco_q0039_0103.htm";
				} else if (cond >= 1) {
					return "guard_babenco_q0039_0105.htm";
				}
				break;
			case BATHIS:
				if (cond == 1) {
					return "captain_bathia_q0039_0101.htm";
				} else if (cond == 2) {
					return "captain_bathia_q0039_0203.htm";
				} else if (cond == 3 && qs.getQuestItemsCount(BLACK_BONE_NECKLACE) >= 100 && qs.getQuestItemsCount(RED_BONE_NECKLACE) >= 100) {
					return "captain_bathia_q0039_0202.htm";
				} else if (cond == 4) {
					return "captain_bathia_q0039_0304.htm";
				} else if (cond == 5 && qs.getQuestItemsCount(INCENSE_POUCH) >= 30 && qs.getQuestItemsCount(GEM_OF_MAILLE) >= 30) {
					return "captain_bathia_q0039_0303.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		if (cond == 2) {
			if ((npcId == MAILLE_LIZARDMAN || npcId == MAILLE_LIZARDMAN_SCOUT) && qs.getQuestItemsCount(BLACK_BONE_NECKLACE) < 100) {
				qs.giveItems(BLACK_BONE_NECKLACE, 1);
				if (qs.getQuestItemsCount(BLACK_BONE_NECKLACE) >= 100 && qs.getQuestItemsCount(RED_BONE_NECKLACE) >= 100) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			} else if (npcId == MAILLE_LIZARDMAN_GUARD && qs.getQuestItemsCount(RED_BONE_NECKLACE) < 100) {
				qs.giveItems(RED_BONE_NECKLACE, 1);
				if (qs.getQuestItemsCount(BLACK_BONE_NECKLACE) >= 100 && qs.getQuestItemsCount(RED_BONE_NECKLACE) >= 100) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 4) {
			if ((npcId == MAILLE_LIZARDMAN_SCOUT || npcId == MAILLE_LIZARDMAN_GUARD) && qs.getQuestItemsCount(INCENSE_POUCH) < 30) {
				qs.giveItems(INCENSE_POUCH, 1);
				if (qs.getQuestItemsCount(INCENSE_POUCH) >= 30 && qs.getQuestItemsCount(GEM_OF_MAILLE) >= 30) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			} else if (npcId == GIANT_ARANEID && qs.getQuestItemsCount(GEM_OF_MAILLE) < 30) {
				qs.giveItems(GEM_OF_MAILLE, 1);
				if (qs.getQuestItemsCount(INCENSE_POUCH) >= 30 && qs.getQuestItemsCount(GEM_OF_MAILLE) >= 30) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
