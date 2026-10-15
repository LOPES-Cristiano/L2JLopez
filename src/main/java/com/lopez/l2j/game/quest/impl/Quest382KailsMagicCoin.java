package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 382 - 382_KailsMagicCoin
 */
@Component
public class Quest382KailsMagicCoin extends Quest {

	public static final int bxI = 5898; // ROYAL_MEMBERSHIP
	public static final int bxO = 30687; // VERGARA
	public static final int SILVER_BASILISK = 5961;
	public static final int GOLD_GOLEM = 5962;
	public static final int BLOOD_DRAGON = 5963;

	public static final Map<Integer, int[]> bxP = new HashMap<>();
	static {
		bxP.put(21017, new int[]{SILVER_BASILISK});
		bxP.put(21019, new int[]{GOLD_GOLEM});
		bxP.put(21020, new int[]{BLOOD_DRAGON});
		bxP.put(21022, new int[]{SILVER_BASILISK, GOLD_GOLEM, BLOOD_DRAGON});
	}

	public Quest382KailsMagicCoin(QuestManager questManager) {
		super(382, "382_KailsMagicCoin", "382_KailsMagicCoin");
		addStartNpc(bxO);
		addTalkId(bxO);
		addKillId(21017, 21019, 21020, 21022);
		registerQuestItems(SILVER_BASILISK, GOLD_GOLEM, BLOOD_DRAGON);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "head_blacksmith_vergara_q0382_03.htm";
		}

		String string2 = event;
		if (event.equalsIgnoreCase("head_blacksmith_vergara_q0382_03.htm") || event.equalsIgnoreCase("30687-03.htm")) {
			if (pc != null && pc.getLevel() >= 55 && qs.getQuestItemsCount(bxI) > 0L) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				string2 = "head_blacksmith_vergara_q0382_03.htm";
			} else {
				string2 = "head_blacksmith_vergara_q0382_01.htm";
				qs.exitQuest(true);
			}
		} else if (event.equalsIgnoreCase("list")) {
			string2 = null;
		}
		return string2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = qs.getCond();
		if (qs.getQuestItemsCount(bxI) == 0L || pc.getLevel() < 55) {
			html = "head_blacksmith_vergara_q0382_01.htm";
			qs.exitQuest(true);
		} else {
			html = n == 0 ? "head_blacksmith_vergara_q0382_02.htm" : "head_blacksmith_vergara_q0382_04.htm";
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null || qs.getState() != State.STARTED || qs.getQuestItemsCount(bxI) == 0L) {
			return null;
		}
		int[] nArray = bxP.get(npc.getNpcId());
		if (nArray != null && nArray.length > 0) {
			qs.rollAndGive(nArray[ThreadLocalRandom.current().nextInt(nArray.length)], 1, 10.0);
		}
		return null;
	}
}
