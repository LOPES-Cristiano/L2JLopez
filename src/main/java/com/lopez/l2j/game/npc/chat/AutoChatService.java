package com.lopez.l2j.game.npc.chat;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico para falas e dialogos periodicos automaticos de NPCs (guardas, sacerdotes de Seven Signs, etc).
 * Porta de AutoChatHandler do legado L2JDream.
 */
@Service
public class AutoChatService {

	private static final Logger log = LoggerFactory.getLogger(AutoChatService.class);
	public static final int CHAT_RANGE = 1250;

	private final AutoChatTable chatTable;
	private final GameWorld world;

	private final Map<Integer, AutoChatSession> activeSessions = new ConcurrentHashMap<>();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("autochat-clock-", 0).factory());

	@Autowired
	public AutoChatService(AutoChatTable chatTable, GameWorld world) {
		this.chatTable = chatTable;
		this.world = world;
	}

	@PostConstruct
	public void init() {
		scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
		log.info("AutoChatService iniciado com {} chats pre-configurados.", chatTable.size());
	}

	@PreDestroy
	public void stop() {
		scheduler.shutdown();
		activeSessions.clear();
	}

	public boolean registerNpc(NpcInstance npc) {
		if (npc == null) {
			return false;
		}
		Optional<AutoChatData> dataOpt = chatTable.getByNpcId(npc.npcId());
		if (dataOpt.isEmpty()) {
			return false;
		}
		return registerCustomChat(npc, dataOpt.get().chatDelayMs(), dataOpt.get().chatTexts());
	}

	public boolean registerCustomChat(NpcInstance npc, long delayMs, List<String> texts) {
		if (npc == null || texts == null || texts.isEmpty()) {
			return false;
		}
		AutoChatData data = new AutoChatData(0, npc.npcId(), delayMs, texts);
		AutoChatSession session = new AutoChatSession(npc, data, 0, -1L);
		activeSessions.put(npc.objectId(), session);
		return true;
	}

	public void unregisterNpc(int objectId) {
		activeSessions.remove(objectId);
	}

	public int activeSessionsCount() {
		return activeSessions.size();
	}

	public Optional<AutoChatSession> getSession(int objectId) {
		return Optional.ofNullable(activeSessions.get(objectId));
	}

	public void tick() {
		step(System.currentTimeMillis());
	}

	public void step(long nowMillis) {
		for (AutoChatSession session : activeSessions.values()) {
			try {
				if (session.npc.isDead()) {
					continue;
				}

				if (session.nextChatTime < 0) {
					session.nextChatTime = nowMillis + session.data.chatDelayMs();
				}

				if (nowMillis >= session.nextChatTime) {
					triggerChat(session, nowMillis);
				}
			} catch (Exception e) {
				log.warn("Erro no processamento de AutoChat para NPC {}: {}", session.npc.objectId(), e.getMessage());
			}
		}
	}

	public void triggerChat(int objectId, long nowMillis) {
		AutoChatSession session = activeSessions.get(objectId);
		if (session != null) {
			triggerChat(session, nowMillis);
		}
	}

	private void triggerChat(AutoChatSession session, long nowMillis) {
		List<String> texts = session.data.chatTexts();
		if (texts.isEmpty()) {
			return;
		}

		String rawText = texts.get(session.currentIndex % texts.size());
		session.currentIndex = (session.currentIndex + 1) % texts.size();
		session.nextChatTime = nowMillis + session.data.chatDelayMs();

		String formattedText = formatChatText(rawText, session.npc);

		if (world != null) {
			CreatureSay say = new CreatureSay(session.npc.objectId(), CreatureSay.ALL, session.npc.name(), formattedText);
			world.broadcast(say, p -> isInsideRadius(p.x(), p.y(), session.npc.x(), session.npc.y(), CHAT_RANGE));
		}
	}

	private String formatChatText(String rawText, NpcInstance npc) {
		if (rawText == null) {
			return "";
		}
		if (!rawText.contains("%")) {
			return rawText;
		}

		String out = rawText;
		if (out.contains("%player_cabal_loser%") || out.contains("%player_cabal_winner%") || out.contains("%player_name%")) {
			String playerName = "Adventurer";
			if (world != null) {
				for (GameWorld.OnlinePlayer p : world.players()) {
					if (isInsideRadius(p.x(), p.y(), npc.x(), npc.y(), CHAT_RANGE)) {
						playerName = p.name();
						break;
					}
				}
			}
			out = out.replace("%player_cabal_loser%", playerName)
					.replace("%player_cabal_winner%", playerName)
					.replace("%player_name%", playerName);
		}
		return out;
	}

	private static boolean isInsideRadius(int x1, int y1, int x2, int y2, int radius) {
		long dx = x1 - x2;
		long dy = y1 - y2;
		return dx * dx + dy * dy <= (long) radius * radius;
	}

	public static final class AutoChatSession {
		private final NpcInstance npc;
		private final AutoChatData data;
		private int currentIndex;
		private long nextChatTime;

		public AutoChatSession(NpcInstance npc, AutoChatData data, int currentIndex, long nextChatTime) {
			this.npc = npc;
			this.data = data;
			this.currentIndex = currentIndex;
			this.nextChatTime = nextChatTime;
		}

		public NpcInstance npc() { return npc; }
		public AutoChatData data() { return data; }
		public int currentIndex() { return currentIndex; }
		public long nextChatTime() { return nextChatTime; }
	}
}
