package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;

/**
 * Quest4000ShadowWeapons: Troca de Shadow Weapon Exchange Coupons (D e C) por armas shadow temporárias.
 * Retail ID 4000.
 */
public class Quest4000ShadowWeapons extends Quest {

	public static final int QUEST_ID = 4000;
	public static final String QUEST_NAME = "4000_ShadowWeapons";

	public static final int D_COUPON = 8869;
	public static final int C_COUPON = 8870;

	private static final int[] NPCS = {
			30037, 30066, 30070, 30109, 30115, 30120, 30174, 30175, 30176, 30187, 30191, 30195,
			30288, 30289, 30290, 30297, 30373, 30462, 30474, 30498, 30499, 30500, 30503, 30504,
			30505, 30508, 30511, 30512, 30513, 30594, 30595, 30676, 30677, 30681, 30685, 30687,
			30689, 30694, 30699, 30704, 30845, 30847, 30849, 30854, 30857, 30862, 30865, 30894,
			30897, 30900, 30905, 30910, 30913, 31269, 31272, 31276, 31279, 31285, 31288, 31314,
			31317, 31321, 31324, 31326, 31328, 31331, 31334, 31336, 31958, 31961, 31965, 31968,
			31974, 31977, 31996, 32092, 32093, 32094, 32095, 32096, 32097, 32098, 32193, 32196,
			32199, 32202, 32205, 32206, 32209, 32210, 32213, 32214, 32217, 32218, 32221, 32222,
			32225, 32226, 32229, 32230, 32233, 32234
	};

	public Quest4000ShadowWeapons() {
		this(null);
	}

	public Quest4000ShadowWeapons(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Custom");
		for (int npcId : NPCS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession session) {
		if (session == null || session.activeChar() == null) {
			return null;
		}
		QuestState qs = checkQuestState(session);
		if (qs == null) {
			qs = newQuestState(session);
		}

		long hasD = qs.getQuestItemsCount(D_COUPON);
		long hasC = qs.getQuestItemsCount(C_COUPON);

		if (hasD > 0 || hasC > 0) {
			int multisell = 306893003;
			if (hasD == 0) {
				multisell = 306893002;
			} else if (hasC == 0) {
				multisell = 306893001;
			}
			session.showMultiSell(npc, multisell);
			qs.exitQuest(true);
			return null;
		} else {
			qs.exitQuest(true);
			return "exchange-no.htm";
		}
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession session) {
		QuestState qs = checkQuestState(session);
		if (qs == null) {
			return null;
		}
		return "exchange.htm";
	}
}
