package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.stereotype.Component;

/**
 * Quest 259 - Ranchers Plea
 */
@Component
public class Quest259RanchersPlea extends Quest {

	public static final int GIANT_SPIDER_SKIN = 1495;
	public static final int HEALING_POTION = 1061;
	public static final int WOODEN_ARROW = 17;
	public static final int SOULSHOT_NO_GRADE = 1835;
	public static final int SPIRITSHOT_NO_GRADE = 2509;
	public static final int EDMOND = 30497;
	public static final int MARIUS = 30405;
	public static final int GIANT_SPIDER = 20103;
	public static final int TALON_SPIDER = 20106;
	public static final int BLADE_SPIDER = 20108;

	public static final int bdo = 1495;
	public static final int bdp = 1061;
	public static final int bdq = 17;
	public static final int bdr = 1835;
	public static final int bds = 2509;
	public static final int bdt = 30497;
	public static final int bdu = 30405;
	public static final int bdv = 20103;
	public static final int bdw = 20106;
	public static final int bdx = 20108;

	public Quest259RanchersPlea(QuestManager questManager) {
		super(259, "259_RanchersPlea", "Ranchers Plea");
		addStartNpc(30497);
		addTalkId(30497, 30405);
		addKillId(20103, 20106, 20108);
		registerQuestItems(1495);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = player != null ? player.getQuestState(getName()) : null;
		if (qs == null) {
			return event;
		}
		int npcId = npc != null ? npc.getNpcId() : 0;
		if (npcId == MARIUS) {
			if (event.equalsIgnoreCase("reply_1") || event.equalsIgnoreCase("30405-03.htm")) {
				return "marius_q0259_03.htm";
			} else if ((event.equalsIgnoreCase("reply_2") || event.equalsIgnoreCase("30405-04.htm")) && qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				qs.giveItems(HEALING_POTION, 2);
				qs.takeItems(GIANT_SPIDER_SKIN, 10);
				return "marius_q0259_04.htm";
			}
		}
		return onEvent(event, qs);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)
				|| "edmond_q0259_03.htm".equalsIgnoreCase(event) || "30497-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "edmond_q0259_03.htm";
		}

		if (event.equalsIgnoreCase("reply_1") || event.equalsIgnoreCase("30497-06.htm") || event.equalsIgnoreCase("edmond_q0259_06.htm")) {
			qs.exitQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
			return "edmond_q0259_06.htm";
		} else if (event.equalsIgnoreCase("reply_2") || event.equalsIgnoreCase("30497-07.htm") || event.equalsIgnoreCase("edmond_q0259_07.htm")) {
			return "edmond_q0259_07.htm";
		} else if (event.equalsIgnoreCase("30405-03.htm") || event.equalsIgnoreCase("marius_q0259_03.htm")) {
			return "marius_q0259_03.htm";
		} else if (event.equalsIgnoreCase("30405-04.htm") || event.equalsIgnoreCase("marius_q0259_04.htm")) {
			if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				qs.giveItems(HEALING_POTION, 2);
				qs.takeItems(GIANT_SPIDER_SKIN, 10);
				return "marius_q0259_04.htm";
			}
			return "marius_q0259_07.htm";
		} else if (event.equalsIgnoreCase("reply_3") || event.equalsIgnoreCase("30405-05.htm") || event.equalsIgnoreCase("marius_q0259_05.htm")) {
			if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				qs.giveItems(WOODEN_ARROW, 250);
				qs.takeItems(GIANT_SPIDER_SKIN, 10);
				return "marius_q0259_05.htm";
			}
			return "marius_q0259_07.htm";
		} else if (event.equalsIgnoreCase("reply_4") || event.equalsIgnoreCase("30405-07.htm") || event.equalsIgnoreCase("30405-06.htm")) {
			if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				return "marius_q0259_06.htm";
			}
			return "marius_q0259_07.htm";
		} else if (event.equalsIgnoreCase("reply_5") || event.equalsIgnoreCase("marius_q0259_05a.htm")) {
			if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				qs.giveItems(SOULSHOT_NO_GRADE, 60);
				qs.takeItems(GIANT_SPIDER_SKIN, 10);
				return "marius_q0259_05a.htm";
			}
			return "marius_q0259_07.htm";
		} else if (event.equalsIgnoreCase("reply_6") || event.equalsIgnoreCase("marius_q0259_05c.htm")) {
			if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
				qs.giveItems(SPIRITSHOT_NO_GRADE, 30);
				qs.takeItems(GIANT_SPIDER_SKIN, 10);
				return "marius_q0259_05c.htm";
			}
			return "marius_q0259_07.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getStateId();
		switch (n2) {
			case 1: {
				if (n != EDMOND) break;
				if (pc.level() >= 15) {
					html = "edmond_q0259_02.htm";
					break;
				}
				html = "edmond_q0259_01.htm";
				break;
			}
			case 2: {
				if (n == EDMOND) {
					if (qs.getCond() == 1 && qs.getQuestItemsCount(GIANT_SPIDER_SKIN) < 1) {
						html = "edmond_q0259_04.htm";
						break;
					}
					if (qs.getCond() != 1 || qs.getQuestItemsCount(GIANT_SPIDER_SKIN) < 1) break;
					html = "edmond_q0259_05.htm";
					if (qs.getQuestItemsCount(GIANT_SPIDER_SKIN) >= 10) {
						qs.giveItems(57, qs.getQuestItemsCount(GIANT_SPIDER_SKIN) * 25 + 250);
					} else {
						qs.giveItems(57, qs.getQuestItemsCount(GIANT_SPIDER_SKIN) * 25);
					}
					qs.takeItems(GIANT_SPIDER_SKIN, -1);
					break;
				}
				if (n != MARIUS) break;
				if (qs.getCond() == 1 && qs.getQuestItemsCount(GIANT_SPIDER_SKIN) < 10) {
					html = "marius_q0259_01.htm";
					break;
				}
				if (qs.getCond() != 1 || qs.getQuestItemsCount(GIANT_SPIDER_SKIN) < 10) break;
				html = "marius_q0259_02.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() > 0) {
			qs.rollAndGive(GIANT_SPIDER_SKIN, 1, 100.0);
		}
		return null;
	}
}
