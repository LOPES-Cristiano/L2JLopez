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
 * Quest 364 - 364_JovialAccordion
 */
@Component
public class Quest364JovialAccordion extends Quest {

	public static final int BARBADO = 30959;
	public static final int brX = 30957;
	public static final int bsd = 30060;
	public static final int bse = 30960;
	public static final int bsf = 30961;
	public static final int bsg = 4323;
	public static final int bsh = 4324;
	public static final int bsi = 4321;
	public static final int bsj = 4421;

	public Quest364JovialAccordion(QuestManager questManager) {
	super(364, "364_JovialAccordion", "364_JovialAccordion");
		this.addStartNpc(BARBADO);
		this.addTalkId(brX);
		this.addTalkId(bsd);
		this.addTalkId(bse);
		this.addTalkId(bsf);
		this.addQuestItem(bsg);
		this.addQuestItem(bsh);
		this.addQuestItem(bsi);
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
        int n = qs.getStateId();
        int n2 = qs.getCond();
        if (event.equalsIgnoreCase("30959-02.htm") && n == 1 && n2 == 0) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30957-02.htm") && n == 2 && n2 == 1) {
            qs.setCond(2);
            qs.giveItems(bsg, 1L);
            qs.giveItems(bsh, 1L);
        } else if (event.equalsIgnoreCase("30960-03.htm") && n2 == 2 && qs.getQuestItemsCount(bsh) > 0L) {
            qs.takeItems(bsh, -1L);
            qs.giveItems(bsi, 1L);
            string2 = "30960-02.htm";
        } else if (event.equalsIgnoreCase("30961-03.htm") && n2 == 2 && qs.getQuestItemsCount(bsg) > 0L) {
            qs.takeItems(bsg, -1L);
            string2 = "30961-02.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getState() == State.CREATED) {
            if (n != BARBADO) {
                return html;
            }
            qs.setCond(0);
            qs.set("ok", "0");
        }
        int n2 = qs.getCond();
        if (n == BARBADO) {
            if (n2 == 0) {
                html = "30959-01.htm";
            } else if (n2 == 3) {
                html = "30959-03.htm";
                qs.giveItems(bsj, 1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else if (n2 > 0) {
                html = "30959-02.htm";
            }
        } else if (n == brX) {
            if (n2 == 1) {
                html = "30957-01.htm";
            } else if (n2 == 3) {
                html = "30957-05.htm";
            } else if (n2 == 2) {
                if (qs.getInt("ok") == 1 && qs.getQuestItemsCount(bsg) == 0L) {
                    qs.setCond(3);
                    html = "30957-04.htm";
                } else {
                    html = "30957-03.htm";
                }
            }
        } else if (n == bsd && n2 == 2 && qs.getQuestItemsCount(bsi) > 0L) {
            qs.set("ok", "1");
            qs.takeItems(bsi, -1L);
            html = "30060-01.htm";
        } else if (n == bse && n2 == 2) {
            html = "30960-01.htm";
        } else if (n == bsf && n2 == 2) {
            html = "30961-01.htm";
        }
        return html;
    
	}

}
