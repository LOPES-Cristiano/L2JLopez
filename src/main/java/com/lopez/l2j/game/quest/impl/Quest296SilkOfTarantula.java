package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 296 - 296_SilkOfTarantula
 */
@Component
public class Quest296SilkOfTarantula extends Quest {

	public static final int MION = 30519;
	public static final int DEFENDER_NATHAN = 30548;
	public static final int CRIMSON_TARANTULA = 20394;
	public static final int HUNTER_TARANTULA = 20403;
	public static final int PLUNDER_TARANTULA = 20508;
	public static final int TARANTULA_SPIDER_SILK = 1493;
	public static final int TARANTULA_SPINNERETTE = 1494;
	public static final int RING_OF_RACCOON = 1508;
	public static final int RING_OF_FIREFLY = 1509;

	public static final int beQ = 30519;
	public static final int beR = 30548;
	public static final int beS = 20394;
	public static final int beT = 20403;
	public static final int beU = 20508;
	public static final int beV = 1493;
	public static final int beW = 1494;
	public static final int beX = 1508;
	public static final int beY = 1509;

	public Quest296SilkOfTarantula(QuestManager questManager) {
		super(296, "296_SilkOfTarantula", "296_SilkOfTarantula");
		addStartNpc(30519);
		addTalkId(30519, 30548);
		addKillId(20508, 20403, 20394);
		registerQuestItems(1493, 1494);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)
				|| "30519-03.htm".equalsIgnoreCase(event) || "trader_mion_q0296_03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "trader_mion_q0296_03.htm";
		}

		String string2 = event;
		if (event.equalsIgnoreCase("reply_1") || event.equalsIgnoreCase("30519-06.htm") || event.equalsIgnoreCase("trader_mion_q0296_06.htm")) {
			qs.takeItems(TARANTULA_SPIDER_SILK, -1L);
			qs.takeItems(TARANTULA_SPINNERETTE, -1L);
			string2 = "trader_mion_q0296_06.htm";
			qs.exitQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
		} else if (event.equalsIgnoreCase("reply_2") || event.equalsIgnoreCase("30519-07.htm") || event.equalsIgnoreCase("trader_mion_q0296_07.htm")) {
			string2 = "trader_mion_q0296_07.htm";
		} else if (event.equalsIgnoreCase("reply_3") || event.equalsIgnoreCase("30548-02.htm") || event.equalsIgnoreCase("defender_nathan_q0296_03.htm")) {
			if (qs.getQuestItemsCount(TARANTULA_SPINNERETTE) >= 1L) {
				string2 = "defender_nathan_q0296_03.htm";
				qs.giveItems(TARANTULA_SPIDER_SILK, (long) (15 + ThreadLocalRandom.current().nextInt(9)) * qs.getQuestItemsCount(TARANTULA_SPINNERETTE));
				qs.takeItems(TARANTULA_SPINNERETTE, -1L);
			} else {
				string2 = "defender_nathan_q0296_02.htm";
			}
		}
		return string2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getStateId();
		switch (n2) {
			case 1: {
				if (n != MION) break;
				if (pc.getLevel() < 15) {
					html = "trader_mion_q0296_01.htm";
					qs.exitQuest(true);
					break;
				}
				if (qs.getQuestItemsCount(RING_OF_RACCOON) > 0L || qs.getQuestItemsCount(RING_OF_FIREFLY) > 0L) {
					html = "trader_mion_q0296_02.htm";
					break;
				}
				html = "trader_mion_q0296_08.htm";
				break;
			}
			case 2: {
				if (n == MION) {
					if (qs.getQuestItemsCount(TARANTULA_SPIDER_SILK) < 1L) {
						html = "trader_mion_q0296_04.htm";
					} else if (qs.getQuestItemsCount(TARANTULA_SPIDER_SILK) >= 1L) {
						html = "trader_mion_q0296_05.htm";
						if (qs.getQuestItemsCount(TARANTULA_SPIDER_SILK) >= 10L) {
							qs.giveItems(57, qs.getQuestItemsCount(TARANTULA_SPIDER_SILK) * 30L + 2000L);
						} else {
							qs.giveItems(57, qs.getQuestItemsCount(TARANTULA_SPIDER_SILK) * 30L);
						}
						qs.takeItems(TARANTULA_SPIDER_SILK, -1L);
						qs.playSound(QuestState.SOUND_FINISH);
						qs.exitQuest(true);
					}
				}
				if (n != DEFENDER_NATHAN) break;
				html = "defender_nathan_q0296_01.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc != null ? npc.getNpcId() : 0;
		if (n == CRIMSON_TARANTULA || n == HUNTER_TARANTULA || n == PLUNDER_TARANTULA) {
			int n2 = ThreadLocalRandom.current().nextInt(100);
			if (n2 > 95) {
				qs.rollAndGive(TARANTULA_SPINNERETTE, 1, 100.0);
			} else if (n2 > 45) {
				qs.rollAndGive(TARANTULA_SPIDER_SILK, 1, 100.0);
			}
		}
		return null;
	}
}
