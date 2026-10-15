package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 260 - Hunt The Orcs
 */
@Component
public class Quest260HuntTheOrcs extends Quest {

	public static final int RAYEN = 30221;
	public static final int bdy = 30221;
	public static final int ORC_AMULET = 1114;
	public static final int ORC_NECKLACE = 1115;

	public Quest260HuntTheOrcs(QuestManager questManager) {
		super(260, "260_HuntTheOrcs", "Hunt The Orcs");
		addStartNpc(30221);
		addTalkId(30221);
		addKillId(20468, 20469, 20470, 20471, 20472, 20473);
		registerQuestItems(1114, 1115);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

		String event2 = event;
		if (event.equalsIgnoreCase("sentinel_rayjien_q0260_03.htm") || event.equalsIgnoreCase("30221-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("sentinel_rayjien_q0260_06.htm") || event.equalsIgnoreCase("30221-06.htm")) {
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		}
		return event2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getStateId();
		String html = "noquest";
		switch (n2) {
			case 1: {
				if (n != RAYEN) break;
				if (pc.level() >= 6 && pc.race() == 1) {
					html = "sentinel_rayjien_q0260_02.htm";
					break;
				}
				if (pc.race() != 1) {
					html = "sentinel_rayjien_q0260_00.htm";
					break;
				}
				if (pc.level() >= 6) break;
				html = "sentinel_rayjien_q0260_01.htm";
				break;
			}
			case 2: {
				if (n != RAYEN) break;
				if (qs.getQuestItemsCount(ORC_AMULET) == 0 && qs.getQuestItemsCount(ORC_NECKLACE) == 0) {
					html = "sentinel_rayjien_q0260_04.htm";
					break;
				}
				if (qs.getQuestItemsCount(ORC_AMULET) <= 0 && qs.getQuestItemsCount(ORC_NECKLACE) <= 0) break;
				html = "sentinel_rayjien_q0260_05.htm";
				if (qs.getQuestItemsCount(ORC_AMULET) + qs.getQuestItemsCount(ORC_NECKLACE) >= 10) {
					qs.giveItems(57, qs.getQuestItemsCount(ORC_AMULET) * 12 + qs.getQuestItemsCount(ORC_NECKLACE) * 30 + 1000);
				} else {
					qs.giveItems(57, qs.getQuestItemsCount(ORC_AMULET) * 12 + qs.getQuestItemsCount(ORC_NECKLACE) * 30);
				}
				qs.takeAllItems(ORC_AMULET, ORC_NECKLACE);
				if (pc.level() < 25 && qs.getInt("tutorial_quest_ex") != 10) {
					qs.showQuestionMark(26);
					qs.set("tutorial_quest_ex", "10");
					if (pc.isMage()) {
						qs.playTutorialVoice("tutorial_voice_027");
						qs.giveItems(5790, 3000);
					} else {
						qs.playTutorialVoice("tutorial_voice_026");
						qs.giveItems(5789, 6000);
					}
				}
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc != null ? npc.getNpcId() : 0;
		if (qs.getCond() > 0) {
			if (n == 20468 || n == 20469 || n == 20470) {
				qs.rollAndGive(ORC_AMULET, 1, 14.0);
			} else if (n == 20471 || n == 20472 || n == 20473) {
				qs.rollAndGive(ORC_NECKLACE, 1, 14.0);
			}
		}
		return null;
	}
}
