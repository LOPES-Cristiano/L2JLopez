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
 * Quest 221: Testimony of Prosperity
 * 2º Passo de 2ª Classe para todas as classes da raça Dwarf (Bounty Hunter, Warsmith).
 */
@Component
public class Quest221TestimonyOfProsperity extends Quest {

	public static final int QUEST_ID = 221;
	public static final String QUEST_NAME = "221_TestimonyOfProsperity";

	// NPCs
	public static final int WILFORD = 30005;
	public static final int PARMAN = 30104;
	public static final int LILITH = 30368;
	public static final int BRIGHT = 30466;
	public static final int SHARI = 30517;
	public static final int MION = 30519;
	public static final int LOCKIRIN = 30531;
	public static final int SPIRON = 30532;
	public static final int BALANKI = 30533;
	public static final int KEEF = 30534;
	public static final int FILAUR = 30535;
	public static final int ARIN = 30536;
	public static final int MARYSE_REDBONNET = 30553;
	public static final int BOLTER = 30554;
	public static final int TOROCCO = 30555;
	public static final int TOMA = 30556;
	public static final int PIOTUR = 30597;
	public static final int EMILY = 30620;
	public static final int NIKOLA = 30621;
	public static final int BOX_OF_TITAN = 30622;

	// Monstros
	public static final int MANDRAGORA_SPROUT_1 = 20223;
	public static final int MANDRAGORA_SPROUT_2 = 20154;
	public static final int MANDRAGORA_SAPLING = 20155;
	public static final int MANDRAGORA_BLOSSOM = 20156;
	public static final int MARSH_STAKATO = 20157;
	public static final int GIANT_CRIMSON_ANT = 20228;
	public static final int MARSH_STAKATO_WORKER = 20230;
	public static final int TOAD_LORD = 20231;
	public static final int MARSH_STAKATO_SOLDIER = 20232;
	public static final int MARSH_SPIDER = 20233;
	public static final int MARSH_STAKATO_DRONE = 20234;

	// Itens
	public static final int ADENA = 57;
	public static final int ANIMAL_SKIN = 1867;
	public static final int RECIPE_TITAN_KEY = 3023;
	public static final int KEY_OF_TITAN = 3030;
	public static final int MARK_OF_PROSPERITY = 3238;
	public static final int RING_OF_TESTIMONY1 = 3239;
	public static final int RING_OF_TESTIMONY2 = 3240;
	public static final int OLD_ACCOUNT_BOOK = 3241;
	public static final int BLESSED_SEED = 3242;
	public static final int RECIPE_OF_EMILLY = 3243;
	public static final int LILITH_ELVEN_WAFER = 3244;
	public static final int MAPHR_TABLET_FRAGMENT = 3245;
	public static final int COLLECTION_LICENSE = 3246;
	public static final int LOCKIRIN_NOTICE_1 = 3247;
	public static final int LOCKIRIN_NOTICE_2 = 3248;
	public static final int LOCKIRIN_NOTICE_3 = 3249;
	public static final int LOCKIRIN_NOTICE_4 = 3250;
	public static final int LOCKIRIN_NOTICE_5 = 3251;
	public static final int CONTRIBUTION_OF_SHARI = 3252;
	public static final int CONTRIBUTION_OF_MION = 3253;
	public static final int CONTRIBUTION_OF_MARYSE = 3254;
	public static final int MARYSE_REQUEST = 3255;
	public static final int CONTRIBUTION_OF_TOMA = 3256;
	public static final int RECEIPT_OF_BOLTER = 3257;
	public static final int RECEIPT_OF_CONTRIBUTION_1 = 3258;
	public static final int RECEIPT_OF_CONTRIBUTION_2 = 3259;
	public static final int RECEIPT_OF_CONTRIBUTION_3 = 3260;
	public static final int RECEIPT_OF_CONTRIBUTION_4 = 3261;
	public static final int RECEIPT_OF_CONTRIBUTION_5 = 3262;
	public static final int PROCURATION_OF_TOROCCO = 3263;
	public static final int BRIGHT_LIST = 3264;
	public static final int MANDRAGORA_PETAL = 3265;
	public static final int CRIMSON_MOSS = 3266;
	public static final int MANDRAGORA_BOUQUET = 3267;
	public static final int PARMAN_INSTRUCTIONS = 3268;
	public static final int PARMANS_LETTER = 3269;
	public static final int CLAY_DOUGH = 3270;
	public static final int PATTERN_OF_KEYHOLE = 3271;
	public static final int NIKOLAS_LIST = 3272;
	public static final int STAKATO_SHELL = 3273;
	public static final int TOAD_LORD_SAC = 3274;
	public static final int SPIDER_THORN = 3275;
	public static final int CRYSTAL_BROOCH = 3428;

	private static final Set<Integer> VALID_CLASSES = Set.of(54, 56);

	@Autowired
	public Quest221TestimonyOfProsperity(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Prosperity");

		addStartNpc(PARMAN);
		addTalkId(WILFORD, PARMAN, LILITH, BRIGHT, SHARI, MION, LOCKIRIN, SPIRON, BALANKI, KEEF, FILAUR, ARIN,
				MARYSE_REDBONNET, BOLTER, TOROCCO, TOMA, PIOTUR, EMILY, NIKOLA, BOX_OF_TITAN);

		addKillId(MANDRAGORA_SPROUT_1, MANDRAGORA_SPROUT_2, MANDRAGORA_SAPLING, MANDRAGORA_BLOSSOM,
				MARSH_STAKATO, GIANT_CRIMSON_ANT, MARSH_STAKATO_WORKER, TOAD_LORD,
				MARSH_STAKATO_SOLDIER, MARSH_SPIDER, MARSH_STAKATO_DRONE);

		registerQuestItems(RING_OF_TESTIMONY1, RING_OF_TESTIMONY2,
				OLD_ACCOUNT_BOOK, BLESSED_SEED, RECIPE_OF_EMILLY, LILITH_ELVEN_WAFER, MAPHR_TABLET_FRAGMENT,
				COLLECTION_LICENSE, LOCKIRIN_NOTICE_1, LOCKIRIN_NOTICE_2, LOCKIRIN_NOTICE_3,
				LOCKIRIN_NOTICE_4, LOCKIRIN_NOTICE_5, CONTRIBUTION_OF_SHARI, CONTRIBUTION_OF_MION,
				CONTRIBUTION_OF_MARYSE, MARYSE_REQUEST, CONTRIBUTION_OF_TOMA, RECEIPT_OF_BOLTER,
				RECEIPT_OF_CONTRIBUTION_1, RECEIPT_OF_CONTRIBUTION_2, RECEIPT_OF_CONTRIBUTION_3,
				RECEIPT_OF_CONTRIBUTION_4, RECEIPT_OF_CONTRIBUTION_5, PROCURATION_OF_TOROCCO,
				BRIGHT_LIST, MANDRAGORA_PETAL, CRIMSON_MOSS, MANDRAGORA_BOUQUET, PARMAN_INSTRUCTIONS,
				PARMANS_LETTER, CLAY_DOUGH, PATTERN_OF_KEYHOLE, NIKOLAS_LIST, STAKATO_SHELL,
				TOAD_LORD_SAC, SPIDER_THORN, CRYSTAL_BROOCH);

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

		if ("1".equalsIgnoreCase(event) || "30104-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(RING_OF_TESTIMONY1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30104-04.htm";
		} else if ("30104_1".equalsIgnoreCase(event) || "30104-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BLESSED_SEED, -1);
			qs.takeItems(RECIPE_OF_EMILLY, -1);
			qs.takeItems(LILITH_ELVEN_WAFER, -1);
			qs.takeItems(OLD_ACCOUNT_BOOK, -1);
			qs.takeItems(RING_OF_TESTIMONY1, -1);
			if ("30104-07.htm".equalsIgnoreCase(event) && player != null && player.level() < 38) {
				qs.giveItems(PARMAN_INSTRUCTIONS, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30104-07.htm";
			}
			qs.giveItems(RING_OF_TESTIMONY2, 1);
			qs.giveItems(PARMANS_LETTER, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30104-08.htm";
		} else if ("30531-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(COLLECTION_LICENSE) ? "30531-04.htm" : "30531-02.htm";
		} else if ("30531-03.htm".equalsIgnoreCase(event)) {
			qs.giveItems(COLLECTION_LICENSE, 1);
			qs.giveItems(LOCKIRIN_NOTICE_1, 1);
			qs.giveItems(LOCKIRIN_NOTICE_2, 1);
			qs.giveItems(LOCKIRIN_NOTICE_3, 1);
			qs.giveItems(LOCKIRIN_NOTICE_4, 1);
			qs.giveItems(LOCKIRIN_NOTICE_5, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30531-03.htm";
		} else if ("30534-03a.htm".equalsIgnoreCase(event) && qs.getQuestItemsCount(ADENA) >= 5000) {
			qs.takeItems(ADENA, 5000);
			qs.takeItems(PROCURATION_OF_TOROCCO, -1);
			qs.giveItems(RECEIPT_OF_CONTRIBUTION_3, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30534-03b.htm";
		} else if ("30005-04.htm".equalsIgnoreCase(event)) {
			qs.giveItems(CRYSTAL_BROOCH, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30005-04.htm";
		} else if ("30466-03.htm".equalsIgnoreCase(event)) {
			qs.giveItems(BRIGHT_LIST, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30466-03.htm";
		} else if ("30555-02.htm".equalsIgnoreCase(event)) {
			qs.giveItems(PROCURATION_OF_TOROCCO, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30555-02.htm";
		} else if ("30368-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CRYSTAL_BROOCH, -1);
			qs.giveItems(LILITH_ELVEN_WAFER, 1);
			checkFirstPart(qs);
			return "30368-03.htm";
		} else if ("30597-02.htm".equalsIgnoreCase(event)) {
			qs.giveItems(BLESSED_SEED, 1);
			checkFirstPart(qs);
			return "30597-02.htm";
		} else if ("30620-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MANDRAGORA_BOUQUET, -1);
			qs.giveItems(RECIPE_OF_EMILLY, 1);
			checkFirstPart(qs);
			return "30620-03.htm";
		} else if ("30621-04.htm".equalsIgnoreCase(event)) {
			qs.giveItems(CLAY_DOUGH, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30621-04.htm";
		} else if ("30622-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CLAY_DOUGH, -1);
			qs.giveItems(PATTERN_OF_KEYHOLE, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30622-02.htm";
		} else if ("30622-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KEY_OF_TITAN, -1);
			qs.takeItems(NIKOLAS_LIST, -1);
			qs.takeItems(RECIPE_TITAN_KEY, -1);
			qs.takeItems(STAKATO_SHELL, -1);
			qs.takeItems(SPIDER_THORN, -1);
			qs.takeItems(TOAD_LORD_SAC, -1);
			qs.giveItems(MAPHR_TABLET_FRAGMENT, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30622-04.htm";
		} else if ("30104-12.htm".equalsIgnoreCase(event) || "30104-13.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MAPHR_TABLET_FRAGMENT)) {
				qs.takeItems(MAPHR_TABLET_FRAGMENT, -1);
				qs.takeItems(RING_OF_TESTIMONY2, -1);
				qs.giveItems(MARK_OF_PROSPERITY, 1);
				qs.addExpAndSp(113249, 7800);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private void checkFirstPart(QuestState qs) {
		if (qs.hasQuestItems(BLESSED_SEED) && qs.hasQuestItems(OLD_ACCOUNT_BOOK)
				&& qs.hasQuestItems(RECIPE_OF_EMILLY) && qs.hasQuestItems(LILITH_ELVEN_WAFER)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == PARMAN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30104-03.htm";
				} else {
					return "30104-01.htm";
				}
			} else if (cond == 1) {
				return "30104-05.htm";
			} else if (cond == 2) {
				return "30104-06.htm";
			} else if (cond == 3) {
				if (player != null && player.level() < 38) {
					return "30104-09.htm";
				}
				qs.takeItems(PARMAN_INSTRUCTIONS, -1);
				qs.giveItems(PARMANS_LETTER, 1);
				qs.giveItems(RING_OF_TESTIMONY2, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30104-10.htm";
			} else if (cond > 3 && cond < 7) {
				if (cond == 6 && qs.hasQuestItems(MAPHR_TABLET_FRAGMENT)) {
					return "30104-11.htm";
				}
				return "30104-11.htm";
			} else if (cond == 7 || cond == 8) {
				return "30104-12.htm";
			} else if (cond == 9 && qs.hasQuestItems(MAPHR_TABLET_FRAGMENT)) {
				qs.takeItems(MAPHR_TABLET_FRAGMENT, -1);
				qs.takeItems(RING_OF_TESTIMONY2, -1);
				qs.giveItems(MARK_OF_PROSPERITY, 1);
				qs.addExpAndSp(113249, 7800);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30104-13.htm";
			}
		} else if (npcId == LOCKIRIN) {
			if (cond == 1 || cond == 2) {
				if (qs.hasQuestItems(COLLECTION_LICENSE)) {
					if (qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_1) && qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_2)
							&& qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_3) && qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_4)
							&& qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_5)) {
						qs.takeItems(COLLECTION_LICENSE, -1);
						qs.takeItems(RECEIPT_OF_CONTRIBUTION_1, -1);
						qs.takeItems(RECEIPT_OF_CONTRIBUTION_2, -1);
						qs.takeItems(RECEIPT_OF_CONTRIBUTION_3, -1);
						qs.takeItems(RECEIPT_OF_CONTRIBUTION_4, -1);
						qs.takeItems(RECEIPT_OF_CONTRIBUTION_5, -1);
						qs.giveItems(OLD_ACCOUNT_BOOK, 1);
						checkFirstPart(qs);
						return "30531-05.htm";
					}
					return "30531-04.htm";
				}
				return qs.hasQuestItems(OLD_ACCOUNT_BOOK) ? "30531-06.htm" : "30531-01.htm";
			} else if (cond >= 4) {
				return "30531-07.htm";
			}
		} else if (npcId == SPIRON) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(LOCKIRIN_NOTICE_1)) {
					qs.takeItems(LOCKIRIN_NOTICE_1, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30532-01.htm";
				} else if (qs.hasQuestItems(CONTRIBUTION_OF_SHARI)) {
					qs.takeItems(CONTRIBUTION_OF_SHARI, -1);
					qs.giveItems(RECEIPT_OF_CONTRIBUTION_1, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30532-03.htm";
				}
				return qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_1) ? "30532-04.htm" : "30532-02.htm";
			}
		} else if (npcId == BALANKI) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(LOCKIRIN_NOTICE_2)) {
					qs.takeItems(LOCKIRIN_NOTICE_2, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30533-01.htm";
				} else if (qs.hasQuestItems(CONTRIBUTION_OF_MARYSE) && qs.hasQuestItems(CONTRIBUTION_OF_MION)) {
					qs.takeItems(CONTRIBUTION_OF_MARYSE, -1);
					qs.takeItems(CONTRIBUTION_OF_MION, -1);
					qs.giveItems(RECEIPT_OF_CONTRIBUTION_2, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30533-03.htm";
				}
				return qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_2) ? "30533-04.htm" : "30533-02.htm";
			}
		} else if (npcId == KEEF) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(LOCKIRIN_NOTICE_3)) {
					qs.takeItems(LOCKIRIN_NOTICE_3, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30534-01.htm";
				} else if (qs.hasQuestItems(PROCURATION_OF_TOROCCO)) {
					return "30534-03.htm";
				}
				return qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_3) ? "30534-04.htm" : "30534-02.htm";
			}
		} else if (npcId == FILAUR) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(LOCKIRIN_NOTICE_4)) {
					qs.takeItems(LOCKIRIN_NOTICE_4, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30535-01.htm";
				} else if (qs.hasQuestItems(RECEIPT_OF_BOLTER)) {
					qs.takeItems(RECEIPT_OF_BOLTER, -1);
					qs.giveItems(RECEIPT_OF_CONTRIBUTION_4, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30535-03.htm";
				}
				return qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_4) ? "30535-04.htm" : "30535-02.htm";
			}
		} else if (npcId == ARIN) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(LOCKIRIN_NOTICE_5)) {
					qs.takeItems(LOCKIRIN_NOTICE_5, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30536-01.htm";
				} else if (qs.hasQuestItems(CONTRIBUTION_OF_TOMA)) {
					qs.takeItems(CONTRIBUTION_OF_TOMA, -1);
					qs.giveItems(RECEIPT_OF_CONTRIBUTION_5, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30536-03.htm";
				}
				return qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_5) ? "30536-04.htm" : "30536-02.htm";
			}
		} else if (npcId == SHARI) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(CONTRIBUTION_OF_SHARI)) {
					return "30517-02.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_1) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_1)) {
					qs.giveItems(CONTRIBUTION_OF_SHARI, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30517-01.htm";
				}
			}
		} else if (npcId == MION) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(CONTRIBUTION_OF_MION)) {
					return "30519-02.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_2) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_2)) {
					qs.giveItems(CONTRIBUTION_OF_MION, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30519-01.htm";
				}
			}
		} else if (npcId == MARYSE_REDBONNET) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(MARYSE_REQUEST)) {
					if (qs.getQuestItemsCount(ANIMAL_SKIN) < 100) {
						return "30553-02.htm";
					}
					qs.takeItems(ANIMAL_SKIN, 100);
					qs.takeItems(MARYSE_REQUEST, -1);
					qs.giveItems(CONTRIBUTION_OF_MARYSE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30553-03.htm";
				} else if (qs.hasQuestItems(CONTRIBUTION_OF_MARYSE)) {
					return "30553-04.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_2) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_2)) {
					qs.giveItems(MARYSE_REQUEST, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30553-01.htm";
				}
			}
		} else if (npcId == TOROCCO) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(PROCURATION_OF_TOROCCO)) {
					return "30555-03.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_3) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_3)) {
					return "30555-01.htm";
				}
			}
		} else if (npcId == BOLTER) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(RECEIPT_OF_BOLTER)) {
					return "30554-02.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_4) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_4)) {
					qs.giveItems(RECEIPT_OF_BOLTER, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30554-01.htm";
				}
			}
		} else if (npcId == TOMA) {
			if (cond == 1 && qs.hasQuestItems(COLLECTION_LICENSE)) {
				if (qs.hasQuestItems(CONTRIBUTION_OF_TOMA)) {
					return "30556-02.htm";
				} else if (!qs.hasQuestItems(LOCKIRIN_NOTICE_5) && !qs.hasQuestItems(RECEIPT_OF_CONTRIBUTION_5)) {
					qs.giveItems(CONTRIBUTION_OF_TOMA, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30556-01.htm";
				}
			}
		} else if (npcId == PIOTUR) {
			if (cond == 1 || cond == 2) {
				return qs.hasQuestItems(BLESSED_SEED) ? "30597-03.htm" : "30597-01.htm";
			} else if (cond >= 4) {
				return "30597-04.htm";
			}
		} else if (npcId == WILFORD) {
			if (cond == 1 || cond == 2) {
				if (qs.hasQuestItems(LILITH_ELVEN_WAFER)) {
					return "30005-06.htm";
				}
				return qs.hasQuestItems(CRYSTAL_BROOCH) ? "30005-05.htm" : "30005-01.htm";
			} else if (cond >= 4) {
				return "30005-07.htm";
			}
		} else if (npcId == LILITH) {
			if (cond == 1 || cond == 2) {
				if (qs.hasQuestItems(CRYSTAL_BROOCH)) {
					return "30368-01.htm";
				} else if (qs.hasQuestItems(LILITH_ELVEN_WAFER)) {
					return "30368-04.htm";
				}
			} else if (cond >= 4) {
				return "30368-05.htm";
			}
		} else if (npcId == BRIGHT) {
			if (cond == 1 || cond == 2) {
				if (qs.hasQuestItems(RECIPE_OF_EMILLY)) {
					return "30466-07.htm";
				} else if (qs.hasQuestItems(MANDRAGORA_BOUQUET)) {
					return "30466-06.htm";
				} else if (qs.hasQuestItems(BRIGHT_LIST)) {
					if (qs.getQuestItemsCount(CRIMSON_MOSS) >= 10 && qs.getQuestItemsCount(MANDRAGORA_PETAL) >= 20) {
						qs.takeItems(BRIGHT_LIST, -1);
						qs.takeItems(CRIMSON_MOSS, -1);
						qs.takeItems(MANDRAGORA_PETAL, -1);
						qs.giveItems(MANDRAGORA_BOUQUET, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
						return "30466-05.htm";
					}
					return "30466-04.htm";
				}
				return "30466-01.htm";
			} else if (cond >= 4) {
				return "30466-08.htm";
			}
		} else if (npcId == EMILY) {
			if (cond == 1 || cond == 2) {
				if (qs.hasQuestItems(RECIPE_OF_EMILLY)) {
					return "30620-04.htm";
				} else if (qs.hasQuestItems(MANDRAGORA_BOUQUET)) {
					return "30620-01.htm";
				}
			} else if (cond >= 4) {
				return "30620-05.htm";
			}
		} else if (npcId == NIKOLA) {
			if (cond == 4) {
				qs.takeItems(PARMANS_LETTER, -1);
				qs.giveItems(CLAY_DOUGH, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30621-01.htm";
			} else if (cond == 5) {
				qs.takeItems(CLAY_DOUGH, -1);
				qs.giveItems(MAPHR_TABLET_FRAGMENT, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30621-04.htm";
			} else if (cond == 6) {
				qs.takeItems(PATTERN_OF_KEYHOLE, -1);
				qs.giveItems(NIKOLAS_LIST, 1);
				qs.giveItems(RECIPE_TITAN_KEY, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30621-06.htm";
			} else if (cond == 7 || cond == 8) {
				return qs.hasQuestItems(KEY_OF_TITAN) ? "30621-08.htm" : "30621-07.htm";
			} else if (cond == 9) {
				return "30621-09.htm";
			}
		} else if (npcId == BOX_OF_TITAN) {
			if (cond == 5) {
				return "30622-01.htm";
			} else if (cond == 8 && qs.hasQuestItems(KEY_OF_TITAN)) {
				return "30622-03.htm";
			}
			return "30622-05.htm";
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

		if (qs.hasQuestItems(BRIGHT_LIST)) {
			if ((npcId == MANDRAGORA_SPROUT_1 || npcId == MANDRAGORA_SPROUT_2 || npcId == MANDRAGORA_SAPLING
					|| npcId == MANDRAGORA_BLOSSOM) && qs.getQuestItemsCount(MANDRAGORA_PETAL) < 20) {
				qs.giveItems(MANDRAGORA_PETAL, 1);
				qs.playSound(qs.getQuestItemsCount(MANDRAGORA_PETAL) >= 20 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			} else if (npcId == GIANT_CRIMSON_ANT && qs.getQuestItemsCount(CRIMSON_MOSS) < 10) {
				qs.giveItems(CRIMSON_MOSS, 1);
				qs.playSound(qs.getQuestItemsCount(CRIMSON_MOSS) >= 10 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 7) {
			if ((npcId == MARSH_STAKATO || npcId == MARSH_STAKATO_WORKER || npcId == MARSH_STAKATO_SOLDIER
					|| npcId == MARSH_STAKATO_DRONE) && qs.getQuestItemsCount(STAKATO_SHELL) < 20) {
				qs.giveItems(STAKATO_SHELL, 1);
			} else if (npcId == TOAD_LORD && qs.getQuestItemsCount(TOAD_LORD_SAC) < 10) {
				qs.giveItems(TOAD_LORD_SAC, 1);
			} else if (npcId == MARSH_SPIDER && qs.getQuestItemsCount(SPIDER_THORN) < 10) {
				qs.giveItems(SPIDER_THORN, 1);
			}

			if (qs.getQuestItemsCount(STAKATO_SHELL) >= 20
					&& qs.getQuestItemsCount(TOAD_LORD_SAC) >= 10
					&& qs.getQuestItemsCount(SPIDER_THORN) >= 10) {
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
