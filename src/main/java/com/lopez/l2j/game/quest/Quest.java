package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.network.game.GameSession;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe base representativa de uma Quest no Lineage II Interlude.
 * Provê os hooks de eventos de NPC, combate, entrada no mundo e despacho de eventos assíncronos.
 */
public abstract class Quest {

	private static final Logger log = LoggerFactory.getLogger(Quest.class);

	private final int questId;
	private final String name;
	private final String descr;

	private final Set<Integer> questItemIds = ConcurrentHashMap.newKeySet();
	private final Set<Integer> startNpcIds = ConcurrentHashMap.newKeySet();
	private final Set<Integer> talkNpcIds = ConcurrentHashMap.newKeySet();
	private final Set<Integer> killNpcIds = ConcurrentHashMap.newKeySet();
	private final Set<Integer> firstTalkNpcIds = ConcurrentHashMap.newKeySet();

	public Quest(int questId, String name, String descr) {
		this.questId = questId;
		this.name = name;
		this.descr = descr;
	}

	public int getQuestId() {
		return questId;
	}

	public String getName() {
		return name;
	}

	public String getDescr() {
		return descr;
	}

	public Set<Integer> getQuestItemIds() {
		return Collections.unmodifiableSet(questItemIds);
	}

	public void registerQuestItems(int... itemIds) {
		if (itemIds != null) {
			for (int id : itemIds) {
				questItemIds.add(id);
			}
		}
	}

	public void addStartNpc(int... npcIds) {
		if (npcIds != null) {
			for (int id : npcIds) {
				startNpcIds.add(id);
				talkNpcIds.add(id);
			}
		}
	}

	public void addTalkId(int... npcIds) {
		if (npcIds != null) {
			for (int id : npcIds) {
				talkNpcIds.add(id);
			}
		}
	}

	public void addKillId(int... npcIds) {
		if (npcIds != null) {
			for (int id : npcIds) {
				killNpcIds.add(id);
			}
		}
	}

	public void addFirstTalkId(int... npcIds) {
		if (npcIds != null) {
			for (int id : npcIds) {
				firstTalkNpcIds.add(id);
			}
		}
	}

	public boolean hasStartNpc(int npcId) {
		return startNpcIds.contains(npcId);
	}

	public boolean hasTalkNpc(int npcId) {
		return talkNpcIds.contains(npcId);
	}

	public boolean hasKillNpc(int npcId) {
		return killNpcIds.contains(npcId);
	}

	public boolean hasFirstTalkNpc(int npcId) {
		return firstTalkNpcIds.contains(npcId);
	}

	/**
	 * Cria ou recupera um QuestState para o jogador.
	 */
	public QuestState newQuestState(GameSession player) {
		if (player == null || player.activeChar() == null) {
			return null;
		}
		QuestState qs = player.getQuestState(name);
		if (qs == null) {
			qs = new QuestState(this, player, State.CREATED);
			player.addQuestState(qs);
		} else {
			qs.setPlayer(player);
		}
		return qs;
	}

	public QuestState checkQuestState(GameSession player) {
		if (player == null) {
			return null;
		}
		return player.getQuestState(name);
	}

	// ==================== Lifecycle Hooks ====================

	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs != null) {
			return onEvent(event, qs);
		}
		return null;
	}

	public String onEvent(String event, QuestState qs) {
		return null;
	}

	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player != null ? player.getQuestState(name) : null;
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs != null) {
			return onTalk(npc, qs);
		}
		return null;
	}

	public String onTalk(NpcInstance npc, QuestState qs) {
		return null;
	}

	public String onFirstTalk(NpcInstance npc, GameSession player) {
		return null;
	}

	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player != null ? player.getQuestState(name) : null;
		if (qs != null) {
			return onKill(npc, qs, isPet);
		}
		return null;
	}

	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}

	public void onEnterWorld(GameSession player) {
	}

	public void onPlayerLevelUp(GameSession player, int oldLevel, int newLevel) {
	}

	// ==================== Dispatchers ====================

	public String notifyEvent(String event, NpcInstance npc, GameSession player) {
		try {
			return onAdvEvent(event, npc, player);
		} catch (Exception e) {
			log.error("Erro ao despachar onAdvEvent na quest {}: {}", name, e.getMessage(), e);
			return null;
		}
	}

	public String notifyTalk(NpcInstance npc, GameSession player) {
		try {
			return onTalk(npc, player);
		} catch (Exception e) {
			log.error("Erro ao despachar onTalk na quest {}: {}", name, e.getMessage(), e);
			return null;
		}
	}

	public String notifyFirstTalk(NpcInstance npc, GameSession player) {
		try {
			return onFirstTalk(npc, player);
		} catch (Exception e) {
			log.error("Erro ao despachar onFirstTalk na quest {}: {}", name, e.getMessage(), e);
			return null;
		}
	}

	public String notifyKill(NpcInstance npc, GameSession player, boolean isPet) {
		try {
			return onKill(npc, player, isPet);
		} catch (Exception e) {
			log.error("Erro ao despachar onKill na quest {}: {}", name, e.getMessage(), e);
			return null;
		}
	}
}
