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
 * Quest 613 - 613_ProveYourCourage
 */
@Component
public class Quest613ProveYourCourage extends Quest {

	public static final int bLP = 31377;
	public static final int bKI = 25299;
	public static final int Uj = 7223;
	public static final int bLS = 7240;
	public static final int bLH = 7229;

	public Quest613ProveYourCourage(QuestManager questManager) {
	super(613, "613_ProveYourCourage", "613_ProveYourCourage");
		this.addStartNpc(31377);
		this.addKillId(25299);
		this.addQuestItem(7240);
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
        if (event.equals("quest_accept")) {
            qs.setCond(1);
            qs.set("prove_your_courage_varka", String.valueOf(11), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "elder_ashas_barka_durai_q0613_0104.htm";
        } else if (event.equals("reply_3")) {
            if (qs.getQuestItemsCount(7240) >= 1L) {
                qs.takeItems(7240, -1L);
                qs.giveItems(7229, 1L);
                qs.addExpAndSp(10000L, 0L);
                qs.unset("prove_your_courage_varka");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "elder_ashas_barka_durai_q0613_0201.htm";
            } else {
                string2 = "elder_ashas_barka_durai_q0613_0202.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("prove_your_courage_varka");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31377) break;
                if (pc.getLevel() >= 75) {
                    if (qs.getQuestItemsCount(7223) >= 1L) {
                        html = "elder_ashas_barka_durai_q0613_0101.htm";
                        break;
                    }
                    qs.exitQuest(true);
                    html = "elder_ashas_barka_durai_q0613_0102.htm";
                    break;
                }
                qs.exitQuest(true);
                html = "elder_ashas_barka_durai_q0613_0103.htm";
                break;
            }
            case 2: {
                if (n2 != 31377 || n < 11 || n > 12) break;
                html = n == 12 && qs.getQuestItemsCount(7240) >= 1L ? "elder_ashas_barka_durai_q0613_0105.htm" : "elder_ashas_barka_durai_q0613_0106.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        int n2 = qs.getInt("prove_your_courage_varka");
        int n3 = npc.getNpcId();
        if (n2 == 11 && n3 == 25299 && (n = ThreadLocalRandom.current().nextInt(1000)) < 1000) {
            if (qs.getQuestItemsCount(7240) + 1L >= 1L) {
                if (qs.getQuestItemsCount(7240) < 1L) {
                    qs.setCond(2);
                    qs.set("prove_your_courage_varka", String.valueOf(12), true);
                    qs.giveItems(7240, 1L);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            } else {
                qs.giveItems(7240, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
