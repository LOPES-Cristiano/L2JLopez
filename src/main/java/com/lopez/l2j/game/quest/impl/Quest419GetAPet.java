package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 419: Get a Pet
 * Pet Manager Martin em Gludio, cacada de garras/presas e quiz para obter o Lobo (Wolf Collar 2375).
 */
@Component
public class Quest419GetAPet extends Quest {

	public static final int QUEST_ID = 419;
	public static final String QUEST_NAME = "419_GetAPet";

	// NPCs
	public static final int MARTIN = 30731;
	public static final int BELLA = 30256;
	public static final int METTY = 30072;
	public static final int ELLIE = 30091;

	// Itens
	public static final int ANIMAL_LOVER_LIST1 = 3417;
	public static final int ANIMAL_SLAYER_LIST1 = 3418;
	public static final int ANIMAL_SLAYER_LIST2 = 3419;
	public static final int ANIMAL_SLAYER_LIST3 = 3420;
	public static final int ANIMAL_SLAYER_LIST4 = 3421;
	public static final int ANIMAL_SLAYER_LIST5 = 3422;
	public static final int BLOODY_FANG = 3423;
	public static final int BLOODY_CLAW = 3424;
	public static final int BLOODY_NAIL = 3425;
	public static final int BLOODY_KASHA_FANG = 3426;
	public static final int BLOODY_TARANTULA_NAIL = 3427;
	public static final int WOLF_COLLAR = 2375;

	// Mobs por raca
	public static final int[] MOBS = {
			20103, 20106, 20108, // Human: Giant Spider, Talon Spider, Blade Spider
			20460, 20308, 20466, // Elf: Crimson Spider, Hook Spider, Pincer Spider
			20025, 20105, 20034, // Dark Elf: Lesser Dark Horror, Dark Horror, Prowler
			20474, 20476, 20478, // Orc: Kasha Spider, Blade Spider, Giant Spider
			20403, 20508         // Dwarf: Hunter Tarantula, Plunder Tarantula
	};

	@Autowired
	public Quest419GetAPet(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Get a Pet");
		addStartNpc(MARTIN);
		addTalkId(MARTIN, BELLA, METTY, ELLIE);
		for (int m : MOBS) {
			addKillId(m);
		}
		registerQuestItems(
				ANIMAL_LOVER_LIST1, ANIMAL_SLAYER_LIST1, ANIMAL_SLAYER_LIST2,
				ANIMAL_SLAYER_LIST3, ANIMAL_SLAYER_LIST4, ANIMAL_SLAYER_LIST5,
				BLOODY_FANG, BLOODY_CLAW, BLOODY_NAIL, BLOODY_KASHA_FANG, BLOODY_TARANTULA_NAIL
		);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private int getProofItem(int race) {
		return switch (race) {
			case 1 -> BLOODY_CLAW;           // Elf
			case 2 -> BLOODY_NAIL;           // Dark Elf
			case 3 -> BLOODY_KASHA_FANG;     // Orc
			case 4 -> BLOODY_TARANTULA_NAIL; // Dwarf
			default -> BLOODY_FANG;          // Human
		};
	}

	private int getSlayerList(int race) {
		return switch (race) {
			case 1 -> ANIMAL_SLAYER_LIST2;
			case 2 -> ANIMAL_SLAYER_LIST3;
			case 3 -> ANIMAL_SLAYER_LIST4;
			case 4 -> ANIMAL_SLAYER_LIST5;
			default -> ANIMAL_SLAYER_LIST1;
		};
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if ("details".equalsIgnoreCase(event)) {
			return "419_confirm.htm";
		} else if ("agree".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			int list = getSlayerList(pc.race());
			qs.giveItems(list, 1);
			return "419_slay.htm";
		} else if ("disagree".equalsIgnoreCase(event)) {
			qs.exitQuest(true);
			return "419_cancelled.htm";
		} else if ("talk_bella".equalsIgnoreCase(event)) {
			qs.set("talk_bella", "1");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30256-02.htm";
		} else if ("talk_ellie".equalsIgnoreCase(event)) {
			qs.set("talk_ellie", "1");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30091-02.htm";
		} else if ("talk_metty".equalsIgnoreCase(event)) {
			qs.set("talk_metty", "1");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30072-02.htm";
		} else if ("try_quiz".equalsIgnoreCase(event) || "correct_answer".equalsIgnoreCase(event)) {
			// Acertou as perguntas do quiz de Martin
			qs.takeItems(ANIMAL_LOVER_LIST1, -1);
			qs.giveItems(WOLF_COLLAR, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "Completed.htm";
		} else if ("wrong_answer".equalsIgnoreCase(event)) {
			return "419_failed.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == MARTIN) {
			if (cond == 0) {
				if (pc.level() >= 15) {
					return "Start.htm";
				}
				qs.exitQuest(true);
				return "419_low_level.htm";
			} else if (cond == 1) {
				int proof = getProofItem(pc.race());
				if (qs.getQuestItemsCount(proof) >= 50) {
					qs.setCond(2);
					qs.takeItems(proof, -1);
					qs.takeItems(getSlayerList(pc.race()), -1);
					qs.giveItems(ANIMAL_LOVER_LIST1, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "419_talk_villagers.htm";
				}
				return "419_slay.htm";
			} else if (cond == 2) {
				// Verifica se conversou com Bella, Ellie e Metty
				if (qs.getInt("talk_bella") == 1 && qs.getInt("talk_ellie") == 1 && qs.getInt("talk_metty") == 1) {
					return "419_quiz_intro.htm";
				}
				return "419_talk_villagers.htm";
			}
		} else if (npcId == BELLA && cond == 2) {
			return "30256-01.htm";
		} else if (npcId == ELLIE && cond == 2) {
			return "30091-01.htm";
		} else if (npcId == METTY && cond == 2) {
			return "30072-01.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null || qs.getCond() != 1) return null;

		int proof = getProofItem(pc.race());
		long count = qs.getQuestItemsCount(proof);
		if (count < 50) {
			qs.giveItems(proof, 1);
			if (count + 1 >= 50) {
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
