package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 274 - Skirmish With The Werewolves
 */
@Component
public class Quest274SkirmishWithTheWerewolves extends Quest {

	public static final int BRUKURSE = 30569;
	public static final int MARAKU_WEREWOLF_HEAD = 1477;
	public static final int NECKLACE_OF_VALOR = 1507;
	public static final int NECKLACE_OF_COURAGE = 1506;
	public static final int MARAKU_WOLFMEN_TOTEM = 1501;

	public static final int beh = 1477;
	public static final int bei = 1507;
	public static final int bej = 1506;
	public static final int bek = 1501;

	public Quest274SkirmishWithTheWerewolves(QuestManager questManager) {
		super(274, "274_SkirmishWithTheWerewolves", "Skirmish With The Werewolves");
		addStartNpc(30569);
		addTalkId(30569);
		addKillId(20363, 20364);
		registerQuestItems(1477, 1501);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

		String event2 = event;
		if (event.equalsIgnoreCase("prefect_brukurse_q0274_03.htm") || event.equalsIgnoreCase("30569-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		return event2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = qs.getStateId();
		int n2 = qs.getCond();
		if (n == 1) {
			if (pc.race() != 3) {
				html = "prefect_brukurse_q0274_00.htm";
				qs.exitQuest(true);
			} else if (pc.level() < 9) {
				html = "prefect_brukurse_q0274_01.htm";
				qs.exitQuest(true);
			} else {
				if (qs.getQuestItemsCount(NECKLACE_OF_VALOR) > 0 || qs.getQuestItemsCount(NECKLACE_OF_COURAGE) > 0) {
					html = "prefect_brukurse_q0274_02.htm";
					return html;
				}
				html = "prefect_brukurse_q0274_07.htm";
			}
		} else if (n2 == 1) {
			html = "prefect_brukurse_q0274_04.htm";
		} else if (n2 == 2) {
			if (qs.getQuestItemsCount(MARAKU_WEREWOLF_HEAD) < 40) {
				html = "prefect_brukurse_q0274_04.htm";
			} else {
				qs.takeItems(MARAKU_WEREWOLF_HEAD, -1);
				qs.giveItems(57, 3500, true);
				if (qs.getQuestItemsCount(MARAKU_WOLFMEN_TOTEM) >= 1) {
					qs.giveItems(57, qs.getQuestItemsCount(MARAKU_WOLFMEN_TOTEM) * 600, true);
					qs.takeItems(MARAKU_WOLFMEN_TOTEM, -1);
				}
				html = "prefect_brukurse_q0274_05.htm";
				qs.exitQuest(true);
				qs.playSound(QuestState.SOUND_FINISH);
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1 && qs.getQuestItemsCount(MARAKU_WEREWOLF_HEAD) < 40) {
			if (qs.getQuestItemsCount(MARAKU_WEREWOLF_HEAD) < 39) {
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else {
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.setCond(2);
			}
			qs.giveItems(MARAKU_WEREWOLF_HEAD, 1);
		}
		if (ThreadLocalRandom.current().nextInt(100) < 5) {
			qs.giveItems(MARAKU_WOLFMEN_TOTEM, 1);
		}
		return null;
	}
}
