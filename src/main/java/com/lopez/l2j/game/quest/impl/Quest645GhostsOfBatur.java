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
 * Quest 645 - 645_GhostsOfBatur
 */
@Component
public class Quest645GhostsOfBatur extends Quest {

	public static final int KARUDA = 32017;
	public static final int aEL = KARUDA;
	public static final int CURSED_GRAVE_GOODS = 8089;
	public static final int bTy = CURSED_GRAVE_GOODS;

	public static final int[][] REWARDS = new int[][]{
			{1878, 18},
			{1879, 7},
			{1880, 4},
			{1881, 6},
			{1882, 10},
			{1883, 2}
	};

	public static final int[] MOBS = new int[]{
			22007, 22009, 22010, 22011, 22012, 22013, 22014, 22015, 22016
	};

	@Autowired
	public Quest645GhostsOfBatur(QuestManager questManager) {
		super(645, "645_GhostsOfBatur", "Ghosts of Batur");
		this.addStartNpc(KARUDA);
		this.addTalkId(KARUDA);
		for (int n : MOBS) {
			this.addKillId(n);
		}
		this.addQuestItem(CURSED_GRAVE_GOODS);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)
				|| "32017-03.htm".equalsIgnoreCase(event)
				|| "30501-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30501-03.htm".equalsIgnoreCase(event) ? "30501-03.htm" : "32017-03.htm";
		} else if ("32017-06.htm".equalsIgnoreCase(event) || "30501-06.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("0".equalsIgnoreCase(event)
				|| "1".equalsIgnoreCase(event)
				|| "2".equalsIgnoreCase(event)
				|| "3".equalsIgnoreCase(event)
				|| "4".equalsIgnoreCase(event)
				|| "5".equalsIgnoreCase(event)) {
			int idx = Integer.parseInt(event);
			if (qs.getQuestItemsCount(CURSED_GRAVE_GOODS) >= 180L) {
				qs.takeItems(CURSED_GRAVE_GOODS, -1L);
				qs.giveItems(REWARDS[idx][0], REWARDS[idx][1], true);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "32017-07.htm";
			} else {
				qs.setCond(1);
				return "32017-04.htm";
			}
		} else if (qs.getCond() == 2) {
			if (qs.getQuestItemsCount(CURSED_GRAVE_GOODS) >= 180L) {
				for (int i = 0; i < REWARDS.length; ++i) {
					if (!event.equalsIgnoreCase(String.valueOf(REWARDS[i][0]))) continue;
					qs.takeItems(CURSED_GRAVE_GOODS, -1L);
					qs.giveItems(REWARDS[i][0], REWARDS[i][1], true);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(true);
					return "32017-07.htm";
				}
			} else {
				qs.setCond(1);
				return "32017-04.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = qs.getCond();
		if (n == 0) {
			if (pc.getLevel() < 23 || pc.getLevel() > 36) {
				html = "32017-02.htm";
				qs.exitQuest(true);
			} else {
				html = "32017-01.htm";
			}
		} else if (n == 1) {
			html = "32017-04.htm";
		} else if (n == 2) {
			html = qs.getQuestItemsCount(CURSED_GRAVE_GOODS) >= 180L ? "32017-05.htm" : "32017-01.htm";
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1 && qs.getQuestItemsCount(CURSED_GRAVE_GOODS) < 180L && qs.rollAndGive(CURSED_GRAVE_GOODS, 1, 2, 180, 70.0)) {
			qs.setCond(2);
			qs.setState(State.STARTED);
		}
		return null;
	}
}
