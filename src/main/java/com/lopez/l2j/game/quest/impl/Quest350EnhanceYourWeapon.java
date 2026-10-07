package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.stereotype.Component;

/**
 * Quest 350: Enhance Your Weapon
 *
 * Missão oficial de aquisição dos Soul Crystals (Red, Green, Blue - Stage 0) para absorção
 * de almas e concessão de Special Abilities (SA) em armas C, B, A e S-Grade.
 */
@Component
public class Quest350EnhanceYourWeapon extends Quest {

	public static final int QUEST_ID = 350;
	public static final String QUEST_NAME = "350_EnhanceYourWeapon";

	// NPCs
	public static final int JUREK = 30115;
	public static final int GIDEON = 30856;
	public static final int WINONIN = 30194;

	// Cristais Estágio 0
	public static final int RED_SOUL_CRYSTAL_0 = 4629;
	public static final int GREEN_SOUL_CRYSTAL_0 = 4639;
	public static final int BLUE_SOUL_CRYSTAL_0 = 4649;

	public Quest350EnhanceYourWeapon() {
		super(QUEST_ID, QUEST_NAME, "Enhance Your Weapon");
		addStartNpc(JUREK, GIDEON, WINONIN);
		addTalkId(JUREK, GIDEON, WINONIN);
	}

	public static boolean hasSoulCrystal(QuestState st) {
		for (int id = 4629; id <= 4664; id++) {
			if (st.hasQuestItems(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState st = checkQuestState(player);
		if (st == null) {
			st = newQuestState(player);
		}
		if (st == null) {
			return null;
		}

		if (event.endsWith("-04.htm")) {
			st.setState(State.STARTED);
			st.set("cond", 1);
			st.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if (event.endsWith("-09.htm")) {
			// Escolheu Red Soul Crystal
			if (!hasSoulCrystal(st)) {
				st.giveItems(RED_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		} else if (event.endsWith("-10.htm")) {
			// Escolheu Green Soul Crystal
			if (!hasSoulCrystal(st)) {
				st.giveItems(GREEN_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		} else if (event.endsWith("-11.htm")) {
			// Escolheu Blue Soul Crystal
			if (!hasSoulCrystal(st)) {
				st.giveItems(BLUE_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState st = player.getQuestState(QUEST_NAME);
		if (st == null) {
			st = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return "noquest";
		}

		int npcId = npc.npcId();

		if (st.isCreated()) {
			if (c.level() < 40) {
				return npcId + "-03.htm"; // Nível mínimo 40
			}
			return npcId + "-01.htm"; // Apresentação
		}

		if (st.isStarted()) {
			if (hasSoulCrystal(st)) {
				return npcId + "-05.htm"; // Já possui cristal em absorção
			}
			return npcId + "-06.htm"; // Pode escolher novo cristal
		}

		return "noquest";
	}
}
