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
 * Quest 381 - 381_LetsBecomeARoyalMember
 */
@Component
public class Quest381LetsBecomeARoyalMember extends Quest {

	public static final int bxF = 5899;
	public static final int bxG = 5900;
	public static final int ble = 3813;
	public static final int bxH = 7569;
	public static final int bxI = 5898;
	public static final int blJ = 30232;
	public static final int bxJ = 30090;
	public static final int bxK = 21018;
	public static final int bxL = 27316;
	public static final int bxM = 5;
	public static final int bxN = 100;

	public Quest381LetsBecomeARoyalMember(QuestManager questManager) {
	super(381, "381_LetsBecomeARoyalMember", "381_LetsBecomeARoyalMember");
		this.addStartNpc(blJ);
		this.addTalkId(bxJ);
		this.addKillId(bxK);
		this.addKillId(bxL);
		this.addQuestItem(bxF);
		this.addQuestItem(bxG);
		this.addQuestItem(bxH);
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
        if (event.equalsIgnoreCase("warehouse_keeper_sorint_q0381_02.htm")) {
            if (pc.getLevel() >= 55 && qs.getQuestItemsCount(ble) > 0L) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "warehouse_keeper_sorint_q0381_03.htm";
            } else {
                string2 = "warehouse_keeper_sorint_q0381_02.htm";
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("sandra_q0381_02.htm") && qs.getCond() == 1) {
            qs.set("id", "1");
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        int n2 = npc != null ? npc.getNpcId() : 0;
        long l = qs.getQuestItemsCount(bxG);
        if (n2 == blJ) {
            if (n == 0) {
                html = "warehouse_keeper_sorint_q0381_01.htm";
            } else if (n == 1) {
                long l2 = qs.getQuestItemsCount(bxF);
                if (l2 > 0L && l > 0L) {
                    qs.takeItems(bxF, -1L);
                    qs.takeItems(bxG, -1L);
                    qs.giveItems(bxI, 1L);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    html = "warehouse_keeper_sorint_q0381_06.htm";
                } else if (l == 0L) {
                    html = "warehouse_keeper_sorint_q0381_05.htm";
                } else if (l2 == 0L) {
                    html = "warehouse_keeper_sorint_q0381_04.htm";
                }
            }
        } else {
            long l3 = qs.getQuestItemsCount(bxH);
            if (l > 0L) {
                html = "sandra_q0381_05.htm";
            } else if (l3 > 0L) {
                qs.takeItems(bxH, -1L);
                qs.giveItems(bxG, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
                html = "sandra_q0381_04.htm";
            } else {
                html = qs.getInt("id") == 0 ? "sandra_q0381_01.htm" : "sandra_q0381_03.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        long l = qs.getQuestItemsCount(bxG);
        long l2 = qs.getQuestItemsCount(bxF);
        long l3 = qs.getQuestItemsCount(bxH);
        if (n == bxK && l2 == 0L) {
            if ((ThreadLocalRandom.current().nextDouble(100.0) < (bxM))) {
                qs.giveItems(bxF, 1L);
                if (l > 0L || l3 > 0L) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n == bxL && l3 + l == 0L && qs.getInt("id") != 0 && (ThreadLocalRandom.current().nextDouble(100.0) < (bxN))) {
            qs.giveItems(bxH, 1L);
            if (l2 > 0L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
