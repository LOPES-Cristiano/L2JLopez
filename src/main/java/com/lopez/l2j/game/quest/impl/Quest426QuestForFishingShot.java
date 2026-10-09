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
 * Quest 426: Quest for Fishing Shot
 * Guild de Pesca em todas as cidades.
 * Coleta de Sweet Fluid (7586) de monstros para fabricar Fishing Shots.
 */
@Component
public class Quest426QuestForFishingShot extends Quest {

	public static final int QUEST_ID = 426;
	public static final String QUEST_NAME = "426_FishingShot";

	public static final int SWEET_FLUID = 7586;

	// Fishing Guild Members
	public static final int[] GUILD_MEMBERS = {
			31562, 31563, 31564, 31565, 31566, 31567, 31568, 31569, 31570,
			31571, 31572, 31573, 31574, 31575, 31576, 31577, 31578, 31579,
			31696, 31697, 31989, 32007
	};

	// Mobs selecionados das principais zonas de caca
	public static final int[] HUNT_MOBS = {
			20074, 20077, 20079, 20080, 20081, 20084, 20087, 20088,
			20819, 20812, 20456, 21024, 20386, 22046, 20772, 21058,
			21060, 20983, 20985, 20043, 20317, 20814, 20794, 20796
	};

	// Fishing Shot Item IDs por Grade (No-Grade ate S-Grade)
	public static final int FISHING_SHOT_NG = 6535;
	public static final int FISHING_SHOT_D = 6536;
	public static final int FISHING_SHOT_C = 6537;
	public static final int FISHING_SHOT_B = 6538;
	public static final int FISHING_SHOT_A = 6539;
	public static final int FISHING_SHOT_S = 6540;

	@Autowired
	public Quest426QuestForFishingShot(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Quest for Fishing Shot");
		addStartNpc(GUILD_MEMBERS);
		addTalkId(GUILD_MEMBERS);
		addKillId(HUNT_MOBS);
		registerQuestItems(SWEET_FLUID);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "fisher_berix_q0426_03.htm";
		} else if ("reward_ng".equalsIgnoreCase(event)) {
			long fluid = qs.getQuestItemsCount(SWEET_FLUID);
			if (fluid >= 1) {
				qs.takeItems(SWEET_FLUID, 1);
				qs.giveItems(FISHING_SHOT_NG, 66);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "fisher_berix_q0426_06.htm";
		} else if ("reward_d".equalsIgnoreCase(event)) {
			long fluid = qs.getQuestItemsCount(SWEET_FLUID);
			if (fluid >= 1) {
				qs.takeItems(SWEET_FLUID, 1);
				qs.giveItems(FISHING_SHOT_D, 40);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "fisher_berix_q0426_06.htm";
		} else if ("reply_3".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "fisher_berix_q0426_08.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		int cond = qs.getCond();
		if (qs.getState() == State.CREATED) {
			return "fisher_berix_q0426_01.htm";
		} else if (cond == 1) {
			if (qs.getQuestItemsCount(SWEET_FLUID) == 0) {
				return "fisher_berix_q0426_04.htm";
			}
			return "fisher_berix_q0426_05.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 1 && ThreadLocalRandom.current().nextInt(100) < 30) {
			qs.giveItems(SWEET_FLUID, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
