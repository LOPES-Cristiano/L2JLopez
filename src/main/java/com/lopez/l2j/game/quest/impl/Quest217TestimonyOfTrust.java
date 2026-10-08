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
 * Quest 217: Testimony of Trust (2º Passo de 2ª Classe para todas as classes humanas, élficas e dark élficas).
 */
@Component
public class Quest217TestimonyOfTrust extends Quest {

	public static final int QUEST_ID = 217;
	public static final String QUEST_NAME = "217_TestimonyOfTrust";

	// NPCs
	public static final int HOLLIN = 30191;
	public static final int QUILT = 30031;
	public static final int OZZY = 30154;
	public static final int THIFIELL = 30358;
	public static final int CLAYTON = 30464;
	public static final int SERESIN = 30657;
	public static final int KAKAI = 30565;
	public static final int MANAKIA = 30515;
	public static final int LOCKIRIN = 30531;
	public static final int NIKOLA = 30621;

	// Monstros
	public static final int DRYAD = 20013;
	public static final int DRYAD_ELDER = 20019;
	public static final int LIREIN = 20036;
	public static final int LIREIN_ELDER = 20044;
	public static final int ACTEON = 27120;
	public static final int LUCIEN = 27121;
	public static final int GUARDIAN_BASILISK = 20550;
	public static final int WINDSUS = 20553;
	public static final int PORTA = 20213;

	// Itens
	public static final int MARK_OF_TRUST = 2734;
	public static final int LETTER_TO_ELF = 1558;
	public static final int LETTER_TO_DARKELF = 1556;
	public static final int LETTER_TO_SERESIN = 2739;
	public static final int LETTER_TO_ORC = 2738;
	public static final int LETTER_TO_DWARF = 2737;
	public static final int SCROLL_OF_DARKELF_TRUST = 2740;
	public static final int SCROLL_OF_ELF_TRUST = 2741;
	public static final int SCROLL_OF_DWARF_TRUST = 2742;
	public static final int SCROLL_OF_ORC_TRUST = 2743;
	public static final int RECOMMENDATION_OF_HOLLIN = 2744;
	public static final int ORDER_OF_OZZY = 2745;
	public static final int BREATH_OF_WINDS = 2746;
	public static final int SEED_OF_VERDURE = 2747;
	public static final int LETTER_OF_THIFIELL = 2748;
	public static final int ORDER_OF_CLAYTON = 2755;
	public static final int BASILISK_PLASMA = 2752;
	public static final int STAKATO_ICHOR = 2753;
	public static final int HONEY_DEW = 2754;
	public static final int STAKATO_FLUID = 2750;
	public static final int BASILISK_PLASMA_INGREDIENT = 2749;
	public static final int GIANT_APHID = 2751;
	public static final int LETTER_OF_MANAKIA = 2757;
	public static final int PARASITE_OF_LOTA = 2756;
	public static final int LETTER_OF_KAKAI = 2758;
	public static final int LETTER_OF_LOCKIRIN = 2759;
	public static final int HEART_OF_PORTA = 2761;
	public static final int ORDER_OF_NIKOLA = 2760;

	@Autowired
	public Quest217TestimonyOfTrust(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Trust");

		addStartNpc(HOLLIN);
		addTalkId(HOLLIN);
		addTalkId(QUILT);
		addTalkId(OZZY);
		addTalkId(THIFIELL);
		addTalkId(CLAYTON);
		addTalkId(SERESIN);
		addTalkId(KAKAI);
		addTalkId(MANAKIA);
		addTalkId(LOCKIRIN);
		addTalkId(NIKOLA);

		addKillId(DRYAD);
		addKillId(DRYAD_ELDER);
		addKillId(LIREIN);
		addKillId(LIREIN_ELDER);
		addKillId(ACTEON);
		addKillId(LUCIEN);
		addKillId(GUARDIAN_BASILISK);
		addKillId(WINDSUS);
		addKillId(PORTA);

		// Marsh stakato
		addKillId(20157);
		addKillId(20230);
		addKillId(20232);
		addKillId(20234);

		// Ants
		for (int mob = 20082; mob <= 20088; mob++) {
			addKillId(mob);
		}

		registerQuestItems(LETTER_TO_ELF, LETTER_TO_DARKELF, LETTER_TO_SERESIN, LETTER_TO_ORC,
				LETTER_TO_DWARF, SCROLL_OF_DARKELF_TRUST, SCROLL_OF_ELF_TRUST, SCROLL_OF_DWARF_TRUST,
				SCROLL_OF_ORC_TRUST, RECOMMENDATION_OF_HOLLIN, ORDER_OF_OZZY, BREATH_OF_WINDS,
				SEED_OF_VERDURE, LETTER_OF_THIFIELL, ORDER_OF_CLAYTON, BASILISK_PLASMA,
				STAKATO_ICHOR, HONEY_DEW, STAKATO_FLUID, BASILISK_PLASMA_INGREDIENT,
				GIANT_APHID, LETTER_OF_MANAKIA, PARASITE_OF_LOTA, LETTER_OF_KAKAI,
				LETTER_OF_LOCKIRIN, HEART_OF_PORTA, ORDER_OF_NIKOLA);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			qs.giveItems(LETTER_TO_ELF, 1);
			qs.giveItems(LETTER_TO_DARKELF, 1);
			return "30191-04.htm";
		} else if ("30154_1".equalsIgnoreCase(event)) {
			return "30154-02.htm";
		} else if ("30154_2".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_TO_ELF, -1);
			qs.giveItems(ORDER_OF_OZZY, 1);
			qs.setCond(2);
			qs.playSound("ItemSound.quest_middle");
			return "30154-03.htm";
		} else if ("30358_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_TO_DARKELF, -1);
			qs.giveItems(LETTER_OF_THIFIELL, 1);
			qs.setCond(5);
			qs.playSound("ItemSound.quest_middle");
			return "30358-02.htm";
		} else if ("30657_1".equalsIgnoreCase(event)) {
			PlayerCharacter player = qs.getPlayerCharacter();
			if (player != null && player.getLevel() >= 38) {
				qs.takeItems(LETTER_TO_SERESIN, -1);
				qs.giveItems(LETTER_TO_ORC, 1);
				qs.giveItems(LETTER_TO_DWARF, 1);
				qs.setCond(12);
				qs.playSound("ItemSound.quest_middle");
				return "30657-03.htm";
			}
			return "30657-02.htm";
		} else if ("30565_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_TO_ORC, -1);
			qs.giveItems(LETTER_OF_MANAKIA, 1);
			qs.setCond(13);
			qs.playSound("ItemSound.quest_middle");
			return "30565-02.htm";
		} else if ("30515_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_MANAKIA, -1);
			qs.setCond(14);
			qs.playSound("ItemSound.quest_middle");
			return "30515-02.htm";
		} else if ("30531_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_TO_DWARF, -1);
			qs.giveItems(LETTER_OF_LOCKIRIN, 1);
			qs.setCond(18);
			qs.playSound("ItemSound.quest_middle");
			return "30531-02.htm";
		} else if ("30621_1".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_LOCKIRIN, -1);
			qs.giveItems(ORDER_OF_NIKOLA, 1);
			qs.setCond(19);
			qs.playSound("ItemSound.quest_middle");
			return "30621-02.htm";
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

		if (qs.getQuestItemsCount(MARK_OF_TRUST) > 0) {
			return "completed";
		}

		if (npcId == HOLLIN) {
			if (cond == 0) {
				if (player.getLevel() >= 37) {
					return "30191-03.htm";
				}
				return "30191-01.htm";
			} else if (cond == 9 && qs.getQuestItemsCount(SCROLL_OF_ELF_TRUST) > 0 && qs.getQuestItemsCount(SCROLL_OF_DARKELF_TRUST) > 0) {
				qs.takeItems(SCROLL_OF_DARKELF_TRUST, -1);
				qs.takeItems(SCROLL_OF_ELF_TRUST, -1);
				qs.giveItems(LETTER_TO_SERESIN, 1);
				qs.setCond(10);
				qs.playSound("ItemSound.quest_middle");
				return "30191-05.htm";
			} else if (cond == 22 && qs.getQuestItemsCount(SCROLL_OF_DWARF_TRUST) > 0 && qs.getQuestItemsCount(SCROLL_OF_ORC_TRUST) > 0) {
				qs.takeItems(SCROLL_OF_DWARF_TRUST, -1);
				qs.takeItems(SCROLL_OF_ORC_TRUST, -1);
				qs.giveItems(RECOMMENDATION_OF_HOLLIN, 1);
				qs.setCond(23);
				qs.playSound("ItemSound.quest_middle");
				return "30191-06.htm";
			} else if (cond == 1) {
				return "30191-08.htm";
			} else if (cond == 8) {
				return "30191-09.htm";
			}
		} else if (npcId == OZZY) {
			if (cond == 1 && qs.getQuestItemsCount(LETTER_TO_ELF) > 0) {
				return "30154-01.htm";
			} else if (cond == 2 && qs.getQuestItemsCount(ORDER_OF_OZZY) > 0) {
				return "30154-04.htm";
			} else if (cond == 3 && qs.getQuestItemsCount(BREATH_OF_WINDS) > 0 && qs.getQuestItemsCount(SEED_OF_VERDURE) > 0) {
				qs.takeItems(BREATH_OF_WINDS, -1);
				qs.takeItems(SEED_OF_VERDURE, -1);
				qs.takeItems(ORDER_OF_OZZY, -1);
				qs.giveItems(SCROLL_OF_ELF_TRUST, 1);
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				return "30154-05.htm";
			} else if (cond == 4) {
				return "30154-06.htm";
			}
		} else if (npcId == THIFIELL) {
			if (cond == 4 && qs.getQuestItemsCount(LETTER_TO_DARKELF) > 0) {
				return "30358-01.htm";
			} else if (cond == 8 && qs.getQuestItemsCount(HONEY_DEW) > 0 && qs.getQuestItemsCount(STAKATO_ICHOR) > 0 && qs.getQuestItemsCount(BASILISK_PLASMA) > 0) {
				qs.takeItems(BASILISK_PLASMA, -1);
				qs.takeItems(HONEY_DEW, -1);
				qs.takeItems(STAKATO_ICHOR, -1);
				qs.giveItems(SCROLL_OF_DARKELF_TRUST, 1);
				qs.setCond(9);
				qs.playSound("ItemSound.quest_middle");
				return "30358-03.htm";
			} else if (cond == 7) {
				return "30358-04.htm";
			} else if (cond == 5) {
				return "30358-05.htm";
			}
		} else if (npcId == CLAYTON) {
			if (cond == 5 && qs.getQuestItemsCount(LETTER_OF_THIFIELL) > 0) {
				qs.takeItems(LETTER_OF_THIFIELL, -1);
				qs.giveItems(ORDER_OF_CLAYTON, 1);
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				return "30464-01.htm";
			} else if (cond == 6) {
				return "30464-02.htm";
			} else if (cond == 7) {
				qs.takeItems(ORDER_OF_CLAYTON, -1);
				qs.setCond(8);
				qs.playSound("ItemSound.quest_middle");
				return "30464-03.htm";
			}
		} else if (npcId == SERESIN) {
			if (cond == 10 || cond == 11) {
				if (player.getLevel() >= 38) {
					return "30657-01.htm";
				}
				qs.setCond(11);
				return "30657-02.htm";
			}
		} else if (npcId == KAKAI) {
			if (cond == 12 && qs.getQuestItemsCount(LETTER_TO_ORC) > 0) {
				return "30565-01.htm";
			} else if (cond == 13) {
				return "30565-03.htm";
			} else if (cond == 16) {
				qs.takeItems(LETTER_OF_KAKAI, -1);
				qs.giveItems(SCROLL_OF_ORC_TRUST, 1);
				qs.setCond(17);
				qs.playSound("ItemSound.quest_middle");
				return "30565-04.htm";
			} else if (cond >= 17) {
				return "30565-05.htm";
			}
		} else if (npcId == MANAKIA) {
			if (cond == 13 && qs.getQuestItemsCount(LETTER_OF_MANAKIA) > 0) {
				return "30515-01.htm";
			} else if (cond == 14) {
				return "30515-03.htm";
			} else if (cond == 15 && qs.getQuestItemsCount(PARASITE_OF_LOTA) >= 10) {
				qs.takeItems(PARASITE_OF_LOTA, -1);
				qs.giveItems(LETTER_OF_KAKAI, 1);
				qs.setCond(16);
				qs.playSound("ItemSound.quest_middle");
				return "30515-04.htm";
			} else if (cond == 16) {
				return "30515-05.htm";
			}
		} else if (npcId == LOCKIRIN) {
			if (cond == 17 && qs.getQuestItemsCount(LETTER_TO_DWARF) > 0) {
				return "30531-01.htm";
			} else if (cond == 18) {
				return "30531-03.htm";
			} else if (cond == 21) {
				qs.giveItems(SCROLL_OF_DWARF_TRUST, 1);
				qs.setCond(22);
				qs.playSound("ItemSound.quest_middle");
				return "30531-04.htm";
			} else if (cond == 22) {
				return "30531-05.htm";
			}
		} else if (npcId == NIKOLA) {
			if (cond == 18 && qs.getQuestItemsCount(LETTER_OF_LOCKIRIN) > 0) {
				return "30621-01.htm";
			} else if (cond == 19) {
				return "30621-03.htm";
			} else if (cond == 20 && qs.getQuestItemsCount(HEART_OF_PORTA) > 0) {
				qs.takeItems(HEART_OF_PORTA, -1);
				qs.takeItems(ORDER_OF_NIKOLA, -1);
				qs.setCond(21);
				qs.playSound("ItemSound.quest_middle");
				return "30621-04.htm";
			} else if (cond == 21) {
				return "30621-05.htm";
			}
		} else if (npcId == QUILT) {
			if (cond == 23 && qs.getQuestItemsCount(RECOMMENDATION_OF_HOLLIN) > 0) {
				qs.takeItems(RECOMMENDATION_OF_HOLLIN, -1);
				qs.giveItems(MARK_OF_TRUST, 1);
				qs.rewardItems(7562, 8); // Dimensional Diamond
				qs.addExpAndSp(39571, 2500);
				qs.playSound("ItemSound.quest_finish");
				qs.exitCurrentQuest(false);
				return "30031-01.htm";
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

		if (cond == 2) {
			if ((npcId == LIREIN || npcId == LIREIN_ELDER) && qs.getQuestItemsCount(BREATH_OF_WINDS) == 0) {
				qs.giveItems(BREATH_OF_WINDS, 1);
				if (qs.getQuestItemsCount(SEED_OF_VERDURE) > 0) {
					qs.setCond(3);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			} else if ((npcId == DRYAD || npcId == DRYAD_ELDER) && qs.getQuestItemsCount(SEED_OF_VERDURE) == 0) {
				qs.giveItems(SEED_OF_VERDURE, 1);
				if (qs.getQuestItemsCount(BREATH_OF_WINDS) > 0) {
					qs.setCond(3);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 6) {
			if (npcId == GUARDIAN_BASILISK && qs.getQuestItemsCount(BASILISK_PLASMA) == 0) {
				qs.giveItems(BASILISK_PLASMA, 1);
				checkClaytonOrderComplete(qs);
			} else if ((npcId == 20157 || npcId == 20230 || npcId == 20232 || npcId == 20234) && qs.getQuestItemsCount(HONEY_DEW) == 0) {
				qs.giveItems(HONEY_DEW, 1);
				checkClaytonOrderComplete(qs);
			} else if ((npcId >= 20082 && npcId <= 20088) && qs.getQuestItemsCount(STAKATO_ICHOR) == 0) {
				qs.giveItems(STAKATO_ICHOR, 1);
				checkClaytonOrderComplete(qs);
			}
		} else if (cond == 14 && npcId == WINDSUS) {
			if (qs.getQuestItemsCount(PARASITE_OF_LOTA) < 10) {
				qs.giveItems(PARASITE_OF_LOTA, 1);
				if (qs.getQuestItemsCount(PARASITE_OF_LOTA) >= 10) {
					qs.setCond(15);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 19 && npcId == PORTA) {
			if (qs.getQuestItemsCount(HEART_OF_PORTA) == 0) {
				qs.giveItems(HEART_OF_PORTA, 1);
				qs.setCond(20);
				qs.playSound("ItemSound.quest_middle");
			}
		}

		return null;
	}

	private void checkClaytonOrderComplete(QuestState qs) {
		if (qs.getQuestItemsCount(BASILISK_PLASMA) > 0
				&& qs.getQuestItemsCount(HONEY_DEW) > 0
				&& qs.getQuestItemsCount(STAKATO_ICHOR) > 0) {
			qs.setCond(7);
			qs.playSound("ItemSound.quest_middle");
		} else {
			qs.playSound("ItemSound.quest_itemget");
		}
	}
}
