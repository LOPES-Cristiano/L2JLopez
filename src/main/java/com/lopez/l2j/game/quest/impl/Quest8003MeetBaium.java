package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 8003_MeetBaium: NPC Angelic Vortex (31862) verificando se Baium esta acessivel
 * e consumindo Blooded Fabric (4295) para teleportar o jogador ao 14o andar da Torre da Insolencia.
 */
@Component
public class Quest8003MeetBaium extends Quest {

	public static final int ANGELIC_VORTEX = 31862;
	public static final int BLOODED_FABRIC = 4295;

	private final BaiumService baiumService;

	@Autowired
	public Quest8003MeetBaium(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) BaiumService baiumService) {
		super(8003, "8003_MeetBaium", "custom");
		this.baiumService = baiumService;
		addStartNpc(ANGELIC_VORTEX);
		addTalkId(ANGELIC_VORTEX);
		addFirstTalkId(ANGELIC_VORTEX);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "31862-no.htm";
		}
		PlayerCharacter player = qs.playerChar();
		boolean canEnter = (baiumService == null || baiumService.canEnter(player)) && qs.getQuestItemsCount(BLOODED_FABRIC) > 0;
		return canEnter ? "31862.htm" : "31862-no.htm";
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return "31862-2.htm";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null) {
			return null;
		}
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}

		if ("31862-3.htm".equalsIgnoreCase(event)) {
			qs.exitQuest(true);
			return "31862-3.htm";
		}

		if ("baium".equalsIgnoreCase(event) || "enter".equalsIgnoreCase(event) || "enter_baium".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BLOODED_FABRIC) > 0) {
				qs.takeItems(BLOODED_FABRIC, 1);
				player.teleportToLocation(BaiumService.FLOOR14_X, BaiumService.FLOOR14_Y, BaiumService.FLOOR14_Z);
				qs.exitQuest(true);
				return null;
			} else {
				qs.exitQuest(true);
				return "31862-no.htm";
			}
		}
		return event;
	}
}
