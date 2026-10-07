package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 401: Path to Warrior (1ª Troca de Classe do Human Fighter para Warrior).
 * Portada com fidelidade retail e paridade estrita aos scripts oficiais de Lineage II Interlude.
 */
@Component
public class Quest401PathToWarrior extends Quest {

	public static final int QUEST_ID = 401;
	public static final String QUEST_NAME = "401_PathToWarrior";

	// NPCs
	public static final int AURON = 30010;
	public static final int SIMPLON = 30253;

	// Monstros
	public static final int TRACKER_SKELETON = 20035;
	public static final int SKELETON_SCOUT = 20042;
	public static final int POISON_SPIDER = 20038;
	public static final int ARACHNID_TRACKER = 20043;

	// Itens de Quest
	public static final int EINS_LETTER = 1138;
	public static final int WARRIOR_GUILD_MARK = 1139;
	public static final int RUSTED_BRONZE_SWORD1 = 1140;
	public static final int RUSTED_BRONZE_SWORD2 = 1141;
	public static final int RUSTED_BRONZE_SWORD3 = 1142;
	public static final int SIMPLONS_LETTER = 1143;
	public static final int POISON_SPIDER_LEG2 = 1144;
	public static final int MEDALLION_OF_WARRIOR = 1145;

	@Autowired
	public Quest401PathToWarrior(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Warrior");

		addStartNpc(AURON);
		addTalkId(AURON);
		addTalkId(SIMPLON);

		addKillId(TRACKER_SKELETON);
		addKillId(SKELETON_SCOUT);
		addKillId(POISON_SPIDER);
		addKillId(ARACHNID_TRACKER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		if ("401_1".equalsIgnoreCase(event)) {
			if (c.classId() == 0) { // Human Fighter
				if (c.level() >= 18) {
					if (qs.getQuestItemsCount(MEDALLION_OF_WARRIOR) > 0) {
						return "30010-04.htm";
					} else {
						return "30010-05.htm";
					}
				} else {
					return "30010-02.htm";
				}
			} else if (c.classId() == 1) { // Já é Warrior
				return "30010-02a.htm";
			} else {
				return "30010-03.htm";
			}
		} else if ("401_2".equalsIgnoreCase(event)) {
			return "30010-10.htm";
		} else if ("401_3".equalsIgnoreCase(event)) {
			qs.takeItems(SIMPLONS_LETTER, 1);
			qs.takeItems(RUSTED_BRONZE_SWORD2, 1);
			qs.giveItems(RUSTED_BRONZE_SWORD3, 1);
			qs.setCond(5);
			return "30010-11.htm";
		} else if ("1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(EINS_LETTER) == 0) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				qs.giveItems(EINS_LETTER, 1);
				return "30010-06.htm";
			}
		} else if ("30253_1".equalsIgnoreCase(event)) {
			qs.takeItems(EINS_LETTER, 1);
			qs.giveItems(WARRIOR_GUILD_MARK, 1);
			qs.setCond(2);
			return "30253-02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId != AURON && !qs.isStarted()) {
			return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
		}

		if (npcId == AURON) {
			if (cond == 0) {
				return "30010-01.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(EINS_LETTER) > 0) {
				return "30010-07.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(WARRIOR_GUILD_MARK) == 1) {
				return "30010-08.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(SIMPLONS_LETTER) > 0 && qs.getQuestItemsCount(RUSTED_BRONZE_SWORD2) > 0) {
				return "30010-09.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(RUSTED_BRONZE_SWORD3) > 0) {
				if (qs.getQuestItemsCount(POISON_SPIDER_LEG2) < 20) {
					return "30010-12.htm";
				} else {
					qs.takeItems(POISON_SPIDER_LEG2, -1);
					qs.takeItems(RUSTED_BRONZE_SWORD3, 1);
					qs.giveItems(57, 81900); // Adena
					qs.giveItems(MEDALLION_OF_WARRIOR, 1);
					qs.addExpAndSp(295862, 16814);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound(QuestState.SOUND_FINISH);
					return "30010-13.htm";
				}
			}
		} else if (npcId == SIMPLON) {
			if (cond > 0 && qs.getQuestItemsCount(EINS_LETTER) > 0) {
				return "30253-01.htm";
			} else if (cond > 0 && qs.getQuestItemsCount(WARRIOR_GUILD_MARK) > 0) {
				if (qs.getQuestItemsCount(RUSTED_BRONZE_SWORD1) < 1) {
					return "30253-03.htm";
				} else if (qs.getQuestItemsCount(RUSTED_BRONZE_SWORD1) < 10) {
					return "30253-04.htm";
				} else {
					qs.takeItems(WARRIOR_GUILD_MARK, 1);
					qs.takeItems(RUSTED_BRONZE_SWORD1, -1);
					qs.giveItems(RUSTED_BRONZE_SWORD2, 1);
					qs.giveItems(SIMPLONS_LETTER, 1);
					qs.setCond(4);
					return "30253-05.htm";
				}
			} else if (cond > 0 && qs.getQuestItemsCount(SIMPLONS_LETTER) > 0) {
				return "30253-06.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == TRACKER_SKELETON || npcId == SKELETON_SCOUT) {
			if (cond == 2 && qs.getQuestItemsCount(RUSTED_BRONZE_SWORD1) < 10) {
				if (ThreadLocalRandom.current().nextInt(10) < 4) {
					qs.giveItems(RUSTED_BRONZE_SWORD1, 1);
					if (qs.getQuestItemsCount(RUSTED_BRONZE_SWORD1) >= 10) {
						qs.playSound(QuestState.SOUND_MIDDLE);
						qs.setCond(3);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		} else if (npcId == POISON_SPIDER || npcId == ARACHNID_TRACKER) {
			if (cond == 5 && qs.getQuestItemsCount(POISON_SPIDER_LEG2) < 20 && qs.getQuestItemsCount(RUSTED_BRONZE_SWORD3) == 1) {
				qs.giveItems(POISON_SPIDER_LEG2, 1);
				if (qs.getQuestItemsCount(POISON_SPIDER_LEG2) >= 20) {
					qs.playSound(QuestState.SOUND_MIDDLE);
					qs.setCond(6);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
