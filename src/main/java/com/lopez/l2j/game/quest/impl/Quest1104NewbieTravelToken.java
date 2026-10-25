package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 1104_NewbieTravelToken: Teleporte gratuito para personagens de nivel < 20
 * consumindo Newbie Travel Token (ID 8542) entre as vilas iniciais.
 */
@Component
public class Quest1104NewbieTravelToken extends Quest {

	public static final int TOKEN = 8542;

	public static final Map<Integer, int[]> DESTINATIONS = Map.of(
			30600, new int[] { 12111, 16686, -4584 },     // Dark Elf Village
			30601, new int[] { 115632, -177996, -896 },   // Dwarven Village
			30599, new int[] { 45475, 48359, -3056 },     // Elven Village
			30602, new int[] { -45032, -113598, -192 },   // Orc Village
			30598, new int[] { -84081, 243227, -3728 }    // Talking Island
	);

	@Autowired
	public Quest1104NewbieTravelToken(@Autowired(required = false) QuestManager questManager) {
		super(1104, "1104_NewbieTravelToken", "Teleports");
		for (int npcId : DESTINATIONS.keySet()) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs.playerLevel() >= 20) {
			qs.exitQuest(true);
			return "1.htm";
		}
		int npcId = npc != null ? npc.npcId() : 30598;
		return npcId + ".htm";
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
		if (event.chars().allMatch(Character::isDigit)) {
			try {
				int dest = Integer.parseInt(event);
				int[] loc = DESTINATIONS.get(dest);
				if (loc != null) {
					if (qs.getQuestItemsCount(TOKEN) > 0) {
						qs.takeItems(TOKEN, 1);
						player.teleportToLocation(loc[0], loc[1], loc[2]);
						qs.exitQuest(true);
						return null;
					} else {
						qs.exitQuest(true);
						return "Incorrect item count";
					}
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
