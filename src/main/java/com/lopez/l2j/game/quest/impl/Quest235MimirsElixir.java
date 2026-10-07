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
 * Quest 235: Mimir's Elixir (2º Passo Obrigatório para Subclasse no Lineage II Interlude).
 * Concede a lendária Mimir's Elixir (6319) e desbloqueia a adição de Subclasses nos Grand Masters.
 */
@Component
public class Quest235MimirsElixir extends Quest {

	public static final int QUEST_ID = 235;
	public static final String QUEST_NAME = "235_MimirsElixir";

	// NPCs
	public static final int LADD = 30721;
	public static final int JOAN = 30718;

	// Monstros
	public static final int CHIMERA_PIECE = 20965;
	public static final int BLOODY_GUARDIAN = 21090;

	// Itens
	public static final int STAR_OF_DESTINY = 5011;
	public static final int PURE_SILVER = 6320;
	public static final int TRUE_GOLD = 6321;
	public static final int SAGES_STONE = 6322;
	public static final int BLOOD_FIRE = 6318;
	public static final int MIMIRS_ELIXIR = 6319;
	public static final int ENCHANT_WEAPON_A = 729;

	@Autowired
	public Quest235MimirsElixir(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Mimir's Elixir");

		addStartNpc(LADD);
		addTalkId(LADD);
		addTalkId(JOAN);

		addKillId(CHIMERA_PIECE);
		addKillId(BLOODY_GUARDIAN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30721-02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == LADD) {
			if (cond == 0) {
				if (c.level() >= 75 && qs.getQuestItemsCount(STAR_OF_DESTINY) > 0) {
					return "30721-01.htm";
				} else {
					return "30721-01a.htm";
				}
			} else if (cond == 8) {
				// Síntese do elixir finalizada
				qs.takeItems(STAR_OF_DESTINY, -1);
				qs.takeItems(PURE_SILVER, -1);
				qs.takeItems(TRUE_GOLD, -1);
				qs.takeItems(BLOOD_FIRE, -1);
				qs.giveItems(MIMIRS_ELIXIR, 1);
				qs.giveItems(ENCHANT_WEAPON_A, 1);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound(QuestState.SOUND_FINISH);
				return "30721-07.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == CHIMERA_PIECE && cond == 3) {
			if (qs.getQuestItemsCount(SAGES_STONE) == 0) {
				qs.giveItems(SAGES_STONE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				qs.setCond(4);
			}
		} else if (npcId == BLOODY_GUARDIAN && cond == 6) {
			if (qs.getQuestItemsCount(BLOOD_FIRE) == 0) {
				qs.giveItems(BLOOD_FIRE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				qs.setCond(7);
			}
		}
		return null;
	}
}
