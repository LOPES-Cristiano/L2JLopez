package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
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

	private static final int[] SOUL_ABSORBING_MOBS = {
		20550, 20553, 20577, 20578, 20579, 20581, 20582, 20600, 20630,
		20641, 20642, 20643, 20644, 20645, 20674, 20679, 20680, 20686,
		20821, 20823, 20826, 20827, 20828, 20829, 20830, 20831,
		25001, 25019, 25035, 25054, 25070, 25082, 25092, 25109, 25126,
		25143, 25163, 25198, 25229, 25235, 25245, 25248, 25249, 25252,
		25266, 25269, 25276, 25282, 25283, 25286, 25299, 25302, 25305,
		25309, 25312, 25315, 25319, 25325, 25328, 25407, 25450, 25514,
		29001, 29006, 29014, 29019, 29020, 29022, 29028, 29046, 29047, 29065
	};

	public Quest350EnhanceYourWeapon() {
		this(null);
	}

	@Autowired
	public Quest350EnhanceYourWeapon(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Enhance Your Weapon");
		addStartNpc(JUREK, GIDEON, WINONIN);
		addTalkId(JUREK, GIDEON, WINONIN);
		addKillId(SOUL_ABSORBING_MOBS);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
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
		return handleEvent(event, st);
	}

	@Override
	public String onEvent(String event, QuestState st) {
		if (st == null) {
			return null;
		}
		return handleEvent(event, st);
	}

	private String handleEvent(String event, QuestState st) {
		if ("30115-04.htm".equalsIgnoreCase(event) || "30194-04.htm".equalsIgnoreCase(event) || "30856-04.htm".equalsIgnoreCase(event)) {
			st.setState(State.STARTED);
			st.set("cond", 1);
			st.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30115-09.htm".equalsIgnoreCase(event) || "30194-09.htm".equalsIgnoreCase(event) || "30856-09.htm".equalsIgnoreCase(event)) {
			if (!hasSoulCrystal(st)) {
				st.giveItems(RED_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		} else if ("30115-10.htm".equalsIgnoreCase(event) || "30194-10.htm".equalsIgnoreCase(event) || "30856-10.htm".equalsIgnoreCase(event)) {
			if (!hasSoulCrystal(st)) {
				st.giveItems(GREEN_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		} else if ("30115-11.htm".equalsIgnoreCase(event) || "30194-11.htm".equalsIgnoreCase(event) || "30856-11.htm".equalsIgnoreCase(event)) {
			if (!hasSoulCrystal(st)) {
				st.giveItems(BLUE_SOUL_CRYSTAL_0, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			return event;
		} else if ("exit.htm".equalsIgnoreCase(event) || "30115-21.htm".equalsIgnoreCase(event) || "30194-21.htm".equalsIgnoreCase(event) || "30856-21.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
			return "exit.htm";
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
		return handleTalk(npc, st, c);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState st) {
		if (st == null) {
			return "noquest";
		}
		PlayerCharacter c = st.playerChar();
		if (c == null) {
			return "noquest";
		}
		return handleTalk(npc, st, c);
	}

	private String handleTalk(NpcInstance npc, QuestState st, PlayerCharacter c) {
		int npcId = npc != null ? npc.npcId() : JUREK;

		if (st.isCreated()) {
			if (c.level() < 40) {
				return switch (npcId) {
					case WINONIN -> "30194-02.htm";
					case GIDEON -> "30856-02.htm";
					default -> "30115-02.htm";
				};
			}
			return switch (npcId) {
				case WINONIN -> "30194-01.htm";
				case GIDEON -> "30856-01.htm";
				default -> "30115-01.htm";
			};
		}

		if (st.isStarted()) {
			if (hasSoulCrystal(st)) {
				return switch (npcId) {
					case WINONIN -> "30194-05.htm";
					case GIDEON -> "30856-05.htm";
					default -> "30115-05.htm";
				};
			}
			return switch (npcId) {
				case WINONIN -> "30194-08.htm";
				case GIDEON -> "30856-08.htm";
				default -> "30115-08.htm";
			};
		}

		return "noquest";
	}
}
