package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 337: Audience with the Land Dragon (Acesso oficial ao Grand Boss Antharas).
 * Concede a lendaria Portal Stone (3865) necessaria para transpor o Heart of Warding no Covil de Antharas.
 */
@Component
public class Quest337AudienceWithLandDragon extends Quest {

	public static final int QUEST_ID = 337;
	public static final String QUEST_NAME = "337_AudienceWithLandDragon";

	// NPCs
	public static final int GABRIELLE = 30753;
	public static final int ORVEN = 30857;
	public static final int KENDRA = 30851;
	public static final int CHAKIRIS = 30705;
	public static final int KAIENA = 30720;
	public static final int MOKE = 30498;
	public static final int HELTON = 30678;
	public static final int GILMORE = 30754;
	public static final int THEODRIC = 30755;
	public static final int KLEIN = 31759;

	// Monstros
	public static final int BLOOD_QUEEN = 18001;
	public static final int SACRIFICE_OF_THE_SACRIFICED = 27171;
	public static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	public static final int HARIT_LIZARDMAN_MATRIARCH = 20645;
	public static final int HARIT_LIZARDMAN_ZEALOT = 27172;
	public static final int KRANROT = 20650;
	public static final int HAMRUT = 20649;
	public static final int MARSH_DRAKE = 20680;
	public static final int MARSH_STALKER = 20679;
	public static final int ABYSSAL_JEWEL_1 = 27165;
	public static final int JEWEL_GUARDIAN_MARA = 27168;
	public static final int ABYSSAL_JEWEL_2 = 27166;
	public static final int JEWEL_GUARDIAN_MUSFEL = 27169;
	public static final int CAVE_MAIDEN_1 = 20134;
	public static final int CAVE_MAIDEN = 20287;
	public static final int CAVE_KEEPER_1 = 20246;
	public static final int CAVE_KEEPER = 20277;
	public static final int MALRUK_KNIGHT = 20245;
	public static final int CAVE_MAIDEN_3 = 20234;
	public static final int GAMLIN = 20651;
	public static final int BLOOD_QUEEN_2 = 18002;
	public static final int ABYSSAL_JEWEL_3 = 27167;
	public static final int JEWEL_GUARDIAN_PYTON = 27170;

	// Itens
	public static final int FEATHER_OF_SEEKER = 3852;
	public static final int KRAKIAN_TOOTH = 3853;
	public static final int MARK_OF_WATCHMAN = 3864;
	public static final int REMAINS_OF_SACRIFICED = 3857;
	public static final int TOTEM_OF_LAND_DRAGON = 3858;
	public static final int KRANROT_SKIN = 3855;
	public static final int HAMRUT_LEG = 3856;
	public static final int MARSH_DRAKE_TALONS = 3854;
	public static final int MARSH_STALKER_HORN = 3853;
	public static final int FIRST_FRAGMENT_OF_ABYSS_JEWEL = 3859;
	public static final int MARA_FANG = 3862;
	public static final int SECOND_FRAGMENT_OF_ABYSS_JEWEL = 3860;
	public static final int MUSFEL_FANG = 3863;
	public static final int HERALD_OF_SLAYER = 3890;
	public static final int THIRD_FRAGMENT_OF_ABYSS_JEWEL = 3861;
	public static final int PORTAL_STONE = 3865;

	private static final int[][] DROPS_ON_KILL = {
		{SACRIFICE_OF_THE_SACRIFICED, 1, 1, REMAINS_OF_SACRIFICED},
		{HARIT_LIZARDMAN_ZEALOT, 1, 2, TOTEM_OF_LAND_DRAGON},
		{KRANROT, 1, 3, KRANROT_SKIN},
		{HAMRUT, 1, 3, HAMRUT_LEG},
		{MARSH_DRAKE, 1, 4, MARSH_DRAKE_TALONS},
		{MARSH_STALKER, 1, 4, MARSH_STALKER_HORN},
		{JEWEL_GUARDIAN_MARA, 2, 5, MARA_FANG},
		{JEWEL_GUARDIAN_MUSFEL, 2, 6, MUSFEL_FANG}
	};

	@Autowired
	public Quest337AudienceWithLandDragon(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Audience with the Land Dragon");

		addStartNpc(GABRIELLE);
		addTalkId(GABRIELLE, ORVEN, KENDRA, CHAKIRIS, KAIENA, MOKE, HELTON, GILMORE, THEODRIC, KLEIN);

		addAttackId(ABYSSAL_JEWEL_1, ABYSSAL_JEWEL_2, ABYSSAL_JEWEL_3);
		addKillId(
			BLOOD_QUEEN, BLOOD_QUEEN_2, SACRIFICE_OF_THE_SACRIFICED,
			HARIT_LIZARDMAN_SHAMAN, HARIT_LIZARDMAN_MATRIARCH, HARIT_LIZARDMAN_ZEALOT,
			KRANROT, HAMRUT, MARSH_DRAKE, MARSH_STALKER, GAMLIN,
			ABYSSAL_JEWEL_1, JEWEL_GUARDIAN_MARA,
			ABYSSAL_JEWEL_2, JEWEL_GUARDIAN_MUSFEL,
			CAVE_MAIDEN_1, CAVE_MAIDEN, CAVE_MAIDEN_3, CAVE_KEEPER_1, CAVE_KEEPER, MALRUK_KNIGHT,
			ABYSSAL_JEWEL_3, JEWEL_GUARDIAN_PYTON
		);

		registerQuestItems(
			FEATHER_OF_SEEKER, MARSH_STALKER_HORN, MARSH_DRAKE_TALONS, KRANROT_SKIN, HAMRUT_LEG,
			REMAINS_OF_SACRIFICED, TOTEM_OF_LAND_DRAGON, FIRST_FRAGMENT_OF_ABYSS_JEWEL,
			SECOND_FRAGMENT_OF_ABYSS_JEWEL, THIRD_FRAGMENT_OF_ABYSS_JEWEL, MARA_FANG,
			MUSFEL_FANG, MARK_OF_WATCHMAN, HERALD_OF_SLAYER
		);

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
		return handleEvent(event, qs);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) return null;
		return handleEvent(event, qs);
	}

	private String handleEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(FEATHER_OF_SEEKER, 1);
			return "gabrielle_started.htm";
		}

		if ("gabrielle_finish".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(FEATHER_OF_SEEKER, -1);
			qs.takeItems(KRAKIAN_TOOTH, -1);
			qs.giveItems(PORTAL_STONE, 1);
			qs.exitQuest(false);
			return "gabrielle_completed.htm";
		}

		if ("30753-05.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("drop1", "1");
			qs.set("drop2", "1");
			qs.set("drop3", "1");
			qs.set("drop4", "1");
			qs.giveItems(FEATHER_OF_SEEKER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30753-09.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MARK_OF_WATCHMAN, -1);
			qs.setCond(2);
			qs.set("drop5", "2");
			qs.set("drop6", "2");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30754-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.set("drop7", "4");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30755-05.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL)) {
				qs.takeItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL, -1);
				qs.takeItems(HERALD_OF_SLAYER, -1);
				qs.giveItems(PORTAL_STONE, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return event;
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) return "gabrielle_noquest.htm";

		PlayerCharacter character = player.activeChar();
		if (character == null) return null;
		return handleTalk(npc, qs, character);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) return "noquest";
		PlayerCharacter character = qs.playerChar();
		if (character == null) return "noquest";
		return handleTalk(npc, qs, character);
	}

	private String handleTalk(NpcInstance npc, QuestState qs, PlayerCharacter character) {
		int npcId = npc != null ? npc.npcId() : 0;
		int cond = qs.getCond();

		if (npcId == GABRIELLE) {
			if (qs.isCreated()) {
				if (character.level() >= 50) {
					return "gabrielle_intro.htm";
				} else {
					return "gabrielle_low_level.htm";
				}
			}
			if (cond == 1) {
				if (qs.getInt("drop1") > 0) {
					return qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 4 ? "30753-06.htm" : "30753-08.htm";
				}
				return "gabrielle_hunting.htm";
			}
			if (cond == 2) {
				if (qs.getInt("drop5") > 0) {
					if (qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 2) {
						return "30753-10.htm";
					}
					qs.takeItems(FEATHER_OF_SEEKER, -1);
					qs.takeItems(MARK_OF_WATCHMAN, -1);
					qs.giveItems(HERALD_OF_SLAYER, 1);
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30753-11.htm";
				}
				return "gabrielle_ready_to_finish.htm";
			}
			if (cond == 3) {
				return "30753-12.htm";
			}
			if (cond == 4) {
				return "30753-13.htm";
			}
			if (qs.isCompleted()) {
				return "gabrielle_already_done.htm";
			}
		} else if (npcId == ORVEN) {
			if (cond == 1) {
				if (qs.getInt("drop1") == 1) {
					if (qs.hasQuestItems(REMAINS_OF_SACRIFICED)) {
						qs.takeItems(REMAINS_OF_SACRIFICED, -1);
						qs.giveItems(MARK_OF_WATCHMAN, 1);
						qs.unset("drop1");
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "30857-02.htm";
					}
					return "30857-01.htm";
				}
				if (qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 4) {
					return "30857-03.htm";
				}
				return "30857-04.htm";
			}
		} else if (npcId == KENDRA) {
			if (cond == 1) {
				if (qs.getInt("drop2") == 1) {
					if (qs.hasQuestItems(TOTEM_OF_LAND_DRAGON)) {
						qs.takeItems(TOTEM_OF_LAND_DRAGON, -1);
						qs.giveItems(MARK_OF_WATCHMAN, 1);
						qs.unset("drop2");
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "30851-02.htm";
					}
					return "30851-01.htm";
				}
				if (qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 4) {
					return "30851-03.htm";
				}
				return "30851-04.htm";
			}
		} else if (npcId == CHAKIRIS) {
			if (cond == 1) {
				if (qs.getInt("drop3") == 1) {
					if (qs.hasQuestItems(KRANROT_SKIN) && qs.hasQuestItems(HAMRUT_LEG)) {
						qs.takeItems(KRANROT_SKIN, -1);
						qs.takeItems(HAMRUT_LEG, -1);
						qs.giveItems(MARK_OF_WATCHMAN, 1);
						qs.unset("drop3");
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "30705-02.htm";
					}
					return "30705-01.htm";
				}
				if (qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 4) {
					return "30705-03.htm";
				}
				return "30705-04.htm";
			}
		} else if (npcId == KAIENA) {
			if (cond == 1) {
				if (qs.getInt("drop4") == 1) {
					if (qs.hasQuestItems(MARSH_DRAKE_TALONS) && qs.hasQuestItems(MARSH_STALKER_HORN)) {
						qs.takeItems(MARSH_DRAKE_TALONS, -1);
						qs.takeItems(MARSH_STALKER_HORN, -1);
						qs.giveItems(MARK_OF_WATCHMAN, 1);
						qs.unset("drop4");
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "30720-02.htm";
					}
					return "30720-01.htm";
				}
				if (qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 4) {
					return "30720-03.htm";
				}
				return "30720-04.htm";
			}
		} else if (npcId == MOKE) {
			if (cond == 2) {
				switch (qs.getInt("drop5")) {
					case 2:
						qs.set("drop5", "1");
						return "30498-01.htm";
					case 1:
						if (qs.hasQuestItems(FIRST_FRAGMENT_OF_ABYSS_JEWEL) && qs.hasQuestItems(MARA_FANG)) {
							qs.takeItems(FIRST_FRAGMENT_OF_ABYSS_JEWEL, -1);
							qs.takeItems(MARA_FANG, -1);
							qs.giveItems(MARK_OF_WATCHMAN, 1);
							qs.unset("drop5");
							qs.playSound(QuestState.SOUND_MIDDLE);
							return "30498-03.htm";
						}
						return "30498-02.htm";
					case 0:
						return qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 2 ? "30498-04.htm" : "30498-05.htm";
				}
			}
		} else if (npcId == HELTON) {
			if (cond == 2) {
				switch (qs.getInt("drop6")) {
					case 2:
						qs.set("drop6", "1");
						return "30678-01.htm";
					case 1:
						if (qs.hasQuestItems(SECOND_FRAGMENT_OF_ABYSS_JEWEL) && qs.hasQuestItems(MUSFEL_FANG)) {
							qs.takeItems(SECOND_FRAGMENT_OF_ABYSS_JEWEL, -1);
							qs.takeItems(MUSFEL_FANG, -1);
							qs.giveItems(MARK_OF_WATCHMAN, 1);
							qs.unset("drop6");
							qs.playSound(QuestState.SOUND_MIDDLE);
							return "30678-03.htm";
						}
						return "30678-02.htm";
					case 0:
						return qs.getQuestItemsCount(MARK_OF_WATCHMAN) < 2 ? "30678-04.htm" : "30678-05.htm";
				}
			}
		} else if (npcId == GILMORE) {
			if (cond == 1) {
				return "gilmore_clues.htm";
			}
			if (cond == 2) {
				return "30754-01.htm";
			}
			if (cond == 3) {
				return "30754-02.htm";
			}
			if (cond == 4) {
				return !qs.hasQuestItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL) ? "30754-04.htm" : "30754-05.htm";
			}
		} else if (npcId == THEODRIC) {
			if (qs.isCompleted() || qs.hasQuestItems(PORTAL_STONE)) {
				return "theodric_enter.htm";
			}
			if (cond < 3) {
				return "theodric_no_stone.htm";
			}
			if (cond == 3) {
				return "30755-02.htm";
			}
			if (cond == 4) {
				return !qs.hasQuestItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL) ? "30755-03.htm" : "30755-04.htm";
			}
			return "theodric_no_stone.htm";
		} else if (npcId == KLEIN) {
			return "30755-01.htm";
		}

		return null;
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) return null;
		int npcId = npc != null ? npc.npcId() : 0;
		if (npcId == ABYSSAL_JEWEL_1 && qs.getCond() == 2 && qs.getInt("drop5") == 1 && !qs.hasQuestItems(FIRST_FRAGMENT_OF_ABYSS_JEWEL)) {
			qs.giveItems(FIRST_FRAGMENT_OF_ABYSS_JEWEL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == ABYSSAL_JEWEL_2 && qs.getCond() == 2 && qs.getInt("drop6") == 1 && !qs.hasQuestItems(SECOND_FRAGMENT_OF_ABYSS_JEWEL)) {
			qs.giveItems(SECOND_FRAGMENT_OF_ABYSS_JEWEL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == ABYSSAL_JEWEL_3 && qs.getCond() == 4 && qs.getInt("drop7") == 4 && !qs.hasQuestItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL)) {
			qs.giveItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null || !qs.isStarted()) return null;
		return handleKill(npc, qs);
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) return null;
		return handleKill(npc, qs);
	}

	private String handleKill(NpcInstance npc, QuestState qs) {
		int npcId = npc != null ? npc.npcId() : 0;
		int cond = qs.getCond();

		if (cond == 1 && qs.getInt("drop1") == 0 && (npcId == CAVE_KEEPER || npcId == CAVE_MAIDEN || npcId == MALRUK_KNIGHT)) {
			long count = qs.getQuestItemsCount(KRAKIAN_TOOTH);
			if (count < 10) {
				qs.giveItems(KRAKIAN_TOOTH, 1);
				if (qs.getQuestItemsCount(KRAKIAN_TOOTH) >= 10) {
					qs.setCond(2);
				}
			}
			return null;
		}

		for (int[] drop : DROPS_ON_KILL) {
			if (drop[0] == npcId) {
				if (drop[1] == cond && qs.getInt("drop" + drop[2]) == 1 && !qs.hasQuestItems(drop[3])) {
					qs.giveItems(drop[3], 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return null;
			}
		}

		if (cond == 1) {
			if ((npcId == BLOOD_QUEEN || npcId == BLOOD_QUEEN_2) && qs.getInt("drop1") == 1 && !qs.hasQuestItems(REMAINS_OF_SACRIFICED)) {
				qs.addSpawn(SACRIFICE_OF_THE_SACRIFICED);
			} else if ((npcId == HARIT_LIZARDMAN_SHAMAN || npcId == HARIT_LIZARDMAN_MATRIARCH) && qs.getInt("drop2") == 1 && !qs.hasQuestItems(TOTEM_OF_LAND_DRAGON)) {
				if (ThreadLocalRandom.current().nextInt(5) == 0) {
					qs.addSpawn(HARIT_LIZARDMAN_ZEALOT);
				}
			}
		} else if (cond == 4 && qs.getInt("drop7") == 4 && !qs.hasQuestItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL)) {
			if (npcId == CAVE_KEEPER || npcId == CAVE_KEEPER_1 || npcId == CAVE_MAIDEN || npcId == CAVE_MAIDEN_1 || npcId == CAVE_MAIDEN_3) {
				if (ThreadLocalRandom.current().nextInt(5) == 0) {
					qs.addSpawn(ABYSSAL_JEWEL_3);
				}
			} else if (npcId == ABYSSAL_JEWEL_3) {
				qs.giveItems(THIRD_FRAGMENT_OF_ABYSS_JEWEL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
