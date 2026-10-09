package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 616 - 616_MagicalPowerofFire2
 */
@Component
public class Quest616MagicalPowerofFire2 extends Quest {

	public static final int bMi = 31558;
	public static final int bMd = 31379;
	public static final int bMj = 7244;
	public static final int bMg = 7243;
	public static final int bMk = 25306;
	private NpcInstance bMl = null;

	public Quest616MagicalPowerofFire2(QuestManager questManager) {
		super(616, "616_MagicalPowerofFire2", "616_MagicalPowerofFire2");
		this.addStartNpc(31379);
		this.addTalkId(31558);
		this.addKillId(25306);
		this.addQuestItem(7244);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

		String string2 = event;
		if (event.equalsIgnoreCase("quest_accept")) {
			string2 = "shaman_udan_q0616_0104.htm";
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("616_1")) {
			if (Math.max(0L, 0L) + 10800000L > System.currentTimeMillis()) {
				string2 = "totem_of_ketra_q0616_0204.htm";
			} else if (qs.getQuestItemsCount(7243) >= 1L && (this.bMl == null || this.bMl.isDead())) {
				qs.takeItems(7243, 1L);
				this.bMl = qs.addSpawn(25306, 142528, -82528, -6496);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				string2 = "totem_of_ketra_q0616_0203.htm";
			}
		} else if (event.equalsIgnoreCase("616_3")) {
			if (qs.getQuestItemsCount(7244) >= 1L) {
				qs.takeItems(7244, -1L);
				qs.addExpAndSp(10000L, 0L);
				qs.playSound(QuestState.SOUND_FINISH);
				string2 = "shaman_udan_q0616_0301.htm";
				qs.exitQuest(true);
			} else {
				string2 = "shaman_udan_q0616_0302.htm";
			}
		}
		return string2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getCond();
		switch (n) {
			case 31379: {
				if (n2 == 0) {
					if (pc.getLevel() >= 75) {
						if (qs.getQuestItemsCount(7243) >= 1L) {
							html = "shaman_udan_q0616_0101.htm";
							break;
						}
						html = "shaman_udan_q0616_0102.htm";
						qs.exitQuest(true);
						break;
					}
					html = "shaman_udan_q0616_0103.htm";
					qs.exitQuest(true);
					break;
				}
				if (n2 == 1) {
					html = "shaman_udan_q0616_0105.htm";
					break;
				}
				if (n2 == 2) {
					html = "shaman_udan_q0616_0202.htm";
					break;
				}
				if (n2 != 3 || qs.getQuestItemsCount(7244) < 1L) break;
				html = "shaman_udan_q0616_0201.htm";
				break;
			}
			case 31558: {
				if (Math.max(0L, 0L) + 10800000L > System.currentTimeMillis()) {
					html = "totem_of_ketra_q0616_0204.htm";
					break;
				}
				if (this.bMl != null && !this.bMl.isDead()) {
					html = "totem_of_ketra_q0616_0202.htm";
					break;
				}
				if (n2 == 1) {
					html = "totem_of_ketra_q0616_0101.htm";
					break;
				}
				if (n2 != 2) break;
				if (this.bMl == null || this.bMl.isDead()) {
					this.bMl = qs.addSpawn(25306, 142528, -82528, -6496);
					html = "totem_of_ketra_q0616_0204.htm";
					break;
				}
				html = "<html><body>Already in spawn.</body></html>";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getQuestItemsCount(7244) == 0L) {
			qs.giveItems(7244, 1L);
			qs.setCond(3);
			if (this.bMl != null) {
				this.bMl.deleteMe();
			}
			this.bMl = null;
		}
		return null;
	}

}
