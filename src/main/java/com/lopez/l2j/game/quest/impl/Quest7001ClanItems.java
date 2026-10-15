package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 7001_ClanItems: Mercador de itens de cla e armaduras Apella (NPCs 32024, 32025).
 */
@Component
public class Quest7001ClanItems extends Quest {

	public static final int[] NPCS = { 32024, 32025 };

	@Autowired
	public Quest7001ClanItems(@Autowired(required = false) QuestManager questManager) {
		super(7001, "7001_ClanItems", "custom");
		for (int npcId : NPCS) {
			addStartNpc(npcId);
			addFirstTalkId(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		if (npc == null || qs == null) {
			return null;
		}
		PlayerCharacter player = qs.playerChar();
		int npcId = npc.npcId();
		return (player != null && player.isClanLeader()) ? npcId + ".htm" : npcId + "-no.htm";
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return onFirstTalk(npc, qs);
	}
}
