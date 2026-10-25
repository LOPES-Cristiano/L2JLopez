package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList.QuestEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Gerenciador central de Quests do L2JLopez.
 * Controla registro de quests, persistencia de variaveis em character_quests e despacho de eventos.
 */
@Service
public class QuestManager implements SmartInitializingSingleton {

	private static final Logger log = LoggerFactory.getLogger(QuestManager.class);
	private static final Comparator<Quest> QUEST_ID_COMPARATOR = Comparator.comparingInt(Quest::getQuestId);

	private final JdbcTemplate jdbcTemplate;
	private final List<Quest> discoveredQuests;
	private volatile boolean discoveredInitialized = false;

	private final Map<Integer, Quest> questsById = new ConcurrentHashMap<>();
	private final Map<String, Quest> questsByName = new ConcurrentHashMap<>();
	private final Map<Integer, List<Quest>> questsByStartNpc = new ConcurrentHashMap<>();
	private final Map<Integer, List<Quest>> questsByTalkNpc = new ConcurrentHashMap<>();
	private final Map<Integer, List<Quest>> questsByFirstTalkNpc = new ConcurrentHashMap<>();
	private final Map<Integer, List<Quest>> questsByKillNpc = new ConcurrentHashMap<>();
	private final Map<Integer, List<Quest>> questsByAttackNpc = new ConcurrentHashMap<>();

	public QuestManager(JdbcTemplate jdbcTemplate) {
		this(jdbcTemplate, null);
	}

	@Autowired
	public QuestManager(
			@Autowired(required = false) JdbcTemplate jdbcTemplate,
			@Autowired(required = false) @Lazy List<Quest> discoveredQuests) {
		this.jdbcTemplate = jdbcTemplate;
		this.discoveredQuests = discoveredQuests;
		if (discoveredQuests != null && !(discoveredQuests instanceof SpringProxy)) {
			this.discoveredInitialized = true;
			registerAll(discoveredQuests);
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		ensureDiscoveredQuestsRegistered();
	}

	private void ensureDiscoveredQuestsRegistered() {
		if (!discoveredInitialized && discoveredQuests != null) {
			discoveredInitialized = true;
			try {
				registerAll(discoveredQuests);
			} catch (Exception e) {
				log.debug("Auto-registro de quests diferido: {}", e.getMessage());
			}
		}
	}

	public void registerAll(Collection<Quest> quests) {
		if (quests == null) {
			return;
		}
		for (Quest q : quests) {
			if (q != null) {
				registerQuest(q);
			}
		}
	}

	public void registerQuest(Quest quest) {
		if (quest == null) {
			return;
		}
		int qid = quest.getQuestId();
		String qName = quest.getName().toLowerCase(java.util.Locale.ROOT);

		questsById.put(qid, quest);
		questsByName.put(qName, quest);

		// Alias com ID direto e padronizado
		questsByName.putIfAbsent(String.valueOf(qid), quest);
		questsByName.putIfAbsent(String.format("%03d", qid), quest);
		questsByName.putIfAbsent("q" + qid, quest);
		questsByName.putIfAbsent("q" + String.format("%03d", qid), quest);
		questsByName.putIfAbsent("quest" + qid, quest);

		// Variacoes com ou sem underscore inicial e prefixos Q001_ / 1_
		String noUnder = qName.startsWith("_") ? qName.substring(1) : qName;
		questsByName.putIfAbsent(noUnder, quest);
		questsByName.putIfAbsent("_" + noUnder, quest);
		int underIdx = noUnder.indexOf('_');
		if (qid > 0 && underIdx > 0) {
			String suffix = noUnder.substring(underIdx);
			questsByName.putIfAbsent(qid + suffix, quest);
			questsByName.putIfAbsent(String.format("%03d%s", qid, suffix), quest);
			questsByName.putIfAbsent(String.format("q%03d%s", qid, suffix), quest);
			questsByName.putIfAbsent(String.format("q%d%s", qid, suffix), quest);
		}

		quest.setRegisteredManager(this);
		indexQuestNpcs(quest);

		log.info("Quest registrada: [{}] {}", quest.getQuestId(), quest.getName());
	}

	void indexQuestNpcs(Quest quest) {
		if (quest == null) {
			return;
		}
		for (int npcId : quest.getStartNpcIds()) {
			addQuestToNpcIndex(questsByStartNpc, npcId, quest);
		}
		for (int npcId : quest.getTalkNpcIds()) {
			addQuestToNpcIndex(questsByTalkNpc, npcId, quest);
		}
		for (int npcId : quest.getFirstTalkNpcIds()) {
			addQuestToNpcIndex(questsByFirstTalkNpc, npcId, quest);
		}
		for (int npcId : quest.getKillNpcIds()) {
			addQuestToNpcIndex(questsByKillNpc, npcId, quest);
		}
		for (int npcId : quest.getAttackNpcIds()) {
			addQuestToNpcIndex(questsByAttackNpc, npcId, quest);
		}
	}

	private static void addQuestToNpcIndex(Map<Integer, List<Quest>> index, int npcId, Quest quest) {
		index.compute(npcId, (k, existing) -> {
			List<Quest> list = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
			list.removeIf(q -> q.getQuestId() == quest.getQuestId() && q.getName().equals(quest.getName()));
			list.add(quest);
			list.sort(QUEST_ID_COMPARATOR);
			return new CopyOnWriteArrayList<>(list);
		});
	}

	public List<Quest> getQuestsForStartNpc(int npcId) {
		ensureDiscoveredQuestsRegistered();
		List<Quest> list = questsByStartNpc.get(npcId);
		return list != null ? Collections.unmodifiableList(list) : List.of();
	}

	public List<Quest> getQuestsForTalkNpc(int npcId) {
		ensureDiscoveredQuestsRegistered();
		List<Quest> list = questsByTalkNpc.get(npcId);
		return list != null ? Collections.unmodifiableList(list) : List.of();
	}

	public List<Quest> getQuestsForFirstTalkNpc(int npcId) {
		ensureDiscoveredQuestsRegistered();
		List<Quest> list = questsByFirstTalkNpc.get(npcId);
		return list != null ? Collections.unmodifiableList(list) : List.of();
	}

	public List<Quest> getQuestsForKillNpc(int npcId) {
		ensureDiscoveredQuestsRegistered();
		List<Quest> list = questsByKillNpc.get(npcId);
		return list != null ? Collections.unmodifiableList(list) : List.of();
	}

	public List<Quest> getQuestsForAttackNpc(int npcId) {
		ensureDiscoveredQuestsRegistered();
		List<Quest> list = questsByAttackNpc.get(npcId);
		return list != null ? Collections.unmodifiableList(list) : List.of();
	}

	public Quest getQuest(int questId) {
		ensureDiscoveredQuestsRegistered();
		return questsById.get(questId);
	}

	public Quest getQuest(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		ensureDiscoveredQuestsRegistered();
		String clean = name.trim().toLowerCase(java.util.Locale.ROOT);
		Quest q = questsByName.get(clean);
		if (q != null) {
			return q;
		}

		// Tenta remover prefixo '_' ou 'q'
		if (clean.startsWith("_")) {
			q = questsByName.get(clean.substring(1));
			if (q != null) return q;
		}
		if (clean.startsWith("q")) {
			q = questsByName.get(clean.substring(1));
			if (q != null) return q;
		}

		// Tenta extrair numero de missao do inicio (ex: "_021_...", "21_...", "21")
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^[_qQ]*(\\d+)").matcher(clean);
		if (m.find()) {
			try {
				int parsedId = Integer.parseInt(m.group(1));
				Quest byId = questsById.get(parsedId);
				if (byId != null) {
					return byId;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		return null;
	}

	public Collection<Quest> getAllQuests() {
		ensureDiscoveredQuestsRegistered();
		return questsById.values();
	}

	// ==================== Persistência ====================

	public void loadQuestsForPlayer(GameSession player) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		int charId = player.activeChar().objectId();
		if (jdbcTemplate != null) {
			try {
				String sql = "SELECT name, var, value FROM character_quests WHERE charId = ? AND class_index = 0";
				jdbcTemplate.query(sql, rs -> {
					String qName = rs.getString("name");
					String var = rs.getString("var");
					String val = rs.getString("value");

					Quest quest = getQuest(qName);
					if (quest != null) {
						QuestState qs = player.getQuestState(quest.getName());
						if (qs == null) {
							qs = new QuestState(quest, player, State.CREATED);
							player.addQuestState(qs);
						}
						if ("<state>".equalsIgnoreCase(var)) {
							if ("Started".equalsIgnoreCase(val)) {
								qs.setStateInternal(State.STARTED);
							} else if ("Completed".equalsIgnoreCase(val)) {
								qs.setStateInternal(State.COMPLETED);
							} else {
								qs.setStateInternal(State.CREATED);
							}
						} else {
							qs.setInternal(var, val);
						}
					}
				}, charId);
			} catch (Exception e) {
				log.warn("Erro ao carregar quests do banco para charId {}: {}", charId, e.getMessage());
			}
		}
	}

	public void saveQuestState(QuestState qs) {
		if (qs == null || jdbcTemplate == null) {
			return;
		}
		int charId = qs.getCharId();
		if (charId <= 0) {
			return;
		}
		String qName = qs.getQuestName();
		try {
			for (Map.Entry<String, String> entry : qs.getAllVars().entrySet()) {
				String var = entry.getKey();
				String val = entry.getValue();
				String sql = "REPLACE INTO character_quests (charId, name, var, value, class_index) VALUES (?, ?, ?, ?, 0)";
				jdbcTemplate.update(sql, charId, qName, var, val);
			}
		} catch (Exception e) {
			log.warn("Erro ao persistir quest {} para charId {}: {}", qName, charId, e.getMessage());
		}
	}

	public void deleteQuestState(QuestState qs) {
		if (qs == null || jdbcTemplate == null) {
			return;
		}
		int charId = qs.getCharId();
		String qName = qs.getQuestName();
		try {
			String sql = "DELETE FROM character_quests WHERE charId = ? AND name = ? AND class_index = 0";
			jdbcTemplate.update(sql, charId, qName);
		} catch (Exception e) {
			log.warn("Erro ao deletar quest {} para charId {}: {}", qName, charId, e.getMessage());
		}
	}

	public void deleteQuestVar(QuestState qs, String var) {
		if (qs == null || var == null || jdbcTemplate == null) {
			return;
		}
		int charId = qs.getCharId();
		String qName = qs.getQuestName();
		try {
			String sql = "DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ? AND class_index = 0";
			jdbcTemplate.update(sql, charId, qName, var);
		} catch (Exception e) {
			log.warn("Erro ao deletar var {} da quest {} para charId {}: {}", var, qName, charId, e.getMessage());
		}
	}

	// ==================== Dispatchers ====================

	public void onPlayerLogin(GameSession player) {
		ensureDiscoveredQuestsRegistered();
		loadQuestsForPlayer(player);
		for (Quest q : questsById.values()) {
			q.onEnterWorld(player);
		}
		sendQuestList(player);
	}

	public void sendQuestList(GameSession player) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		List<QuestEntry> entries = new ArrayList<>();
		for (QuestState qs : player.getAllQuestStates()) {
			if (qs.isStarted() && qs.getQuest() != null && qs.getQuest().isVisible()) {
				entries.add(new QuestEntry(qs.getQuest().getQuestId(), qs.getQuestListState()));
			}
		}
		player.send(new QuestList(entries));
	}

	public String onNpcTalk(NpcInstance npc, GameSession player) {
		if (npc == null || player == null) {
			return null;
		}
		int npcId = npc.npcId();
		for (Quest q : getQuestsForTalkNpc(npcId)) {
			String html = q.notifyTalk(npc, player);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}
		return null;
	}

	public String onNpcFirstTalk(NpcInstance npc, GameSession player) {
		if (npc == null || player == null) {
			return null;
		}
		int npcId = npc.npcId();
		for (Quest q : getQuestsForFirstTalkNpc(npcId)) {
			String html = q.notifyFirstTalk(npc, player);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}
		return null;
	}

	public void onNpcKill(NpcInstance npc, GameSession player, boolean isPet) {
		if (npc == null || player == null) {
			return;
		}
		int npcId = npc.npcId();
		for (Quest q : getQuestsForKillNpc(npcId)) {
			q.notifyKill(npc, player, isPet);
		}
	}

	public void onNpcAttack(NpcInstance npc, GameSession player, boolean isPet) {
		if (npc == null || player == null) {
			return;
		}
		int npcId = npc.npcId();
		for (Quest q : getQuestsForAttackNpc(npcId)) {
			q.notifyAttack(npc, player, isPet);
		}
	}

	public void onPlayerLevelUp(GameSession player, int oldLevel, int newLevel) {
		if (player == null) {
			return;
		}
		for (Quest q : questsById.values()) {
			q.onPlayerLevelUp(player, oldLevel, newLevel);
		}
	}

	public void onTutorialLink(GameSession player, String link) {
		Quest tutorial = getQuest(255);
		if (tutorial != null) {
			String cmd = link != null && link.startsWith("link ") ? link.substring(5).trim() : link;
			tutorial.notifyEvent(cmd, null, player);
		}
	}

	public void onTutorialPassCmd(GameSession player, String bypass) {
		Quest tutorial = getQuest(255);
		if (tutorial != null) {
			String cmd = bypass != null && bypass.startsWith("bypass -h ") ? bypass.substring(10).trim() : bypass;
			tutorial.notifyEvent(cmd, null, player);
		}
	}

	public void onTutorialQuestionMark(GameSession player, int number) {
		Quest tutorial = getQuest(255);
		if (tutorial != null) {
			tutorial.notifyEvent("QM" + number, null, player);
		}
	}

	public void onTutorialClientEvent(GameSession player, int eventId) {
		Quest tutorial = getQuest(255);
		if (tutorial != null) {
			tutorial.notifyEvent("CE" + eventId, null, player);
		}
	}
}
