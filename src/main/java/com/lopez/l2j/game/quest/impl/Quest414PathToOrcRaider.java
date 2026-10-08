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
 * Quest 414: Path to Orc Raider (1ª Troca de Classe do Orc Fighter para Orc Raider).
 */
@Component
public class Quest414PathToOrcRaider extends Quest {

	public static final int QUEST_ID = 414;
	public static final String QUEST_NAME = "414_PathToOrcRaider";

	// NPCs
	public static final int KARUKIA = 30570;
	public static final int KASMAN = 30501;
	public static final int TAZEER = 31978;

	// Monstros
	public static final int GOBLIN_TOMB_RAIDER_LEADER = 20320;
	public static final int KURUKA_RATMAN_LEADER = 27045;
	public static final int UMBAR_ORC = 27054;
	public static final int TIMORA_ORC = 27320;

	// Itens
	public static final int GREEN_BLOOD = 1578;
	public static final int GOBLIN_DWELLING_MAP = 1579;
	public static final int KURUKA_RATMAN_TOOTH = 1580;
	public static final int BETRAYER_UMBAR_REPORT = 1589;
	public static final int HEAD_OF_BETRAYER = 1591;
	public static final int TIMORA_ORC_HEAD = 8544;
	public static final int MARK_OF_RAIDER = 1592;

	@Autowired
	public Quest414PathToOrcRaider(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Orc Raider");

		addStartNpc(KARUKIA);
		addTalkId(KARUKIA);
		addTalkId(KASMAN);
		addTalkId(TAZEER);

		addKillId(GOBLIN_TOMB_RAIDER_LEADER);
		addKillId(KURUKA_RATMAN_LEADER);
		addKillId(UMBAR_ORC);
		addKillId(TIMORA_ORC);

		registerQuestItems(GREEN_BLOOD, GOBLIN_DWELLING_MAP, KURUKA_RATMAN_TOOTH,
				BETRAYER_UMBAR_REPORT, HEAD_OF_BETRAYER, TIMORA_ORC_HEAD);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30570-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(GOBLIN_DWELLING_MAP, 1);
			qs.playSound("ItemSound.quest_accept");
			return event;
		} else if ("30570-07a.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KURUKA_RATMAN_TOOTH, -1);
			qs.takeItems(GOBLIN_DWELLING_MAP, -1);
			qs.takeItems(GREEN_BLOOD, -1);
			qs.giveItems(BETRAYER_UMBAR_REPORT, 1);
			qs.setCond(3);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("30570-07b.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KURUKA_RATMAN_TOOTH, -1);
			qs.takeItems(GOBLIN_DWELLING_MAP, -1);
			qs.takeItems(GREEN_BLOOD, -1);
			qs.setCond(5);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("31978-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound("ItemSound.quest_middle");
			return event;
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
		PlayerCharacter player = qs.getPlayerCharacter();
		int classId = player != null ? player.getClassId() : -1;
		int level = player != null ? player.getLevel() : 0;

		if (npcId == KARUKIA) {
			if (cond == 0) {
				if (level >= 18 && classId == 0x2c && qs.getQuestItemsCount(MARK_OF_RAIDER) == 0 && qs.getQuestItemsCount(GOBLIN_DWELLING_MAP) == 0) {
					return "30570-01.htm";
				} else if (classId != 0x2c) {
					return classId == 0x2d ? "30570-02a.htm" : "30570-03.htm";
				} else if (level < 18 && classId == 0x2c) {
					return "30570-02.htm";
				} else if (qs.getQuestItemsCount(MARK_OF_RAIDER) > 0) {
					return "30570-04.htm";
				} else {
					return "30570-02.htm";
				}
			} else if (cond > 0 && qs.getQuestItemsCount(GOBLIN_DWELLING_MAP) == 1 && qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) < 10) {
				return "30570-06.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(GOBLIN_DWELLING_MAP) == 1 && qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) >= 10 && qs.getQuestItemsCount(BETRAYER_UMBAR_REPORT) == 0) {
				return "30570-07.htm";
			} else if (cond > 5) {
				return "30570-07b.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(BETRAYER_UMBAR_REPORT) > 0 && qs.getQuestItemsCount(HEAD_OF_BETRAYER) < 2) {
				return "30570-08.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(BETRAYER_UMBAR_REPORT) > 0 && qs.getQuestItemsCount(HEAD_OF_BETRAYER) >= 2) {
				return "30570-09.htm";
			}
		} else if (npcId == KASMAN && cond > 0) {
			if (qs.getQuestItemsCount(BETRAYER_UMBAR_REPORT) > 0 && qs.getQuestItemsCount(HEAD_OF_BETRAYER) == 0) {
				return "30501-01.htm";
			} else if (qs.getQuestItemsCount(HEAD_OF_BETRAYER) > 0 && qs.getQuestItemsCount(HEAD_OF_BETRAYER) < 2) {
				return "30501-02.htm";
			} else if (qs.getQuestItemsCount(HEAD_OF_BETRAYER) >= 2) {
				qs.rewardItems(57, 81900);
				qs.takeItems(HEAD_OF_BETRAYER, -1);
				qs.takeItems(BETRAYER_UMBAR_REPORT, -1);
				qs.giveItems(MARK_OF_RAIDER, 1);
				qs.addExpAndSp(295862, 17354);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound("ItemSound.quest_finish");
				return "30501-03.htm";
			}
		} else if (npcId == TAZEER) {
			if (cond == 5) {
				return "31978-01.htm";
			} else if (cond == 6) {
				return "31978-04.htm";
			} else if (cond == 7) {
				qs.takeItems(TIMORA_ORC_HEAD, -1);
				qs.giveItems(MARK_OF_RAIDER, 1);
				qs.rewardItems(57, 81900);
				qs.addExpAndSp(160267, 10656);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound("ItemSound.quest_finish");
				return "31978-05.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int cond = qs.getCond();
		int npcId = npc.getNpcId();

		if (npcId == GOBLIN_TOMB_RAIDER_LEADER) {
			if (cond > 0 && qs.getQuestItemsCount(GOBLIN_DWELLING_MAP) == 1 && qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) < 10) {
				long blood = qs.getQuestItemsCount(GREEN_BLOOD);
				if (blood > 1 && ThreadLocalRandom.current().nextInt(100) < blood * 10) {
					qs.takeItems(GREEN_BLOOD, -1);
					qs.giveItems(KURUKA_RATMAN_TOOTH, 1);
					if (qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) >= 10) {
						qs.playSound("ItemSound.quest_middle");
						qs.setCond(2);
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				} else {
					qs.giveItems(GREEN_BLOOD, 1);
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == KURUKA_RATMAN_LEADER) {
			if (cond > 0 && qs.getQuestItemsCount(GOBLIN_DWELLING_MAP) == 1 && qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) < 10) {
				qs.takeItems(GREEN_BLOOD, -1);
				qs.giveItems(KURUKA_RATMAN_TOOTH, 1);
				if (qs.getQuestItemsCount(KURUKA_RATMAN_TOOTH) >= 10) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(2);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == UMBAR_ORC) {
			if (cond > 0 && qs.getQuestItemsCount(BETRAYER_UMBAR_REPORT) > 0 && qs.getQuestItemsCount(HEAD_OF_BETRAYER) < 2) {
				qs.giveItems(HEAD_OF_BETRAYER, 1);
				if (qs.getQuestItemsCount(HEAD_OF_BETRAYER) >= 2) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(4);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == TIMORA_ORC) {
			if (cond == 6) {
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				qs.giveItems(TIMORA_ORC_HEAD, 1);
			}
		}
		return null;
	}
}
