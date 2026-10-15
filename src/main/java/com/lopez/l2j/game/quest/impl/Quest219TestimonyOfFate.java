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
 * Quest 219: Testimony of Fate
 * 2º Passo de 2ª Classe para todas as classes da raça Dark Elf (Palus Knight, Assassin, Dark Wizard, Shillien Oracle).
 */
@Component
public class Quest219TestimonyOfFate extends Quest {

	public static final int QUEST_ID = 219;
	public static final String QUEST_NAME = "219_TestimonyOfFate";

	// NPCs
	public static final int KAIRA = 30476;
	public static final int METHEUS = 30614;
	public static final int IXIA = 30463;
	public static final int ALDERS_SPIRIT = 30613;
	public static final int ROA = 30114;
	public static final int NORMAN = 30210;
	public static final int THIFIEL = 30358;
	public static final int ARKENIA = 30419;
	public static final int BLOODY_PIXY = 31845;
	public static final int BLIGHT_TREANT = 31850;

	// Monstros
	public static final int HANGMAN_TREE = 20144;
	public static final int MARSH_STAKATO = 20157;
	public static final int MEDUSA = 20158;
	public static final int TYRANT = 20192;
	public static final int TYRANT_KINGPIN = 20193;
	public static final int DEAD_SEEKER = 20202;
	public static final int MARSH_STAKATO_WORKER = 20230;
	public static final int MARSH_STAKATO_SOLDIER = 20232;
	public static final int MARSH_SPIDER = 20233;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int GRANDIS = 20554;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int KARUL_BUGBEAR = 20600;
	public static final int BLACK_WILLOW_LURKER = 27079;

	// Itens
	public static final int MARK_OF_FATE = 3172;
	public static final int KAIRAS_LETTER1 = 3173;
	public static final int METHEUS_FUNERAL_JAR = 3174;
	public static final int KASANDRAS_REMAINS = 3175;
	public static final int HERBALISM_TEXTBOOK = 3176;
	public static final int IXIAS_LIST = 3177;
	public static final int MEDUSA_ICHOR = 3178;
	public static final int MARSH_SPIDER_FLUIDS = 3179;
	public static final int DEAD_SEEKER_DUNG = 3180;
	public static final int TYRANT_BLOOD = 3181;
	public static final int NIGHTSHADE_ROOT = 3182;
	public static final int BELLADONNA = 3183;
	public static final int ALDERS_SKULL1 = 3184;
	public static final int ALDERS_SKULL2 = 3185;
	public static final int ALDERS_RECEIPT = 3186;
	public static final int REVELATIONS_MANUSCRIPT = 3187;
	public static final int KAIRAS_INSTRUCTIONS = 3188;
	public static final int KAIRAS_RECOMMEND = 3189;
	public static final int PALUS_CHARM = 3190;
	public static final int THIFIELS_LETTER = 3191;
	public static final int ARKENIAS_NOTE = 3192;
	public static final int PIXY_GARNET = 3193;
	public static final int GRANDIS_SKULL = 3194;
	public static final int KARUL_BUGBEAR_SKULL = 3195;
	public static final int BREKA_OVERLORD_SKULL = 3196;
	public static final int LETO_OVERLORD_SKULL = 3197;
	public static final int RED_FAIRY_DUST = 3198;
	public static final int BLIGHT_TREANT_SEED = 3199;
	public static final int BLACK_WILLOW_LEAF = 3200;
	public static final int BLIGHT_TREANT_SAP = 3201;
	public static final int ARKENIAS_LETTER = 1246;
	public static final int ARKENIAS_LETTER_RETAIL = 3202;

	private static final Set<Integer> VALID_CLASSES = Set.of(32, 35, 39, 42);

	@Autowired
	public Quest219TestimonyOfFate(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Fate");

		addStartNpc(KAIRA);
		addTalkId(KAIRA, METHEUS, IXIA, ALDERS_SPIRIT, ROA, NORMAN, THIFIEL, ARKENIA, BLOODY_PIXY, BLIGHT_TREANT);

		addKillId(HANGMAN_TREE, MARSH_STAKATO, MEDUSA, TYRANT, TYRANT_KINGPIN, DEAD_SEEKER,
				MARSH_STAKATO_WORKER, MARSH_STAKATO_SOLDIER, MARSH_SPIDER, MARSH_STAKATO_DRONE,
				BREKA_ORC_OVERLORD, GRANDIS, LETO_LIZARDMAN_OVERLORD, KARUL_BUGBEAR, BLACK_WILLOW_LURKER);

		registerQuestItems(KAIRAS_LETTER1, METHEUS_FUNERAL_JAR, KASANDRAS_REMAINS, HERBALISM_TEXTBOOK,
				IXIAS_LIST, MEDUSA_ICHOR, MARSH_SPIDER_FLUIDS, DEAD_SEEKER_DUNG, TYRANT_BLOOD, NIGHTSHADE_ROOT,
				BELLADONNA, ALDERS_SKULL1, ALDERS_SKULL2, ALDERS_RECEIPT, REVELATIONS_MANUSCRIPT,
				KAIRAS_INSTRUCTIONS, KAIRAS_RECOMMEND, PALUS_CHARM, THIFIELS_LETTER, ARKENIAS_NOTE,
				PIXY_GARNET, GRANDIS_SKULL, KARUL_BUGBEAR_SKULL, BREKA_OVERLORD_SKULL, LETO_OVERLORD_SKULL,
				RED_FAIRY_DUST, BLIGHT_TREANT_SEED, BLACK_WILLOW_LEAF, BLIGHT_TREANT_SAP,
				ARKENIAS_LETTER, ARKENIAS_LETTER_RETAIL);

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

		if ("1".equalsIgnoreCase(event) || "30476-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(KAIRAS_LETTER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30476-05.htm";
		} else if ("30614-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KAIRAS_LETTER1, -1);
			qs.giveItems(METHEUS_FUNERAL_JAR, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30114_1".equalsIgnoreCase(event) || "30114-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ALDERS_SKULL2, -1);
			qs.giveItems(ALDERS_RECEIPT, 1);
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30114-04.htm";
		} else if ("30476_2".equalsIgnoreCase(event) || "30476-12.htm".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_MIDDLE);
			if ("30476-12.htm".equalsIgnoreCase(event) && player != null && player.level() < 38) {
				qs.giveItems(KAIRAS_INSTRUCTIONS, 1);
				qs.setCond(14);
				return "30476-13.htm";
			}
			qs.takeItems(REVELATIONS_MANUSCRIPT, -1);
			qs.giveItems(KAIRAS_RECOMMEND, 1);
			qs.setCond(15);
			return "30476-12.htm";
		} else if ("30419_1".equalsIgnoreCase(event) || "30419-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(THIFIELS_LETTER, -1);
			qs.giveItems(ARKENIAS_NOTE, 1);
			qs.setCond(17);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30419-02.htm";
		} else if ("31845_1".equalsIgnoreCase(event) || "31845-02.htm".equalsIgnoreCase(event)) {
			qs.giveItems(PIXY_GARNET, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "31845-02.htm";
		} else if ("31850_1".equalsIgnoreCase(event) || "31850-02.htm".equalsIgnoreCase(event)) {
			qs.giveItems(BLIGHT_TREANT_SEED, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "31850-02.htm";
		} else if ("30419_2".equalsIgnoreCase(event) || "30419-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ARKENIAS_NOTE, -1);
			qs.takeItems(BLIGHT_TREANT_SAP, -1);
			qs.takeItems(RED_FAIRY_DUST, -1);
			qs.giveItems(ARKENIAS_LETTER, 1);
			qs.giveItems(ARKENIAS_LETTER_RETAIL, 1);
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30419-05.htm";
		} else if ("30358-04.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ARKENIAS_LETTER) || qs.hasQuestItems(ARKENIAS_LETTER_RETAIL)) {
				qs.takeItems(ARKENIAS_LETTER, -1);
				qs.takeItems(ARKENIAS_LETTER_RETAIL, -1);
				qs.takeItems(PALUS_CHARM, -1);
				qs.giveItems(MARK_OF_FATE, 1);
				qs.addExpAndSp(115000, 12000);
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
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == KAIRA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30476-03.htm";
				} else {
					return "30476-02.htm";
				}
			} else if (cond == 1) {
				return "30476-06.htm";
			} else if (cond == 2 || cond == 3) {
				return "30476-07.htm";
			} else if (cond > 3 && cond < 9) {
				return "30476-08.htm";
			} else if (cond == 9) {
				qs.takeItems(ALDERS_SKULL1, -1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.addSpawn(ALDERS_SPIRIT);
				return "30476-09.htm";
			} else if (cond > 9 && cond < 13) {
				return "30476-10.htm";
			} else if (cond == 13 && qs.hasQuestItems(REVELATIONS_MANUSCRIPT)) {
				return "30476-11.htm";
			} else if (cond == 14) {
				if (player != null && player.level() < 38) {
					return "30476-14.htm";
				}
				qs.takeItems(KAIRAS_INSTRUCTIONS, -1);
				qs.takeItems(REVELATIONS_MANUSCRIPT, -1);
				qs.giveItems(KAIRAS_RECOMMEND, 1);
				qs.setCond(15);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30476-12.htm";
			} else if (cond == 15) {
				return "30476-16.htm";
			} else if (cond > 15) {
				return "30476-17.htm";
			}
		} else if (npcId == METHEUS) {
			if (cond == 1) {
				return "30614-01.htm";
			} else if (cond == 2) {
				return "30614-02.htm";
			} else if (cond == 3 && qs.hasQuestItems(KASANDRAS_REMAINS)) {
				qs.takeItems(KASANDRAS_REMAINS, -1);
				qs.giveItems(HERBALISM_TEXTBOOK, 1);
				qs.setCond(4);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30614-03.htm";
			} else if (cond > 3 && cond < 8) {
				return "30614-04.htm";
			} else if (cond == 8) {
				qs.takeItems(BELLADONNA, -1);
				qs.giveItems(ALDERS_SKULL1, 1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30614-05.htm";
			} else if (cond > 8) {
				return "30614-06.htm";
			}
		} else if (npcId == IXIA) {
			if (cond == 4 || cond == 5) {
				qs.takeItems(HERBALISM_TEXTBOOK, -1);
				qs.giveItems(IXIAS_LIST, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30463-01.htm";
			} else if (cond == 6) {
				return "30463-02.htm";
			} else if (cond == 7) {
				qs.takeItems(IXIAS_LIST, -1);
				qs.takeItems(DEAD_SEEKER_DUNG, -1);
				qs.takeItems(MARSH_SPIDER_FLUIDS, -1);
				qs.takeItems(MEDUSA_ICHOR, -1);
				qs.takeItems(NIGHTSHADE_ROOT, -1);
				qs.takeItems(TYRANT_BLOOD, -1);
				qs.giveItems(BELLADONNA, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30463-03.htm";
			} else if (cond == 8) {
				return "30463-04.htm";
			} else if (cond > 8) {
				return "30463-05.htm";
			}
		} else if (npcId == ALDERS_SPIRIT) {
			if (cond == 10) {
				qs.giveItems(ALDERS_SKULL2, 1);
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30613-01.htm";
			}
		} else if (npcId == ROA) {
			if (cond == 11) {
				return "30114-01.htm";
			} else if (cond == 12) {
				return "30114-05.htm";
			} else if (cond > 12) {
				return "30114-06.htm";
			}
		} else if (npcId == NORMAN) {
			if (cond == 12) {
				qs.takeItems(ALDERS_RECEIPT, -1);
				qs.giveItems(REVELATIONS_MANUSCRIPT, 1);
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30210-01.htm";
			} else if (cond > 12) {
				return "30210-02.htm";
			}
		} else if (npcId == THIFIEL) {
			if (cond == 15 && qs.hasQuestItems(KAIRAS_RECOMMEND)) {
				qs.takeItems(KAIRAS_RECOMMEND, -1);
				qs.giveItems(PALUS_CHARM, 1);
				qs.giveItems(THIFIELS_LETTER, 1);
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30358-01.htm";
			} else if (cond == 16) {
				return "30358-02.htm";
			} else if (cond == 17) {
				return "30358-03.htm";
			} else if (cond == 18 && (qs.hasQuestItems(ARKENIAS_LETTER) || qs.hasQuestItems(ARKENIAS_LETTER_RETAIL))) {
				return "30358-03.htm";
			}
		} else if (npcId == ARKENIA) {
			if (cond == 16) {
				return "30419-01.htm";
			} else if (cond == 17) {
				return (qs.hasQuestItems(BLIGHT_TREANT_SAP) && qs.hasQuestItems(RED_FAIRY_DUST))
						? "30419-04.htm"
						: "30419-03.htm";
			} else if (cond == 18) {
				return "30419-06.htm";
			}
		} else if (npcId == BLOODY_PIXY) {
			if (cond == 17) {
				if (qs.hasQuestItems(PIXY_GARNET)) {
					if (qs.getQuestItemsCount(GRANDIS_SKULL) >= 10
							&& qs.getQuestItemsCount(KARUL_BUGBEAR_SKULL) >= 10
							&& qs.getQuestItemsCount(BREKA_OVERLORD_SKULL) >= 10
							&& qs.getQuestItemsCount(LETO_OVERLORD_SKULL) >= 10) {
						qs.takeItems(BREKA_OVERLORD_SKULL, -1);
						qs.takeItems(GRANDIS_SKULL, -1);
						qs.takeItems(KARUL_BUGBEAR_SKULL, -1);
						qs.takeItems(LETO_OVERLORD_SKULL, -1);
						qs.takeItems(PIXY_GARNET, -1);
						qs.giveItems(RED_FAIRY_DUST, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
						return "31845-04.htm";
					}
					return "31845-03.htm";
				} else if (qs.hasQuestItems(RED_FAIRY_DUST)) {
					return "31845-05.htm";
				} else {
					return "31845-01.htm";
				}
			} else if (cond == 18) {
				return "31845-05.htm";
			}
		} else if (npcId == BLIGHT_TREANT) {
			if (cond == 17) {
				if (qs.hasQuestItems(BLIGHT_TREANT_SEED)) {
					if (qs.hasQuestItems(BLACK_WILLOW_LEAF)) {
						qs.takeItems(BLACK_WILLOW_LEAF, -1);
						qs.takeItems(BLIGHT_TREANT_SEED, -1);
						qs.giveItems(BLIGHT_TREANT_SAP, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
						return "31850-04.htm";
					}
					return "31850-03.htm";
				} else if (qs.hasQuestItems(BLIGHT_TREANT_SAP)) {
					return "31850-05.htm";
				} else {
					return "31850-01.htm";
				}
			} else if (cond == 18) {
				return "31850-05.htm";
			}
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

		if (cond == 2 && npcId == HANGMAN_TREE && !qs.hasQuestItems(KASANDRAS_REMAINS)) {
			qs.takeItems(METHEUS_FUNERAL_JAR, -1);
			qs.giveItems(KASANDRAS_REMAINS, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if (cond == 6) {
			if (npcId == DEAD_SEEKER && qs.getQuestItemsCount(DEAD_SEEKER_DUNG) < 10) {
				qs.giveItems(DEAD_SEEKER_DUNG, 1);
			} else if ((npcId == TYRANT || npcId == TYRANT_KINGPIN) && qs.getQuestItemsCount(TYRANT_BLOOD) < 10) {
				qs.giveItems(TYRANT_BLOOD, 1);
			} else if (npcId == MEDUSA && qs.getQuestItemsCount(MEDUSA_ICHOR) < 10) {
				qs.giveItems(MEDUSA_ICHOR, 1);
			} else if ((npcId == MARSH_STAKATO || npcId == MARSH_STAKATO_WORKER || npcId == MARSH_STAKATO_SOLDIER
					|| npcId == MARSH_STAKATO_DRONE) && qs.getQuestItemsCount(NIGHTSHADE_ROOT) < 10) {
				qs.giveItems(NIGHTSHADE_ROOT, 1);
			} else if (npcId == MARSH_SPIDER && qs.getQuestItemsCount(MARSH_SPIDER_FLUIDS) < 10) {
				qs.giveItems(MARSH_SPIDER_FLUIDS, 1);
			}

			if (qs.getQuestItemsCount(DEAD_SEEKER_DUNG) >= 10
					&& qs.getQuestItemsCount(TYRANT_BLOOD) >= 10
					&& qs.getQuestItemsCount(MEDUSA_ICHOR) >= 10
					&& qs.getQuestItemsCount(NIGHTSHADE_ROOT) >= 10
					&& qs.getQuestItemsCount(MARSH_SPIDER_FLUIDS) >= 10) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (qs.hasQuestItems(PIXY_GARNET)) {
			if (npcId == GRANDIS && qs.getQuestItemsCount(GRANDIS_SKULL) < 10) {
				qs.giveItems(GRANDIS_SKULL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if (npcId == LETO_LIZARDMAN_OVERLORD && qs.getQuestItemsCount(LETO_OVERLORD_SKULL) < 10) {
				qs.giveItems(LETO_OVERLORD_SKULL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if (npcId == BREKA_ORC_OVERLORD && qs.getQuestItemsCount(BREKA_OVERLORD_SKULL) < 10) {
				qs.giveItems(BREKA_OVERLORD_SKULL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if (npcId == KARUL_BUGBEAR && qs.getQuestItemsCount(KARUL_BUGBEAR_SKULL) < 10) {
				qs.giveItems(KARUL_BUGBEAR_SKULL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == BLACK_WILLOW_LURKER && qs.hasQuestItems(BLIGHT_TREANT_SEED) && !qs.hasQuestItems(BLACK_WILLOW_LEAF)) {
			qs.giveItems(BLACK_WILLOW_LEAF, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
