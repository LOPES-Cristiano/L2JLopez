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
 * Quest 022: Tragedy in von Hellmann Forest
 * High Priest Tifaren em Rune, Innocentin e Well em Forest of the Dead.
 */
@Component
public class Quest022TragedyInVonHellmannForest extends Quest {

	public static final int QUEST_ID = 22;
	public static final String QUEST_NAME = "022_TragedyInVonHellmannForest";

	public static final int TIFAREN = 31334;
	public static final int INNOCENTIN = 31328;
	public static final int WELL = 31527;

	public static final int CROSS_OF_EINHASAD = 7141;
	public static final int SKULL_OF_CRUEL = 7142;
	public static final int LETTER_OF_HELLMANN = 7143;

	public static final int[] MOBS = {21553, 21554, 21555, 21556, 21561};

	@Autowired
	public Quest022TragedyInVonHellmannForest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Tragedy in von Hellmann Forest");
		addStartNpc(TIFAREN);
		addTalkId(TIFAREN, INNOCENTIN, WELL);
		addKillId(MOBS);
		registerQuestItems(SKULL_OF_CRUEL, LETTER_OF_HELLMANN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "grandmagister_tifaren_q0022_04.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == TIFAREN) {
			if (cond == 0) {
				if (pc.level() >= 63 && qs.hasQuestItems(CROSS_OF_EINHASAD)) return "grandmagister_tifaren_q0022_01.htm";
				qs.exitQuest(true);
				return "grandmagister_tifaren_q0022_03.htm";
			} else if (cond == 1) {
				return "grandmagister_tifaren_q0022_05.htm";
			} else if (cond == 2 && qs.hasQuestItems(SKULL_OF_CRUEL)) {
				qs.takeItems(SKULL_OF_CRUEL, -1);
				qs.giveItems(LETTER_OF_HELLMANN, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "grandmagister_tifaren_q0022_07.htm";
			}
		} else if (npcId == INNOCENTIN) {
			if (cond == 3 && qs.hasQuestItems(LETTER_OF_HELLMANN)) {
				qs.takeItems(LETTER_OF_HELLMANN, -1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "highpriest_innocentin_q0022_02.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 1 && !qs.hasQuestItems(SKULL_OF_CRUEL)) {
			qs.giveItems(SKULL_OF_CRUEL, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
