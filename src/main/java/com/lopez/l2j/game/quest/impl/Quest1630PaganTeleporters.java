package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 1630_PaganTeleporters: Abertura das portas e espelhos de teleporte do Pagan Temple
 * verificando Visitor's Mark (8064), Faded Visitor's Mark (8065) e Pagan's Mark (8067).
 */
@Component
public class Quest1630PaganTeleporters extends Quest {

	public static final int VISITOR_MARK = 8064;
	public static final int FADED_VISITOR_MARK = 8065;
	public static final int PAGAN_MARK = 8067;

	public static final int DOOR_1 = 19160001;
	public static final int DOOR_2A = 19160010;
	public static final int DOOR_2B = 19160011;

	private final DoorTable doorTable;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("PaganDoor-Scheduler").unstarted(r);
		return t;
	});

	public Quest1630PaganTeleporters() {
		this(null, null);
	}

	public Quest1630PaganTeleporters(QuestManager questManager) {
		this(questManager, null);
	}

	@Autowired
	public Quest1630PaganTeleporters(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) DoorTable doorTable) {
		super(1630, "1630_PaganTeleporters", "Teleports");
		this.doorTable = doorTable;

		for (int npcId : new int[] { 32034, 32035, 32036, 32037 }) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		for (int npcId : new int[] { 32039, 32040 }) {
			addStartNpc(npcId);
			addFirstTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		DoorTable dt = doorTable != null ? doorTable : DoorTable.getInstance();
		if (dt != null) {
			if ("Close_Door1".equalsIgnoreCase(event)) {
				dt.closeDoor(DOOR_1);
			} else if ("Close_Door2".equalsIgnoreCase(event)) {
				dt.closeDoor(DOOR_2A);
				dt.closeDoor(DOOR_2B);
			}
		}
		return null;
	}

	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		if (npc == null || qs == null) {
			return "";
		}
		int npcId = npc.npcId();
		if (npcId == 32039 && qs.getPlayer() != null) {
			qs.getPlayer().teleportToLocation(-12766, -35840, -10856);
		} else if (npcId == 32040 && qs.getPlayer() != null) {
			qs.getPlayer().teleportToLocation(34962, -49758, -763);
		}
		qs.exitQuest(true);
		return "";
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (npc == null || qs == null) {
			return null;
		}
		int npcId = npc.npcId();
		DoorTable dt = doorTable != null ? doorTable : DoorTable.getInstance();
		String htmltext = null;

		if (npcId == 32034) {
			if (qs.getQuestItemsCount(VISITOR_MARK) == 0
					&& qs.getQuestItemsCount(FADED_VISITOR_MARK) == 0
					&& qs.getQuestItemsCount(PAGAN_MARK) == 0) {
				return "Mark1.htm";
			} else {
				if (dt != null) {
					dt.openDoor(DOOR_1);
				}
				scheduler.schedule(() -> onAdvEvent("Close_Door1", null, null), 10, TimeUnit.SECONDS);
				htmltext = "FadedMark.htm";
			}
		} else if (npcId == 32035) {
			if (dt != null) {
				dt.openDoor(DOOR_1);
			}
			scheduler.schedule(() -> onAdvEvent("Close_Door1", null, null), 10, TimeUnit.SECONDS);
			htmltext = "FadedMark.htm";
		} else if (npcId == 32036) {
			if (qs.getQuestItemsCount(PAGAN_MARK) == 0) {
				return "Mark2.htm";
			} else {
				if (dt != null) {
					dt.openDoor(DOOR_2A);
					dt.openDoor(DOOR_2B);
				}
				scheduler.schedule(() -> onAdvEvent("Close_Door2", null, null), 10, TimeUnit.SECONDS);
				htmltext = "Mark3.htm";
			}
		} else if (npcId == 32037) {
			if (dt != null) {
				dt.openDoor(DOOR_2A);
				dt.openDoor(DOOR_2B);
			}
			scheduler.schedule(() -> onAdvEvent("Close_Door2", null, null), 10, TimeUnit.SECONDS);
			htmltext = "FadedMark.htm";
		}

		qs.exitQuest(true);
		return htmltext;
	}
}
