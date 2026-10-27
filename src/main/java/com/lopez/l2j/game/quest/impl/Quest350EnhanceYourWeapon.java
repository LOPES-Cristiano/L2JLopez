package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

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
	public static final int GREEN_SOUL_CRYSTAL_0 = 4640;
	public static final int BLUE_SOUL_CRYSTAL_0 = 4651;

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

	public static boolean isSoulCrystal(int itemId) {
		return (itemId >= 4629 && itemId <= 4664)
				|| (itemId >= 5577 && itemId <= 5582)
				|| itemId == 5908 || itemId == 5911 || itemId == 5914;
	}

	public static int getSoulCrystalStage(int crystalId) {
		if (crystalId >= 4629 && crystalId <= 4639) {
			return crystalId - 4629;
		}
		if (crystalId >= 4640 && crystalId <= 4650) {
			return crystalId - 4640;
		}
		if (crystalId >= 4651 && crystalId <= 4661) {
			return crystalId - 4651;
		}
		if (crystalId >= 5577 && crystalId <= 5579) {
			return 11;
		}
		if (crystalId >= 5580 && crystalId <= 5582) {
			return 12;
		}
		if (crystalId == 5908 || crystalId == 5911 || crystalId == 5914) {
			return 13;
		}
		return -1;
	}

	public static int countSoulCrystals(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return 0;
		}
		int total = 0;
		for (var item : player.inventory().items()) {
			if (item != null && isSoulCrystal(item.itemId())) {
				total += Math.max(1, item.count());
			}
		}
		return total;
	}

	public static int countSoulCrystals(QuestState st) {
		if (st == null) {
			return 0;
		}
		if (st.playerChar() != null && st.playerChar().inventory() != null) {
			int invCount = countSoulCrystals(st.playerChar());
			if (invCount > 0) {
				return invCount;
			}
		}
		int total = 0;
		for (int id = 4629; id <= 4664; id++) {
			total += st.getQuestItemsCount(id);
		}
		for (int id = 5577; id <= 5582; id++) {
			total += st.getQuestItemsCount(id);
		}
		total += st.getQuestItemsCount(5908);
		total += st.getQuestItemsCount(5911);
		total += st.getQuestItemsCount(5914);
		return total;
	}

	public static boolean hasResonance(PlayerCharacter player) {
		return countSoulCrystals(player) > 1;
	}

	public static boolean hasResonance(QuestState st) {
		return countSoulCrystals(st) > 1;
	}

	public static boolean isCompatibleMonsterLevel(int crystalStage, int mobLevel, boolean isRaidOrBoss) {
		if (crystalStage < 0 || crystalStage >= 13) {
			return false;
		}
		if (crystalStage >= 10) {
			return isRaidOrBoss && (mobLevel <= 0 || mobLevel >= (40 + (crystalStage * 2)));
		}
		if (mobLevel <= 0) {
			return true;
		}
		int minMobLevel = 40 + (crystalStage * 2);
		return mobLevel >= minMobLevel;
	}

	public static boolean hasSoulCrystal(QuestState st) {
		for (int id = 4629; id <= 4664; id++) {
			if (st.hasQuestItems(id)) {
				return true;
			}
		}
		for (int id = 5577; id <= 5582; id++) {
			if (st.hasQuestItems(id)) {
				return true;
			}
		}
		return st.hasQuestItems(5908) || st.hasQuestItems(5911) || st.hasQuestItems(5914);
	}

	public static int getNextCrystalId(int crystalId) {
		// Red Soul Crystal 0..10 (4629..4639), 11 (5577), 12 (5580), 13 (5908)
		if (crystalId >= 4629 && crystalId <= 4638) {
			return crystalId + 1;
		}
		if (crystalId == 4639) {
			return 5577;
		}
		if (crystalId == 5577) {
			return 5580;
		}
		if (crystalId == 5580) {
			return 5908;
		}
		// Green Soul Crystal 0..10 (4640..4650), 11 (5578), 12 (5581), 13 (5911)
		if (crystalId >= 4640 && crystalId <= 4649) {
			return crystalId + 1;
		}
		if (crystalId == 4650) {
			return 5578;
		}
		if (crystalId == 5578) {
			return 5581;
		}
		if (crystalId == 5581) {
			return 5911;
		}
		// Blue Soul Crystal 0..10 (4651..4661), 11 (5579), 12 (5582), 13 (5914)
		if (crystalId >= 4651 && crystalId <= 4660) {
			return crystalId + 1;
		}
		if (crystalId == 4661) {
			return 5579;
		}
		if (crystalId == 5579) {
			return 5582;
		}
		if (crystalId == 5582) {
			return 5914;
		}
		return 0;
	}

	public static int getNextSoulCrystalId(int crystalId) {
		return getNextCrystalId(crystalId);
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		if (player != null && npc != null && npc.isRaid() && player.party() != null) {
			for (GameSession member : player.party().members()) {
				if (member != null && member.activeChar() != null) {
					double dist = Math.hypot(member.activeChar().x() - npc.x(), member.activeChar().y() - npc.y());
					if (dist <= Party.getPartyRange()) {
						QuestState mst = member.getQuestState(QUEST_NAME);
						if (mst != null && mst.isStarted()) {
							onKill(npc, mst, isPet);
						}
					}
				}
			}
			return null;
		}
		QuestState st = player != null ? player.getQuestState(QUEST_NAME) : null;
		if (st != null) {
			onKill(npc, st, isPet);
		}
		return null;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState st, boolean isPet) {
		if (st == null || !st.isStarted()) {
			return null;
		}
		if (hasResonance(st)) {
			if (st.player() != null) {
				st.player().send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"O Soul Crystal ressoou com outro cristal no inventario e falhou em absorver a alma."));
			}
			return null;
		}
		boolean isRaidOrBoss = npc != null && (npc.isRaid()
				|| (npc.npcId() >= 25000 && npc.npcId() < 26000)
				|| (npc.npcId() >= 29000 && npc.npcId() < 30000));
		int mobLevel = (npc != null && npc.template() != null) ? npc.template().level() : 0;

		int[] candidates = {
			4629, 4630, 4631, 4632, 4633, 4634, 4635, 4636, 4637, 4638, 4639, 5577, 5580,
			4640, 4641, 4642, 4643, 4644, 4645, 4646, 4647, 4648, 4649, 4650, 5578, 5581,
			4651, 4652, 4653, 4654, 4655, 4656, 4657, 4658, 4659, 4660, 4661, 5579, 5582
		};
		for (int cid : candidates) {
			if (st.getQuestItemsCount(cid) == 1) {
				int stage = getSoulCrystalStage(cid);
				if (stage >= 10 && !isRaidOrBoss && npc != null) {
					break;
				}
				if (npc != null && !isCompatibleMonsterLevel(stage, mobLevel, isRaidOrBoss)) {
					break;
				}
				int nextId = getNextCrystalId(cid);
				if (nextId > 0) {
					int chance = (npc == null || isRaidOrBoss) ? 100 : Math.max(1, Config.CHANCE_TO_LEVEL_SA);
					if (npc == null || isRaidOrBoss || ThreadLocalRandom.current().nextInt(100) < chance) {
						st.takeItems(cid, 1);
						st.giveItems(nextId, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
				}
			}
		}
		return null;
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
