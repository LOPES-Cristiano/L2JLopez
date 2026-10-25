package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 234: Fate's Whisper (1º Passo Obrigatório para Subclasse no Lineage II Interlude).
 * Concede a lendária Star of Destiny (5011).
 */
@Component
public class Quest234FatesWhisper extends Quest {

	public static final int QUEST_ID = 234;
	public static final String QUEST_NAME = "234_FatesWhisper";

	// NPCs
	public static final int REORIN = 31002;
	public static final int CLIFF = 30182;
	public static final int FERRIS = 30847;
	public static final int ZENKIN = 30178;
	public static final int KASPAR = 30833;
	public static final int CABRIO_COFFER = 31027;
	public static final int KERNON_CHEST = 31028;
	public static final int GOLKONDA_CHEST = 31029;
	public static final int HALLATE_CHEST = 31030;

	// Monstros / Raid Bosses
	public static final int CABRIO = 25035;
	public static final int KERNON = 25054;
	public static final int GOLKONDA = 25126;
	public static final int HALLATE = 25220;
	public static final int BAIUM = 29020;

	// Itens
	public static final int PIPETTE_KNIFE = 4665;
	public static final int REIRIAS_SOUL_ORB = 4666;
	public static final int KERMONS_INFERNIUM_SCEPTER = 4667;
	public static final int GOLCONDAS_INFERNIUM_SCEPTER = 4668;
	public static final int HALLATES_INFERNIUM_SCEPTER = 4669;
	public static final int REORINS_HAMMER = 4670;
	public static final int REORINS_MOLD = 4671;
	public static final int INFERNIUM_VARNISH = 4672;
	public static final int RED_PIPETTE_KNIFE = 4673;
	public static final int STAR_OF_DESTINY = 5011;
	public static final int CRYSTAL_B = 1460;

	private static final Map<Integer, Integer> CHEST_SPAWN = Map.of(
			CABRIO, CABRIO_COFFER,
			KERNON, KERNON_CHEST,
			GOLKONDA, GOLKONDA_CHEST,
			HALLATE, HALLATE_CHEST
	);

	@Autowired
	public Quest234FatesWhisper(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Fate's Whisper");

		addStartNpc(REORIN);
		addTalkId(REORIN, CLIFF, FERRIS, ZENKIN, KASPAR, CABRIO_COFFER, KERNON_CHEST, GOLKONDA_CHEST, HALLATE_CHEST);

		addAttackId(BAIUM);
		addKillId(CABRIO, KERNON, GOLKONDA, HALLATE, BAIUM);

		registerQuestItems(PIPETTE_KNIFE, REIRIAS_SOUL_ORB, KERMONS_INFERNIUM_SCEPTER,
				GOLCONDAS_INFERNIUM_SCEPTER, HALLATES_INFERNIUM_SCEPTER, REORINS_HAMMER,
				REORINS_MOLD, INFERNIUM_VARNISH, RED_PIPETTE_KNIFE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "31002-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31002-03.htm";
		} else if ("30182-01c.htm".equalsIgnoreCase(event)) {
			qs.giveItems(INFERNIUM_VARNISH, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30182-01c.htm";
		} else if ("30178-01a.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30178-01a.htm";
		} else if ("30833-01b.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.giveItems(PIPETTE_KNIFE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30833-01b.htm";
		} else if (event.startsWith("selectBGrade_")) {
			String bGradeId = event.replace("selectBGrade_", "");
			qs.set("weaponId", bGradeId);
			return "31002-13.htm";
		} else if (event.startsWith("confirmWeapon")) {
			qs.set("bypass", "1");
			qs.setCond(11);
			return "31002-14.htm";
		} else if (event.startsWith("selectAGrade_")
				|| "selectAGrade_150".equalsIgnoreCase(event) || "selectAGrade_212".equalsIgnoreCase(event)
				|| "selectAGrade_235".equalsIgnoreCase(event) || "selectAGrade_2504".equalsIgnoreCase(event)
				|| "selectAGrade_269".equalsIgnoreCase(event) || "selectAGrade_288".equalsIgnoreCase(event)
				|| "selectAGrade_5233".equalsIgnoreCase(event) || "selectAGrade_7884".equalsIgnoreCase(event)
				|| "selectAGrade_7894".equalsIgnoreCase(event) || "selectAGrade_7899".equalsIgnoreCase(event)
				|| "selectAGrade_80".equalsIgnoreCase(event) || "selectAGrade_98".equalsIgnoreCase(event)) {
			if (qs.getInt("bypass") == 1) {
				int itemId = qs.getInt("weaponId");
				if (itemId > 0 && qs.hasQuestItems(itemId)) {
					int aGradeItemId = Integer.parseInt(event.replace("selectAGrade_", ""));
					qs.takeItems(itemId, 1);
					qs.giveItems(aGradeItemId, 1);
					qs.giveItems(STAR_OF_DESTINY, 1);
					qs.setCond(0);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "31002-12.htm";
				}
				return "31002-15.htm";
			}
			return "31002-16.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == REORIN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (c.level() >= 75) {
					return "31002-02.htm";
				} else {
					return "31002-01.htm";
				}
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(REIRIAS_SOUL_ORB) > 0) {
					qs.takeItems(REIRIAS_SOUL_ORB, -1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-05.htm";
				}
				return "31002-04.htm";
			} else if (cond == 2) {
				if (qs.getQuestItemsCount(KERMONS_INFERNIUM_SCEPTER) > 0
						&& qs.getQuestItemsCount(GOLCONDAS_INFERNIUM_SCEPTER) > 0
						&& qs.getQuestItemsCount(HALLATES_INFERNIUM_SCEPTER) > 0) {
					qs.takeItems(KERMONS_INFERNIUM_SCEPTER, -1);
					qs.takeItems(GOLCONDAS_INFERNIUM_SCEPTER, -1);
					qs.takeItems(HALLATES_INFERNIUM_SCEPTER, -1);
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-06.htm";
				}
				return "31002-05a.htm";
			} else if (cond == 3) {
				if (qs.hasQuestItems(INFERNIUM_VARNISH)) {
					qs.takeItems(INFERNIUM_VARNISH, -1);
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-07.htm";
				}
				return "31002-06b.htm";
			} else if (cond == 4) {
				if (qs.hasQuestItems(REORINS_HAMMER)) {
					qs.takeItems(REORINS_HAMMER, -1);
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-08.htm";
				}
				return "31002-07b.htm";
			} else if (cond >= 5 && cond <= 7) {
				return "31002-08b.htm";
			} else if (cond == 8) {
				qs.takeItems(REORINS_MOLD, -1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31002-09.htm";
			} else if (cond == 9) {
				if (qs.getQuestItemsCount(CRYSTAL_B) >= 984) {
					qs.takeItems(CRYSTAL_B, 984);
					qs.setCond(10);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-BGradeList.htm";
				}
				return "31002-09b.htm";
			} else if (cond == 10) {
				if (qs.getInt("bypass") == 1) {
					int itemId = qs.getInt("weaponId");
					return qs.hasQuestItems(itemId) ? "31002-AGradeList.htm" : "31002-15.htm";
				}
				return "31002-BGradeList.htm";
			} else if (cond >= 11) {
				if (qs.getQuestItemsCount(CRYSTAL_B) >= 984) {
					qs.takeItems(CRYSTAL_B, 984);
					qs.giveItems(STAR_OF_DESTINY, 1);
					qs.giveItems(79, 1); // Sword of Damascus (Low A-Grade representativo)
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound(QuestState.SOUND_FINISH);
					return "31002-12.htm";
				}
				return "31002-11.htm";
			}
		} else if (npcId == CLIFF) {
			if (cond == 3) {
				return !qs.hasQuestItems(INFERNIUM_VARNISH) ? "30182-01.htm" : "30182-02.htm";
			}
		} else if (npcId == FERRIS) {
			if (cond == 4 && !qs.hasQuestItems(REORINS_HAMMER)) {
				qs.giveItems(REORINS_HAMMER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30847-01.htm";
			} else if (cond >= 4 && qs.hasQuestItems(REORINS_HAMMER)) {
				return "30847-02.htm";
			}
		} else if (npcId == ZENKIN) {
			if (cond == 5) {
				return "30178-01.htm";
			} else if (cond > 5) {
				return "30178-02.htm";
			}
		} else if (npcId == KASPAR) {
			if (cond == 6) {
				return "30833-01.htm";
			} else if (cond == 7) {
				if (qs.hasQuestItems(PIPETTE_KNIFE) && !qs.hasQuestItems(RED_PIPETTE_KNIFE)) {
					return "30833-02.htm";
				}
				qs.takeItems(RED_PIPETTE_KNIFE, -1);
				qs.giveItems(REORINS_MOLD, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30833-03.htm";
			} else if (cond > 7) {
				return "30833-04.htm";
			}
		} else if (npcId == CABRIO_COFFER && cond == 1) {
			if (qs.getQuestItemsCount(REIRIAS_SOUL_ORB) == 0) {
				qs.giveItems(REIRIAS_SOUL_ORB, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31027-01.htm";
			}
			return "31027-02.htm";
		} else if (npcId == KERNON_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(KERMONS_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(KERMONS_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31028-01.htm";
			}
			return "31028-02.htm";
		} else if (npcId == GOLKONDA_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(GOLCONDAS_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(GOLCONDAS_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31029-01.htm";
			}
			return "31029-02.htm";
		} else if (npcId == HALLATE_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(HALLATES_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(HALLATES_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31030-01.htm";
			}
			return "31030-02.htm";
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		if (npc.npcId() == BAIUM && qs.getCond() == 7 && qs.hasQuestItems(PIPETTE_KNIFE) && !qs.hasQuestItems(RED_PIPETTE_KNIFE)) {
			qs.takeItems(PIPETTE_KNIFE, 1);
			qs.giveItems(RED_PIPETTE_KNIFE, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		if (npcId == BAIUM && qs.getCond() == 7 && qs.getQuestItemsCount(PIPETTE_KNIFE) > 0) {
			qs.takeItems(PIPETTE_KNIFE, 1);
			qs.giveItems(RED_PIPETTE_KNIFE, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if (CHEST_SPAWN.containsKey(npcId)) {
			qs.addSpawn(CHEST_SPAWN.get(npcId), npc.x(), npc.y(), npc.z(), 120000);
		}
		return null;
	}
}
