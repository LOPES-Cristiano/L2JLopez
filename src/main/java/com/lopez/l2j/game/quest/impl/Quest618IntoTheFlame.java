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
 * Quest 618: Into the Flame (Acesso ao Grand Boss Valakas).
 * Concede a lendária Floating Stone (7267).
 */
@Component
public class Quest618IntoTheFlame extends Quest {

	public static final int QUEST_ID = 618;
	public static final String QUEST_NAME = "618_IntoTheFlame";

	// NPCs
	public static final int KLEIN = 31540;
	public static final int HILDA = 31271;

	// Monstros (Kookaburras, Bandersnatches, Grendels)
	public static final int KINKU = 21274;
	public static final int KINKU_LEADER = 21275;
	public static final int KINKU_SCOUT = 21276;
	public static final int[] MOBS = {
			21274, 21275, 21276, 21277, 21282, 21283, 21284, 21285, 21290, 21291, 21292, 21293
	};

	// Itens
	public static final int VACUALITE_ORE = 7265;
	public static final int VACUALITE = 7266;
	public static final int FLOATING_STONE = 7267;

	public Quest618IntoTheFlame() {
		this(null);
	}

	@Autowired
	public Quest618IntoTheFlame(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Into the Flame");

		addStartNpc(KLEIN);
		addTalkId(KLEIN, HILDA);

		addKillId(MOBS);

		registerQuestItems(VACUALITE_ORE, VACUALITE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return null;
		}
		return onEvent(event, qs);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("31540-03.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31540-03.htm";
		} else if ("31540-05.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(VACUALITE) > 0 && cond == 4) {
				qs.takeItems(VACUALITE, 1);
				qs.giveItems(FLOATING_STONE, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "31540-05.htm";
			}
			return "31540-03.htm";
		} else if ("31271-02.htm".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "31271-02.htm";
		} else if ("31271-05.htm".equalsIgnoreCase(event)) {
			if ((cond == 2 || cond == 3) && qs.getQuestItemsCount(VACUALITE_ORE) >= 50) {
				qs.takeItems(VACUALITE_ORE, -1);
				qs.giveItems(VACUALITE, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31271-05.htm";
			}
			return "31271-03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player != null ? player.getQuestState(QUEST_NAME) : null;
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		return onTalk(npc, qs);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter c = qs.playerChar();
		if (c == null) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == KLEIN) {
			if (cond == 0) {
				if (c.level() >= 60) {
					return "31540-02.htm";
				} else {
					return "31540-01.htm";
				}
			} else if (cond == 4 && qs.getQuestItemsCount(VACUALITE) > 0) {
				return "31540-04.htm";
			}
			return "31540-03.htm";
		} else if (npcId == HILDA) {
			if (cond == 1) {
				return "31271-01.htm";
			} else if (cond == 2) {
				if (qs.getQuestItemsCount(VACUALITE_ORE) >= 50) {
					qs.setCond(3);
					return "31271-04.htm";
				}
				return "31271-03.htm";
			} else if (cond == 3) {
				return "31271-04.htm";
			} else if (cond == 4) {
				return "31271-06.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player != null ? player.getQuestState(QUEST_NAME) : null;
		if (qs == null) {
			return null;
		}
		return onKill(npc, qs, isPet);
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		if (qs.getCond() == 2 && qs.getQuestItemsCount(VACUALITE_ORE) < 50) {
			if (ThreadLocalRandom.current().nextInt(100) < 50) {
				qs.giveItems(VACUALITE_ORE, 1);
				if (qs.getQuestItemsCount(VACUALITE_ORE) >= 50) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
