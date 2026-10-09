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
 * Quest 504 - 504_CompetitionForTheBanditStronghold
 */
@Component
public class Quest504CompetitionForTheBanditStronghold extends Quest {

	public static final int bIP = 35437;
	public static final int bIQ = 20570;
	public static final int bIR = 20571;
	public static final int bIS = 20572;
	public static final int bIT = 20573;
	public static final int bIU = 20574;
	public static final int bIV = 4332;
	public static final int bIW = 5009;
	public static final int bIX = 4333;

	public Quest504CompetitionForTheBanditStronghold(QuestManager questManager) {
	super(504, "504_CompetitionForTheBanditStronghold", "504_CompetitionForTheBanditStronghold");
		this.addStartNpc(35437);
		this.addTalkId(35437);
		this.addKillId(20570);
		this.addKillId(20571);
		this.addKillId(20572);
		this.addKillId(20573);
		this.addKillId(20574);
		this.addQuestItem(4333, 4332, 5009);
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
        if (event.equalsIgnoreCase("azit_messenger_q0504_02.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.giveItems(4333, 1L);
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
		PlayerCharacter player = pc;
		Clan clan = player.getClan();
		if (clan == null || player.getObjectId() != clan.getLeaderId()) {
			html = "azit_messenger_q0504_05.htm";
		} else if (player.getObjectId() == clan.getLeaderId() && clan.getLevel() < 4) {
			html = "azit_messenger_q0504_04.htm";
		} else if (clan.getHasHideout() > 0) {
			html = "azit_messenger_q0504_10.htm";
		} else if (n == 0) {
			html = "azit_messenger_q0504_01.htm";
		} else if (qs.getQuestItemsCount(4333) == 1L && qs.getQuestItemsCount(4332) < 30L) {
			html = "azit_messenger_q0504_07.htm";
		} else if (qs.getQuestItemsCount(5009) >= 1L) {
			html = "azit_messenger_q0504_07a.htm";
		} else if (qs.getQuestItemsCount(4333) == 1L && qs.getQuestItemsCount(4332) == 30L) {
			qs.takeItems(4332, -1L);
			qs.takeItems(4333, -1L);
			qs.giveItems(5009, 1L);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.setCond(-1);
			html = "azit_messenger_q0504_08.htm";
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount(4332) < 30L) {
            qs.giveItems(4332, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
