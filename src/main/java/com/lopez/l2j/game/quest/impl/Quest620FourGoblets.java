package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 620: Four Goblets
 * Acesso ao Four Sepulchers, obtenção dos 4 Cálices sagrados e do Antique Brooch para Frintezza.
 */
@Component
public class Quest620FourGoblets extends Quest {

	public static final int QUEST_ID = 620;
	public static final String QUEST_NAME = "620_FourGoblets";

	// NPCs
	public static final int NAMELESS_SPIRIT = 31453;
	public static final int GHOST_OF_WIGOTH_1 = 31452;
	public static final int GHOST_OF_WIGOTH_2 = 31454;

	// Monstros / Raid Bosses Halisha
	public static final int HALISHA_1 = 25339;
	public static final int HALISHA_2 = 25342;
	public static final int HALISHA_3 = 25346;
	public static final int HALISHA_4 = 25349;

	// Itens
	public static final int RELIC = 7254;
	public static final int SEALED_BOX = 7255;
	public static final int GOBLET_ALECTIA = 7256;
	public static final int GOBLET_TISHA = 7257;
	public static final int GOBLET_MEKARA = 7258;
	public static final int GOBLET_MORIGUL = 7259;
	public static final int GRAVE_PASS = 7261;
	public static final int ANTIQUE_BROOCH = 7262;

	// S-Grade Recipes
	public static final int[] S_RECIPES = {6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580};

	@Autowired
	public Quest620FourGoblets(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Four Goblets");

		addStartNpc(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT, GHOST_OF_WIGOTH_1, GHOST_OF_WIGOTH_2);

		addKillId(HALISHA_1, HALISHA_2, HALISHA_3, HALISHA_4);

		registerQuestItems(GOBLET_ALECTIA, GOBLET_TISHA, GOBLET_MEKARA, GOBLET_MORIGUL, SEALED_BOX, GRAVE_PASS);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31453-13.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("31453-16.htm".equalsIgnoreCase(event)) {
			if (hasAllFourGoblets(qs)) {
				qs.takeItems(GOBLET_ALECTIA, -1);
				qs.takeItems(GOBLET_TISHA, -1);
				qs.takeItems(GOBLET_MEKARA, -1);
				qs.takeItems(GOBLET_MORIGUL, -1);
				qs.giveItems(ANTIQUE_BROOCH, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_FINISH);
				return event;
			}
		} else if ("OpenBox".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SEALED_BOX)) {
				qs.takeItems(SEALED_BOX, 1);
				int roll = ThreadLocalRandom.current().nextInt(100);
				if (roll < 30) {
					int rcp = S_RECIPES[ThreadLocalRandom.current().nextInt(S_RECIPES.length)];
					qs.giveItems(rcp, 1);
				} else if (roll < 70) {
					qs.giveItems(57, 10000); // 10k Adena
				}
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31454-BoxOpened.htm";
			}
		}

		return event;
	}

	private boolean hasAllFourGoblets(QuestState qs) {
		return qs.hasQuestItems(GOBLET_ALECTIA) && qs.hasQuestItems(GOBLET_TISHA)
				&& qs.hasQuestItems(GOBLET_MEKARA) && qs.hasQuestItems(GOBLET_MORIGUL);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == NAMELESS_SPIRIT) {
			if (cond == 0) {
				if (player != null && player.level() >= 74) {
					return "31453-1.htm";
				} else {
					return "31453-12.htm";
				}
			} else if (cond == 1) {
				if (hasAllFourGoblets(qs)) {
					return "31453-15.htm";
				}
				return "31453-14.htm";
			} else if (cond == 2) {
				return "31453-17.htm";
			}
		} else if (npcId == GHOST_OF_WIGOTH_1 || npcId == GHOST_OF_WIGOTH_2) {
			if (cond >= 1) {
				return "31454-1.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		if (npcId == HALISHA_1 && !qs.hasQuestItems(GOBLET_ALECTIA)) {
			qs.giveItems(GOBLET_ALECTIA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_2 && !qs.hasQuestItems(GOBLET_TISHA)) {
			qs.giveItems(GOBLET_TISHA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_3 && !qs.hasQuestItems(GOBLET_MEKARA)) {
			qs.giveItems(GOBLET_MEKARA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_4 && !qs.hasQuestItems(GOBLET_MORIGUL)) {
			qs.giveItems(GOBLET_MORIGUL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}
