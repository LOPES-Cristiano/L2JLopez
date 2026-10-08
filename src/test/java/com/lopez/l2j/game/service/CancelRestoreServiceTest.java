package com.lopez.l2j.game.service;

import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelRestoreServiceTest {

	private CancelRestoreService cancelService;
	private PlayerCharacter player;
	private GameSession session;

	@BeforeEach
	void setUp() {
		cancelService = new CancelRestoreService();
		cancelService.setRestoreDelaySeconds(15);

		player = mock(PlayerCharacter.class);
		session = mock(GameSession.class);

		PlayerEffects effects = new PlayerEffects();
		when(player.effects()).thenReturn(effects);
		when(player.objectId()).thenReturn(7001);
		when(player.name()).thenReturn("CancelTarget");
		when(player.isDead()).thenReturn(false);
		when(player.isOlympiadMode()).thenReturn(false);
		when(session.activeCharacter()).thenReturn(player);
	}

	@Test
	void testCancelBuffsAndRestore() {
		// Adiciona 5 buffs ao jogador
		long oneHour = 3600_000L;
		player.effects().addBuff(1068, 3, oneHour); // Might
		player.effects().addBuff(1040, 3, oneHour); // Shield
		player.effects().addBuff(1086, 2, oneHour); // Haste
		player.effects().addBuff(1204, 2, oneHour); // Wind Walk
		player.effects().addBuff(1085, 3, oneHour); // Acumen

		assertEquals(5, player.effects().active().size());

		// Cancela 3 buffs
		List<PlayerEffects.ActiveBuff> canceled = cancelService.applyCancelWithRestore(player, session, 3);
		assertEquals(3, canceled.size());
		assertEquals(2, player.effects().active().size());
		assertEquals(1, cancelService.getPendingTasks().size());

		int taskId = cancelService.getPendingTasks().keySet().iterator().next();

		// Executa callback de restauracao dos 15 segundos
		cancelService.restoreTaskExecution(taskId, player, session);

		// Todos os 5 buffs foram restaurados!
		assertEquals(5, player.effects().active().size());
		assertEquals(0, cancelService.getPendingTasks().size());
	}

	@Test
	void testNoblesseBlessingProtectedFromCancel() {
		long oneHour = 3600_000L;
		player.effects().addBuff(CancelRestoreService.NOBLESSE_BLESSING_SKILL_ID, 1, oneHour); // Noblesse Blessing
		player.effects().addBuff(1068, 3, oneHour); // Might

		List<PlayerEffects.ActiveBuff> canceled = cancelService.applyCancelWithRestore(player, session, 5);
		assertEquals(1, canceled.size());
		assertEquals(1068, canceled.get(0).skillId());

		// Noblesse permanece ativo
		assertTrue(player.effects().hasSkill(CancelRestoreService.NOBLESSE_BLESSING_SKILL_ID));
	}

	@Test
	void testRestorationDiscardedIfPlayerDies() {
		long oneHour = 3600_000L;
		player.effects().addBuff(1068, 3, oneHour);
		player.effects().addBuff(1040, 3, oneHour);

		cancelService.applyCancelWithRestore(player, session, 2);
		assertEquals(0, player.effects().active().size());

		int taskId = cancelService.getPendingTasks().keySet().iterator().next();

		// Jogador morre antes de completar os 15s
		when(player.isDead()).thenReturn(true);
		cancelService.restoreTaskExecution(taskId, player, session);

		// Buffs nao devem ser restaurados
		assertEquals(0, player.effects().active().size());
	}

	@Test
	void testRestorationDiscardedInOlympiadMode() {
		long oneHour = 3600_000L;
		player.effects().addBuff(1068, 3, oneHour);

		cancelService.applyCancelWithRestore(player, session, 1);
		assertEquals(0, player.effects().active().size());

		int taskId = cancelService.getPendingTasks().keySet().iterator().next();

		// Jogador entra em combate nas Olimpiadas
		when(player.isOlympiadMode()).thenReturn(true);
		cancelService.restoreTaskExecution(taskId, player, session);

		// Buffs cancelados nas Olimpiadas nao retornam (regra estrita retail)
		assertEquals(0, player.effects().active().size());
	}
}
