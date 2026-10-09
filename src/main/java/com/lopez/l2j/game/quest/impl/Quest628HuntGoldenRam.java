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
 * Quest 628: Hunt of the Golden Ram Mercenary Force
 * Mercenary Kahman em Swamp of Screams / Beast Farm.
 * Subida de patente de Mercenario (Recruit -> Soldier -> Elite) para ter acesso ao Stakato Nest.
 */
@Component
public class Quest628HuntGoldenRam extends Quest {

	public static final int QUEST_ID = 628;
	public static final String QUEST_NAME = "628_HuntGoldenRam";

	public static final int KAHMAN = 31554;

	public static final int RECRUIT_BADGE = 7246;
	public static final int SOLDIER_BADGE = 7247;
	public static final int SPLINTER_STAKATO_CHITIN = 7248;
	public static final int NEEDLE_STAKATO_CHITIN = 7249;

	// Aliases de compatibilidade para testes
	public static final int PIERCE = KAHMAN;
	public static final int CHITIN = SPLINTER_STAKATO_CHITIN;

	public static final int[] MOBS = {
			21508, 21509, 21510, 21511, 21512, 21513, 21514, 21515, 21516, 21517
	};

	@Autowired
	public Quest628HuntGoldenRam(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hunt of the Golden Ram Mercenary Force");
		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);
		addKillId(MOBS);
		registerQuestItems(SPLINTER_STAKATO_CHITIN, NEEDLE_STAKATO_CHITIN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event))) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "merc_kahmun_q0628_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event) || "upgrade_badge".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) {
				qs.takeItems(SPLINTER_STAKATO_CHITIN, -1);
				qs.giveItems(RECRUIT_BADGE, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "merc_kahmun_q0628_08.htm";
			}
		} else if ("reply_2".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100 && qs.hasQuestItems(RECRUIT_BADGE)) {
				qs.takeItems(NEEDLE_STAKATO_CHITIN, -1);
				qs.takeItems(RECRUIT_BADGE, -1);
				qs.giveItems(SOLDIER_BADGE, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "merc_kahmun_q0628_11.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int cond = qs.getCond();
		if (cond == 0) {
			if (pc.level() >= 66) return "merc_kahmun_q0628_01.htm";
			qs.exitQuest(true);
			return "merc_kahmun_q0628_02.htm";
		} else if (cond == 1) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) {
				return "merc_kahmun_q0628_04.htm";
			}
			return "merc_kahmun_q0628_06.htm";
		} else if (cond == 2) {
			if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100) {
				return "merc_kahmun_q0628_09.htm";
			}
			return "merc_kahmun_q0628_10.htm";
		}
		return "merc_kahmun_q0628_12.htm";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int cond = qs.getCond();
		if (cond == 1) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) < 100) {
				qs.giveItems(SPLINTER_STAKATO_CHITIN, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 2) {
			if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) < 100) {
				qs.giveItems(NEEDLE_STAKATO_CHITIN, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
