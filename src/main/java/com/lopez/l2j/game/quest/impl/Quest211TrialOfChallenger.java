package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 211: Trial of the Challenger (1º Passo de 2ª Classe para Gladiator, Warlord, Destroyer, Tyrant).
 */
@Component
public class Quest211TrialOfChallenger extends Quest {

	public static final int QUEST_ID = 211;
	public static final String QUEST_NAME = "211_TrialOfChallenger";

	// NPCs
	public static final int FILAUR = 30535;
	public static final int KASH = 30644;
	public static final int MARTIEN = 30645;
	public static final int RALDO = 30646;
	public static final int CHEST_OF_SHYSLASSYS = 30647;

	// Monstros
	public static final int SHYSLASSYS = 27110;
	public static final int GORR = 27112;
	public static final int BARAHAM = 27113;
	public static final int SUCCUBUS_QUEEN = 27114;

	// Itens
	public static final int LETTER_OF_KASH = 2628;
	public static final int SCROLL_OF_SHYSLASSY = 2631;
	public static final int WATCHERS_EYE1 = 2629;
	public static final int WATCHERS_EYE2 = 2630;
	public static final int BROKEN_KEY = 2632;
	public static final int MARK_OF_CHALLENGER = 2627;
	public static final int DIMENSIONAL_DIAMOND = 7562;

	// Recompensas do Baú
	public static final int ADENA = 57;
	public static final int ELVEN_NECKLACE_BEADS = 1904;
	public static final int WHITE_TUNIC_PATTERN = 1936;
	public static final int IRON_BOOTS_DESIGN = 1940;
	public static final int MANTICOR_SKIN_GAITERS_PATTERN = 1943;
	public static final int RIP_GAUNTLETS_PATTERN = 1946;
	public static final int TOME_OF_BLOOD_PAGE = 2030;
	public static final int MITHRIL_SCALE_GAITERS_MATERIAL = 2918;
	public static final int BRIGANDINE_GAUNTLETS_PATTERN = 2927;

	private static final Set<Integer> VALID_CLASSES = Set.of(1, 19, 32, 45, 47); // Warrior, Elven Knight, Palus Knight, Orc Raider, Orc Monk

	@Autowired
	public Quest211TrialOfChallenger(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Challenger");

		registerQuestItems(LETTER_OF_KASH, WATCHERS_EYE1, WATCHERS_EYE2, SCROLL_OF_SHYSLASSY, BROKEN_KEY);

		addStartNpc(KASH);
		addTalkId(FILAUR, KASH, MARTIEN, RALDO, CHEST_OF_SHYSLASSYS);

		addKillId(SHYSLASSYS, GORR, BARAHAM, SUCCUBUS_QUEEN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30644-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30644-05.htm";
		} else if ("30644_1".equalsIgnoreCase(event) || "30644-04.htm".equalsIgnoreCase(event)) {
			return "30644-04.htm";
		} else if ("30645_1".equalsIgnoreCase(event) || "30645-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_KASH, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30645-02.htm";
		} else if ("30646_1".equalsIgnoreCase(event) || "30646-02.htm".equalsIgnoreCase(event)) {
			return "30646-02.htm";
		} else if ("30646_2".equalsIgnoreCase(event) || "30646-03.htm".equalsIgnoreCase(event)) {
			return "30646-03.htm";
		} else if ("30646_3".equalsIgnoreCase(event) || "30646-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(WATCHERS_EYE2, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30646-04.htm";
		} else if ("30646_4".equalsIgnoreCase(event) || "30646-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(WATCHERS_EYE2, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30646-06.htm";
		} else if ("30647_1".equalsIgnoreCase(event) || "30647-04.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BROKEN_KEY)) {
				if (!qs.hasQuestItems(SCROLL_OF_SHYSLASSY)) {
					qs.giveItems(SCROLL_OF_SHYSLASSY, 1);
				}
				qs.takeItems(BROKEN_KEY, 1);
				if (java.util.concurrent.ThreadLocalRandom.current().nextInt(10) < 2) {
					qs.playSound(QuestState.SOUND_JACKPOT);
					int chance = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
					if (chance > 90) {
						qs.rewardItems(BRIGANDINE_GAUNTLETS_PATTERN, 1);
						qs.rewardItems(IRON_BOOTS_DESIGN, 1);
						qs.rewardItems(MANTICOR_SKIN_GAITERS_PATTERN, 1);
						qs.rewardItems(MITHRIL_SCALE_GAITERS_MATERIAL, 1);
						qs.rewardItems(RIP_GAUNTLETS_PATTERN, 1);
					} else if (chance > 70) {
						qs.rewardItems(ELVEN_NECKLACE_BEADS, 1);
						qs.rewardItems(TOME_OF_BLOOD_PAGE, 1);
					} else if (chance > 40) {
						qs.rewardItems(WHITE_TUNIC_PATTERN, 1);
					} else {
						qs.rewardItems(IRON_BOOTS_DESIGN, 1);
					}
					return "30647-03.htm";
				}
				qs.rewardItems(ADENA, java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 1001));
				return "30647-02.htm";
			}
			return "30647-04.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		if (qs.isCompleted()) {
			return "<html><body>This quest has already been completed.</body></html>";
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == KASH) {
			if (cond == 0) {
				if (VALID_CLASSES.contains(c.classId())) {
					if (c.level() >= 35) {
						return "30644-03.htm";
					} else {
						return "30644-01.htm";
					}
				} else {
					return "30644-02.htm";
				}
			} else if (cond == 1) {
				return "30644-06.htm";
			} else if (cond == 2 && qs.hasQuestItems(SCROLL_OF_SHYSLASSY)) {
				qs.takeItems(SCROLL_OF_SHYSLASSY, 1);
				qs.giveItems(LETTER_OF_KASH, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30644-07.htm";
			} else if (cond == 3 && qs.hasQuestItems(LETTER_OF_KASH)) {
				return "30644-08.htm";
			} else if (cond > 3) {
				return "30644-09.htm";
			}
		} else if (npcId == MARTIEN) {
			if (cond == 3 && qs.hasQuestItems(LETTER_OF_KASH)) {
				return "30645-01.htm";
			} else if (cond == 4 && !qs.hasQuestItems(WATCHERS_EYE1)) {
				return "30645-03.htm";
			} else if (cond == 5 && qs.hasQuestItems(WATCHERS_EYE1)) {
				qs.takeItems(WATCHERS_EYE1, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30645-04.htm";
			} else if (cond == 6) {
				return "30645-05.htm";
			} else if (cond == 7) {
				return "30645-07.htm";
			} else if (cond > 7) {
				return "30645-06.htm";
			}
		} else if (npcId == CHEST_OF_SHYSLASSYS) {
			if (cond >= 2) {
				return "30647-01.htm";
			}
		} else if (npcId == RALDO) {
			if (cond == 7 && qs.hasQuestItems(WATCHERS_EYE2)) {
				return "30646-01.htm";
			} else if (cond == 8) {
				return "30646-06a.htm";
			} else if (cond == 10) {
				qs.addExpAndSp(533803, 34621);
				qs.giveItems(ADENA, 97278);
				qs.giveItems(DIMENSIONAL_DIAMOND, 8);
				qs.takeItems(BROKEN_KEY, -1);
				qs.giveItems(MARK_OF_CHALLENGER, 1);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound(QuestState.SOUND_FINISH);
				return "30646-07.htm";
			}
		} else if (npcId == FILAUR) {
			if (cond == 8) {
				if (c.level() >= 36) {
					qs.setCond(9);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30535-01.htm";
				}
				return "30535-03.htm";
			} else if (cond == 9) {
				qs.addRadar(176560, -184969, -3729);
				return "30535-02.htm";
			} else if (cond == 10) {
				return "30535-04.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == SHYSLASSYS && cond == 1) {
			qs.giveItems(BROKEN_KEY, 1);
			qs.giveItems(SCROLL_OF_SHYSLASSY, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(2);
			qs.addSpawn(CHEST_OF_SHYSLASSYS);
		} else if (npcId == GORR && cond == 4) {
			qs.giveItems(WATCHERS_EYE1, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(5);
		} else if (npcId == BARAHAM && cond == 6) {
			qs.giveItems(WATCHERS_EYE2, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(7);
			qs.addSpawn(RALDO);
		} else if (npcId == SUCCUBUS_QUEEN && cond == 9) {
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(10);
			qs.addSpawn(RALDO);
		}
		return null;
	}
}
