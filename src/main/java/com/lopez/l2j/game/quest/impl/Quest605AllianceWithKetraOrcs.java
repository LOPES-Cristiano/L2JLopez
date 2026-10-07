package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Quest 605: Alliance with Ketra Orcs
 *
 * Progressão épica de Aliança em 5 Estágios com os Ketra Orcs em Goddard.
 * Hierarch Wahkan (31371) troca insígnias de Varka Silenos por marcas de aliança (7211–7215).
 */
@Component
public class Quest605AllianceWithKetraOrcs extends Quest {

	public static final int QUEST_ID = 605;
	public static final String QUEST_NAME = "605_AllianceWithKetraOrcs";

	public static final int WAHKAN = 31371;

	// Marcas de Aliança Ketra (Estágios 1 a 5)
	public static final int MARK_KETRA_1 = 7211;
	public static final int MARK_KETRA_2 = 7212;
	public static final int MARK_KETRA_3 = 7213;
	public static final int MARK_KETRA_4 = 7214;
	public static final int MARK_KETRA_5 = 7215;

	// Insígnias Varka
	public static final int VARKA_BADGE_SOLDIER = 7216;
	public static final int VARKA_BADGE_CAPTAIN = 7217;
	public static final int VARKA_BADGE_GENERAL = 7218;

	// Totens especiais
	public static final int TOTEM_OF_VALOR = 7219;
	public static final int TOTEM_OF_WISDOM = 7220;

	// Inimigos Silenos Nível 1
	private static final Set<Integer> VARKA_SOLDIERS = Set.of(21350, 21351, 21353, 21354, 21355);
	// Inimigos Silenos Nível 2
	private static final Set<Integer> VARKA_OFFICERS = Set.of(21357, 21358, 21360, 21361, 21362, 21369, 21370);
	// Inimigos Silenos Nível 3+
	private static final Set<Integer> VARKA_CAPTAINS = Set.of(21364, 21365, 21366, 21368, 21371, 21372, 21373, 21374, 21375);

	public Quest605AllianceWithKetraOrcs() {
		super(QUEST_ID, QUEST_NAME, "Alliance with Ketra Orcs");
		addStartNpc(WAHKAN);
		addTalkId(WAHKAN);
		for (int mobId : VARKA_SOLDIERS) {
			addKillId(mobId);
		}
		for (int mobId : VARKA_OFFICERS) {
			addKillId(mobId);
		}
		for (int mobId : VARKA_CAPTAINS) {
			addKillId(mobId);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState st = checkQuestState(player);
		if (st == null) {
			st = newQuestState(player);
		}
		if (st == null) {
			return null;
		}

		if ("31371-04.htm".equalsIgnoreCase(event)) {
			st.setState(State.STARTED);
			st.set("cond", 1);
			st.playSound(QuestState.SOUND_ACCEPT);
			return "31371-04.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState st = player.getQuestState(QUEST_NAME);
		if (st == null) {
			st = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return "noquest";
		}

		if (st.isCreated()) {
			if (c.level() < 74) {
				return "31371-03.htm"; // Nível insuficiente
			}
			return "31371-01.htm"; // Início
		}

		if (st.isStarted()) {
			int cond = st.getInt("cond");

			// Estágio 1 -> Requer 100 Varka Soldier Badges
			if (cond == 1) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 100);
					st.giveItems(MARK_KETRA_1, 1);
					st.set("cond", 2);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-05.htm"; // Promovido para Estágio 1!
				}
				return "31371-04a.htm"; // Em busca das 100 insígnias
			}

			// Estágio 2 -> Requer 200 Soldier + 100 Officer Badges
			if (cond == 2) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 200 && st.count(VARKA_BADGE_CAPTAIN) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 200);
					st.takeItems(VARKA_BADGE_CAPTAIN, 100);
					st.takeItems(MARK_KETRA_1, -1);
					st.giveItems(MARK_KETRA_2, 1);
					st.set("cond", 3);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-08.htm"; // Promovido para Estágio 2!
				}
				return "31371-06.htm";
			}

			// Estágio 3 -> Requer 300 Soldier + 200 Officer + 100 Captain Badges
			if (cond == 3) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 300 && st.count(VARKA_BADGE_CAPTAIN) >= 200 && st.count(VARKA_BADGE_GENERAL) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 300);
					st.takeItems(VARKA_BADGE_CAPTAIN, 200);
					st.takeItems(VARKA_BADGE_GENERAL, 100);
					st.takeItems(MARK_KETRA_2, -1);
					st.giveItems(MARK_KETRA_3, 1);
					st.set("cond", 4);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-11.htm"; // Promovido para Estágio 3!
				}
				return "31371-09.htm";
			}

			if (cond == 4) {
				if (st.hasQuestItems(MARK_KETRA_4)) {
					return "31371-14.htm";
				}
				return "31371-12.htm";
			}

			if (cond >= 5) {
				return "31371-15.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState st = checkQuestState(player);
		if (st == null || !st.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = st.getInt("cond");

		if (VARKA_SOLDIERS.contains(npcId)) {
			int max = cond == 1 ? 100 : (cond == 2 ? 200 : 300);
			if (st.count(VARKA_BADGE_SOLDIER) < max) {
				st.giveItems(VARKA_BADGE_SOLDIER, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (VARKA_OFFICERS.contains(npcId) && cond >= 2) {
			int max = cond == 2 ? 100 : 200;
			if (st.count(VARKA_BADGE_CAPTAIN) < max) {
				st.giveItems(VARKA_BADGE_CAPTAIN, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (VARKA_CAPTAINS.contains(npcId) && cond >= 3) {
			if (st.count(VARKA_BADGE_GENERAL) < 100) {
				st.giveItems(VARKA_BADGE_GENERAL, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
