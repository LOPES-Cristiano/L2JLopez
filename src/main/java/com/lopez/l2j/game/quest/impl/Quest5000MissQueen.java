package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 5000_MissQueen: NPC Adventurers' Guide Miss Queen (31760..31766) entregando cupons
 * para iniciantes e viajantes e persistindo estado em character_variables.
 */
@Component
public class Quest5000MissQueen extends Quest {

	public static final int COUPON_ONE = 7832;
	public static final int COUPON_TWO = 7833;

	public static final int NEWBIE_REWARD = 16;
	public static final int TRAVELER_REWARD = 32;

	public static final int[] NPCS = { 31760, 31761, 31762, 31763, 31764, 31765, 31766 };

	private final CharacterVariablesService characterVariables;

	public Quest5000MissQueen() {
		this(null, null);
	}

	public Quest5000MissQueen(QuestManager questManager) {
		this(questManager, null);
	}

	@Autowired
	public Quest5000MissQueen(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) CharacterVariablesService characterVariables) {
		super(5000, "5000_MissQueen", "custom");
		this.characterVariables = characterVariables;
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
		return "31760.htm";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null || player.activeChar() == null) {
			return null;
		}
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		PlayerCharacter active = player.activeChar();
		int level = active.level();
		int pkkills = active.pkKills();
		int occupationLevel = active.classLevel();
		int newbie = active.getNewbie();

		if ("newbie_give_coupon".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 25 && pkkills == 0 && occupationLevel == 0) {
				boolean alreadyGiven = (newbie & NEWBIE_REWARD) == NEWBIE_REWARD
						|| (characterVariables != null && characterVariables.has(active.objectId(), "miss_queen_newbie"));
				if (!alreadyGiven) {
					active.setNewbie(newbie | NEWBIE_REWARD);
					if (characterVariables != null) {
						characterVariables.set(active.objectId(), "miss_queen_newbie", "1");
					}
					qs.giveItems(COUPON_ONE, 1);
					return "31760-2.htm";
				} else {
					return "31760-1.htm";
				}
			} else {
				return "31760-3.htm";
			}
		} else if ("traveller_give_coupon".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 25 && pkkills == 0 && occupationLevel == 1) {
				boolean alreadyGiven = (newbie & TRAVELER_REWARD) == TRAVELER_REWARD
						|| (characterVariables != null && characterVariables.has(active.objectId(), "miss_queen_traveler"));
				if (!alreadyGiven) {
					active.setNewbie(newbie | TRAVELER_REWARD);
					if (characterVariables != null) {
						characterVariables.set(active.objectId(), "miss_queen_traveler", "1");
					}
					qs.giveItems(COUPON_TWO, 1);
					return "31760-5.htm";
				} else {
					return "31760-4.htm";
				}
			} else {
				return "31760-6.htm";
			}
		}
		return null;
	}
}
