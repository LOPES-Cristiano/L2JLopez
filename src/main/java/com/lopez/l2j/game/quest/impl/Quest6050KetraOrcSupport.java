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
 * 6050_KetraOrcSupport: Suporte da tribo Ketra Orc (31370..31376)
 * Buffs em troca de Buffalo Horns (7186), Warehouse e Gatekeeper por ally level (1 a 5).
 */
@Component
public class Quest6050KetraOrcSupport extends Quest {

	public static final int KADUN = 31370;
	public static final int WAHKAN = 31371;
	public static final int ASEFA = 31372;
	public static final int ATAN = 31373;
	public static final int JAFF = 31374;
	public static final int JUMARA = 31375;
	public static final int KURFA = 31376;

	public static final int HORN = 7186;

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
	public Quest6050KetraOrcSupport(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) SkillService skillService) {
		super(6050, "6050_KetraOrcSupport", "custom");
		this.skillService = skillService;

		for (int i = 31370; i <= 31376; i++) {
			addFirstTalkId(i);
		}
		addTalkId(ASEFA, KURFA, JAFF);
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
		long horns = qs.getQuestItemsCount(HORN);

		if (npcId == KADUN) {
			return aLevel > 0 ? "31370-friend.htm" : "31370-no.htm";
		} else if (npcId == WAHKAN) {
			return aLevel > 0 ? "31371-friend.htm" : "31371-no.htm";
		} else if (npcId == ASEFA) {
			qs.setState(State.STARTED);
			if (aLevel < 1) {
				return "31372-3.htm";
			} else if (aLevel < 3) {
				return "31372-1.htm";
			} else {
				return horns > 0 ? "31372-4.htm" : "31372-2.htm";
			}
		} else if (npcId == ATAN) {
			if (player != null && player.karma() > 0) {
				return "31373-pk.htm";
			} else if (aLevel <= 0) {
				return "31373-no.htm";
			} else if (aLevel == 1 || aLevel == 2) {
				return "31373-1.htm";
			} else {
				return "31373-2.htm";
			}
		} else if (npcId == JAFF) {
			if (aLevel <= 0) {
				return "31374-no.htm";
			} else if (aLevel == 1) {
				return "31374-1.htm";
			} else if (aLevel == 2 || aLevel == 3) {
				return "31374-2.htm";
			} else {
				return "31374-4.htm";
			}
		} else if (npcId == JUMARA) {
			if (aLevel == 2) {
				return "31375-1.htm";
			} else if (aLevel == 3 || aLevel == 4) {
				return "31375-2.htm";
			} else if (aLevel == 5) {
				return "31375-3.htm";
			} else {
				return "31375-no.htm";
			}
		} else if (npcId == KURFA) {
			if (aLevel <= 0) {
				return "31376-no.htm";
			} else if (aLevel < 4) {
				return "31376-1.htm";
			} else if (aLevel == 4) {
				return "31376-2.htm";
			} else {
				return "31376-3.htm";
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
			if (qs.getQuestItemsCount(HORN) >= buff.cost()) {
				qs.takeItems(HORN, buff.cost());
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
				return "31372-4.htm";
			}
		} else if ("Withdraw".equalsIgnoreCase(event)) {
			var wh = player.context() != null ? player.context().warehouse() : null;
			var stored = wh != null ? wh.getWarehouseItems(active.objectId()) : java.util.List.<com.lopez.l2j.game.item.ItemInstance>of();
			if (stored.isEmpty()) {
				return "31374-3.htm";
			} else {
				player.send(new ActionFailed());
				player.send(new WareHouseWithdrawalList(1, (int) active.inventory().adena(), stored));
				return null;
			}
		} else if ("Teleport".equalsIgnoreCase(event)) {
			if (aLevel == 4) {
				return "31376-4.htm";
			} else if (aLevel == 5) {
				return "31376-5.htm";
			}
		}
		return event;
	}
}
