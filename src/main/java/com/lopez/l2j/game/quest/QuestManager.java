package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList.QuestEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Gerenciador central de Quests do L2JLopez.
 * Controla registro de quests, persistencia de variaveis em character_quests e despacho de eventos.
 */
@Service
public class QuestManager {

	private static final Logger log = LoggerFactory.getLogger(QuestManager.class);

	private final JdbcTemplate jdbcTemplate;
	private final Map<Integer, Quest> questsById = new ConcurrentHashMap<>();
	private final Map<String, Quest> questsByName = new ConcurrentHashMap<>();

	@Autowired
	public QuestManager(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void registerQuest(Quest quest) {
		if (quest == null) {
			return;
		}
		questsById.put(quest.getQuestId(), quest);
		questsByName.put(quest.getName().toLowerCase(java.util.Locale.ROOT), quest);
		log.info("Quest registrada: [{}] {}", quest.getQuestId(), quest.getName());
	}

	public Quest getQuest(int questId) {
		return questsById.get(questId);
	}

	public Quest getQuest(String name) {
		if (name == null) {
			return null;
		}
		return questsByName.get(name.toLowerCase(java.util.Locale.ROOT));
	}

	public Collection<Quest> getAllQuests() {
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
								qs.setState(State.STARTED);
							} else if ("Completed".equalsIgnoreCase(val)) {
								qs.setState(State.COMPLETED);
							} else {
								qs.setState(State.CREATED);
							}
						} else {
							qs.set(var, val);
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

	// ==================== Dispatchers ====================

	public void onPlayerLogin(GameSession player) {
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
			if (qs.isStarted() && qs.getQuest() != null) {
				entries.add(new QuestEntry(qs.getQuest().getQuestId(), qs.getCond()));
			}
		}
		player.send(new QuestList(entries));
	}

	public String onNpcTalk(NpcInstance npc, GameSession player) {
		if (npc == null || player == null) {
			return null;
		}
		int npcId = npc.npcId();
		for (Quest q : questsById.values()) {
			if (q.hasTalkNpc(npcId) || q.hasStartNpc(npcId)) {
				String html = q.notifyTalk(npc, player);
				if (html != null && !html.isBlank()) {
					return html;
				}
			}
		}
		return null;
	}

	public String onNpcFirstTalk(NpcInstance npc, GameSession player) {
		if (npc == null || player == null) {
			return null;
		}
		int npcId = npc.npcId();
		for (Quest q : questsById.values()) {
			if (q.hasFirstTalkNpc(npcId)) {
				String html = q.notifyFirstTalk(npc, player);
				if (html != null && !html.isBlank()) {
					return html;
				}
			}
		}
		return null;
	}

	public void onNpcKill(NpcInstance npc, GameSession player, boolean isPet) {
		if (npc == null || player == null) {
			return;
		}
		int npcId = npc.npcId();
		for (Quest q : questsById.values()) {
			if (q.hasKillNpc(npcId)) {
				q.notifyKill(npc, player, isPet);
			}
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
			tutorial.notifyEvent(link, null, player);
		}
	}

	public void onTutorialPassCmd(GameSession player, String bypass) {
		Quest tutorial = getQuest(255);
		if (tutorial != null) {
			tutorial.notifyEvent(bypass, null, player);
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
