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
 * Quest 340 - 340_SubjugationofLizardmen
 */
@Component
public class Quest340SubjugationofLizardmen extends Quest {

	public static final int bmN = 30385; // WEISZ
	public static final int bmO = 30037; // LEVIAN
	public static final int bmP = 30375; // ADONIUS
	public static final int bmQ = 30989; // CHEST_OF_BIFRONS
	public static final int bmR = 20008;
	public static final int bmS = 20010;
	public static final int bmT = 20014;
	public static final int bmU = 20027;
	public static final int bmV = 20024;
	public static final int bmW = 25146;
	public static final int bmX = 4257; // SINISTER_TOTEM
	public static final int bmY = 4256; // HOLY_SYMBOL
	public static final int bmZ = 4255; // TRADE_CARGO
	public static final int SACRED_INCENSE_POUCH = 4258;

	public Quest340SubjugationofLizardmen(QuestManager questManager) {
		super(340, "340_SubjugationofLizardmen", "340_SubjugationofLizardmen");
		addStartNpc(bmN);
		addTalkId(bmN, bmO, bmP, bmQ);
		registerQuestItems(4255, 4256, 4257, 4258);
		addKillId(bmR, bmS, bmT, bmV, bmU, 20030, 20356, 20357, 21100, 21101, bmW);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		int cond = qs.getCond();
		int n2 = qs.getInt("subj_lizardmen");

		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event) || "30385-03.htm".equalsIgnoreCase(event)) {
			if (pc == null || pc.getLevel() >= 17) {
				qs.setCond(1);
				qs.set("subj_lizardmen", "1", true);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				return "30385-03.htm".equalsIgnoreCase(event) ? "30385-03.htm" : "guard_weisz_q0340_03.htm";
			}
			return "guard_weisz_q0340_02.htm";
		} else if ("menu_select?ask=340&reply=1".equalsIgnoreCase(event)) {
			if (cond == 2 || n2 == 2) {
				qs.setCond(3);
				qs.set("subj_lizardmen", "3", true);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "priest_adonius_q0340_02.htm";
			} else if (cond == 4 || n2 == 4) {
				qs.setCond(5);
				qs.set("subj_lizardmen", "5", true);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "levian_q0340_02.htm";
			} else if (cond == 5 || n2 == 5) {
				qs.setCond(6);
				qs.set("subj_lizardmen", "6", true);
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.giveItems(SACRED_INCENSE_POUCH, 1L);
				return "chest_of_bifrons_q0340_02.htm";
			}
			return "guard_weisz_q0340_04.htm";
		} else if ("menu_select?ask=340&reply=2".equalsIgnoreCase(event) || "30385-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(bmZ, -1L);
			qs.set("subj_lizardmen", "2", true);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30385-07.htm".equalsIgnoreCase(event) ? "30385-07.htm" : "guard_weisz_q0340_07.htm";
		} else if ("menu_select?ask=340&reply=3".equalsIgnoreCase(event)) {
			return "guard_weisz_q0340_08.htm";
		} else if ("menu_select?ask=340&reply=4".equalsIgnoreCase(event) || "30385-09.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(bmZ) >= 30L) {
				qs.giveItems(57, 4090L);
				qs.takeItems(bmZ, -1L);
				qs.set("subj_lizardmen", "1", true);
				return "30385-09.htm".equalsIgnoreCase(event) ? "30385-09.htm" : "guard_weisz_q0340_09.htm";
			}
		} else if ("menu_select?ask=340&reply=5".equalsIgnoreCase(event) || "30385-10.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(bmZ) >= 30L) {
				qs.giveItems(57, 4090L);
				qs.takeItems(bmZ, -1L);
				qs.unset("subj_lizardmen");
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30385-10.htm".equalsIgnoreCase(event) ? "30385-10.htm" : "guard_weisz_q0340_10.htm";
			}
		} else if ("30375-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.set("subj_lizardmen", "3", true);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30375-02.htm";
		} else if ("30037-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.set("subj_lizardmen", "5", true);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30037-02.htm";
		} else if ("30989-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.set("subj_lizardmen", "6", true);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.giveItems(SACRED_INCENSE_POUCH, 1L);
			return "30989-02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = qs.getInt("subj_lizardmen");
		if (n == 0 && qs.getCond() > 0) {
			n = qs.getCond();
		}
		int n2 = npc != null ? npc.getNpcId() : 0;
		int n3 = qs.getStateId();
		switch (n3) {
			case 1: {
				if (n2 != bmN) break;
				if (pc.getLevel() < 17) {
					html = "guard_weisz_q0340_01.htm";
					qs.exitQuest(true);
					break;
				}
				html = "guard_weisz_q0340_02.htm";
				break;
			}
			case 2: {
				if (n2 == bmN) {
					if (n == 1 && qs.getQuestItemsCount(bmZ) < 30L) {
						html = "guard_weisz_q0340_05.htm";
					} else if (n == 1 && qs.getQuestItemsCount(bmZ) >= 30L) {
						html = "guard_weisz_q0340_06.htm";
					} else if (n == 2) {
						html = "guard_weisz_q0340_11.htm";
					} else if (n >= 3 && n < 7) {
						html = "guard_weisz_q0340_12.htm";
					} else if (n == 7) {
						qs.giveItems(57, 14700L);
						qs.unset("subj_lizardmen");
						qs.playSound(QuestState.SOUND_FINISH);
						qs.exitQuest(false);
						html = "guard_weisz_q0340_13.htm";
					}
				} else if (n2 == bmO) {
					if (n == 4) {
						html = "levian_q0340_01.htm";
					} else if (n == 5) {
						html = "levian_q0340_03.htm";
					} else if (n == 6) {
						qs.takeItems(SACRED_INCENSE_POUCH, -1L);
						qs.set("subj_lizardmen", "7", true);
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "levian_q0340_04.htm";
					} else if (n == 7) {
						html = "levian_q0340_05.htm";
					}
				} else if (n2 == bmP) {
					if (n == 2) {
						html = "priest_adonius_q0340_01.htm";
					} else if (n == 3 && (qs.getQuestItemsCount(bmY) == 0L || qs.getQuestItemsCount(bmX) == 0L)) {
						html = "priest_adonius_q0340_03.htm";
					} else if (n == 3 && qs.getQuestItemsCount(bmY) >= 1L && qs.getQuestItemsCount(bmX) >= 1L) {
						qs.takeItems(bmY, -1L);
						qs.takeItems(bmX, -1L);
						qs.setCond(4);
						qs.set("subj_lizardmen", "4", true);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "priest_adonius_q0340_04.htm";
					} else if (n == 4) {
						html = "priest_adonius_q0340_05.htm";
					} else if (n >= 5) {
						html = "priest_adonius_q0340_06.htm";
					}
				} else if (n2 == bmQ && n == 5) {
					html = "chest_of_bifrons_q0340_01.htm";
				}
				break;
			}
			case 3: {
				html = getAlreadyCompletedMsg();
				break;
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null || qs.getState() != State.STARTED) {
			return null;
		}
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getInt("subj_lizardmen");
		if (n2 == 0) {
			n2 = qs.getCond();
		}
		if (n == bmR || n == bmS || n == bmT) {
			if (n2 == 1 && qs.getQuestItemsCount(bmZ) < 30L && ThreadLocalRandom.current().nextInt(100) < 63) {
				qs.giveItems(bmZ, 1L);
				if (qs.getQuestItemsCount(bmZ) >= 30L) {
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (n == bmU || n == bmV || n == 20030 || n == 20356 || n == 20357 || n == 21100 || n == 21101) {
			if (n2 == 3) {
				if (qs.getQuestItemsCount(bmY) == 0L) {
					if (ThreadLocalRandom.current().nextInt(100) <= 18) {
						qs.giveItems(bmY, 1L);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				} else if (qs.getQuestItemsCount(bmX) == 0L && ThreadLocalRandom.current().nextInt(100) <= 18) {
					qs.giveItems(bmX, 1L);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		} else if (n == bmW) {
			qs.addSpawn(bmQ);
		}
		return null;
	}
}
