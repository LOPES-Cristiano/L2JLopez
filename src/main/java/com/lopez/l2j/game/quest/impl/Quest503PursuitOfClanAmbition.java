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
 * Quest 503: Pursuit of Clan Ambition (Elevacao de Clã para Nivel 5).
 * Exige lider de cla de nivel 4 com pelo menos nivel 55+.
 * Concede o Scepter of Judgement (3838) necessario para ascender a forca militar do cla.
 */
@Component
public class Quest503PursuitOfClanAmbition extends Quest {

	public static final int QUEST_ID = 503;
	public static final String QUEST_NAME = "503_PursuitOfClanAmbition";

	// NPCs
	public static final int GUSTAF = 30758; // Sir Gustaf Athebalt em Oren
	public static final int MARTIEN = 30645;
	public static final int BALTHAZAR = 30868;
	public static final int RODEMAI = 30867;

	// Monstros
	public static final int DRAKE = 20137;
	public static final int GIANT_SOLDIER = 20654;

	// Itens
	public static final int GUSTAF_INSTRUCTION = 3834;
	public static final int SCEPTER_OF_JUDGEMENT = 3838;

	@Autowired
	public Quest503PursuitOfClanAmbition(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Pursuit of Clan Ambition");

		addStartNpc(GUSTAF);
		addTalkId(GUSTAF);
		addTalkId(MARTIEN);
		addTalkId(BALTHAZAR);
		addTalkId(RODEMAI);

		addKillId(DRAKE);
		addKillId(GIANT_SOLDIER);

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
			qs.giveItems(GUSTAF_INSTRUCTION, 1);
			return "gustaf_started.htm";
		}

		if ("gustaf_finish".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(GUSTAF_INSTRUCTION, -1);
			qs.giveItems(SCEPTER_OF_JUDGEMENT, 1);
			qs.exitQuest(false);
			return "gustaf_concluded.htm";
		}

		return null;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) return "gustaf_noquest.htm";

		PlayerCharacter character = player.activeChar();
		if (character == null) return null;

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == GUSTAF) {
			if (qs.isCreated()) {
				if (character.isClanLeader() && character.level() >= 55) {
					return "gustaf_intro.htm";
				} else {
					return "gustaf_not_eligible.htm";
				}
			}
			if (cond == 1) {
				return "gustaf_pending.htm";
			}
			if (cond == 2) {
				return "gustaf_ready_finish.htm";
			}
			if (qs.isCompleted()) {
				return "gustaf_done.htm";
			}
		} else if (npcId == MARTIEN) {
			if (cond == 1) {
				qs.setCond(2);
				return "martien_alliance_confirmed.htm";
			}
		}

		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null || !qs.isStarted()) return null;
		return null;
	}
}
