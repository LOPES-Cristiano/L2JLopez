package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowScreenMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 1201_NewbieToken: Entrega de cupons de arma e armadura de iniciante
 * (COUPON_ONE = 7832, COUPON_TWO = 7833) e abertura das Multisells 528 e 529.
 */
@Component
public class Quest1201NewbieToken extends Quest {

	public static final int COUPON_ONE = 7832;
	public static final int COUPON_TWO = 7833;

	public static final int WEAPON_REWARD = 16;
	public static final int ARMOR_REWARD = 32;

	public static final int WEAPON_MULTISELL = 528;
	public static final int ARMOR_MULTISELL = 529;

	public static final int[] NPCS = { 30598, 30599, 30600, 30601, 30602, 31076, 31077, 32135 };

	private final CharacterVariablesService characterVariables;

	public Quest1201NewbieToken() {
		this(null, null);
	}

	public Quest1201NewbieToken(QuestManager questManager) {
		this(questManager, null);
	}

	@Autowired
	public Quest1201NewbieToken(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) CharacterVariablesService characterVariables) {
		super(1201, "1201_NewbieToken", "custom");
		this.characterVariables = characterVariables;
		for (int npcId : NPCS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return "1.htm";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null || player.activeChar() == null) {
			return null;
		}
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		PlayerCharacter active = player.activeChar();
		int level = active.level();
		int pkkills = active.pkKills();
		int occupationLevel = active.classLevel();
		int newbie = active.getNewbie();

		if ("newbie_give_weapon_coupon".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 39 && pkkills == 0 && occupationLevel == 0) {
				if ((newbie & WEAPON_REWARD) != WEAPON_REWARD) {
					active.setNewbie(newbie | WEAPON_REWARD);
					if (characterVariables != null) {
						characterVariables.set(active.objectId(), "newbie_token_weapon", "1");
					}
					qs.giveItems(COUPON_ONE, 5);
					player.send(new ExShowScreenMessage("You have received 5 coupons for beginner weapons!", 4000));
					return "2.htm";
				} else {
					return "3.htm";
				}
			} else {
				return "4.htm";
			}
		} else if ("newbie_show_weapon".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 39 && pkkills == 0 && occupationLevel == 0) {
				player.showMultiSell(npc, WEAPON_MULTISELL);
				return null;
			} else {
				return "5.htm";
			}
		} else if ("newbie_give_armor_coupon".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 39 && pkkills == 0 && occupationLevel > 0) {
				if ((newbie & ARMOR_REWARD) != ARMOR_REWARD) {
					active.setNewbie(newbie | ARMOR_REWARD);
					if (characterVariables != null) {
						characterVariables.set(active.objectId(), "newbie_token_armor", "1");
					}
					qs.giveItems(COUPON_TWO, 1);
					return "6.htm";
				} else {
					return "7.htm";
				}
			} else {
				return "8.htm";
			}
		} else if ("newbie_show_armor".equalsIgnoreCase(event)) {
			if (level >= 6 && level <= 39 && pkkills == 0 && occupationLevel > 0) {
				player.showMultiSell(npc, ARMOR_MULTISELL);
				return null;
			} else {
				return "9.htm";
			}
		}
		return null;
	}
}
