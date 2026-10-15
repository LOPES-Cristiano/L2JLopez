package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 628: Hunt of the Golden Ram Mercenary Force
 * Mercenary Kahman (31554) em Swamp of Screams.
 * Subida de patente de Mercenário (Recruit -> Soldier) para ter acesso completo ao acampamento Golden Ram.
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

	private static final Map<Integer, Integer> CHANCES = Map.of(
			21508, 50,
			21509, 43,
			21510, 52,
			21511, 58,
			21512, 75,
			21513, 50,
			21514, 43,
			21515, 52,
			21516, 53,
			21517, 74
	);

	public Quest628HuntGoldenRam() {
		this(null);
	}

	@Autowired
	public Quest628HuntGoldenRam(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hunt of the Golden Ram Mercenary Force");
		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);
		addKillId(21508, 21509, 21510, 21511, 21512, 21513, 21514, 21515, 21516, 21517);
		registerQuestItems(SPLINTER_STAKATO_CHITIN, NEEDLE_STAKATO_CHITIN, RECRUIT_BADGE, SOLDIER_BADGE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)
				|| "31554-02.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			if (qs.hasQuestItems(SOLDIER_BADGE)) {
				qs.setCond(3);
				return "merc_kahmun_q0628_05.htm";
			} else if (qs.hasQuestItems(RECRUIT_BADGE)) {
				qs.setCond(2);
				return "merc_kahmun_q0628_04.htm";
			}
			qs.setCond(1);
			return "31554-02.htm".equalsIgnoreCase(event) ? "31554-02.htm" : "merc_kahmun_q0628_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event)
				|| "upgrade_badge".equalsIgnoreCase(event)
				|| "31554-03a.htm".equalsIgnoreCase(event)
				|| "31554-04.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) {
				qs.takeItems(SPLINTER_STAKATO_CHITIN, -1);
				qs.giveItems(RECRUIT_BADGE, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31554-03a.htm".equalsIgnoreCase(event) ? "31554-04.htm" : "merc_kahmun_q0628_08.htm";
			}
		} else if ("reply_2".equalsIgnoreCase(event)
				|| "31554-05.htm".equalsIgnoreCase(event)
				|| "31554-05a.htm".equalsIgnoreCase(event)
				|| "31554-05b.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100 && qs.hasQuestItems(RECRUIT_BADGE)) {
				qs.takeItems(SPLINTER_STAKATO_CHITIN, -1);
				qs.takeItems(NEEDLE_STAKATO_CHITIN, -1);
				qs.takeItems(RECRUIT_BADGE, -1);
				qs.giveItems(SOLDIER_BADGE, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_FINISH);
				return "31554-05.htm".equalsIgnoreCase(event) ? "31554-05.htm" : "merc_kahmun_q0628_11.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event)
				|| "31554-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(RECRUIT_BADGE, -1);
			qs.takeItems(SOLDIER_BADGE, -1);
			qs.takeItems(SPLINTER_STAKATO_CHITIN, -1);
			qs.takeItems(NEEDLE_STAKATO_CHITIN, -1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "31554-07.htm".equalsIgnoreCase(event) ? "31554-07.htm" : "merc_kahmun_q0628_13.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		int cond = qs.getCond();
		if (cond == 0 || qs.isCreated()) {
			if (pc.level() >= 66) {
				return "merc_kahmun_q0628_01.htm";
			}
			qs.exitQuest(true);
			return "merc_kahmun_q0628_02.htm";
		} else if (cond == 1) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) {
				return "merc_kahmun_q0628_04.htm";
			}
			return "merc_kahmun_q0628_06.htm";
		} else if (cond == 2) {
			if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100
					&& qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100) {
				qs.takeItems(SPLINTER_STAKATO_CHITIN, -1);
				qs.takeItems(NEEDLE_STAKATO_CHITIN, -1);
				qs.takeItems(RECRUIT_BADGE, -1);
				qs.giveItems(SOLDIER_BADGE, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_FINISH);
				return "merc_kahmun_q0628_10.htm";
			} else if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100) {
				return "merc_kahmun_q0628_09.htm";
			}
			return "merc_kahmun_q0628_10.htm";
		}
		return "merc_kahmun_q0628_12.htm";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int cond = qs.getCond();
		int npcId = npc.getNpcId();
		int chance = CHANCES.getOrDefault(npcId, 50);

		if (npcId >= 21508 && npcId <= 21512) {
			if ((cond == 1 || cond == 2) && qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) < 100) {
				if (ThreadLocalRandom.current().nextInt(100) < chance) {
					qs.giveItems(SPLINTER_STAKATO_CHITIN, 1);
					if (qs.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) {
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		} else if (npcId >= 21513 && npcId <= 21517) {
			if (cond == 2 && qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) < 100) {
				if (ThreadLocalRandom.current().nextInt(100) < chance) {
					qs.giveItems(NEEDLE_STAKATO_CHITIN, 1);
					if (qs.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100) {
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}
		return null;
	}
}
