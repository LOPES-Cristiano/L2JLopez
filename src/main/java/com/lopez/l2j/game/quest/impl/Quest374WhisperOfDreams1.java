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
 * Quest 374: Whisper of Dreams, Part 1
 * Seer Manakia em Giran & Torai em Dragon Valley.
 * Farm de Death Blader e Cave Beast no Lair of Antharas para receitas/partes de armaduras A-Grade seladas.
 */
@Component
public class Quest374WhisperOfDreams1 extends Quest {

	public static final int QUEST_ID = 374;
	public static final String QUEST_NAME = "374_WhisperOfDreams1";

	public static final int MANAKIA = 30515;
	public static final int TORAI = 30557;

	public static final int CAVE_BEAST = 20620;
	public static final int DEATH_BLADER = 20621;

	public static final int CAVE_BEAST_TOOTH = 5884;
	public static final int DEATH_BLADER_LIGHT = 5885;
	public static final int SEALED_MYSTERIOUS_STONE = 5886;
	public static final int MYSTERIOUS_STONE = 5887;

	// Aliases de compatibilidade para testes
	public static final int VAN_DIEM = MANAKIA;
	public static final int CB_TOOTH = CAVE_BEAST_TOOTH;
	public static final int DW_LIGHT = DEATH_BLADER_LIGHT;
	public static final int SEALED_STONE = SEALED_MYSTERIOUS_STONE;

	@Autowired
	public Quest374WhisperOfDreams1(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Whisper of Dreams, Part 1");
		addStartNpc(MANAKIA);
		addTalkId(MANAKIA, TORAI);
		addKillId(CAVE_BEAST, DEATH_BLADER);
		registerQuestItems(CAVE_BEAST_TOOTH, DEATH_BLADER_LIGHT, SEALED_MYSTERIOUS_STONE, MYSTERIOUS_STONE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event))) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "seer_manakia_q0374_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			return "seer_manakia_q0374_06.htm";
		} else if ("reply_2".equalsIgnoreCase(event)) {
			qs.exitQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
			return "seer_manakia_q0374_07.htm";
		} else if (("reply_3".equalsIgnoreCase(event) || "reward_exchange".equalsIgnoreCase(event)) && qs.getQuestItemsCount(CAVE_BEAST_TOOTH) >= 65 && qs.getQuestItemsCount(DEATH_BLADER_LIGHT) >= 65) {
			qs.takeItems(CAVE_BEAST_TOOTH, -1);
			qs.takeItems(DEATH_BLADER_LIGHT, -1);
			qs.giveItems(5486, 3); // Tallum Tunic Textures
			qs.giveItems(57, 15886);
			qs.playSound(QuestState.SOUND_FINISH);
			return "seer_manakia_q0374_10.htm";
		} else if ("torai_stone".equalsIgnoreCase(event) || ("reply_1".equalsIgnoreCase(event) && qs.hasQuestItems(SEALED_MYSTERIOUS_STONE))) {
			qs.takeItems(SEALED_MYSTERIOUS_STONE, -1);
			qs.giveItems(MYSTERIOUS_STONE, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "torai_q0374_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == MANAKIA) {
			if (cond == 0) {
				if (pc.level() >= 56) return "seer_manakia_q0374_01.htm";
				qs.exitQuest(true);
				return "seer_manakia_q0374_02.htm";
			} else {
				if (qs.getQuestItemsCount(CAVE_BEAST_TOOTH) >= 65 && qs.getQuestItemsCount(DEATH_BLADER_LIGHT) >= 65) {
					return "seer_manakia_q0374_05.htm";
				}
				return "seer_manakia_q0374_04.htm";
			}
		} else if (npcId == TORAI) {
			if (qs.hasQuestItems(SEALED_MYSTERIOUS_STONE)) {
				return "torai_q0374_01.htm";
			}
			return "torai_q0374_03.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		if (npcId == CAVE_BEAST) {
			qs.giveItems(CAVE_BEAST_TOOTH, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == DEATH_BLADER) {
			qs.giveItems(DEATH_BLADER_LIGHT, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		if (!qs.hasQuestItems(SEALED_MYSTERIOUS_STONE) && !qs.hasQuestItems(MYSTERIOUS_STONE) && ThreadLocalRandom.current().nextInt(100) < 5) {
			qs.giveItems(SEALED_MYSTERIOUS_STONE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
