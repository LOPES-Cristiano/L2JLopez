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

	private static final Set<Integer> VALID_CLASSES = Set.of(1, 19, 32, 45, 47); // Warrior, Elven Knight, Palus Knight, Orc Raider, Orc Monk

	@Autowired
	public Quest211TrialOfChallenger(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Challenger");

		addStartNpc(KASH);
		addTalkId(KASH);
		addTalkId(MARTIEN);
		addTalkId(RALDO);
		addTalkId(CHEST_OF_SHYSLASSYS);

		addKillId(SHYSLASSYS);
		addKillId(GORR);
		addKillId(BARAHAM);
		addKillId(SUCCUBUS_QUEEN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30644-05.htm";
		} else if ("30644_1".equalsIgnoreCase(event)) {
			return "30644-04.htm";
		} else if ("30645_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_KASH, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30645-02.htm";
		} else if ("30647_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BROKEN_KEY) == 1) {
				qs.giveItems(SCROLL_OF_SHYSLASSY, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30647-02.htm";
			}
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
			} else if (cond == 2 && qs.getQuestItemsCount(SCROLL_OF_SHYSLASSY) == 1) {
				qs.takeItems(SCROLL_OF_SHYSLASSY, 1);
				qs.giveItems(LETTER_OF_KASH, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30644-07.htm";
			} else if (cond == 3 && qs.getQuestItemsCount(LETTER_OF_KASH) == 1) {
				return "30644-08.htm";
			} else if (cond >= 7) {
				return "30644-09.htm";
			}
		} else if (npcId == MARTIEN) {
			if (cond == 3 && qs.getQuestItemsCount(LETTER_OF_KASH) == 1) {
				return "30645-01.htm";
			} else if (cond == 4 && qs.getQuestItemsCount(WATCHERS_EYE1) == 0) {
				return "30645-03.htm";
			} else if (cond == 5 && qs.getQuestItemsCount(WATCHERS_EYE1) > 0) {
				qs.takeItems(WATCHERS_EYE1, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30645-04.htm";
			} else if (cond == 6) {
				return "30645-05.htm";
			} else if (cond >= 7) {
				return "30645-06.htm";
			}
		} else if (npcId == CHEST_OF_SHYSLASSYS) {
			if (cond == 2) {
				return "30647-01.htm";
			}
		} else if (npcId == RALDO) {
			if (cond == 10) {
				qs.addExpAndSp(533803, 34621);
				qs.giveItems(57, 97278); // Adena
				qs.giveItems(DIMENSIONAL_DIAMOND, 8);
				qs.takeItems(BROKEN_KEY, -1);
				qs.giveItems(MARK_OF_CHALLENGER, 1);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound(QuestState.SOUND_FINISH);
				return "30646-07.htm";
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
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(2);
		} else if (npcId == GORR && cond == 4) {
			qs.giveItems(WATCHERS_EYE1, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(5);
		} else if (npcId == BARAHAM && cond == 6) {
			qs.giveItems(WATCHERS_EYE2, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(7);
		}
		return null;
	}
}
