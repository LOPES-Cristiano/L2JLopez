package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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
 * Quest 633 - 633_InTheForgottenVillage
 */
@Component
public class Quest633InTheForgottenVillage extends Quest {

	public static final int bQu = 31388;
	public static final int bQv = 7544;
	public static final int bQw = 7545;
	public static final Map<Integer, Double> bQx = new HashMap<Integer, Double>();
	public static final Map<Integer, Double> bQy = new HashMap<Integer, Double>();

	public Quest633InTheForgottenVillage(QuestManager questManager) {
	super(633, "633_InTheForgottenVillage", "633_InTheForgottenVillage");
		bQx.put(21557, 32.8);
		bQx.put(21558, 32.8);
		bQx.put(21559, 33.7);
		bQx.put(21560, 33.7);
		bQx.put(21563, 34.2);
		bQx.put(21564, 34.8);
		bQx.put(21565, 35.1);
		bQx.put(21566, 35.9);
		bQx.put(21567, 35.9);
		bQx.put(21572, 36.5);
		bQx.put(21574, 38.3);
		bQx.put(21575, 38.3);
		bQx.put(21580, 38.5);
		bQx.put(21581, 39.5);
		bQx.put(21583, 39.7);
		bQx.put(21584, 40.1);
		bQy.put(21553, 34.7);
		bQy.put(21554, 34.7);
		bQy.put(21561, 45.0);
		bQy.put(21578, 50.1);
		bQy.put(21596, 35.9);
		bQy.put(21597, 37.0);
		bQy.put(21598, 44.1);
		bQy.put(21599, 39.5);
		bQy.put(21600, 40.8);
		bQy.put(21601, 41.1);
		this.addStartNpc(bQu);
		this.addQuestItem(bQv);
		for (int n : bQy.keySet()) {
		this.addKillId(n);
		}
		for (int n : bQx.keySet()) {
		this.addKillId(n);
		}
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
		}
		String html = event;

        String string2 = event;
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "day_mina_q0633_0104.htm";
        }
        if (event.equalsIgnoreCase("633_4")) {
            qs.takeItems(bQv, -1L);
            qs.playSound(QuestState.SOUND_FINISH);
            string2 = "day_mina_q0633_0204.htm";
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("633_1")) {
            string2 = "day_mina_q0633_0201.htm";
        } else if (event.equalsIgnoreCase("633_3") && qs.getCond() == 2) {
            if (qs.getQuestItemsCount(bQv) >= 200L) {
                qs.takeItems(bQv, -1L);
                qs.giveItems(57, 25000L);
                qs.addExpAndSp(305235L, 0L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.setCond(1);
                string2 = "day_mina_q0633_0202.htm";
            } else {
                string2 = "day_mina_q0633_0203.htm";
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
        int n2 = qs.getCond();
        int n3 = qs.getStateId();
        if (n == bQu) {
            if (n3 == 1) {
                if (pc.getLevel() >= 65) {
                    html = "day_mina_q0633_0101.htm";
                } else {
                    html = "day_mina_q0633_0103.htm";
                    qs.exitQuest(true);
                }
            } else if (n2 == 1) {
                html = "day_mina_q0633_0106.htm";
            } else if (n2 == 2) {
                html = "day_mina_q0633_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        long l;
        int n = npc != null ? npc.getNpcId() : 0;
        if (bQy.containsKey(n)) {
            qs.rollAndGive(bQw, 1, bQy.get(n));
        } else if (bQx.containsKey(n) && (l = qs.getQuestItemsCount(bQv)) < 200L && (ThreadLocalRandom.current().nextDouble(100.0) < (bQx.get(n)))) {
            qs.giveItems(bQv, 1L);
            if (l >= 199L) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
