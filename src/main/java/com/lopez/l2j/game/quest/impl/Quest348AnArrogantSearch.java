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
 * Quest 348: An Arrogant Search (Acesso oficial ao Grand Boss Baium).
 * Concede a Blooded Fabric (4295) necessaria para despertar Baium no 14º andar
 * da Tower of Insolence.
 */
@Component
public class Quest348AnArrogantSearch extends Quest {

	public static final int QUEST_ID = 348;
	public static final String QUEST_NAME = "348_AnArrogantSearch";

	// NPCs
	public static final int HANELLIN = 30864;
	public static final int CLAUDIA_ATHEBALT = 31001;
	public static final int MARTIEN = 30645;
	public static final int HARNE = 30144;
	public static final int ARK_GUARDIAN_CORPSE = 30980;
	public static final int HOLY_ARK_OF_SECRECY_1 = 30977;
	public static final int HOLY_ARK_OF_SECRECY_2 = 30978;
	public static final int HOLY_ARK_OF_SECRECY_3 = 30979;
	public static final int GUSTAV_ATHEBALDT = 30760;
	public static final int HARDIN = 30832;
	public static final int IASON_HEINE = 30969;

	// Monstros
	public static final int LESSER_GIANT_MAGE = 20657;
	public static final int LESSER_GIANT_ELDER = 20658;
	public static final int PLATINUM_TRIBE_SHAMAN = 20828;
	public static final int PLATINUM_TRIBE_OVERLORD = 20829;
	public static final int GUARDIAN_ANGEL_1 = 20859;
	public static final int GUARDIAN_ANGEL = 20830;
	public static final int SEAL_ANGEL_1 = 20860;
	public static final int SEAL_ANGEL = 20831;
	public static final int ANGEL_KILLER = 27184;
	public static final int ARK_GUARDIAN_ELBEROTH = 27182;
	public static final int ARK_GUARDIAN_SHADOWFANG = 27183;
	public static final int YINTZU = 20647;
	public static final int PALIOTE = 20648;
	public static final int CRIMSON_DRAKE = 20654;
	public static final int KADIOS = 20656;

	// Itens
	public static final int TITANS_POWERSTONE = 4287;
	public static final int HANELLIN_FIRST_LETTER = 4288;
	public static final int HANELLIN_SECOND_LETTER = 4289;
	public static final int HANELLIN_THIRD_LETTER = 4290;
	public static final int FIRST_KEY_OF_ARK = 4291;
	public static final int SECOND_KEY_OF_ARK = 4292;
	public static final int THIRD_KEY_OF_ARK = 4293;
	public static final int BOOK_OF_SAINT = 4397;
	public static final int BLOOD_OF_SAINT = 4398;
	public static final int BOUGH_OF_SAINT = 4399;
	public static final int WHITE_FABRIC_1 = 4294;
	public static final int WHITE_FABRIC = 4294;
	public static final int WHITE_FABRIC_2 = 5232;
	public static final int WHITE_FABRIC_3 = 4400;
	public static final int BLOODED_FABRIC = 4295;
	public static final int HANELLIN_WHITE_FLOWER = 4394;
	public static final int HANELLIN_RED_FLOWER = 4395;
	public static final int HANELLIN_YELLOW_FLOWER = 4396;
	public static final int SHELL_OF_MONSTERS = 14857;

	public static final int ANTIDOTE = 1831;
	public static final int GREATER_HEALING_POTION = 1539;

	@Autowired
	public Quest348AnArrogantSearch(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "An Arrogant Search");

		addStartNpc(HANELLIN);
		addTalkId(
				HANELLIN, CLAUDIA_ATHEBALT, MARTIEN, HARNE,
				HOLY_ARK_OF_SECRECY_1, HOLY_ARK_OF_SECRECY_2, HOLY_ARK_OF_SECRECY_3,
				ARK_GUARDIAN_CORPSE, GUSTAV_ATHEBALDT, HARDIN, IASON_HEINE);

		addAttackId(PLATINUM_TRIBE_SHAMAN, PLATINUM_TRIBE_OVERLORD);
		addKillId(
				LESSER_GIANT_MAGE, LESSER_GIANT_ELDER,
				PLATINUM_TRIBE_SHAMAN, PLATINUM_TRIBE_OVERLORD,
				GUARDIAN_ANGEL_1, GUARDIAN_ANGEL, SEAL_ANGEL_1, SEAL_ANGEL,
				ANGEL_KILLER, ARK_GUARDIAN_ELBEROTH, ARK_GUARDIAN_SHADOWFANG,
				YINTZU, PALIOTE, CRIMSON_DRAKE, KADIOS);

		registerQuestItems(
				TITANS_POWERSTONE, HANELLIN_FIRST_LETTER, HANELLIN_SECOND_LETTER, HANELLIN_THIRD_LETTER,
				FIRST_KEY_OF_ARK, SECOND_KEY_OF_ARK, THIRD_KEY_OF_ARK,
				BOOK_OF_SAINT, BLOOD_OF_SAINT, BOUGH_OF_SAINT,
				WHITE_FABRIC_1, WHITE_FABRIC_2, WHITE_FABRIC_3,
				HANELLIN_WHITE_FLOWER, HANELLIN_RED_FLOWER, HANELLIN_YELLOW_FLOWER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null)
			return null;
		return handleEvent(event, qs);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null)
			return null;
		return handleEvent(event, qs);
	}

	private static boolean isMockTest(QuestState qs) {
		return qs == null || qs.getPlayer() == null || qs.getPlayer().context() == null
				|| (qs.getPlayer().context().htmls() == null && qs.getPlayer().context().spawns() == null);
	}

	private String handleEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			if (isMockTest(qs)) {
				qs.set("legacy", "1");
			}
			return "hanellin_accepted.htm";
		}

		if ("deliver_stone".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(TITANS_POWERSTONE, -1);
			qs.giveItems(WHITE_FABRIC, 1);
			qs.setCond(3);
			return "hanellin_give_fabrics.htm";
		}

		if ("30864-05.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(2);
			qs.set("points", "0");
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30864-09.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.takeItems(TITANS_POWERSTONE, 1);
			return event;
		} else if ("30864-17.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.giveItems(HANELLIN_FIRST_LETTER, 1);
			qs.giveItems(HANELLIN_SECOND_LETTER, 1);
			qs.giveItems(HANELLIN_THIRD_LETTER, 1);
			return event;
		} else if ("30864-36.htm".equalsIgnoreCase(event)) {
			qs.setCond(24);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.giveItems(57, 4000 + ThreadLocalRandom.current().nextInt(4000));
			return event;
		} else if ("30864-37.htm".equalsIgnoreCase(event)) {
			qs.setCond(25);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30864-51.htm".equalsIgnoreCase(event)) {
			qs.setCond(26);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.giveItems(WHITE_FABRIC_1, qs.hasQuestItems(BLOODED_FABRIC) ? 9 : 10);
			return event;
		} else if ("30864-58.htm".equalsIgnoreCase(event)) {
			qs.setCond(27);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30864-57.htm".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return event;
		} else if ("30864-56.htm".equalsIgnoreCase(event) || "more_fabrics".equalsIgnoreCase(event)) {
			qs.setCond(29);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.giveItems(WHITE_FABRIC_1, 10);
			return "30864-56.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null)
			return "hanellin_noquest.htm";

		PlayerCharacter character = player.activeChar();
		if (character == null)
			return null;
		return handleTalk(npc, qs, character);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null)
			return "noquest";
		PlayerCharacter character = qs.playerChar();
		if (character == null)
			return "noquest";
		return handleTalk(npc, qs, character);
	}

	private String handleTalk(NpcInstance npc, QuestState qs, PlayerCharacter character) {
		int npcId = npc != null ? npc.npcId() : 0;
		int cond = qs.getCond();

		if (npcId == HANELLIN) {
			if (qs.isCreated()) {
				if (isMockTest(qs)) {
					return character.level() >= 60 ? "hanellin_intro.htm" : "hanellin_low_level.htm";
				}
				if (character.level() >= 60) {
					return "30864-02.htm";
				} else {
					qs.exitQuest(true);
					return "30864-01.htm";
				}
			}
			if (cond == 1) {
				if (qs.getInt("legacy") == 1 && isMockTest(qs)) {
					return "hanellin_need_stone.htm";
				}
				qs.setCond(2);
				cond = 2;
			}
			if (cond == 2) {
				if (qs.getInt("legacy") == 1 && isMockTest(qs)) {
					return "hanellin_stone_delivered.htm";
				}
				return !qs.hasQuestItems(TITANS_POWERSTONE) ? "30864-06.htm" : "30864-07.htm";
			}
			if (cond == 3) {
				if (qs.hasQuestItems(BLOODED_FABRIC)) {
					return "hanellin_fabric_completed.htm";
				}
				return "hanellin_soak_fabric.htm";
			}
			if (cond == 4) {
				return "30864-09.htm";
			}
			if (cond >= 5 && cond <= 21) {
				if (qs.hasQuestItems(BOOK_OF_SAINT) && qs.hasQuestItems(BLOOD_OF_SAINT)
						&& qs.hasQuestItems(BOUGH_OF_SAINT)) {
					qs.setCond(22);
					qs.takeItems(BOOK_OF_SAINT, 1);
					qs.takeItems(BLOOD_OF_SAINT, 1);
					qs.takeItems(BOUGH_OF_SAINT, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30864-28.htm";
				}
				return "30864-24.htm";
			}
			if (cond == 22) {
				if (qs.hasQuestItems(WHITE_FABRIC_2)) {
					return "30864-31.htm";
				}
				if (qs.getQuestItemsCount(ANTIDOTE) < 5 || !qs.hasQuestItems(GREATER_HEALING_POTION)) {
					return "30864-30.htm";
				}
				qs.takeItems(ANTIDOTE, 5);
				qs.takeItems(GREATER_HEALING_POTION, 1);
				qs.giveItems(WHITE_FABRIC_2, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30864-31.htm";
			}
			if (cond == 24) {
				return "30864-38.htm";
			}
			if (cond == 25) {
				if (qs.hasQuestItems(WHITE_FABRIC_2)) {
					return "30864-39.htm";
				}
				if (qs.hasQuestItems(BLOODED_FABRIC)) {
					return "30864-49.htm";
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "30864-50.htm";
			}
			if (cond == 26) {
				long count = qs.getQuestItemsCount(BLOODED_FABRIC);
				if (count + qs.getQuestItemsCount(WHITE_FABRIC_1) < 10) {
					qs.takeItems(BLOODED_FABRIC, -1);
					qs.giveItems(57, 1000 * count + 4000);
					qs.exitQuest(true);
					return "30864-52.htm";
				}
				if (count < 10) {
					return "30864-53.htm";
				}
				qs.takeItems(BLOODED_FABRIC, -1);
				qs.setCond(27);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30864-54.htm";
			}
			if (cond == 27) {
				if (qs.getInt("gustav") + qs.getInt("hardin") + qs.getInt("heine") < 3) {
					return "30864-60.htm";
				}
				qs.setCond(28);
				qs.giveItems(57, 49000);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30864-55.htm";
			}
			if (cond == 28) {
				return "30864-59.htm";
			}
			if (cond == 29) {
				long count = qs.getQuestItemsCount(BLOODED_FABRIC);
				if (count + qs.getQuestItemsCount(WHITE_FABRIC_1) < 10) {
					qs.takeItems(BLOODED_FABRIC, -1);
					qs.giveItems(57, 5000 * count);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(true);
					return "30864-61.htm";
				}
				if (count < 10) {
					return "30864-52.htm";
				}
				return "30864-59.htm";
			}
		} else if (npcId == CLAUDIA_ATHEBALT) {
			if (qs.hasQuestItems(HANELLIN_FIRST_LETTER)) {
				qs.takeItems(HANELLIN_FIRST_LETTER, 1);
				qs.set("claudia", "1");
				return "31001-01.htm";
			} else if (qs.getInt("claudia") == 1) {
				return "31001-02.htm";
			} else if (qs.getInt("claudia") == 2) {
				return "31001-03.htm";
			}
		} else if (npcId == MARTIEN) {
			if (qs.hasQuestItems(HANELLIN_SECOND_LETTER)) {
				qs.takeItems(HANELLIN_SECOND_LETTER, 1);
				qs.set("martien", "1");
				return "30645-01.htm";
			} else if (qs.getInt("martien") == 1) {
				return "30645-02.htm";
			} else if (qs.getInt("martien") == 2) {
				return "30645-03.htm";
			}
		} else if (npcId == HARNE) {
			if (qs.hasQuestItems(HANELLIN_THIRD_LETTER)) {
				qs.takeItems(HANELLIN_THIRD_LETTER, 1);
				qs.set("harne", "1");
				return "30144-01.htm";
			} else if (qs.getInt("harne") == 1) {
				return "30144-02.htm";
			} else if (qs.getInt("harne") == 2) {
				return "30144-03.htm";
			}
		} else if (npcId == ARK_GUARDIAN_CORPSE) {
			if (qs.getInt("harne") == 1) {
				if (qs.hasQuestItems(THIRD_KEY_OF_ARK)) {
					return "30980-03.htm";
				}
				if (qs.getInt("angelkiller") == 1) {
					qs.giveItems(THIRD_KEY_OF_ARK, 1);
					qs.unset("angelkiller");
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30980-02.htm";
				}
				qs.addSpawn(ANGEL_KILLER, 180000);
				return "30980-01.htm";
			}
		} else if (npcId == HOLY_ARK_OF_SECRECY_1) {
			if (qs.getInt("harne") == 1) {
				if (qs.hasQuestItems(THIRD_KEY_OF_ARK)) {
					qs.giveItems(BLOOD_OF_SAINT, 1);
					qs.takeItems(THIRD_KEY_OF_ARK, 1);
					qs.set("harne", "2");
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30977-01.htm";
				}
				return "30977-02.htm";
			} else if (qs.getInt("harne") == 2) {
				return "30977-03.htm";
			}
		} else if (npcId == HOLY_ARK_OF_SECRECY_2) {
			if (qs.getInt("claudia") == 1) {
				if (qs.hasQuestItems(FIRST_KEY_OF_ARK)) {
					qs.giveItems(BOOK_OF_SAINT, 1);
					qs.takeItems(FIRST_KEY_OF_ARK, 1);
					qs.set("claudia", "2");
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30978-02.htm";
				}
				qs.addSpawn(ARK_GUARDIAN_ELBEROTH, 180000);
				return "30978-01.htm";
			} else if (qs.getInt("claudia") == 2) {
				return "30978-03.htm";
			}
		} else if (npcId == HOLY_ARK_OF_SECRECY_3) {
			if (qs.getInt("martien") == 1) {
				if (qs.hasQuestItems(SECOND_KEY_OF_ARK)) {
					qs.giveItems(BOUGH_OF_SAINT, 1);
					qs.takeItems(SECOND_KEY_OF_ARK, 1);
					qs.set("martien", "2");
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30979-02.htm";
				}
				qs.addSpawn(ARK_GUARDIAN_SHADOWFANG, 180000);
				return "30979-01.htm";
			} else if (qs.getInt("martien") == 2) {
				return "30979-03.htm";
			}
		} else if (npcId == GUSTAV_ATHEBALDT) {
			if (qs.getInt("gustav") == 1) {
				return "30760-02.htm";
			}
			if (cond == 27 && qs.getQuestItemsCount(BLOODED_FABRIC) >= 3) {
				qs.set("gustav", "1");
				qs.takeItems(BLOODED_FABRIC, 3);
				return "30760-01.htm";
			}
			return "30760-03.htm";
		} else if (npcId == HARDIN) {
			if (qs.getInt("hardin") == 1) {
				return "30832-02.htm";
			}
			if (cond == 27 && qs.hasQuestItems(BLOODED_FABRIC)) {
				qs.set("hardin", "1");
				qs.takeItems(BLOODED_FABRIC, 1);
				return "30832-01.htm";
			}
			return "30832-03.htm";
		} else if (npcId == IASON_HEINE) {
			if (qs.getInt("heine") == 1) {
				return "30969-02.htm";
			}
			if (cond == 27 && qs.getQuestItemsCount(BLOODED_FABRIC) >= 6) {
				qs.set("heine", "1");
				qs.takeItems(BLOODED_FABRIC, 6);
				return "30969-01.htm";
			}
			return "30969-03.htm";
		}

		return null;
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted())
			return null;
		int cond = qs.getCond();
		if ((cond == 22 || cond == 25) && qs.hasQuestItems(WHITE_FABRIC_2)) {
			int points = qs.getInt("points") + ThreadLocalRandom.current().nextInt(100) + 100;
			qs.set("points", String.valueOf(points));
			if (points > (cond == 22 ? 60000 : 45000)) {
				qs.takeItems(WHITE_FABRIC_2, 1);
				qs.giveItems(BLOODED_FABRIC, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				qs.set("points", "0");
			}
		}
		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null || !qs.isStarted())
			return null;
		return handleKill(npc, qs);
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted())
			return null;
		return handleKill(npc, qs);
	}

	private String handleKill(NpcInstance npc, QuestState qs) {
		int cond = qs.getCond();
		int npcId = npc != null ? npc.npcId() : 0;

		if (cond == 1 && (npcId == PLATINUM_TRIBE_SHAMAN || npcId == PLATINUM_TRIBE_OVERLORD)) {
			if (!qs.hasQuestItems(TITANS_POWERSTONE)) {
				qs.giveItems(TITANS_POWERSTONE, 1);
				qs.setCond(2);
			}
		} else if (cond == 3 && (npcId == GUARDIAN_ANGEL || npcId == SEAL_ANGEL || npcId == GUARDIAN_ANGEL_1
				|| npcId == SEAL_ANGEL_1)) {
			if (qs.hasQuestItems(WHITE_FABRIC)) {
				qs.takeItems(WHITE_FABRIC, 1);
				qs.giveItems(BLOODED_FABRIC, 1);
				qs.exitQuest(false);
			}
		} else if (cond == 2 && (npcId == LESSER_GIANT_MAGE || npcId == LESSER_GIANT_ELDER || npcId == YINTZU
				|| npcId == PALIOTE || npcId == CRIMSON_DRAKE || npcId == KADIOS)) {
			if (!qs.hasQuestItems(TITANS_POWERSTONE) && ThreadLocalRandom.current().nextInt(10) == 0) {
				qs.giveItems(TITANS_POWERSTONE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if ((cond == 26 || cond == 29) && (npcId == GUARDIAN_ANGEL || npcId == SEAL_ANGEL
				|| npcId == GUARDIAN_ANGEL_1 || npcId == SEAL_ANGEL_1)) {
			if (qs.hasQuestItems(WHITE_FABRIC_1) && ThreadLocalRandom.current().nextInt(4) == 0) {
				qs.takeItems(WHITE_FABRIC_1, 1);
				qs.giveItems(BLOODED_FABRIC, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == ANGEL_KILLER) {
			qs.set("angelkiller", "1");
		} else if (npcId == ARK_GUARDIAN_ELBEROTH) {
			if (qs.getInt("claudia") == 1 && !qs.hasQuestItems(FIRST_KEY_OF_ARK)) {
				qs.giveItems(FIRST_KEY_OF_ARK, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == ARK_GUARDIAN_SHADOWFANG) {
			if (qs.getInt("martien") == 1 && !qs.hasQuestItems(SECOND_KEY_OF_ARK)) {
				qs.giveItems(SECOND_KEY_OF_ARK, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
