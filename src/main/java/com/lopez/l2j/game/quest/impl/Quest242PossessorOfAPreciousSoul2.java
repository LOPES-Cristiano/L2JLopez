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
 * Quest 242: Possessor of a Precious Soul - Part 2
 * Resgate do unicórnio sagrado e obtenção da Caradine's Letter (Parte 3).
 */
@Component
public class Quest242PossessorOfAPreciousSoul2 extends Quest {

	public static final int QUEST_ID = 242;
	public static final String QUEST_NAME = "242_PossessorOfAPreciousSoul_2";

	// NPCs
	public static final int VIRGIL = 31742;
	public static final int KASSANDRA = 31743;
	public static final int OGMAR = 31744;
	public static final int FALLEN_UNICORN = 31746;
	public static final int PURE_UNICORN = 31747;
	public static final int CORNERSTONE = 31748;
	public static final int MYSTERIOUS_KNIGHT = 31751;
	public static final int ANGEL_CORPSE = 31752;
	public static final int KALIS = 30759;
	public static final int MATILD = 30738;

	// Monstros
	public static final int RESTRAINER_OF_GLORY = 27317;

	// Itens
	public static final int VIRGILS_LETTER = 7677;
	public static final int GOLDEN_HAIR = 7590;
	public static final int ORB_OF_BINDING = 7595;
	public static final int SORCERY_INGREDIENT = 7596;
	public static final int CARADINE_LETTER = 7678;

	@Autowired
	public Quest242PossessorOfAPreciousSoul2(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Possessor of a Precious Soul - Part 2");

		addStartNpc(VIRGIL);
		addTalkId(VIRGIL, KASSANDRA, OGMAR, FALLEN_UNICORN, PURE_UNICORN, CORNERSTONE,
				MYSTERIOUS_KNIGHT, ANGEL_CORPSE, KALIS, MATILD);

		addKillId(RESTRAINER_OF_GLORY);

		registerQuestItems(GOLDEN_HAIR, ORB_OF_BINDING, SORCERY_INGREDIENT);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("31742-3.htm".equalsIgnoreCase(event)) {
			if (cond == 0) {
				qs.setState(State.STARTED);
				qs.takeItems(VIRGILS_LETTER, 1);
				qs.setCond(1);
				qs.playSound(QuestState.SOUND_ACCEPT);
			}
			return event;
		} else if ("31743-5.htm".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31744-2.htm".equalsIgnoreCase(event)) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31751-2.htm".equalsIgnoreCase(event)) {
			if (cond == 3) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30759-2.htm".equalsIgnoreCase(event)) {
			if (cond == 6) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30738-2.htm".equalsIgnoreCase(event)) {
			if (cond == 7) {
				qs.setCond(8);
				qs.giveItems(SORCERY_INGREDIENT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30759-5.htm".equalsIgnoreCase(event)) {
			if (cond == 8) {
				qs.setCond(9);
				qs.takeItems(GOLDEN_HAIR, -1);
				qs.takeItems(SORCERY_INGREDIENT, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
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

		if (npcId == VIRGIL) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() < 60) {
					return "31742-2.htm";
				} else if (qs.hasQuestItems(VIRGILS_LETTER)) {
					return "31742-1.htm";
				} else {
					return "31742-2.htm";
				}
			} else if (cond == 1) {
				return "31742-4.htm";
			} else if (cond == 11) {
				qs.setCond(0);
				qs.giveItems(CARADINE_LETTER, 1);
				qs.addExpAndSp(455764, 38047);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "31742-6.htm";
			}
		} else if (npcId == KASSANDRA) {
			if (cond == 1) {
				return "31743-1.htm";
			} else if (cond == 2) {
				return "31743-6.htm";
			} else if (cond == 11) {
				return "31743-7.htm";
			}
		} else if (npcId == OGMAR) {
			if (cond == 2) {
				return "31744-1.htm";
			} else if (cond == 3) {
				return "31744-3.htm";
			}
		} else if (npcId == MYSTERIOUS_KNIGHT) {
			if (cond == 3) {
				return "31751-1.htm";
			} else if (cond == 4) {
				return "31751-3.htm";
			} else if (cond == 5 && qs.hasQuestItems(GOLDEN_HAIR)) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31751-4.htm";
			} else if (cond == 6) {
				return "31751-5.htm";
			}
		} else if (npcId == ANGEL_CORPSE) {
			if (cond == 4) {
				int talk = qs.getInt("talk");
				if (talk < 3) {
					qs.set("talk", talk + 1);
					return "31752-2.htm";
				} else {
					qs.setCond(5);
					qs.giveItems(GOLDEN_HAIR, 1);
					qs.unset("talk");
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31752-1.htm";
				}
			} else if (cond == 5) {
				return "31752-2.htm";
			}
		} else if (npcId == KALIS) {
			if (cond == 6) {
				return "30759-1.htm";
			} else if (cond == 7) {
				return "30759-3.htm";
			} else if (cond == 8 && qs.hasQuestItems(SORCERY_INGREDIENT)) {
				return "30759-4.htm";
			} else if (cond == 9) {
				return "30759-6.htm";
			}
		} else if (npcId == MATILD) {
			if (cond == 7) {
				return "30738-1.htm";
			} else if (cond == 8) {
				return "30738-3.htm";
			}
		} else if (npcId == CORNERSTONE) {
			if (cond == 9) {
				if (!qs.hasQuestItems(ORB_OF_BINDING)) {
					return "31748-1.htm";
				} else {
					qs.takeItems(ORB_OF_BINDING, 1);
					int cornerstones = qs.getInt("cornerstones") + 1;
					qs.set("cornerstones", cornerstones);
					qs.playSound(QuestState.SOUND_MIDDLE);
					if (cornerstones >= 4) {
						qs.setCond(10);
						qs.playSound(QuestState.SOUND_MIDDLE);
					}
					return "31748-2.htm";
				}
			}
		} else if (npcId == FALLEN_UNICORN) {
			if (cond == 9) {
				return "31746-1.htm";
			} else if (cond == 10) {
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31746-2.htm";
			}
		} else if (npcId == PURE_UNICORN) {
			if (cond == 10) {
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31747-1.htm";
			} else if (cond == 11) {
				return "31747-2.htm";
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

		if (cond == 9 && npcId == RESTRAINER_OF_GLORY) {
			if (qs.getQuestItemsCount(ORB_OF_BINDING) < 4) {
				if (ThreadLocalRandom.current().nextInt(100) < 60) {
					qs.giveItems(ORB_OF_BINDING, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}
