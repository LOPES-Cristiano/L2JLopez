package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.WareHouseWithdrawalList;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 6051_VarkaSilenosSupport: Suporte da tribo Varka Silenos (31377..31383)
 * Buffs em troca de Nepenthese Seeds (7187), Warehouse e Teleport por ally level (-1 a -5).
 */
@Component
public class Quest6051VarkaSilenosSupport extends Quest {

	public static final int ASHAS = 31377;
	public static final int NARAN = 31378;
	public static final int UDAN = 31379;
	public static final int DIYABU = 31380;
	public static final int HAGOS = 31381;
	public static final int SHIKON = 31382;
	public static final int TERANU = 31383;

	public static final int SEED = 7187;

	public record BuffInfo(int skillId, int level, int cost) {}

	public static final Map<String, BuffInfo> BUFFS = Map.of(
			"1", new BuffInfo(4359, 1, 2), // Focus
			"2", new BuffInfo(4360, 1, 2), // Death Whisper
			"3", new BuffInfo(4345, 1, 3), // Might
			"4", new BuffInfo(4355, 1, 3), // Acumen
			"5", new BuffInfo(4352, 1, 3), // Berserker
			"6", new BuffInfo(4354, 1, 3), // Vampiric Rage
			"7", new BuffInfo(4356, 1, 6), // Empower
			"8", new BuffInfo(4357, 1, 6)  // Haste
	);

	private final SkillService skillService;

	@Autowired
	public Quest6051VarkaSilenosSupport(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) SkillService skillService) {
		super(6051, "6051_VarkaSilenosSupport", "custom");
		this.skillService = skillService;

		for (int i = 31377; i <= 31383; i++) {
			addFirstTalkId(i);
		}
		addTalkId(UDAN, HAGOS, TERANU);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		if (npc == null || qs == null) {
			return null;
		}
		PlayerCharacter player = qs.playerChar();
		int npcId = npc.npcId();
		int aLevel = player != null ? player.getAllianceWithVarkaKetra() : 0;
		long seeds = qs.getQuestItemsCount(SEED);

		if (npcId == ASHAS) {
			return aLevel < 0 ? "31377-friend.htm" : "31377-no.htm";
		} else if (npcId == NARAN) {
			return aLevel < 0 ? "31378-friend.htm" : "31378-no.htm";
		} else if (npcId == UDAN) {
			qs.setState(State.STARTED);
			if (aLevel > -1) {
				return "31379-3.htm";
			} else if (aLevel > -3) {
				return "31379-1.htm";
			} else {
				return seeds > 0 ? "31379-4.htm" : "31379-2.htm";
			}
		} else if (npcId == DIYABU) {
			if (player != null && player.karma() > 0) {
				return "31380-pk.htm";
			} else if (aLevel >= 0) {
				return "31380-no.htm";
			} else if (aLevel == -1 || aLevel == -2) {
				return "31380-1.htm";
			} else {
				return "31380-2.htm";
			}
		} else if (npcId == HAGOS) {
			if (aLevel >= 0) {
				return "31381-no.htm";
			} else if (aLevel == -1) {
				return "31381-1.htm";
			} else if (aLevel == -2 || aLevel == -3) {
				return "31381-2.htm";
			} else {
				return "31381-4.htm";
			}
		} else if (npcId == SHIKON) {
			if (aLevel == -2) {
				return "31382-1.htm";
			} else if (aLevel == -3 || aLevel == -4) {
				return "31382-2.htm";
			} else if (aLevel == -5) {
				return "31382-3.htm";
			} else {
				return "31382-no.htm";
			}
		} else if (npcId == TERANU) {
			if (aLevel >= 0) {
				return "31383-no.htm";
			} else if (aLevel > -4) {
				return "31383-1.htm";
			} else if (aLevel == -4) {
				return "31383-2.htm";
			} else {
				return "31383-3.htm";
			}
		}
		return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null) {
			return null;
		}
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		PlayerCharacter active = player.activeChar();
		int aLevel = active != null ? active.getAllianceWithVarkaKetra() : 0;

		if (BUFFS.containsKey(event)) {
			BuffInfo buff = BUFFS.get(event);
			if (qs.getQuestItemsCount(SEED) >= buff.cost()) {
				qs.takeItems(SEED, buff.cost());
				int npcObj = npc != null ? npc.objectId() : active.objectId();
				int nx = npc != null ? npc.x() : active.x();
				int ny = npc != null ? npc.y() : active.y();
				int nz = npc != null ? npc.z() : active.z();

				if (skillService != null && skillService.table() != null) {
					skillService.table().get(buff.skillId(), buff.level()).ifPresent(sk -> player.applySkillEffects(sk, false));
				} else if (active != null) {
					active.effects().addBuff(buff.skillId(), buff.level(), 7200_000L);
				}
				player.send(new MagicSkillUse(npcObj, active.objectId(), buff.skillId(), buff.level(), 500, 0,
						nx, ny, nz, active.x(), active.y(), active.z()));
				return "31379-4.htm";
			}
		} else if ("Withdraw".equalsIgnoreCase(event)) {
			var wh = player.context() != null ? player.context().warehouse() : null;
			var stored = wh != null ? wh.getWarehouseItems(active.objectId()) : java.util.List.<com.lopez.l2j.game.item.ItemInstance>of();
			if (stored.isEmpty()) {
				return "31381-3.htm";
			} else {
				player.send(new ActionFailed());
				player.send(new WareHouseWithdrawalList(1, (int) active.inventory().adena(), stored));
				return null;
			}
		} else if ("Teleport".equalsIgnoreCase(event)) {
			if (aLevel == -4) {
				return "31383-4.htm";
			} else if (aLevel == -5) {
				return "31383-5.htm";
			}
		}
		return event;
	}
}
