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
 * Quest 202 - Hmage Tutorial
 */
@Component
public class Quest202HmageTutorial extends Quest {

	public static final int aJo = 30017;
	public static final int aJp = 30019;
	public static final int aJi = 18342;
	public static final int aJq = 1068;
	public static final int aJk = 6353;
	public static final int ayb = 5790;
	public static final int aya = 5789;
	public static final int aJr = 10;
	public static final int aJn = 0x100000;

	public Quest202HmageTutorial(QuestManager questManager) {
		super(202, "202_HmageTutorial", "Tutorial");
		addTalkId(30017, 30018, 30019, 30020, 30021);
		addKillId(18342, 20001, 20130);
		addFirstTalkId(30017, 30018, 30019, 30020, 30021);
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
		if (event.equalsIgnoreCase("timer_newbie_helper")) {
			if (n == 0) {
				qs.playTutorialVoice("tutorial_voice_009b");
				setTutorialVar(qs, qs2, "tutorial_quest_ex", "1");
			}
			if (n == 3) {
				qs.playTutorialVoice("tutorial_voice_010b");
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
		if (event.equalsIgnoreCase("reply_31") && qs.getQuestItemsCount(1068) > 0) {
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
			event2 = "gallin002.htm";
			qs.takeItems(1068, -1);
			qs.startQuestTimer("timer_grand_master", 60000);
			if (n <= 3) {
				setTutorialVar(qs, qs2, "tutorial_quest_ex", "4");
			}
			qs.addRadar(-84081, 243277, -3723);
		} else if (event.equalsIgnoreCase("reply_42")) {
			event2 = "gallin006.htm";
			qs.addRadar(-84081, 243277, -3723);
			qs.setState(State.COMPLETED);
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
		boolean bl = pc.race() != 3 && pc.isMage();
		QuestState qs255 = getTutorialQuestState(qs);
		int n2 = pc.classId();
		int n3 = getTutorialEx(qs, qs255);
		int n4 = qs255 != null && qs255.get("tutorial_quest") != null ? qs255.getInt("tutorial_quest") : qs.getInt("tutorial_quest");
		switch (n) {
			case 30018, 30019, 30020, 30021 -> {
				int n5 = n4 & 0x7FFFFF00;
				if (n3 < 0) {
					if (n2 == 10 && pc.race() == 0) {
						qs.setState(State.STARTED);
						qs.startQuestTimer("timer_newbie_helper", 30000);
						html = "doff001.htm";
						setTutorialVar(qs, qs255, "tutorial_quest_ex", "0");
						qs.onTutorialClientEvent(n5 | 0x100000);
						break;
					}
					html = "doff006.htm";
					break;
				}
				if ((n3 == 1 || n3 == 2 || n3 == 0) && qs.getQuestItemsCount(6353) < 1) {
					html = "doff002.htm";
					break;
				}
				if ((n3 == 1 || n3 == 2 || n3 == 0) && qs.getQuestItemsCount(6353) > 0) {
					html = "doff003.htm";
					qs.takeItems(6353, -1);
					setTutorialVar(qs, qs255, "tutorial_quest_ex", "3");
					qs.giveItems(1068, 1);
					qs.startQuestTimer("timer_newbie_helper", 30000);
					setTutorialVar(qs, qs255, "tutorial_quest", String.valueOf(n5 | 4));
					qs.addRadar(-91036, 248044, -3568);
					if (!bl || qs.getQuestItemsCount(5790) > 0) break;
					qs.playTutorialVoice("tutorial_voice_027");
					qs.giveItems(5790, 100);
					break;
				}
				if (n3 == 3) {
					html = "doff004.htm";
					break;
				}
				if (n3 <= 3) break;
				html = "doff005.htm";
			}
			case 30017 -> {
				if (qs.getQuestItemsCount(1068) > 0) {
					html = "gallin001.htm";
					break;
				}
				if (qs.getQuestItemsCount(1068) == 0 && n3 > 3) {
					html = "gallin004.htm";
					break;
				}
				if (qs.getQuestItemsCount(1068) != 0 || n3 > 3) break;
				html = "gallin003.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null) return null;
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		QuestState qs255 = getTutorialQuestState(qs);
		int n = getTutorialEx(qs, qs255);
		if (n < 0) n = 0;
		if (n == 0 || n == 1) {
			qs.playTutorialVoice("tutorial_voice_011");
			qs.showQuestionMark(3);
			setTutorialVar(qs, qs255, "tutorial_quest", "2");
		}
		if ((n == 0 || n == 1 || n == 2) && qs.getQuestItemsCount(6353) < 1) {
			qs.giveItems(6353, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.playTutorialVoice("tutorial_voice_013");
			qs.showQuestionMark(5);
		}
		return null;
	}

}
