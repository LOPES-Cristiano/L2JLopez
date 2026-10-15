package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 31: Secret Buried in the Swamp
 * Mercenary Supplier Abercrombie, Corpse of Dwarf e Forgotten Monuments (31661-31664).
 */
@Component
public class Quest031SecretBuriedInTheSwamp extends Quest {

	public static final int QUEST_ID = 31;
	public static final String QUEST_NAME = "031_SecretBuriedintheSwamp";

	public static final int START_NPC = 31555;
	public static final int ABERCROMBIE = 31555;
	public static final int FORGOTTEN_MONUMENT_1 = 31661;
	public static final int FORGOTTEN_MONUMENT_2 = 31662;
	public static final int FORGOTTEN_MONUMENT_3 = 31663;
	public static final int FORGOTTEN_MONUMENT_4 = 31664;
	public static final int CORPSE_OF_DWARF = 31665;

	public static final int KRORINS_JOURNAL = 7252;

	@Autowired
	public Quest031SecretBuriedInTheSwamp(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Secret Buried in the Swamp");
		addStartNpc(ABERCROMBIE);
		addTalkId(ABERCROMBIE, FORGOTTEN_MONUMENT_1, FORGOTTEN_MONUMENT_2, FORGOTTEN_MONUMENT_3, FORGOTTEN_MONUMENT_4, CORPSE_OF_DWARF);
		registerQuestItems(KRORINS_JOURNAL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "31555-1.htm".equalsIgnoreCase(event) || "31555-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "supplier_abercrombie_q0031_0104.htm";
		} else if ("31665-1.htm".equalsIgnoreCase(event) || "31665-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(KRORINS_JOURNAL, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("31555-4.htm".equalsIgnoreCase(event) || "31555-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("31661-1.htm".equalsIgnoreCase(event) || "31661-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("31662-1.htm".equalsIgnoreCase(event) || "31662-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("31663-0a.htm".equalsIgnoreCase(event) || "31663-00a.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("31663-1.htm".equalsIgnoreCase(event) || "31663-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("31664-1.htm".equalsIgnoreCase(event) || "31664-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.giveItems(KRORINS_JOURNAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "dead_dwarf_q0031_0201.htm";
			} else if (cond == 2 && qs.hasQuestItems(KRORINS_JOURNAL)) {
				qs.takeItems(KRORINS_JOURNAL, -1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "supplier_abercrombie_q0031_0301.htm";
			} else if (cond == 3) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "old_slate1_q0031_0401.htm";
			} else if (cond == 4) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "old_slate2_q0031_0501.htm";
			} else if (cond == 5) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "old_slate3_q0031_0601.htm";
			} else if (cond == 6) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "old_slate4_q0031_0701.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event) || "31555-7.htm".equalsIgnoreCase(event) || "31555-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KRORINS_JOURNAL, -1);
			qs.giveItems(57, 40000);
			qs.addExpAndSp(130000, 0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "supplier_abercrombie_q0031_0801.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case ABERCROMBIE:
				if (cond == 0) {
					if (pc.level() >= 66) return "supplier_abercrombie_q0031_0101.htm";
					qs.exitQuest(true);
					return "supplier_abercrombie_q0031_0103.htm";
				} else if (cond == 1) {
					return "supplier_abercrombie_q0031_0105.htm";
				} else if (cond == 2 && qs.hasQuestItems(KRORINS_JOURNAL)) {
					return "supplier_abercrombie_q0031_0201.htm";
				} else if (cond >= 3 && cond <= 6) {
					return "supplier_abercrombie_q0031_0302.htm";
				} else if (cond == 7) {
					return "supplier_abercrombie_q0031_0701.htm";
				}
				break;
			case CORPSE_OF_DWARF:
				if (cond == 1) {
					return "dead_dwarf_q0031_0101.htm";
				} else if (cond >= 2) {
					return "dead_dwarf_q0031_0203.htm";
				}
				break;
			case FORGOTTEN_MONUMENT_1:
				if (cond == 3) {
					return "old_slate1_q0031_0301.htm";
				} else if (cond > 3) {
					return "old_slate1_q0031_0402.htm";
				}
				break;
			case FORGOTTEN_MONUMENT_2:
				if (cond == 4) {
					return "old_slate2_q0031_0401.htm";
				} else if (cond > 4) {
					return "old_slate2_q0031_0502.htm";
				}
				break;
			case FORGOTTEN_MONUMENT_3:
				if (cond == 5) {
					return "old_slate3_q0031_0501.htm";
				} else if (cond > 5) {
					return "old_slate3_q0031_0602.htm";
				}
				break;
			case FORGOTTEN_MONUMENT_4:
				if (cond == 6) {
					return "old_slate4_q0031_0601.htm";
				} else if (cond > 6) {
					return "old_slate4_q0031_0702.htm";
				}
				break;
		}
		return "noquest";
	}
}
