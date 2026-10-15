package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 161 - Fruits Of Mothertree
 */
@Component
public class Quest161FruitsOfMothertree extends Quest {

	public Quest161FruitsOfMothertree(QuestManager questManager) {
		super(161, "161_FruitsOfMothertree", "Fruits Of Mothertree");
		addStartNpc(30362);
		addTalkId(30362, 30371);
		registerQuestItems(1037, 1036);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30362-04.htm".equalsIgnoreCase(event)) {
			qs.giveItems(1036, 1);
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30362-04.htm".equalsIgnoreCase(event) ? "30362-04.htm" : "andellria_q0312_04.htm";
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
				if (pc.getRace() != Race.elf) {
					html = "andellria_q0312_00.htm";
					qs.exitQuest(true);
					break;
				}
				if (pc.level() >= 3) {
					html = "andellria_q0312_03.htm";
					break;
				}
				html = "andellria_q0312_02.htm";
				qs.exitQuest(true);
				break;
			}
			case 2: {
				if (n == 30362) {
					if (qs.getQuestItemsCount(1036) == 1 && qs.getQuestItemsCount(1037) == 0) {
						html = "andellria_q0312_05.htm";
						break;
					}
					if (qs.getQuestItemsCount(1037) != 1) break;
					html = "andellria_q0312_06.htm";
					qs.giveItems(57, 1000);
					qs.addExpAndSp(1000, 0);
					qs.takeItems(1037, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					break;
				}
				if (n != 30371) break;
				if (qs.getQuestItemsCount(1036) == 1) {
					html = "thalya_q0312_01.htm";
					qs.giveItems(1037, 1);
					qs.takeItems(1036, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					break;
				}
				if (qs.getQuestItemsCount(1037) != 1) break;
				html = "thalya_q0312_02.htm";
			}
		}
		return html;
	}
}
