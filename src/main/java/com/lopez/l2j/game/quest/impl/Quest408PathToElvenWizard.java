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
 * Quest 408: Path to Elven Wizard (1ª Troca de Classe do Elven Mystic para Elven Wizard).
 */
@Component
public class Quest408PathToElvenWizard extends Quest {

	public static final int QUEST_ID = 408;
	public static final String QUEST_NAME = "408_PathToElvenwizard";

	// NPCs
	public static final int ROSELLA = 30414;
	public static final int GREENIS = 30157;
	public static final int THALIA = 30371;
	public static final int NORTHWIND = 30423;

	// Monstros
	public static final int DRYAD_ELDER = 20019;
	public static final int PINCER_SPIDER = 20466;
	public static final int SUKAR_WERERAT_LEADER = 20047;

	// Itens
	public static final int ROGELLIAS_LETTER = 1218;
	public static final int RED_DOWN = 1219;
	public static final int MAGICAL_POWERS_RUBY = 1220;
	public static final int PURE_AQUAMARINE = 1221;
	public static final int APPETIZING_APPLE = 1222;
	public static final int GOLD_LEAVES = 1223;
	public static final int IMMORTAL_LOVE = 1224;
	public static final int AMETHYST = 1225;
	public static final int NOBILITY_AMETHYST = 1226;
	public static final int FERTILITY_PERIDOT = 1229;
	public static final int ETERNITY_DIAMOND = 1230;
	public static final int CHARM_OF_GRAIN = 1272;
	public static final int SAP_OF_WORLD_TREE = 1273;
	public static final int LUCKY_POTPOURI = 1274;

	@Autowired
	public Quest408PathToElvenWizard(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Elven Wizard");

		addStartNpc(ROSELLA);
		addTalkId(ROSELLA);
		addTalkId(GREENIS);
		addTalkId(THALIA);
		addTalkId(NORTHWIND);

		addKillId(DRYAD_ELDER);
		addKillId(PINCER_SPIDER);
		addKillId(SUKAR_WERERAT_LEADER);

		registerQuestItems(ROGELLIAS_LETTER, RED_DOWN, MAGICAL_POWERS_RUBY, PURE_AQUAMARINE,
				APPETIZING_APPLE, GOLD_LEAVES, IMMORTAL_LOVE, AMETHYST, NOBILITY_AMETHYST,
				FERTILITY_PERIDOT, CHARM_OF_GRAIN, SAP_OF_WORLD_TREE, LUCKY_POTPOURI);

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
		int level = player != null ? player.getLevel() : 0;
		int classId = player != null ? player.getClassId() : -1;

		if ("1".equalsIgnoreCase(event)) {
			if (classId != 0x19) {
				return classId == 0x1a ? "30414-02a.htm" : "30414-03.htm";
			} else if (level < 18) {
				return "30414-04.htm";
			} else if (qs.getQuestItemsCount(ETERNITY_DIAMOND) > 0) {
				return "30414-05.htm";
			} else {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				if (qs.getQuestItemsCount(FERTILITY_PERIDOT) == 0) {
					qs.giveItems(FERTILITY_PERIDOT, 1);
				}
				return "30414-06.htm";
			}
		} else if ("408_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(MAGICAL_POWERS_RUBY) > 0) {
				return "30414-10.htm";
			} else {
				if (qs.getQuestItemsCount(ROGELLIAS_LETTER) == 0) {
					qs.giveItems(ROGELLIAS_LETTER, 1);
				}
				return "30414-07.htm";
			}
		} else if ("408_4".equalsIgnoreCase(event)) {
			qs.takeItems(ROGELLIAS_LETTER, -1);
			if (qs.getQuestItemsCount(CHARM_OF_GRAIN) == 0) {
				qs.giveItems(CHARM_OF_GRAIN, 1);
			}
			return "30157-02.htm";
		} else if ("408_2".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(PURE_AQUAMARINE) > 0) {
				return "30414-13.htm";
			} else {
				if (qs.getQuestItemsCount(APPETIZING_APPLE) == 0) {
					qs.giveItems(APPETIZING_APPLE, 1);
				}
				return "30414-14.htm";
			}
		} else if ("408_5".equalsIgnoreCase(event)) {
			qs.takeItems(APPETIZING_APPLE, -1);
			if (qs.getQuestItemsCount(SAP_OF_WORLD_TREE) == 0) {
				qs.giveItems(SAP_OF_WORLD_TREE, 1);
			}
			return "30371-02.htm";
		} else if ("408_3".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(NOBILITY_AMETHYST) > 0) {
				return "30414-17.htm";
			} else {
				if (qs.getQuestItemsCount(IMMORTAL_LOVE) == 0) {
					qs.giveItems(IMMORTAL_LOVE, 1);
				}
				return "30414-18.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ROSELLA) {
			if (cond == 0) {
				return "30414-01.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(MAGICAL_POWERS_RUBY) > 0
						&& qs.getQuestItemsCount(PURE_AQUAMARINE) > 0
						&& qs.getQuestItemsCount(NOBILITY_AMETHYST) > 0) {
					qs.takeItems(MAGICAL_POWERS_RUBY, -1);
					qs.takeItems(PURE_AQUAMARINE, -1);
					qs.takeItems(NOBILITY_AMETHYST, -1);
					qs.takeItems(FERTILITY_PERIDOT, -1);
					qs.rewardItems(57, 81900);
					qs.giveItems(ETERNITY_DIAMOND, 1);
					qs.addExpAndSp(295862, 17964);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30414-24.htm";
				} else {
					return "30414-25.htm";
				}
			}
		} else if (npcId == GREENIS && cond > 0) {
			if (qs.getQuestItemsCount(ROGELLIAS_LETTER) > 0) {
				return "30157-01.htm";
			} else if (qs.getQuestItemsCount(CHARM_OF_GRAIN) > 0 && qs.getQuestItemsCount(RED_DOWN) < 5) {
				return "30157-03.htm";
			} else if (qs.getQuestItemsCount(CHARM_OF_GRAIN) > 0 && qs.getQuestItemsCount(RED_DOWN) >= 5) {
				qs.takeItems(RED_DOWN, -1);
				qs.takeItems(CHARM_OF_GRAIN, -1);
				qs.giveItems(MAGICAL_POWERS_RUBY, 1);
				return "30157-04.htm";
			} else if (qs.getQuestItemsCount(MAGICAL_POWERS_RUBY) > 0) {
				return "30157-05.htm";
			}
		} else if (npcId == THALIA && cond > 0) {
			if (qs.getQuestItemsCount(APPETIZING_APPLE) > 0) {
				return "30371-01.htm";
			} else if (qs.getQuestItemsCount(SAP_OF_WORLD_TREE) > 0 && qs.getQuestItemsCount(GOLD_LEAVES) < 5) {
				return "30371-03.htm";
			} else if (qs.getQuestItemsCount(SAP_OF_WORLD_TREE) > 0 && qs.getQuestItemsCount(GOLD_LEAVES) >= 5) {
				qs.takeItems(GOLD_LEAVES, -1);
				qs.takeItems(SAP_OF_WORLD_TREE, -1);
				qs.giveItems(PURE_AQUAMARINE, 1);
				return "30371-04.htm";
			} else if (qs.getQuestItemsCount(PURE_AQUAMARINE) > 0) {
				return "30371-05.htm";
			}
		} else if (npcId == NORTHWIND && cond > 0) {
			if (qs.getQuestItemsCount(IMMORTAL_LOVE) > 0) {
				qs.takeItems(IMMORTAL_LOVE, -1);
				qs.giveItems(LUCKY_POTPOURI, 1);
				return "30423-01.htm";
			} else if (qs.getQuestItemsCount(LUCKY_POTPOURI) > 0 && qs.getQuestItemsCount(AMETHYST) < 2) {
				return "30423-02.htm";
			} else if (qs.getQuestItemsCount(LUCKY_POTPOURI) > 0 && qs.getQuestItemsCount(AMETHYST) >= 2) {
				qs.takeItems(AMETHYST, -1);
				qs.takeItems(LUCKY_POTPOURI, -1);
				qs.giveItems(NOBILITY_AMETHYST, 1);
				return "30423-03.htm";
			} else if (qs.getQuestItemsCount(NOBILITY_AMETHYST) > 0) {
				return "30423-04.htm";
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

		if (npcId == PINCER_SPIDER && qs.getQuestItemsCount(CHARM_OF_GRAIN) > 0 && qs.getQuestItemsCount(RED_DOWN) < 5) {
			qs.giveItems(RED_DOWN, 1);
			if (qs.getQuestItemsCount(RED_DOWN) >= 5) {
				qs.playSound("ItemSound.quest_middle");
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		} else if (npcId == DRYAD_ELDER && qs.getQuestItemsCount(SAP_OF_WORLD_TREE) > 0 && qs.getQuestItemsCount(GOLD_LEAVES) < 5) {
			qs.giveItems(GOLD_LEAVES, 1);
			if (qs.getQuestItemsCount(GOLD_LEAVES) >= 5) {
				qs.playSound("ItemSound.quest_middle");
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		} else if (npcId == SUKAR_WERERAT_LEADER && qs.getQuestItemsCount(LUCKY_POTPOURI) > 0 && qs.getQuestItemsCount(AMETHYST) < 2) {
			qs.giveItems(AMETHYST, 1);
			if (qs.getQuestItemsCount(AMETHYST) >= 2) {
				qs.playSound("ItemSound.quest_middle");
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		}
		return null;
	}
}
