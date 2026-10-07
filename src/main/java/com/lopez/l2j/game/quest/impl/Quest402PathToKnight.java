package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 402: Path to Knight (1ª Troca de Classe do Human Fighter para Knight).
 */
@Component
public class Quest402PathToKnight extends Quest {

	public static final int QUEST_ID = 402;
	public static final String QUEST_NAME = "402_PathToKnight";

	// NPCs
	public static final int SIR_KLAUS_VASPER = 30417;
	public static final int BIOTIN = 30031;
	public static final int LEVIAN = 30037;
	public static final int GILBERT = 30039;
	public static final int COANE = 30289;
	public static final int SIR_AARON_TANFORD = 30653;

	// Itens
	public static final int MARK_OF_ESQUIRE = 1271;
	public static final int SWORD_OF_RITUAL = 1161;
	public static final int COIN_OF_LORDS1 = 1162;
	public static final int COIN_OF_LORDS2 = 1163;
	public static final int COIN_OF_LORDS3 = 1164;
	public static final int COIN_OF_LORDS4 = 1165;
	public static final int COIN_OF_LORDS5 = 1166;
	public static final int COIN_OF_LORDS6 = 1167;
	public static final int GLUDIO_GUARDS_MARK1 = 1168;
	public static final int BUGBEAR_NECKLACE = 1169;
	public static final int EINHASAD_CHURCH_MARK1 = 1170;
	public static final int EINHASAD_CRUCIFIX = 1171;
	public static final int GLUDIO_GUARDS_MARK2 = 1172;
	public static final int POISON_SPIDER_LEG1 = 1173;
	public static final int EINHASAD_CHURCH_MARK2 = 1174;
	public static final int LIZARDMAN_TOTEM = 1175;
	public static final int GLUDIO_GUARDS_MARK3 = 1176;
	public static final int GIANT_SPIDER_HUSK = 1177;
	public static final int EINHASAD_CHURCH_MARK3 = 1178;
	public static final int HORRIBLE_SKULL = 1179;

	// Drop config: mobId -> [item_required, item_reward, max, chance]
	private static final Map<Integer, int[]> DROPLIST = Map.of(
			20775, new int[]{GLUDIO_GUARDS_MARK1, BUGBEAR_NECKLACE, 10, 100},
			27024, new int[]{EINHASAD_CHURCH_MARK1, EINHASAD_CRUCIFIX, 12, 100},
			20038, new int[]{GLUDIO_GUARDS_MARK2, POISON_SPIDER_LEG1, 20, 100},
			20043, new int[]{GLUDIO_GUARDS_MARK2, POISON_SPIDER_LEG1, 20, 100},
			20050, new int[]{GLUDIO_GUARDS_MARK2, POISON_SPIDER_LEG1, 20, 100},
			20030, new int[]{EINHASAD_CHURCH_MARK2, LIZARDMAN_TOTEM, 20, 50},
			20027, new int[]{EINHASAD_CHURCH_MARK2, LIZARDMAN_TOTEM, 20, 100},
			20024, new int[]{EINHASAD_CHURCH_MARK2, LIZARDMAN_TOTEM, 20, 100},
			20103, new int[]{GLUDIO_GUARDS_MARK3, GIANT_SPIDER_HUSK, 20, 40},
			20404, new int[]{EINHASAD_CHURCH_MARK3, HORRIBLE_SKULL, 10, 100}
	);

	@Autowired
	public Quest402PathToKnight(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Human Knight");

		addStartNpc(SIR_KLAUS_VASPER);
		addTalkId(SIR_KLAUS_VASPER);
		addTalkId(BIOTIN);
		addTalkId(LEVIAN);
		addTalkId(GILBERT);
		addTalkId(COANE);
		addTalkId(SIR_AARON_TANFORD);

		for (int mobId : DROPLIST.keySet()) {
			addKillId(mobId);
		}

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

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		if ("30417-02a.htm".equalsIgnoreCase(event)) {
			if (c.classId() == 0) {
				if (c.level() >= 18) {
					if (qs.getQuestItemsCount(SWORD_OF_RITUAL) > 0) {
						return "30417-04.htm";
					} else {
						return "30417-05.htm";
					}
				} else {
					return "30417-02.htm";
				}
			} else if (c.classId() == 4) { // Já é Knight
				return "30417-02a.htm";
			} else {
				return "30417-03.htm";
			}
		} else if ("30417-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.giveItems(MARK_OF_ESQUIRE, 1);
			return "30417-08.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == SIR_KLAUS_VASPER) {
			if (cond == 0) {
				return "30417-01.htm";
			} else if (cond == 1) {
				long totalCoins = qs.getQuestItemsCount(COIN_OF_LORDS1) + qs.getQuestItemsCount(COIN_OF_LORDS2)
						+ qs.getQuestItemsCount(COIN_OF_LORDS3) + qs.getQuestItemsCount(COIN_OF_LORDS4)
						+ qs.getQuestItemsCount(COIN_OF_LORDS5) + qs.getQuestItemsCount(COIN_OF_LORDS6);

				if (totalCoins < 3) {
					return "30417-09.htm";
				} else {
					qs.takeItems(MARK_OF_ESQUIRE, -1);
					qs.takeItems(COIN_OF_LORDS1, -1);
					qs.takeItems(COIN_OF_LORDS2, -1);
					qs.takeItems(COIN_OF_LORDS3, -1);
					qs.takeItems(COIN_OF_LORDS4, -1);
					qs.takeItems(COIN_OF_LORDS5, -1);
					qs.takeItems(COIN_OF_LORDS6, -1);
					qs.giveItems(57, 81900);
					qs.giveItems(SWORD_OF_RITUAL, 1);
					qs.addExpAndSp(295862, 16814);
					qs.exitQuest(false);
					qs.playSound(QuestState.SOUND_FINISH);
					return "30417-13.htm";
				}
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int[] drop = DROPLIST.get(npc.npcId());
		if (drop != null) {
			int requiredItem = drop[0];
			int rewardItem = drop[1];
			int maxCount = drop[2];
			int chance = drop[3];

			if (qs.getQuestItemsCount(requiredItem) > 0 && qs.getQuestItemsCount(rewardItem) < maxCount) {
				if (ThreadLocalRandom.current().nextInt(100) < chance) {
					qs.giveItems(rewardItem, 1);
					if (qs.getQuestItemsCount(rewardItem) >= maxCount) {
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
