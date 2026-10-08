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
 * Quest 412: Path to Dark Wizard (1ª Troca de Classe do Dark Mystic para Dark Wizard).
 */
@Component
public class Quest412PathToDarkWizard extends Quest {

	public static final int QUEST_ID = 412;
	public static final String QUEST_NAME = "412_PathToDarkwizard";

	// NPCs
	public static final int VARIKA = 30421;
	public static final int CHARKEREN = 30415;
	public static final int ANNIKA = 30418;
	public static final int ARKENIA = 30419;

	// Monstros
	public static final int MARSH_ZOMBIE = 20015;
	public static final int SKELETON_SCOUT = 20022;
	public static final int MISERY_SKELETON = 20045;
	public static final int SKELETON_HUNTER = 20517;
	public static final int SKELETON_HUNTER_ARCHER = 20518;

	// Itens
	public static final int SEEDS_OF_ANGER = 1253;
	public static final int SEEDS_OF_DESPAIR = 1254;
	public static final int SEEDS_OF_HORROR = 1255;
	public static final int SEEDS_OF_LUNACY = 1256;
	public static final int FAMILYS_ASHES = 1257;
	public static final int KNEE_BONE = 1259;
	public static final int HEART_OF_LUNACY = 1260;
	public static final int JEWEL_OF_DARKNESS = 1261;
	public static final int LUCKY_KEY = 1277;
	public static final int CANDLE = 1278;
	public static final int HUB_SCENT = 1279;

	@Autowired
	public Quest412PathToDarkWizard(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Dark Wizard");

		addStartNpc(VARIKA);
		addTalkId(VARIKA);
		addTalkId(CHARKEREN);
		addTalkId(ANNIKA);
		addTalkId(ARKENIA);

		addKillId(MARSH_ZOMBIE);
		addKillId(SKELETON_SCOUT);
		addKillId(MISERY_SKELETON);
		addKillId(SKELETON_HUNTER);
		addKillId(SKELETON_HUNTER_ARCHER);

		registerQuestItems(SEEDS_OF_ANGER, SEEDS_OF_DESPAIR, SEEDS_OF_HORROR, SEEDS_OF_LUNACY,
				FAMILYS_ASHES, KNEE_BONE, HEART_OF_LUNACY, LUCKY_KEY, CANDLE, HUB_SCENT);

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
			if (level >= 18 && classId == 0x26 && qs.getQuestItemsCount(JEWEL_OF_DARKNESS) == 0) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				qs.giveItems(SEEDS_OF_DESPAIR, 1);
				return "30421-05.htm";
			} else if (classId != 0x26) {
				return classId == 0x27 ? "30421-02a.htm" : "30421-03.htm";
			} else if (level < 18 && classId == 0x26) {
				return "30421-02.htm";
			} else if (qs.getQuestItemsCount(JEWEL_OF_DARKNESS) > 0) {
				return "30421-04.htm";
			}
		} else if ("412_1".equalsIgnoreCase(event)) {
			return qs.getQuestItemsCount(SEEDS_OF_ANGER) > 0 ? "30421-06.htm" : "30421-07.htm";
		} else if ("412_2".equalsIgnoreCase(event)) {
			return qs.getQuestItemsCount(SEEDS_OF_HORROR) > 0 ? "30421-09.htm" : "30421-10.htm";
		} else if ("412_3".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(SEEDS_OF_LUNACY) > 0) {
				return "30421-12.htm";
			} else if (qs.getQuestItemsCount(SEEDS_OF_DESPAIR) > 0) {
				qs.giveItems(HUB_SCENT, 1);
				return "30421-13.htm";
			}
		} else if ("412_4".equalsIgnoreCase(event)) {
			qs.giveItems(LUCKY_KEY, 1);
			return "30415-03.htm";
		} else if ("30418_1".equalsIgnoreCase(event)) {
			qs.giveItems(CANDLE, 1);
			return "30418-02.htm";
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

		if (npcId == VARIKA) {
			if (cond == 0) {
				return qs.getQuestItemsCount(JEWEL_OF_DARKNESS) == 0 ? "30421-01.htm" : "30421-04.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(SEEDS_OF_DESPAIR) > 0
						&& qs.getQuestItemsCount(SEEDS_OF_HORROR) > 0
						&& qs.getQuestItemsCount(SEEDS_OF_LUNACY) > 0
						&& qs.getQuestItemsCount(SEEDS_OF_ANGER) > 0) {
					qs.takeItems(SEEDS_OF_HORROR, 1);
					qs.takeItems(SEEDS_OF_ANGER, 1);
					qs.takeItems(SEEDS_OF_LUNACY, 1);
					qs.takeItems(SEEDS_OF_DESPAIR, 1);
					qs.rewardItems(57, 81900);
					qs.giveItems(JEWEL_OF_DARKNESS, 1);
					qs.addExpAndSp(295862, 17664);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30421-16.htm";
				} else {
					return "30421-17.htm";
				}
			}
		} else if (npcId == ARKENIA && cond == 1) {
			if (qs.getQuestItemsCount(HUB_SCENT) == 0 && qs.getQuestItemsCount(HEART_OF_LUNACY) == 0) {
				qs.giveItems(HUB_SCENT, 1);
				return "30419-01.htm";
			} else if (qs.getQuestItemsCount(HUB_SCENT) > 0 && qs.getQuestItemsCount(HEART_OF_LUNACY) < 3) {
				return "30419-02.htm";
			} else if (qs.getQuestItemsCount(HUB_SCENT) > 0 && qs.getQuestItemsCount(HEART_OF_LUNACY) >= 3) {
				qs.giveItems(SEEDS_OF_LUNACY, 1);
				qs.takeItems(HEART_OF_LUNACY, 3);
				qs.takeItems(HUB_SCENT, 1);
				return "30419-03.htm";
			}
		} else if (npcId == CHARKEREN && cond == 1) {
			if (qs.getQuestItemsCount(SEEDS_OF_ANGER) == 0) {
				if (qs.getQuestItemsCount(FAMILYS_ASHES) == 0 && qs.getQuestItemsCount(LUCKY_KEY) == 0) {
					return "30415-01.htm";
				} else if (qs.getQuestItemsCount(FAMILYS_ASHES) < 3 && qs.getQuestItemsCount(LUCKY_KEY) == 1) {
					return "30415-04.htm";
				} else if (qs.getQuestItemsCount(FAMILYS_ASHES) >= 3 && qs.getQuestItemsCount(LUCKY_KEY) == 1) {
					qs.giveItems(SEEDS_OF_ANGER, 1);
					qs.takeItems(FAMILYS_ASHES, 3);
					qs.takeItems(LUCKY_KEY, 1);
					return "30415-05.htm";
				}
			} else {
				return "30415-06.htm";
			}
		} else if (npcId == ANNIKA && cond > 0 && qs.getQuestItemsCount(SEEDS_OF_HORROR) == 0) {
			if (qs.getQuestItemsCount(CANDLE) == 0 && qs.getQuestItemsCount(KNEE_BONE) == 0) {
				return "30418-01.htm";
			} else if (qs.getQuestItemsCount(CANDLE) == 1 && qs.getQuestItemsCount(KNEE_BONE) < 2) {
				return "30418-03.htm";
			} else if (qs.getQuestItemsCount(CANDLE) == 1 && qs.getQuestItemsCount(KNEE_BONE) >= 2) {
				qs.giveItems(SEEDS_OF_HORROR, 1);
				qs.takeItems(CANDLE, 1);
				qs.takeItems(KNEE_BONE, 2);
				return "30418-04.htm";
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

		if (npcId == MARSH_ZOMBIE) {
			if (cond == 1 && qs.getQuestItemsCount(LUCKY_KEY) == 1 && qs.getQuestItemsCount(FAMILYS_ASHES) < 3) {
				if (ThreadLocalRandom.current().nextInt(2) == 0) {
					qs.giveItems(FAMILYS_ASHES, 1);
					if (qs.getQuestItemsCount(FAMILYS_ASHES) == 3) {
						qs.playSound("ItemSound.quest_middle");
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				}
			}
		} else if (npcId == SKELETON_HUNTER || npcId == SKELETON_HUNTER_ARCHER || npcId == SKELETON_SCOUT) {
			if (cond == 1 && qs.getQuestItemsCount(CANDLE) == 1 && qs.getQuestItemsCount(KNEE_BONE) < 2) {
				if (ThreadLocalRandom.current().nextInt(2) == 0) {
					qs.giveItems(KNEE_BONE, 1);
					if (qs.getQuestItemsCount(KNEE_BONE) == 2) {
						qs.playSound("ItemSound.quest_middle");
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				}
			}
		} else if (npcId == MISERY_SKELETON) {
			if (cond == 1 && qs.getQuestItemsCount(HUB_SCENT) == 1 && qs.getQuestItemsCount(HEART_OF_LUNACY) < 3) {
				if (ThreadLocalRandom.current().nextInt(2) == 0) {
					qs.giveItems(HEART_OF_LUNACY, 1);
					if (qs.getQuestItemsCount(HEART_OF_LUNACY) == 3) {
						qs.playSound("ItemSound.quest_middle");
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				}
			}
		}
		return null;
	}
}
