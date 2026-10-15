package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 152 - Shards Of Golem
 */
@Component
public class Quest152ShardsOfGolem extends Quest {

	public Quest152ShardsOfGolem(QuestManager questManager) {
		super(152, "152_ShardsOfGolem", "Shards Of Golem");
		addStartNpc(30035);
		addTalkId(30035, 30283);
		addKillId(20016);
		registerQuestItems(1008, 1009, 1010, 1011);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30035-02.htm".equalsIgnoreCase(event) || "30035-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			if (qs.getQuestItemsCount(1008) == 0) {
				qs.giveItems(1008, 1);
			}
			return "quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) ? "harry_q0152_04.htm" : event;
		} else if ("reply=2".equalsIgnoreCase(event) || "30283-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.takeItems(1008, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			if (qs.getQuestItemsCount(1009) == 0) {
				qs.giveItems(1009, 1);
			}
			return "30283-02.htm".equalsIgnoreCase(event) ? "30283-02.htm" : "blacksmith_alltran_q0152_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "no-quest";
		int n = npc.getNpcId();
		int n2 = qs.getStateId();
		switch (n2) {
			case 1: {
				if (n != 30035) break;
				if (pc.level() < 10 || pc.level() > 17) {
					html = "harry_q0152_02.htm";
					qs.exitQuest(true);
					break;
				}
				html = "harry_q0152_03.htm";
				break;
			}
			case 2: {
				if (n == 30035) {
					if (qs.getQuestItemsCount(1008) != 0 && qs.getQuestItemsCount(1011) == 0) {
						html = "harry_q0152_05a.htm";
						break;
					}
					if (qs.getQuestItemsCount(1009) != 0 && qs.getQuestItemsCount(1011) == 0) {
						html = "harry_q0152_05.htm";
						break;
					}
					if (qs.getQuestItemsCount(1011) == 0) break;
					qs.takeItems(1011, -1);
					qs.takeItems(1009, -1);
					qs.giveItems(23, 1);
					qs.addExpAndSp(5000, 0);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					html = "harry_q0152_06.htm";
					break;
				}
				if (n != 30283) break;
				if (qs.getQuestItemsCount(1008) != 0) {
					html = "blacksmith_alltran_q0152_01.htm";
					break;
				}
				if (qs.getQuestItemsCount(1009) != 0 && qs.getQuestItemsCount(1010) < 5 && qs.getQuestItemsCount(1011) == 0) {
					html = "blacksmith_alltran_q0152_03.htm";
					break;
				}
				if (qs.getQuestItemsCount(1009) != 0 && qs.getQuestItemsCount(1010) >= 5 && qs.getQuestItemsCount(1011) == 0) {
					qs.setCond(4);
					qs.takeItems(1010, -1);
					if (qs.getQuestItemsCount(1011) == 0) {
						qs.giveItems(1011, 1);
					}
					qs.playSound(QuestState.SOUND_MIDDLE);
					html = "blacksmith_alltran_q0152_04.htm";
					break;
				}
				if (qs.getQuestItemsCount(1009) == 0 || qs.getQuestItemsCount(1011) == 0) break;
				html = "blacksmith_alltran_q0152_05.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc.getNpcId();
		if (n == 20016 && qs.getQuestItemsCount(1010) < 5 && ThreadLocalRandom.current().nextInt(100) < 30) {
			qs.giveItems(1010, 1);
			if (qs.getQuestItemsCount(1010) >= 5) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
