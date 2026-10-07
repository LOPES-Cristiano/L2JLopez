package com.lopez.l2j.network.game.security;

import com.lopez.l2j.network.game.packet.GameClientPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro declarativo de taxa de pacotes (Anti-Flood Rate Limiter).
 * Inspirado nas regras industriais do packetfilter.xml para neutralizar exploits e spam de rede.
 */
@Service
public class PacketRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(PacketRateLimiter.class);

	public record PacketRule(int maxCount, long perMs, boolean actionFailed, boolean drop, boolean logWarning) {
		public static PacketRule of(int maxCount, long perMs, boolean actionFailed, boolean drop) {
			return new PacketRule(maxCount, perMs, actionFailed, drop, true);
		}
	}

	public record RateCheckResult(boolean allowed, boolean shouldSendActionFailed, boolean shouldDrop) {
		public static final RateCheckResult ALLOWED = new RateCheckResult(true, false, false);
		public static final RateCheckResult RATE_LIMITED = new RateCheckResult(false, true, true);
	}

	private final Map<Class<?>, PacketRule> rules = new ConcurrentHashMap<>();

	public PacketRateLimiter() {
		initDefaultRules();
	}

	private void initDefaultRules() {
		// Regras calibradas conforme padrao de packetfilter.xml
		rules.put(GameClientPacket.AttackRequest.class, PacketRule.of(2, 300, true, true));
		rules.put(GameClientPacket.RequestEnchantItem.class, PacketRule.of(1, 500, true, true));
		rules.put(GameClientPacket.RequestDestroyItem.class, PacketRule.of(2, 300, true, true));
		rules.put(GameClientPacket.RequestItemList.class, PacketRule.of(5, 1000, true, false));
		rules.put(GameClientPacket.RequestMagicSkillUse.class, PacketRule.of(5, 400, true, true));
		rules.put(GameClientPacket.SendBypassBuildCmd.class, PacketRule.of(5, 1000, true, true));
		rules.put(GameClientPacket.Say2.class, PacketRule.of(10, 1000, true, false));
		rules.put(GameClientPacket.RequestRestart.class, PacketRule.of(1, 2000, true, true));
	}

	public void registerRule(Class<?> packetClass, PacketRule rule) {
		rules.put(packetClass, rule);
	}

	public static class SessionRateState {
		private final Map<Class<?>, Deque<Long>> timestampsByPacket = new ConcurrentHashMap<>();

		public synchronized boolean recordAndCheck(Class<?> packetClass, int maxCount, long windowMs, long now) {
			Deque<Long> queue = timestampsByPacket.computeIfAbsent(packetClass, k -> new ArrayDeque<>(maxCount + 1));

			// Remove timestamps mais antigos que a janela de tempo
			while (!queue.isEmpty() && (now - queue.peekFirst()) > windowMs) {
				queue.pollFirst();
			}

			if (queue.size() >= maxCount) {
				return false; // Limite excedido
			}

			queue.addLast(now);
			return true;
		}
	}

	public RateCheckResult checkRate(SessionRateState sessionState, Class<?> packetClass) {
		if (sessionState == null || packetClass == null) {
			return RateCheckResult.ALLOWED;
		}

		PacketRule rule = rules.get(packetClass);
		if (rule == null) {
			return RateCheckResult.ALLOWED;
		}

		long now = System.currentTimeMillis();
		boolean allowed = sessionState.recordAndCheck(packetClass, rule.maxCount(), rule.perMs(), now);

		if (!allowed) {
			if (rule.logWarning()) {
				log.debug("Rate limit atingido para o pacote {} (max {} em {}ms)",
						packetClass.getSimpleName(), rule.maxCount(), rule.perMs());
			}
			return new RateCheckResult(false, rule.actionFailed(), rule.drop());
		}

		return RateCheckResult.ALLOWED;
	}
}
