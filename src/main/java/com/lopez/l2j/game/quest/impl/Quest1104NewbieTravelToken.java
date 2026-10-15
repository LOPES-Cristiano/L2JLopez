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
		if (qs.getPlayer() != null && qs.playerLevel() >= 20) {
			qs.exitQuest(true);
			return "<html><body>Newbie Guide:<br>Newbie Guides offer teleportation among Newbie Towns to any character below level 20.<br>If you are above level 20, please use the official gatekeepers.</body></html>";
		}
		int npcId = npc != null ? npc.npcId() : 30598;
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>Newbie Guide:<br>Where would you like to go? Teleportation between starter villages requires 1 <font color=\"LEVEL\">Newbie Travel Token</font>.<br><br>");
		if (npcId != 30598) {
			sb.append("<a action=\"bypass -h Quest 1104_NewbieTravelToken 30598\">Talking Island Village</a><br>");
		}
		if (npcId != 30599) {
			sb.append("<a action=\"bypass -h Quest 1104_NewbieTravelToken 30599\">Elven Village</a><br>");
		}
		if (npcId != 30600) {
			sb.append("<a action=\"bypass -h Quest 1104_NewbieTravelToken 30600\">Dark Elven Village</a><br>");
		}
		if (npcId != 30602) {
			sb.append("<a action=\"bypass -h Quest 1104_NewbieTravelToken 30602\">Orc Village</a><br>");
		}
		if (npcId != 30601) {
			sb.append("<a action=\"bypass -h Quest 1104_NewbieTravelToken 30601\">Dwarven Village</a><br>");
		}
		sb.append("</body></html>");
		return sb.toString();
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
						return "<html><body>Newbie Guide:<br>You do not have any <font color=\"LEVEL\">Newbie Travel Token</font>.</body></html>";
					}
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
