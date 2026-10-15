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
 * Quest 266 - Plea Of Pixies
 */
@Component
public class Quest266PleaOfPixies extends Quest {

	public static final int PIXY_MURIKA = 31852;
	public static final int PREDATORS_FANG = 1334;
	public static final int EMERALD = 1337;
	public static final int BLUE_ONYX = 1338;
	public static final int ONYX = 1339;
	public static final int GLASS_SHARD = 1336;
	public static final int REC_LEATHER_BOOT = 2176;
	public static final int REC_SPIRITSHOT = 3032;

	public static final int bdM = 1334;
	public static final int bdN = 1337;
	public static final int bdO = 1338;
	public static final int bdP = 1339;
	public static final int bdQ = 1336;
	public static final int bdR = 2176;
	public static final int bdS = 3032;

	public Quest266PleaOfPixies(QuestManager questManager) {
		super(266, "266_PleaOfPixies", "Plea Of Pixies");
		addStartNpc(31852);
		addTalkId(31852);
		addKillId(20525, 20530, 20534, 20537);
		registerQuestItems(1334);
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

		String event2 = event;
		if (event.equalsIgnoreCase("pixy_murika_q0266_03.htm") || event.equalsIgnoreCase("31852-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		return event2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		if (qs.getCond() == 0) {
			if (pc.race() != 1) {
				html = "pixy_murika_q0266_00.htm";
				qs.exitQuest(true);
			} else if (pc.level() < 3) {
				html = "pixy_murika_q0266_01.htm";
				qs.exitQuest(true);
			} else {
				html = "pixy_murika_q0266_02.htm";
			}
		} else if (qs.getQuestItemsCount(PREDATORS_FANG) < 100) {
			html = "pixy_murika_q0266_04.htm";
		} else {
			qs.takeItems(PREDATORS_FANG, -1);
			int n = ThreadLocalRandom.current().nextInt(100);
			if (n < 2) {
				qs.giveItems(EMERALD, 1);
				qs.giveItems(REC_SPIRITSHOT, 1);
				qs.playSound(QuestState.SOUND_JACKPOT);
			} else if (n < 20) {
				qs.giveItems(BLUE_ONYX, 1);
				qs.giveItems(REC_LEATHER_BOOT, 1);
			} else if (n < 45) {
				qs.giveItems(ONYX, 1);
			} else {
				qs.giveItems(GLASS_SHARD, 1);
			}
			html = "pixy_murika_q0266_05.htm";
			qs.exitQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1) {
			qs.rollAndGive(PREDATORS_FANG, 1, 1, 100, 60 + (npc != null ? npc.getLevel() * 5 : 0));
			if (qs.getQuestItemsCount(PREDATORS_FANG) >= 100) {
				qs.setCond(2);
			}
		}
		return null;
	}
}
