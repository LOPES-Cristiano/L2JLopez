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
 * Quest 275 - Black Winged Spies
 */
@Component
public class Quest275BlackWingedSpies extends Quest {

	public static final int TANTUS = 30567;
	public static final int DARKWING_BAT = 20316;
	public static final int VARANGKA_TRACKER = 27043;
	public static final int DARKWING_BAT_FANG = 1478;
	public static final int VARANGKA_PARASITE = 1479;

	public static final int aXM = 30567;
	public static final int bel = 20316;
	public static final int bem = 27043;
	public static final int ben = 1478;
	public static final int beo = 1479;
	public static final int bep = 10;

	public Quest275BlackWingedSpies(QuestManager questManager) {
		super(275, "275_BlackWingedSpies", "Black Winged Spies");
		addStartNpc(30567);
		addTalkId(30567);
		addKillId(20316, 27043);
		registerQuestItems(1478, 1479);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		String html = event;

		if ((event.equalsIgnoreCase("neruga_chief_tantus_q0275_03.htm") || event.equalsIgnoreCase("30567-03.htm")) && qs.getState() == State.CREATED) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		return html;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (npc != null && npc.getNpcId() != TANTUS) {
			return "noquest";
		}
		int n = qs.getStateId();
		if (n == 1) {
			if (pc.race() != 3) {
				qs.exitQuest(true);
				return "neruga_chief_tantus_q0275_00.htm";
			}
			if (pc.level() < 11) {
				qs.exitQuest(true);
				return "neruga_chief_tantus_q0275_01.htm";
			}
			qs.setCond(0);
			return "neruga_chief_tantus_q0275_02.htm";
		}
		if (n != 2) {
			return "noquest";
		}
		int n2 = qs.getCond();
		if (qs.getQuestItemsCount(DARKWING_BAT_FANG) < 70) {
			if (n2 != 1) {
				qs.setCond(1);
			}
			return "neruga_chief_tantus_q0275_04.htm";
		}
		if (n2 == 2) {
			qs.takeItems(DARKWING_BAT_FANG, -1);
			qs.takeItems(VARANGKA_PARASITE, -1);
			qs.giveItems(57, 4550);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "neruga_chief_tantus_q0275_05.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getState() != State.STARTED) {
			return null;
		}
		int n = npc != null ? npc.getNpcId() : 0;
		long l = qs.getQuestItemsCount(DARKWING_BAT_FANG);
		if (n == DARKWING_BAT && l < 70) {
			giveDarkwingBatFang(qs, 1);
			if (l > 10 && l < 66 && ThreadLocalRandom.current().nextInt(100) < 10 && qs.getQuestItemsCount(VARANGKA_PARASITE) == 0) {
				qs.addSpawn(VARANGKA_TRACKER);
				qs.giveItems(VARANGKA_PARASITE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (n == VARANGKA_TRACKER && l < 70 && qs.getQuestItemsCount(VARANGKA_PARASITE) > 0) {
			qs.takeItems(VARANGKA_PARASITE, -1);
			giveDarkwingBatFang(qs, 5);
		}
		return null;
	}

	private void giveDarkwingBatFang(QuestState qs, int count) {
		long current = qs.getQuestItemsCount(DARKWING_BAT_FANG);
		long toGive = Math.min(count, 70 - current);
		if (toGive > 0) {
			qs.giveItems(DARKWING_BAT_FANG, toGive);
		}
		if (qs.getQuestItemsCount(DARKWING_BAT_FANG) >= 70) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}
}
