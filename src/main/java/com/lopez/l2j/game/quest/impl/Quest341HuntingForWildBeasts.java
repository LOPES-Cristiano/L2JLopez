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
 * Quest 341 - 341_HuntingForWildBeasts
 */
@Component
public class Quest341HuntingForWildBeasts extends Quest {

	public static final int blR = 30078;
	public static final int bna = 20021;
	public static final int bnb = 20203;
	public static final int bnc = 20310;
	public static final int bnd = 20335;
	public static final int bne = 4259;
	public static final int bnf = 40;

	public Quest341HuntingForWildBeasts(QuestManager questManager) {
	super(341, "341_HuntingForWildBeasts", "341_HuntingForWildBeasts");
		this.addStartNpc(blR);
		this.addKillId(bna);
		this.addKillId(bnb);
		this.addKillId(bnc);
		this.addKillId(bnd);
		this.addQuestItem(bne);
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
        if (event.equalsIgnoreCase("quest_accept") && qs.getState() == State.CREATED) {
            string2 = "pano_q0341_04.htm";
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != blR) {
            return html;
        }
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.getLevel() >= 20) {
                html = "pano_q0341_01.htm";
                qs.setCond(0);
            } else {
                html = "pano_q0341_02.htm";
                qs.exitQuest(true);
            }
        } else if (n == 2) {
            if (qs.getQuestItemsCount(bne) >= 20L) {
                html = "pano_q0341_05.htm";
                qs.takeItems(bne, -1L);
                qs.giveItems(57, 3710L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                html = "pano_q0341_06.htm";
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
        long l = qs.getQuestItemsCount(bne);
        if (l < 20L && (ThreadLocalRandom.current().nextDouble(100.0) < (bnf))) {
            qs.giveItems(bne, 1L);
            if (l == 19L) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
