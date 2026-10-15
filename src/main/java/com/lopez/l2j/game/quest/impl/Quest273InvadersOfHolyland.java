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
 * Quest 273 - Invaders Of Holyland
 */
@Component
public class Quest273InvadersOfHolyland extends Quest {

	public static final int VARKEES = 30566;
	public static final int BLACK_SOULSTONE = 1475;
	public static final int RED_SOULSTONE = 1476;

	public static final int bee = 30566;
	public static final int bef = 1475;
	public static final int beg = 1476;

	public Quest273InvadersOfHolyland(QuestManager questManager) {
		super(273, "273_InvadersOfHolyland", "Invaders Of Holyland");
		addStartNpc(30566);
		addTalkId(30566);
		addKillId(20311, 20312, 20313);
		registerQuestItems(1475, 1476);
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
		if (event.equalsIgnoreCase("atuba_chief_varkees_q0273_03.htm") || event.equalsIgnoreCase("30566-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("atuba_chief_varkees_q0273_07.htm") || event.equalsIgnoreCase("30566-07.htm")) {
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		} else if (event.equalsIgnoreCase("atuba_chief_varkees_q0273_08.htm") || event.equalsIgnoreCase("30566-08.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		return event2;
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
				if (n != VARKEES) break;
				if (pc.race() != 3) {
					html = "atuba_chief_varkees_q0273_00.htm";
					qs.exitQuest(true);
					break;
				}
				if (pc.level() < 6) {
					html = "atuba_chief_varkees_q0273_01.htm";
					qs.exitQuest(true);
					break;
				}
				html = "atuba_chief_varkees_q0273_02.htm";
				break;
			}
			case 2: {
				if (n != VARKEES) break;
				if (qs.getQuestItemsCount(BLACK_SOULSTONE) + qs.getQuestItemsCount(RED_SOULSTONE) == 0) {
					html = "atuba_chief_varkees_q0273_04.htm";
					break;
				}
				if (qs.getQuestItemsCount(RED_SOULSTONE) == 0) {
					long l = qs.getQuestItemsCount(BLACK_SOULSTONE);
					html = "atuba_chief_varkees_q0273_05.htm";
					if (qs.getQuestItemsCount(BLACK_SOULSTONE) >= 10) {
						qs.giveItems(57, l * 3 + 1500);
					} else {
						qs.giveItems(57, l * 3);
					}
					qs.takeItems(BLACK_SOULSTONE, l);
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
					qs.exitQuest(true);
					qs.playSound(QuestState.SOUND_FINISH);
					break;
				}
				html = "atuba_chief_varkees_q0273_06.htm";
				if (qs.getQuestItemsCount(BLACK_SOULSTONE) + qs.getQuestItemsCount(RED_SOULSTONE) >= 10) {
					qs.giveItems(57, qs.getQuestItemsCount(RED_SOULSTONE) * 10 + qs.getQuestItemsCount(BLACK_SOULSTONE) * 3 + 1800);
				} else {
					qs.giveItems(57, qs.getQuestItemsCount(RED_SOULSTONE) * 10 + qs.getQuestItemsCount(BLACK_SOULSTONE) * 3);
				}
				qs.takeAllItems(BLACK_SOULSTONE, RED_SOULSTONE);
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
				qs.exitQuest(true);
				qs.playSound(QuestState.SOUND_FINISH);
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getCond();
		if (n == 20311) {
			if (n2 == 1) {
				if (ThreadLocalRandom.current().nextInt(100) < 90) {
					qs.giveItems(BLACK_SOULSTONE, 1);
				} else {
					qs.giveItems(RED_SOULSTONE, 1);
				}
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (n == 20312) {
			if (n2 == 1) {
				if (ThreadLocalRandom.current().nextInt(100) < 87) {
					qs.giveItems(BLACK_SOULSTONE, 1);
				} else {
					qs.giveItems(RED_SOULSTONE, 1);
				}
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (n == 20313 && n2 == 1) {
			if (ThreadLocalRandom.current().nextInt(100) < 77) {
				qs.giveItems(BLACK_SOULSTONE, 1);
			} else {
				qs.giveItems(RED_SOULSTONE, 1);
			}
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
