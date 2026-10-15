package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 222: Test of the Duelist
 * 3º Passo de 2ª Classe para Gladiator, Warlord, Paladin, Dark Avenger, Sword Singer, Bladedancer, Tyrant.
 */
@Component
public class Quest222TestOfDuelist extends Quest {

	public static final int QUEST_ID = 222;
	public static final String QUEST_NAME = "222_TestOfDuelist";

	// NPCs
	public static final int KAIYAN = 30623;

	// Monstros
	public static final int PUNCHER = 20085;
	public static final int NOBLE_ANT = 20090;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int DEAD_SEEKER = 20202;
	public static final int BREKA_OVERLORD = 20270;
	public static final int FETTERED_SOUL = 20552;
	public static final int LETO_OVERLORD = 20582;
	public static final int ENCHANTED_MONSTER_EYE = 20564;
	public static final int TAMLIN_ORC = 20601;
	public static final int TAMLIN_ORC_ARCHER = 20602;

	// Monstros Finais
	public static final int EXCURO = 20214;
	public static final int KRATOR = 20217;
	public static final int GRANDIS = 20554;
	public static final int TIMAK_ORC_OVERLORD = 20588;
	public static final int LAKIN = 20604;

	// Itens
	public static final int MARK_OF_DUELIST = 2762;
	public static final int ORDER_GLUDIO = 2763;
	public static final int ORDER_DION = 2764;
	public static final int ORDER_GIRAN = 2765;
	public static final int ORDER_OREN = 2766;
	public static final int ORDER_ADEN = 2767;
	public static final int PUNCHER_SHARD = 2768;
	public static final int NOBLE_ANT_FEELER = 2769;
	public static final int DRONE_CHITIN = 2770;
	public static final int DEAD_SEEKER_FANG = 2771;
	public static final int OVERLORD_NECKLACE = 2772;
	public static final int FETTERED_SOUL_CHAIN = 2773;
	public static final int CHIEF_AMULET = 2774;
	public static final int ENCHANTED_EYE_MEAT = 2775;
	public static final int TAMRIN_ORC_RING = 2776;
	public static final int TAMRIN_ORC_ARROW = 2777;
	public static final int FINAL_ORDER = 2778;
	public static final int EXCURO_SKIN = 2779;
	public static final int KRATOR_SHARD = 2780;
	public static final int GRANDIS_SKIN = 2781;
	public static final int TIMAK_ORC_BELT = 2782;
	public static final int LAKIN_MACE = 2783;

	private static final Set<Integer> VALID_CLASSES = Set.of(1, 19, 32, 45, 47);

	@Autowired
	public Quest222TestOfDuelist(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Duelist");

		addStartNpc(KAIYAN);
		addTalkId(KAIYAN);

		addKillId(PUNCHER, NOBLE_ANT, MARSH_STAKATO_DRONE, DEAD_SEEKER, BREKA_OVERLORD,
				FETTERED_SOUL, LETO_OVERLORD, ENCHANTED_MONSTER_EYE, TAMLIN_ORC, TAMLIN_ORC_ARCHER,
				EXCURO, KRATOR, GRANDIS, TIMAK_ORC_OVERLORD, LAKIN);

		registerQuestItems(ORDER_GLUDIO, ORDER_DION, ORDER_GIRAN, ORDER_OREN, ORDER_ADEN,
				PUNCHER_SHARD, NOBLE_ANT_FEELER, DRONE_CHITIN, DEAD_SEEKER_FANG, OVERLORD_NECKLACE,
				FETTERED_SOUL_CHAIN, CHIEF_AMULET, ENCHANTED_EYE_MEAT, TAMRIN_ORC_RING, TAMRIN_ORC_ARROW,
				FINAL_ORDER, EXCURO_SKIN, KRATOR_SHARD, GRANDIS_SKIN, TIMAK_ORC_BELT, LAKIN_MACE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();

		if ("30623-04.htm".equalsIgnoreCase(event)) {
			if (player != null && player.race() == 3) {
				return "30623-05.htm";
			}
			return "30623-04.htm";
		} else if ("1".equalsIgnoreCase(event) || "30623-07.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.setCond(2);
			qs.giveItems(ORDER_GLUDIO, 1);
			qs.giveItems(ORDER_DION, 1);
			qs.giveItems(ORDER_GIRAN, 1);
			qs.giveItems(ORDER_OREN, 1);
			qs.giveItems(ORDER_ADEN, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30623-07.htm";
		} else if ("30623-16.htm".equalsIgnoreCase(event) || "30623_1".equalsIgnoreCase(event)) {
			qs.takeItems(ORDER_GLUDIO, -1);
			qs.takeItems(ORDER_DION, -1);
			qs.takeItems(ORDER_GIRAN, -1);
			qs.takeItems(ORDER_OREN, -1);
			qs.takeItems(ORDER_ADEN, -1);
			qs.takeItems(PUNCHER_SHARD, -1);
			qs.takeItems(NOBLE_ANT_FEELER, -1);
			qs.takeItems(DRONE_CHITIN, -1);
			qs.takeItems(DEAD_SEEKER_FANG, -1);
			qs.takeItems(OVERLORD_NECKLACE, -1);
			qs.takeItems(FETTERED_SOUL_CHAIN, -1);
			qs.takeItems(CHIEF_AMULET, -1);
			qs.takeItems(ENCHANTED_EYE_MEAT, -1);
			qs.takeItems(TAMRIN_ORC_RING, -1);
			qs.takeItems(TAMRIN_ORC_ARROW, -1);
			qs.giveItems(FINAL_ORDER, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30623-16.htm";
		} else if ("30623-19.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(FINAL_ORDER)) {
				qs.takeItems(FINAL_ORDER, -1);
				qs.takeItems(EXCURO_SKIN, -1);
				qs.takeItems(KRATOR_SHARD, -1);
				qs.takeItems(GRANDIS_SKIN, -1);
				qs.takeItems(TIMAK_ORC_BELT, -1);
				qs.takeItems(LAKIN_MACE, -1);
				qs.giveItems(MARK_OF_DUELIST, 1);
				qs.addExpAndSp(47215, 4000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int cond = qs.getCond();

		if (cond == 0) {
			if (qs.isCompleted()) {
				return "<html><body>This quest has already been completed.</body></html>";
			} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
				return "30623-03.htm";
			} else {
				return "30623-01.htm";
			}
		} else if (cond == 1 || cond == 2) {
			return "30623-14.htm";
		} else if (cond == 3) {
			return "30623-15.htm";
		} else if (cond == 4) {
			return "30623-17.htm";
		} else if (cond == 5) {
			if (qs.getQuestItemsCount(EXCURO_SKIN) >= 3 && qs.getQuestItemsCount(KRATOR_SHARD) >= 3
					&& qs.getQuestItemsCount(GRANDIS_SKIN) >= 3 && qs.getQuestItemsCount(TIMAK_ORC_BELT) >= 3
					&& qs.getQuestItemsCount(LAKIN_MACE) >= 3) {
				qs.takeItems(FINAL_ORDER, -1);
				qs.takeItems(EXCURO_SKIN, -1);
				qs.takeItems(KRATOR_SHARD, -1);
				qs.takeItems(GRANDIS_SKIN, -1);
				qs.takeItems(TIMAK_ORC_BELT, -1);
				qs.takeItems(LAKIN_MACE, -1);
				qs.giveItems(MARK_OF_DUELIST, 1);
				qs.addExpAndSp(47215, 4000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
			}
			return "30623-18.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 2) {
			int itemToGive = 0;
			switch (npcId) {
				case PUNCHER -> itemToGive = PUNCHER_SHARD;
				case NOBLE_ANT -> itemToGive = NOBLE_ANT_FEELER;
				case MARSH_STAKATO_DRONE -> itemToGive = DRONE_CHITIN;
				case DEAD_SEEKER -> itemToGive = DEAD_SEEKER_FANG;
				case BREKA_OVERLORD -> itemToGive = OVERLORD_NECKLACE;
				case FETTERED_SOUL -> itemToGive = FETTERED_SOUL_CHAIN;
				case LETO_OVERLORD -> itemToGive = CHIEF_AMULET;
				case ENCHANTED_MONSTER_EYE -> itemToGive = ENCHANTED_EYE_MEAT;
				case TAMLIN_ORC -> itemToGive = TAMRIN_ORC_RING;
				case TAMLIN_ORC_ARCHER -> itemToGive = TAMRIN_ORC_ARROW;
			}
			if (itemToGive > 0 && qs.getQuestItemsCount(itemToGive) < 10) {
				qs.giveItems(itemToGive, 1);
				if (qs.getQuestItemsCount(PUNCHER_SHARD) >= 10
						&& qs.getQuestItemsCount(NOBLE_ANT_FEELER) >= 10
						&& qs.getQuestItemsCount(DRONE_CHITIN) >= 10
						&& qs.getQuestItemsCount(DEAD_SEEKER_FANG) >= 10
						&& qs.getQuestItemsCount(OVERLORD_NECKLACE) >= 10
						&& qs.getQuestItemsCount(FETTERED_SOUL_CHAIN) >= 10
						&& qs.getQuestItemsCount(CHIEF_AMULET) >= 10
						&& qs.getQuestItemsCount(ENCHANTED_EYE_MEAT) >= 10
						&& qs.getQuestItemsCount(TAMRIN_ORC_RING) >= 10
						&& qs.getQuestItemsCount(TAMRIN_ORC_ARROW) >= 10) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 4) {
			int itemToGive = 0;
			switch (npcId) {
				case EXCURO -> itemToGive = EXCURO_SKIN;
				case KRATOR -> itemToGive = KRATOR_SHARD;
				case GRANDIS -> itemToGive = GRANDIS_SKIN;
				case TIMAK_ORC_OVERLORD -> itemToGive = TIMAK_ORC_BELT;
				case LAKIN -> itemToGive = LAKIN_MACE;
			}
			if (itemToGive > 0 && qs.getQuestItemsCount(itemToGive) < 3) {
				qs.giveItems(itemToGive, 1);
				if (qs.getQuestItemsCount(EXCURO_SKIN) >= 3
						&& qs.getQuestItemsCount(KRATOR_SHARD) >= 3
						&& qs.getQuestItemsCount(GRANDIS_SKIN) >= 3
						&& qs.getQuestItemsCount(TIMAK_ORC_BELT) >= 3
						&& qs.getQuestItemsCount(LAKIN_MACE) >= 3) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}
