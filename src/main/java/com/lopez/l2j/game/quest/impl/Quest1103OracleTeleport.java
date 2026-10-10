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
 * Quest 1103 - 1103_OracleTeleport
 */
@Component
public class Quest1103OracleTeleport extends Quest {

	public static final int aEw = 5901;
	public static final int[] aEx = new int[]{-80157, 111344, -4901};
	public static final int[] aEy = new int[]{-81261, 86531, -5157};

	public Quest1103OracleTeleport(QuestManager questManager) {
	super(1103, "1103_OracleTeleport", "1103_OracleTeleport");
		int n;
		for (n = 31078; n < 31092; ++n) {
		this.addStartNpc(n);
		}
		for (n = 31168; n <= 31170; ++n) {
		this.addStartNpc(n);
		}
		for (n = 31692; n <= 31696; ++n) {
		this.addStartNpc(n);
		}
		for (n = 31997; n <= 31999; ++n) {
		this.addStartNpc(n);
		}
		for (n = 31127; n <= 31142; ++n) {
		this.addStartNpc(n);
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
		return html;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        PlayerCharacter player = pc;
        switch (n) {
            case 31078: 
            case 31079: 
            case 31080: 
            case 31081: 
            case 31082: 
            case 31083: 
            case 31084: 
            case 31168: 
            case 31692: 
            case 31694: 
            case 31997: {
                qs.set("FestivalBackCoords", player.x() + "," + player.y() + "," + player.z());
                player.teleToLocation(aEx);
                break;
            }
            case 31085: 
            case 31086: 
            case 31087: 
            case 31088: 
            case 31089: 
            case 31090: 
            case 31091: 
            case 31169: 
            case 31693: 
            case 31695: 
            case 31998: {
                qs.set("FestivalBackCoords", player.x() + "," + player.y() + "," + player.z());
                player.teleToLocation(aEy);
                break;
            }
            default: {
                String html = qs.get("FestivalBackCoords");
                if (html == null) {
                    return "ssq_npc_priest_q507_03.htm";
                }
                if (qs.getQuestItemsCount(5901) > 0L) {
                    qs.takeItems(5901, qs.getQuestItemsCount(5901));
                }
                qs.unset("FestivalBackCoords");
                String[] stringArray = html.split(",");
                int n2 = Integer.parseInt(stringArray[0]);
                int n3 = Integer.parseInt(stringArray[1]);
                int n4 = Integer.parseInt(stringArray[2]);
                player.teleToLocation(n2, n3, n4);
            }
        }
        return null;
    
	}

}
