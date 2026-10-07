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
 * Quest 348: An Arrogant Search (Acesso oficial ao Grand Boss Baium).
 * Concede a Blooded Fabric (4295) necessaria para despertar Baium no 14º andar da Tower of Insolence.
 */
@Component
public class Quest348AnArrogantSearch extends Quest {

	public static final int QUEST_ID = 348;
	public static final String QUEST_NAME = "348_AnArrogantSearch";

	// NPCs
	public static final int HANELLIN = 30864; // Magister Hanellin em Aden
	public static final int CLAUDIA_ATHEBALT = 31001;
	public static final int MARTIEN = 30645;

	// Monstros da ToI
	public static final int PLATINUM_TRIBE_SHAMAN = 20828;
	public static final int PLATINUM_TRIBE_OVERLORD = 20829;
	public static final int GUARDIAN_ANGEL = 20830;
	public static final int SEAL_ANGEL = 20831;

	// Itens
	public static final int TITANS_POWERSTONE = 4287;
	public static final int HANELLIN_FIRST_LETTER = 4288;
	public static final int WHITE_FABRIC = 4294;
	public static final int BLOODED_FABRIC = 4295;

	@Autowired
	public Quest348AnArrogantSearch(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "An Arrogant Search");

		addStartNpc(HANELLIN);
		addTalkId(HANELLIN);
		addTalkId(CLAUDIA_ATHEBALT);
		addTalkId(MARTIEN);

		addKillId(PLATINUM_TRIBE_SHAMAN);
		addKillId(PLATINUM_TRIBE_OVERLORD);
		addKillId(GUARDIAN_ANGEL);
		addKillId(SEAL_ANGEL);

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
			return "hanellin_accepted.htm";
		}

		if ("deliver_stone".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(TITANS_POWERSTONE, -1);
			qs.giveItems(WHITE_FABRIC, 1);
			qs.setCond(3);
			return "hanellin_give_fabrics.htm";
		}

		return null;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) return "hanellin_noquest.htm";

		PlayerCharacter character = player.activeChar();
		if (character == null) return null;

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == HANELLIN) {
			if (qs.isCreated()) {
				if (character.level() >= 60) {
					return "hanellin_intro.htm";
				} else {
					return "hanellin_low_level.htm";
				}
			}
			if (cond == 1) {
				return "hanellin_need_stone.htm";
			}
			if (cond == 2) {
				return "hanellin_stone_delivered.htm";
			}
			if (cond == 3) {
				if (qs.hasQuestItems(BLOODED_FABRIC)) {
					return "hanellin_fabric_completed.htm";
				}
				return "hanellin_soak_fabric.htm";
			}
		}

		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null || !qs.isStarted()) return null;

		int cond = qs.getCond();
		int npcId = npc.npcId();

		if (cond == 1 && (npcId == PLATINUM_TRIBE_SHAMAN || npcId == PLATINUM_TRIBE_OVERLORD)) {
			if (!qs.hasQuestItems(TITANS_POWERSTONE)) {
				qs.giveItems(TITANS_POWERSTONE, 1);
				qs.setCond(2);
			}
		} else if (cond == 3 && (npcId == GUARDIAN_ANGEL || npcId == SEAL_ANGEL)) {
			if (qs.hasQuestItems(WHITE_FABRIC)) {
				qs.takeItems(WHITE_FABRIC, 1);
				qs.giveItems(BLOODED_FABRIC, 1);
				qs.exitQuest(false); // Conclui com concessao do tecido ensanguentado
			}
		}

		return null;
	}
}
