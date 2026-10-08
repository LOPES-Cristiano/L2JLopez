package com.lopez.l2j.network.security;

import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.security.FloodProtectorChannel;
import com.lopez.l2j.network.game.security.GranularFloodProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários para a Onda C11:
 * FloodProtectors Granulares de Alta Frequência (10 canais críticos com punições progressivas).
 *
 * <p>Critério de Aceite oficial:</p>
 * <ul>
 *   <li>Simulação de envio de 50 pacotes UseItem em menos de 100ms comprovando o descarte seguro e punição progressiva sem sobrecarga de CPU.</li>
 *   <li>10 canais críticos ativos (UseItem, UsePotion, Subclass, Enchant, Warehouse, Social, Hero, Trade, Move, Multisell).</li>
 * </ul>
 */
class GranularFloodProtectorTest {

	private GranularFloodProtector floodProtector;
	private GranularFloodProtector.FloodSessionState sessionState;

	@BeforeEach
	void setUp() {
		floodProtector = new GranularFloodProtector();
		sessionState = new GranularFloodProtector.FloodSessionState();
	}

	@Test
	@DisplayName("Criterio Onda C11: 50 pacotes UseItem em menos de 100ms com descarte seguro e punicao progressiva")
	void testFiftyUseItemPacketsInUnder100Ms() {
		long startTimestamp = 1000000L;
		long startNano = System.nanoTime();

		int allowedCount = 0;
		int actionFailedCount = 0;
		int logWarningCount = 0;
		int kickCount = 0;

		// Dispara 50 verificacoes consecutivas em passos de 1ms (total = 50ms < 100ms)
		for (int i = 0; i < 50; i++) {
			long currentTimestamp = startTimestamp + i;
			var result = floodProtector.check(sessionState, FloodProtectorChannel.USE_ITEM, currentTimestamp);

			if (result.allowed()) {
				allowedCount++;
			} else {
				switch (result.punishment()) {
					case ACTION_FAILED -> actionFailedCount++;
					case LOG_WARNING -> logWarningCount++;
					case KICK -> kickCount++;
					default -> {}
				}
			}
		}

		long durationNano = System.nanoTime() - startNano;
		long durationMs = durationNano / 1_000_000L;

		// 1. Apenas a 1ª chamada dentro da janela de 100ms e permitida
		assertEquals(1, allowedCount, "Exatamente 1 requisicao deve ser permitida no inicio da janela");

		// 2. As 49 seguintes sao progressivamente rejeitadas
		assertEquals(2, actionFailedCount, "Requisicoes 2 e 3 sofrem descarte silencioso simples (ActionFailed)");
		assertTrue(logWarningCount > 0, "Requisicoes seguintes sofrem alerta de seguranca");
		assertTrue(kickCount > 0, "Apos exceder o limite de punicao (15), atinge punicao KICK");
		assertEquals(49, actionFailedCount + logWarningCount + kickCount, "Todas as 49 chamadas excedentes foram bloqueadas");

		// 3. Eficiencia extrema de CPU: 50 checagens executadas em menos de 20ms
		assertTrue(durationMs < 20, "Tempo total de processamento deve ser desprezivel (sem sobrecarga de CPU): " + durationMs + "ms");
	}

	@Test
	@DisplayName("Criterio Onda C11: Requisicao permitida apos expiracao do intervalo do canal")
	void testUseItemAllowedAfterIntervalExpires() {
		long t0 = 1000L;

		// 1. Primeira requisicao permitida
		var res1 = floodProtector.check(sessionState, FloodProtectorChannel.USE_ITEM, t0);
		assertTrue(res1.allowed());

		// 2. Requisicao aos 50ms (intervalo = 100ms) -> bloqueada
		var res2 = floodProtector.check(sessionState, FloodProtectorChannel.USE_ITEM, t0 + 50);
		assertFalse(res2.allowed());

		// 3. Requisicao aos 120ms (decorridos > 100ms) -> liberada com sucesso
		var res3 = floodProtector.check(sessionState, FloodProtectorChannel.USE_ITEM, t0 + 120);
		assertTrue(res3.allowed(), "Apos passar a janela do intervalo de 100ms, nova requisicao deve ser permitida");
	}

	@Test
	@DisplayName("Criterio Onda C11: Todos os 10 canais criticos definidos com limites industriais")
	void testAllTenChannelsConfigured() {
		assertEquals(10, FloodProtectorChannel.values().length, "Devem existir exatamente 10 canais criticos de protecao");

		assertNotNull(FloodProtectorChannel.USE_ITEM);
		assertNotNull(FloodProtectorChannel.USE_POTION);
		assertNotNull(FloodProtectorChannel.SUBCLASS_CHANGE);
		assertNotNull(FloodProtectorChannel.REQUEST_ENCHANT);
		assertNotNull(FloodProtectorChannel.WAREHOUSE_TRANSACTION);
		assertNotNull(FloodProtectorChannel.SOCIAL_ACTION);
		assertNotNull(FloodProtectorChannel.HERO_VOICE);
		assertNotNull(FloodProtectorChannel.TRADE_REQUEST);
		assertNotNull(FloodProtectorChannel.MOVE_ACTION);
		assertNotNull(FloodProtectorChannel.MULTISELL);

		for (FloodProtectorChannel ch : FloodProtectorChannel.values()) {
			assertTrue(ch.intervalMs() > 0, "Intervalo de " + ch.name() + " deve ser positivo");
			assertTrue(ch.punishmentLimit() > 0, "Limite de punicao de " + ch.name() + " deve ser positivo");
		}
	}

	@Test
	@DisplayName("Criterio Onda C11: Mapeamento automatico de pacotes para canais de protecao")
	void testPacketChannelResolution() {
		assertEquals(FloodProtectorChannel.USE_ITEM, floodProtector.resolveChannel(new GameClientPacket.UseItem(12345)));
		assertEquals(FloodProtectorChannel.MULTISELL, floodProtector.resolveChannel(new GameClientPacket.MultiSellChoose(1, 1, 1)));
		assertEquals(FloodProtectorChannel.MOVE_ACTION, floodProtector.resolveChannel(new GameClientPacket.MoveBackwardToLocation(100, 200, 300, 0, 0, 0)));
		assertEquals(FloodProtectorChannel.REQUEST_ENCHANT, floodProtector.resolveChannel(new GameClientPacket.RequestEnchantItem(99)));
	}
}
