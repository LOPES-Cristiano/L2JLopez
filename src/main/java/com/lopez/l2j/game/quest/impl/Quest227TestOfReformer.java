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
 * Quest 227: Test of the Reformer
 * 3º Passo de 2ª Classe para Prophet, Shillien Elder.
 */
@Component
public class Quest227TestOfReformer extends Quest {

	public static final int QUEST_ID = 227;
	public static final String QUEST_NAME = "227_TestOfReformer";

	// NPCs
	public static final int PUPINA = 30118;
	public static final int SLA = 30666;
	public static final int RAMUS = 30667;
	public static final int KATARI = 30668;
	public static final int KAKAN = 30669;
	public static final int NYAKURI = 30670;
	public static final int OL_MAHUM_PILGRIM = 30732;

	// Monstros
	public static final int MISERY_SKELETON = 20022;
	public static final int SKELETON_ARCHER = 20100;
	public static final int SKELETON_MARKSMAN = 20102;
	public static final int SKELETON_LORD = 20104;
	public static final int SILENT_HORROR = 20404;
	public static final int NAMELESS_REVENANT = 27099;
	public static final int ARURAUNE = 27128;
	public static final int OL_MAHUM_INSPECTOR = 27129;
	public static final int OL_MAHUM_BETRAYER = 27130;
	public static final int CRIMSON_WEREWOLF = 27131;
	public static final int KRUDEL_LIZARDMAN = 27132;

	// Itens
	public static final int MARK_OF_REFORMER = 2821;
	public static final int BOOK_OF_REFORM = 2822;
	public static final int LETTER_OF_INTRODUCTION = 2823;
	public static final int SLAS_LETTER = 2824;
	public static final int GREETINGS = 2825;
	public static final int OLMAHUMS_MONEY = 2826;
	public static final int KATARIS_LETTER = 2827;
	public static final int NYAKURIS_LETTER = 2828;
	public static final int UNDEAD_LIST = 2829;
	public static final int RAMUSS_LETTER = 2830;
	public static final int RIPPED_DIARY = 2831;
	public static final int HUGE_NAIL = 2832;
	public static final int LETTER_OF_BETRAYER = 2833;
	public static final int BONE_FRAGMENT_4 = 2834;
	public static final int BONE_FRAGMENT_5 = 2835;
	public static final int BONE_FRAGMENT_6 = 2836;
	public static final int BONE_FRAGMENT_7 = 2837;
	public static final int BONE_FRAGMENT_8 = 2838;
	public static final int BONE_FRAGMENT_9 = 2839;
	public static final int KAKANS_LETTER = 3037;

	private static final Set<Integer> VALID_CLASSES = Set.of(15, 42);

	@Autowired
	public Quest227TestOfReformer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Reformer");

		addStartNpc(PUPINA);
		addTalkId(PUPINA, SLA, RAMUS, KATARI, KAKAN, NYAKURI, OL_MAHUM_PILGRIM);

		addAttackId(NAMELESS_REVENANT, CRIMSON_WEREWOLF);
		addKillId(MISERY_SKELETON, SKELETON_ARCHER, SKELETON_MARKSMAN, SKELETON_LORD,
				SILENT_HORROR, NAMELESS_REVENANT, ARURAUNE, OL_MAHUM_INSPECTOR,
				OL_MAHUM_BETRAYER, CRIMSON_WEREWOLF, KRUDEL_LIZARDMAN);

		registerQuestItems(BOOK_OF_REFORM, LETTER_OF_INTRODUCTION, SLAS_LETTER, GREETINGS,
				OLMAHUMS_MONEY, KATARIS_LETTER, NYAKURIS_LETTER, UNDEAD_LIST, RAMUSS_LETTER,
				RIPPED_DIARY, HUGE_NAIL, LETTER_OF_BETRAYER, BONE_FRAGMENT_4, BONE_FRAGMENT_5,
				BONE_FRAGMENT_6, BONE_FRAGMENT_7, BONE_FRAGMENT_8, BONE_FRAGMENT_9, KAKANS_LETTER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30118-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BOOK_OF_REFORM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30118-04.htm";
		} else if ("30118_1".equalsIgnoreCase(event) || "30118-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BOOK_OF_REFORM, -1);
			qs.takeItems(HUGE_NAIL, -1);
			qs.giveItems(LETTER_OF_INTRODUCTION, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30118-06.htm";
		} else if ("30666_3".equalsIgnoreCase(event) || "30666-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_INTRODUCTION, -1);
			qs.giveItems(SLAS_LETTER, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30666-04.htm";
		} else if ("30669-03.htm".equalsIgnoreCase(event) || "30669_1".equalsIgnoreCase(event)) {
			if (qs.getCond() != 12) {
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			qs.addSpawn(CRIMSON_WEREWOLF, -9382, -89852, -2333, 299000);
			return "30669-03.htm";
		} else if ("30670-03.htm".equalsIgnoreCase(event) || "30670_1".equalsIgnoreCase(event)) {
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(KRUDEL_LIZARDMAN, 126019, -179983, -1781, 299000);
			return "30670-03.htm";
		} else if ("30666-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RAMUSS_LETTER)) {
				qs.takeItems(KATARIS_LETTER, -1);
				qs.takeItems(KAKANS_LETTER, -1);
				qs.takeItems(NYAKURIS_LETTER, -1);
				qs.takeItems(RAMUSS_LETTER, -1);
				qs.giveItems(MARK_OF_REFORMER, 1);
				qs.addExpAndSp(164032, 17500);
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

		if (npcId == PUPINA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30118-03.htm";
				} else {
					return "30118-01.htm";
				}
			} else if (cond == 1 || cond == 2) {
				return "30118-05.htm";
			} else if (cond == 3) {
				return "30118-05.htm";
			} else if (cond > 3) {
				return "30118-07.htm";
			}
		} else if (npcId == SLA) {
			if (cond == 4) {
				return "30666-01.htm";
			} else if (cond >= 5 && cond <= 9) {
				return "30666-05.htm";
			} else if (cond == 10) {
				qs.takeItems(OLMAHUMS_MONEY, -1);
				qs.giveItems(GREETINGS, 3);
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30666-06.htm";
			} else if (cond >= 11 && cond <= 18) {
				return "30666-06.htm";
			} else if (cond >= 19 && qs.hasQuestItems(RAMUSS_LETTER)) {
				return "30666-06.htm";
			}
		} else if (npcId == KATARI) {
			if (cond == 5 || cond == 6) {
				if (cond == 5) {
					qs.takeItems(SLAS_LETTER, -1);
					qs.setCond(6);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
				qs.addSpawn(OL_MAHUM_PILGRIM, -4015, 40141, -3664, 60000);
				qs.addSpawn(OL_MAHUM_INSPECTOR, -4034, 40201, -3665, 60000);
				return "30668-01.htm";
			} else if (cond == 7) {
				qs.addSpawn(OL_MAHUM_PILGRIM, -4015, 40141, -3664, 60000);
				return "30668-01.htm";
			} else if (cond == 8) {
				qs.addSpawn(OL_MAHUM_BETRAYER, -4106, 40174, -3660, 40000);
				return "30668-02.htm";
			} else if (cond == 9) {
				qs.takeItems(LETTER_OF_BETRAYER, -1);
				qs.giveItems(KATARIS_LETTER, 1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30668-03.htm";
			} else if (cond > 9) {
				return "30668-04.htm";
			}
		} else if (npcId == OL_MAHUM_PILGRIM) {
			if (cond == 7) {
				qs.giveItems(OLMAHUMS_MONEY, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30732-01.htm";
			}
		} else if (npcId == KAKAN) {
			if (cond == 11 || cond == 12) {
				return "30669-01.htm";
			} else if (cond == 13) {
				qs.takeItems(GREETINGS, 1);
				qs.giveItems(KAKANS_LETTER, 1);
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30669-04.htm";
			} else if (cond > 13) {
				return "30669-04.htm";
			}
		} else if (npcId == NYAKURI) {
			if (cond == 14 || cond == 15) {
				return "30670-01.htm";
			} else if (cond == 16) {
				qs.takeItems(GREETINGS, 1);
				qs.giveItems(NYAKURIS_LETTER, 1);
				qs.setCond(17);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30670-04.htm";
			} else if (cond > 16) {
				return "30670-04.htm";
			}
		} else if (npcId == RAMUS) {
			if (cond == 17) {
				qs.takeItems(GREETINGS, 1);
				qs.giveItems(UNDEAD_LIST, 1);
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30667-01.htm";
			} else if (cond == 18) {
				return "30667-02.htm";
			} else if (cond == 19) {
				qs.takeItems(BONE_FRAGMENT_4, -1);
				qs.takeItems(BONE_FRAGMENT_5, -1);
				qs.takeItems(BONE_FRAGMENT_6, -1);
				qs.takeItems(BONE_FRAGMENT_7, -1);
				qs.takeItems(BONE_FRAGMENT_8, -1);
				qs.takeItems(UNDEAD_LIST, -1);
				qs.giveItems(RAMUSS_LETTER, 1);
				qs.setCond(20);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30667-03.htm";
			} else if (cond > 19) {
				return "30667-03.htm";
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

		switch (npcId) {
			case NAMELESS_REVENANT -> {
				if (cond == 1 || cond == 2) {
					qs.giveItems(RIPPED_DIARY, 1);
					if (qs.getQuestItemsCount(RIPPED_DIARY) >= 7) {
						qs.takeItems(RIPPED_DIARY, -1);
						qs.setCond(2);
						qs.addSpawn(ARURAUNE);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case ARURAUNE -> {
				if (cond == 2) {
					qs.giveItems(HUGE_NAIL, 1);
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case OL_MAHUM_INSPECTOR -> {
				if (cond == 6) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case OL_MAHUM_BETRAYER -> {
				if (cond == 8) {
					qs.giveItems(LETTER_OF_BETRAYER, 1);
					qs.setCond(9);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case CRIMSON_WEREWOLF -> {
				if (cond == 12) {
					qs.setCond(13);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case KRUDEL_LIZARDMAN -> {
				if (cond == 15) {
					qs.setCond(16);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case SILENT_HORROR -> {
				if (cond == 18 && !qs.hasQuestItems(BONE_FRAGMENT_4)) {
					qs.giveItems(BONE_FRAGMENT_4, 1);
					if (qs.hasQuestItems(BONE_FRAGMENT_5) && qs.hasQuestItems(BONE_FRAGMENT_6)
							&& qs.hasQuestItems(BONE_FRAGMENT_7) && qs.hasQuestItems(BONE_FRAGMENT_8)) {
						qs.setCond(19);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case SKELETON_LORD -> {
				if (cond == 18 && !qs.hasQuestItems(BONE_FRAGMENT_5)) {
					qs.giveItems(BONE_FRAGMENT_5, 1);
					if (qs.hasQuestItems(BONE_FRAGMENT_4) && qs.hasQuestItems(BONE_FRAGMENT_6)
							&& qs.hasQuestItems(BONE_FRAGMENT_7) && qs.hasQuestItems(BONE_FRAGMENT_8)) {
						qs.setCond(19);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case SKELETON_MARKSMAN -> {
				if (cond == 18 && !qs.hasQuestItems(BONE_FRAGMENT_6)) {
					qs.giveItems(BONE_FRAGMENT_6, 1);
					if (qs.hasQuestItems(BONE_FRAGMENT_4) && qs.hasQuestItems(BONE_FRAGMENT_5)
							&& qs.hasQuestItems(BONE_FRAGMENT_7) && qs.hasQuestItems(BONE_FRAGMENT_8)) {
						qs.setCond(19);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case MISERY_SKELETON -> {
				if (cond == 18 && !qs.hasQuestItems(BONE_FRAGMENT_7)) {
					qs.giveItems(BONE_FRAGMENT_7, 1);
					if (qs.hasQuestItems(BONE_FRAGMENT_4) && qs.hasQuestItems(BONE_FRAGMENT_5)
							&& qs.hasQuestItems(BONE_FRAGMENT_6) && qs.hasQuestItems(BONE_FRAGMENT_8)) {
						qs.setCond(19);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case SKELETON_ARCHER -> {
				if (cond == 18 && !qs.hasQuestItems(BONE_FRAGMENT_8)) {
					qs.giveItems(BONE_FRAGMENT_8, 1);
					if (qs.hasQuestItems(BONE_FRAGMENT_4) && qs.hasQuestItems(BONE_FRAGMENT_5)
							&& qs.hasQuestItems(BONE_FRAGMENT_6) && qs.hasQuestItems(BONE_FRAGMENT_7)) {
						qs.setCond(19);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}

		return null;
	}
}
