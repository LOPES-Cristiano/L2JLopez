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
 * Quest 229: Test of the Witchcraft
 * 3º Passo de 2ª Classe para Necromancer e Spellhowler.
 */
@Component
public class Quest229TestOfWitchcraft extends Quest {

	public static final int QUEST_ID = 229;
	public static final String QUEST_NAME = "229_TestOfWitchcraft";

	// NPCs
	public static final int ORIM = 30630;
	public static final int ALEXANDRIA = 30098;
	public static final int IKER = 30110;
	public static final int VASPER = 30417;
	public static final int VADIN = 30188;

	// Itens
	public static final int MARK_OF_WITCHCRAFT = 3307;
	public static final int ORIMS_DIAGRAM = 3308;
	public static final int ALEXANDRIAS_BOOK = 3309;
	public static final int IKERS_LIST = 3310;
	public static final int BRIMSTONE1 = 3323;
	public static final int PURGATORY_KEY = 3333;
	public static final int ZERUEL_BIND_CRYSTAL = 3334;

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 39);

	@Autowired
	public Quest229TestOfWitchcraft(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Witchcraft");

		addStartNpc(ORIM);
		addTalkId(ORIM, ALEXANDRIA, IKER, VASPER, VADIN);

		registerQuestItems(ORIMS_DIAGRAM, ALEXANDRIAS_BOOK, IKERS_LIST, BRIMSTONE1, PURGATORY_KEY, ZERUEL_BIND_CRYSTAL);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30630-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ORIMS_DIAGRAM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30630-08.htm";
		} else if ("30630-22.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ZERUEL_BIND_CRYSTAL)) {
				qs.takeItems(ZERUEL_BIND_CRYSTAL, -1);
				qs.giveItems(MARK_OF_WITCHCRAFT, 1);
				qs.addExpAndSp(139039, 16000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
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

		if (npcId == ORIM) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30630-03.htm";
				} else {
					return "30630-01.htm";
				}
			} else if (cond == 1) {
				return "30630-09.htm";
			} else if (cond >= 14 && qs.hasQuestItems(ZERUEL_BIND_CRYSTAL)) {
				return "30630-21.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
