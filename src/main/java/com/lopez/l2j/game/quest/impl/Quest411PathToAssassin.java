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
 * Quest 411: Path to Assassin (1ª Troca de Classe do Dark Fighter para Assassin).
 */
@Component
public class Quest411PathToAssassin extends Quest {

	public static final int QUEST_ID = 411;
	public static final String QUEST_NAME = "411_PathToAssassin";

	// NPCs
	public static final int TRISKEL = 30416;
	public static final int LEIKAN = 30382;
	public static final int ARKENIA = 30419;

	// Monstros
	public static final int ONYX_BEAST = 20369;
	public static final int CALPICO = 27036;

	// Itens
	public static final int SHILENS_CALL = 1245;
	public static final int ARKENIAS_LETTER = 1246;
	public static final int LEIKANS_NOTE = 1247;
	public static final int ONYX_BEASTS_MOLAR = 1248;
	public static final int SHILENS_TEARS = 1250;
	public static final int ARKENIA_RECOMMEND = 1251;
	public static final int IRON_HEART = 1252;

	@Autowired
	public Quest411PathToAssassin(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Assassin");

		addStartNpc(TRISKEL);
		addTalkId(TRISKEL);
		addTalkId(LEIKAN);
		addTalkId(ARKENIA);

		addKillId(ONYX_BEAST);
		addKillId(CALPICO);

		registerQuestItems(SHILENS_CALL, ARKENIAS_LETTER, LEIKANS_NOTE,
				ONYX_BEASTS_MOLAR, SHILENS_TEARS, ARKENIA_RECOMMEND);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int level = player != null ? player.getLevel() : 0;
		int classId = player != null ? player.getClassId() : -1;

		if ("1".equalsIgnoreCase(event)) {
			if (level >= 18 && classId == 0x1f && qs.getQuestItemsCount(IRON_HEART) == 0) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				qs.giveItems(SHILENS_CALL, 1);
				return "30416-05.htm";
			} else if (classId != 0x1f) {
				return classId == 0x23 ? "30416-02a.htm" : "30416-02.htm";
			} else if (level < 18 && classId == 0x1f) {
				return "30416-03.htm";
			} else if (qs.getQuestItemsCount(IRON_HEART) > 0) {
				return "30416-04.htm";
			}
		} else if ("30419_1".equalsIgnoreCase(event)) {
			qs.giveItems(ARKENIAS_LETTER, 1);
			qs.takeItems(SHILENS_CALL, 1);
			qs.setCond(2);
			qs.playSound("ItemSound.quest_middle");
			return "30419-05.htm";
		} else if ("30382_1".equalsIgnoreCase(event)) {
			qs.giveItems(LEIKANS_NOTE, 1);
			qs.takeItems(ARKENIAS_LETTER, 1);
			qs.setCond(3);
			qs.playSound("ItemSound.quest_middle");
			return "30382-03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == TRISKEL) {
			if (cond == 0) {
				return qs.getQuestItemsCount(IRON_HEART) == 0 ? "30416-01.htm" : "30416-04.htm";
			} else if (cond >= 1) {
				if (qs.getQuestItemsCount(ARKENIA_RECOMMEND) == 1) {
					qs.rewardItems(57, 81900);
					qs.takeItems(ARKENIA_RECOMMEND, 1);
					qs.giveItems(IRON_HEART, 1);
					qs.addExpAndSp(295862, 21264);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30416-06.htm";
				} else if (qs.getQuestItemsCount(ARKENIAS_LETTER) == 1) {
					return "30416-07.htm";
				} else if (qs.getQuestItemsCount(LEIKANS_NOTE) == 1) {
					return "30416-08.htm";
				} else if (qs.getQuestItemsCount(SHILENS_TEARS) == 1) {
					return "30416-10.htm";
				} else if (qs.getQuestItemsCount(SHILENS_CALL) == 1) {
					return "30416-11.htm";
				} else {
					return "30416-09.htm";
				}
			}
		} else if (npcId == ARKENIA && cond >= 1) {
			if (qs.getQuestItemsCount(SHILENS_CALL) == 1) {
				return "30419-01.htm";
			} else if (qs.getQuestItemsCount(ARKENIAS_LETTER) == 1) {
				return "30419-07.htm";
			} else if (qs.getQuestItemsCount(SHILENS_TEARS) == 1) {
				qs.giveItems(ARKENIA_RECOMMEND, 1);
				qs.takeItems(SHILENS_TEARS, 1);
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				return "30419-08.htm";
			} else if (qs.getQuestItemsCount(ARKENIA_RECOMMEND) == 1) {
				return "30419-09.htm";
			} else if (qs.getQuestItemsCount(LEIKANS_NOTE) == 1) {
				return "30419-10.htm";
			} else {
				return "30419-11.htm";
			}
		} else if (npcId == LEIKAN && cond >= 1) {
			if (qs.getQuestItemsCount(ARKENIAS_LETTER) == 1) {
				return "30382-01.htm";
			} else if (qs.getQuestItemsCount(LEIKANS_NOTE) == 1) {
				long molars = qs.getQuestItemsCount(ONYX_BEASTS_MOLAR);
				if (molars == 0) {
					return "30382-05.htm";
				} else if (molars < 10) {
					return "30382-06.htm";
				} else {
					qs.setCond(5);
					qs.playSound("ItemSound.quest_middle");
					qs.takeItems(ONYX_BEASTS_MOLAR, -1);
					qs.takeItems(LEIKANS_NOTE, 1);
					return "30382-07.htm";
				}
			} else if (qs.getQuestItemsCount(SHILENS_TEARS) == 1) {
				return "30382-08.htm";
			} else {
				return "30382-09.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ONYX_BEAST) {
			if (cond >= 1 && qs.getQuestItemsCount(LEIKANS_NOTE) == 1 && qs.getQuestItemsCount(ONYX_BEASTS_MOLAR) < 10) {
				qs.giveItems(ONYX_BEASTS_MOLAR, 1);
				if (qs.getQuestItemsCount(ONYX_BEASTS_MOLAR) == 10) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(4);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == CALPICO) {
			if (cond >= 1 && qs.getQuestItemsCount(SHILENS_TEARS) == 0) {
				qs.giveItems(SHILENS_TEARS, 1);
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(6);
			}
		}
		return null;
	}
}
