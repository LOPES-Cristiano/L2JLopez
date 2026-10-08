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
 * Quest 247: Possessor of a Precious Soul - Part 4 (Consagração do jogador como Nobless e entrega da Noblesse Tiara).
 */
@Component
public class Quest247PossessorOfAPreciousSoul4 extends Quest {

	public static final int QUEST_ID = 247;
	public static final String QUEST_NAME = "247_PossessorOfAPreciousSoul_4";

	// NPCs
	public static final int CARADINE = 31740;
	public static final int LADY_OF_THE_LAKE = 31745;

	// Itens
	public static final int CARADINE_LETTER = 7679;
	public static final int NOBLESS_TIARA = 7694;

	@Autowired
	public Quest247PossessorOfAPreciousSoul4(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Possessor of a Precious Soul - Part 4");

		addStartNpc(CARADINE);
		addTalkId(CARADINE);
		addTalkId(LADY_OF_THE_LAKE);

		registerQuestItems(NOBLESS_TIARA);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();

		if ("31740-03.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound("ItemSound.quest_accept");
			qs.takeItems(CARADINE_LETTER, -1);
			return event;
		} else if ("31740-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("31745-05.htm".equalsIgnoreCase(event)) {
			if (player != null) {
				player.setNoble(true);
			}
			qs.giveItems(NOBLESS_TIARA, 1);
			qs.addExpAndSp(93836, 0);
			qs.playSound("ItemSound.quest_finish");
			qs.exitCurrentQuest(false);
			return event;
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

		if (npcId == CARADINE) {
			if (cond == 0) {
				if (qs.hasQuestItems(CARADINE_LETTER)) {
					if (player != null && player.getLevel() >= 75) {
						return "31740-01.htm";
					}
					return "31740-02.htm";
				}
				return "31740-02.htm";
			} else if (cond == 1) {
				return "31740-04.htm";
			} else if (cond == 2) {
				return "31740-06.htm";
			}
		} else if (npcId == LADY_OF_THE_LAKE) {
			if (cond == 2) {
				if (player != null && player.getLevel() < 75) {
					return "31745-06.htm";
				}
				return "31745-01.htm";
			}
		}

		return "noquest";
	}
}
