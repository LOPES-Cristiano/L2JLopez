package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 617: Gather the Flames
 *
 * Coleta de Torches (Item 7264) em Forge of the Gods para troca por receitas de armas S-Grade
 * com o ferreiro Vulcan (31539) e Warsmith Rooney (32049).
 */
@Component
public class Quest617GatherTheFlames extends Quest {

	public static final int QUEST_ID = 617;
	public static final String QUEST_NAME = "617_GatherTheFlames";

	public static final int TORCH = 7264;
	public static final int VULCAN = 31539;
	public static final int ROONEY = 32049;
	public static final int HILDA = 31271;

	// Receitas de Armas S-Grade (60%): Forgotten Blade, Basalt Battlehammer, Imperial Staff, Angel Slayer,
	// Dragon Hunter Axe, Saint's Spear, Demon Splinter, Heavens Divider, Arcana Mace, Draconic Bow
	public static final List<Integer> S_GRADE_WEAPON_RECIPES = List.of(
			6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580);

	// Lista de monstros clássicos de FotG
	public static final int[] FOTG_MOBS = {
			21381, 21653, 21387, 21655, 21390, 21656, 21389, 21388,
			21383, 21392, 21382, 21654, 21384, 21394, 21395, 21385,
			21391, 21393, 21657, 21386, 21652, 21378, 21376, 21377,
			21379, 21380
	};

	public Quest617GatherTheFlames() {
		super(QUEST_ID, QUEST_NAME, "Gather the Flames");
		addStartNpc(VULCAN, HILDA);
		addTalkId(VULCAN, ROONEY, HILDA);
		for (int mobId : FOTG_MOBS) {
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

		PlayerCharacter c = player.activeChar();

		if ("31539-03.htm".equalsIgnoreCase(event)) {
			if (c != null && c.level() >= 74) {
				st.setState(State.STARTED);
				st.set("cond", 1);
				st.playSound(QuestState.SOUND_ACCEPT);
				return "31539-03.htm";
			} else {
				st.exitQuest(true);
				return "31539-02.htm";
			}
		} else if ("31539-05.htm".equalsIgnoreCase(event)) {
			// Troca de 1.000 Torches por receita S-Grade
			if (st.count(TORCH) >= 1000) {
				st.takeItems(TORCH, 1000);
				int randomRecipe = S_GRADE_WEAPON_RECIPES.get(
						ThreadLocalRandom.current().nextInt(S_GRADE_WEAPON_RECIPES.size()));
				st.giveItems(randomRecipe, 1);
				st.playSound(QuestState.SOUND_FINISH);
				return "31539-07.htm";
			}
			return "31539-06.htm"; // Torches insuficientes
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

		int npcId = npc.npcId();

		if (npcId == VULCAN) {
			if (st.isCreated()) {
				if (c.level() < 74) {
					return "31539-02.htm";
				}
				return "31539-01.htm";
			}
			if (st.isStarted()) {
				if (st.count(TORCH) >= 1000) {
					return "31539-04.htm"; // Pode trocar
				}
				return "31539-05a.htm"; // Menos de 1000 torches
			}
		} else if (npcId == ROONEY) {
			if (st.isStarted() && st.count(TORCH) >= 1000) {
				return "32049-01.htm"; // Rooney oferece troca se tiver 1000 torches
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

		// Drop com taxa de ~55% por mob em FotG
		if (ThreadLocalRandom.current().nextInt(100) < 55) {
			st.giveItems(TORCH, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
