package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 224: Test of the Sagittarius
 * 3º Passo de 2ª Classe para Hawkeye, Silver Ranger, Phantom Ranger.
 */
@Component
public class Quest224TestOfSagittarius extends Quest {

	public static final int QUEST_ID = 224;
	public static final String QUEST_NAME = "224_TestOfSagittarius";

	// NPCs
	public static final int BERNARD = 30702;
	public static final int HAMIL = 30626;
	public static final int ARON = 30653;
	public static final int VOKIYAN = 30514;

	// Monstros
	public static final int ANT = 20079;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int ROAD_RATMAN = 20551;
	public static final int MANASHEN_GARGOYLE = 20563;
	public static final int MARSH_SPIDER = 20233;

	// Itens
	public static final int MARK_OF_SAGITTARIUS = 3293;
	public static final int BERNARDS_INTRODUCTION = 3294;
	public static final int LETTER_OF_HAMIL1 = 3295;
	public static final int LETTER_OF_HAMIL2 = 3296;
	public static final int LETTER_OF_HAMIL3 = 3297;
	public static final int HUNTERS_RUNE1 = 3298;
	public static final int HUNTERS_RUNE2 = 3299;
	public static final int TALISMAN_OF_KADESH = 3300;
	public static final int TALISMAN_OF_SNAKE = 3301;
	public static final int CRESCENT_MOON_BOW = 3028;

	private static final Set<Integer> VALID_CLASSES = Set.of(7, 22, 35); // Rogue, Elven Scout, Assassin

	@Autowired
	public Quest224TestOfSagittarius(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Sagittarius");

		addStartNpc(BERNARD);
		addTalkId(BERNARD, HAMIL, ARON, VOKIYAN);

		addKillId(ANT, BREKA_ORC_SHAMAN, ROAD_RATMAN, MANASHEN_GARGOYLE, MARSH_SPIDER);

		registerQuestItems(BERNARDS_INTRODUCTION, LETTER_OF_HAMIL1, LETTER_OF_HAMIL2, LETTER_OF_HAMIL3,
				HUNTERS_RUNE1, HUNTERS_RUNE2, TALISMAN_OF_KADESH, TALISMAN_OF_SNAKE, CRESCENT_MOON_BOW);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30702-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BERNARDS_INTRODUCTION, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30702-04.htm";
		} else if ("30626_1".equalsIgnoreCase(event)) {
			qs.takeItems(BERNARDS_INTRODUCTION, -1);
			qs.giveItems(LETTER_OF_HAMIL1, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30626-03.htm";
		} else if ("30626_7".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TALISMAN_OF_KADESH)) {
				qs.takeItems(TALISMAN_OF_KADESH, -1);
				qs.giveItems(MARK_OF_SAGITTARIUS, 1);
				qs.addExpAndSp(54726, 20250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30626-13.htm";
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BERNARD) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30702-02.htm";
				} else {
					return "30702-01.htm";
				}
			} else if (cond == 1) {
				return "30702-05.htm";
			}
		} else if (npcId == HAMIL) {
			if (cond == 1) {
				return "30626-01.htm";
			} else if (cond == 14 && qs.hasQuestItems(TALISMAN_OF_KADESH)) {
				return "30626-12.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
