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
 * Quest 205 - Orc Tutorial
 */
@Component
public class Quest205OrcTutorial extends Quest {

	public static final int aJx = 30573;
	public static final int aJy = 30575;
	public static final int aJi = 18342;
	public static final int aJz = 1496;
	public static final int aJk = 6353;
	public static final int aya = 5789;
	public static final int ayb = 5790;
	public static final int aJm = 49;
	public static final int aJn = 0x100000;

	public Quest205OrcTutorial(QuestManager questManager) {
		super(205, "205_OrcTutorial", "Tutorial");
		addTalkId(30573, 30574, 30575);
		addKillId(18342, 20419);
		addFirstTalkId(30573, 30574, 30575);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private QuestState getTutorialQuestState(QuestState qs) {
		if (qs == null || qs.getPlayer() == null) {
			return qs;
		}
		QuestState qs255 = qs.getPlayer().getQuestState(Quest255Tutorial.QUEST_NAME);
		return qs255 != null ? qs255 : qs;
	}

	private int getTutorialEx(QuestState qs, QuestState qs255) {
		if (qs255 != null && qs255.get("tutorial_quest_ex") != null) {
			return qs255.getInt("tutorial_quest_ex");
		}
		return qs.get("tutorial_quest_ex") == null ? -1 : qs.getInt("tutorial_quest_ex");
	}

	private void setTutorialVar(QuestState qs, QuestState qs255, String key, String val) {
		qs.set(key, val, true);
		if (qs255 != null && qs255 != qs) {
			qs255.set(key, val, true);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		if (pc == null) {
			return null;
		}
		String event2 = event;
		QuestState qs2 = getTutorialQuestState(qs);
		int n = getTutorialEx(qs, qs2);
		int n3 = pc.isMage() ? 1 : 0;
		if (event.equalsIgnoreCase("timer_newbie_helper")) {
			if (n == 0) {
				if (n3 == 0) {
					qs.playTutorialVoice("tutorial_voice_009a");
				} else {
					qs.playTutorialVoice("tutorial_voice_009c");
				}
				setTutorialVar(qs, qs2, "tutorial_quest_ex", "1");
			}
			if (n == 3) {
				qs.playTutorialVoice("tutorial_voice_010e");
			}
			return null;
		}
		if (event.equalsIgnoreCase("timer_grand_master")) {
			if (n >= 4) {
				qs.showQuestionMark(7);
				qs.playSound("ItemSound.quest_tutorial");
				qs.playTutorialVoice("tutorial_voice_025");
			}
			return null;
		}
		if (event.equalsIgnoreCase("reply_31") && qs.getQuestItemsCount(1496) > 0) {
			if (!pc.isMage() && qs.getQuestItemsCount(5789) <= 200) {
				qs.giveItems(5789, 200);
				qs.playTutorialVoice("tutorial_voice_026");
				qs.addExpAndSp(0, 50);
			}
			if (pc.isMage() && qs.getQuestItemsCount(5789) <= 200 && qs.getQuestItemsCount(5790) <= 100) {
				if (pc.classId() == 49) {
					qs.giveItems(5789, 200);
					qs.playTutorialVoice("tutorial_voice_026");
				} else {
					qs.giveItems(5790, 100);
					qs.playTutorialVoice("tutorial_voice_027");
				}
				qs.addExpAndSp(0, 50);
			}
			event2 = "guardian_vullkus002.htm";
			qs.takeItems(1496, 1);
			qs.startQuestTimer("timer_grand_master", 60000);
			if (n <= 3) {
				setTutorialVar(qs, qs2, "tutorial_quest_ex", "4");
			}
			qs.addRadar(-45032, -113598, -192);
		} else if (event.equalsIgnoreCase("reply_42")) {
			event2 = "guardian_vullkus006.htm";
			qs.addRadar(-45032, -113598, -192);
			qs.setState(State.COMPLETED);
			setTutorialVar(qs, qs2, "tutorial_quest_ex", "4");
			setTutorialVar(qs, qs2, "Gemstone", "1");
		}
		return event2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return onFirstTalk(npc, qs);
	}

	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) return "noquest";
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		QuestState qs255 = getTutorialQuestState(qs);
		int n2 = pc.classId();
		int n3 = getTutorialEx(qs, qs255);
		int n4 = qs255 != null && qs255.get("tutorial_quest") != null ? qs255.getInt("tutorial_quest") : qs.getInt("tutorial_quest");
		switch (n) {
			case 30574, 30575 -> {
				int n5 = n4 & 0x7FFFFF00;
				if (n3 < 0) {
					if (pc.race() == 3) {
						qs.setState(State.STARTED);
						qs.startQuestTimer("timer_newbie_helper", 30000);
						html = n2 == 49 ? "shela_priestess_of_fire001.htm" : "carl001.htm";
						setTutorialVar(qs, qs255, "tutorial_quest_ex", "0");
						qs.onTutorialClientEvent(n5 | 0x100000);
						break;
					}
					html = "carl006.htm";
					break;
				}
				if ((n3 == 1 || n3 == 2 || n3 == 0) && qs.getQuestItemsCount(6353) < 1) {
					if (n2 == 49) {
						html = "shela_priestess_of_fire002.htm";
						break;
					}
					html = "carl002.htm";
					break;
				}
				if ((n3 == 1 || n3 == 2 || n3 == 0) && qs.getQuestItemsCount(6353) > 0) {
					qs.takeItems(6353, -1);
					setTutorialVar(qs, qs255, "tutorial_quest_ex", "3");
					setTutorialVar(qs, qs255, "Gemstone", "1");
					qs.giveItems(1496, 1);
					qs.startQuestTimer("timer_newbie_helper", 30000);
					setTutorialVar(qs, qs255, "tutorial_quest", String.valueOf(n5 | 4));
					qs.addRadar(-56736, -113680, -672);
					if (n2 != 49) {
						if (qs.getQuestItemsCount(5789) <= 0) {
							qs.giveItems(5789, 200);
						}
						qs.playTutorialVoice("tutorial_voice_026");
						html = "shela_priestess_of_fire003f.htm";
					}
					if (n2 != 49) break;
					if (qs.getQuestItemsCount(5790) <= 0) {
						qs.giveItems(5790, 100);
					}
					qs.playTutorialVoice("tutorial_voice_027");
					html = "shela_priestess_of_fire003m.htm";
					break;
				}
				if (n3 == 3) {
					html = "shela_priestess_of_fire004.htm";
					break;
				}
				if (n3 <= 3) break;
				html = "carl005.htm";
			}
			case 30573 -> {
				if (qs.getQuestItemsCount(1496) > 0) {
					html = "guardian_vullkus001.htm";
					break;
				}
				if (qs.getQuestItemsCount(1496) == 0 && n3 > 3) {
					html = "guardian_vullkus004.htm";
					break;
				}
				if (qs.getQuestItemsCount(1496) != 0 || n3 > 3) break;
				html = "guardian_vullkus003.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || qs.isCompleted()) return null;
		PlayerCharacter pc = qs.playerChar();
		if (pc == null || pc.level() >= 6) return null;

		QuestState qs255 = getTutorialQuestState(qs);
		int n = getTutorialEx(qs, qs255);
		if (n >= 3) return null;
		if (n < 0) n = 0;
		if (n == 0 || n == 1) {
			qs.playTutorialVoice("tutorial_voice_011");
			qs.showQuestionMark(3);
			setTutorialVar(qs, qs255, "tutorial_quest", "2");
		}
		if ((n == 0 || n == 1 || n == 2)
				&& qs.getQuestItemsCount(6353) < 1
				&& (qs255 == null || (qs255.getQuestItemsCount(6353) < 1 && qs255.getInt("Gemstone") == 0))
				&& qs.getInt("Gemstone") == 0) {
			qs.giveItems(6353, 1);
			setTutorialVar(qs, qs255, "Gemstone", "1");
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.playTutorialVoice("tutorial_voice_013");
			qs.showQuestionMark(5);
		}
		return null;
	}

}
