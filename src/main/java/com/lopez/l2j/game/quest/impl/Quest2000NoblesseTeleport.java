package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 2000_NoblesseTeleport: Teleporte exclusivo para Noblesses nos Gatekeepers oficiais.
 */
@Component
public class Quest2000NoblesseTeleport extends Quest {

	public static final int[] NPCS = {
			30006, 30059, 30080, 30134, 30146, 30177, 30233, 30256, 30320,
			30540, 30576, 30836, 30848, 30878, 30899, 31275, 31320, 31964
	};

	@Autowired
	public Quest2000NoblesseTeleport(@Autowired(required = false) QuestManager questManager) {
		super(2000, "2000_NoblesseTeleport", "Teleports");
		for (int npcId : NPCS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter player = qs != null ? qs.playerChar() : null;
		String htmltext = (player != null && player.isNoble()) ? "noble.htm" : "nobleteleporter-no.htm";
		if (qs != null) {
			qs.exitQuest(true);
		}
		return htmltext;
	}
}
