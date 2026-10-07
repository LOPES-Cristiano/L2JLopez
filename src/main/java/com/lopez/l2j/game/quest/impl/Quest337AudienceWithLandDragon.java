package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 337: Audience with the Land Dragon (Acesso oficial ao Grand Boss Antharas).
 * Concede a lendaria Portal Stone (3865) necessaria para transpor o Heart of Warding no Covil de Antharas.
 */
@Component
public class Quest337AudienceWithLandDragon extends Quest {

	public static final int QUEST_ID = 337;
	public static final String QUEST_NAME = "337_AudienceWithLandDragon";

	// NPCs
	public static final int GABRIELLE = 30753; // Giran
	public static final int GILMORE = 30754;   // Dragon Valley Entrance
	public static final int THEODRIC = 30755;  // Antharas Lair Gatekeeper

	// Monstros
	public static final int CAVE_KEEPER = 20277;
	public static final int CAVE_MAIDEN = 20287;
	public static final int MALRUK_KNIGHT = 20245;

	// Itens
	public static final int FEATHER_OF_SEEKER = 3852;
	public static final int KRAKIAN_TOOTH = 3853;
	public static final int PORTAL_STONE = 3865;

	@Autowired
	public Quest337AudienceWithLandDragon(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Audience with the Land Dragon");

		addStartNpc(GABRIELLE);
		addTalkId(GABRIELLE);
		addTalkId(GILMORE);
		addTalkId(THEODRIC);

		addKillId(CAVE_KEEPER);
		addKillId(CAVE_MAIDEN);
		addKillId(MALRUK_KNIGHT);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) return null;

		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(FEATHER_OF_SEEKER, 1);
			return "gabrielle_started.htm";
		}

		if ("gabrielle_finish".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(FEATHER_OF_SEEKER, -1);
			qs.takeItems(KRAKIAN_TOOTH, -1);
			qs.giveItems(PORTAL_STONE, 1);
			qs.exitQuest(false);
			return "gabrielle_completed.htm";
		}

		return null;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) return "gabrielle_noquest.htm";

		PlayerCharacter character = player.activeChar();
		if (character == null) return null;

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == GABRIELLE) {
			if (qs.isCreated()) {
				if (character.level() >= 50) {
					return "gabrielle_intro.htm";
				} else {
					return "gabrielle_low_level.htm";
				}
			}
			if (cond == 1) {
				return "gabrielle_hunting.htm";
			}
			if (cond == 2) {
				return "gabrielle_ready_to_finish.htm";
			}
			if (qs.isCompleted()) {
				return "gabrielle_already_done.htm";
			}
		} else if (npcId == GILMORE) {
			if (cond == 1) {
				return "gilmore_clues.htm";
			}
		} else if (npcId == THEODRIC) {
			if (qs.isCompleted() || qs.hasQuestItems(PORTAL_STONE)) {
				return "theodric_enter.htm";
			}
			return "theodric_no_stone.htm";
		}

		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null || !qs.isStarted()) return null;

		if (qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(KRAKIAN_TOOTH);
			if (count < 10) {
				qs.giveItems(KRAKIAN_TOOTH, 1);
				if (qs.getQuestItemsCount(KRAKIAN_TOOTH) >= 10) {
					qs.setCond(2);
				}
			}
		}

		return null;
	}
}
