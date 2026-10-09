package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Superclasse base que encapsula todo o fluxo de progressao das 31 Sagas de 3a Classe no Lineage II Interlude.
 * Replica fielmente a sequencia das 6 Tablets of Vision, os Halisha's Marks, Arconte de Halisha e troca de classe.
 */
public abstract class SagaMasterQuest extends Quest {

	public static final int[] ARCHON_MINIONS = {21646, 21647, 21648, 21649, 21650, 21651};
	public static final int[] GUARDIAN_ANGELS = {27214, 27215, 27216};
	public static final int[] ARCHON_HELLISHA_NORM = {18212, 18213, 18214, 18215, 18216, 18217, 18218, 18219};

	protected final int classId;
	protected final int prevClass;
	protected final int[] npc;
	protected final int[] items;
	protected final int[] mob;
	protected final int[] x;
	protected final int[] y;
	protected final int[] z;
	protected final String[] text;

	// Rastreador de kills de Guardian Angels por player
	private final ConcurrentHashMap<Integer, Integer> angelKills = new ConcurrentHashMap<>();

	public SagaMasterQuest(int questId, String questName, String descr, QuestManager questManager,
			int classId, int prevClass, int[] npc, int[] items, int[] mob,
			int[] x, int[] y, int[] z, String[] text) {
		super(questId, questName, descr);
		this.classId = classId;
		this.prevClass = prevClass;
		this.npc = npc;
		this.items = items;
		this.mob = mob;
		this.x = x;
		this.y = y;
		this.z = z;
		this.text = text;

		// Registra NPCs iniciais e de conversa
		addStartNpc(npc[0]);
		for (int n : npc) {
			addTalkId(n);
		}
		for (int m : mob) {
			addKillId(m);
		}
		for (int am : ARCHON_MINIONS) {
			addKillId(am);
		}
		for (int ga : GUARDIAN_ANGELS) {
			addKillId(ga);
		}
		for (int ah : ARCHON_HELLISHA_NORM) {
			addKillId(ah);
		}
		for (int it : items) {
			if (it != 0 && it != 7080 && it != 7081 && it != 6480 && it != 6482) {
				registerQuestItems(it);
			}
		}

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	public int getTargetClassId() {
		return classId;
	}

	public int getRequiredPrevClass() {
		return prevClass;
	}

	protected void completeSaga(QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return;

		angelKills.remove(pc.objectId());
		qs.addExpAndSp(2586527, 0);
		qs.giveItems(57, 5000000); // 5kk Adena
		qs.giveItems(6622, 1);       // Book of Giants (Giant's Codex)
		qs.exitQuest(false);

		// Converte a classe para a 3a Classe
		pc.classId(this.classId);
		if (!pc.isSubClassActive() && pc.baseClassId() == this.prevClass) {
			pc.baseClassId(this.classId);
		}
		qs.playSound(QuestState.SOUND_FANFARE);
	}

	public void giveHallishaMark(QuestState qs) {
		long current = qs.getQuestItemsCount(items[3]);
		if (current < 700) {
			qs.giveItems(items[3], 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else {
			// Atingiu os 700 Halisha's Marks: derrota o Arconte e adquire o Amuleto 5
			qs.takeItems(items[3], -1);
			qs.giveItems(items[8], 1); // Resonance Amulet 5
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if ("0-011.htm".equalsIgnoreCase(event) || "0-012.htm".equalsIgnoreCase(event) ||
				"0-013.htm".equalsIgnoreCase(event) || "0-014.htm".equalsIgnoreCase(event) ||
				"0-015.htm".equalsIgnoreCase(event)) {
			return event;
		}

		if ("accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.giveItems(items[10], 1);
			return "0-03.htm";
		} else if ("0-1".equalsIgnoreCase(event)) {
			if (pc.level() < 76) {
				qs.exitQuest(true);
				return "0-02.htm";
			}
			return "0-05.htm";
		} else if ("0-2".equalsIgnoreCase(event)) {
			if (pc.level() >= 76) {
				qs.takeItems(items[10], -1);
				completeSaga(qs);
				return "0-07.htm";
			} else {
				qs.takeItems(items[10], -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.setCond(20);
				return "0-08.htm";
			}
		} else if ("1-3".equalsIgnoreCase(event)) {
			qs.setCond(3);
			return "1-05.htm";
		} else if ("1-4".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.takeItems(items[0], 1); // Ice Crystal
			if (items[11] != 0) {
				qs.takeItems(items[11], 1); // Divine Stone of Wisdom
			}
			qs.giveItems(items[1], 1);
			return "1-06.htm";
		} else if ("2-1".equalsIgnoreCase(event)) {
			qs.setCond(2);
			return "2-05.htm";
		} else if ("2-2".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.takeItems(items[1], 1);
			qs.giveItems(items[4], 1); // Resonance Amulet 1
			return "2-06.htm";
		} else if ("3-5".equalsIgnoreCase(event)) {
			return "3-07.htm";
		} else if ("3-6".equalsIgnoreCase(event)) {
			qs.setCond(11);
			return "3-02.htm";
		} else if ("3-7".equalsIgnoreCase(event)) {
			qs.setCond(12);
			return "3-03.htm";
		} else if ("3-8".equalsIgnoreCase(event)) {
			qs.setCond(13);
			qs.takeItems(items[2], 1);
			qs.giveItems(items[7], 1); // Resonance Amulet 4
			return "3-08.htm";
		} else if ("4-1".equalsIgnoreCase(event)) {
			return "4-010.htm";
		} else if ("4-2".equalsIgnoreCase(event) || "4-3".equalsIgnoreCase(event)) {
			qs.giveItems(items[9], 1); // Resonance Amulet 6
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "4-011.htm";
		} else if ("5-1".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.takeItems(items[4], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "5-02.htm";
		} else if ("6-1".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.takeItems(items[5], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "6-03.htm";
		} else if ("7-1".equalsIgnoreCase(event)) {
			return "7-02.htm";
		} else if ("7-2".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.takeItems(items[6], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "7-06.htm";
		} else if ("8-1".equalsIgnoreCase(event)) {
			qs.setCond(14);
			qs.takeItems(items[7], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "8-02.htm";
		} else if ("9-1".equalsIgnoreCase(event)) {
			qs.setCond(17);
			qs.takeItems(items[8], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "9-03.htm";
		} else if ("10-1".equalsIgnoreCase(event)) {
			return "10-02.htm";
		} else if ("10-2".equalsIgnoreCase(event)) {
			qs.setCond(19);
			qs.takeItems(items[9], 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "10-06.htm";
		} else if ("11-9".equalsIgnoreCase(event)) {
			qs.setCond(15);
			return "11-03.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npcInst, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npcInst.getNpcId();
		int cond = qs.getCond();

		// Valida classe compativel
		if (pc.classId() != this.prevClass && pc.baseClassId() != this.prevClass) {
			return "noquest";
		}

		if (cond == 0) {
			if (npcId == npc[0]) {
				return "0-01.htm";
			}
		} else if (cond == 1) {
			if (npcId == npc[0]) return "0-04.htm";
			if (npcId == npc[2]) return "2-01.htm";
		} else if (cond == 2) {
			if (npcId == npc[2]) return "2-02.htm";
			if (npcId == npc[1]) return "1-01.htm";
		} else if (cond == 3) {
			if (npcId == npc[1]) {
				if (qs.hasQuestItems(items[0])) {
					if (items[11] == 0 || qs.hasQuestItems(items[11])) {
						return "1-03.htm";
					}
				}
				return "1-02.htm";
			}
			if (npcId == 31537) { // Tunatun em Quest 72
				if (!qs.hasQuestItems(7546)) {
					qs.giveItems(7546, 1);
					return "tunatun_q72_01.htm";
				}
				return "tunatun_q72_02.htm";
			}
		} else if (cond == 4) {
			if (npcId == npc[1]) return "1-04.htm";
			if (npcId == npc[2]) return "2-03.htm";
		} else if (cond == 5) {
			if (npcId == npc[2]) return "2-04.htm";
			if (npcId == npc[5]) return "5-01.htm";
		} else if (cond == 6) {
			if (npcId == npc[5]) return "5-03.htm";
			if (npcId == npc[6]) return "6-01.htm";
		} else if (cond == 7) {
			if (npcId == npc[6]) return "6-02.htm";
		} else if (cond == 8) {
			if (npcId == npc[6]) return "6-04.htm";
			if (npcId == npc[7]) return "7-01.htm";
		} else if (cond == 9) {
			if (npcId == npc[7]) return "7-05.htm";
		} else if (cond == 10) {
			if (npcId == npc[7]) return "7-07.htm";
			if (npcId == npc[3]) return "3-01.htm";
		} else if (cond == 11 || cond == 12) {
			if (npcId == npc[3]) {
				return qs.hasQuestItems(items[2]) ? "3-05.htm" : "3-04.htm";
			}
		} else if (cond == 13) {
			if (npcId == npc[3]) return "3-06.htm";
			if (npcId == npc[8]) return "8-01.htm";
		} else if (cond == 14) {
			if (npcId == npc[8]) return "8-03.htm";
			if (npcId == npc[11]) return "11-01.htm";
		} else if (cond == 15) {
			if (npcId == npc[11]) return "11-02.htm";
			if (npcId == npc[9]) return "9-01.htm";
		} else if (cond == 16) {
			if (npcId == npc[9]) return "9-02.htm";
		} else if (cond == 17) {
			if (npcId == npc[9]) return "9-04.htm";
			if (npcId == npc[10]) return "10-01.htm";
		} else if (cond == 18) {
			if (npcId == npc[10]) return "10-05.htm";
		} else if (cond == 19) {
			if (npcId == npc[10]) return "10-07.htm";
			if (npcId == npc[0]) return "0-06.htm";
		} else if (cond == 20 && npcId == npc[0]) {
			if (pc.level() >= 76) {
				completeSaga(qs);
				return "0-09.htm";
			}
			return "0-010.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npcInst, QuestState qs, boolean isPet) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int npcId = npcInst.getNpcId();
		int cond = qs.getCond();

		// 1. Guardian Angels (Cond 6 -> Matar 10 para obter Amuleto 2)
		for (int ga : GUARDIAN_ANGELS) {
			if (ga == npcId && cond == 6) {
				int kills = angelKills.merge(pc.objectId(), 1, Integer::sum);
				if (kills >= 10) {
					angelKills.remove(pc.objectId());
					qs.giveItems(items[5], 1); // Resonance Amulet 2
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
				return null;
			}
		}

		// 2. Archon Minions (Cond 15 -> Farm de 700 Halisha's Marks)
		for (int am : ARCHON_MINIONS) {
			if (am == npcId && cond == 15) {
				giveHallishaMark(qs);
				return null;
			}
		}

		// 3. Arconte de Halisha Raid ou Quest Mob (Cond 15 -> dropa Resonance Amulet 5)
		for (int ah : ARCHON_HELLISHA_NORM) {
			if (ah == npcId && cond == 15) {
				qs.giveItems(items[8], 1); // Resonance Amulet 5
				qs.takeItems(items[3], -1);
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return null;
			}
		}
		if (mob[1] == npcId && cond == 15) {
			qs.giveItems(items[8], 1); // Resonance Amulet 5
			qs.takeItems(items[3], -1);
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return null;
		}

		// 4. Mob 0 na Tablet 3 (Cond 8 -> dropa Resonance Amulet 3)
		if (mob[0] == npcId && cond == 8) {
			qs.giveItems(items[6], 1); // Resonance Amulet 3
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return null;
		}

		// 5. Mob 2 na Tablet 6 (Cond 17 -> dropa Resonance Amulet 6)
		if (mob[2] == npcId && cond == 17) {
			qs.set("Tab", "1");
			qs.giveItems(items[9], 1); // Resonance Amulet 6
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return null;
		}

		return null;
	}
}
