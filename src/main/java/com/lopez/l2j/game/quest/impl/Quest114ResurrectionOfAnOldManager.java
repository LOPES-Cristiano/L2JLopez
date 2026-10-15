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
 * Quest 114: Resurrection of an Old Manager
 */
@Component
public class Quest114ResurrectionOfAnOldManager extends Quest {

	public static final int QUEST_ID = 114;
	public static final String QUEST_NAME = "114_ResurrectionOfAnOldManager";
	public static final int START_NPC = 32041;

	public static final int NEWYEAR = 31961;
	public static final int YUMI = 32041;
	public static final int STONE = 32046;
	public static final int WENDY = 32047;
	public static final int BOX = 32050;

	public static final int GOLEM = 27318;

	public static final int DETECTOR = 8090;
	public static final int DETECTOR_2 = 8091;
	public static final int STARSTONE = 8287;
	public static final int LETTER = 8288;
	public static final int STARSTONE_2 = 8289;

	@Autowired
	public Quest114ResurrectionOfAnOldManager(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Resurrection of an Old Manager");
		addStartNpc(START_NPC);
		addTalkId(YUMI, WENDY, BOX, STONE, NEWYEAR);
		addKillId(GOLEM);
		registerQuestItems(DETECTOR, DETECTOR_2, STARSTONE, LETTER, STARSTONE_2);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		String htmltext = event;

		if ("quest_accept".equalsIgnoreCase(event)
				|| "32041-02.htm".equalsIgnoreCase(event)
				|| "collecter_yumi_q0114_04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.set("talk", "0");
			qs.set("golemSpawned", "0");
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "collecter_yumi_q0114_0104.htm";
		} else if ("32041-06.htm".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			qs.set("talk", "1");
		} else if ("32041-07.htm".equalsIgnoreCase(event) || "reply_2".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.set("talk", "0");
			qs.set("full", "1");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32041-10.htm".equalsIgnoreCase(event) || "reply_7".equalsIgnoreCase(event)) {
			int choice = qs.getInt("choice");
			if (choice == 1) {
				htmltext = "32041-10.htm";
			} else if (choice == 2) {
				htmltext = "32041-10a.htm";
			} else if (choice == 3) {
				htmltext = "32041-10b.htm";
			}
		} else if ("32041-11.htm".equalsIgnoreCase(event) || "reply_8".equalsIgnoreCase(event)) {
			qs.set("talk", "1");
		} else if ("32041-18.htm".equalsIgnoreCase(event) || "reply_10".equalsIgnoreCase(event)) {
			qs.set("talk", "2");
		} else if ("32041-20.htm".equalsIgnoreCase(event) || "reply_11".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.set("talk", "0");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32041-25.htm".equalsIgnoreCase(event) || "reply_13".equalsIgnoreCase(event)) {
			qs.setCond(17);
			qs.playSound(QuestState.SOUND_MIDDLE);
			if (!qs.hasQuestItems(DETECTOR)) {
				qs.giveItems(DETECTOR, 1);
			}
		} else if ("32041-28.htm".equalsIgnoreCase(event) || "reply_15".equalsIgnoreCase(event)) {
			qs.set("talk", "1");
			qs.takeItems(DETECTOR_2, -1);
		} else if ("32041-31.htm".equalsIgnoreCase(event) || "reply_14".equalsIgnoreCase(event)) {
			if (qs.getInt("choice") > 1) {
				htmltext = "32041-37.htm";
			}
		} else if ("32041-32.htm".equalsIgnoreCase(event) || "reply_17".equalsIgnoreCase(event)) {
			qs.setCond(21);
			qs.playSound(QuestState.SOUND_MIDDLE);
			if (!qs.hasQuestItems(LETTER)) {
				qs.giveItems(LETTER, 1);
			}
		} else if ("32041-36.htm".equalsIgnoreCase(event) || "reply_16".equalsIgnoreCase(event)) {
			qs.setCond(20);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32046-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(19);
			if (qs.hasQuestItems(DETECTOR)) {
				qs.takeItems(DETECTOR, -1);
				qs.giveItems(DETECTOR_2, 1);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32046-06.htm".equalsIgnoreCase(event) || "reply_4".equalsIgnoreCase(event)) {
			qs.takeItems(STARSTONE_2, -1);
			qs.giveItems(57, 70362);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
		} else if ("32047-01.htm".equalsIgnoreCase(event)) {
			int talk = qs.getInt("talk");
			int talk1 = qs.getInt("talk1");
			if (talk == 1 && talk1 == 1) {
				htmltext = "32047-04.htm";
			} else if (talk == 2 && talk1 == 2 && qs.getInt("talk2") == 2) {
				htmltext = "32047-08.htm";
			}
		} else if ("32047-02.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("talk") == 0) {
				qs.set("talk", "1");
			}
		} else if ("32047-03.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("talk1") == 0) {
				qs.set("talk1", "1");
			}
		} else if ("32047-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.set("talk", "0");
			qs.set("choice", "1");
			qs.unset("talk1");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-06.htm".equalsIgnoreCase(event) || "reply_5".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.set("talk", "0");
			qs.set("choice", "2");
			qs.unset("talk1");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-07.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.set("talk", "0");
			qs.set("choice", "3");
			qs.unset("talk1");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-13.htm".equalsIgnoreCase(event) || "reply_405".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-13a.htm".equalsIgnoreCase(event) || "reply_503".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-15.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("talk") == 0) {
				qs.set("talk", "1");
			}
		} else if ("32047-15a.htm".equalsIgnoreCase(event) || "reply_504".equalsIgnoreCase(event)) {
			if (qs.getInt("golemSpawned") == 0) {
				qs.addSpawn(GOLEM, 96977, -110625, -3322, 0);
				qs.set("golemSpawned", "1");
				qs.startQuestTimer("golemDespawn", 900000);
			} else {
				htmltext = "32047-19a.htm";
			}
		} else if ("32047-17a.htm".equalsIgnoreCase(event) || "reply_505".equalsIgnoreCase(event)) {
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-20.htm".equalsIgnoreCase(event) || "32047-30.htm".equalsIgnoreCase(event)) {
			qs.set("talk", "2");
		} else if ("32047-23.htm".equalsIgnoreCase(event) || "reply_605".equalsIgnoreCase(event)) {
			qs.setCond(13);
			qs.set("talk", "0");
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-25.htm".equalsIgnoreCase(event) || "reply_607".equalsIgnoreCase(event)) {
			qs.setCond(15);
			qs.takeItems(STARSTONE, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-33.htm".equalsIgnoreCase(event) || "reply_408".equalsIgnoreCase(event)) {
			int cond = qs.getCond();
			if (cond == 7) {
				qs.setCond(8);
				qs.set("talk", "0");
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (cond == 8) {
				qs.setCond(9);
				htmltext = "32047-34.htm";
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if ("32047-34.htm".equalsIgnoreCase(event) || "reply_409".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("32047-38.htm".equalsIgnoreCase(event) || "reply_612".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(57) >= 3000) {
				qs.setCond(26);
				qs.takeItems(57, 3000);
				qs.giveItems(STARSTONE_2, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				htmltext = "32047-39.htm";
			}
		} else if ("32050-01.htm".equalsIgnoreCase(event)
				|| "32050-02.htm".equalsIgnoreCase(event)
				|| "reply_999".equalsIgnoreCase(event)) {
			qs.set("talk", "1");
			qs.playSound("ItemSound.armor_wood_3");
		} else if ("32050-04.htm".equalsIgnoreCase(event) || "reply_606".equalsIgnoreCase(event)) {
			qs.setCond(14);
			qs.set("talk", "0");
			if (!qs.hasQuestItems(STARSTONE)) {
				qs.giveItems(STARSTONE, 1);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("reply_410".equalsIgnoreCase(event)) {
			qs.setCond(23);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("reply_411".equalsIgnoreCase(event) || "menu_select?ask=114&reply=411".equalsIgnoreCase(event)) {
			qs.setCond(24);
			if (!qs.hasQuestItems(STARSTONE_2)) {
				qs.giveItems(STARSTONE_2, 1);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31961-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(22);
			qs.takeItems(LETTER, -1);
			if (!qs.hasQuestItems(STARSTONE_2)) {
				qs.giveItems(STARSTONE_2, 1);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("golemDespawn".equalsIgnoreCase(event)) {
			qs.unset("golemSpawned");
			return null;
		}
		return htmltext;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		if (qs.isCompleted()) {
			return "completed";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		int talk = qs.getInt("talk");

		if (npcId == YUMI) {
			if (cond == 0) {
				if (pc.level() >= 49) {
					return "collecter_yumi_q0114_0101.htm";
				}
				qs.exitQuest(true);
				return "collecter_yumi_q0114_0103.htm";
			} else if (cond == 1) {
				if (talk == 1) {
					return "32041-06.htm";
				}
				return "collecter_yumi_q0114_0105.htm";
			} else if (cond == 2) {
				if (qs.getInt("full") == 1) {
					return "32041-08.htm";
				}
				qs.giveItems(57, 70362);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collecter_yumi_q0114_0202.htm";
			} else if (cond > 2 && cond < 6) {
				if (talk == 0) {
					return "32041-09.htm";
				} else if (talk == 1) {
					return "32041-11.htm";
				}
				return "32041-18.htm";
			} else if (cond == 6) {
				return "32041-21.htm";
			} else if (cond == 9 || cond == 12 || cond == 16) {
				return "32041-22.htm";
			} else if (cond == 17) {
				return "32041-26.htm";
			} else if (cond == 19) {
				return talk == 0 ? "32041-27.htm" : "32041-28.htm";
			} else if (cond == 20) {
				return "32041-36.htm";
			} else if (cond == 21) {
				return "32041-33.htm";
			} else if (cond == 22 || cond == 25 || cond == 26) {
				qs.setCond(27);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "32041-34.htm";
			} else if (cond == 27) {
				return "32041-35.htm";
			}
		} else if (npcId == WENDY) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "wendy_q0114_0201.htm";
			} else if (cond == 2) {
				if (talk == 1 && qs.getInt("talk1") == 1) {
					return "32047-04.htm";
				}
				return "32047-01.htm";
			} else if (cond == 3) {
				return "32047-09.htm";
			} else if (cond == 4 || cond == 5) {
				return "32047-09a.htm";
			} else if (cond == 6) {
				int choice = qs.getInt("choice");
				if (choice == 1) {
					return talk == 0 ? "32047-10.htm" : "32047-20.htm";
				} else if (choice == 2) {
					return "32047-10a.htm";
				} else {
					if (talk == 0) {
						return "32047-14.htm";
					} else if (talk == 1) {
						return "32047-15.htm";
					}
					return "32047-20.htm";
				}
			} else if (cond == 7) {
				if (talk == 0) {
					return "32047-14.htm";
				} else if (talk == 1) {
					return "32047-15.htm";
				}
				return "32047-20.htm";
			} else if (cond == 8) {
				return "32047-30.htm";
			} else if (cond == 9) {
				return "32047-27.htm";
			} else if (cond == 10) {
				return "32047-14a.htm";
			} else if (cond == 11) {
				return "32047-16a.htm";
			} else if (cond == 12) {
				return "32047-18a.htm";
			} else if (cond == 13) {
				return "32047-23.htm";
			} else if (cond == 14) {
				return "32047-24.htm";
			} else if (cond == 15) {
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "32047-26.htm";
			} else if (cond == 16) {
				return "32047-27.htm";
			} else if (cond == 20) {
				return "32047-35.htm";
			} else if (cond == 23) {
				return "chaos_secretary_wendy_q0114_23z.htm";
			} else if (cond == 24 && qs.hasQuestItems(STARSTONE_2)) {
				qs.setCond(25);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0114_24a.htm";
			} else if (cond == 25) {
				return "chaos_secretary_wendy_q0114_24a.htm";
			} else if (cond == 26) {
				return "32047-40.htm";
			}
		} else if (npcId == BOX) {
			if (cond == 13) {
				return talk == 0 ? "32050-01.htm" : "32050-03.htm";
			} else if (cond == 14) {
				return "32050-05.htm";
			} else if (cond == 23) {
				return "chaos_box2_q0114_04b.htm";
			} else if (cond == 24) {
				return "chaos_box2_q0114_05z.htm";
			}
		} else if (npcId == STONE) {
			if (cond == 17) {
				qs.setCond(18);
				qs.takeItems(DETECTOR, -1);
				qs.giveItems(DETECTOR_2, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "32046-01.htm";
			} else if (cond == 18) {
				return "32046-01.htm";
			} else if (cond == 19) {
				return "32046-02.htm";
			} else if (cond == 27) {
				return "32046-03.htm";
			}
		} else if (npcId == NEWYEAR) {
			if (cond == 21) {
				return "31961-01.htm";
			} else if (cond == 22) {
				return "31961-03.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		if (npc.getNpcId() == GOLEM && qs.getCond() == 10) {
			qs.setCond(11);
			qs.unset("golemSpawned");
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
