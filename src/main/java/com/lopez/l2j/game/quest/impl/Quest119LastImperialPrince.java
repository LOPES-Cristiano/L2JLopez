package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 119: Last Imperial Prince (Acesso ao Last Imperial Tomb e Grand Boss Frintezza).
 */
@Component
public class Quest119LastImperialPrince extends Quest {

	public static final int QUEST_ID = 119;
	public static final String QUEST_NAME = "119_LastImperialPrince";

	// NPCs
	public static final int NAMELESS_SPIRIT = 31453;
	public static final int DEVORIN = 32009;

	// Itens
	public static final int ANTIQUE_BROOCH = 7262;
	public static final int FRINTEZZA_SCROLL = 8073;

	@Autowired
	public Quest119LastImperialPrince(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Last Imperial Prince");

		addStartNpc(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT);
		addTalkId(DEVORIN);

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

		if ("31453-4.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31453-4.htm";
		} else if ("32009-2.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ANTIQUE_BROOCH) < 1) {
				qs.exitQuest(true);
				return "<html><body>Quest Four Goblets is not accomplished or the condition is not suitable.</body></html>";
			}
			return "32009-2.htm";
		} else if ("32009-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32009-3.htm";
		} else if ("31453-7.htm".equalsIgnoreCase(event)) {
			qs.giveItems(57, 68787); // Adena
			qs.giveItems(FRINTEZZA_SCROLL, 1);
			qs.setState(State.COMPLETED);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31453-7.htm";
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

		if (qs.getQuestItemsCount(ANTIQUE_BROOCH) < 1) {
			qs.exitQuest(true);
			return "<html><body>Quest Four Goblets is not accomplished or the condition is not suitable.</body></html>";
		}

		if (npcId == NAMELESS_SPIRIT) {
			if (cond == 0) {
				if (c.level() < 74) {
					return "<html><body>Quest for characters level 74 and above.</body></html>";
				} else {
					return "31453-1.htm";
				}
			} else if (cond == 1) {
				return "31453-4.htm";
			} else if (cond == 2) {
				return "31453-5.htm";
			}
		} else if (npcId == DEVORIN) {
			if (cond == 1) {
				return "32009-1.htm";
			} else if (cond == 2) {
				return "32009-3.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}
}
