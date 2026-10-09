package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.Clan;
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
 * Quest 655 - 655_AGrandPlanForTamingWildBeasts
 */
@Component
public class Quest655AGrandPlanForTamingWildBeasts extends Quest {

	public static final int bIP = 35627;
	public static final int bqF = 8084;
	public static final int bUi = 8293;

	public Quest655AGrandPlanForTamingWildBeasts(QuestManager questManager) {
	super(655, "655_AGrandPlanForTamingWildBeasts", "655_AGrandPlanForTamingWildBeasts");
		this.addStartNpc(35627);
		this.addTalkId(35627);
		this.addQuestItem(8084, 8293);
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
        if (event.equalsIgnoreCase("farm_messenger_q0655_06.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
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
        ClanHall clanHall = (ClanHall)ResidenceHolder.getInstance().getResidence(63);
        if (((SiegeEvent)clanHall.getSiegeEvent()).isRegistrationOver()) {
            html = null;
            this.showHtmlFile(player, "farm_messenger_q0655_02.htm", false, "%siege_time%", TimeUtils.toSimpleFormat(clanHall.getSiegeDate()));
        } else if (clan == null || player.getObjectId() != clan.getLeaderId()) {
            html = "farm_messenger_q0655_03.htm";
        } else if (player.getObjectId() == clan.getLeaderId() && clan.getLevel() < 4) {
            html = "farm_messenger_q0655_05.htm";
        } else if (((SiegeEvent)clanHall.getSiegeEvent()).getSiegeClan("attackers", player.getClan()) != null) {
            html = "farm_messenger_q0655_07.htm";
        } else if (clan.getHasHideout() > 0) {
            html = "farm_messenger_q0655_04.htm";
        } else if (n == 0) {
            html = "farm_messenger_q0655_01.htm";
        } else if (n == 1 && qs.getQuestItemsCount(8084) < 10L) {
            html = "farm_messenger_q0655_08.htm";
        } else if (n == 1 && qs.getQuestItemsCount(8084) == 10L) {
            qs.setCond(-1);
            qs.takeItems(8084, -1L);
            qs.giveItems(8293, 1L);
            html = "farm_messenger_q0655_10.htm";
        } else if (qs.getQuestItemsCount(8293) == 1L) {
            html = "farm_messenger_q0655_09.htm";
        }
        return html;
    
	}

}
