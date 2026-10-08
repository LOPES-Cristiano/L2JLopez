package com.lopez.l2j.network.game.security;

import com.lopez.l2j.network.game.packet.GameClientPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Servico de protecao granular de alta frequencia contra flood de pacotes e exploits - Onda C11.
 *
 * <p>Punições Progressivas:</p>
 * <ol>
 *   <li>1 a 2 requisicoes em excesso: Descarte silencioso (ActionFailed).</li>
 *   <li>3 a (limite-1) requisicoes em excesso: Log de seguranca (LOG_WARNING) + descarte silencioso.</li>
 *   <li>Acima do limite de punicao: Punição severa configurada (KICK da conexao ou BAN temporario).</li>
 * </ol>
 */
@Service
public class GranularFloodProtector {

	private static final Logger log = LoggerFactory.getLogger(GranularFloodProtector.class);

	public record FloodCheckResult(
			boolean allowed,
			FloodProtectorChannel.PunishmentType punishment,
			int violationCount,
			String message
	) {
		public static FloodCheckResult allow() {
			return new FloodCheckResult(true, FloodProtectorChannel.PunishmentType.NONE, 0, null);
		}

		public static FloodCheckResult reject(FloodProtectorChannel.PunishmentType punishment, int count, String msg) {
			return new FloodCheckResult(false, punishment, count, msg);
		}
	}

	public static class FloodSessionState {
		private final long[] lastTimestamps = new long[FloodProtectorChannel.values().length];
		private final int[] violations = new int[FloodProtectorChannel.values().length];

		public synchronized FloodCheckResult evaluate(FloodProtectorChannel channel, long now) {
			int idx = channel.ordinal();
			long elapsed = now - lastTimestamps[idx];

			if (elapsed < channel.intervalMs()) {
				violations[idx]++;
				int vCount = violations[idx];

				if (vCount >= channel.punishmentLimit()) {
					return FloodCheckResult.reject(channel.punishment(), vCount,
							"Limite severo de flood atingido no canal " + channel.channelName() + ". Punicao: " + channel.punishment());
				} else if (vCount >= 3) {
					return FloodCheckResult.reject(FloodProtectorChannel.PunishmentType.LOG_WARNING, vCount,
							"Atividade excessiva de flood detectada no canal " + channel.channelName() + " (tentativa " + vCount + ").");
				} else {
					return FloodCheckResult.reject(FloodProtectorChannel.PunishmentType.ACTION_FAILED, vCount,
							"Requisicao descartada por frequencia rapida (ActionFailed).");
				}
			} else {
				violations[idx] = 0;
				lastTimestamps[idx] = now;
				return FloodCheckResult.allow();
			}
		}

		public int getViolations(FloodProtectorChannel channel) {
			return violations[channel.ordinal()];
		}
	}

	/**
	 * Avalia a requisicao no canal especificado usando timestamp atual.
	 */
	public FloodCheckResult check(FloodSessionState sessionState, FloodProtectorChannel channel) {
		return check(sessionState, channel, System.currentTimeMillis());
	}

	/**
	 * Avalia a requisicao no canal especificado com timestamp customizado (para testes deterministicos de alta taxa).
	 */
	public FloodCheckResult check(FloodSessionState sessionState, FloodProtectorChannel channel, long timestamp) {
		if (sessionState == null || channel == null) {
			return FloodCheckResult.allow();
		}

		FloodCheckResult result = sessionState.evaluate(channel, timestamp);

		if (!result.allowed()) {
			if (result.punishment() == FloodProtectorChannel.PunishmentType.LOG_WARNING) {
				log.warn("Security Alert: {}", result.message());
			} else if (result.punishment() == FloodProtectorChannel.PunishmentType.KICK) {
				log.error("Security Punishment Triggered: {}", result.message());
			}
		}

		return result;
	}

	/**
	 * Identifica o canal critico correspondente a um pacote do cliente.
	 */
	public FloodProtectorChannel resolveChannel(GameClientPacket packet) {
		if (packet == null) {
			return null;
		}

		return switch (packet) {
			case GameClientPacket.UseItem ignored -> FloodProtectorChannel.USE_ITEM;
			case GameClientPacket.MoveBackwardToLocation ignored -> FloodProtectorChannel.MOVE_ACTION;
			case GameClientPacket.ValidatePosition ignored -> FloodProtectorChannel.MOVE_ACTION;
			case GameClientPacket.RequestEnchantItem ignored -> FloodProtectorChannel.REQUEST_ENCHANT;
			case GameClientPacket.MultiSellChoose ignored -> FloodProtectorChannel.MULTISELL;
			case GameClientPacket.SendWareHouseDepositList ignored -> FloodProtectorChannel.WAREHOUSE_TRANSACTION;
			case GameClientPacket.SendWareHouseWithDrawList ignored -> FloodProtectorChannel.WAREHOUSE_TRANSACTION;
			case GameClientPacket.RequestActionUse ignored -> FloodProtectorChannel.SOCIAL_ACTION;
			default -> null;
		};
	}
}
