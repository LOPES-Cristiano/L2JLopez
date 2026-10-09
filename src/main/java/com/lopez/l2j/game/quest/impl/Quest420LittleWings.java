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
 * Quest 420: Little Wings
 * Cooper em Giran -> Cronos em Hunters -> Maria em Dion -> Fairy Mymyu em Enchanted Valley.
 * Recompensa: Flauta do Hatchling (Dragonflute of Wind 3500, Star 3501 ou Twilight 3502).
 */
@Component
public class Quest420LittleWings extends Quest {

	public static final int QUEST_ID = 420;
	public static final String QUEST_NAME = "420_LittleWings";

	// NPCs
	public static final int COOPER = 30829;
	public static final int CRONOS = 30610;
	public static final int BYRON = 30711;
	public static final int MARIA = 30608;
	public static final int MYMYU = 30747;
	public static final int EXARION = 30748;
	public static final int ZWEN = 30749;
	public static final int KALIBRAN = 30750;
	public static final int SUZET = 30751;
	public static final int SHAMHAI = 30752;

	// Itens
	public static final int FAIRY_STONE = 3816;
	public static final int DELUXE_FAIRY_STONE = 3817;
	public static final int FAIRY_STONE_LIST = 3818;
	public static final int DELUXE_FAIRY_STONE_LIST = 3819;
	public static final int TOAD_LORD_BACK_SKIN = 3820;
	public static final int JUICE_OF_MONKSHOOD = 3821;
	public static final int SCALE_OF_DRAKE_KALIBRAN = 3826;
	public static final int EGG_OF_DRAKE_KALIBRAN = 3827;

	// Flautas
	public static final int DRAGONFLUTE_OF_WIND = 3500;
	public static final int DRAGONFLUTE_OF_STAR = 3501;
	public static final int DRAGONFLUTE_OF_TWILIGHT = 3502;

	// Monstros
	public static final int ENCHANTED_VALLEY_DRAKE = 20580;
	public static final int TOAD_LORD = 20231;

	@Autowired
	public Quest420LittleWings(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Little Wings");
		addStartNpc(COOPER);
		addTalkId(COOPER, CRONOS, BYRON, MARIA, MYMYU, EXARION, ZWEN, KALIBRAN, SUZET, SHAMHAI);
		addKillId(TOAD_LORD, ENCHANTED_VALLEY_DRAKE);
		registerQuestItems(
				FAIRY_STONE, DELUXE_FAIRY_STONE, FAIRY_STONE_LIST, DELUXE_FAIRY_STONE_LIST,
				TOAD_LORD_BACK_SKIN, JUICE_OF_MONKSHOOD, SCALE_OF_DRAKE_KALIBRAN, EGG_OF_DRAKE_KALIBRAN
		);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if ("30829-02.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30610-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(FAIRY_STONE_LIST, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30608-03.htm".equalsIgnoreCase(event)) {
			// Entrega ingredientes a Maria e recebe Fairy Stone
			qs.setCond(3);
			qs.takeItems(FAIRY_STONE_LIST, -1);
			qs.takeItems(TOAD_LORD_BACK_SKIN, -1);
			qs.giveItems(FAIRY_STONE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30711-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30747-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(FAIRY_STONE, -1);
			qs.giveItems(JUICE_OF_MONKSHOOD, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30750-02.htm".equalsIgnoreCase(event)) {
			// Kalibran pede ovos
			qs.setCond(6);
			qs.giveItems(SCALE_OF_DRAKE_KALIBRAN, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("reward_flute".equalsIgnoreCase(event) || "30747-07.htm".equalsIgnoreCase(event)) {
			// Recompensa o Hatchling
			qs.takeItems(SCALE_OF_DRAKE_KALIBRAN, -1);
			qs.takeItems(EGG_OF_DRAKE_KALIBRAN, -1);
			qs.takeItems(JUICE_OF_MONKSHOOD, -1);
			int flute = switch (ThreadLocalRandom.current().nextInt(3)) {
				case 0 -> DRAGONFLUTE_OF_WIND;
				case 1 -> DRAGONFLUTE_OF_STAR;
				default -> DRAGONFLUTE_OF_TWILIGHT;
			};
			qs.giveItems(flute, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "30747-07.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == COOPER) {
			if (cond == 0) {
				if (pc.level() >= 35) {
					return "30829-01.htm";
				}
				qs.exitQuest(true);
				return "30829-00.htm";
			}
			return "30829-03.htm";
		} else if (npcId == CRONOS) {
			if (cond == 1) return "30610-01.htm";
			if (cond == 2) return "30610-07.htm";
		} else if (npcId == MARIA) {
			if (cond == 2) return "30608-01.htm";
			if (cond == 3) return "30608-04.htm";
		} else if (npcId == BYRON) {
			if (cond == 3) return "30711-01.htm";
			if (cond == 4) return "30711-04.htm";
		} else if (npcId == MYMYU) {
			if (cond == 4) return "30747-01.htm";
			if (cond == 5) return "30747-03.htm";
			if (cond >= 6 && qs.hasQuestItems(EGG_OF_DRAKE_KALIBRAN)) {
				return "30747-06.htm";
			}
		} else if (npcId == KALIBRAN) {
			if (cond == 5) return "30750-01.htm";
			if (cond == 6) {
				if (qs.getQuestItemsCount(EGG_OF_DRAKE_KALIBRAN) >= 20) {
					return "30750-03.htm";
				}
				return "30750-02.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == TOAD_LORD && cond == 2) {
			if (qs.getQuestItemsCount(TOAD_LORD_BACK_SKIN) < 10) {
				qs.giveItems(TOAD_LORD_BACK_SKIN, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == ENCHANTED_VALLEY_DRAKE && cond == 6) {
			if (qs.getQuestItemsCount(EGG_OF_DRAKE_KALIBRAN) < 20) {
				qs.giveItems(EGG_OF_DRAKE_KALIBRAN, 1);
				if (qs.getQuestItemsCount(EGG_OF_DRAKE_KALIBRAN) >= 20) {
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
