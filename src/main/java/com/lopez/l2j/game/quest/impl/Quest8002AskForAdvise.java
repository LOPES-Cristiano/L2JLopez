package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 8002_AskForAdvise: Orientacoes iniciais nos Newbie Guides por raça do personagem.
 */
@Component
public class Quest8002AskForAdvise extends Quest {

	public static final int[] NPCS = { 30598, 30599, 30600, 30601, 30602 };

	@Autowired
	public Quest8002AskForAdvise(@Autowired(required = false) QuestManager questManager) {
		super(8002, "8002_AskForAdvise", "custom");
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
		if (npc == null || qs == null) {
			return null;
		}
		PlayerCharacter player = qs.playerChar();
		Race race = player != null ? player.getRace() : Race.human;
		int npcId = npc.npcId();
		String htmltext;

		switch (npcId) {
			case 30598 -> htmltext = (race == Race.human) ? "30598.htm" : "30598-no.htm";
			case 30599 -> htmltext = (race == Race.elf) ? "30599.htm" : "30599-no.htm";
			case 30600 -> htmltext = (race == Race.darkelf) ? "30600.htm" : "30600-no.htm";
			case 30601 -> htmltext = (race == Race.dwarf) ? "30601.htm" : "30601-no.htm";
			case 30602 -> htmltext = (race == Race.orc) ? "30602.htm" : "30602-no.htm";
			default -> htmltext = npcId + "-no.htm";
		}

		qs.exitQuest(true);
		return htmltext;
	}
}
