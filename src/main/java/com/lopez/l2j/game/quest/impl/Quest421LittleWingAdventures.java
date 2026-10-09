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
 * Quest 421: Little Wing's Big Adventure
 * Evolucao do Hatchling (nivel 55+) em Strider (Montaria).
 * Cronos em Hunters Village -> Fairy Mymyu em Enchanted Valley -> Leaf of Trees.
 */
@Component
public class Quest421LittleWingAdventures extends Quest {

	public static final int QUEST_ID = 421;
	public static final String QUEST_NAME = "421_LittleWingAdventures";

	public static final int CRONOS = 30610;
	public static final int MYMYU = 30747;

	// Flautas de Hatchling
	public static final int DRAGONFLUTE_OF_WIND = 3500;
	public static final int DRAGONFLUTE_OF_STAR = 3501;
	public static final int DRAGONFLUTE_OF_TWILIGHT = 3502;

	// Flautas de Strider
	public static final int STRIDER_WIND = 4422;
	public static final int STRIDER_STAR = 4423;
	public static final int STRIDER_TWILIGHT = 4424;

	public static final int FAIRY_LEAF = 4325;

	@Autowired
	public Quest421LittleWingAdventures(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Little Wing's Big Adventure");
		addStartNpc(CRONOS);
		addTalkId(CRONOS, MYMYU);
		registerQuestItems(FAIRY_LEAF);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if ("30610-05.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30747-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(FAIRY_LEAF, 4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("evolve_strider".equalsIgnoreCase(event) || "30747-06.htm".equalsIgnoreCase(event)) {
			// Converte a flauta do hatchling para a flauta do strider
			int striderFlute = STRIDER_WIND;
			if (qs.hasQuestItems(DRAGONFLUTE_OF_STAR)) {
				qs.takeItems(DRAGONFLUTE_OF_STAR, 1);
				striderFlute = STRIDER_STAR;
			} else if (qs.hasQuestItems(DRAGONFLUTE_OF_TWILIGHT)) {
				qs.takeItems(DRAGONFLUTE_OF_TWILIGHT, 1);
				striderFlute = STRIDER_TWILIGHT;
			} else {
				qs.takeItems(DRAGONFLUTE_OF_WIND, 1);
			}
			qs.takeItems(FAIRY_LEAF, -1);
			qs.giveItems(striderFlute, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "30747-06.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == CRONOS) {
			if (cond == 0) {
				if (pc.level() >= 45 && (qs.hasQuestItems(DRAGONFLUTE_OF_WIND) ||
						qs.hasQuestItems(DRAGONFLUTE_OF_STAR) || qs.hasQuestItems(DRAGONFLUTE_OF_TWILIGHT))) {
					return "30610-01.htm";
				}
				qs.exitQuest(true);
				return "30610-00.htm";
			}
			return "30610-06.htm";
		} else if (npcId == MYMYU) {
			if (cond == 1) return "30747-01.htm";
			if (cond == 2) {
				if (qs.getQuestItemsCount(FAIRY_LEAF) == 0) {
					return "30747-05.htm";
				}
				return "30747-03.htm";
			}
		}

		return "noquest";
	}
}
