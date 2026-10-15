package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 230: Test of the Summoner
 * 3º Passo de 2ª Classe para Warlock, Elemental Summoner, Phantom Summoner.
 */
@Component
public class Quest230TestOfSummoner extends Quest {

	public static final int QUEST_ID = 230;
	public static final String QUEST_NAME = "230_TestOfSummoner";

	// NPCs
	public static final int LARA = 30063;
	public static final int GALATEA = 30634;
	public static final int ALMORS = 30635;
	public static final int CAMONIELL = 30636;
	public static final int BELTHUS = 30637;
	public static final int BASILLA = 30638;
	public static final int CELESTIEL = 30639;
	public static final int BRYNTHEA = 30640;

	// Monstros
	public static final int NOBLE_ANT = 20089;
	public static final int NOBLE_ANT_LEADER = 20090;
	public static final int WYRM = 20176;
	public static final int TYRANT = 20192;
	public static final int TYRANT_KINGPIN = 20193;
	public static final int BREKA_ORC = 20267;
	public static final int BREKA_ORC_ARCHER = 20268;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int BREKA_ORC_WARRIOR = 20271;
	public static final int FETTERED_SOUL = 20552;
	public static final int WINDSUS = 20553;
	public static final int GIANT_FUNGUS = 20555;
	public static final int MANASHEN_GARGOYLE = 20563;
	public static final int LETO_LIZARDMAN = 20577;
	public static final int LETO_LIZARDMAN_ARCHER = 20578;
	public static final int LETO_LIZARDMAN_SOLDIER = 20579;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int KARUL_BUGBEAR = 20600;

	// Quest Monsters
	public static final int PAKO_THE_CAT = 27102;
	public static final int UNICORN_RACER = 27103;
	public static final int SHADOW_TUREN = 27104;
	public static final int MIMI_THE_CAT = 27105;
	public static final int UNICORN_PHANTASM = 27106;
	public static final int SILHOUETTE_TILFO = 27107;

	// Itens
	public static final int MARK_OF_SUMMONER = 3336;
	public static final int LETO_LIZARDMAN_AMULET = 3337;
	public static final int SAC_OF_REDSPORES = 3338;
	public static final int KARUL_BUGBEAR_TOTEM = 3339;
	public static final int SHARDS_OF_MANASHEN = 3340;
	public static final int BREKA_ORC_TOTEM = 3341;
	public static final int CRIMSON_BLOODSTONE = 3342;
	public static final int TALONS_OF_TYRANT = 3343;
	public static final int WINGS_OF_DRONEANT = 3344;
	public static final int TUSK_OF_WINDSUS = 3345;
	public static final int FANGS_OF_WYRM = 3346;
	public static final int LARA_LIST_1 = 3347;
	public static final int LARA_LIST_2 = 3348;
	public static final int LARA_LIST_3 = 3349;
	public static final int LARA_LIST_4 = 3350;
	public static final int LARA_LIST_5 = 3351;
	public static final int GALATEAS_LETTER = 3352;
	public static final int BEGINNERS_ARCANA = 3353;
	public static final int ALMORS_ARCANA = 3354;
	public static final int CAMONIELL_ARCANA = 3355;
	public static final int BELTHUS_ARCANA = 3356;
	public static final int BASILLIA_ARCANA = 3357;
	public static final int CELESTIEL_ARCANA = 3358;
	public static final int BRYNTHEA_ARCANA = 3359;
	public static final int CRYSTAL_OF_PROGRESS_1 = 3360;
	public static final int CRYSTAL_OF_INPROGRESS_1 = 3361;
	public static final int CRYSTAL_OF_FOUL_1 = 3362;
	public static final int CRYSTAL_OF_DEFEAT_1 = 3363;
	public static final int CRYSTAL_OF_VICTORY_1 = 3364;
	public static final int CRYSTAL_OF_PROGRESS_2 = 3365;
	public static final int CRYSTAL_OF_INPROGRESS_2 = 3366;
	public static final int CRYSTAL_OF_FOUL_2 = 3367;
	public static final int CRYSTAL_OF_DEFEAT_2 = 3368;
	public static final int CRYSTAL_OF_VICTORY_2 = 3369;
	public static final int CRYSTAL_OF_PROGRESS_3 = 3370;
	public static final int CRYSTAL_OF_INPROGRESS_3 = 3371;
	public static final int CRYSTAL_OF_FOUL_3 = 3372;
	public static final int CRYSTAL_OF_DEFEAT_3 = 3373;
	public static final int CRYSTAL_OF_VICTORY_3 = 3374;
	public static final int CRYSTAL_OF_PROGRESS_4 = 3375;
	public static final int CRYSTAL_OF_INPROGRESS_4 = 3376;
	public static final int CRYSTAL_OF_FOUL_4 = 3377;
	public static final int CRYSTAL_OF_DEFEAT_4 = 3378;
	public static final int CRYSTAL_OF_VICTORY_4 = 3379;
	public static final int CRYSTAL_OF_PROGRESS_5 = 3380;
	public static final int CRYSTAL_OF_INPROGRESS_5 = 3381;
	public static final int CRYSTAL_OF_FOUL_5 = 3382;
	public static final int CRYSTAL_OF_DEFEAT_5 = 3383;
	public static final int CRYSTAL_OF_VICTORY_5 = 3384;
	public static final int CRYSTAL_OF_PROGRESS_6 = 3385;
	public static final int CRYSTAL_OF_INPROGRESS_6 = 3386;
	public static final int CRYSTAL_OF_FOUL_6 = 3387;
	public static final int CRYSTAL_OF_DEFEAT_6 = 3388;
	public static final int CRYSTAL_OF_VICTORY_6 = 3389;

	private static final int[][] LARA_LISTS = {
			{ LARA_LIST_1, SAC_OF_REDSPORES, LETO_LIZARDMAN_AMULET },
			{ LARA_LIST_2, KARUL_BUGBEAR_TOTEM, SHARDS_OF_MANASHEN },
			{ LARA_LIST_3, CRIMSON_BLOODSTONE, BREKA_ORC_TOTEM },
			{ LARA_LIST_4, TUSK_OF_WINDSUS, TALONS_OF_TYRANT },
			{ LARA_LIST_5, WINGS_OF_DRONEANT, FANGS_OF_WYRM }
	};

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 26, 39);

	@Autowired
	public Quest230TestOfSummoner(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Summoner");

		addStartNpc(GALATEA);
		addTalkId(GALATEA, LARA, ALMORS, CAMONIELL, BELTHUS, BASILLA, CELESTIEL, BRYNTHEA);

		addAttackId(PAKO_THE_CAT, UNICORN_RACER, SHADOW_TUREN, MIMI_THE_CAT, UNICORN_PHANTASM, SILHOUETTE_TILFO);
		addKillId(NOBLE_ANT, NOBLE_ANT_LEADER, WYRM, TYRANT, TYRANT_KINGPIN,
				BREKA_ORC, BREKA_ORC_ARCHER, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD, BREKA_ORC_WARRIOR,
				FETTERED_SOUL, WINDSUS, GIANT_FUNGUS, MANASHEN_GARGOYLE,
				LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER,
				LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD,
				KARUL_BUGBEAR, PAKO_THE_CAT, UNICORN_RACER, SHADOW_TUREN,
				MIMI_THE_CAT, UNICORN_PHANTASM, SILHOUETTE_TILFO);

		registerQuestItems(LETO_LIZARDMAN_AMULET, SAC_OF_REDSPORES, KARUL_BUGBEAR_TOTEM, SHARDS_OF_MANASHEN,
				BREKA_ORC_TOTEM, CRIMSON_BLOODSTONE, TALONS_OF_TYRANT, WINGS_OF_DRONEANT,
				TUSK_OF_WINDSUS, FANGS_OF_WYRM, LARA_LIST_1, LARA_LIST_2, LARA_LIST_3,
				LARA_LIST_4, LARA_LIST_5, GALATEAS_LETTER, BEGINNERS_ARCANA, ALMORS_ARCANA,
				CAMONIELL_ARCANA, BELTHUS_ARCANA, BASILLIA_ARCANA, CELESTIEL_ARCANA,
				BRYNTHEA_ARCANA, CRYSTAL_OF_PROGRESS_1, CRYSTAL_OF_INPROGRESS_1,
				CRYSTAL_OF_FOUL_1, CRYSTAL_OF_DEFEAT_1, CRYSTAL_OF_VICTORY_1,
				CRYSTAL_OF_PROGRESS_2, CRYSTAL_OF_INPROGRESS_2, CRYSTAL_OF_FOUL_2,
				CRYSTAL_OF_DEFEAT_2, CRYSTAL_OF_VICTORY_2, CRYSTAL_OF_PROGRESS_3,
				CRYSTAL_OF_INPROGRESS_3, CRYSTAL_OF_FOUL_3, CRYSTAL_OF_DEFEAT_3,
				CRYSTAL_OF_VICTORY_3, CRYSTAL_OF_PROGRESS_4, CRYSTAL_OF_INPROGRESS_4,
				CRYSTAL_OF_FOUL_4, CRYSTAL_OF_DEFEAT_4, CRYSTAL_OF_VICTORY_4,
				CRYSTAL_OF_PROGRESS_5, CRYSTAL_OF_INPROGRESS_5, CRYSTAL_OF_FOUL_5,
				CRYSTAL_OF_DEFEAT_5, CRYSTAL_OF_VICTORY_5, CRYSTAL_OF_PROGRESS_6,
				CRYSTAL_OF_INPROGRESS_6, CRYSTAL_OF_FOUL_6, CRYSTAL_OF_DEFEAT_6,
				CRYSTAL_OF_VICTORY_6);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30634-04.htm".equalsIgnoreCase(event) || "30634-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.set("Belthus", "1");
			qs.set("Brynthea", "1");
			qs.set("Celestiel", "1");
			qs.set("Camoniell", "1");
			qs.set("Basilla", "1");
			qs.set("Almors", "1");
			qs.giveItems(GALATEAS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30634-08.htm".equalsIgnoreCase(event) ? "30634-08.htm" : "30634-04.htm";
		} else if ("30634-09.htm".equalsIgnoreCase(event)) {
			if (hasAllArcanas(qs)) {
				qs.takeItems(BEGINNERS_ARCANA, -1);
				qs.takeItems(ALMORS_ARCANA, -1);
				qs.takeItems(CAMONIELL_ARCANA, -1);
				qs.takeItems(BELTHUS_ARCANA, -1);
				qs.takeItems(BASILLIA_ARCANA, -1);
				qs.takeItems(CELESTIEL_ARCANA, -1);
				qs.takeItems(BRYNTHEA_ARCANA, -1);
				qs.takeItems(LARA_LIST_1, -1);
				qs.takeItems(LARA_LIST_2, -1);
				qs.takeItems(LARA_LIST_3, -1);
				qs.takeItems(LARA_LIST_4, -1);
				qs.takeItems(LARA_LIST_5, -1);
				qs.giveItems(MARK_OF_SUMMONER, 1);
				qs.addExpAndSp(148409, 30000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		} else if ("30063-02.htm".equalsIgnoreCase(event) || "30063_1".equalsIgnoreCase(event)) {
			qs.takeItems(GALATEAS_LETTER, -1);
			int random = ThreadLocalRandom.current().nextInt(5);
			qs.giveItems(LARA_LISTS[random][0], 1);
			qs.set("Lara", String.valueOf(random + 1));
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30063-02.htm";
		} else if ("30063-04.htm".equalsIgnoreCase(event) || "30063_2".equalsIgnoreCase(event)) {
			int random = ThreadLocalRandom.current().nextInt(5);
			qs.giveItems(LARA_LISTS[random][0], 1);
			qs.set("Lara", String.valueOf(random + 1));
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30063-04.htm";
		} else if ("30635-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30635-03.htm" : "30635-02.htm";
		} else if ("30635-04.htm".equalsIgnoreCase(event) || "30635_1".equalsIgnoreCase(event)) {
			qs.set("Almors", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_1, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_1, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_1, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30635-04.htm";
		} else if ("30636-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30636-03.htm" : "30636-02.htm";
		} else if ("30636-04.htm".equalsIgnoreCase(event) || "30636_1".equalsIgnoreCase(event)) {
			qs.set("Camoniell", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_2, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_2, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_2, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30636-04.htm";
		} else if ("30637-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30637-03.htm" : "30637-02.htm";
		} else if ("30637-04.htm".equalsIgnoreCase(event) || "30637_1".equalsIgnoreCase(event)) {
			qs.set("Belthus", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_3, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_3, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_3, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30637-04.htm";
		} else if ("30638-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30638-03.htm" : "30638-02.htm";
		} else if ("30638-04.htm".equalsIgnoreCase(event) || "30638_1".equalsIgnoreCase(event)) {
			qs.set("Basilla", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_4, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_4, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_4, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30638-04.htm";
		} else if ("30639-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30639-03.htm" : "30639-02.htm";
		} else if ("30639-04.htm".equalsIgnoreCase(event) || "30639_1".equalsIgnoreCase(event)) {
			qs.set("Celestiel", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_5, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_5, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_5, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30639-04.htm";
		} else if ("30640-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(BEGINNERS_ARCANA) ? "30640-03.htm" : "30640-02.htm";
		} else if ("30640-04.htm".equalsIgnoreCase(event) || "30640_1".equalsIgnoreCase(event)) {
			qs.set("Brynthea", "2");
			qs.takeItems(CRYSTAL_OF_FOUL_6, -1);
			qs.takeItems(CRYSTAL_OF_DEFEAT_6, -1);
			qs.takeItems(BEGINNERS_ARCANA, 1);
			qs.giveItems(CRYSTAL_OF_PROGRESS_6, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30640-04.htm";
		}

		return event;
	}

	private boolean hasAllArcanas(QuestState qs) {
		return qs.hasQuestItems(ALMORS_ARCANA) && qs.hasQuestItems(CAMONIELL_ARCANA)
				&& qs.hasQuestItems(BELTHUS_ARCANA) && qs.hasQuestItems(BASILLIA_ARCANA)
				&& qs.hasQuestItems(CELESTIEL_ARCANA) && qs.hasQuestItems(BRYNTHEA_ARCANA);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == GALATEA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30634-03.htm";
				} else {
					return "30634-01.htm";
				}
			} else if (hasAllArcanas(qs) || cond == 4) {
				return "30634-08.htm";
			} else if (cond == 1) {
				return "30634-05.htm";
			} else if (cond == 2 || cond == 3) {
				return !qs.hasQuestItems(BEGINNERS_ARCANA) ? "30634-10.htm" : "30634-11.htm";
			}
		} else if (npcId == LARA) {
			if (cond == 1) {
				return "30063-01.htm";
			} else if (cond >= 2) {
				int laraIdx = qs.getInt("Lara");
				if (laraIdx == 0) {
					return "30063-03.htm";
				}
				int[] laraPart = LARA_LISTS[laraIdx - 1];
				if (qs.getQuestItemsCount(laraPart[1]) < 30 || qs.getQuestItemsCount(laraPart[2]) < 30) {
					return "30063-05.htm";
				}
				qs.takeItems(laraPart[0], -1);
				qs.takeItems(laraPart[1], -1);
				qs.takeItems(laraPart[2], -1);
				qs.giveItems(BEGINNERS_ARCANA, 2);
				qs.unset("Lara");
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30063-06.htm";
			}
		} else if (npcId == ALMORS) {
			int stat = qs.getInt("Almors");
			if (stat <= 1) return "30635-01.htm";
			if (stat == 2) return "30635-08.htm";
			if (stat == 3) return "30635-09.htm";
			if (stat == 4) return "30635-05.htm";
			if (stat == 5) return "30635-06.htm";
			if (stat == 6) {
				qs.set("Almors", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_1, -1);
				qs.giveItems(ALMORS_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30635-07.htm";
			}
			return "30635-10.htm";
		} else if (npcId == CAMONIELL) {
			int stat = qs.getInt("Camoniell");
			if (stat <= 1) return "30636-01.htm";
			if (stat == 2) return "30636-08.htm";
			if (stat == 3) return "30636-09.htm";
			if (stat == 4) return "30636-05.htm";
			if (stat == 5) return "30636-06.htm";
			if (stat == 6) {
				qs.set("Camoniell", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_2, -1);
				qs.giveItems(CAMONIELL_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30636-07.htm";
			}
			return "30636-10.htm";
		} else if (npcId == BELTHUS) {
			int stat = qs.getInt("Belthus");
			if (stat <= 1) return "30637-01.htm";
			if (stat == 2) return "30637-08.htm";
			if (stat == 3) return "30637-09.htm";
			if (stat == 4) return "30637-05.htm";
			if (stat == 5) return "30637-06.htm";
			if (stat == 6) {
				qs.set("Belthus", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_3, -1);
				qs.giveItems(BELTHUS_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30637-07.htm";
			}
			return "30637-10.htm";
		} else if (npcId == BASILLA) {
			int stat = qs.getInt("Basilla");
			if (stat <= 1) return "30638-01.htm";
			if (stat == 2) return "30638-08.htm";
			if (stat == 3) return "30638-09.htm";
			if (stat == 4) return "30638-05.htm";
			if (stat == 5) return "30638-06.htm";
			if (stat == 6) {
				qs.set("Basilla", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_4, -1);
				qs.giveItems(BASILLIA_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30638-07.htm";
			}
			return "30638-10.htm";
		} else if (npcId == CELESTIEL) {
			int stat = qs.getInt("Celestiel");
			if (stat <= 1) return "30639-01.htm";
			if (stat == 2) return "30639-08.htm";
			if (stat == 3) return "30639-09.htm";
			if (stat == 4) return "30639-05.htm";
			if (stat == 5) return "30639-06.htm";
			if (stat == 6) {
				qs.set("Celestiel", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_5, -1);
				qs.giveItems(CELESTIEL_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30639-07.htm";
			}
			return "30639-10.htm";
		} else if (npcId == BRYNTHEA) {
			int stat = qs.getInt("Brynthea");
			if (stat <= 1) return "30640-01.htm";
			if (stat == 2) return "30640-08.htm";
			if (stat == 3) return "30640-09.htm";
			if (stat == 4) return "30640-05.htm";
			if (stat == 5) return "30640-06.htm";
			if (stat == 6) {
				qs.set("Brynthea", "7");
				qs.takeItems(CRYSTAL_OF_VICTORY_6, -1);
				qs.giveItems(BRYNTHEA_ARCANA, 1);
				if (hasAllArcanas(qs)) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30640-07.htm";
			}
			return "30640-10.htm";
		}

		return "noquest";
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		switch (npcId) {
			case PAKO_THE_CAT -> {
				if (qs.getInt("Almors") == 2) {
					qs.set("Almors", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_1, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_1, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case UNICORN_RACER -> {
				if (qs.getInt("Camoniell") == 2) {
					qs.set("Camoniell", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_2, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_2, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case SHADOW_TUREN -> {
				if (qs.getInt("Belthus") == 2) {
					qs.set("Belthus", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_3, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_3, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MIMI_THE_CAT -> {
				if (qs.getInt("Basilla") == 2) {
					qs.set("Basilla", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_4, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_4, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case UNICORN_PHANTASM -> {
				if (qs.getInt("Celestiel") == 2) {
					qs.set("Celestiel", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_5, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_5, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case SILHOUETTE_TILFO -> {
				if (qs.getInt("Brynthea") == 2) {
					qs.set("Brynthea", "3");
					qs.takeItems(CRYSTAL_OF_PROGRESS_6, -1);
					qs.giveItems(CRYSTAL_OF_INPROGRESS_6, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int lara = qs.getInt("Lara");

		switch (npcId) {
			case GIANT_FUNGUS -> {
				if (lara == 1 && qs.getQuestItemsCount(SAC_OF_REDSPORES) < 30) {
					qs.giveItems(SAC_OF_REDSPORES, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER,
					LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD -> {
				if (lara == 1 && qs.getQuestItemsCount(LETO_LIZARDMAN_AMULET) < 30) {
					qs.giveItems(LETO_LIZARDMAN_AMULET, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MANASHEN_GARGOYLE -> {
				if (lara == 2 && qs.getQuestItemsCount(SHARDS_OF_MANASHEN) < 30) {
					qs.giveItems(SHARDS_OF_MANASHEN, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case KARUL_BUGBEAR -> {
				if (lara == 2 && qs.getQuestItemsCount(KARUL_BUGBEAR_TOTEM) < 30) {
					qs.giveItems(KARUL_BUGBEAR_TOTEM, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case BREKA_ORC, BREKA_ORC_ARCHER, BREKA_ORC_WARRIOR, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD -> {
				if (lara == 3 && qs.getQuestItemsCount(BREKA_ORC_TOTEM) < 30) {
					qs.giveItems(BREKA_ORC_TOTEM, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case FETTERED_SOUL -> {
				if (lara == 3 && qs.getQuestItemsCount(CRIMSON_BLOODSTONE) < 30) {
					qs.giveItems(CRIMSON_BLOODSTONE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case WINDSUS -> {
				if (lara == 4 && qs.getQuestItemsCount(TUSK_OF_WINDSUS) < 30) {
					qs.giveItems(TUSK_OF_WINDSUS, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case TYRANT, TYRANT_KINGPIN -> {
				if (lara == 4 && qs.getQuestItemsCount(TALONS_OF_TYRANT) < 30) {
					qs.giveItems(TALONS_OF_TYRANT, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case NOBLE_ANT, NOBLE_ANT_LEADER -> {
				if (lara == 5 && qs.getQuestItemsCount(WINGS_OF_DRONEANT) < 30) {
					qs.giveItems(WINGS_OF_DRONEANT, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case WYRM -> {
				if (lara == 5 && qs.getQuestItemsCount(FANGS_OF_WYRM) < 30) {
					qs.giveItems(FANGS_OF_WYRM, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case PAKO_THE_CAT -> {
				if (qs.getInt("Almors") == 2 || qs.getInt("Almors") == 3) {
					qs.set("Almors", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_1, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_1, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_1, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case UNICORN_RACER -> {
				if (qs.getInt("Camoniell") == 2 || qs.getInt("Camoniell") == 3) {
					qs.set("Camoniell", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_2, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_2, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_2, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case SHADOW_TUREN -> {
				if (qs.getInt("Belthus") == 2 || qs.getInt("Belthus") == 3) {
					qs.set("Belthus", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_3, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_3, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_3, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case MIMI_THE_CAT -> {
				if (qs.getInt("Basilla") == 2 || qs.getInt("Basilla") == 3) {
					qs.set("Basilla", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_4, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_4, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_4, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case UNICORN_PHANTASM -> {
				if (qs.getInt("Celestiel") == 2 || qs.getInt("Celestiel") == 3) {
					qs.set("Celestiel", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_5, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_5, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_5, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case SILHOUETTE_TILFO -> {
				if (qs.getInt("Brynthea") == 2 || qs.getInt("Brynthea") == 3) {
					qs.set("Brynthea", "6");
					qs.takeItems(CRYSTAL_OF_PROGRESS_6, -1);
					qs.takeItems(CRYSTAL_OF_INPROGRESS_6, -1);
					qs.giveItems(CRYSTAL_OF_VICTORY_6, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}
